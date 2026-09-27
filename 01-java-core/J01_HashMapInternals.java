import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/*
 * J01  How HashMap works inside: a runnable demo
 *
 * WHAT YOU WILL SEE (the numbers match J01_HashMapInternals.md)
 *   Step 1  put(101): 101 % 16 = 5, so the entry goes to bucket 5
 *   Step 2  get(101) goes straight to bucket 5 and never looks anywhere else
 *   Step 3  put(101) again replaces the value; the size stays 1
 *   Step 4  117 % 16 is also 5: a collision, so bucket 5 holds 101 -> 117
 *   Step 5  the 13th entry doubles the table to 32 buckets; 117 moves to bucket 21
 *   Step 6  9 keys crowd one bucket, but with only 16 buckets HashMap resizes
 *           instead of building a tree
 *   Step 7  one null key is allowed, and it lives in bucket 0
 *   Step 8  a key that changes after put() can't be found any more
 *
 * HOW TO RUN   java 01-java-core/J01_HashMapInternals.java   (or click "Run" above main)
 * READ FIRST   J01_HashMapInternals.md
 */
public class J01_HashMapInternals {

    public static void main(String[] args) {
        // Our map: employee ID -> name. A new HashMap starts with 16 empty buckets.
        Map<Integer, String> employees = new HashMap<>();

        // Step 1: HashMap turns the key into a bucket number. For an Integer the
        // hashCode is the number itself, so the bucket is 101 % 16 = 5.
        step("Step 1: put(101, \"Rahul\")");
        employees.put(101, "Rahul");
        System.out.println("101 % 16 = " + bucketOf(101, 16) + ", so 101 goes to bucket 5");
        showBuckets(employees, 16);

        // Step 2: get() does the same math, so it lands on bucket 5 directly.
        step("Step 2: get(101)");
        System.out.println("get(101) = " + employees.get(101));
        System.out.println("Notice: it calculated bucket 5 and never looked at the other 15 buckets.");

        // Step 3: the same key again. equals() says "same key", so the value is
        // replaced and put() hands back the old value.
        step("Step 3: put(101, \"Rahul Sharma\"), the same key again");
        String oldValue = employees.put(101, "Rahul Sharma");
        System.out.println("put() returned the old value: " + oldValue);
        System.out.println("size is still " + employees.size() + ", because keys are never duplicated");
        showBuckets(employees, 16);

        // Step 4: 117 % 16 is also 5. Two different keys, one bucket: a collision.
        // Both entries stay in bucket 5, one after the other.
        step("Step 4: put(117, \"Priya\"), a collision");
        employees.put(117, "Priya");
        System.out.println("117 % 16 = " + bucketOf(117, 16) + ", the same bucket as 101");
        showBuckets(employees, 16);
        System.out.println("get(117) = " + employees.get(117));
        System.out.println("Notice: hashCode() picked bucket 5, then equals() picked 117 inside it.");

        // Step 5: 16 buckets x 0.75 = 12. When the 13th entry arrives, HashMap
        // doubles to 32 buckets and moves every entry (now % 32 instead of % 16).
        step("Step 5: resize when the map goes past 12 entries (16 x 0.75)");
        for (int id = 102; id <= 111; id++) {       // 10 more employees: 12 entries in total
            employees.put(id, "Employee " + id);
        }
        System.out.println("12 entries in 16 buckets (still at the limit, no resize yet):");
        showBuckets(employees, 16);

        employees.put(112, "Employee 112");         // the 13th entry: 13 > 12, so HashMap doubles
        System.out.println();
        System.out.println("13th entry added, so HashMap doubled to 32 buckets:");
        showBuckets(employees, 32);
        System.out.println("Notice: 101 stayed in bucket 5. 117 moved to bucket 21 (5 + 16). The collision is gone.");

        // Step 6: what if one bucket still gets crowded? See crowdedBucket() below.
        step("Step 6: a crowded bucket (more than 8 keys in one bucket)");
        crowdedBucket();

        // Step 7: a null key is allowed once. Its hash is treated as 0: bucket 0.
        step("Step 7: null key");
        employees.put(null, "Unknown");
        System.out.println("get(null) = " + employees.get(null));
        System.out.println("Notice: one null key is fine; it always lives in bucket 0.");

        // Step 8: why keys must never change. See changingAKeyAfterPut() below.
        step("Step 8: never change a key after put()");
        changingAKeyAfterPut();
    }

    // -------------------------------------------------------------------------
    // Step 6: what HashMap does when one bucket gets more than 8 entries
    // -------------------------------------------------------------------------
    static void crowdedBucket() {
        Map<Integer, String> map = new HashMap<>();               // 16 buckets

        // 8 keys, each 16 apart, so every key % 16 = 5 and they all go to bucket 5.
        for (int key = 5; key <= 117; key += 16) {                // 5, 21, 37, 53, 69, 85, 101, 117
            map.put(key, "Employee " + key);
        }
        System.out.println("8 keys, each 16 apart. Every key % 16 = 5, so all go to bucket 5:");
        showBucketKeys(map.keySet(), 16);
        // HashMap prints its keys bucket by bucket, so this order shows where they REALLY are.
        System.out.println("Real HashMap order: " + map.keySet());

        // The 9th key makes bucket 5 hold 9 entries, which is more than 8.
        // The map has only 16 buckets (fewer than 64), so HashMap doubles to 32
        // INSTEAD of building a tree, even though it holds just 9 entries.
        map.put(133, "Employee 133");
        System.out.println();
        System.out.println("Add a 9th key (133). Bucket 5 now has 9 entries, more than 8.");
        System.out.println("Only 16 buckets (fewer than 64), so HashMap RESIZES to 32 instead of making a tree:");
        showBucketKeys(map.keySet(), 32);
        System.out.println("Real HashMap order: " + map.keySet() + "   <- changed, so the real map resized");
        System.out.println("Notice: the crowd of 9 split into 5 + 4. No tree needed.");

        // A bad hashCode is different: if every key's hashCode is 1, the bucket
        // never changes, however many buckets there are. Resizing can't help.
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

        key.id = 102;                               // the mistake: the key changes AFTER put()
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
     * Which bucket a key goes to: the same formula HashMap uses.
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
