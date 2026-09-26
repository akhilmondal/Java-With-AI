# S00 · Streams toolkit: the collectors you need for the 8 programs

**Read this first (12 min). Then run [S00_StreamsToolkit.java](S00_StreamsToolkit.java) to watch each step happen.**

This is your **look-up sheet** for the practice file [S01_StreamPractice.java](S01_StreamPractice.java). Understand the 8 tools using the six payments below, then write the 8 programs yourself. Come back here only after 10 minutes stuck on a problem.

J08 covered filter, map, flatMap, sorted and Optional. This page adds the **collectors**: the tools that group, count, sum and pick things.

| txnId | amount | status | mode |
|---|---|---|---|
| TXN1 | 1,200 | SUCCESS | UPI |
| TXN2 | 450 | FAILED | CARD |
| TXN3 | 300 | PENDING | UPI |
| TXN4 | 15,000 | SUCCESS | NETBANKING |
| TXN5 | 800 | SUCCESS | CARD |
| TXN6 | 650 | FAILED | UPI |

---

## The problem

Most stream questions in interviews sound like "group these, then count / sum / find the biggest in each group". With plain loops, that means a map, null checks and a lot of code. With streams it's one `collect(...)` call, as long as you know which **collector** to use.

## Real-life picture: the post office sorting room

Letters (payments) come along a belt, and the sorting room puts them where they belong:

- **groupingBy:** sort the letters into **bins by PIN code** (by status).
- **counting:** count the letters in each bin.
- **summingInt:** add up the postage in each bin.
- **mapping:** from each letter, keep only the address label (just the txnId).
- **maxBy:** find the heaviest parcel in each bin.
- **partitioningBy:** exactly **two bins**, "Speed Post: yes" and "Speed Post: no".
- **toMap:** a register with **one line per tracking number**, plus a rule for what to do when the same number turns up twice.

| Sorting room | Collector |
|---|---|
| bins by PIN code | `groupingBy(Payment::status)` |
| count per bin | `groupingBy(..., counting())` |
| total postage per bin | `groupingBy(..., summingInt(Payment::amount))` |
| keep only the label | `groupingBy(..., mapping(Payment::txnId, toList()))` |
| heaviest in each bin | `groupingBy(..., maxBy(comparator))` |
| two bins, yes and no | `partitioningBy(p -> p.amount() > 1000)` |
| one line per tracking number | `toMap(Payment::txnId, Payment::amount)` |

```text
belt:             TXN1   TXN2   TXN3   TXN4   TXN5   TXN6
                    \      |      |      |      |     /
groupingBy(status): [FAILED]      [PENDING]     [SUCCESS]
                    TXN2 TXN6       TXN3        TXN1 TXN4 TXN5
counting():            2              1              3
summingInt(amount):  1,100           300          17,000
```

---

## Step by step

### Step 1 · Collect the results: `toList()` and `joining()`

```java
PAYMENTS.stream().filter(p -> p.status().equals("SUCCESS")).map(Payment::txnId).toList()
// [TXN1, TXN4, TXN5]

PAYMENTS.stream().map(Payment::txnId).collect(Collectors.joining(", "))
// "TXN1, TXN2, TXN3, TXN4, TXN5, TXN6"
```

`.toList()` is from Java 16. On older Java, write `.collect(Collectors.toList())`.

### Step 2 · `groupingBy`: sort into bins

```java
groupingBy(Payment::status)                                        // status -> its payments
groupingBy(Payment::status, counting())                            // status -> how many
groupingBy(Payment::status, summingInt(Payment::amount))           // status -> total amount
groupingBy(Payment::mode, mapping(Payment::txnId, toList()))       // mode -> just the ids
```

| Call | Result |
|---|---|
| by status | {FAILED=[TXN2, TXN6], PENDING=[TXN3], SUCCESS=[TXN1, TXN4, TXN5]} |
| count by status | {FAILED=**2**, PENDING=**1**, SUCCESS=**3**} |
| sum by status | FAILED 450 + 650 = **1,100**, PENDING **300**, SUCCESS 1,200 + 15,000 + 800 = **17,000** |
| ids by mode | {CARD=[TXN2, TXN5], NETBANKING=[TXN4], UPI=[TXN1, TXN3, TXN6]} |

The second argument is called the **downstream collector**. It says what to do with each bin. Without it, you get a List.

`groupingBy` returns a **HashMap**, which has no order (J04). The demo copies it into a TreeMap only so the keys print from A to Z.

### Step 3 · The biggest in each bin: `maxBy` and `collectingAndThen`

```java
groupingBy(Payment::status, maxBy(Comparator.comparingInt(Payment::amount)))
// {FAILED=Optional[TXN6], PENDING=Optional[TXN3], SUCCESS=Optional[TXN4]}
```

