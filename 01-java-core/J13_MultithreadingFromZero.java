import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/*
 * J13  Multithreading from zero: a runnable demo
 *
 * THE STORY (why each piece exists)
 *   One thread does one thing at a time (900 ms)   -> threads                 (Java 1.0)
 *   Threads share data and spoil it                -> synchronized, volatile  (Java 1.0)
 *   Locks can deadlock, new Thread() is costly,
 *   wait/notify is easy to get wrong               -> java.util.concurrent: pools, atomics,
 *                                                     BlockingQueue, latches   (Java 5)
 *   OS threads are heavy                           -> virtual threads         (Java 21, see J11)
 *
 * WHAT YOU WILL SEE (the numbers match J13_MultithreadingFromZero.md)
 *   Step 1   3 biller calls: about 900 ms on 1 thread, about 300 ms on 3 threads
 *   Step 2   three ways to create a thread, and a Callable that returns Rs 450
 *   Step 3   run() runs on "main" (no new thread); start() runs on a new thread;
 *            calling start() twice throws IllegalThreadStateException
 *   Step 4   a thread's life: NEW, RUNNABLE, TIMED_WAITING, BLOCKED, WAITING, TERMINATED
 *   Step 5   sleep() keeps the lock (the other thread waits about 250 ms),
 *            wait() gives it back (the other thread waits about 0 ms)
 *   Step 6   danger 1, the race: 2 threads x 100,000 count++ lose updates;
 *            synchronized and AtomicInteger give exactly 200,000
 *   Step 7   danger 2, visibility: a stop flag without volatile is never seen;
 *            interrupt() is the built-in way to ask a thread to stop
 *   Step 8   danger 3, deadlock: two transfers lock the accounts in opposite order
 *            and freeze; locking in the same order fixes it (Rahul 800, Priya 700)
 *   Step 9   producer-consumer with a BlockingQueue that holds only 2
 *   Step 10  CountDownLatch waits for 3 billers; Semaphore lets only 2 calls in at once
 *   Step 11  ThreadLocal: each thread sees its own value, and the leak in a pool
 *   Step 12  the classic coding question: two threads print 1 to 10 in turn
 *
 * Timings and race results change a little on every run, so yours will differ.
 *
 * HOW TO RUN   java 01-java-core/J13_MultithreadingFromZero.java   (or click "Run" above main)
 * READ FIRST   J13_MultithreadingFromZero.md
 */
public class J13_MultithreadingFromZero {

    static final String[] BILLERS = {"ELECTRICITY", "WATER", "GAS"};

    public static void main(String[] args) throws Exception {
        // Step 1: one worker makes the 3 biller calls one after another.
        // Three workers make them at the same time, so the waiting overlaps.
        step("Step 1: one worker vs three workers (3 biller calls of 300 ms each)");
        oneVersusThreeWorkers();

        // Step 2: a thread needs a JOB (the code to run). Three ways to give it one,
        // plus Callable when you need a result back.
        step("Step 2: three ways to create a thread, and one way to get a result back");
        threeWaysToCreate();

        // Step 3: the most common beginner mistake. run() is a normal method call.
        step("Step 3: start() vs run()");
        startVersusRun();

        // Step 4: we catch real threads in each of their 6 states.
        step("Step 4: the life of a thread (its 6 states)");
        threadStates();

        // Step 5: both pause a thread, but only one of them lets go of the lock.
        step("Step 5: sleep() keeps the lock, wait() gives it back");
        sleepVersusWait();

        // Step 6: the first danger of sharing: updates get lost.
        step("Step 6: danger 1, the race (two threads, one counter)");
        raceCondition();

        // Step 7: the second danger: a thread may never see another thread's change.
        step("Step 7: danger 2, visibility, and how to stop a thread properly");
        visibilityAndStopping();

        // Step 8: the third danger: two threads wait for each other forever.
        step("Step 8: danger 3, deadlock (two transfers wait for each other)");
        deadlock();

        // Step 9: one thread produces work, another consumes it. The queue does the waiting.
        step("Step 9: handing work over with a BlockingQueue (producer-consumer)");
        producerConsumer();

        // Step 10: two ready-made helpers from Java 5.
        step("Step 10: CountDownLatch (wait for 3) and Semaphore (only 2 at a time)");
        latchAndSemaphore();

        // Step 11: a variable where every thread has its own copy. Spring uses this a lot (B13).
        step("Step 11: ThreadLocal, each thread's own copy");
        threadLocal();

        // Step 12: asked in many interviews: make two threads take turns.
        step("Step 12: the classic coding question, two threads print 1 to 10 in turn");
        oddEvenPrinter();
    }

