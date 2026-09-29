import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;

/*
 * B13  Multithreading in Spring Boot: a runnable demo that needs NO Spring
 *
 * This file copies, in plain Java, what Tomcat and Spring do with your code,
 * so you can watch it happen. The thread names match the real ones. The real
 * Spring code (annotations, beans, properties) is in the .md.
 *
 * THE STORY (why each piece exists)
 *   One request at a time is too slow          -> Tomcat gives every request its own thread (200 max)
 *   All those threads share ONE bean object     -> keep beans stateless: request data in local variables
 *   Slow side work makes the user wait          -> @Async hands it to another thread       (Spring 3.0)
 *   Default pools, lost context, many servers   -> your own ThreadPoolTaskExecutor, a TaskDecorator,
 *                                                  and locks in the DATABASE, not synchronized
 *
 * WHAT YOU WILL SEE (the numbers match B13_MultithreadingInSpringBoot.md)
 *   Step 1   3 requests answered in about 300 ms on threads http-nio-8080-exec-1, -2, -3
 *   Step 2   a bean with a field puts Priya's name on Rahul's receipt; a stateless bean doesn't
 *   Step 4   @Async: the user waits about 50 ms instead of about 450 ms for the SMS
 *   Step 5   a pool with core 2, queue 2, max 4 gets 8 jobs: 2 run, 2 wait, 2 get extra threads,
 *            2 are rejected; CallerRunsPolicy makes the caller do job 7 itself
 *   Step 7   the trace ID is null on the @Async thread; a TaskDecorator copies it across
 *   Step 8   @Scheduled with 1 thread: a slow job makes another job 500 ms late; 2 threads fix it
 *   Step 10  synchronized can't stop a double debit across 2 servers (Rs 500, should be Rs 400);
 *            a version check in the database can (Rs 400, one retry)
 *
 * Timings change a little on every run, so yours will differ by a few ms.
 *
 * HOW TO RUN   java 05-spring-boot/B13_MultithreadingInSpringBoot.java   (or click "Run" above main)
 * READ FIRST   J13_MultithreadingFromZero.md, then B13_MultithreadingInSpringBoot.md
 */
public class B13_MultithreadingInSpringBoot {

    public static void main(String[] args) throws Exception {
        // Step 1: Tomcat runs every HTTP request on a thread from its pool.
        // Your controller code is multithreaded even if you never write "new Thread()".
        step("Step 1: your app is already multithreaded (Tomcat's request threads)");
        requestThreads();

        // Step 2: Spring makes ONE object per bean (singleton). All request threads share it.
        // A field in that bean is shared by every user at the same time.
        step("Step 2: one bean, many threads (a field mixes up two users)");
        singletonBeanWithField();

        // Step 4: @Async hands slow side work (an SMS) to another thread,
        // so the user gets the answer without waiting for it.
        step("Step 4: @Async, send the SMS without making the user wait");
        asyncSideWork();

        // Step 5: the 4 settings of a thread pool, and what happens when it's full.
        step("Step 5: your own pool: core 2, queue 2, max 4, and 8 jobs");
        poolSettings();

        // Step 7: the trace ID, the logged-in user and the transaction live in ThreadLocals.
        // A job on another thread starts with none of them.
        step("Step 7: ThreadLocal context doesn't follow the job to another thread");
        contextIsLost();

        // Step 8: @Scheduled jobs share a scheduler pool, and Spring Boot's default has 1 thread.
        step("Step 8: @Scheduled with 1 thread vs 2 threads");
        scheduledJobs();

        // Step 10: production runs 2 or more copies of your app. synchronized only works inside one.
        step("Step 10: synchronized can't stop a double debit across 2 servers");
        twoServers();
    }

    // -------------------------------------------------------------------------
    // Step 1
    // -------------------------------------------------------------------------

    static void requestThreads() throws Exception {
        ExecutorService tomcat = Executors.newFixedThreadPool(3, named("http-nio-8080-exec-"));   // Tomcat: 200 by default
        String[] requests = {"GET /bills/ELECTRICITY", "GET /bills/WATER", "GET /bills/GAS"};
        long start = System.nanoTime();
        List<Future<String>> answers = new ArrayList<>();
        for (String request : requests) {
            answers.add(tomcat.submit(() -> {
                sleep(300);                             // the controller calls the biller: 300 ms
                return request + " -> handled by " + Thread.currentThread().getName();
            }));
        }
        for (Future<String> answer : answers) {
            System.out.println(answer.get());
        }
        System.out.println("3 requests answered in " + msSince(start) + " ms, not 900: each one had its own thread");
        tomcat.shutdown();
        System.out.println("Notice: nobody wrote new Thread(). Tomcat did it for every request.");
    }

