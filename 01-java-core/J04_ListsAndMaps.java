import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/*
 * J04  ArrayList vs LinkedList, HashMap vs LinkedHashMap vs TreeMap: a runnable demo
 *
 * WHAT YOU WILL SEE (the numbers match J04_ListsAndMaps.md)
 *   Step 1  ArrayList: get(3) jumps straight there; add(0) shifts 5 items; capacity 10 -> 15 -> 22 -> 33 -> 49
 *   Step 2  LinkedList: addFirst/addLast just link; get(3) walks T0 -> T1 -> T2 -> T3
 *   Step 3  timings: get(i) is ArrayList's win, add(0) is LinkedList's win
 *   Step 5  the same keys 250, 42, 305, 101 print in three different orders
 *   Step 6  TreeMap answers "nearest key" questions: floorKey(200) = 101, fee slabs
 *   Step 7  LinkedHashMap as an LRU cache: WATER gets evicted
 *
 * HOW TO RUN   java 01-java-core/J04_ListsAndMaps.java   (or click "Run" above main)
 * READ FIRST   J04_ListsAndMaps.md
 */
public class J04_ListsAndMaps {

    public static void main(String[] args) {
        // Step 1: an ArrayList is an array inside. Index access is a jump;
        // inserting at the front means shifting everyone one place right.
        step("Step 1: ArrayList, an array inside");
        List<String> seats = new ArrayList<>(List.of("T1", "T2", "T3", "T4", "T5"));
        System.out.println("list          : " + seats);
        System.out.println("get(3)        : " + seats.get(3) + "   (jumps straight to index 3)");
        seats.add(0, "T0");                                    // T1..T5 each shift one place right
        System.out.println("after add(0)  : " + seats + "   (5 items shifted)");

        // We can't see ArrayList's real array, so this uses the same growth
        // formula as ArrayList: new size = old size + half of it.
        StringBuilder growth = new StringBuilder("10");
        int capacity = 10;
        for (int i = 0; i < 4; i++) {
            capacity = capacity + (capacity >> 1);             // capacity >> 1 is "half of it"
            growth.append(" -> ").append(capacity);
        }
        System.out.println("capacity grows: " + growth);        // 10 -> 15 -> 22 -> 33 -> 49
        System.out.println("Notice: reading by index is instant; inserting at the front moves everyone.");

        // Step 2: a LinkedList is a chain of nodes. Adding at either end just
        // links a node; reaching index 3 means walking node by node.
        step("Step 2: LinkedList, coaches with links");
        LinkedList<String> train = new LinkedList<>(List.of("T1", "T2", "T3", "T4", "T5"));
        train.addFirst("T0");                                  // just attach at the front, nothing shifts
        train.addLast("T6");                                   // just attach at the back
        System.out.println("list          : " + train);
        System.out.println("get(3)        : " + train.get(3) + "   (walks T0 -> T1 -> T2 -> T3)");
        System.out.println("Notice: adding at the ends is instant; reaching the middle means walking.");

        // Step 3: measure both. Each list wins one test.
        step("Step 3: measure it (your times will differ, the winner won't)");
        System.out.println("get(i) for every i, 20,000 items:");
        System.out.println("  ArrayList : " + timeGetByIndex(new ArrayList<>(), 20_000) + " ms");
        System.out.println("  LinkedList: " + timeGetByIndex(new LinkedList<>(), 20_000) + " ms");
        System.out.println("add(0, x) 30,000 times:");
        System.out.println("  ArrayList : " + timeAddAtFront(new ArrayList<>(), 30_000) + " ms");
        System.out.println("  LinkedList: " + timeAddAtFront(new LinkedList<>(), 30_000) + " ms");

        // Step 5: the same four keys go into three maps. The ORDER they come back in
        // is the whole difference between the three.
        step("Step 5: same keys, three maps, three orders");
        int[] ids = {250, 42, 305, 101};                       // put in this order
        Map<Integer, String> hash = new HashMap<>();
        Map<Integer, String> linked = new LinkedHashMap<>();
        Map<Integer, String> tree = new TreeMap<>();
        for (int id : ids) {
            hash.put(id, "Employee " + id);
            linked.put(id, "Employee " + id);
            tree.put(id, "Employee " + id);
        }
        System.out.println("HashMap       : " + hash.keySet() + "   (bucket order: 305->1, 101->5, 250 and 42->10)");
        System.out.println("LinkedHashMap : " + linked.keySet() + "   (the order they were put in)");
        System.out.println("TreeMap       : " + tree.keySet() + "   (sorted)");

        // Step 6: TreeMap is sorted, so it can answer "nearest key" questions.
        step("Step 6: TreeMap's range methods");
        TreeMap<Integer, String> employees = new TreeMap<>(tree);
        System.out.println("firstKey()       : " + employees.firstKey());          // 42
        System.out.println("lastKey()        : " + employees.lastKey());           // 305
        System.out.println("floorKey(200)    : " + employees.floorKey(200));       // 101: biggest key <= 200
        System.out.println("ceilingKey(200)  : " + employees.ceilingKey(200));     // 250: smallest key >= 200
        System.out.println("headMap(250)     : " + employees.headMap(250).keySet()); // [42, 101]

        // Fee slabs: from this amount -> this fee (in rupees)
        TreeMap<Integer, Integer> feeSlabs = new TreeMap<>();
        feeSlabs.put(0, 0);
        feeSlabs.put(1_000, 5);
        feeSlabs.put(5_000, 10);
        feeSlabs.put(10_000, 15);
        for (int amount : new int[] {999, 7_200, 10_000}) {
            int fee = feeSlabs.floorEntry(amount).getValue();  // the slab that starts at or below the amount
            System.out.println("fee for Rs " + amount + " = Rs " + fee);
        }
        System.out.println("Notice: one floorEntry() call replaces a whole if-else chain.");

        // Step 7: LinkedHashMap in ACCESS order + a size limit = an LRU cache.
        step("Step 7: LinkedHashMap as an LRU cache (holds 3 billers)");
        Map<String, String> cache = new LruCache<>(3);
        cache.put("ELEC", "Electricity board");
        cache.put("WATER", "Water board");
        cache.put("GAS", "Gas company");
        System.out.println("after 3 puts    : " + cache.keySet());   // [ELEC, WATER, GAS]
        cache.get("ELEC");                                            // ELEC was just used, so it moves to the end
        System.out.println("after get(ELEC) : " + cache.keySet());   // [WATER, GAS, ELEC]
        cache.put("MOBILE", "Mobile recharge");                       // 4 > 3, so the least recently used goes
        System.out.println("after put MOBILE: " + cache.keySet() + "   (WATER evicted)");
        System.out.println("Notice: the front of the list is always the least recently used entry.");
    }

