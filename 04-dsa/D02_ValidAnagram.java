import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/*
 * D02  Valid Anagram                                              Part 4: DSA
 *
 * THE PROBLEM
 *   Two strings are anagrams if they use exactly the same letters, the same
 *   number of times, in any order. Return true if they are.
 *
 *   Our example: "listen" and "silent"  -> true   (l, i, s, t, e, n once each)
 *                "rat" and "car"        -> false  (t vs c)
 *                "aab" and "abb"        -> false  (same letters, different counts!)
 *   Assume lowercase letters a to z, like the classic LeetCode version.
 *
 * HOW TO USE THIS FILE
 *   1. Write your own answer in mySolution() below. Give it 15 minutes first.
 *   2. Run:  java 04-dsa/D02_ValidAnagram.java
 *      It checks your answer on 5 inputs, then shows both approaches working.
 *   3. Read approaches 1 and 2 below, then say the idea and the complexity aloud.
 */
public class D02_ValidAnagram {

    // =========================================================================
    // YOUR TURN
    // =========================================================================
    static boolean mySolution(String s, String t) {
        // TODO: delete the next line and write your answer
        throw new UnsupportedOperationException("not written yet");
    }

    // =========================================================================
    // APPROACH 1: sort both, then compare
    // =========================================================================
    //   Sort the letters of each word. Anagrams become the same word.
    //     "listen" -> e i l n s t
    //     "silent" -> e i l n s t     same -> true
    //     "rat"    -> a r t
    //     "car"    -> a c r           different -> false
    //
    //   Time : O(n log n), because of the sort.
    //   Space: O(n), for the two char arrays.
    static boolean bySorting(String s, String t) {
        if (s.length() != t.length()) {
            return false;                                  // different lengths can never be anagrams
        }
        char[] a = s.toCharArray();
        char[] b = t.toCharArray();
        Arrays.sort(a);
        Arrays.sort(b);
        return Arrays.equals(a, b);
    }

    // =========================================================================
    // WHY THE BETTER WAY EXISTS (the story)
    // =========================================================================
    //   The pain : sorting does more work than the question needs. It puts
    //              every letter in ORDER, which costs O(n log n), but we only
    //              need to know HOW MANY of each letter there are.
    //   The clue : there are only 26 possible letters, so 26 counters are
    //              enough, however long the words are.
    //   The fix  : count instead of sort. One pass, O(n) time, and the space
    //              is always 26 slots, so O(1).
    //   Its limit: 26 slots only work for a to z. For any characters (Hindi,
    //              emoji, uppercase), approach 2b swaps the array for a HashMap.

    // =========================================================================
    // APPROACH 2: count the letters (the answer interviewers want)
    // =========================================================================
    //   Picture a shopkeeper's tally: +1 for every letter of the first word,
    //   -1 for every letter of the second. If every tally ends at 0, the
    //   words used the same letters the same number of times.
    //   int[26] has one slot per letter: slot 0 = 'a', slot 1 = 'b', ...
    //   ch - 'a' gives the slot, so 'e' - 'a' = 4.
    //
    //   Trace for "listen" / "silent":
    //     after +1 for "listen":  e=1 i=1 l=1 n=1 s=1 t=1
    //     after -1 for "silent":  e=0 i=0 l=0 n=0 s=0 t=0   all zero -> true
    //   Trace for "rat" / "car":
    //     after +1 for "rat":     a=1 c=0 r=1 t=1
    //     after -1 for "car":     a=0 c=-1 r=0 t=1           not all zero -> false
    //
    //   Time : O(n). One pass over each word, then 26 checks.
    //   Space: O(1). Always 26 counters, however long the words are.
    static boolean byCounting(String s, String t) {
        if (s.length() != t.length()) {
            return false;
        }
        int[] count = new int[26];
        for (int i = 0; i < s.length(); i++) {
            count[s.charAt(i) - 'a']++;                    // +1 for the first word's letter
            count[t.charAt(i) - 'a']--;                    // -1 for the second word's letter
        }
        for (int c : count) {
            if (c != 0) {
                return false;                              // some letter didn't balance out
            }
        }
        return true;
    }

    /** Approach 2b: the same idea with a HashMap, for ANY characters (Hindi, emoji, digits). */
    static boolean byCountingAnyCharacters(String s, String t) {
        if (s.length() != t.length()) {
            return false;
        }
        Map<Character, Integer> count = new HashMap<>();
        for (char c : s.toCharArray()) {
            count.merge(c, 1, Integer::sum);               // +1
        }
        for (char c : t.toCharArray()) {
            int left = count.merge(c, -1, Integer::sum);   // -1, and get the new value
            if (left < 0) {
                return false;                              // t uses this character more often than s
            }
        }
        return true;                                       // same length and never below 0, so all are 0
    }