    // -------------------------------------------------------------------------
    // Step 2
    // -------------------------------------------------------------------------

    /** A MISTAKE on purpose: a singleton bean that keeps request data in a FIELD. */
    static class ReceiptServiceWithField {
        private String currentUser;                     // ONE field, shared by every request thread

        String payAndBuildReceipt(String user, int amount) {
            currentUser = user;                         // Rahul's request writes "Rahul" here...
            sleep(100);                                 // ...saves the payment (a pretend DB call)...
            return "Receipt for " + currentUser + ": Rs " + amount;   // ...but Priya's request wrote "Priya" meanwhile
        }
    }

    /** The fix: no fields for request data. The parameter lives on each thread's own stack (J09). */
    static class ReceiptServiceStateless {
        String payAndBuildReceipt(String user, int amount) {
            sleep(100);
            return "Receipt for " + user + ": Rs " + amount;
        }
    }

    static void singletonBeanWithField() throws Exception {
        ReceiptServiceWithField withField = new ReceiptServiceWithField();       // ONE object, like a singleton bean
        System.out.println("A bean WITH a field:");
        twoUsersPayAtOnce(withField::payAndBuildReceipt);

        ReceiptServiceStateless stateless = new ReceiptServiceStateless();
        System.out.println("A STATELESS bean:");
        twoUsersPayAtOnce(stateless::payAndBuildReceipt);
        System.out.println("Notice: same code, same threads. The only difference is WHERE the user's name was kept.");
    }

    static void twoUsersPayAtOnce(BiFunction<String, Integer, String> service) throws Exception {
        ExecutorService tomcat = Executors.newFixedThreadPool(2, named("http-nio-8080-exec-"));
        Future<String> rahul = tomcat.submit(() -> service.apply("Rahul", 500));
        sleep(30);                                      // Priya's request arrives 30 ms later
        Future<String> priya = tomcat.submit(() -> service.apply("Priya", 800));
        printReceipt("Rahul", rahul.get());
        printReceipt("Priya", priya.get());
        tomcat.shutdown();
    }

    static void printReceipt(String payer, String receipt) {
        System.out.println("  " + payer + " gets: " + receipt
                + (receipt.contains(payer) ? "" : "   <- WRONG: another user's name!"));
    }

    // -------------------------------------------------------------------------
    // Step 4
    // -------------------------------------------------------------------------

    /** In Spring: a @Service with an @Async method. Here we do by hand what Spring's @Async proxy does. */
    static class NotificationService {
        private final ExecutorService asyncPool;        // in Spring: the executor behind @Async

        NotificationService(ExecutorService asyncPool) {
            this.asyncPool = asyncPool;
        }

        void sendSms(String user, String txnId) {
            sleep(400);                                 // the SMS gateway is slow
            System.out.println("  SMS for " + txnId + " sent to " + user + " on thread "
                    + Thread.currentThread().getName());
        }

        /** What calling an @Async method does: hand the job to the pool, and return at once. */
        void sendSmsAsync(String user, String txnId) {
            asyncPool.submit(() -> sendSms(user, txnId));
        }
    }

    static void asyncSideWork() throws Exception {
        ExecutorService asyncPool = Executors.newFixedThreadPool(2, named("notify-"));
        NotificationService notifications = new NotificationService(asyncPool);
        ExecutorService tomcat = Executors.newFixedThreadPool(1, named("http-nio-8080-exec-"));

        long waited = tomcat.submit(() -> {
            long start = System.nanoTime();
            sleep(50);                                  // save the payment: 50 ms
            notifications.sendSms("Rahul", "TXN1001");  // WITHOUT @Async: the request waits for the SMS
            return msSince(start);
        }).get();
        System.out.println("without @Async: the user waited " + waited + " ms");

        waited = tomcat.submit(() -> {
            long start = System.nanoTime();
            sleep(50);
            notifications.sendSmsAsync("Rahul", "TXN1002");   // WITH @Async: hand it over, answer at once
            return msSince(start);
        }).get();
        System.out.println("with @Async   : the user waited " + waited + " ms (the SMS is still on its way)");

        asyncPool.shutdown();
        asyncPool.awaitTermination(2, TimeUnit.SECONDS);   // let the SMS finish before the next step
        tomcat.shutdown();
        System.out.println("Notice: the SMS still took 400 ms. It just happened on 'notify-1', not on the user's request.");
    }

    // -------------------------------------------------------------------------
    // Step 5
    // -------------------------------------------------------------------------

