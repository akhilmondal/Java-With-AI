import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;

/*
 * J03  Strings, the String pool, StringBuilder vs StringBuffer: runnable demo
 *
 * Read J03_StringsAndStringPool.md first. This file runs the same steps so you
 * can see them happen. The step numbers match the .md file.
 *
 * Run it:  java 01-java-core/J03_StringsAndStringPool.java
 *          (or click "Run" above main() in VS Code)
 */
public class J03_StringsAndStringPool {

    public static void main(String[] args) throws InterruptedException {
        step("Step 1: a String never changes");
        String gateway = "PAYU";
        gateway.toLowerCase();                         // makes a NEW String "payu", but we don't keep it
        System.out.println("after gateway.toLowerCase(): gateway = " + gateway);          // PAYU
        String other = gateway;                        // a second variable pointing to "PAYU"
        gateway = gateway.toLowerCase();               // gateway now points to the new "payu"
        System.out.println("after gateway = gateway.toLowerCase(): gateway = " + gateway
                + ", other = " + other);                                                   // payu, PAYU

        step("Step 2: the String pool");
        String a = "PAYU";                             // a literal goes into the pool
        String b = "PAYU";                             // the pool already has it: the SAME object
        String c = new String("PAYU");                 // "new" always makes a separate object
        System.out.println("a == b          : " + (a == b));            // true
        System.out.println("a == c          : " + (a == c));            // false
        System.out.println("a.equals(c)     : " + a.equals(c));         // true
        System.out.println("a == c.intern() : " + (a == c.intern()));   // true: intern() gives the pool copy

        step("Step 3: why immutable (sharing is only safe if nobody can change it)");
        System.out.println("a and b are the same object, so if a could change it, b would change too.");
        System.out.println("String also saves its hashCode after the first call: "
                + "\"PAYU\".hashCode() = " + a.hashCode());

        step("Step 4: joined by the compiler vs joined while running");
        String pa = "PA";
        final String finalPa = "PA";
        // Brackets matter: without them, Java would join the label text first.
        System.out.println("\"PA\" + \"YU\" == \"PAYU\"   : " + ("PA" + "YU" == "PAYU"));   // true: compiler joins it
        System.out.println("pa + \"YU\" == \"PAYU\"     : " + (pa + "YU" == "PAYU"));       // false: joined while running
        System.out.println("finalPa + \"YU\" == \"PAYU\": " + (finalPa + "YU" == "PAYU"));  // true: final + literal is a constant

        step("Step 5: joining in a loop");
        String line = "";
        int copied = 0;
        for (int round = 1; round <= 5; round++) {
            line += "TXN0001";                         // a brand-new String every round
            copied += line.length();                   // the whole new String had to be written
            System.out.println("round " + round + ": new String of " + line.length() + " chars");
        }
        System.out.println("total characters copied with += : " + copied);               // 105
        System.out.println("StringBuilder writes each char once: " + line.length());     // 35

        int n = 50_000;
        long start = System.nanoTime();
        String slow = "";
        for (int i = 0; i < n; i++) {
            slow += "x";                               // n new Strings
        }
        long stringMs = (System.nanoTime() - start) / 1_000_000;

        start = System.nanoTime();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) {
            sb.append("x");                            // one growing buffer
        }
        String fast = sb.toString();
        long builderMs = (System.nanoTime() - start) / 1_000_000;
        System.out.println(n + " joins: String += took " + stringMs + " ms, StringBuilder took "
                + builderMs + " ms (same result: " + slow.equals(fast) + ")");

        step("Step 6: StringBuilder vs StringBuffer with two threads");
        StringBuffer buffer = new StringBuffer();
        StringBuilder builder = new StringBuilder();
        appendFromTwoThreads("StringBuffer ", buffer::append, buffer::length);    // always 200000
        appendFromTwoThreads("StringBuilder", builder::append, builder::length);  // less, and it changes every run
    }

    /**
     * Two threads each append "x" 100,000 times to the SAME object, so the right
     * final length is 200,000.
     */
    static void appendFromTwoThreads(String label, Consumer<String> append, Supplier<Integer> length)
            throws InterruptedException {
        AtomicBoolean crashed = new AtomicBoolean(false);   // a thread-safe true/false flag (topic J05)
        Runnable job = () -> {
            try {
                for (int i = 0; i < 100_000; i++) {
                    append.accept("x");
                }
            } catch (RuntimeException e) {
                // An unsafe StringBuilder can even throw ArrayIndexOutOfBoundsException
                // when two threads grow its internal array at the same time.
                crashed.set(true);
            }
        };
        Thread t1 = new Thread(job);
        Thread t2 = new Thread(job);
        t1.start();
        t2.start();
        t1.join();                                     // wait for both threads to finish
        t2.join();
        System.out.println(label + " length: " + length.get() + " (expected 200000)"
                + (crashed.get() ? " and one thread CRASHED" : ""));
    }

    static void step(String text) {
        System.out.println();
        System.out.println("=== " + text + " ===");
    }
}