    // -------------------------------------------------------------------------
    // Step 1
    // -------------------------------------------------------------------------

    /** Pretends to call a biller's API. The call takes 300 ms, like a real HTTP call. */
    static int fetchBill(String biller) {
        sleep(300);
        return switch (biller) {                        // a switch expression (Java 14, see J11)
            case "ELECTRICITY" -> 1200;
            case "WATER" -> 450;
            default -> 300;                             // GAS
        };
    }

    static void oneVersusThreeWorkers() throws InterruptedException {
        System.out.println("CPU cores on this machine: " + Runtime.getRuntime().availableProcessors());

        long start = System.nanoTime();
        int total = 0;
        for (String biller : BILLERS) {
            total += fetchBill(biller);                 // main waits for each call, one after another
        }
        System.out.println("1 thread : Rs " + total + " in " + msSince(start) + " ms (300 + 300 + 300)");

        start = System.nanoTime();
        int[] amounts = new int[3];                     // each worker writes only its own slot
        Thread[] workers = new Thread[3];
        for (int i = 0; i < 3; i++) {
            int slot = i;
            String biller = BILLERS[i];
            workers[i] = new Thread(() -> amounts[slot] = fetchBill(biller), "worker-" + biller);
            workers[i].start();                         // start(): the job now runs on its own thread
        }
        for (Thread worker : workers) {
            worker.join();                              // join(): main waits here until that worker is done
        }
        System.out.println("3 threads: Rs " + (amounts[0] + amounts[1] + amounts[2]) + " in " + msSince(start)
                + " ms (the three waits happened together)");
        System.out.println("Notice: each call still took 300 ms. The WAITING happened at the same time.");
    }

    // -------------------------------------------------------------------------
    // Step 2
    // -------------------------------------------------------------------------

    /** Way 1: extend Thread. It works, but now this class can't extend anything else. */
    static class ReminderThread extends Thread {
        ReminderThread() {
            super("way-1-thread");
        }

        @Override
        public void run() {
            System.out.println("  way 1, extends Thread      -> runs on " + Thread.currentThread().getName());
        }
    }

    /** Way 2: implement Runnable. The JOB is separate from the WORKER, so any thread or pool can run it. */
    static class ReminderJob implements Runnable {
        @Override
        public void run() {
            System.out.println("  way 2, implements Runnable -> runs on " + Thread.currentThread().getName());
        }
    }

    static void threeWaysToCreate() throws Exception {
        Thread way1 = new ReminderThread();
        way1.start();
        way1.join();

        Thread way2 = new Thread(new ReminderJob(), "way-2-runnable");
        way2.start();
        way2.join();

        // Way 3: Runnable has only one method, so a lambda can be the job (Java 8).
        Thread way3 = new Thread(() -> System.out.println("  way 3, a lambda             -> runs on "
                + Thread.currentThread().getName()), "way-3-lambda");
        way3.start();
        way3.join();

        // A Runnable returns nothing. A Callable returns a value, and a pool runs it (J06).
        ExecutorService pool = Executors.newFixedThreadPool(1);
        Callable<Integer> fetchWater = () -> fetchBill("WATER");
        Future<Integer> future = pool.submit(fetchWater);   // returns at once, with a "token" for the result
        System.out.println("  Callable + pool            -> WATER bill Rs " + future.get()
                + " (get() waited for the answer)");
        pool.shutdown();
        System.out.println("Notice: real code rarely says new Thread(). It gives jobs to a pool (J06) or to @Async (B13).");
    }

    // -------------------------------------------------------------------------
    // Step 3
    // -------------------------------------------------------------------------

    static void startVersusRun() throws InterruptedException {
        Runnable job = () -> System.out.println("  the job runs on: " + Thread.currentThread().getName());
        Thread worker = new Thread(job, "worker-1");

        System.out.println("worker.run()   = a normal method call, no new thread:");
        worker.run();
        System.out.println("worker.start() = Java makes a new thread, and THAT thread calls run():");
        worker.start();
        worker.join();
        try {
            worker.start();                             // a finished thread can't be started again
        } catch (IllegalThreadStateException e) {
            System.out.println("worker.start() again -> IllegalThreadStateException (a thread runs only once)");
        }
        System.out.println("Notice: run() printed 'main', so no second thread was used. Always call start().");
    }

