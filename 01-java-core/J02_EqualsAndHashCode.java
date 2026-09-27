import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/*
 * J02  equals() and hashCode(): a runnable demo
 *
 * WHAT YOU WILL SEE (the numbers match J02_EqualsAndHashCode.md)
 *   Step 1  override nothing: two copies of employee 101 count as 2 employees
 *   Step 2  override only equals(): still 2, because the map never calls equals()
 *   Step 3  override only hashCode(): same bucket (132 % 16 = 4), but still 2
 *   Step 4  override both: 1 employee, and get(copy) finds the value
 *   Step 5  the rules: the same hashCode doesn't mean equal ("Aa" and "BB"),
 *           and equals/hashCode must use the same fields
 *   Step 6  a record writes both methods for you, from ALL its fields
 *
 * HOW TO RUN   java 01-java-core/J02_EqualsAndHashCode.java   (or click "Run" above main)
 * READ FIRST   J02_EqualsAndHashCode.md
 */
public class J02_EqualsAndHashCode {

    public static void main(String[] args) {
        // Step 1: two objects with the same data. Java doesn't know that
        // "same ID = same employee", so by default they are different.
        step("Step 1: override nothing");
        PlainEmployee p1 = new PlainEmployee(101, "Rahul");
        PlainEmployee p2 = new PlainEmployee(101, "Rahul");    // same data, a different object
        System.out.println("p1 == p2  : " + (p1 == p2));        // false: two objects in memory
        System.out.println("hashCodes : " + p1.hashCode() + " and " + p2.hashCode()
                + "  (made up by the JVM, nothing to do with id 101)");
        check(p1, p2);

        // Step 2: equals() now says "same ID = same employee", but hashCode() is
        // still the random one, so the copies go to different buckets.
        step("Step 2: override only equals()");
        check(new EqualsOnlyEmployee(101, "Rahul"), new EqualsOnlyEmployee(101, "Rahul"));
        System.out.println("Notice: equals() is true, but the map compares hashCodes first and never asks equals().");

        // Step 3: hashCode() now uses the ID, so both copies land in the same bucket,
        // but equals() is still the default "same object?" check.
        step("Step 3: override only hashCode()");
        HashCodeOnlyEmployee h1 = new HashCodeOnlyEmployee(101, "Rahul");
        System.out.println("hashCode = Objects.hash(101) = " + h1.hashCode()
                + ", bucket = " + h1.hashCode() + " % 16 = " + (h1.hashCode() % 16));
        check(h1, new HashCodeOnlyEmployee(101, "Rahul"));
        System.out.println("Notice: right bucket, but equals() is still ==, so the copy isn't recognised.");

        // Step 4: both overridden, on the same field. Same bucket AND equals() says yes.
        step("Step 4: override both (correct)");
        check(new Employee(101, "Rahul"), new Employee(101, "Rahul"));
        System.out.println("Notice: same bucket and equals() true -> one employee, and get() works.");

        // Step 5: the rules. First: the same hashCode does NOT mean equal (a collision).
        step("Step 5: the rules");
        System.out.println("\"Aa\".hashCode() = " + "Aa".hashCode() + ", \"BB\".hashCode() = " + "BB".hashCode());
        System.out.println("\"Aa\".equals(\"BB\") = " + "Aa".equals("BB") + "  (a collision, and that's allowed)");

        // Second: equals() and hashCode() must use the same fields.
        System.out.println();
        System.out.println("Rule 3 broken: equals() uses id, hashCode() uses id + name:");
        check(new MismatchedEmployee(101, "Rahul"), new MismatchedEmployee(101, "Rahul Sharma"));
        System.out.println("Notice: equal objects got different hashCodes, so the set kept both.");

        // Step 6: a record (Java 16+) generates equals() and hashCode() from ALL its fields.
        step("Step 6: a record writes equals() and hashCode() for you");
        check(new EmployeeRecord(101, "Rahul"), new EmployeeRecord(101, "Rahul"));
        System.out.println("record (101, Rahul) equals (101, Rahul Sharma)? "
                + new EmployeeRecord(101, "Rahul").equals(new EmployeeRecord(101, "Rahul Sharma")));
        System.out.println("Notice: a record compares ALL fields, so a different name = a different employee.");
    }

