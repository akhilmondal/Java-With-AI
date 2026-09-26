# J04 · ArrayList vs LinkedList, and HashMap vs LinkedHashMap vs TreeMap

**Read this first (12 min). Then run [J04_ListsAndMaps.java](J04_ListsAndMaps.java) to watch each step happen.**

Don't memorize sentences. Understand the 7 steps and two small examples:
- five transaction IDs **T1 to T5** for the lists,
- four employee IDs **250, 42, 305, 101** for the maps.

Once you get those, you can pick the right collection and defend the choice.

---

## The problem

"Which list or map would you use, and why?" is really a question about **what each one is good and bad at**. There are two families here:

- **Lists:** `ArrayList` vs `LinkedList`. They differ in how the items are stored, so some operations are fast in one and slow in the other.
- **Maps:** `HashMap` vs `LinkedHashMap` vs `TreeMap`. They differ in the **order** you get the keys back in, and in speed.

## Real-life pictures

**ArrayList is a row of numbered cinema seats.** You can walk straight to seat 3. But if someone must sit in seat 0, everyone in the row shifts one seat to the right. When the row is full, everyone moves to a bigger hall, one and a half times the size.

**LinkedList is a train of coaches.** Each coach is linked to the one before it and the one after it. Attaching a coach at the front or the back is quick. But to reach the fourth coach, you walk through the first three.

**The three maps:**
- **HashMap** is the cupboard with drawers from J01. It's fast, but you get the files back in drawer order, which looks random.
- **LinkedHashMap** is the same cupboard plus a register that records the order the files arrived in.
- **TreeMap** is a telephone directory. It's always sorted, and finding a name takes a few "higher or lower" steps (J01's guessing game).

| Real life | Java |
|---|---|
| cinema seats in a row | `ArrayList` (an array inside) |
| train coaches linked together | `LinkedList` (nodes with prev/next links) |
| cupboard with drawers | `HashMap` |
| cupboard plus an arrival register | `LinkedHashMap` |
| telephone directory | `TreeMap` (sorted, a red-black tree inside) |

---

## Part A: lists

### Step 1 · ArrayList: an array inside

```text
index:   [0]  [1]  [2]  [3]  [4]  [5] ... [9]
          T1   T2   T3   T4   T5   (empty seats: capacity 10)
```

- **`get(3)`** jumps straight to seat 3 and returns T4. That's **O(1)**, one step.
- **`add("T6")` at the end** takes the next empty seat. That's O(1).
- **`add(0, "T0")` at the front**: T1 to T5 each shift one seat to the right, which is **5 moves**. With n items, that's **O(n)**. Removing from the front shifts everything left, the same way.
- **When the array is full**, ArrayList creates a new array **1.5 times bigger** and copies every item over: **10 → 15 → 22 → 33 → 49**. The new size is the old size plus half of it (10 + 5 = 15, 15 + 7 = 22, 22 + 11 = 33). The first array of 10 is only created at the first `add()`.

### Step 2 · LinkedList: coaches with links

```text
null <- [T1] <-> [T2] <-> [T3] <-> [T4] <-> [T5] -> null
        first                                last
```

- **`addFirst("T0")` or `addLast("T6")`** just attaches a node at the front or back. Nothing shifts, so it's **O(1)**.
- After those two adds, the list is [T0, T1, T2, T3, T4, T5, T6]. **`get(3)`** has to walk from the nearer end: T0, then T1, then T2, then T3. That's **O(n)**.
- **Adding in the middle** is O(n) to walk to the spot, then O(1) to link the new node.
- **Memory:** every item sits inside a node that also stores two links (prev and next), so a LinkedList uses much more memory than an ArrayList.

### Step 3 · Measure it (the demo does this)

| Test | ArrayList | LinkedList |
|---|---|---|
| `get(i)` for every i, 20,000 items | 0 to 1 ms (jumps) | about 120 ms (walks every time) |
| `add(0, x)` 30,000 times | about 22 ms (shifts everything) | 2 to 3 ms (just links) |

These times come from three runs on this laptop. Yours will be different, but the same list will win each test.

### Step 4 · Which list to pick

| | get by index | add at the end | add or remove at the front | memory |
|---|---|---|---|---|
| ArrayList | **O(1)** | O(1) usually | O(n), everything shifts | less |
| LinkedList | O(n), walks | O(1) | **O(1)** | more (2 links per item) |