    static void poolSettings() {
        // try-with-resources on a pool (Java 19+): at the end, close() shuts it down and waits for its jobs.
        try (ThreadPoolExecutor pool = new ThreadPoolExecutor(
                2, 4,                                   // core: 2 threads always, max: 4 threads
                60, TimeUnit.SECONDS,                   // an extra thread stops after 60 s with no work
                new ArrayBlockingQueue<>(2),            // the queue holds 2 waiting jobs
                named("task-"),
                new ThreadPoolExecutor.AbortPolicy())) {   // full? throw (Spring says TaskRejectedException)

            for (int job = 1; job <= 8; job++) {
                int threadsBefore = pool.getPoolSize();
                try {
                    pool.execute(() -> sleep(300));     // every job takes 300 ms
                    int threads = pool.getPoolSize();
                    if (threads > threadsBefore) {
                        System.out.println("job " + job + ": runs at once on thread " + threads
                                + (threads <= 2 ? " (a core thread)" : " (an EXTRA thread, only because the queue was full)"));
                    } else {
                        System.out.println("job " + job + ": waits in the queue (" + pool.getQueue().size() + " of 2)");
                    }
                } catch (RejectedExecutionException e) {
                    System.out.println("job " + job + ": REJECTED (all 4 threads busy and the queue is full)");
                }
            }
        }

        // The same pool, but when it's full, the CALLER runs the job itself.
        try (ThreadPoolExecutor callerRuns = new ThreadPoolExecutor(2, 4, 60, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(2), named("task-"), new ThreadPoolExecutor.CallerRunsPolicy())) {
            for (int job = 1; job <= 6; job++) {
                callerRuns.execute(() -> sleep(300));   // jobs 1 to 6 fill the 4 threads and the queue
            }
            String[] ranOn = new String[1];
            long start = System.nanoTime();
            callerRuns.execute(() -> {
                ranOn[0] = Thread.currentThread().getName();
                sleep(300);
            });
            System.out.println("job 7 with CallerRunsPolicy: ran on '" + ranOn[0] + "', and the caller was busy for "
                    + msSince(start) + " ms");
        }
        System.out.println("Notice: extra threads start only when the QUEUE is full, not when the core threads are busy.");
    }

    // -------------------------------------------------------------------------
    // Step 7
    // -------------------------------------------------------------------------

    /** Like SLF4J's MDC: a request filter puts a trace ID here, and every log line prints it. */
    static final ThreadLocal<String> TRACE_ID = new ThreadLocal<>();

    static void log(String message) {
        System.out.println("  [traceId=" + TRACE_ID.get() + "] " + Thread.currentThread().getName() + ": " + message);
    }

    /** What a Spring TaskDecorator does: copy the caller's context into the job, and clean up after it. */
    static Runnable copyTraceId(Runnable job) {
        String traceId = TRACE_ID.get();                // read on the CALLER's thread (the request)
        return () -> {
            TRACE_ID.set(traceId);                      // set on the WORKER's thread
            try {
                job.run();
            } finally {
                TRACE_ID.remove();                      // pool threads are reused, so always clean up
            }
        };
    }

    static void contextIsLost() throws Exception {
        ExecutorService asyncPool = Executors.newFixedThreadPool(1, named("notify-"));
        ExecutorService tomcat = Executors.newFixedThreadPool(1, named("http-nio-8080-exec-"));
        tomcat.submit(() -> {
            TRACE_ID.set("req-7f3a");                   // a filter sets this at the start of every request
            try {
                log("payment TXN1001 saved");
                asyncPool.submit(() -> log("sending SMS (plain @Async)"));
                asyncPool.submit(copyTraceId(() -> log("sending SMS (@Async with a TaskDecorator)")));
            } finally {
                TRACE_ID.remove();
            }
        }).get();
        asyncPool.shutdown();
        asyncPool.awaitTermination(2, TimeUnit.SECONDS);
        tomcat.shutdown();
        System.out.println("Notice: the same thing happens to the logged-in user and to the @Transactional transaction.");
    }

    // -------------------------------------------------------------------------
    // Step 8
    // -------------------------------------------------------------------------

    static void scheduledJobs() throws InterruptedException {
        System.out.println("1 scheduler thread (the Spring Boot default):");
        runTwoJobs(1);
        System.out.println("2 scheduler threads (spring.task.scheduling.pool.size=2):");
        runTwoJobs(2);
    }

