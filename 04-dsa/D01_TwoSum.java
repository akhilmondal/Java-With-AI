import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/*
 * D01  Two Sum                                                    Part 4: DSA
 *
 * THE PROBLEM
 *   You get an array of numbers and a target. Return the positions (indices)
 *   of the two numbers that add up to the target. There is exactly one answer,
 *   and you can't use the same position twice.
 *
 *   Our example: bill amounts [200, 450, 700, 250], wallet balance 900.
 *   Which two bills use up exactly 900?  200 + 700 = 900, at positions 0 and 2.
 *   Answer: [0, 2]
 *
 * HOW TO USE THIS FILE
 *   1. Write your own answer in mySolution() below. Give it 15 minutes first.
 *   2. Run:  java 04-dsa/D01_TwoSum.java
 *      It checks your answer on 4 inputs, then shows both approaches working.
 *   3. Read approaches 1 and 2 below, then say the idea and the complexity aloud.
 */
public class D01_TwoSum {

    // =========================================================================
    // YOUR TURN
    // =========================================================================
    static int[] mySolution(int[] nums, int target) {
        // TODO: delete the next line and write your answer
        throw new UnsupportedOperationException("not written yet");
    }

    // =========================================================================
    // APPROACH 1: brute force, try every pair
    // =========================================================================
    //   For each number, check it against every number after it.
    //
    //   Trace for [200, 450, 700, 250], target 900:
    //     (0,1)  200 + 450 = 650    no
    //     (0,2)  200 + 700 = 900    yes -> [0, 2]
    //
    //   Time : O(n^2). n numbers make about n x n / 2 pairs.
    //          10,000 numbers is about 5 crore pairs.
    //   Space: O(1). Nothing extra is stored.
    static int[] bruteForce(int[] nums, int target) {
        for (int i = 0; i < nums.length; i++) {
            for (int j = i + 1; j < nums.length; j++) {      // j starts after i: never the same position twice
                if (nums[i] + nums[j] == target) {
                    return new int[] {i, j};
                }
            }
        }
        return new int[0];                                    // no pair (the problem says this won't happen)
    }

    // =========================================================================
    // WHY THE BETTER WAY EXISTS (the story)
    // =========================================================================
    //   The pain : brute force asks every pair "do we add up to 900?". For
    //              10,000 bills that's about 5 crore questions. The timing at
    //              the end of main() shows the cost on 20,000 numbers.
    //   The waste: each bill already KNOWS the partner it needs (900 - 200 = 700).
    //              We just can't find that partner fast in a plain array.
    //   The fix  : remember every bill we've seen in a HashMap. "Is 700 there?"
    //              is then ONE lookup (J01), not a scan of the whole array.
    //   The trade: we spend O(n) extra memory to save a full scan per number.

    // =========================================================================
    // APPROACH 2: a HashMap, one pass (the answer interviewers want)
    // =========================================================================
    //   Picture a register at the door. Each bill walks in and asks: "is my
    //   partner (the amount I need to reach 900) already in the register?"
    //   If yes, we're done. If not, the bill writes itself into the register.
    //
    //   Trace for [200, 450, 700, 250], target 900:
    //     i=0  200: need 700. register []              -> not there, write 200 -> 0
    //     i=1  450: need 450. register [200]           -> not there, write 450 -> 1
    //     i=2  700: need 200. register [200, 450]      -> FOUND at 0 -> [0, 2]
    //
    //   Time : O(n). One pass, and each HashMap lookup is O(1) on average (J01).
    //   Space: O(n). The register can hold up to n numbers.
    static int[] withHashMap(int[] nums, int target) {
        Map<Integer, Integer> seen = new HashMap<>();         // number -> its position
        for (int i = 0; i < nums.length; i++) {
            int need = target - nums[i];                      // the partner we're looking for
            if (seen.containsKey(need)) {
                return new int[] {seen.get(need), i};
            }
            // Check FIRST, then add. Otherwise, for [3, 2, 4] with target 6,
            // the 3 would find itself and answer [0, 0].
            seen.put(nums[i], i);
        }
        return new int[0];
    }