> **ArrayList is the default.** Most code adds at the end and reads by index or loops over the list.
> LinkedList is rarely the right answer. Even for a queue or a stack, `ArrayDeque` is usually better.

---

## Part B: maps

### Step 5 · Same keys, three maps, three orders

Put the employee IDs **250, 42, 305, 101** (in that order) into each map, then print the keys:

| Map | Printed order | Why |
|---|---|---|
| HashMap | **[305, 101, 250, 42]** | bucket order (J01): 305 % 16 = 1, 101 % 16 = 5, and 250 and 42 both give 10 |
| LinkedHashMap | **[250, 42, 305, 101]** | the order they were put in |
| TreeMap | **[42, 101, 250, 305]** | sorted by key |

> **HashMap has no order you can rely on. LinkedHashMap remembers arrival order. TreeMap keeps keys sorted.**

### Step 6 · TreeMap's range methods (useful in payments)

TreeMap is sorted, so it can answer "nearest key" questions:

- `firstKey()` returns 42 and `lastKey()` returns 305.
- `floorKey(200)` returns **101**, the biggest key that's 200 or less.
- `ceilingKey(200)` returns **250**, the smallest key that's 200 or more.
- `headMap(250)` returns {42, 101}, all the keys below 250.

**A real use: fee slabs.** Fees start at an amount, like this: from ₹0 the fee is ₹0, from ₹1,000 it's ₹5, from ₹5,000 it's ₹10, and from ₹10,000 it's ₹15.

| Amount | `floorKey(amount)` | Fee |
|---|---|---|
| ₹999 | 0 | ₹0 |
| ₹7,200 | 5,000 | ₹10 |
| ₹10,000 | 10,000 | ₹15 |

It's one line of code with no if-else chain: `slabs.floorEntry(amount).getValue()`.

### Step 7 · LinkedHashMap as an LRU cache

LinkedHashMap can keep **access order** instead of insertion order: every `get` moves that key to the end. Add a size limit and you have an **LRU cache** (Least Recently Used, so the entry unused for the longest time gets evicted first).

Here's a cache of biller details that holds **3** billers:

1. Put ELEC, WATER and GAS. The order is [ELEC, WATER, GAS].
2. Call `get("ELEC")`. ELEC was just used, so it moves to the end: [WATER, GAS, ELEC].
3. Put MOBILE. Now there are 4 entries, but the limit is 3, so the least recently used one, **WATER**, is evicted: [GAS, ELEC, MOBILE].

### The maps in one table

| | Order | get/put | null key | Inside |
|---|---|---|---|---|
| HashMap | none (bucket order) | O(1) | one allowed | array of buckets |
| LinkedHashMap | insertion (or access) | O(1) | one allowed | HashMap plus a linked list through the entries |
| TreeMap | sorted by key | O(log n) | **not allowed** | red-black tree |

The same three orders exist for sets: `HashSet`, `LinkedHashSet` and `TreeSet`. Each one uses the matching map inside.

---

## How to explain it in the interview

Use your own words. Cover these points in this order, using T1 to T5 and the four employee IDs:

1. **ArrayList** is a resizable array. `get(i)` is O(1). Adding or removing at the front or in the middle is O(n) because items shift. It grows by 1.5 times.
2. **LinkedList** is a doubly linked list. Adding or removing at the ends is O(1), but `get(i)` is O(n) because it walks. It uses more memory per item.
3. In practice, **ArrayList is the default**. LinkedList is rare, and ArrayDeque is better for queues.
4. **HashMap** has no order and is O(1). **LinkedHashMap** keeps insertion order, or access order for an LRU cache. **TreeMap** keeps keys sorted, is O(log n) and has floor/ceiling range methods.
5. Pick by need: plain speed → HashMap, arrival order → LinkedHashMap, sorted keys or ranges → TreeMap.

**Here's how it can sound** (about a minute, simple words):

