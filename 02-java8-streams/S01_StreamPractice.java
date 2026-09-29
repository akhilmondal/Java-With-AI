import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.IntStream;

/*
 * S01  Stream practice: write the 8 programs yourself
 *
 * WHY STREAMS (the story)
 *   Before Java 8, each of these 8 programs was a loop with a map, null checks
 *   and a temporary list, often 8 to 12 lines. Java 8 streams say the same
 *   thing as one pipeline: source -> filter / map -> collect. That's why
 *   interviewers ask for streams: they show you think about WHAT you want,
 *   not only HOW to loop.
 *
 * HOW TO USE
 *   1. Pick a problem below. Try it for 10 minutes WITHOUT looking anything up.
 *   2. Run:  java 02-java8-streams/S01_StreamPractice.java
 *      Each problem prints [DONE] when your answer is right. If it isn't right yet,
 *      you see the expected output next to yours. The last line shows your score out of 8.
 *   3. Stuck for 10 minutes? Read the hint sheet in S00_StreamsToolkit.md,
 *      then the HINTS at the bottom of this file.
 *   4. Only after that, compare with S02_StreamSolutions.java.
 *
 * RULES: use streams (no for loops) inside the 8 methods. Don't change the
 * method signatures or the data, because the checker depends on them.
 */
public class S01_StreamPractice {

    record Employee(int id, String name, String department, int salary) { }

    static final List<Employee> EMPLOYEES = List.of(
            new Employee(101, "Rahul", "PAYMENTS", 50_000),
            new Employee(102, "Priya", "PAYMENTS", 70_000),
            new Employee(103, "Amit", "BILLING", 50_000),
            new Employee(104, "Sneha", "BILLING", 60_000),
            new Employee(105, "Vikram", "UI", 55_000));

    // =========================================================================
    // 1. Character frequency
    //    Input    : "SUCCESS"
    //    Expected : {S=3, U=1, C=2, E=1}
    //    Bonus    : keep that first-seen order (the checker accepts any order)
    // =========================================================================
    static Map<Character, Long> charFrequency(String text) {
        // TODO: write it with streams
        return Map.of();
    }

    // =========================================================================
    // 2. First non-repeated character
    //    Input    : "SUCCESS"
    //    Expected : U        (S repeats, U appears only once and comes first)
    // =========================================================================
    static Character firstNonRepeated(String text) {
        // TODO: write it with streams
        return null;
    }

    // =========================================================================
    // 3. Duplicate elements in a list
    //    Input    : [T1, T2, T1, T3, T2, T4]   (a callback log: T1 and T2 came twice)
    //    Expected : [T1, T2]
    // =========================================================================
    static Set<String> duplicates(List<String> ids) {
        // TODO: write it with streams
        return Set.of();
    }

    // =========================================================================
    // 4. Second-highest number
    //    Input    : [15000, 450, 15000, 1200, 300]
    //    Expected : 1200     (careful: 15000 appears twice)
    // =========================================================================
    static int secondHighest(List<Integer> amounts) {
        // TODO: write it with streams
        return -1;
    }

    // =========================================================================
    // 5. Employees grouped by department
    //    Expected : {BILLING=[Amit, Sneha], PAYMENTS=[Rahul, Priya], UI=[Vikram]}
    // =========================================================================
    static Map<String, List<Employee>> groupByDepartment(List<Employee> employees) {
        // TODO: write it with streams
        return Map.of();
    }

    // =========================================================================
    // 6. Highest-paid employee per department
    //    Expected : {BILLING=Sneha, PAYMENTS=Priya, UI=Vikram}
    // =========================================================================
    static Map<String, Employee> highestPaidByDepartment(List<Employee> employees) {
        // TODO: write it with streams
        return Map.of();
    }

    // =========================================================================
    // 7. Employees sorted by salary descending, then by name
    //    Expected : [Priya, Sneha, Vikram, Amit, Rahul]
    //               (Amit and Rahul both earn 50000, so A comes before R)
    // =========================================================================
    static List<Employee> sortBySalaryDescThenName(List<Employee> employees) {
        // TODO: write it with streams
        return List.of();
    }

    // =========================================================================
    // 8. Numbers partitioned into even and odd
    //    Input    : 1 to 10
    //    Expected : {false=[1, 3, 5, 7, 9], true=[2, 4, 6, 8, 10]}
    // =========================================================================
    static Map<Boolean, List<Integer>> evenAndOdd(List<Integer> numbers) {
        // TODO: write it with streams
        return Map.of();
    }

