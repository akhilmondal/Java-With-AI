# J01 · How HashMap works inside

**Read this first (10 min). Then run [J01_HashMapInternals.java](J01_HashMapInternals.java) to watch each step happen.**

Don't memorize sentences. Understand the 8 steps and the example with keys **101** and **117**. Once you get those, you can explain HashMap in your own words. Interviewers prefer that to a recited speech.

---

## The problem HashMap solves

Say you keep 1 lakh employees in a `List` and someone asks: "Who has ID 50321?"
A list checks them one by one, which can mean 1 lakh checks.

A `HashMap` doesn't search. It **calculates** where the employee is kept, goes straight there, and checks only that spot. So it usually takes one step. That's called **O(1)**.

## Real-life picture: a cupboard with 16 drawers

Picture an office cupboard with 16 drawers, numbered 0 to 15. Every employee file goes in drawer number **ID % 16**.

- **Storing a file:** calculate the drawer and put the file there.
- **Finding a file:** calculate the same drawer and look only inside it.
- **Two files in one drawer:** keep both, and read the name on each to find the right one.
- **Cupboard about 75% full:** get a cupboard with 32 drawers and re-arrange the files.

HashMap does exactly this:

| Cupboard | HashMap |
|---|---|
| 16 drawers | an array of 16 **buckets** |
| drawer = ID % 16 | `hashCode()` turned into a bucket number |
| two files in the same drawer | a **collision** |
| reading the name on each file | `equals()` |
| about 75% full, so a bigger cupboard | **resize** at load factor 0.75 |

```text
bucket:  [0] [1] [2] [3] [4] [5] [6] [7] ... [15]
                              |
                              v
                         [101=Rahul] -> [117=Priya]     two keys, same bucket
```

Each `[key=value]` box is a small object called a **Node**. It holds the key, the value, the key's hash, and `next`, a link to the next node in the same bucket.

---

## Step by step

We'll store employees in a `Map<Integer, String>`. The key is the employee ID and the value is the name.

### Step 1 · `put(101, "Rahul")`: which bucket?

1. Java asks the key for its number with `hashCode()`. For an `Integer`, the hashCode is just the number itself: **101**.
2. Bucket number = 101 % 16 = **5**.
3. Bucket 5 is empty, so the entry goes there.

```text
bucket 5 : [101=Rahul]          all other buckets are empty
```

### Step 2 · `get(101)`

Java does the same calculation: 101 % 16 = 5. It goes to bucket 5, finds key 101 there, and returns "Rahul".
It never looked at the other 15 buckets. **That's the whole trick.**

### Step 3 · `put(101, "Rahul Sharma")`: the same key again

Bucket 5 already has key 101, and `equals()` says it's the same key. So HashMap **replaces the value** and adds no new entry.

- `size()` stays 1.
- `put()` returns the old value, "Rahul".

A map never keeps two entries with the same key.

### Step 4 · `put(117, "Priya")`: collision

117 % 16 = **5**. That's bucket 5 again, and it already holds 101.
When two different keys land in the same bucket, it's called a **collision**. HashMap keeps both, linked one after the other:

```text
bucket 5 : [101=Rahul Sharma] -> [117=Priya]
```

Now `get(117)` works like this:

1. 117 % 16 = 5, so go to bucket 5.
2. The first node has key 101. Does `equals(117)` match? No, so move to the next node.
3. The next node has key 117. Does `equals(117)` match? Yes, so return "Priya".

> **hashCode() picks the bucket. equals() picks the entry inside the bucket.**
> Remember this line. It answers half the follow-up questions.

### Step 5 · The map gets full: resize

A new HashMap has 16 buckets and a **load factor of 0.75**, and 16 × 0.75 = 12.
So when the **13th** entry goes in, HashMap:

1. creates a new array with **32** buckets (double the size),
2. moves every entry to its new bucket, now using % 32.

Here's where our two keys end up:

| Key | With 16 buckets | With 32 buckets |
|---|---|---|
| 101 | 101 % 16 = 5 | 101 % 32 = 5, so it **stays** |
| 117 | 117 % 16 = 5 | 117 % 32 = 21, so it **moves** |