    // -------------------------------------------------------------------------
    // Step 4
    // -------------------------------------------------------------------------

    static void threadStates() throws InterruptedException {
        // NEW -> RUNNABLE -> TERMINATED, with a thread that keeps the CPU busy for 200 ms
        Thread busy = new Thread(() -> {
            long until = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(200);
            while (System.nanoTime() < until) {
                // working on the CPU
            }
        }, "busy");
        System.out.println("NEW           created, start() not called yet  -> " + busy.getState());
        busy.start();
        System.out.println("RUNNABLE      running, or ready to run         -> " + waitForState(busy, Thread.State.RUNNABLE));
        busy.join();
        System.out.println("TERMINATED    run() has finished               -> " + busy.getState());

        // TIMED_WAITING: paused for a fixed time
        Thread sleeper = new Thread(() -> sleep(300), "sleeper");
        sleeper.start();
        System.out.println("TIMED_WAITING inside sleep(300)                -> "
                + waitForState(sleeper, Thread.State.TIMED_WAITING));
        sleeper.join();

        // BLOCKED: waiting to get a lock that another thread holds
        Object lock = new Object();
        Thread blocked = new Thread(() -> {
            synchronized (lock) {
                // got the lock at last, nothing else to do
            }
        }, "blocked");
        synchronized (lock) {                           // main holds the lock...
            blocked.start();                            // ...so this thread has to wait for it
            System.out.println("BLOCKED       waiting for a lock main holds    -> "
                    + waitForState(blocked, Thread.State.BLOCKED));
        }
        blocked.join();

        // WAITING: waiting with no time limit, until another thread gives a signal
        CountDownLatch go = new CountDownLatch(1);
        Thread waiter = new Thread(() -> awaitQuietly(go), "waiter");
        waiter.start();
        System.out.println("WAITING       waiting for a signal, no limit   -> "
                + waitForState(waiter, Thread.State.WAITING));
        go.countDown();                                 // the signal: the waiter can go now
        waiter.join();
        System.out.println("Notice: BLOCKED = waiting for a LOCK. WAITING = waiting for ANOTHER THREAD to act.");
    }

    // -------------------------------------------------------------------------
    // Step 5
    // -------------------------------------------------------------------------