    // =========================================================================
    // Run it: the checker, a live trace, and a timing
    // =========================================================================
    public static void main(String[] args) {
        int[][] inputs = {{200, 450, 700, 250}, {2, 7, 11, 15}, {3, 2, 4}, {3, 3}};
        int[] targets = {900, 9, 6, 6};
        String[] expected = {"[0, 2]", "[0, 1]", "[1, 2]", "[0, 1]"};

        System.out.println("=== Your solution ===");
        int passed = 0;
        for (int t = 0; t < inputs.length; t++) {
            String label = Arrays.toString(inputs[t]) + ", target " + targets[t];
            try {
                String got = sorted(mySolution(inputs[t], targets[t]));
                boolean ok = got.equals(expected[t]);
                passed += ok ? 1 : 0;
                System.out.println((ok ? "[DONE] " : "[WRONG] ") + label + " -> " + got
                        + (ok ? "" : "   expected " + expected[t]));
            } catch (UnsupportedOperationException e) {
                System.out.println("[TODO] " + label + " -> not written yet (expected " + expected[t] + ")");
            }
        }
        System.out.println("Score: " + passed + "/" + inputs.length);

        System.out.println();
        System.out.println("=== Both approaches on the same inputs ===");
        for (int t = 0; t < inputs.length; t++) {
            System.out.println(Arrays.toString(inputs[t]) + ", target " + targets[t]
                    + " -> brute force " + sorted(bruteForce(inputs[t], targets[t]))
                    + ", HashMap " + sorted(withHashMap(inputs[t], targets[t])));
        }

        System.out.println();
        System.out.println("=== Watch the HashMap approach, step by step ===");
        traceWithHashMap(new int[] {200, 450, 700, 250}, 900);

        System.out.println();
        System.out.println("=== O(n^2) vs O(n) on 20,000 numbers (the pair is at the very end) ===");
        int n = 20_000;
        int[] big = new int[n];
        for (int i = 0; i < n - 2; i++) {
            big[i] = i;                                       // 0, 1, 2, ...: no two of these reach the target
        }
        big[n - 2] = 1_000_000;
        big[n - 1] = 2_000_000;                               // the only pair: the last two numbers
        long start = System.nanoTime();
        int[] slow = bruteForce(big, 3_000_000);
        long bruteMs = (System.nanoTime() - start) / 1_000_000;
        start = System.nanoTime();
        int[] fast = withHashMap(big, 3_000_000);
        long mapMs = (System.nanoTime() - start) / 1_000_000;
        System.out.println("brute force: " + sorted(slow) + " in " + bruteMs + " ms  (about 20 crore pairs checked)");
        System.out.println("HashMap    : " + sorted(fast) + " in " + mapMs + " ms  (20,000 steps)");
        System.out.println("(your times will differ; the gap won't)");
    }

    /** The same as withHashMap(), but it prints every step. */
    static void traceWithHashMap(int[] nums, int target) {
        // A LinkedHashMap here only so the register prints in arrival order (J04);
        // the real solution above uses a plain HashMap.
        Map<Integer, Integer> seen = new LinkedHashMap<>();
        for (int i = 0; i < nums.length; i++) {
            int need = target - nums[i];
            System.out.print("i=" + i + "  " + nums[i] + ": need " + need + ". register " + seen.keySet());
            if (seen.containsKey(need)) {
                System.out.println(" -> FOUND at " + seen.get(need) + " -> [" + seen.get(need) + ", " + i + "]");
                return;
            }
            seen.put(nums[i], i);
            System.out.println(" -> not there, write " + nums[i] + " -> " + i);
        }
    }

