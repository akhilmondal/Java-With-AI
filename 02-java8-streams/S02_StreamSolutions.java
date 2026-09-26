import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/*
 * S02  Stream solutions: open this ONLY after you've tried S01 yourself
 *
 * Each problem has:
 *   - the solution, with a comment on every step
 *   - "Say it like this": how to talk through it while live coding
 *   - the cost (time complexity)
 *   - sometimes a second way, or the classic mistake, also checked
 *
 * Run it:  java 02-java8-streams/S02_StreamSolutions.java
 */
public class S02_StreamSolutions {

    record Employee(int id, String name, String department, int salary) { }

    static final List<Employee> EMPLOYEES = List.of(
            new Employee(101, "Rahul", "PAYMENTS", 50_000),
            new Employee(102, "Priya", "PAYMENTS", 70_000),
            new Employee(103, "Amit", "BILLING", 50_000),
            new Employee(104, "Sneha", "BILLING", 60_000),
            new Employee(105, "Vikram", "UI", 55_000));

    // =========================================================================
    // 1. Character frequency: "SUCCESS" -> {S=3, U=1, C=2, E=1}
    //    Say it like this: "I turn the string into a stream of characters, group
    //    equal characters, and count each group. LinkedHashMap keeps the order
    //    in which the characters first appear."
    //    Cost: O(n), one pass.
    // =========================================================================
    static Map<Character, Long> charFrequency(String text) {
        return text.chars()                                   // int codes: 83, 85, 67, ...
                .mapToObj(c -> (char) c)                      // back to Characters: S, U, C, ...
                .collect(Collectors.groupingBy(
                        Function.identity(),                  // group by the character itself
                        LinkedHashMap::new,                   // keep first-seen order
                        Collectors.counting()));              // count each group
    }

    // =========================================================================
    // 2. First non-repeated character: "SUCCESS" -> U
    //    Say it like this: "I count the characters in first-seen order, like
    //    problem 1, then return the first one whose count is 1."
    //    Cost: O(n): one pass to count, one pass over at most n entries.
    // =========================================================================
    static Character firstNonRepeated(String text) {
        return charFrequency(text).entrySet().stream()        // S=3, U=1, C=2, E=1 (in order)
                .filter(entry -> entry.getValue() == 1)        // U=1, E=1
                .map(Map.Entry::getKey)                        // U, E
                .findFirst()                                   // Optional[U]
                .orElse(null);                                 // null if every character repeats
    }