The collision is gone, because more buckets means fewer collisions.
Every entry either **stays** at its bucket number or **moves to that number + 16**. Here 117 went from 5 to 21, which is 5 + 16.

**Why 0.75?** It's a balance. A lower value wastes memory on empty buckets. A higher value means more collisions, which makes the map slower. 0.75 is the default middle point.

### Step 6 · Too many keys in one bucket: tree (Java 8)

**The problem.** A bucket normally holds 0, 1 or 2 entries, so checking them one by one is instant. With a normal hashCode, about 60% of buckets are empty, 30% have one entry and 7% have two. But sometimes many keys land in the same bucket. Then `get()` has to check them one by one, and that is slow.

**How can one bucket get crowded?** There are two ways:

1. **The map is still small.** With 16 buckets, take the keys 5, 21, 37, 53, 69, 85, 101, 117 and 133. They are 16 apart, so each one % 16 = 5, and all 9 land in bucket 5.
2. **The keys have a bad `hashCode()`.** If a class's hashCode() returns 1 for every object, every key lands in bucket 1, no matter how many buckets there are.

**What HashMap does when one bucket gets more than 8 entries.** It asks one question: *do I have at least 64 buckets?*

| Buckets right now | What HashMap does | Why |
|---|---|---|
| fewer than 64 | **resize** (double the buckets) | The bucket may be crowded only because the map is small, and doubling splits the crowd. |
| 64 or more | turn that bucket into a **tree** | The map is already big and the bucket is still crowded, so more buckets won't help. The keys really collide. |

**Way 1 in numbers: fewer than 64 buckets, so it resizes.**
When the 9th key goes in, bucket 5 holds 9 entries. The map has only 16 buckets, so HashMap doubles to 32. It does this even though it holds just 9 entries and the normal limit is 12. With 32 buckets the keys land here:

| Keys | key % 32 | New bucket |
|---|---|---|
| 5, 37, 69, 101, 133 | 5 | bucket 5 (5 entries) |
| 21, 53, 85, 117 | 21 | bucket 21 (4 entries) |

The crowd of 9 has split into 5 and 4, so no tree is needed. That's what "more buckets spread the keys out" means. The .java file shows the real HashMap doing this.

**Way 2 in numbers: a bad hashCode, so it makes a tree.**
If every hashCode is 1, then 1 % 16 = 1, 1 % 64 = 1 and 1 % 1024 = 1. The bucket never changes, so resizing can't help. Once the map has 64 buckets, HashMap turns that bucket into a tree.

**Why a tree is faster: the guessing game.**
I think of a number from 1 to 1000.

- If you guess one by one, like walking a list, it can take 1000 guesses.
- If I say "higher" or "lower" after each guess, you guess the middle every time and need at most 10 guesses. That's how a sorted tree works.

```text
List: check one by one           5 -> 21 -> 37 -> 53 -> 69 -> 85 -> 101 -> 117 -> 133

Tree: smaller goes left, bigger goes right
                    69
                /        \
             21            117
            /  \          /   \
           5    37      85     133
                  \       \
                   53      101
```

In the list, finding 101 takes 7 checks. In the tree it takes 4: 69, then right to 117, left to 85, right to 101.
With 1,000 entries, the list needs up to 1,000 checks and the tree about 10. That's O(log n) instead of O(n).
*Red-black* is just the kind of tree Java uses. It keeps itself balanced, so it never turns into one long line.

**Why more than 8?** The JDK's own notes say that with a normal hashCode, the chance of 8 entries in one bucket is about 6 in 10 crore. So a bucket that big almost always means a bad hashCode, and it's worth building a tree. Below that, checking a few entries one by one is fast enough.

**Why go back to a list at 6, not at 8?** When a tree bucket shrinks to 6 or fewer entries, HashMap turns it back into a plain list (it checks this when it resizes). The gap between 8 and 6 stops it from flipping back and forth. Think of an AC that switches on at 26°C and off at 24°C. If both were 25°C, it would click on and off all day. The same way, a bucket that goes 8 → 9 → 8 → 9 doesn't keep converting between a list and a tree.