    /** The two positions in increasing order, so [2, 0] and [0, 2] count as the same answer. */
    static String sorted(int[] answer) {
        int[] copy = answer.clone();
        Arrays.sort(copy);
        return Arrays.toString(copy);
    }
}

/*
 * =============================================================================
 *  HOW TO EXPLAIN IT IN THE INTERVIEW (talk before you type; cover these points)
 *    1. Brute force: check every pair with two loops. O(n^2) time, O(1) space.
 *    2. Better: for each number x, the partner I need is target - x. I keep a
 *       HashMap of the numbers I've already seen, with their index.
 *    3. For each number: if its partner is in the map, return both indices;
 *       otherwise put the number in the map. Check first, then add, so a number
 *       never pairs with itself.
 *    4. One pass: O(n) time, O(n) extra space. I trade memory for speed.
 *
 *  Here's how it can sound:
 *    "The brute force is two nested loops over all pairs, which is O(n squared).
 *     I can do it in one pass with a HashMap: for each number I compute the
 *     complement, target minus the number, and check if I've already seen it.
 *     If yes, I return the stored index and the current one. If not, I store
 *     the current number with its index. For 200, 450, 700 and target 900,
 *     when I reach 700 I need 200, which I stored at index 0, so the answer is
 *     0 and 2. That's O(n) time and O(n) space."
 *
 *  FOLLOW-UPS
 *    Q: The array is already sorted. Can you do it without extra space?
 *    A: Two pointers: one at the start, one at the end. If the sum is too
 *       small, move the left one right. If it's too big, move the right one
 *       left. O(n) time, O(1) space.
 *    Q: Why not sort first and use two pointers anyway?
 *    A: Sorting is O(n log n), and it scrambles the original positions, so
 *       you'd have to carry (value, index) pairs along.
 *    Q: Duplicates, like [3, 3] with target 6?
 *    A: That works, because we check before adding: the second 3 finds the
 *       first one.
 *    Q: Three numbers that add up to the target (3Sum)?
 *    A: Sort, then for each number run two pointers on the rest. O(n^2).
 *
 *  SELF-CHECK (answers at the very bottom)
 *    1. [200, 450, 700, 250] with target 950: what's the answer?
 *    2. Why do we check the map BEFORE adding the current number?
 *    3. About how many pairs does brute force check for 10,000 numbers?
 *    4. What are the time and space of the HashMap approach?
 * =============================================================================
 *  Answers: 1) [2, 3], because 700 + 250 = 950
 *           2) so a number never pairs with itself: [3, 2, 4] with target 6 would give [0, 0]
 *           3) about 5 crore (10,000 x 9,999 / 2)   4) O(n) time, O(n) space
 */

/*
 * QUICK REVISION START
 * D01 Two Sum: return the indices of the two numbers that add up to the target.
 *   Story   : checking every pair is about 5 crore checks for 10,000 numbers -> but each number already
 *             knows its partner (target - x) -> a HashMap finds that partner in one lookup: O(n)
 *   Idea    : for each number x, look for its partner (target - x) in a HashMap of the numbers seen so far.
 *   Picture : [200, 450, 700, 250], target 900
 *               200 -> need 700 -> not seen -> remember 200 at 0
 *               450 -> need 450 -> not seen -> remember 450 at 1
 *               700 -> need 200 -> SEEN at 0 -> answer [0, 2]
 *   Cost    : brute force (every pair) O(n^2) time, O(1) space | HashMap O(n) time, O(n) space
 *   Trap    : check the map BEFORE adding x, or [3, 2, 4] with target 6 answers [0, 0]
 *   Sorted input? two pointers from both ends: O(n) time, O(1) space
 *   30-second answer: "Brute force checks every pair, O(n squared). In one pass with a HashMap, for each
 *     number I look up target minus it among the numbers already seen. If it's there I return both
 *     indices, otherwise I store the number with its index. O(n) time, O(n) space."
 *   Memory hook: a register at the door; each bill asks "is my partner already inside?"
 * QUICK REVISION END
 */