    // =========================================================================
    // 3. Duplicate elements: [T1, T2, T1, T3, T2, T4] -> [T1, T2]
    //    Say it like this: "I count how many times each id appears, then keep the
    //    ids that appear more than once."
    //    Cost: O(n).
    // =========================================================================
    static Set<String> duplicates(List<String> ids) {
        return ids.stream()
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))  // {T1=2, T2=2, T3=1, T4=1}
                .entrySet().stream()
                .filter(entry -> entry.getValue() > 1)          // T1, T2
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    /** 3b. Another way: HashSet.add() returns false if the item was already added. */
    static Set<String> duplicatesWithSet(List<String> ids) {
        Set<String> seen = new HashSet<>();
        return ids.stream()
                .filter(id -> !seen.add(id))                    // add fails -> we've seen it before
                .collect(Collectors.toSet());
        // Works for a normal (sequential) stream. Changing an outside Set from a
        // lambda is not safe with parallel streams.
    }

    // =========================================================================
    // 4. Second-highest number: [15000, 450, 15000, 1200, 300] -> 1200
    //    Say it like this: "I remove duplicates, sort from biggest to smallest,
    //    skip the first, and take the next."
    //    Cost: O(n log n) because of the sort.
    // =========================================================================
    static int secondHighest(List<Integer> amounts) {
        return amounts.stream()
                .distinct()                                     // 15000, 450, 1200, 300
                .sorted(Comparator.reverseOrder())              // 15000, 1200, 450, 300
                .skip(1)                                        // 1200, 450, 300
                .findFirst()                                    // Optional[1200]
                .orElseThrow();                                 // no second value -> exception
    }

    /** 4b. The classic mistake: without distinct(), the second item is 15000 again. */
    static int secondHighestWithoutDistinct(List<Integer> amounts) {
        return amounts.stream().sorted(Comparator.reverseOrder()).skip(1).findFirst().orElseThrow();
    }

    // =========================================================================
    // 5. Employees grouped by department
    //    Say it like this: "groupingBy on department gives me a map from each
    //    department to the list of its employees."
    //    Cost: O(n).
    // =========================================================================
    static Map<String, List<Employee>> groupByDepartment(List<Employee> employees) {
        return employees.stream()
                .collect(Collectors.groupingBy(Employee::department));
    }

    // =========================================================================
    // 6. Highest-paid employee per department
    //    Say it like this: "I group by department and, inside each group, keep
    //    the max by salary. maxBy returns an Optional, so I unwrap it with
    //    collectingAndThen; a group is never empty, so get() is safe."
    //    Cost: O(n).
    // =========================================================================
    static Map<String, Employee> highestPaidByDepartment(List<Employee> employees) {
        return employees.stream()
                .collect(Collectors.groupingBy(
                        Employee::department,
                        Collectors.collectingAndThen(
                                Collectors.maxBy(Comparator.comparingInt(Employee::salary)),
                                Optional::get)));
    }

    /** 6b. Another way: toMap, and on the same department keep the one with more salary. */
    static Map<String, Employee> highestPaidWithToMap(List<Employee> employees) {
        return employees.stream()
                .collect(Collectors.toMap(
                        Employee::department,                   // key
                        Function.identity(),                    // value: the employee
                        (a, b) -> a.salary() >= b.salary() ? a : b));   // same key twice: keep the richer one
    }

    // =========================================================================
    // 7. Employees sorted by salary descending, then by name
    //    Say it like this: "I sort with a comparator on salary, reversed for
    //    high to low, and then by name to break ties."
    //    Cost: O(n log n).
    // =========================================================================
    static List<Employee> sortBySalaryDescThenName(List<Employee> employees) {
        return employees.stream()
                .sorted(Comparator.comparingInt(Employee::salary).reversed()
                        .thenComparing(Employee::name))         // Amit before Rahul at 50000
                .toList();
    }

    // =========================================================================
    // 8. Numbers partitioned into even and odd
    //    Say it like this: "partitioningBy with an 'is even' test gives exactly
    //    two lists: false for odd, true for even."
    //    Cost: O(n).
    // =========================================================================
    static Map<Boolean, List<Integer>> evenAndOdd(List<Integer> numbers) {
        return numbers.stream()
                .collect(Collectors.partitioningBy(n -> n % 2 == 0));
    }

    // =========================================================================
    // The same checker as S01, plus the second ways and the classic mistake.
    // =========================================================================
    static int done = 0;

    public static void main(String[] args) {
        check("1. Character frequency", sortedKeys(charFrequency("SUCCESS")),
                "{C=2, E=1, S=3, U=1}");
        System.out.println("       (first-seen order kept: " + charFrequency("SUCCESS") + ")");
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

        System.out.println();
        System.out.println("--- second ways ---");
        System.out.println("3b. duplicates with a Set      : " + sortedSet(duplicatesWithSet(List.of("T1", "T2", "T1", "T3", "T2", "T4"))));
        System.out.println("6b. highest paid with toMap    : " + nameByKey(highestPaidWithToMap(EMPLOYEES)));
        System.out.println("--- the classic mistake ---");
        System.out.println("4.  second highest WITHOUT distinct(): "
                + secondHighestWithoutDistinct(List.of(15_000, 450, 15_000, 1_200, 300)) + "  <- wrong, 15000 counted twice");
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