    static void runTwoJobs(int threads) throws InterruptedException {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(threads, named("scheduling-"));
        CountDownLatch bothDone = new CountDownLatch(2);
        long start = System.nanoTime();
        scheduler.schedule(() -> {
            System.out.println("  reconciliation job (takes 600 ms) started at " + msSince(start) + " ms on "
                    + Thread.currentThread().getName());
            sleep(600);
            bothDone.countDown();
        }, 0, TimeUnit.MILLISECONDS);
        scheduler.schedule(() -> {
            long startedAt = msSince(start);
            System.out.println("  status-check job, due at 100 ms, started at " + startedAt + " ms on "
                    + Thread.currentThread().getName()
                    + (startedAt > 300 ? "   <- LATE: the only thread was busy" : "   <- on time"));
            bothDone.countDown();
        }, 100, TimeUnit.MILLISECONDS);
        bothDone.await();
        scheduler.shutdown();
    }

    // -------------------------------------------------------------------------
    // Step 10
    // -------------------------------------------------------------------------

    /** One row of the wallets table. Every server reads and writes this SAME row. */
    static class WalletRow {
        private int balance = 700;
        private int version = 0;

        /** Like: SELECT balance, version FROM wallets WHERE id = 1 */
        synchronized int[] select() {
            return new int[] {balance, version};
        }

        /** Like: UPDATE wallets SET balance = ? WHERE id = 1 (no check at all) */
        synchronized void updateBalance(int newBalance) {
            balance = newBalance;
        }

        /**
         * Like: UPDATE wallets SET balance = ?, version = version + 1 WHERE id = 1 AND version = ?
         * The DATABASE does the check and the write as one step, for every server.
         * Returns the rows updated: 1, or 0 if someone changed the row after we read it.
         */
        synchronized int updateIfVersion(int newBalance, int versionWeRead) {
            if (version != versionWeRead) {
                return 0;
            }
            balance = newBalance;
            version++;
            return 1;
        }
    }

    /** One copy of the app (one JVM, one server). Its synchronized lock exists only inside this server. */
    static class WalletService {
        private final WalletRow row;

        WalletService(WalletRow row) {
            this.row = row;
        }

        synchronized void debitWithSynchronized(int amount) {
            int balance = row.select()[0];              // read the balance
            sleep(100);                                 // the rest of the request's work
            row.updateBalance(balance - amount);        // write the new balance
        }

        /** Optimistic locking, like JPA's @Version: if the version changed, read again and retry. */
        int debitWithVersionCheck(int amount) {
            int retries = 0;
            while (true) {
                int[] read = row.select();
                sleep(100);
                if (row.updateIfVersion(read[0] - amount, read[1]) == 1) {
                    return retries;
                }
                retries++;                              // 0 rows updated: another server was faster
            }
        }
    }

    static void twoServers() throws InterruptedException {
        WalletRow wallet = new WalletRow();                             // ONE database row: Rs 700
        WalletService server1 = new WalletService(wallet);              // two servers, two bean objects,
        WalletService server2 = new WalletService(wallet);              // so two different locks
        runOnTwoServers(() -> server1.debitWithSynchronized(100), () -> server2.debitWithSynchronized(200));
        System.out.println("synchronized on each server : balance Rs " + wallet.select()[0]
                + " (should be Rs 400: the Rs 100 debit was LOST)");

        WalletRow wallet2 = new WalletRow();
        WalletService serverA = new WalletService(wallet2);
        WalletService serverB = new WalletService(wallet2);
        int[] retries = new int[2];
        runOnTwoServers(() -> retries[0] = serverA.debitWithVersionCheck(100),
                () -> retries[1] = serverB.debitWithVersionCheck(200));
        System.out.println("version check in the DB     : balance Rs " + wallet2.select()[0]
                + " (right), retries: server-1 " + retries[0] + ", server-2 " + retries[1]);
        System.out.println("Notice: synchronized protects ONE JVM. With 2 servers, the database must protect the row.");
    }

    /** Runs the two debits at almost the same time: server-2's request arrives 10 ms after server-1's. */
    static void runOnTwoServers(Runnable onServer1, Runnable onServer2) throws InterruptedException {
        Thread s1 = new Thread(onServer1, "server-1");
        Thread s2 = new Thread(onServer2, "server-2");
        s1.start();
        sleep(10);
        s2.start();
        s1.join();
        s2.join();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /** Names the pool's threads like the real ones: http-nio-8080-exec-1, task-1, scheduling-1 ... */
    static ThreadFactory named(String prefix) {
        AtomicInteger count = new AtomicInteger();
        return job -> new Thread(job, prefix + count.incrementAndGet());
    }

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

    static void step(String text) {
        System.out.println();
        System.out.println("=== " + text + " ===");
    }
}