    /**
     * The same test for every case: add both copies to a HashSet, put the first
     * in a HashMap, then search the map with the second.
     * The correct result is set size 1, and get(copy) finds the value.
     */
    static void check(Object e1, Object e2) {
        Set<Object> set = new HashSet<>();
        set.add(e1);
        set.add(e2);                                             // is e2 treated as a duplicate of e1?

        Map<Object, String> salaryByEmployee = new HashMap<>();
        salaryByEmployee.put(e1, "50,000");
        String found = salaryByEmployee.get(e2);                 // search with the copy

        System.out.println("  e1.equals(e2)  : " + e1.equals(e2));
        System.out.println("  same hashCode? : " + (e1.hashCode() == e2.hashCode()));
        System.out.println("  HashSet size   : " + set.size()
                + (set.size() == 1 ? "       <- correct" : "       <- WRONG: the same employee twice"));
        System.out.println("  map.get(copy)  : " + found
                + (found != null ? "  <- correct" : "    <- WRONG: can't find it"));
    }

    // -------------------------------------------------------------------------
    // The same employee written five ways. Only the overrides differ.
    // -------------------------------------------------------------------------

    /** Step 1: overrides nothing, so it uses Object's equals() (same as ==) and Object's hashCode(). */
    static class PlainEmployee {
        final int id;
        final String name;

        PlainEmployee(int id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    /**
     * Step 2: overrides only equals(). This mistake is shown on purpose.
     * If you compile with "javac -Xlint:all", javac even warns about it:
     * "[overrides] Class ... overrides equals, but neither it nor any superclass
     * overrides hashCode method".
     */
    static class EqualsOnlyEmployee {
        final int id;
        final String name;

        EqualsOnlyEmployee(int id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            EqualsOnlyEmployee other = (EqualsOnlyEmployee) o;
            return this.id == other.id;
        }
        // No hashCode(): it's still Object's random-looking number.
    }

    /** Step 3: overrides only hashCode(). The other mistake, shown on purpose. */
    static class HashCodeOnlyEmployee {
        final int id;
        final String name;

        HashCodeOnlyEmployee(int id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public int hashCode() {
            return Objects.hash(id);        // 31 * 1 + 101 = 132 for id 101
        }
        // No equals(): it's still Object's equals(), which is the same as ==.
    }

    /** Step 4: overrides both, using the same field (id). This is the correct way. */
    static class Employee {
        final int id;                       // final: can't change after creation (J01 Step 8)
        final String name;

        Employee(int id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;                                   // 1. same object? then equal
            if (o == null || getClass() != o.getClass()) return false;   // 2. null or another class? not equal
            Employee other = (Employee) o;                                // 3. now the cast is safe
            return this.id == other.id;                                   // 4. compare what defines "same"
        }

        @Override
        public int hashCode() {
            return Objects.hash(id);                                      // the same field as equals()
        }
    }

    /** Step 5, Rule 3 broken on purpose: equals() uses id, but hashCode() also uses name. */
    static class MismatchedEmployee {
        final int id;
        final String name;

        MismatchedEmployee(int id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            MismatchedEmployee other = (MismatchedEmployee) o;
            return this.id == other.id;                 // only id
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, name);              // id AND name: more fields than equals() uses
        }
    }

    /** Step 6: a record (Java 16+) generates equals() and hashCode() from ALL of its fields. */
    record EmployeeRecord(int id, String name) { }

    static void step(String text) {
        System.out.println();
        System.out.println("=== " + text + " ===");
    }
}