    // =========================================================================
    // The checker. You don't need to change anything below.
    // =========================================================================
    static int done = 0;

    public static void main(String[] args) {
        check("1. Character frequency", sortedKeys(charFrequency("SUCCESS")),
                "{C=2, E=1, S=3, U=1}");
        System.out.println("       (your order: " + charFrequency("SUCCESS") + ", first-seen would be {S=3, U=1, C=2, E=1})");
        check("2. First non-repeated character", String.valueOf(firstNonRepeated("SUCCESS")),
                "U");
        check("3. Duplicate elements", sortedSet(duplicates(List.of("T1", "T2", "T1", "T3", "T2", "T4"))),
                "[T1, T2]");
        check("4. Second-highest number", String.valueOf(secondHighest(List.of(15_000, 450, 15_000, 1_200, 300))),
                "1200");
        check("5. Grouped by department", namesByKey(groupByDepartment(EMPLOYEES)),
                "{BILLING=[Amit, Sneha], PAYMENTS=[Rahul, Priya], UI=[Vikram]}");
        check("6. Highest paid per department", nameByKey(highestPaidByDepartment(EMPLOYEES)),
                "{BILLING=Sneha, PAYMENTS=Priya, UI=Vikram}");
        check("7. Salary desc, then name", names(sortBySalaryDescThenName(EMPLOYEES)),
                "[Priya, Sneha, Vikram, Amit, Rahul]");
        check("8. Even and odd", sortedKeys(evenAndOdd(IntStream.rangeClosed(1, 10).boxed().toList())),
                "{false=[1, 3, 5, 7, 9], true=[2, 4, 6, 8, 10]}");
        System.out.println();
        System.out.println("Score: " + done + "/8");
    }

    static void check(String problem, String actual, String expected) {
        if (actual.equals(expected)) {
            done++;
            System.out.println("[DONE] " + problem + " -> " + actual);
        } else {
            System.out.println("[TODO] " + problem);
            System.out.println("       expected: " + expected);
            System.out.println("       got     : " + actual);
        }
    }

    /** Any map, printed with its keys sorted, so HashMap order doesn't matter. */
    static <K, V> String sortedKeys(Map<K, V> map) {
        return map == null ? "null" : new TreeMap<>(map).toString();
    }

    static String sortedSet(Set<String> set) {
        return set == null ? "null" : new TreeSet<>(set).toString();
    }

    static String names(List<Employee> list) {
        return list == null ? "null" : list.stream().map(Employee::name).toList().toString();
    }

    static String namesByKey(Map<String, List<Employee>> map) {
        if (map == null) {
            return "null";
        }
        Map<String, List<String>> result = new TreeMap<>();
        map.forEach((key, list) -> result.put(key, list.stream().map(Employee::name).toList()));
        return result.toString();
    }

    static String nameByKey(Map<String, Employee> map) {
        if (map == null) {
            return "null";
        }
        Map<String, String> result = new TreeMap<>();
        map.forEach((key, employee) -> result.put(key, employee.name()));
        return result.toString();
    }
}

/*
 * =============================================================================
 *  HINTS: read only after 10 minutes on a problem
 * =============================================================================
 *  1. Turn the String into a stream of Characters (S00 Step 8). Then group equal
 *     characters and count each group (S00 Step 2). LinkedHashMap::new keeps
 *     first-seen order (S00 Step 6).
 *  2. Build the counts exactly like problem 1, in first-seen order. Then stream
 *     over entrySet() and find the first entry whose count is 1. Remember that
 *     findFirst() gives an Optional.
 *  3. Count each id (groupingBy + counting), then keep the ids whose count is
 *     more than 1. Another way: a HashSet's add() returns false when the item
 *     is already in it.
 *  4. Remove repeats first with distinct(), sort biggest first, skip the
 *     biggest, then take the next one.
 *  5. One collector: groupingBy on department.
 *  6. groupingBy on department, with maxBy on salary as the downstream
 *     collector. maxBy gives an Optional, so unwrap it with collectingAndThen.
 *  7. Comparator.comparingInt(Employee::salary).reversed(), then
 *     .thenComparing(Employee::name).
 *  8. partitioningBy with a test for even numbers.
 * =============================================================================
 */
