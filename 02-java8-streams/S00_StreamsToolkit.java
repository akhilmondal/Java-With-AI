import java.util.Comparator;
import java.util.IntSummaryStatistics;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/*
 * S00  Streams toolkit: the collectors you need for the 8 programs (runnable demo)
 *
 * Read S00_StreamsToolkit.md first. This file runs the same steps so you can
 * see them happen. The step numbers match the .md file.
 *
 * Run it:  java 02-java8-streams/S00_StreamsToolkit.java
 *          (or click "Run" above main() in VS Code)
 */
public class S00_StreamsToolkit {

    /** One payment. toString() prints just the id, to keep the output short. */
    record Payment(String txnId, int amount, String status, String mode) {
        @Override
        public String toString() {
            return txnId;
        }
    }

    static final List<Payment> PAYMENTS = List.of(
            new Payment("TXN1", 1_200, "SUCCESS", "UPI"),
            new Payment("TXN2", 450, "FAILED", "CARD"),
            new Payment("TXN3", 300, "PENDING", "UPI"),
            new Payment("TXN4", 15_000, "SUCCESS", "NETBANKING"),
            new Payment("TXN5", 800, "SUCCESS", "CARD"),
            new Payment("TXN6", 650, "FAILED", "UPI"));

    public static void main(String[] args) {
        step("Step 1: collect the results with toList() and joining()");
        List<String> successIds = PAYMENTS.stream()
                .filter(p -> p.status().equals("SUCCESS"))
                .map(Payment::txnId)
                .toList();                                   // Java 16+; before that: .collect(Collectors.toList())
        System.out.println("successful ids : " + successIds);
        String allIds = PAYMENTS.stream()
                .map(Payment::txnId)
                .collect(Collectors.joining(", "));          // one String, with ", " between items
        System.out.println("joining(\", \")  : " + allIds);

        step("Step 2: groupingBy, sorting into bins");
        Map<String, List<Payment>> byStatus = PAYMENTS.stream()
                .collect(Collectors.groupingBy(Payment::status));                         // status -> its payments
        Map<String, Long> countByStatus = PAYMENTS.stream()
                .collect(Collectors.groupingBy(Payment::status, Collectors.counting()));  // status -> how many
        Map<String, Integer> sumByStatus = PAYMENTS.stream()
                .collect(Collectors.groupingBy(Payment::status, Collectors.summingInt(Payment::amount)));
        Map<String, List<String>> idsByMode = PAYMENTS.stream()
                .collect(Collectors.groupingBy(Payment::mode,
                        Collectors.mapping(Payment::txnId, Collectors.toList())));      // keep only the id
        // A HashMap has no order (J04), so sorted() copies it into a TreeMap just to print A to Z.
        System.out.println("groupingBy(status)                 : " + sorted(byStatus));
        System.out.println("groupingBy(status, counting())     : " + sorted(countByStatus));
        System.out.println("groupingBy(status, summingInt(amt)): " + sorted(sumByStatus));
        System.out.println("groupingBy(mode, mapping(id))      : " + sorted(idsByMode));

        step("Step 3: the biggest in each bin, maxBy and collectingAndThen");
        Map<String, Optional<Payment>> maxAsOptional = PAYMENTS.stream()
                .collect(Collectors.groupingBy(Payment::status,
                        Collectors.maxBy(Comparator.comparingInt(Payment::amount))));
        Map<String, Payment> maxPayment = PAYMENTS.stream()
                .collect(Collectors.groupingBy(Payment::status,
                        Collectors.collectingAndThen(
                                Collectors.maxBy(Comparator.comparingInt(Payment::amount)),
                                Optional::get)));              // unwrap: a bin is never empty here
        System.out.println("maxBy                      : " + sorted(maxAsOptional));
        System.out.println("collectingAndThen(get)     : " + sorted(maxPayment));

        step("Step 4: partitioningBy, exactly two bins: true and false");
        Map<Boolean, List<Payment>> bigOrNot = PAYMENTS.stream()
                .collect(Collectors.partitioningBy(p -> p.amount() > 1_000));
        Map<Boolean, Long> bigOrNotCount = PAYMENTS.stream()
                .collect(Collectors.partitioningBy(p -> p.amount() > 1_000, Collectors.counting()));
        System.out.println("partitioningBy(amount > 1000)            : " + bigOrNot);
        System.out.println("partitioningBy(amount > 1000, counting()): " + bigOrNotCount);

        step("Step 5: toMap, and the duplicate-key trap");
        Map<String, Integer> amountById = PAYMENTS.stream()
                .collect(Collectors.toMap(Payment::txnId, Payment::amount));
        System.out.println("toMap(id, amount): " + sorted(amountById));
        List<Payment> callbackLog = List.of(PAYMENTS.get(0), PAYMENTS.get(1), PAYMENTS.get(0));   // TXN1 arrives twice
        try {
            callbackLog.stream().collect(Collectors.toMap(Payment::txnId, Payment::amount));
        } catch (IllegalStateException e) {
            System.out.println("same id twice -> IllegalStateException: " + e.getMessage());
        }
        Map<String, Integer> keepFirst = callbackLog.stream()
                .collect(Collectors.toMap(Payment::txnId, Payment::amount, (first, second) -> first));
        System.out.println("with a merge rule (keep first): " + sorted(keepFirst));

        step("Step 6: count, then filter the counts; choose the map type");
        Map<String, Long> countByMode = PAYMENTS.stream()
                .collect(Collectors.groupingBy(Payment::mode, Collectors.counting()));
        List<String> usedMoreThanOnce = countByMode.entrySet().stream()   // a stream over the map's entries
                .filter(entry -> entry.getValue() > 1)
                .map(Map.Entry::getKey)
                .sorted()
                .toList();
        Map<String, Long> firstSeenOrder = PAYMENTS.stream()
                .collect(Collectors.groupingBy(Payment::mode, LinkedHashMap::new, Collectors.counting()));
        Map<String, Payment> byId = PAYMENTS.stream()
                .collect(Collectors.toMap(Payment::txnId, Function.identity()));   // identity() = "the element itself"
        System.out.println("count by mode                   : " + sorted(countByMode));
        System.out.println("modes used more than once       : " + usedMoreThanOnce);
        System.out.println("LinkedHashMap::new (first seen) : " + firstSeenOrder);
        System.out.println("toMap(id, identity()).get(TXN4) : " + byId.get("TXN4") + " with amount " + byId.get("TXN4").amount());

        step("Step 7: numbers: sum, statistics, top 3, a range");
        int total = PAYMENTS.stream().mapToInt(Payment::amount).sum();   // mapToInt gives sum(), average(), ...
        IntSummaryStatistics stats = PAYMENTS.stream().mapToInt(Payment::amount).summaryStatistics();
        List<Integer> top3 = PAYMENTS.stream()
                .map(Payment::amount)
                .sorted(Comparator.reverseOrder())                    // biggest first
                .limit(3)
                .toList();
        List<Integer> oneToFive = IntStream.rangeClosed(1, 5).boxed().toList();   // boxed(): int -> Integer
        System.out.println("sum of amounts : " + total);
        System.out.printf("statistics     : count=%d, min=%d, max=%d, average=%.2f%n",
                stats.getCount(), stats.getMin(), stats.getMax(), stats.getAverage());
        System.out.println("top 3 amounts  : " + top3);
        System.out.println("rangeClosed(1,5): " + oneToFive);

        step("Step 8: a String as a stream of characters");
        List<Integer> codes = "BBPS".chars().boxed().toList();            // chars() gives int codes
        List<Character> letters = "BBPS".chars()
                .mapToObj(c -> (char) c)                                   // turn each code back into a char
                .toList();
        long unique = "BBPS".chars().distinct().count();
        System.out.println("\"BBPS\".chars()           : " + codes);
        System.out.println("mapToObj(c -> (char) c) : " + letters);
        System.out.println("distinct().count()      : " + unique);
    }

    /** Copies any map into a TreeMap, so the keys print in A to Z order. */
    static String sorted(Map<String, ?> map) {
        return new TreeMap<>(map).toString();
    }

    static void step(String text) {
        System.out.println();
        System.out.println("=== " + text + " ===");
    }
}
