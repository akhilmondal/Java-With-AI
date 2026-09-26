import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/*
 * J06  ExecutorService, Future, CompletableFuture: runnable demo
 *
 * Read J06_ExecutorsAndCompletableFuture.md first. This file runs the same
 * steps so you can see them happen. The step numbers match the .md file.
 *
 * Every biller call "takes" 300 ms (we sleep to pretend it's an HTTP call).
 * Times are measured, so yours will be a few ms different.
 *
 * Run it:  java 01-java-core/J06_ExecutorsAndCompletableFuture.java
 *          (or click "Run" above main() in VS Code)
 */
public class J06_ExecutorsAndCompletableFuture {

    static final Map<String, Integer> BILLS = Map.of("ELECTRICITY", 1200, "WATER", 450, "GAS", 300);
    static final List<String> BILLERS = List.of("ELECTRICITY", "WATER", "GAS");

    public static void main(String[] args) throws Exception {
        step("Step 1: one after another");
        long start = System.nanoTime();
        int total = 0;
        for (String biller : BILLERS) {
            total += fetchBill(biller);                  // each call waits 300 ms
        }
        System.out.println("total Rs " + total + " in " + msSince(start) + " ms (3 x 300 = 900)");

        step("Step 2: a thread pool runs them at the same time");
        System.out.println("3 threads: " + fetchAllWithPool(3));  // all three at once: ~300 ms
        System.out.println("2 threads: " + fetchAllWithPool(2));  // two, then one: ~600 ms

        step("Step 3: Future.get() waits; get(timeout) gives up");
        ExecutorService pool = Executors.newFixedThreadPool(3);
        Future<Integer> slow = pool.submit(() -> fetchSlowBill());   // takes 1,000 ms
        try {
            slow.get(200, TimeUnit.MILLISECONDS);        // wait at most 200 ms
        } catch (TimeoutException e) {
            System.out.println("get(200 ms) -> TimeoutException, so we stop waiting");
            slow.cancel(true);                           // and stop the task too
        }

        step("Step 4: CompletableFuture, chain, combine, all");
        // 4a. start, transform, use. Nobody blocks in the middle.
        CompletableFuture
                .supplyAsync(() -> fetchBill("ELECTRICITY"), pool)   // runs in the pool: 1200
                .thenApply(amount -> amount + 10)                     // add the Rs 10 fee: 1210
                .thenAccept(amount -> System.out.println("thenApply + thenAccept: pay Rs " + amount))
                .join();                                              // wait here only so the demo prints in order

        // 4b. two calls at the same time, then combine the two answers
        start = System.nanoTime();
        CompletableFuture<Integer> bill = CompletableFuture.supplyAsync(() -> fetchBill("ELECTRICITY"), pool);
        CompletableFuture<Integer> fee = CompletableFuture.supplyAsync(() -> fetchFee(), pool);
        int billPlusFee = bill.thenCombine(fee, Integer::sum).join();  // 1200 + 10
        System.out.println("thenCombine: Rs " + billPlusFee + " in " + msSince(start) + " ms (both ran together)");

        // 4c. all three billers, then one total
        start = System.nanoTime();
        List<CompletableFuture<Integer>> calls = new ArrayList<>();
        for (String biller : BILLERS) {
            calls.add(CompletableFuture.supplyAsync(() -> fetchBill(biller), pool));
        }
        CompletableFuture.allOf(calls.toArray(new CompletableFuture<?>[0])).join();   // wait for all three
        int sum = 0;
        for (CompletableFuture<Integer> call : calls) {
            sum += call.join();                          // already finished, so no waiting here
        }
        System.out.println("allOf: total Rs " + sum + " in " + msSince(start) + " ms");

        step("Step 5: errors and timeouts");
        int gasBill = CompletableFuture
                .supplyAsync(() -> fetchBillFromDownBiller("GAS"), pool)
                .exceptionally(error -> {
                    // The real error is wrapped inside a CompletionException, so we read its cause.
                    System.out.println("exceptionally: " + error.getCause().getMessage() + " -> use 0");
                    return 0;
                })
                .join();
        System.out.println("gas bill used: Rs " + gasBill);

        int slowBill = CompletableFuture
                .supplyAsync(() -> fetchSlowBill(), pool)          // takes 1,000 ms
                .completeOnTimeout(0, 400, TimeUnit.MILLISECONDS)   // after 400 ms, just use 0
                .join();
        System.out.println("completeOnTimeout: Rs " + slowBill + " (gave up after 400 ms)");

        step("Step 6: thenApply vs thenCompose");
        // thenApply(amount -> payAsync(...)) would give CompletableFuture<CompletableFuture<String>>.
        // thenCompose flattens it, because the next step is itself async.
        String paymentId = CompletableFuture
                .supplyAsync(() -> fetchBill("WATER"), pool)                     // 450
                .thenCompose(amount -> CompletableFuture.supplyAsync(() -> pay("WATER", amount), pool))
                .join();
        System.out.println("thenCompose: " + paymentId);

        step("Step 7: shutdown");
        pool.shutdown();                                 // no new tasks; let running ones finish
        boolean finished = pool.awaitTermination(5, TimeUnit.SECONDS);
        System.out.println("pool finished: " + finished
                + " (without shutdown(), its threads would keep the program running)");
    }

    /** Runs the three bill fetches on a pool of the given size and reports the total and the time. */
    static String fetchAllWithPool(int threads) throws InterruptedException, ExecutionException {
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        long start = System.nanoTime();
        List<Future<Integer>> futures = new ArrayList<>();
        for (String biller : BILLERS) {
            Callable<Integer> task = () -> fetchBill(biller);   // Callable: returns a value
            futures.add(pool.submit(task));                      // returns at once with a Future
        }
        int total = 0;
        for (Future<Integer> future : futures) {
            total += future.get();                               // get() waits until that task is done
        }
        pool.shutdown();
        return "total Rs " + total + " in " + msSince(start) + " ms";
    }

    // -------------------------------------------------------------------------
    // Pretend biller calls
    // -------------------------------------------------------------------------

    static int fetchBill(String biller) {
        sleep(300);                                      // pretend: an HTTP call takes 300 ms
        return BILLS.get(biller);
    }

    static int fetchFee() {
        sleep(300);
        return 10;                                       // Rs 10 convenience fee
    }

    static int fetchSlowBill() {
        sleep(1_000);                                    // a slow biller: 1 second
        return 999;
    }

    static int fetchBillFromDownBiller(String biller) {
        sleep(100);
        throw new IllegalStateException(biller + " biller is down");
    }

    static String pay(String biller, int amount) {
        sleep(300);                                      // pretend: the payment call
        return "PAY-" + biller + "-" + amount;
    }

    static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();          // cancelled: keep the "interrupted" flag and stop
        }
    }

    static long msSince(long start) {
        return (System.nanoTime() - start) / 1_000_000;
    }

    static void step(String text) {
        System.out.println();
        System.out.println("=== " + text + " ===");
    }
}