**Why not use a tree for every bucket?**

1. **Memory:** a list node stores 4 things: hash, key, value, next. A tree node also stores parent, left, right, prev and a red/black flag, so it's about twice the size.
2. **No gain for small buckets:** most buckets have 0 to 2 entries. Checking 2 entries in a list is already instant, and a tree only adds the work of keeping itself balanced.
3. **Rarely needed:** with a good hashCode, 8 entries in one bucket almost never happens.

So the tree is like a spare tyre: you carry it for emergencies, you don't drive on it every day.

**How to say it in the interview:**

> "From Java 8, if one bucket gets more than 8 entries, HashMap turns that linked list into a red-black tree, so searching that bucket is O(log n) instead of O(n). It does this only if the map has at least 64 buckets. If it's smaller, it resizes instead, because doubling usually spreads the keys out. If the bucket later shrinks to 6 or fewer, it goes back to a list. Trees aren't used everywhere because a tree node takes about twice the memory, and with a good hashCode a bucket almost never gets that big."

*Only if they push further:* inside the tree, entries are sorted by hash. If two keys have the same hash, Java uses compareTo() when the keys are Comparable. String and Integer are, so even String keys that collide badly stay fast.

### Step 7 · null key

HashMap allows **one** null key. Its hash is treated as 0, so it always goes to bucket 0. Values can be null too.

### Step 8 · Never change a key after you put it in

Say the key is your own class, and its ID can be changed later:

1. `put(key with id 101, "Rahul")` stores the entry in bucket 101 % 16 = 5.
2. Later, you change the key's id to 102.
3. `get(key)` now calculates 102 % 16 = 6, looks in bucket 6, finds nothing, and returns `null`.
4. The entry is still in bucket 5, but you can't reach it any more.

That's why keys should be **immutable**. Use `String` or `Integer`, or your own class with `final` fields.

---

## How to explain it in the interview

Use your own words. Cover these 6 points in this order, using the 101/117 example:

1. Inside, HashMap is an **array of buckets**, 16 by default.
2. On `put`, it calls `hashCode()` on the key and turns the result into a **bucket index**.
3. If that bucket is empty, the entry goes there.
4. If another key is already there, that's a **collision**. Both entries stay in the bucket as a linked list. **`equals()`** finds the right one, or replaces the value if it's the same key.
5. From **Java 8**, if a bucket gets **more than 8** entries and the map has 64 or more buckets, that list becomes a **red-black tree**.
6. When the map is **75% full**, it **doubles** in size and moves the entries. `get` follows the same path, so put and get are **O(1) on average**.

**Here's how it can sound** (about a minute, simple words):

> "HashMap is basically an array of buckets, 16 by default. When I put a key, Java calls hashCode on it and converts it into a bucket number. Say key 101 lands in bucket 5, so it's stored there. If I then put key 117 and it also lands in bucket 5, that's a collision. Both entries stay in bucket 5 as a linked list. When I call get(117), Java goes to bucket 5 and uses equals to find the right entry. From Java 8, if one bucket gets more than 8 entries, that list turns into a red-black tree, so the search stays fast. And when the map is 75% full, which is 12 entries for 16 buckets, it doubles to 32 buckets and moves the entries. That's why get and put are O(1) on average."

**Tip:** on a video call, ask "Can I explain with a small example?" and draw a few buckets on paper. Interviewers like this.

---

## Follow-up questions (simple answers)

**Can two different keys have the same hashCode?**
Yes. They go to the same bucket, and `equals()` tells them apart. Even Strings can do this: "Aa" and "BB" both have hashCode 2112.

**Why is the number of buckets always a power of 2 (16, 32, 64)?**
It lets Java find the bucket with a fast bit operation, `hash & (n - 1)`, instead of `%`. The answer is the same as `%`, just faster.

