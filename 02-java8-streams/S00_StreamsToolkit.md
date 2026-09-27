# S00 · Streams toolkit: the collectors you need for the 8 programs

> **In one line:** Most stream interview questions sound like "**group** these, then **count / sum / find the biggest** in each group". The answer is one `collect(...)` call with the right **collector**: `groupingBy`, `counting`, `summingInt`, `maxBy`, `partitioningBy` or `toMap`.

| ⏱️ Read | 🧪 Run | 🎯 Asked |
|---|---|---|
| 12 min | `java 02-java8-streams/S00_StreamsToolkit.java` | The live-coding part of most Java rounds. Practice in [S01](S01_StreamPractice.java) |

This is your **look-up sheet** for the practice file. Learn the tools on these six payments, then write the 8 programs yourself in S01. Come back only after 10 minutes stuck.

| txnId | amount | status | mode |
|---|---|---|---|
| TXN1 | 1,200 | SUCCESS | UPI |
| TXN2 | 450 | FAILED | CARD |
| TXN3 | 300 | PENDING | UPI |
| TXN4 | 15,000 | SUCCESS | NETBANKING |
| TXN5 | 800 | SUCCESS | CARD |
| TXN6 | 650 | FAILED | UPI |

---

## 🧩 Words you need

| Word | In one line |
|---|---|
| **collector** | the "how to gather the results" part of `collect(...)` |
| **groupingBy** | puts items into bins by a key, giving `Map<key, List>` |
| **downstream collector** | the 2nd argument of groupingBy: what to do with each bin (count, sum, max …) |
| **partitioningBy** | exactly two bins: `true` and `false` |
| **merge function** | toMap's rule for "the same key twice" |

---

## 🖼️ Picture it: the post office sorting room

```mermaid
flowchart LR
    B["belt: TXN1 ... TXN6"] --> G{"groupingBy(status)"}
    G --> F["FAILED bin<br/>TXN2, TXN6<br/>count 2, sum 1100"]
    G --> P["PENDING bin<br/>TXN3<br/>count 1, sum 300"]
    G --> S["SUCCESS bin<br/>TXN1, TXN4, TXN5<br/>count 3, sum 17000"]
```

👀 **Notice:** `groupingBy` makes the bins. The **downstream collector** decides what you write on each bin's label: the list, the count, the sum or the biggest.

| Sorting room | Collector |
|---|---|
| bins by PIN code | `groupingBy(Payment::status)` |
| count per bin | `groupingBy(..., counting())` |
| total postage per bin | `groupingBy(..., summingInt(Payment::amount))` |
| keep only the label | `groupingBy(..., mapping(Payment::txnId, toList()))` |
| heaviest parcel per bin | `groupingBy(..., maxBy(comparator))` |
| two bins, "Speed Post: yes / no" | `partitioningBy(p -> p.amount() > 1000)` |
| one register line per tracking number | `toMap(Payment::txnId, Payment::amount)` |

---

## 🔬 How it works, step by step

### Step 1 · `toList()` and `joining()`

```java
PAYMENTS.stream().filter(p -> p.status().equals("SUCCESS")).map(Payment::txnId).toList();
// [TXN1, TXN4, TXN5]
PAYMENTS.stream().map(Payment::txnId).collect(Collectors.joining(", "));
// "TXN1, TXN2, TXN3, TXN4, TXN5, TXN6"
```

`.toList()` is Java 16+. On older Java, write `.collect(Collectors.toList())`.

### Step 2 · `groupingBy` plus a downstream collector

| Call | Result |
|---|---|
| `groupingBy(status)` | {FAILED=[TXN2, TXN6], PENDING=[TXN3], SUCCESS=[TXN1, TXN4, TXN5]} |
| `groupingBy(status, counting())` | {FAILED=**2**, PENDING=**1**, SUCCESS=**3**} |
| `groupingBy(status, summingInt(amount))` | FAILED 450 + 650 = **1,100** · PENDING **300** · SUCCESS 1,200 + 15,000 + 800 = **17,000** |
| `groupingBy(mode, mapping(txnId, toList()))` | {CARD=[TXN2, TXN5], NETBANKING=[TXN4], UPI=[TXN1, TXN3, TXN6]} |

⚠️ `counting()` gives a **Long**, so the map is `Map<String, Long>`. Writing Integer is a classic compile error in live coding.

### Step 3 · The biggest in each bin: `maxBy` and `collectingAndThen`

```mermaid
flowchart LR
    M["groupingBy(status,<br/>maxBy(amount))"] --> O["{FAILED=Optional[TXN6], ...}"]
    O -->|"collectingAndThen(..., Optional::get)"| U["{FAILED=TXN6, PENDING=TXN3, SUCCESS=TXN4}"]
```

