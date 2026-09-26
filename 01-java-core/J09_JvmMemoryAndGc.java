import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.ref.WeakReference;
import java.util.List;

/*
 * J09  JVM memory (stack, heap, metaspace), GC, StackOverflowError vs OutOfMemoryError
 *
 * Read J09_JvmMemoryAndGc.md first. This file runs the same steps so you can
 * see them happen. The step numbers match the .md file.
 *
 * Memory sizes, GC counts and the recursion depth depend on your laptop and
 * JVM, so your numbers will be different. Catching Errors here is ONLY for the
 * demo; never do it in real code.
 *
 * Run it:  java 01-java-core/J09_JvmMemoryAndGc.java
 *          (or click "Run" above main() in VS Code)
 */
public class J09_JvmMemoryAndGc {

    static class Payment {
        final String txnId;
        int amount;

        Payment(String txnId, int amount) {
            this.txnId = txnId;
            this.amount = amount;
        }
    }

    static Object sink;              // keeps each new object "used", so Java can't skip creating it
    static int depth;                // how deep the recursion got (Step 5)

    public static void main(String[] args) {
        step("Step 1: the stack, one frame per method call");
        processPayment(1500);

        step("Step 2: the heap, where every object lives");
        Runtime runtime = Runtime.getRuntime();
        long ram = ((com.sun.management.OperatingSystemMXBean)
                ManagementFactory.getOperatingSystemMXBean()).getTotalMemorySize();
        System.out.println("RAM on this machine : " + mb(ram) + " MB");
        System.out.println("max heap (-Xmx)     : " + mb(runtime.maxMemory()) + " MB  (default: about RAM / 4)");
        System.out.println("heap in use now     : " + mb(runtime.totalMemory() - runtime.freeMemory()) + " MB");

        Payment p = new Payment("TXN1001", 1500);
        changeAmount(p);                              // the method gets a COPY of the reference
        System.out.println("after changeAmount(p) : amount = " + p.amount + "  (same object, field changed)");
        replacePayment(p);                            // reassigning the copy doesn't touch our p
        System.out.println("after replacePayment(p): txnId = " + p.txnId + " (our variable still points to the old object)");

        step("Step 3: metaspace, and the heap's young and old areas");
        for (MemoryPoolMXBean pool : ManagementFactory.getMemoryPoolMXBeans()) {
            System.out.printf("  %-32s %5d MB used%n", pool.getName(), mb(pool.getUsage().getUsed()));
        }

        step("Step 4: garbage collection");
        long youngBefore = youngGcCount();
        for (int i = 0; i < 5_000_000; i++) {
            sink = new byte[128];                     // each old array becomes garbage right away
        }
        System.out.println("created about 700 MB of short-lived garbage");
        System.out.println("young GCs that ran meanwhile: " + (youngGcCount() - youngBefore) + " (nobody called them)");

        Payment payment = new Payment("TXN2002", 800);
        WeakReference<Payment> watcher = new WeakReference<>(payment);   // watches without keeping it alive
        System.out.println("before: payment is " + (watcher.get() != null ? "alive" : "collected"));
        payment = null;                               // no normal reference left, so it's garbage now
        System.gc();                                  // only a REQUEST to run the GC
        System.out.println("after payment = null and System.gc(): " + (watcher.get() != null ? "still alive" : "collected"));

        step("Step 5: StackOverflowError");
        try {
            callMyselfForever();
        } catch (StackOverflowError e) {              // demo only: never catch Errors in real code
            System.out.println("StackOverflowError after " + depth + " nested calls (the thread's stack was full)");
        }

        step("Step 6: OutOfMemoryError");
        long maxHeap = runtime.maxMemory();
        long longsNeeded = maxHeap / 8 * 2;           // an array of longs twice the size of the whole heap
        if (longsNeeded > Integer.MAX_VALUE - 8) {
            System.out.println("heap too big for this trick; run: java -Xmx256m 01-java-core/J09_JvmMemoryAndGc.java");
        } else {
            try {
                long[] tooBig = new long[(int) longsNeeded];
                System.out.println(tooBig.length);
            } catch (OutOfMemoryError e) {            // demo only
                System.out.println("OutOfMemoryError: " + e.getMessage()
                        + " (asked for " + mb(longsNeeded * 8) + " MB, max heap is " + mb(maxHeap) + " MB)");
            }
        }
    }

    // -------------------------------------------------------------------------
    // Step 1: three nested calls = three frames on this thread's stack
    // -------------------------------------------------------------------------
    static void processPayment(int amount) {          // amount (a primitive) lives in this frame
        Payment p = new Payment("TXN1001", amount);   // p (a reference) in this frame, the object on the heap
        validate(p);
    }

    static void validate(Payment p) {
        // StackWalker (Java 9+) lists the frames of THIS class that are on the stack right now.
        List<String> frames = StackWalker.getInstance()
                .walk(s -> s.filter(f -> f.getClassName().equals(J09_JvmMemoryAndGc.class.getName()))
                        .map(StackWalker.StackFrame::getMethodName)
                        .toList());
        System.out.println("frames on the stack (top first): " + frames);
        System.out.println("validating " + p.txnId + " for Rs " + p.amount);
    }

    // -------------------------------------------------------------------------
    // Step 2: Java passes a COPY of the reference (pass-by-value)
    // -------------------------------------------------------------------------
    static void changeAmount(Payment copyOfReference) {
        copyOfReference.amount = 2000;                // follows the copy to the SAME object
    }

    static void replacePayment(Payment copyOfReference) {
        copyOfReference = new Payment("TXN9999", 1);  // only the copy now points somewhere else
        sink = copyOfReference;
    }

    // -------------------------------------------------------------------------
    // Step 5: recursion with no stopping condition
    // -------------------------------------------------------------------------
    static void callMyselfForever() {
        depth++;
        callMyselfForever();                          // every call adds a frame; none ever returns
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------
    static long youngGcCount() {
        long count = 0;
        for (GarbageCollectorMXBean gc : ManagementFactory.getGarbageCollectorMXBeans()) {
            if (gc.getName().contains("Young")) {     // G1 calls it "G1 Young Generation"
                count += gc.getCollectionCount();
            }
        }
        return count;
    }

    static long mb(long bytes) {
        return bytes / (1024 * 1024);
    }

    static void step(String text) {
        System.out.println();
        System.out.println("=== " + text + " ===");
    }
}