**Does HashMap use hashCode() directly?**
Almost. First it mixes the top half of the hashCode into the bottom half: `h ^ (h >>> 16)`. With 16 buckets, only the last 4 bits pick the bucket. Mixing makes the top bits count too, which spreads keys more evenly.

**Is HashMap thread-safe?**
No. Two threads writing at the same time can lose data. Use `ConcurrentHashMap` instead (topic J05).

**Is the order of keys guaranteed?**
No. `LinkedHashMap` keeps insertion order, and `TreeMap` keeps keys sorted (topic J04).

**What changed from Java 7 to Java 8?**
Two things. First, long buckets now become trees. Second, new entries go at the end of the list. Java 7 added them at the front, so if two threads resized at the same time the list could form a loop and `get()` would hang.

**What if I remove from the map while looping over it?**
You get a `ConcurrentModificationException`. Use `iterator.remove()` or `map.entrySet().removeIf(...)` instead.

**How does HashSet work?**
It uses a HashMap inside. Your element is the key, and a dummy object is the value.

**HashMap vs Hashtable?**
Hashtable is old. Every method is synchronized, which makes it slow, and it doesn't allow a null key or value. Use HashMap, or ConcurrentHashMap when threads share the map.

**I'll store 1,000 entries. Can I avoid resizing?**
Yes, give an initial capacity: `new HashMap<>(1334)`, because 1000 / 0.75 ≈ 1334. On Java 19 and later, `HashMap.newHashMap(1000)` does the math for you.

**What's the time complexity?**
put and get are O(1) on average. The worst case is O(n), when every key lands in one bucket. Java 8's trees bring that down to O(log n).

---

## Numbers to remember

| What | Value |
|---|---|
| Buckets at the start | 16 |
| Load factor | 0.75, so the first resize is at the 13th entry |
| Resize | doubles each time: 16 → 32 → 64 → 128 |
| Crowded bucket (more than 8) with fewer than 64 buckets | resize instead of a tree |
| List becomes a tree | more than 8 in one bucket, and at least 64 buckets |
| Tree goes back to a list | 6 or fewer entries |
| null key | one allowed, always in bucket 0 |

## Self-check (answer aloud, then click to check)

<details><summary>1. In a 16-bucket map, which bucket does key 50 go to?</summary>

50 % 16 = 2, so bucket 2.

</details>

<details><summary>2. Keys 3 and 19 go into a 16-bucket map. What happens? And after the resize to 32?</summary>

Both go to bucket 3, because 19 % 16 = 3. That's a collision. After the resize, 3 stays in bucket 3 and 19 moves to bucket 19 (3 + 16).

</details>

<details><summary>3. When does the first resize happen?</summary>

When the 13th entry goes in, because 16 × 0.75 = 12.

</details>

<details><summary>4. Why do we need equals() if we already have hashCode()?</summary>

hashCode only picks the bucket. Different keys can share a bucket, so equals() decides which entry is the right one.

</details>

<details><summary>5. What happens if you change a key after putting it in the map?</summary>

get() calculates a different bucket and returns null. The entry is still inside, but you can't reach it. That's why keys should be immutable.

</details>

<details><summary>6. When does a bucket become a tree?</summary>

When it has more than 8 entries and the map has at least 64 buckets. With fewer buckets, HashMap resizes instead.

</details>

<details><summary>7. One bucket has 9 entries, but the map has only 16 buckets. What happens?</summary>

HashMap doesn't make a tree. It resizes to 32 buckets, and the crowd usually splits into two buckets. With keys 5, 21, 37 ... 133: bucket 5 gets 5 keys and bucket 21 gets 4.

</details>

<details><summary>8. Why does a tree go back to a list at 6 entries, not at 8?</summary>

The gap stops it from flipping back and forth between list and tree when a bucket hovers around 8. It works like an AC that switches on at 26°C and off at 24°C.

</details>

If you get stuck on any of them, add it to [STUMBLE-LIST.md](../STUMBLE-LIST.md). When all 8 feel easy, tick J01 in the [README](../README.md) and send `next`.