    /**
     * An LRU cache in a few lines. The "true" makes LinkedHashMap keep ACCESS order,
     * and removeEldestEntry() is called after every put to decide whether to drop
     * the oldest entry.
     */
    static class LruCache<K, V> extends LinkedHashMap<K, V> {
        private static final long serialVersionUID = 1L;       // LinkedHashMap is Serializable, so javac asks for this
        private final int maxSize;

        LruCache(int maxSize) {
            super(16, 0.75f, true);                            // true = access order, not insertion order
            this.maxSize = maxSize;
        }

        @Override
        protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
            return size() > maxSize;                           // too many? drop the least recently used
        }
    }

    static long timeGetByIndex(List<Integer> list, int n) {
        for (int i = 0; i < n; i++) {
            list.add(i);
        }
        long start = System.nanoTime();
        long sum = 0;
        for (int i = 0; i < n; i++) {
            sum += list.get(i);                                // ArrayList jumps, LinkedList walks
        }
        long ms = (System.nanoTime() - start) / 1_000_000;
        if (sum < 0) {
            System.out.println(sum);                           // uses sum so Java can't skip the loop
        }
        return ms;
    }

    static long timeAddAtFront(List<Integer> list, int n) {
        long start = System.nanoTime();
        for (int i = 0; i < n; i++) {
            list.add(0, i);                                    // ArrayList shifts everything, LinkedList just links
        }
        return (System.nanoTime() - start) / 1_000_000;
    }

    static void step(String text) {
        System.out.println();
        System.out.println("=== " + text + " ===");
    }
}