    static void sleepVersusWait() throws InterruptedException {
        Object lock = new Object();

        // Thread A takes the lock, then SLEEPS for 300 ms. Thread B wants the same lock.
        Thread sleepsWithLock = new Thread(() -> {
            synchronized (lock) {
                sleep(300);                             // sleep() keeps the lock the whole time
            }
        }, "A-sleeps");
        System.out.println("A sleep()s holding the lock -> B waited " + msForSecondThreadToGetLock(sleepsWithLock, lock)
                + " ms for the lock");

        // Thread A takes the lock, then WAITS (up to 300 ms). Thread B wants the same lock.
        Thread waitsWithLock = new Thread(() -> {
            synchronized (lock) {
                try {
                    lock.wait(300);                     // wait() hands the lock back while it waits
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }, "A-waits");
        System.out.println("A wait()s holding the lock  -> B waited " + msForSecondThreadToGetLock(waitsWithLock, lock)
                + " ms for the lock");
        System.out.println("Notice: sleep() keeps the lock. wait() lets it go, so another thread can work meanwhile.");
    }

    /** Starts A, lets it take the lock, then measures how long thread B needs to get the same lock. */
    static long msForSecondThreadToGetLock(Thread a, Object lock) throws InterruptedException {
        a.start();
        sleep(50);                                      // give A time to take the lock first
        long[] waited = new long[1];
        Thread b = new Thread(() -> {
            long start = System.nanoTime();
            synchronized (lock) {
                waited[0] = msSince(start);
            }
        }, "B");
        b.start();
        b.join();
        a.join();
        return waited[0];
    }

    // -------------------------------------------------------------------------
    // Step 6
    // -------------------------------------------------------------------------

    /** NOT safe: value++ is three steps (read, add, write), so two threads overwrite each other. */
    static class PlainCounter {
        int value;

        void addMany() {
            for (int i = 0; i < 100_000; i++) {
                value++;
            }
        }
    }

    /** Safe: synchronized lets only one thread into increment() at a time. */
    static class SyncCounter {
        int value;

        void addMany() {
            for (int i = 0; i < 100_000; i++) {
                increment();
            }
        }

        synchronized void increment() {
            value++;
        }
    }

    static void raceCondition() throws InterruptedException {
        PlainCounter plain = new PlainCounter();
        runTogether(plain::addMany, plain::addMany);
        System.out.println("plain int     : " + plain.value + " (should be 200000; changes every run)");

        SyncCounter sync = new SyncCounter();
        runTogether(sync::addMany, sync::addMany);
        System.out.println("synchronized  : " + sync.value);

        AtomicInteger atomic = new AtomicInteger();
        Runnable addAtomic = () -> {
            for (int i = 0; i < 100_000; i++) {
                atomic.incrementAndGet();               // one safe step, no lock (compare-and-set, J05)
            }
        };
        runTogether(addAtomic, addAtomic);
        System.out.println("AtomicInteger : " + atomic.get());
        System.out.println("Notice: no error was thrown. The number was just silently wrong. That's what makes races dangerous.");
    }

    // -------------------------------------------------------------------------
    // Step 7
    // -------------------------------------------------------------------------

    static boolean plainStop = false;                   // NOT volatile
    static volatile boolean volatileStop = false;       // volatile

    static void visibilityAndStopping() throws InterruptedException {
        Thread plainWorker = new Thread(() -> {
            while (!plainStop) {
                // keeps checking, but may keep reading an old cached "false" forever
            }
        }, "plain-flag");
        Thread volatileWorker = new Thread(() -> {
            while (!volatileStop) {
                // keeps checking; volatile always reads the latest value
            }
        }, "volatile-flag");
        plainWorker.setDaemon(true);                    // daemon: it won't keep the program alive at the end
        volatileWorker.setDaemon(true);
        plainWorker.start();
        volatileWorker.start();

        sleep(500);                                     // let both loops run for a while
        plainStop = true;
        volatileStop = true;
        plainWorker.join(1000);                         // wait up to 1 second for each one to stop
        volatileWorker.join(1000);
        System.out.println("flag without volatile: "
                + (plainWorker.isAlive() ? "still running 1 second later (it never saw stop = true)" : "stopped"));
        System.out.println("flag with volatile   : " + (volatileWorker.isAlive() ? "still running" : "stopped"));

        // Java's built-in "please stop" signal is interrupt(). The thread checks it and leaves cleanly.
        AtomicInteger checks = new AtomicInteger();
        Thread poller = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                checks.incrementAndGet();               // e.g. "check the PENDING payments"
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt(); // sleep() cleared the flag, so set it again, then leave
                }
            }
        }, "status-poller");
        poller.start();
        sleep(350);
        poller.interrupt();                             // a polite request; the thread decides how to stop
        poller.join();
        System.out.println("interrupt()          : the poller stopped cleanly after " + checks.get() + " checks");
    }

    // -------------------------------------------------------------------------
    // Step 8
    // -------------------------------------------------------------------------

    static class Account {
        final int id;
        final String owner;
        int balance;

        Account(int id, String owner, int balance) {
            this.id = id;
            this.owner = owner;
            this.balance = balance;
        }
    }

    /** A MISTAKE on purpose: it locks "from" first, then "to". Two opposite transfers can freeze. */
    static void transferWrongOrder(Account from, Account to, int amount) {
        synchronized (from) {
            sleep(100);                                 // gives the other transfer time to lock ITS first account
            synchronized (to) {
                from.balance -= amount;
                to.balance += amount;
            }
        }
    }

    /** The fix: always lock the account with the smaller id first, whichever way the money moves. */
    static void transferSameOrder(Account from, Account to, int amount) {
        Account first = from.id < to.id ? from : to;
        Account second = from.id < to.id ? to : from;
        synchronized (first) {
            sleep(100);
            synchronized (second) {
                from.balance -= amount;
                to.balance += amount;
            }
        }
    }

    static void deadlock() throws InterruptedException {
        Account rahul = new Account(1, "Rahul", 1000);
        Account priya = new Account(2, "Priya", 500);
        Thread t1 = new Thread(() -> transferWrongOrder(rahul, priya, 300), "transfer-1");
        Thread t2 = new Thread(() -> transferWrongOrder(priya, rahul, 100), "transfer-2");
        t1.setDaemon(true);                             // ONLY so this demo can still end. A real deadlock
        t2.setDaemon(true);                             // keeps these threads stuck until the app restarts.
        t1.start();
        t2.start();
        t1.join(1000);
        t2.join(1000);
        System.out.println("1 second later: transfer-1 alive = " + t1.isAlive() + ", transfer-2 alive = " + t2.isAlive());

        // The same check that "jstack" does: ask the JVM which threads are stuck in a deadlock.
        ThreadMXBean jvmThreads = ManagementFactory.getThreadMXBean();
        long[] stuck = jvmThreads.findDeadlockedThreads();
        if (stuck != null) {
            for (ThreadInfo info : jvmThreads.getThreadInfo(stuck)) {
                String wanted = info.getLockInfo().getIdentityHashCode() == System.identityHashCode(rahul)
                        ? rahul.owner : priya.owner;
                System.out.println("DEADLOCK: " + info.getThreadName() + " waits for " + wanted
                        + "'s account, which " + info.getLockOwnerName() + " holds");
            }
        }

        Account rahul2 = new Account(1, "Rahul", 1000);
        Account priya2 = new Account(2, "Priya", 500);
        Thread s1 = new Thread(() -> transferSameOrder(rahul2, priya2, 300), "fixed-transfer-1");
        Thread s2 = new Thread(() -> transferSameOrder(priya2, rahul2, 100), "fixed-transfer-2");
        s1.start();
        s2.start();
        s1.join();
        s2.join();
        System.out.println("same lock order: both finished. Rahul " + rahul2.balance + ", Priya " + priya2.balance
                + " (total " + (rahul2.balance + priya2.balance) + ", nothing lost)");
    }

    // -------------------------------------------------------------------------
    // Step 9
    // -------------------------------------------------------------------------

    static void producerConsumer() throws InterruptedException {
        BlockingQueue<String> callbacks = new ArrayBlockingQueue<>(2);   // room for only 2 callbacks
        long start = System.nanoTime();

        Thread producer = new Thread(() -> {
            for (int i = 1; i <= 5; i++) {
                String callback = "CB" + i;
                long before = System.nanoTime();
                putQuietly(callbacks, callback);        // put() WAITS while the queue is full
                long waited = msSince(before);
                System.out.printf("[%3d ms] producer put  %s%s%n", msSince(start), callback,
                        waited > 20 ? "   (waited " + waited + " ms: the queue was full)" : "");
            }
        }, "producer");

        Thread consumer = new Thread(() -> {
            sleep(250);                                 // starts late on purpose, so the queue fills up first
            for (int i = 1; i <= 5; i++) {
                String callback = takeQuietly(callbacks);   // take() WAITS while the queue is empty
                System.out.printf("[%3d ms] consumer took %s%n", msSince(start), callback);
                sleep(100);                             // processing one callback takes 100 ms
            }
        }, "consumer");

        producer.start();
        consumer.start();
        producer.join();
        consumer.join();
        System.out.println("Notice: nobody called wait() or notify(). The queue did all the waiting for both sides.");
    }

    // -------------------------------------------------------------------------
    // Step 10
    // -------------------------------------------------------------------------

    static void latchAndSemaphore() throws InterruptedException {
        // CountDownLatch: "wait until 3 things have happened"
        CountDownLatch allAnswered = new CountDownLatch(3);
        ExecutorService threePool = Executors.newFixedThreadPool(3);
        long start = System.nanoTime();
        for (String biller : BILLERS) {
            threePool.submit(() -> {
                fetchBill(biller);
                allAnswered.countDown();                // 3 -> 2 -> 1 -> 0
            });
        }
        allAnswered.await();                            // main waits here until the count reaches 0
        System.out.println("CountDownLatch: all 3 billers answered after " + msSince(start) + " ms");
        threePool.shutdown();

        // Semaphore: "at most 2 at a time". Say the biller allows only 2 calls at once.
        Semaphore twoAtATime = new Semaphore(2);
        AtomicInteger inside = new AtomicInteger();
        AtomicInteger mostInside = new AtomicInteger();
        CountDownLatch finished = new CountDownLatch(5);
        ExecutorService fivePool = Executors.newFixedThreadPool(5);
        start = System.nanoTime();
        for (int i = 0; i < 5; i++) {
            fivePool.submit(() -> {
                callBillerWithPermit(twoAtATime, inside, mostInside);
                finished.countDown();
            });
        }
        finished.await();
        System.out.println("Semaphore(2)  : 5 calls on 5 threads, but at most " + mostInside.get()
                + " inside at once, so " + msSince(start) + " ms (3 rounds: 2 + 2 + 1)");
        fivePool.shutdown();
    }

    /** Takes a permit, calls the biller, and ALWAYS gives the permit back, even if the call fails. */
    static void callBillerWithPermit(Semaphore permits, AtomicInteger inside, AtomicInteger mostInside) {
        try {
            permits.acquire();                          // waits here if 2 calls are already inside
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;                                     // no permit taken, so there's none to give back
        }
        try {
            mostInside.accumulateAndGet(inside.incrementAndGet(), Math::max);   // remember the most inside at once
            fetchBill("WATER");
        } finally {
            inside.decrementAndGet();
            permits.release();                          // in finally: the permit comes back even on an error
        }
    }

    // -------------------------------------------------------------------------
    // Step 11
    // -------------------------------------------------------------------------

    static final ThreadLocal<String> CURRENT_USER = new ThreadLocal<>();

    static void handleRequest(String user) {
        CURRENT_USER.set(user);
        try {
            sleep(100);                                 // both requests are running at the same time
            System.out.println(Thread.currentThread().getName() + " sees : " + CURRENT_USER.get());
        } finally {
            CURRENT_USER.remove();                      // clean up, because pool threads get reused
        }
    }

    static void threadLocal() throws Exception {
        Thread r1 = new Thread(() -> handleRequest("Rahul"), "request-1");
        Thread r2 = new Thread(() -> handleRequest("Priya"), "request-2");
        r1.start();
        r2.start();
        r1.join();
        r2.join();
        System.out.println("main sees      : " + CURRENT_USER.get() + " (main never set it)");

        // The leak: a pool REUSES its threads. Forget remove(), and the next job sees the old value.
        ExecutorService onePool = Executors.newSingleThreadExecutor();
        onePool.submit(() -> CURRENT_USER.set("Rahul")).get();              // job 1 sets it and forgets remove()
        String seen = onePool.submit(() -> CURRENT_USER.get()).get();       // job 2 is someone else's request
        System.out.println("pool, no remove: the next job on the same thread sees '" + seen
                + "' (another user's data!)");
        onePool.shutdown();
    }

    // -------------------------------------------------------------------------
    // Step 12
    // -------------------------------------------------------------------------

    /** Two threads take turns: "odd" prints 1, 3, 5 ... and "even" prints 2, 4, 6 ... */
    static class TakeTurns {
        private int next = 1;
        private final StringBuilder printed = new StringBuilder();

        synchronized void printMyNumbers(boolean iPrintOdd) {
            while (next <= 10) {
                boolean myTurn = (next % 2 == 1) == iPrintOdd;
                if (!myTurn) {
                    try {
                        wait();                         // not my turn: give up the lock, sleep until notified
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                    continue;                           // woke up: check again (always re-check in a loop)
                }
                printed.append(Thread.currentThread().getName()).append(' ').append(next).append("  ");
                next++;
                notifyAll();                            // wake the other thread: it's their turn now
            }
        }
    }

    static void oddEvenPrinter() throws InterruptedException {
        TakeTurns turns = new TakeTurns();
        Thread odd = new Thread(() -> turns.printMyNumbers(true), "odd");
        Thread even = new Thread(() -> turns.printMyNumbers(false), "even");
        even.start();                                   // "even" starts first on purpose: it must still wait for 1
        odd.start();
        odd.join();
        even.join();
        System.out.println(turns.printed.toString().trim());
        System.out.println("Notice: wait() inside a while loop, notifyAll() after each number, all inside synchronized.");
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /** Thread.sleep without the checked exception. If interrupted, it keeps the "please stop" flag set. */
    static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    static long msSince(long startNanos) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
    }

    /** Checks the thread's state every 5 ms, for up to 1 second, until it reaches the wanted state. */
    static Thread.State waitForState(Thread thread, Thread.State wanted) {
        for (int i = 0; i < 200 && thread.getState() != wanted; i++) {
            sleep(5);
        }
        return thread.getState();
    }

    /** Runs two jobs at the same time on two threads, and waits for both to finish. */
    static void runTogether(Runnable first, Runnable second) throws InterruptedException {
        Thread t1 = new Thread(first);
        Thread t2 = new Thread(second);
        t1.start();
        t2.start();
        t1.join();
        t2.join();
    }

    static void putQuietly(BlockingQueue<String> queue, String item) {
        try {
            queue.put(item);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    static String takeQuietly(BlockingQueue<String> queue) {
        try {
            return queue.take();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    static void awaitQuietly(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    static void step(String text) {
        System.out.println();
        System.out.println("=== " + text + " ===");
    }
}
