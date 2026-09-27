import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/*
 * J05  synchronized, volatile, atomics, ConcurrentHashMap: a runnable demo
 *
 * WHAT YOU WILL SEE (the numbers match J05_ConcurrencyBasics.md)
 *   Step 1  2 threads x 100,000 count++ on a plain int: far less than 200,000
 *   Step 2  synchronized: exactly 200,000
 *   Step 3  volatile int: still wrong; but a volatile stop flag works and a plain one doesn't
 *   Step 4  AtomicInteger (compare-and-set): exactly 200,000
 *   Step 5  HashMap loses entries with 2 threads; synchronizedMap and ConcurrentHashMap don't
 *   Step 6  get-then-put on a ConcurrentHashMap still loses updates; merge() doesn't
 *
 * Numbers marked "changes every run" come from a real race between threads,
 * so yours will be different. The point is that they are WRONG.
 *
 * HOW TO RUN   java 01-java-core/J05_ConcurrencyBasics.java   (or click "Run" above main)
 * READ FIRST   J05_ConcurrencyBasics.md
 */
public class J05_ConcurrencyBasics {

    static final int TIMES = 100_000;            // each thread records 100,000 payments

    public static void main(String[] args) throws InterruptedException {
        // Step 1: count++ is really three steps (read, add, write). Two threads
        // mix those steps up and overwrite each other's updates.
        step("Step 1: the race condition (count++ is read, add, write)");
        PlainCounter plain = new PlainCounter();
        runTogether(plain::add, plain::add);
        System.out.println("plain int    : " + plain.value + " (expected 200000, changes every run)");
        System.out.println("Notice: updates were lost. Nobody got an error; the number is just wrong.");

        // Step 2: synchronized lets only ONE thread into increment() at a time.
        step("Step 2: synchronized, one thread at a time");
        SyncCounter sync = new SyncCounter();
        runTogether(sync::add, sync::add);
        System.out.println("synchronized : " + sync.value + " (expected 200000)");

        // Step 3: volatile makes every read see the latest value, but read-add-write
        // can still interleave. It fixes the stop flag, not the counter.
        step("Step 3: volatile, visibility but NOT atomic");
        VolatileCounter vol = new VolatileCounter();
        runTogether(vol::add, vol::add);
        System.out.println("volatile int : " + vol.value + " (expected 200000, still wrong)");
        stopFlagDemo();
        System.out.println("Notice: volatile fixed the flag (visibility) but not the counter (atomicity).");

        // Step 4: AtomicInteger uses the CPU's compare-and-set: "write 6 only if it's
        // still 5; otherwise read again and retry". Nothing is lost, and no lock is taken.
        step("Step 4: AtomicInteger, compare-and-set");
        AtomicInteger atomic = new AtomicInteger();
        Runnable addAtomic = () -> {
            for (int i = 0; i < TIMES; i++) {
                atomic.incrementAndGet();
            }
        };
        runTogether(addAtomic, addAtomic);
        System.out.println("AtomicInteger: " + atomic.get() + " (expected 200000)");

        // Step 5: two threads put 50,000 DIFFERENT keys each into the same map.
        step("Step 5: three maps, two threads, 100,000 different keys");
        System.out.println("HashMap              : " + fillFromTwoThreads(new HashMap<>())
                + " (expected 100000)");
        System.out.println("synchronizedMap      : " + fillFromTwoThreads(Collections.synchronizedMap(new HashMap<>()))
                + " (expected 100000)");
        System.out.println("ConcurrentHashMap    : " + fillFromTwoThreads(new ConcurrentHashMap<>())
                + " (expected 100000)");
        System.out.println("Notice: both safe maps are correct; ConcurrentHashMap just makes threads wait less.");

        // Step 6: each map call is safe, but "get, then put" is TWO calls, and
        // another thread can sneak in between them.
        step("Step 6: a thread-safe map does NOT make get-then-put safe");
        Map<String, Integer> statusCount = new ConcurrentHashMap<>();
        statusCount.put("SUCCESS", 0);
        Runnable getThenPut = () -> {
            for (int i = 0; i < 50_000; i++) {
                statusCount.put("SUCCESS", statusCount.get("SUCCESS") + 1);
            }
        };
        runTogether(getThenPut, getThenPut);
        System.out.println("get then put : " + statusCount.get("SUCCESS") + " (expected 100000, changes every run)");

        statusCount.put("SUCCESS", 0);
        Runnable merge = () -> {
            for (int i = 0; i < 50_000; i++) {
                statusCount.merge("SUCCESS", 1, Integer::sum);   // one atomic step: add 1 to the old value
            }
        };
        runTogether(merge, merge);
        System.out.println("merge        : " + statusCount.get("SUCCESS") + " (expected 100000)");
        System.out.println("Notice: use one-step methods (merge, compute, putIfAbsent) for check-then-act.");
    }