`maxBy` returns an **Optional**, because in general a stream can be empty. Inside a groupingBy bin there's always at least one item, so unwrapping with `Optional::get` is safe. FAILED gives TXN6 because 650 > 450.

### Step 4 · `partitioningBy`: exactly two bins

```java
partitioningBy(p -> p.amount() > 1_000)               // {false=[TXN2, TXN3, TXN5, TXN6], true=[TXN1, TXN4]}
partitioningBy(p -> p.amount() > 1_000, counting())   // {false=4, true=2}
```

🧠 partitioningBy **always** has both keys, even when one list is empty. groupingBy only has the keys that actually appear.

### Step 5 · `toMap`, and the duplicate-key trap

A gateway callback log where **TXN1 arrives twice**, which is common in payments:

```mermaid
flowchart LR
    L["callback log:<br/>TXN1, TXN2, TXN1"] --> T{"toMap(id, amount)"}
    T -->|"no merge rule"| X["IllegalStateException:<br/>Duplicate key TXN1"]
    T -->|"merge: (first, second) -> first"| OK["{TXN1=1200, TXN2=450}"]
```

```java
toMap(Payment::txnId, Payment::amount, (first, second) -> first)   // keep the first
```

### Step 6 · Count, then filter the counts; choose the map type

```java
Map<String, Long> countByMode = ...groupingBy(Payment::mode, counting());   // {CARD=2, NETBANKING=1, UPI=3}
countByMode.entrySet().stream()                   // a stream over the map's entries
        .filter(entry -> entry.getValue() > 1)    // used more than once
        .map(Map.Entry::getKey)
        .toList();                                // [CARD, UPI]
```

**Choose the map** with the 3-argument groupingBy:
- `groupingBy(Payment::mode, LinkedHashMap::new, counting())` gives `{UPI=3, CARD=2, NETBANKING=1}`, which is **first-seen order**.
- `TreeMap::new` gives sorted keys.

`Function.identity()` means "the element itself": `toMap(Payment::txnId, Function.identity())` maps each id to the whole Payment.

### Step 7 · Numbers

```java
mapToInt(Payment::amount).sum()                    // 18,400
mapToInt(Payment::amount).summaryStatistics()      // count=6, min=300, max=15000, average=3066.67
map(Payment::amount).sorted(Comparator.reverseOrder()).limit(3)   // [15000, 1200, 800]
IntStream.rangeClosed(1, 5).boxed().toList()       // [1, 2, 3, 4, 5]
```

- `mapToInt` avoids boxing and gives `sum()` and `average()`.
- `boxed()` turns an `int` back into an `Integer`.
- `skip(n)` jumps over n items, and `limit(n)` keeps n.

### Step 8 · A String as a stream of characters

```java
"BBPS".chars()                          // [66, 66, 80, 83]  <- int CODES, not letters!
"BBPS".chars().mapToObj(c -> (char) c)  // [B, B, P, S]
"BBPS".chars().distinct().count()       // 3
```

---

## 🗺️ Hint sheet: which tools for which S01 problem (use it only after 10 minutes)

```mermaid
flowchart LR
    P1["1 char frequency"] --> T1["Step 8 + Step 2 + LinkedHashMap::new"]
    P2["2 first non-repeated"] --> T2["the counts from 1, then entries with count 1"]
    P3["3 duplicates"] --> T3["Step 6: count, keep > 1"]
    P4["4 second highest"] --> T4["distinct + sorted desc + skip(1)"]
    P5["5 group by dept"] --> T5["Step 2 groupingBy"]
    P6["6 highest paid per dept"] --> T6["Step 3 maxBy + collectingAndThen"]
    P7["7 sort salary desc, name"] --> T7["J08 Comparator chain"]
    P8["8 even and odd"] --> T8["rangeClosed + partitioningBy"]
```

---

## ⚠️ Traps interviewers love

| Trap | Why it's wrong | Do this instead |
|---|---|---|
| `Map<String, Integer>` with `counting()` | counting returns Long | `Map<String, Long>` |
| toMap with possible duplicate keys | IllegalStateException | add a merge function |
| Expecting order from groupingBy | it builds a HashMap | pass `LinkedHashMap::new` or `TreeMap::new` |
| Leaving maxBy's Optional in the result | an ugly `Optional[...]` value | `collectingAndThen(maxBy(...), Optional::get)` |
| Printing `"BBPS".chars()` expecting letters | you get int codes | `mapToObj(c -> (char) c)` |

---

## 🎯 In the interview (live coding)

**What they're really testing**
- *Service companies:* can you write groupingBy and counting without looking it up.
- *Product companies:* the right collector for the job, the types (`Long`), duplicates in toMap, ordering, and the cost: one pass is O(n), a sort is O(n log n).

