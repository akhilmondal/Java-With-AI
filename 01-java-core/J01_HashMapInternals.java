import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/*
 * J01  How HashMap works inside: runnable demo
 *
 * Read J01_HashMapInternals.md first. This file runs the same steps so you
 * can see them happen. The step numbers match the .md file.
 *
 * Run it:  java 01-java-core/J01_HashMapInternals.java
 *          (or click "Run" above main() in VS Code)
 */
public class J01_HashMapInternals {

    public static void main(String[] args) {
        // key = employee ID, value = name. A new HashMap starts with 16 buckets.
        Map<Integer, String> employees = new HashMap<>();

        step("Step 1: put(101, \"Rahul\")");
        employees.put(101, "Rahul");
        System.out.println("101 % 16 = " + bucketOf(101, 16) + ", so 101 goes to bucket 5");
        showBuckets(employees, 16);

        step("Step 2: get(101)");
        // Java calculates bucket 5 again and looks only there.
        System.out.println("get(101) = " + employees.get(101));

        step("Step 3: put(101, \"Rahul Sharma\"), the same key again");
        String oldValue = employees.put(101, "Rahul Sharma");   // replaces the value
        System.out.println("put() returned the old value: " + oldValue);
        System.out.println("size is still " + employees.size() + ", because keys are never duplicated");
        showBuckets(employees, 16);

        step("Step 4: put(117, \"Priya\"), a collision");
        employees.put(117, "Priya");
        System.out.println("117 % 16 = " + bucketOf(117, 16) + ", the same bucket as 101");
        showBuckets(employees, 16);
        // get(117): go to bucket 5 -> is it 101? no -> next node -> is it 117? yes
        System.out.println("get(117) = " + employees.get(117));

        step("Step 5: resize when the map goes past 12 entries (16 x 0.75)");
        for (int id = 102; id <= 111; id++) {       // 10 more employees, 12 entries in total
            employees.put(id, "Employee " + id);
        }
        System.out.println("12 entries in 16 buckets (still at the limit, no resize yet):");
        showBuckets(employees, 16);

        employees.put(112, "Employee 112");         // 13th entry: 13 > 12, so HashMap doubles to 32
        System.out.println();
        System.out.println("13th entry added, so HashMap doubled to 32 buckets:");
        showBuckets(employees, 32);
        System.out.println("101 stayed in bucket 5. 117 moved to bucket 21 (5 + 16). No collision now.");

        step("Step 6: a crowded bucket (more than 8 keys in one bucket)");
        crowdedBucket();

        step("Step 7: null key");
        employees.put(null, "Unknown");             // allowed once, always in bucket 0
        System.out.println("get(null) = " + employees.get(null));

        step("Step 8: never change a key after put()");
        changingAKeyAfterPut();
    }

    // -------------------------------------------------------------------------
    // Step 6: what HashMap does when one bucket gets more than 8 entries
    // -------------------------------------------------------------------------
    static void crowdedBucket() {
        Map<Integer, String> map = new HashMap<>();               // 16 buckets

        // 8 keys, each 16 apart, so every key % 16 = 5 and all go to bucket 5.
        for (int key = 5; key <= 117; key += 16) {                // 5, 21, 37, 53, 69, 85, 101, 117
            map.put(key, "Employee " + key);
        }
        System.out.println("8 keys, each 16 apart. Every key % 16 = 5, so all go to bucket 5:");
        showBucketKeys(map.keySet(), 16);
        // HashMap prints its keys bucket by bucket, so this order shows where they really are.
        System.out.println("Real HashMap order: " + map.keySet());

        // The 9th key makes bucket 5 hold 9 entries, which is more than 8.
        // The map has only 16 buckets (fewer than 64), so HashMap doubles to 32
        // INSTEAD of making a tree, even though it holds only 9 entries.
        map.put(133, "Employee 133");
        System.out.println();
        System.out.println("Add a 9th key (133). Bucket 5 now has 9 entries, more than 8.");
        System.out.println("Only 16 buckets (fewer than 64), so HashMap RESIZES to 32 instead of making a tree:");
        showBucketKeys(map.keySet(), 32);
        System.out.println("Real HashMap order: " + map.keySet() + "   <- changed, so the real map resized");
        System.out.println("The crowd of 9 split into 5 + 4. No tree needed.");

        // A bad hashCode is different. If every key's hashCode is 1, the bucket never changes.
        System.out.println();
        System.out.println("A bad hashCode is different. If every key's hashCode is 1:");
        for (int buckets = 16; buckets <= 1024; buckets *= 4) {
            System.out.println("  " + buckets + " buckets -> bucket " + (1 % buckets));
        }
        System.out.println("Resizing never helps, so with 64+ buckets HashMap turns that bucket into a red-black tree.");
    }

