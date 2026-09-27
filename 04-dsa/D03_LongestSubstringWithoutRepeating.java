import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/*
 * D03  Longest Substring Without Repeating Characters            Part 4: DSA
 *
 * THE PROBLEM
 *   Return the length of the longest substring (a continuous piece of the
 *   string) that has no repeated character.
 *
 *   Our example: "pwwkew"  -> 3   ("wke": w, k, e are all different)
 *   Also:        "abcabcbb" -> 3  ("abc"),  "bbbbb" -> 1  ("b"),  "" -> 0
 *   Careful: "pwke" is not a substring of "pwwkew" (the letters must be next
 *   to each other).
 *
 * HOW TO USE THIS FILE
 *   1. Write your own answer in mySolution() below. Give it 20 minutes first.
 *   2. Run:  java 04-dsa/D03_LongestSubstringWithoutRepeating.java
 *      It checks your answer on 7 inputs, then shows both approaches working.
 *   3. Read approaches 1 and 2 below, then say the idea and the complexity aloud.
 */
public class D03_LongestSubstringWithoutRepeating {

    // =========================================================================
    // YOUR TURN
    // =========================================================================
    static int mySolution(String s) {
        // TODO: delete the next line and write your answer
        throw new UnsupportedOperationException("not written yet");
    }

    // =========================================================================
    // APPROACH 1: brute force, start at every position
    // =========================================================================
    //   From each start position, keep adding characters until one repeats.
    //   Trace for "pwwkew":
    //     start 0: p, w, then w repeats          -> "pw"   = 2
    //     start 1: w, then w repeats             -> "w"    = 1
    //     start 2: w, k, e, then w repeats       -> "wke"  = 3   <- best
    //     start 3: k, e, w, end                  -> "kew"  = 3
    //     start 4: e, w                          -> "ew"   = 2
    //     start 5: w                             -> "w"    = 1
    //
    //   Time : O(n^2). n starts, and each can walk up to n characters.
    //   Space: O(k), where k is the number of different characters (the set).
    static int bruteForce(String s) {
        int best = 0;
        for (int start = 0; start < s.length(); start++) {
            Set<Character> seen = new HashSet<>();
            for (int end = start; end < s.length(); end++) {
                if (!seen.add(s.charAt(end))) {
                    break;                                   // add() is false: this character repeats
                }
                best = Math.max(best, end - start + 1);
            }
        }
        return best;
    }

    // =========================================================================
    // APPROACH 2: a sliding window (the answer interviewers want)
    // =========================================================================
    //   Picture a bank queue with one rule: no two people with the same name.
    //   New people join at the back (right). If a newcomer's name is already in
    //   the queue, everyone from the front up to and including that old person
    //   leaves (left jumps past them). We track the longest queue we ever saw.
    //   We remember the LAST position of every character, so left can jump
    //   there in one step.
    //
    //   Trace for "pwwkew"  (positions 0..5: p w w k e w)
    //     right=0 'p': new                    window "p"    (0..0)  length 1  best 1
    //     right=1 'w': new                    window "pw"   (0..1)  length 2  best 2
    //     right=2 'w': last seen at 1, inside -> left = 2
    //                                         window "w"    (2..2)  length 1  best 2
    //     right=3 'k': new                    window "wk"   (2..3)  length 2  best 2
    //     right=4 'e': new                    window "wke"  (2..4)  length 3  best 3
    //     right=5 'w': last seen at 2, inside -> left = 3
    //                                         window "kew"  (3..5)  length 3  best 3
    //   Answer: 3
    //
    //   Time : O(n). right visits each character once; left only moves forward.
    //   Space: O(k) for the map of last positions (k = number of different characters).
    static int slidingWindow(String s) {
        Map<Character, Integer> lastSeen = new HashMap<>();  // character -> last position
        int best = 0;
        int left = 0;                                        // the window is s[left .. right]
        for (int right = 0; right < s.length(); right++) {
            char c = s.charAt(right);
            if (lastSeen.containsKey(c)) {
                // Jump left past the old copy, but NEVER move left backwards:
                // an old copy that's already outside the window doesn't matter.
                left = Math.max(left, lastSeen.get(c) + 1);
            }
            lastSeen.put(c, right);
            best = Math.max(best, right - left + 1);
        }
        return best;
    }

    /**
     * The classic BUG, shown on purpose: it forgets Math.max, so left can move
     * backwards. On "abba", the second 'a' pulls left back to 1, and it
     * reports "bba" (3) instead of 2.
     */
    static int slidingWindowWithoutMax(String s) {
        Map<Character, Integer> lastSeen = new HashMap<>();
        int best = 0;
        int left = 0;
        for (int right = 0; right < s.length(); right++) {
            char c = s.charAt(right);
            if (lastSeen.containsKey(c)) {
                left = lastSeen.get(c) + 1;                  // BUG: can move left backwards
            }
            lastSeen.put(c, right);
            best = Math.max(best, right - left + 1);
        }
        return best;
    }