**Talk before you type, in this order:**
1. **The shape of the answer:** "I need a map from department to the highest-paid employee."
2. **The collector:** "So groupingBy on department, with maxBy on salary downstream."
3. **The detail:** "maxBy gives an Optional, so I unwrap it with collectingAndThen." Or: "toMap needs a merge function for duplicates." Or: "LinkedHashMap for first-seen order."
4. **The cost:** "One pass, O(n)."

**Sample answer** (simple words):

> "I stream the payments and collect with groupingBy on status, which gives a map from status to its payments. For counts I pass counting() as the downstream collector, and for totals, summingInt on amount. For the biggest payment per status I use maxBy with a comparator, which returns an Optional, so I wrap it in collectingAndThen with Optional::get. For just two groups, like above or below 1,000, partitioningBy is cleaner. And when I build a map with toMap I remember it throws on duplicate keys, so I give it a merge function."

---

## ❓ Follow-up questions

**groupingBy vs partitioningBy?**
partitioningBy takes a true/false test and always has both keys. groupingBy takes any key and has only the keys that appear.

**`toList()` vs `collect(Collectors.toList())`?**
`toList()` (Java 16) is **unmodifiable**. `Collectors.toList()` gives a normal, changeable list.

**toMap with null values?**
It throws NullPointerException.

*Only if they push further:* `teeing(c1, c2, merger)` (Java 12) runs two collectors in one pass and combines them, for example the min and the max together.

---

## 🧪 Test yourself (answer aloud, then click)

<details><summary>1. What does groupingBy(status, counting()) give on the six payments?</summary>

{FAILED=2, PENDING=1, SUCCESS=3}

</details>

<details><summary>2. What's the total of the FAILED payments, and which collector gives it?</summary>

450 + 650 = 1,100, from groupingBy(status, summingInt(Payment::amount)).

</details>

<details><summary>3. Why does maxBy give Optional values, and how do you remove them?</summary>

A stream could be empty in general. Wrap it: collectingAndThen(maxBy(...), Optional::get).

</details>

<details><summary>4. What does toMap(txnId, amount) do with TXN1 twice?</summary>

It throws IllegalStateException: Duplicate key TXN1. Add (first, second) -> first.

</details>

<details><summary>5. What do "BBPS".chars() and then mapToObj(c -> (char) c) give?</summary>

[66, 66, 80, 83], then [B, B, P, S].

</details>

When they all feel easy, tick S00 in the [README](../README.md) and open [S01_StreamPractice.java](S01_StreamPractice.java).

---

## ⚡ Quick Revision (2 hours before the interview)

```mermaid
flowchart LR
    S["stream()"] --> C{"collect(...)"}
    C --> G["groupingBy(key, downstream)"]
    G --> D1["counting() gives Long"]
    G --> D2["summingInt(x)"]
    G --> D3["mapping(x, toList())"]
    G --> D4["collectingAndThen(maxBy(cmp), Optional::get)"]
    C --> PB["partitioningBy(test): true/false"]
    C --> TM["toMap(k, v, merge)"]
```

**🧠 Must remember**
1. `groupingBy(key)` gives `Map<key, List>`. The 2nd argument, the **downstream**, says what to do per bin.
2. `counting()` gives a **Long**. `summingInt(f)` gives the sum. `mapping(f, toList())` keeps only f.
3. **Max per group:** `collectingAndThen(maxBy(comparingInt(f)), Optional::get)`.
4. `partitioningBy(test)` always gives **both** false and true.
5. `toMap(k, v)` **throws on duplicate keys**. Add `(a, b) -> a`.
6. **Order:** `groupingBy(key, LinkedHashMap::new, downstream)` for first-seen order, `TreeMap::new` for sorted.
7. **Count, then filter:** `.entrySet().stream().filter(e -> e.getValue() > 1).map(Map.Entry::getKey)`.
8. `str.chars()` gives **int codes**, so use `mapToObj(c -> (char) c)`. `mapToInt(...).sum()`. `IntStream.rangeClosed(1, n).boxed()`.

**⚠️ Top traps**
- Writing `Map<String, Integer>` with counting().
- A toMap with duplicates and no merge rule.
- Expecting order from a plain groupingBy.

**🎯 30-second answer:** "For 'group and aggregate' questions I use collect with groupingBy and a downstream collector: counting for counts, summingInt for totals, maxBy wrapped in collectingAndThen for the biggest per group. partitioningBy for exactly two groups, and toMap with a merge function when keys can repeat. LinkedHashMap::new if the order matters."

**🔑 Memory hook:** *"The post office: groupingBy makes the bins, and the downstream writes the label: count, sum or heaviest. Partitioning is just Yes and No. toMap is the register: decide what to do if a number repeats."*

**🗣️ Say it aloud (no peeking):**
1. Write the "highest paid per department" collector from memory.
2. What goes wrong with toMap on a callback log, and how do you fix it?
3. How do you get a character-frequency map in first-seen order?
