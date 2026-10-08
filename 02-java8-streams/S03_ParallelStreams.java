import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.IntStream;

/*
 * S03  Parallel streams: a runnable demo
 *
 * THE STORY (why parallel streams exist, and why they are not a free speed-up)
 *   1 core works, 7 cores sit idle                  -> Fork/Join framework        (Java 7)
 *   Fork/Join needs a 30-line RecursiveTask class   -> parallelStream()           (Java 8)
 *   Threads add to one shared ArrayList, items lost -> collect() / reduce()       (Java 8)
 *   Small lists and waiting work get slower         -> use parallel only for big CPU work;
 *                                                      for waiting work, CompletableFuture
 *                                                      on your own pool (see J06)
 *
 * WHAT YOU WILL SEE (the numbers match S03_ParallelStreams.md)
 *   Step 1  the same stream on main only, then on main + common-pool workers
 *   Step 2  8 lakh risk scores: sequential vs parallel time, same total
 *   Step 3  a small list (100 amounts): parallel is NOT faster
 *   Step 4  the trap: forEach(list::add) loses items; collect() gives exactly 10,000
 *   Step 5  order: forEach jumbles 1..8, forEachOrdered keeps it; findAny may differ
 *   Step 6  the reduce trap: identity 10 gives 25 sequential but a bigger number parallel
 *   Step 7  16 biller calls of 300 ms: parallel stream about 600 ms, own pool of 16 about 300 ms
 *
 * HOW TO RUN   java 02-java8-streams/S03_ParallelStreams.java   (or click "Run" above main)
 * READ FIRST   S03_ParallelStreams.md
 */
public class S03_ParallelStreams {

