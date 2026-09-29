import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;

/*
 * J03  Strings, the String pool, StringBuilder vs StringBuffer: a runnable demo
 *
 * THE STORY (why these three classes exist)
 *   Shared text must never change   -> String is immutable        (Java 1.0, 1996)
 *   += in a loop copies everything  -> StringBuffer, with locks   (Java 1.0)
 *   One thread doesn't need locks   -> StringBuilder, no locks    (Java 5, 2004)
 *   So StringBuffer is the OLD one, and StringBuilder is today's default.
 *
 * WHAT YOU WILL SEE (the numbers match J03_StringsAndStringPool.md)
 *   Step 1  toLowerCase() doesn't change "PAYU"; it makes a NEW String
 *   Step 2  two "PAYU" literals are ONE object (==  true); new String(...) is another (== false)
 *   Step 3  why immutability matters: shared objects must never change
 *   Step 4  "PA" + "YU" is joined by the compiler (true); pa + "YU" at runtime (false)
 *   Step 5  += in a loop copies 7+14+21+28+35 = 105 chars; StringBuilder writes 35
 *   Step 6  two threads, one StringBuilder: appends get lost; StringBuffer: always 200000
 *
 * HOW TO RUN   java 01-java-core/J03_StringsAndStringPool.java   (or click "Run" above main)
 * READ FIRST   J03_StringsAndStringPool.md
 */
public class J03_StringsAndStringPool {

    public static void main(String[] args) throws InterruptedException {
        // Step 1: every "changing" method returns a NEW String. The original stays.
        step("Step 1: a String never changes");
        String gateway = "PAYU";
        gateway.toLowerCase();                         // makes a NEW String "payu", but we don't keep it
        System.out.println("after gateway.toLowerCase(): gateway = " + gateway);          // PAYU
        String other = gateway;                        // a second variable pointing to "PAYU"
        gateway = gateway.toLowerCase();               // gateway now points to the new "payu"
        System.out.println("after gateway = gateway.toLowerCase(): gateway = " + gateway
                + ", other = " + other);                                                   // payu, PAYU
        System.out.println("Notice: the variable moved to a new object; the old \"PAYU\" never changed.");

        // Step 2: literals come from the String pool (one shared copy);
        // "new" always creates a separate object.
        step("Step 2: the String pool");
        String a = "PAYU";                             // a literal goes into the pool
        String b = "PAYU";                             // the pool already has it: the SAME object
        String c = new String("PAYU");                 // "new" always makes a separate object
        System.out.println("a == b          : " + (a == b));            // true
        System.out.println("a == c          : " + (a == c));            // false
        System.out.println("a.equals(c)     : " + a.equals(c));         // true
        System.out.println("a == c.intern() : " + (a == c.intern()));   // true: intern() gives the pool copy
        System.out.println("Notice: == compares objects, equals() compares text. Always use equals() for Strings.");

        // Step 3: a and b are ONE object. If Strings could change, changing a would change b.
        step("Step 3: why immutable (sharing is only safe if nobody can change it)");
        System.out.println("a and b are the same object, so if a could change it, b would change too.");
        System.out.println("String also saves its hashCode after the first call: "
                + "\"PAYU\".hashCode() = " + a.hashCode());

        // Step 4: the compiler joins fixed text before the program runs (pool);
        // anything joined while running is a new object.
        step("Step 4: joined by the compiler vs joined while running");
        String pa = "PA";
        final String finalPa = "PA";
        // Brackets matter: without them, Java would join the label text first.
        System.out.println("\"PA\" + \"YU\" == \"PAYU\"   : " + ("PA" + "YU" == "PAYU"));   // true: compiler joins it
        System.out.println("pa + \"YU\" == \"PAYU\"     : " + (pa + "YU" == "PAYU"));       // false: joined while running
        System.out.println("finalPa + \"YU\" == \"PAYU\": " + (finalPa + "YU" == "PAYU"));  // true: final + literal is a constant

        // Step 5: += in a loop builds a brand-new String every round and copies
        // everything so far. StringBuilder keeps one growing buffer.
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
        System.out.println("Notice: += does about n x n / 2 work; StringBuilder does about n.");

        // Step 6: StringBuffer locks every method (thread-safe); StringBuilder doesn't.
        step("Step 6: StringBuilder vs StringBuffer with two threads");
        StringBuffer buffer = new StringBuffer();
        StringBuilder builder = new StringBuilder();
        appendFromTwoThreads("StringBuffer ", buffer::append, buffer::length);    // always 200000
        appendFromTwoThreads("StringBuilder", builder::append, builder::length);  // less, and it changes every run
        System.out.println("Notice: shared between threads -> StringBuffer; inside one method -> StringBuilder.");
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
