import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

/*
 * J08  Comparable vs Comparator, streams (map/flatMap, lazy), Optional: runnable demo
 *
 * Read J08_ComparatorStreamsOptional.md first. This file runs the same steps so
 * you can see them happen. The step numbers match the .md file.
 *
 * Run it:  java 01-java-core/J08_ComparatorStreamsOptional.java
 *          (or click "Run" above main() in VS Code)
 */
public class J08_ComparatorStreamsOptional {

    /**
     * A record (Java 16+) is a short way to write a data class: it gets the fields,
     * getters like name(), and equals/hashCode/toString for free (J02).
     * "implements Comparable" gives it a natural order: by id.
     */
    record Employee(int id, String name, int salary, List<String> skills) implements Comparable<Employee> {
        @Override
        public int compareTo(Employee other) {
            return Integer.compare(this.id, other.id);   // negative: I come first, 0: same, positive: I come after
        }
    }

    static final List<Employee> EMPLOYEES = List.of(           // deliberately NOT in id order
            new Employee(101, "Rahul", 50_000, List.of("Java", "Spring")),
            new Employee(104, "Sneha", 60_000, List.of("Java", "AWS")),
            new Employee(103, "Amit", 50_000, List.of("SQL")),
            new Employee(102, "Priya", 70_000, List.of("Angular", "Java")));

    public static void main(String[] args) {
        step("Step 1: Comparable, the natural order (by id)");
        List<Employee> list = new ArrayList<>(EMPLOYEES);
        Collections.sort(list);                           // uses compareTo()
        System.out.println("sorted by id : " + names(list) + " " + ids(list));
        System.out.println("compareTo(101 vs 102) = " + list.get(0).compareTo(list.get(1)) + "  (negative: 101 comes first)");

        step("Step 2: Comparator, any order you like, from outside the class");
        list.sort(Comparator.comparing(Employee::name));
        System.out.println("by name                      : " + names(list));
        list.sort(Comparator.comparingInt(Employee::salary).reversed()
                .thenComparing(Employee::name));          // tie on salary? then by name
        System.out.println("by salary high->low, then name: " + names(list) + " " + salaries(list));

        // Trap: TreeSet/TreeMap treat "compare == 0" as "the same element".
        Set<Employee> bySalary = new TreeSet<>(Comparator.comparingInt(Employee::salary));
        bySalary.addAll(EMPLOYEES);
        System.out.println("TreeSet by salary only has size " + bySalary.size()
                + " (not 4: Rahul and Amit both earn 50000, so one is dropped)");

        step("Step 3: a stream is lazy; the terminal operation makes it run");
        Stream<Employee> pipeline = EMPLOYEES.stream()
                .filter(e -> {
                    System.out.println("  checking " + e.name());
                    return e.salary() > 55_000;
                });
        System.out.println("pipeline built, nothing checked yet (no terminal operation)");
        Optional<Employee> firstRich = pipeline.findFirst();   // terminal operation: NOW it runs
        System.out.println("findFirst -> " + firstRich.map(Employee::name).orElse("none")
                + " (stopped early: Amit and Priya were never checked)");
        int total = EMPLOYEES.stream().mapToInt(Employee::salary).sum();   // another terminal operation
        System.out.println("sum of salaries = " + total);                   // 60000 + 50000 + 50000 + 70000

        step("Step 4: map vs flatMap");
        List<List<String>> mapped = EMPLOYEES.stream()
                .map(Employee::skills)                     // one employee -> ONE list
                .toList();
        System.out.println("map     -> " + mapped.size() + " items: " + mapped);
        List<String> flat = EMPLOYEES.stream()
                .flatMap(e -> e.skills().stream())         // one employee -> MANY skills, all in one stream
                .toList();
        System.out.println("flatMap -> " + flat.size() + " items: " + flat);
        System.out.println("distinct-> " + flat.stream().distinct().toList());

        step("Step 5: a stream can be used only once");
        Stream<String> names = EMPLOYEES.stream().map(Employee::name);
        System.out.println("first count: " + names.count());
        try {
            names.count();                                 // the same stream again
        } catch (IllegalStateException e) {
            System.out.println("second use -> IllegalStateException: " + e.getMessage());
        }

        step("Step 6: Optional, a box that may be empty");
        System.out.println("findById(101) -> " + findById(101).map(Employee::name));     // Optional[Rahul]
        System.out.println("findById(999) -> " + findById(999).map(Employee::name));     // Optional.empty
        System.out.println("findById(999).map(name).orElse(\"Unknown\") -> "
                + findById(999).map(Employee::name).orElse("Unknown"));
        try {
            findById(999).orElseThrow(() -> new IllegalArgumentException("No employee 999"));
        } catch (IllegalArgumentException e) {
            System.out.println("orElseThrow -> " + e.getMessage());
        }

        System.out.println("orElse vs orElseGet when the value IS there (Rahul):");
        Optional<Employee> rahul = findById(101);
        System.out.println(" orElse:");
        rahul.orElse(createDefault());                     // createDefault() runs anyway
        System.out.println(" orElseGet:");
        rahul.orElseGet(() -> createDefault());            // createDefault() does NOT run
        System.out.println(" (nothing printed for orElseGet)");
    }

    static Optional<Employee> findById(int id) {
        return EMPLOYEES.stream().filter(e -> e.id() == id).findFirst();
    }

    static Employee createDefault() {
        System.out.println("  createDefault() ran");      // imagine a DB call here
        return new Employee(0, "Unknown", 0, List.of());
    }

    static List<String> names(List<Employee> list) {
        return list.stream().map(Employee::name).toList();
    }

    static List<Integer> ids(List<Employee> list) {
        return list.stream().map(Employee::id).toList();
    }

    static List<Integer> salaries(List<Employee> list) {
        return list.stream().map(Employee::salary).toList();
    }

    static void step(String text) {
        System.out.println();
        System.out.println("=== " + text + " ===");
    }
}