`maxBy` gives an **Optional** (J08), because in general the stream could be empty. Inside groupingBy a bin always has at least one item, so it's safe to unwrap:

```java
groupingBy(Payment::status, collectingAndThen(maxBy(...), Optional::get))
// {FAILED=TXN6, PENDING=TXN3, SUCCESS=TXN4}
```

`collectingAndThen(collector, finisher)` means: collect as usual, then apply one last step to the result. FAILED gives TXN6 because 650 > 450.

### Step 4 · `partitioningBy`: exactly two bins

```java
partitioningBy(p -> p.amount() > 1_000)
// {false=[TXN2, TXN3, TXN5, TXN6], true=[TXN1, TXN4]}

partitioningBy(p -> p.amount() > 1_000, counting())
// {false=4, true=2}
```

**partitioningBy vs groupingBy:** partitioning always has both keys, `false` and `true`, even if one list is empty. groupingBy only has the keys that actually appear.

### Step 5 · `toMap`, and the duplicate-key trap

```java
toMap(Payment::txnId, Payment::amount)
// {TXN1=1200, TXN2=450, TXN3=300, TXN4=15000, TXN5=800, TXN6=650}
```

Now a callback log where **TXN1 arrives twice**, which really happens with payment gateways:

```text
IllegalStateException: Duplicate key TXN1 (attempted merging values 1200 and 1200)
```

The fix is a third argument, the **merge rule**, which decides what to do with two values for the same key:

```java
toMap(Payment::txnId, Payment::amount, (first, second) -> first)   // keep the first
// {TXN1=1200, TXN2=450}
```

### Step 6 · Count, then filter the counts; choose the map type

**Count, then filter:** first build the counts, then stream over the map's **entries**:

```java
Map<String, Long> countByMode = ...groupingBy(Payment::mode, counting());    // {CARD=2, NETBANKING=1, UPI=3}
countByMode.entrySet().stream()
        .filter(entry -> entry.getValue() > 1)      // used more than once
        .map(Map.Entry::getKey)
        .toList();                                  // [CARD, UPI]
```

**Choose the map:** the 3-argument `groupingBy` lets you pick the map type:

```java
groupingBy(Payment::mode, LinkedHashMap::new, counting())
// {UPI=3, CARD=2, NETBANKING=1}   <- first-seen order: UPI (TXN1), then CARD (TXN2), then NETBANKING (TXN4)
```

Use `LinkedHashMap::new` for first-seen order and `TreeMap::new` for sorted keys.

**`Function.identity()`** means "the element itself", the same as `x -> x`:

```java
toMap(Payment::txnId, Function.identity())      // id -> the whole Payment
```

### Step 7 · Numbers

```java
mapToInt(Payment::amount).sum()                  // 18,400
mapToInt(Payment::amount).summaryStatistics()    // count=6, min=300, max=15000, average=3066.67
map(Payment::amount).sorted(Comparator.reverseOrder()).limit(3)   // [15000, 1200, 800]
IntStream.rangeClosed(1, 5).boxed().toList()     // [1, 2, 3, 4, 5]
```

- `mapToInt` gives an `IntStream`, which has `sum()`, `average()`, `max()` and `summaryStatistics()`, with no boxing.
- `boxed()` turns an `int` back into an `Integer`, so you can collect it into a List.
- `sorted(Comparator.reverseOrder())` sorts from biggest to smallest.
- `skip(n)` jumps over the first n items, and `limit(n)` keeps only the first n.

### Step 8 · A String as a stream of characters

```java
"BBPS".chars()                          // [66, 66, 80, 83]  <- int codes, not letters!
"BBPS".chars().mapToObj(c -> (char) c)  // [B, B, P, S]
"BBPS".chars().distinct().count()       // 3
```

`chars()` gives an `IntStream` of character codes, so turn them back with `mapToObj(c -> (char) c)`. After that, all the collectors above work on characters.

---

## Hint sheet: which tools for which practice problem

Use this **only after 10 minutes** on a problem in [S01_StreamPractice.java](S01_StreamPractice.java):

| S01 problem | Tools |
|---|---|
| 1. Character frequency | Step 8 (chars → mapToObj) + Step 2 (groupingBy + counting) + Step 6 (LinkedHashMap::new keeps first-seen order) |
| 2. First non-repeated character | the same counts as problem 1, then Step 6 (stream the entries, take the first with count 1) |
| 3. Duplicate elements | Step 6 (count, then keep counts > 1) |
| 4. Second-highest number | Step 7 (sort biggest first) + `distinct()` + `skip(1)` + `findFirst()` |
| 5. Employees grouped by department | Step 2 (groupingBy) |
| 6. Highest-paid employee per department | Step 3 (maxBy + collectingAndThen) |
| 7. Sort by salary descending, then by name | J08 Step 2 (Comparator chain) |
| 8. Numbers partitioned into even and odd | Step 7 (rangeClosed) + Step 4 (partitioningBy) |