    // -------------------------------------------------------------------------
    // Counters. Each add() runs a loop of 100,000 increments.
    // -------------------------------------------------------------------------

    /** Not safe: value++ is three steps, so two threads overwrite each other. */
    static class PlainCounter {
        int value;

        void add() {
            for (int i = 0; i < TIMES; i++) {
                value++;
            }
        }
    }

    /** Safe: synchronized lets only one thread inside at a time. */
    static class SyncCounter {
        int value;

        void add() {
            for (int i = 0; i < TIMES; i++) {
                increment();
            }
        }

        synchronized void increment() {
            value++;
        }
    }

    /** Still NOT safe: volatile makes the latest value visible, but value++ is still three steps. */
    static class VolatileCounter {
        volatile int value;

        void add() {
            for (int i = 0; i < TIMES; i++) {
                value++;
            }
        }
    }

    // -------------------------------------------------------------------------
    // Step 3, part 2: a stop flag with and without volatile
    // -------------------------------------------------------------------------
    static boolean plainStop = false;             // NOT volatile
    static volatile boolean volatileStop = false; // volatile

    static void stopFlagDemo() throws InterruptedException {
        Thread plainWorker = new Thread(() -> {
            while (!plainStop) {
                // keep checking; may keep seeing an old cached "false" forever
            }
        });
        Thread volatileWorker = new Thread(() -> {
            while (!volatileStop) {
                // keep checking; volatile means it always reads the latest value
            }
        });
        plainWorker.setDaemon(true);              // lets the program end even if this thread never stops
        volatileWorker.setDaemon(true);
        plainWorker.start();
        volatileWorker.start();

        Thread.sleep(500);                        // let both loops run for a while
        plainStop = true;
        volatileStop = true;
        plainWorker.join(1000);                   // wait up to 1 second for each to stop
        volatileWorker.join(1000);

        System.out.println("stop flag without volatile: "
                + (plainWorker.isAlive() ? "still running 1 second later (never saw stop = true)" : "stopped"));
        System.out.println("stop flag with volatile   : "
                + (volatileWorker.isAlive() ? "still running" : "stopped"));
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /** Two threads put 50,000 different keys each into the same map. Returns the final size. */
    static int fillFromTwoThreads(Map<Integer, Integer> map) throws InterruptedException {
        AtomicBoolean crashed = new AtomicBoolean(false);
        Runnable firstHalf = () -> putRange(map, 0, 50_000, crashed);
        Runnable secondHalf = () -> putRange(map, 50_000, 100_000, crashed);
        runTogether(firstHalf, secondHalf);
        return crashed.get() ? -1 : map.size();   // -1 means a thread crashed
    }

    static void putRange(Map<Integer, Integer> map, int from, int to, AtomicBoolean crashed) {
        try {
            for (int key = from; key < to; key++) {
                map.put(key, key);
            }
        } catch (RuntimeException e) {            // an unsafe HashMap can even throw while resizing
            crashed.set(true);
        }
    }

    /** Runs two jobs at the same time in two threads and waits for both to finish. */
    static void runTogether(Runnable first, Runnable second) throws InterruptedException {
        Thread t1 = new Thread(first);
        Thread t2 = new Thread(second);
        t1.start();
        t2.start();
        t1.join();
        t2.join();
    }

    static void step(String text) {
        System.out.println();
        System.out.println("=== " + text + " ===");
    }
}