> "ArrayList is backed by an array, so get by index is O(1). But inserting at the front shifts every element, so that's O(n), and when it's full it grows by 50% and copies everything. LinkedList is a doubly linked list, so adding or removing at the ends is O(1), but get by index has to walk the nodes, which is O(n), and each node carries two extra links. In practice I use ArrayList almost always. For maps, HashMap gives O(1) with no ordering. LinkedHashMap keeps insertion order, and with access order it can work as an LRU cache. TreeMap keeps keys sorted in a red-black tree, so operations are O(log n), and it has methods like floorKey, which I'd use for things like fee slabs by amount."

**Tip:** if they ask "which would you use for X", say what X needs first (order? sorted? fast lookup?), then name the class.

---

## Follow-up questions (simple answers)

**ArrayList vs Vector?**
Vector is the old version: every method is synchronized, and it doubles in size when full. Use ArrayList, or a concurrent collection if threads share the list.

**What's the default capacity of an ArrayList?**
10, but the array is only created at the first `add()`. Then it grows by 1.5 times: 15, 22, 33, 49 and so on.

**Why is ArrayList often faster than LinkedList, even with some shifting?**
An array's items sit next to each other in memory, so the CPU reads them quickly, and Java moves blocks of them in one go. A LinkedList's nodes are scattered, and it has to walk node by node to reach a position.

**When would you really use LinkedList?**
Rarely: when you add or remove at both ends a lot and never access by index. Even then, `ArrayDeque` is usually faster.

**Arrays.asList vs List.of?**
`Arrays.asList` has a fixed size: `set()` works, but `add()` and `remove()` throw UnsupportedOperationException. `List.of` is fully unmodifiable and doesn't allow nulls.

**Can TreeMap have a null key?**
No. It has to compare keys to sort them, so a null key throws NullPointerException. Keys must be Comparable, or you must give it a Comparator (J08).

**How does LinkedHashMap remember the order?**
Every entry also sits in a doubly linked list (links to the previous and next entry) in arrival order. That costs 2 extra links per entry.

*Only if they push further:* Java 21 added `SequencedCollection` and `SequencedMap`. They give `getFirst()`, `getLast()` and `reversed()` on List, Deque, LinkedHashMap, TreeMap and similar classes.

---

## Numbers to remember

| What | Value |
|---|---|
| ArrayList default capacity | 10 (created at the first add) |
| ArrayList growth | 1.5×: 10 → 15 → 22 → 33 → 49 |
| ArrayList `get(i)` / `add(0, x)` | O(1) / O(n) |
| LinkedList `get(i)` / `addFirst` | O(n) / O(1) |
| HashMap / LinkedHashMap / TreeMap get | O(1) / O(1) / O(log n) |
| TreeMap null key | not allowed |

## Self-check (answer aloud, then click to check)

<details><summary>1. An ArrayList holds 5 items. How many items move when you call add(0, x)?</summary>

All 5. Each one shifts one place to the right.

</details>

<details><summary>2. An ArrayList starts with capacity 10. What are the next three capacities?</summary>

15, 22 and 33. Each time, it's the old size plus half of it.

</details>

<details><summary>3. A LinkedList has 1,000 items. Roughly how many hops does get(500) take?</summary>

About 500. It walks from the nearer end, and 500 is the middle.

</details>

<details><summary>4. You put the keys 250, 42, 305 and 101 into each map. In what order does each map print them?</summary>

HashMap prints [305, 101, 250, 42] (bucket order), LinkedHashMap prints [250, 42, 305, 101] (insertion order), and TreeMap prints [42, 101, 250, 305] (sorted).

</details>

<details><summary>5. The fee slabs are ₹0→0, ₹1,000→5, ₹5,000→10 and ₹10,000→15. What's the fee for ₹4,999?</summary>

floorKey(4999) is 1000, so the fee is ₹5.

</details>

<details><summary>6. An LRU cache holds 3 billers. You put ELEC, WATER and GAS, call get WATER, then put MOBILE. Which one is evicted?</summary>

ELEC. After get WATER the order is [ELEC, GAS, WATER], so ELEC is the least recently used when MOBILE arrives.

</details>

<details><summary>7. Which map would you use to show recent transactions in the order they happened?</summary>

LinkedHashMap, because it keeps insertion order. A List works too, if you don't need to look them up by key.

</details>

If you get stuck on any of them, add it to [STUMBLE-LIST.md](../STUMBLE-LIST.md). When all 7 feel easy, tick J04 in the [README](../README.md) and send `next`.
