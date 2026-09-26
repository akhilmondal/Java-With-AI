import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/*
 * J11  Modern Java (8 to 21): lambdas, var, text blocks, switch expressions,
 *      records, sealed types, pattern matching, virtual threads: runnable demo
 *
 * Read J11_ModernJavaFeatures.md first. This file runs the same steps so you
 * can see them happen. The step numbers match the .md file.
 *
 * Run it:  java 01-java-core/J11_ModernJavaFeatures.java
 *          (or click "Run" above main() in VS Code)
 */
public class J11_ModernJavaFeatures {

    enum Status { SUCCESS, FAILED, PENDING }

    /** A record (Java 16): a data class in one line. */
    record Payment(String txnId, int amount, Status status) { }

    /** A sealed interface (Java 17): ONLY these three types may implement it. */
    sealed interface PaymentResult permits Success, Failure, Pending { }

    record Success(String txnId, int amount) implements PaymentResult { }

    record Failure(String txnId, String reason) implements PaymentResult { }

    record Pending(String txnId) implements PaymentResult { }

    /** Your own functional interface: exactly ONE abstract method, so a lambda can implement it. */
    @FunctionalInterface
    interface FeeRule {
        int fee(int amount);
    }

    public static void main(String[] args) throws Exception {
        // var (Java 10): the compiler works out the type, here List<Payment>
        var payments = List.of(                      // List.of (Java 9): a short, unmodifiable list
                new Payment("TXN1", 1_200, Status.SUCCESS),
                new Payment("TXN2", 450, Status.FAILED),
                new Payment("TXN3", 300, Status.PENDING),
                new Payment("TXN4", 15_000, Status.SUCCESS));

        step("Step 1: lambdas and functional interfaces (Java 8)");
        Predicate<Payment> isLarge = p -> p.amount() > 10_000;       // yes/no check
        Function<Payment, String> toId = Payment::txnId;              // converts one thing to another
        Consumer<String> printer = id -> System.out.println("  large payment: " + id);  // takes, returns nothing
        Supplier<String> newId = () -> "TXN5";                        // gives, takes nothing
        for (Payment p : payments) {
            if (isLarge.test(p)) {
                printer.accept(toId.apply(p));
            }
        }
        System.out.println("  next id from the Supplier: " + newId.get());
        FeeRule twoPercent = amount -> amount * 2 / 100;             // our own functional interface
        System.out.println("  FeeRule 2% on 1000 = " + twoPercent.fee(1000));

        step("Step 2: var, text blocks and String helpers (Java 10, 11, 15)");
        System.out.println("  var payments -> a List of " + payments.size() + " payments");
        String json = """
                {
                  "txnId": "%s",
                  "amount": %d
                }""".formatted("TXN1", 1_200);           // a text block (Java 15): multi-line text as written
        System.out.println(json);
        System.out.println("  \"  TXN1  \".strip() = [" + "  TXN1  ".strip() + "]");   // Java 11
        System.out.println("  \"   \".isBlank()   = " + "   ".isBlank());               // Java 11
        System.out.println("  \"=\".repeat(10)    = " + "=".repeat(10));                // Java 11

        step("Step 3: switch expressions (Java 14)");
        for (Payment p : payments) {
            String action = switch (p.status()) {     // returns a value, no break, no fall-through
                case SUCCESS -> "send receipt";
                case FAILED -> "offer retry";
                case PENDING -> "check status later";
            };                                        // miss a case and it won't compile
            System.out.println("  " + p.txnId() + " " + p.status() + " -> " + action);
        }

        step("Step 4: records, sealed types and pattern matching (Java 16, 17, 21)");
        List<PaymentResult> results = List.of(
                new Success("TXN1", 1_200), new Failure("TXN2", "insufficient balance"), new Pending("TXN3"));
        for (PaymentResult result : results) {
            String message = switch (result) {       // pattern matching for switch (Java 21)
                case Success(String id, int amount) -> id + " paid Rs " + amount;   // record pattern: unpacks fields
                case Failure f -> f.txnId() + " failed: " + f.reason();
                case Pending p -> p.txnId() + " is pending";
            };                                        // no default: sealed means the compiler knows all 3 types
            System.out.println("  " + message);
        }
        Object something = new Success("TXN9", 99);
        if (something instanceof Success s) {         // pattern matching for instanceof (Java 16): test + cast in one
            System.out.println("  instanceof pattern: amount = " + s.amount());
        }

        step("Step 5: virtual threads (Java 21), 1,000 tasks that each wait 100 ms");
        long platformMs = runTasks(Executors.newFixedThreadPool(50), 1_000);
        System.out.println("  50 platform threads: " + platformMs + " ms  (1000 / 50 = 20 rounds x 100 ms = ~2000)");
        long virtualMs = runTasks(Executors.newVirtualThreadPerTaskExecutor(), 1_000);
        System.out.println("  virtual threads    : " + virtualMs + " ms  (all 1000 wait at the same time = ~100)");

        step("Step 6: which Java is this?");
        System.out.println("  Runtime.version() = " + Runtime.version() + " (feature release " + Runtime.version().feature() + ")");
    }

    /** Submits the tasks, then closes the executor, which waits for all of them (Java 19+). */
    static long runTasks(ExecutorService executor, int tasks) {
        long start = System.nanoTime();
        try (executor) {
            for (int i = 0; i < tasks; i++) {
                executor.submit(() -> sleep(100));   // pretend: a 100 ms HTTP call
            }
        }
        return (System.nanoTime() - start) / 1_000_000;
    }

    static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    static void step(String text) {
        System.out.println();
        System.out.println("=== " + text + " ===");
    }
}