    public static void main(String[] args) throws Exception {
        int cores = Runtime.getRuntime().availableProcessors();
        System.out.println("CPU cores on this machine      : " + cores);
        System.out.println("common pool parallelism        : " + ForkJoinPool.commonPool().getParallelism()
                + "   (cores - 1, because main also helps)");

        // Step 1: a parallel stream runs on the shared ForkJoinPool (the "common pool").
        // The thread that calls the stream (main) also does part of the work.
        step("Step 1: which threads do the work?");
        System.out.println("sequential uses : " + threadsUsed(false));
        System.out.println("parallel uses   : " + threadsUsed(true) + "   (yours may differ)");
        System.out.println("Notice: one word, .parallel(), and the work is spread over many threads.");

        // Step 2: heavy CPU work on a big list. This is where parallel helps.
        // We warm up first, so the JIT (Java's run-time compiler) does not spoil the timing.
        step("Step 2: 8 lakh payments, a heavy risk score for each");
        int[] amounts = IntStream.rangeClosed(1, 800_000).toArray();
        riskTotal(amounts, false);
        riskTotal(amounts, true);
        long t0 = System.nanoTime();
        long seqTotal = riskTotal(amounts, false);
        long seqMs = (System.nanoTime() - t0) / 1_000_000;
        t0 = System.nanoTime();
        long parTotal = riskTotal(amounts, true);
        long parMs = (System.nanoTime() - t0) / 1_000_000;
        System.out.println("sequential : total " + seqTotal + " in " + seqMs + " ms");
        System.out.println("parallel   : total " + parTotal + " in " + parMs + " ms   (yours may differ)");
        System.out.println("same total : " + (seqTotal == parTotal));
        System.out.println("Notice: the answer is the same; only the time changes.");

        // Step 3: a tiny job on a small list. Splitting and joining cost more than the work.
        step("Step 3: 100 amounts, a simple sum");
        List<Integer> small = IntStream.rangeClosed(1, 100).boxed().toList();
        for (int i = 0; i < 20_000; i++) {   // warm up both ways
            small.stream().mapToInt(Integer::intValue).sum();
            small.parallelStream().mapToInt(Integer::intValue).sum();
        }
        t0 = System.nanoTime();
        for (int i = 0; i < 10_000; i++) small.stream().mapToInt(Integer::intValue).sum();
        long seqNs = (System.nanoTime() - t0) / 10_000;
        t0 = System.nanoTime();
        for (int i = 0; i < 10_000; i++) small.parallelStream().mapToInt(Integer::intValue).sum();
        long parNs = (System.nanoTime() - t0) / 10_000;
        System.out.println("sum = " + small.stream().mapToInt(Integer::intValue).sum());
        System.out.println("sequential : about " + seqNs + " ns per sum");
        System.out.println("parallel   : about " + parNs + " ns per sum   (yours may differ)");
        System.out.println("Notice: for small, cheap work, parallel is slower. Splitting is not free.");

        // Step 4: the classic trap. Many threads call add() on ONE ArrayList at once.
        // ArrayList is not thread-safe, so adds overwrite each other or crash.
        step("Step 4: the trap, adding to a shared ArrayList");
        for (int run = 1; run <= 3; run++) {
            List<Integer> shared = new ArrayList<>();
            String result;
            try {
                IntStream.range(0, 10_000).parallel().forEach(shared::add);   // MISTAKE: shared mutable list
                result = "size " + shared.size();
            } catch (RuntimeException e) {
                result = "crashed with " + e.getClass().getSimpleName();
            }
            System.out.println("wrong, run " + run + " : " + result + "   (expected 10000)");
        }
        List<Integer> safe = IntStream.range(0, 10_000).parallel().boxed().toList();   // Java 8: .collect(Collectors.toList())
        System.out.println("right, collect      : size " + safe.size());
        System.out.println("Notice: collect gives each thread its own small list, then joins them.");

        // Step 5: order. Parallel threads finish in any order.
        step("Step 5: order, forEach vs forEachOrdered, findFirst vs findAny");
        StringBuilder jumbled = new StringBuilder();
        IntStream.rangeClosed(1, 8).parallel().forEach(i -> {
            synchronized (jumbled) { jumbled.append(i).append(' '); }
        });
        StringBuilder ordered = new StringBuilder();
        IntStream.rangeClosed(1, 8).parallel().forEachOrdered(i -> ordered.append(i).append(' '));
        System.out.println("forEach        : " + jumbled.toString().trim() + "   (yours may differ)");
        System.out.println("forEachOrdered : " + ordered.toString().trim());
        List<Integer> bigAmounts = List.of(200, 1500, 300, 2500, 4000, 100, 5000, 700);
        System.out.println("findFirst(>1000) : " + bigAmounts.parallelStream().filter(a -> a > 1000).findFirst().get()
                + "   (always 1500)");
        System.out.println("findAny(>1000)   : " + bigAmounts.parallelStream().filter(a -> a > 1000).findAny().get()
                + "   (any of 1500, 2500, 4000, 5000)");
        System.out.println("Notice: toList() and findFirst() still keep the list's order. forEach does not.");

        // Step 6: reduce runs on each piece, then joins the pieces.
        // The identity (start value) is used ONCE PER PIECE, so it must be neutral: 0 for +, 1 for *.
        step("Step 6: the reduce trap, a wrong identity");
        List<Integer> fees = List.of(1, 2, 3, 4, 5);
        System.out.println("reduce(0, +)  sequential : " + fees.stream().reduce(0, Integer::sum));
        System.out.println("reduce(0, +)  parallel   : " + fees.parallelStream().reduce(0, Integer::sum));
        System.out.println("reduce(10, +) sequential : " + fees.stream().reduce(10, Integer::sum)
                + "   (10 + 15)");
        System.out.println("reduce(10, +) parallel   : " + fees.parallelStream().reduce(10, Integer::sum)
                + "   (10 added once per piece: 5 pieces -> 50 + 15)");
        System.out.println("Notice: start with 0 for a sum, and add the 10 after the stream.");

        // Step 7: waiting work. Each biller call just sleeps 300 ms (like an HTTP call).
        // A parallel stream has only cores threads (7 workers + main), shared by the WHOLE app.
        step("Step 7: 16 biller calls of 300 ms each");
        List<Integer> billers = IntStream.rangeClosed(1, 16).boxed().toList();
        t0 = System.nanoTime();
        int billsA = billers.parallelStream().mapToInt(S03_ParallelStreams::callBiller).sum();
        long streamMs = (System.nanoTime() - t0) / 1_000_000;

        ExecutorService pool = Executors.newFixedThreadPool(16);   // our own pool, sized for waiting
        try {
            t0 = System.nanoTime();
            List<CompletableFuture<Integer>> calls = billers.stream()
                    .map(b -> CompletableFuture.supplyAsync(() -> callBiller(b), pool))
                    .toList();
            int billsB = calls.stream().mapToInt(CompletableFuture::join).sum();
            long poolMs = (System.nanoTime() - t0) / 1_000_000;
            System.out.println("parallel stream (" + cores + " threads)       : " + billsA + " bills in about "
                    + round100(streamMs) + " ms");
            System.out.println("CompletableFuture, own pool of 16 : " + billsB + " bills in about "
                    + round100(poolMs) + " ms");
        } finally {
            pool.shutdown();
        }
        System.out.println("Notice: 16 calls / 8 threads = 2 rounds of 300 ms. Your own pool of 16 = 1 round.");
        System.out.println("Notice: while they sleep, every other parallel stream in the app has no threads.");
    }

    /** Runs a small stream and returns the names of the threads that did the work. */
    static Set<String> threadsUsed(boolean parallel) {
        Set<String> names = ConcurrentHashMap.newKeySet();
        IntStream s = IntStream.rangeClosed(1, 2_000);
        if (parallel) s = s.parallel();
        s.forEach(i -> {
            busyWork(i);
            names.add(Thread.currentThread().getName().replace("ForkJoinPool.commonPool-", ""));
        });
        return new TreeSet<>(names);
    }

    /** A made-up but CPU-heavy risk score: no waiting, only calculation. */
    static long riskTotal(int[] amounts, boolean parallel) {
        IntStream s = IntStream.of(amounts);
        if (parallel) s = s.parallel();
        return s.mapToLong(S03_ParallelStreams::busyWork).sum();
    }

    static long busyWork(int amount) {
        long x = amount;
        for (int i = 0; i < 400; i++) {
            x = (x * 31 + i) % 1_000_003;
        }
        return x % 100;   // a score from 0 to 99
    }

    /** Pretends to be a 300 ms HTTP call to a biller. Returns 1 bill. */
    static int callBiller(int billerId) {
        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return 1;
    }

    static long round100(long ms) {
        return Math.round(ms / 100.0) * 100;
    }

    static void step(String text) {
        System.out.println();
        System.out.println("=== " + text + " ===");
    }
}