    // =========================================================================
    // Run it: the checker, a live trace, and all approaches
    // =========================================================================
    public static void main(String[] args) {
        String[][] inputs = {{"listen", "silent"}, {"rat", "car"}, {"anagram", "nagaram"}, {"aab", "abb"}, {"ab", "abc"}};
        boolean[] expected = {true, false, true, false, false};

        System.out.println("=== Your solution ===");
        int passed = 0;
        for (int i = 0; i < inputs.length; i++) {
            String label = "\"" + inputs[i][0] + "\" vs \"" + inputs[i][1] + "\"";
            try {
                boolean got = mySolution(inputs[i][0], inputs[i][1]);
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
        System.out.println("=== All approaches on the same inputs ===");
        for (String[] pair : inputs) {
            System.out.println("\"" + pair[0] + "\" vs \"" + pair[1] + "\" -> sorting " + bySorting(pair[0], pair[1])
                    + ", counting " + byCounting(pair[0], pair[1])
                    + ", HashMap " + byCountingAnyCharacters(pair[0], pair[1]));
        }

        System.out.println();
        System.out.println("=== Watch the counting approach ===");
        traceCounting("listen", "silent");
        traceCounting("rat", "car");
    }

    /** Prints the non-zero tallies after each word, like the trace in the comments. */
    static void traceCounting(String s, String t) {
        int[] count = new int[26];
        for (char c : s.toCharArray()) {
            count[c - 'a']++;
        }
        System.out.println("after +1 for \"" + s + "\": " + tally(count, s + t));
        for (char c : t.toCharArray()) {
            count[c - 'a']--;
        }
        boolean allZero = Arrays.stream(count).allMatch(c -> c == 0);
        System.out.println("after -1 for \"" + t + "\": " + tally(count, s + t)
                + (allZero ? "   all zero -> true" : "   not all zero -> false"));
    }

    /** Shows the tally for every letter that appears in either word, in a-z order. */
    static String tally(int[] count, String letters) {
        StringBuilder out = new StringBuilder();
        for (char c = 'a'; c <= 'z'; c++) {
            if (letters.indexOf(c) >= 0) {
                out.append(c).append('=').append(count[c - 'a']).append(' ');
            }
        }
        return out.toString().trim();
    }
}

/*
 * =============================================================================
 *  HOW TO EXPLAIN IT IN THE INTERVIEW (talk before you type; cover these points)
 *    1. If the lengths differ, return false straight away.
 *    2. Simple way: sort both strings and compare. O(n log n).
 *    3. Better: count. An int[26] array, +1 for each letter of the first word,
 *       -1 for each letter of the second. Anagrams leave every count at 0.
 *    4. O(n) time and O(1) space, because the array is always 26 long.
 *       For any characters (not just a-z), use a HashMap instead.
 *
 *  Here's how it can sound:
 *    "First, if the lengths are different they can't be anagrams. Then I count
 *     letters with an int array of 26: for each position I add one for the
 *     letter in the first string and subtract one for the letter in the second.
 *     If every count ends at zero, both strings have the same letters the same
 *     number of times. For listen and silent everything cancels out; for rat
 *     and car, c goes to minus one. That's O(n) time and constant space.
 *     Sorting both strings also works, but it's O(n log n)."
 *
 *  FOLLOW-UPS
 *    Q: What if the input has Unicode, uppercase or spaces?
 *    A: Use a HashMap<Character, Integer> instead of int[26] (approach 2b).
 *       Lowercase the strings or strip the spaces first if the rules say so.
 *    Q: Why is it O(1) space if we build an array?
 *    A: The array is always 26 slots, whether the words have 6 letters or 6 lakh.
 *    Q: Group a list of words into anagram groups?
 *    A: Use the sorted word as a HashMap key: "eat", "tea" and "ate" all become
 *       "aet". It's groupingBy from S00.
 *    Q: With streams?
 *    A: Build the character counts of both words (S01 problem 1) and compare the
 *       two maps with equals(). It's short but slower than int[26].
 *
 *  SELF-CHECK (answers at the very bottom)
 *    1. Why does "aab" vs "abb" catch a wrong solution?
 *    2. What does 'd' - 'a' give?
 *    3. What are the time and space of the counting approach?
 *    4. "night" vs "thing": true or false?
 * =============================================================================
 *  Answers: 1) both use the letters {a, b}, so a set-based check says true, but the counts differ
 *           2) 3 (slot 3)   3) O(n) time, O(1) space   4) true
 */

/*
 * QUICK REVISION START
 * D02 Valid Anagram: do two strings use the same letters, the same number of times?
 *   Story   : sorting puts every letter in order, O(n log n), but we only need counts -> only 26 letters
 *             exist -> an int[26] tally: O(n) time, O(1) space -> any characters? use a HashMap
 *   Idea    : a tally in int[26]: +1 for each letter of s, -1 for each letter of t; all zero = anagram
 *   Picture : "listen" / "silent"   e i l n s t : +1 each, then -1 each -> all 0 -> true
 *             "rat" / "car"          c ends at -1, t ends at +1         -> false
 *   Cost    : sort both and compare O(n log n) | counting O(n) time, O(1) space (always 26 slots)
 *   Traps   : check the lengths first | "aab" vs "abb" breaks set-based answers | Unicode or mixed case -> HashMap
 *   30-second answer: "If the lengths differ it's false. Otherwise I count letters in an int array of 26:
 *     plus one for the first string, minus one for the second. If every count is zero they're anagrams.
 *     O(n) time and constant space. Sorting both also works but is O(n log n)."
 *   Memory hook: a shopkeeper's tally: add for s, subtract for t, the books must balance to zero.
 * QUICK REVISION END
 */