    // -------------------------------------------------------------------------
    // Step 8: a key that changes after put() gets lost inside the map
    // -------------------------------------------------------------------------
    static void changingAKeyAfterPut() {
        Map<EmployeeKey, String> map = new HashMap<>();
        EmployeeKey key = new EmployeeKey(101);

        map.put(key, "Rahul");
        System.out.println("put with id 101: stored in bucket " + bucketOf(key, 16));

        key.id = 102;                               // the mistake: key changed AFTER put()
        System.out.println("id changed to 102: get() now looks in bucket " + bucketOf(key, 16));

        // Bucket 6 is empty, so nothing is found.
        System.out.println("get(key)                  = " + map.get(key));
        // Bucket 5 is right this time, but the stored key now says 102, so equals() fails.
        System.out.println("get(new EmployeeKey(101)) = " + map.get(new EmployeeKey(101)));
        // The entry is still inside the map. You just can't reach it.
        System.out.println("size                      = " + map.size());

        System.out.println("Fix: make the id final so it can't change, or use Integer/String as the key.");
    }

    /** A key whose id can change later. This is the mistake, shown on purpose. */
    static class EmployeeKey {
        int id;                                     // not final, so it can be changed

        EmployeeKey(int id) {
            this.id = id;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof EmployeeKey)) {
                return false;
            }
            EmployeeKey other = (EmployeeKey) o;
            return this.id == other.id;
        }

        @Override
        public int hashCode() {
            return id;                              // the bucket is chosen from the id
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Which bucket a key goes to. This is the same formula HashMap uses.
     * For small numbers like 101 it gives the same answer as key % buckets.
     */
    static int bucketOf(Object key, int buckets) {
        if (key == null) {
            return 0;                               // the null key always goes to bucket 0
        }
        int h = key.hashCode();                     // an Integer's hashCode is the number itself
        h = h ^ (h >>> 16);                         // mixes the high bits in; small numbers stay the same
        return h & (buckets - 1);                   // a fast version of h % buckets (buckets is a power of 2)
    }

    /**
     * Prints every non-empty bucket and the entries inside it.
     * We can't open HashMap's real array, so this uses bucketOf(), the same
     * formula HashMap uses.
     */
    static void showBuckets(Map<Integer, String> map, int buckets) {
        Map<Integer, List<String>> byBucket = new TreeMap<>();     // TreeMap keeps bucket numbers in order
        for (Map.Entry<Integer, String> entry : map.entrySet()) {
            int bucket = bucketOf(entry.getKey(), buckets);
            // Create the list for this bucket if it doesn't exist yet, then add the entry to it.
            byBucket.computeIfAbsent(bucket, b -> new ArrayList<>())
                    .add(entry.getKey() + "=" + entry.getValue());
        }
        for (Map.Entry<Integer, List<String>> bucket : byBucket.entrySet()) {
            System.out.printf("  bucket %2d : %s%n", bucket.getKey(), String.join(" -> ", bucket.getValue()));
        }
    }

    /** Like showBuckets(), but prints only the keys, so a crowded bucket fits on one line. */
    static void showBucketKeys(Collection<Integer> keys, int buckets) {
        Map<Integer, List<String>> byBucket = new TreeMap<>();
        for (Integer key : keys) {
            byBucket.computeIfAbsent(bucketOf(key, buckets), b -> new ArrayList<>())
                    .add(String.valueOf(key));
        }
        for (Map.Entry<Integer, List<String>> bucket : byBucket.entrySet()) {
            System.out.printf("  bucket %2d : %s%n", bucket.getKey(), String.join(" -> ", bucket.getValue()));
        }
    }

    static void step(String text) {
        System.out.println();
        System.out.println("=== " + text + " ===");
    }
}