---

## How to explain it in the interview (live coding)

The plan says to **talk before you type**. For any stream question, say these out loud:

1. **The shape of the answer:** "I need a map from department to the highest-paid employee."
2. **The collector:** "So I'll use groupingBy on department, with maxBy on salary as the downstream collector."
3. **The details:** "maxBy gives an Optional, so I unwrap it with collectingAndThen." Or: "toMap throws on duplicate keys, so I pass a merge function." Or: "LinkedHashMap keeps first-seen order."
4. **The cost:** one pass over the list, so O(n). If you sort, it's O(n log n).

**Here's how it can sound** (simple words):

> "I'll stream the payments and collect with groupingBy on status, which gives me a map from status to its list of payments. If I only need counts, I pass counting() as the downstream collector, and for totals, summingInt on amount. For the biggest payment per status I use maxBy with a comparator on amount, which returns an Optional, so I wrap it in collectingAndThen with Optional::get. If I need just two groups, like above or below 1,000, partitioningBy is cleaner. And if I build a map with toMap, I remember it throws on duplicate keys, so I give it a merge function."

---

## Follow-up questions (simple answers)

**Why does `counting()` give a `Long` and not an `Integer`?**
That's just how it's defined: it returns a `long` count. So the map is `Map<String, Long>`. Writing `Map<String, Integer>` is a common compile error in live coding.

**groupingBy vs partitioningBy?**
partitioningBy takes a true/false test and always has both keys. groupingBy takes any key and only has the keys that appear.

**What happens with toMap on duplicate keys, or null values?**
Duplicate keys throw an IllegalStateException unless you give a merge function. A null value throws a NullPointerException.

**`toList()` vs `collect(Collectors.toList())`?**
`toList()` (Java 16) gives an **unmodifiable** list. `Collectors.toList()` gives a normal, changeable list (an ArrayList in practice).

**Why `mapToInt` instead of `map`?**
It avoids wrapping every number in an Integer, and it gives `sum()`, `average()` and `summaryStatistics()`.

**How do you keep the order of groups?**
Use `groupingBy(key, LinkedHashMap::new, downstream)` for first-seen order, or `TreeMap::new` for sorted order.

*Only if they push further:* `toMap(key, value, merge, TreeMap::new)` also lets you choose the map type. And `teeing(c1, c2, merger)` (Java 12) runs two collectors in one pass and combines their results, for example the min and the max together.

---

## Numbers to remember

| What | Result |
|---|---|
| Count by status | FAILED 2, PENDING 1, SUCCESS 3 |
| Sum by status | FAILED 1,100, PENDING 300, SUCCESS 17,000 |
| Biggest per status | FAILED TXN6, PENDING TXN3, SUCCESS TXN4 |
| Amount > 1,000 | true: TXN1, TXN4 · false: the other 4 |
| Sum of all amounts | 18,400 |
| Top 3 amounts | 15,000, 1,200, 800 |

## Self-check (answer aloud, then click to check)

<details><summary>1. groupingBy(status, counting()) on the six payments: what's the result?</summary>

{FAILED=2, PENDING=1, SUCCESS=3}

</details>

<details><summary>2. What's the total amount of the FAILED payments, and which collector gives it?</summary>

450 + 650 = 1,100, from groupingBy(status, summingInt(Payment::amount)).

</details>

<details><summary>3. Why does groupingBy(status, maxBy(...)) give Optional values, and how do you remove them?</summary>

maxBy returns an Optional because a stream could be empty. Wrap it: collectingAndThen(maxBy(...), Optional::get).

</details>

<details><summary>4. What does toMap(txnId, amount) do when TXN1 appears twice?</summary>

It throws IllegalStateException: Duplicate key TXN1. Add a merge function such as (first, second) -> first.

</details>

<details><summary>5. What's the difference between partitioningBy(p -> p.amount() > 100_000) and groupingBy with the same test?</summary>

partitioningBy gives {false=[all six], true=[]}, with both keys always there. groupingBy gives only {false=[all six]}.

</details>

<details><summary>6. What does "BBPS".chars() give, and how do you get letters?</summary>

It gives int codes [66, 66, 80, 83]. Use mapToObj(c -> (char) c) to get [B, B, P, S].

</details>

<details><summary>7. How do you keep groups in first-seen order?</summary>

Use groupingBy(key, LinkedHashMap::new, downstream).

</details>

If you get stuck on any of them, add it to [STUMBLE-LIST.md](../STUMBLE-LIST.md). When all 7 feel easy, tick S00 in the [README](../README.md) and open S01 to start the practice.