    // =========================================================================
    // Run it: the checker, a live trace, and the classic bug
    // =========================================================================
    public static void main(String[] args) {
        String[] inputs = {"pwwkew", "abcabcbb", "bbbbb", "", "abba", " ", "dvdf"};
        int[] expected = {3, 3, 1, 0, 2, 1, 3};

        System.out.println("=== Your solution ===");
        int passed = 0;
        for (int i = 0; i < inputs.length; i++) {
            String label = "\"" + inputs[i] + "\"";
            try {
                int got = mySolution(inputs[i]);
                boolean ok = got == expected[i];
                passed += ok ? 1 : 0;
                System.out.println((ok ? "[DONE] " : "[WRONG] ") + label + " -> " + got
                        + (ok ? "" : "   expected " + expected[i]));
            } catch (UnsupportedOperationException e) {
                System.out.println("[TODO] " + label + " -> not written yet (expected " + expected[i] + ")");
            }
        }
        System.out.println("Score: " + passed + "/" + inputs.length);

        System.out.println();
        System.out.println("=== Both approaches on the same inputs ===");
        for (String s : inputs) {
            System.out.println("\"" + s + "\" -> brute force " + bruteForce(s) + ", sliding window " + slidingWindow(s));
        }

        System.out.println();
        System.out.println("=== Watch the sliding window on \"pwwkew\" ===");
        traceSlidingWindow("pwwkew");

        System.out.println();
        System.out.println("=== The classic bug: forgetting Math.max(left, ...) ===");
        System.out.println("\"abba\" with Math.max    -> " + slidingWindow("abba") + "  (right: \"ab\" or \"ba\")");
        System.out.println("\"abba\" without Math.max -> " + slidingWindowWithoutMax("abba") + "  (WRONG: \"bba\" repeats b)");
    }

    /** The same as slidingWindow(), but it prints every step. */
    static void traceSlidingWindow(String s) {
        Map<Character, Integer> lastSeen = new HashMap<>();
        int best = 0;
        int left = 0;
        for (int right = 0; right < s.length(); right++) {
            char c = s.charAt(right);
            String action = "new";
            if (lastSeen.containsKey(c)) {
                int newLeft = Math.max(left, lastSeen.get(c) + 1);
                action = "last seen at " + lastSeen.get(c) + " -> left = " + newLeft;
                left = newLeft;
            }
            lastSeen.put(c, right);
            best = Math.max(best, right - left + 1);
            System.out.printf("right=%d '%c': %-26s window \"%s\" (%d..%d) length %d, best %d%n",
                    right, c, action, s.substring(left, right + 1), left, right, right - left + 1, best);
        }
        System.out.println("Answer: " + best);
    }
}

/*
 * =============================================================================
 *  HOW TO EXPLAIN IT IN THE INTERVIEW (talk before you type; cover these points)
 *    1. Brute force: start at every position and extend until a character
 *       repeats. O(n^2).
 *    2. Better: a sliding window [left, right] that always has unique characters.
 *       right moves forward one step at a time.
 *    3. A HashMap keeps the last position of each character. When the new
 *       character was seen inside the window, left jumps to just after that
 *       position, using Math.max so left never moves backwards.
 *    4. After each step, best = max(best, right - left + 1).
 *    5. O(n) time, because each character is visited once, and O(k) space for
 *       the map, where k is the number of different characters.
 *
 *  Here's how it can sound:
 *    "I'll use a sliding window. I move right one character at a time and keep
 *     a map of each character's last position. If the character at right was
 *     already seen inside the current window, I move left to just after its
 *     last position. I take the max with the current left, so the window never
 *     goes backwards. After every step I update the best length with right
 *     minus left plus one. For pwwkew, the second w pushes left to 2, and the
 *     window grows to wke, length 3. It's O(n) time and O(k) space."
 *
 *  FOLLOW-UPS
 *    Q: Why Math.max(left, ...)?
 *    A: The old copy may already be outside the window. On "abba", the second
 *       'a' was last seen at 0, but left is already 2, so moving to 1 would
 *       bring a repeated 'b' back in. The demo shows the bug giving 3.
 *    Q: Can you do it without a HashMap?
 *    A: For plain ASCII text, an int[128] of last positions (filled with -1)
 *       works the same way and is faster.
 *    Q: Return the substring, not just its length?
 *    A: Also remember the left position whenever best improves, then take
 *       substring(bestLeft, bestLeft + best).
 *    Q: Where else does the sliding window idea apply?
 *    A: Any "longest or shortest continuous part with a rule" problem, like the
 *       maximum sum of k consecutive payments.
 *
 *  SELF-CHECK (answers at the very bottom)
 *    1. "abcabcbb": what's the answer, and one substring that gives it?
 *    2. "dvdf": why is the answer 3 and not 2?
 *    3. What goes wrong on "abba" without Math.max?
 *    4. What are the time and space of the sliding window?
 * =============================================================================
 *  Answers: 1) 3, from "abc" (or "bca", or "cab")
 *           2) "vdf" is valid: when the second 'd' arrives, left moves just
 *              past the first 'd', not all the way to the new 'd'
 *           3) left moves back to 1, and it wrongly counts "bba" as 3
 *           4) O(n) time, O(k) space (k = number of different characters)
 */

/*
 * QUICK REVISION START
 * D03 Longest Substring Without Repeating Characters: the longest run with no repeated character.
 *   Idea    : a sliding window [left, right] that always holds unique characters, plus a map of each
 *             character's LAST position. On a repeat inside the window: left = max(left, last + 1).
 *   Picture : p w w k e w
 *             [p w]           the second w repeats -> left jumps to 2
 *                 [w k e]     length 3 -> best 3
 *                   [k e w]   length 3 (the last w pushed left to 3)
 *   Cost    : brute force O(n^2) | sliding window O(n) time, O(k) space (k = different characters)
 *   Trap    : forget Math.max -> "abba" gives 3 ("bba") instead of 2
 *   Tests   : "abcabcbb" 3 | "bbbbb" 1 | "" 0 | " " 1 | "dvdf" 3 | "abba" 2
 *   30-second answer: "I slide a window with two pointers and keep each character's last index in a map.
 *     When the right character was already seen inside the window, I move left to just after that index,
 *     using max so it never goes back. Best = max(best, right - left + 1). O(n) time."
 *   Memory hook: a bank queue with a no-same-name rule; people leave from the front until the old namesake is gone.
 * QUICK REVISION END
 */
