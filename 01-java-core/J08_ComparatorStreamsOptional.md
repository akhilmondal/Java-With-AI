# J08 · Comparable vs Comparator, map vs flatMap, intermediate vs terminal, Optional

**Read this first (12 min). Then run [J08_ComparatorStreamsOptional.java](J08_ComparatorStreamsOptional.java) to watch each step happen.**

Don't memorize sentences. Understand the 6 steps and the four employees below. Once you get those, you can answer sorting, stream and Optional questions in your own words. These are also the tools for the 8 stream programs in Part 2.

| id | name | salary | skills |
|---|---|---|---|
| 101 | Rahul | 50,000 | Java, Spring |
| 104 | Sneha | 60,000 | Java, AWS |
| 103 | Amit | 50,000 | SQL |
| 102 | Priya | 70,000 | Angular, Java |

The list is deliberately **not** in id order, so you can see sorting change it.

---

## The problem

You sort things (by id, by salary, by name), you process lists (filter, transform, add up) and you deal with "maybe there's no result" (find an employee who doesn't exist). Java 8 gave you tools for all three:

- **Comparable** and **Comparator** for sorting.
- **Streams** for processing lists in a pipeline.
- **Optional** for "a value may be missing" without returning null.

## Real-life pictures

- **Comparable** is the **token number** printed on each patient's card at a clinic. That's the default order, built into the card itself (the class).
- **Comparator** is the doctor's **special rule for today**: "emergencies first, then by age". It's a rule from outside the card, and you can have as many rules as you want.
- **A stream** is a **factory conveyor belt**. Items pass through stations (filter, map, sorted). Nothing moves until someone at the end **asks for the finished product** (the terminal operation).
- **map vs flatMap:** map swaps each parcel for exactly one new parcel. flatMap **opens each box** and puts everything inside directly on the belt.
- **Optional** is a **gift box that might be empty**. You check before using it, or you say "if it's empty, use this instead".

| Real life | Java |
|---|---|
| the token number on the card | `Comparable.compareTo()`, the natural order |
| the doctor's rule for today | a `Comparator`, one of many possible orders |
| the conveyor belt | a `Stream` pipeline |
| stations on the belt | intermediate operations: `filter`, `map`, `sorted` |
| "give me the finished product" | the terminal operation: `collect`, `findFirst`, `sum` |
| unpacking boxes onto the belt | `flatMap` |
| a gift box that may be empty | `Optional` |

---

## Part A: sorting

### Step 1 · Comparable: the natural order, inside the class

```java
record Employee(int id, String name, int salary, List<String> skills) implements Comparable<Employee> {
    public int compareTo(Employee other) {
        return Integer.compare(this.id, other.id);   // by id
    }
}
Collections.sort(list);        // uses compareTo
```

The result is **[Rahul, Priya, Amit, Sneha]**, with ids **[101, 102, 103, 104]**.

`compareTo` returns a **negative** number (I come first), **0** (same position) or a **positive** number (I come after). The demo's `compareTo(101 vs 102)` returns **-1**, so 101 comes first.

A class gets only **one** natural order.

### Step 2 · Comparator: any order, from outside

```java
list.sort(Comparator.comparing(Employee::name));             // by name
list.sort(Comparator.comparingInt(Employee::salary)
        .reversed()                                         // salary high to low
        .thenComparing(Employee::name));                    // if salaries tie, by name
```

- By name: **[Amit, Priya, Rahul, Sneha]**.
- By salary from high to low, then by name: **[Priya 70,000, Sneha 60,000, Amit 50,000, Rahul 50,000]**. Amit comes before Rahul because they tie on salary, so the name decides.

**Trap:** TreeSet and TreeMap use the comparator to decide what counts as **the same** element. A TreeSet sorted **only by salary** thinks Rahul and Amit (both 50,000) are the same, so it keeps just one: the size is **3, not 4**. If you want to keep both, add a tie-breaker such as `thenComparing(Employee::id)`.

---

## Part B: streams

### Step 3 · Lazy: nothing runs until the terminal operation

```java
Stream<Employee> pipeline = employees.stream()
        .filter(e -> e.salary() > 55_000);   // intermediate: only describes the work
Optional<Employee> first = pipeline.findFirst();   // terminal: NOW it runs
```

What the demo prints:

```text
pipeline built, nothing checked yet (no terminal operation)
  checking Rahul
  checking Sneha
findFirst -> Sneha (stopped early: Amit and Priya were never checked)
```

- **Intermediate operations** (`filter`, `map`, `sorted`, `distinct`, `limit`, `peek`) return a new stream and are **lazy**. They only describe the work.
- **Terminal operations** (`collect`, `toList`, `forEach`, `count`, `sum`, `findFirst`, `anyMatch`) **run** the pipeline and give a result.
- Some terminal operations **stop early**. `findFirst` checked only 2 of the 4 employees.

Another terminal operation: `mapToInt(Employee::salary).sum()` gives 50,000 + 60,000 + 50,000 + 70,000 = **230,000**.

### Step 4 · map vs flatMap

```java
.map(Employee::skills)                  // one employee -> ONE list
.flatMap(e -> e.skills().stream())      // one employee -> MANY skills, all in one stream
```

| | Result | Count |
|---|---|---|
| `map` | [[Java, Spring], [Java, AWS], [SQL], [Angular, Java]] | **4** items, each a list |
| `flatMap` | [Java, Spring, Java, AWS, SQL, Angular, Java] | **7** skills |
| `flatMap` + `distinct()` | [Java, Spring, AWS, SQL, Angular] | **5** unique skills |

> **map: one in, one out. flatMap: one in, many out, flattened into one stream.**
> It's the same idea as `thenCompose` in J06: flatten the box inside the box.

### Step 5 · A stream can be used only once

```java
Stream<String> names = employees.stream().map(Employee::name);
names.count();      // 4
names.count();      // IllegalStateException: stream has already been operated upon or closed
```

A stream isn't a collection that stores data. It's a one-time trip along the belt. To go again, call `employees.stream()` again.

---

## Part C: Optional

### Step 6 · Optional: a box that may be empty

```java
findById(101)                                  // Optional[Rahul]
findById(999)                                  // Optional.empty
findById(999).map(Employee::name).orElse("Unknown")                 // "Unknown"
findById(999).orElseThrow(() -> new IllegalArgumentException("No employee 999"))
```

The useful methods:
- `map` transforms the value if one is there.
- `orElse(x)` gives a default.
- `orElseGet(() -> x)` gives a default that's only created when needed.
- `orElseThrow` throws if the box is empty.
- `ifPresent(...)` runs code only if there's a value.

**The orElse vs orElseGet trap:** Rahul exists, so no default is needed:

```text
orElse:
  createDefault() ran          <- ran anyway, wasted work (imagine a DB call)
orElseGet:
  (nothing printed)            <- the lambda runs only when the box is empty
```

`orElse(createDefault())` evaluates its argument **before** the call, every time. Use `orElseGet` when the default is expensive.

**Rules:**
- Use Optional as a **return type** for "may be missing". Spring Data's `findById` returns `Optional`.
- Don't use it for fields or method parameters.
- Don't call `get()` without checking first.

---

## How to explain it in the interview

Use your own words. Cover these points in this order, using the four employees:

1. **Comparable** is the natural order inside the class (`compareTo`), one per class. **Comparator** is a separate rule; you can have many and chain them: `comparing(salary).reversed().thenComparing(name)`.
2. A stream is **source + intermediate operations + one terminal operation**. Intermediate operations are lazy. Nothing runs until the terminal operation, and some of those stop early (findFirst, anyMatch). A stream can be used only once.
3. **map** turns one element into one result. **flatMap** turns one element into many and flattens them into one stream.
4. **Optional** is a return type that may be empty. Use map, orElse, orElseGet or orElseThrow instead of null checks. orElseGet is lazy, orElse isn't.

**Here's how it can sound** (about a minute, simple words):

> "Comparable gives a class its natural order through compareTo, like sorting employees by id, and there's only one per class. Comparator is an external rule, so I can have many, for example salary descending and then name, using Comparator.comparing, reversed and thenComparing. With streams I build a pipeline: intermediate operations like filter and map are lazy and just describe the work, and the terminal operation, like collect or findFirst, actually runs it. findFirst even stops early. map converts each element to one result, while flatMap converts each element to many and flattens them, like getting all skills from all employees. Optional is for return values that may be missing, like findById. Instead of null checks I use map, orElse or orElseThrow, and I prefer orElseGet when the default is expensive, because orElse always evaluates it."

**Tip:** for sorting questions, say the result aloud with the tie case: "Amit before Rahul, because they tie on salary and A comes before R."

---

## Follow-up questions (simple answers)

**Why use `Integer.compare(a, b)` instead of `a - b` in compareTo?**
Subtraction can overflow. For example, -2,000,000,000 - 1,000,000,000 should be -3,000,000,000, but an int wraps it round to **+1,294,967,296**, so the order comes out wrong. Integer.compare never overflows.

**Should compareTo agree with equals?**
It's strongly recommended. Sorted collections use compareTo to decide duplicates, as the TreeSet trap in Step 2 showed.

**list.sort() vs stream().sorted()?**
`list.sort(comparator)` sorts that same list in place. `stream().sorted()` gives you a sorted stream and leaves the original list unchanged.

**Collection vs Stream?**
A collection **stores** data, and you can loop over it many times. A stream **processes** data once, lazily, and stores nothing.

**findFirst vs findAny?**
They're the same for normal streams. On parallel streams, findAny can return whichever match is found first, so it's faster.

**When should you use parallelStream()?**
Only for big, CPU-heavy work. It uses the shared ForkJoinPool, so it's bad for small lists or blocking calls like HTTP and DB (J06).

**What's peek() for?**
Debugging only. It lets you see elements as they pass through the pipeline.

**Optional.of vs Optional.ofNullable?**
`Optional.of(null)` throws a NullPointerException. `ofNullable(x)` gives an empty Optional when x is null.

---

## Numbers to remember

| What | Value |
|---|---|
| By id | Rahul, Priya, Amit, Sneha |
| By salary from high to low, then name | Priya, Sneha, Amit, Rahul |
| TreeSet by salary only | size 3 (Rahul and Amit tie) |
| findFirst(salary > 55,000) | Sneha, after 2 checks |
| Sum of salaries | 230,000 |
| map / flatMap / distinct over skills | 4 lists / 7 skills / 5 unique |

## Self-check (answer aloud, then click to check)

<details><summary>1. Sort the four employees by salary from high to low, then by name. What's the order?</summary>

Priya (70k), Sneha (60k), Amit (50k), Rahul (50k). Amit is before Rahul because of the name tie-break.

</details>

<details><summary>2. You put all four in a TreeSet with a comparator by salary only. What's the size?</summary>

3. Rahul and Amit compare as 0, so the set treats them as the same element and drops one.

</details>

<details><summary>3. You build employees.stream().filter(...).map(...) but never call a terminal operation. How many elements are processed?</summary>

None. Intermediate operations are lazy.

</details>

<details><summary>4. filter(salary > 55,000).findFirst() over [Rahul, Sneha, Amit, Priya]: how many employees are checked?</summary>

2: Rahul (no), then Sneha (yes), and it stops.

</details>

<details><summary>5. How many items do map(skills) and flatMap(skills) each produce for these four employees?</summary>

map gives 4 lists. flatMap gives 7 skills, and 5 after distinct().

</details>

<details><summary>6. findById(101).orElse(createDefault()): does createDefault() run? And with orElseGet?</summary>

With orElse, yes: its argument always runs. With orElseGet, no: the lambda only runs when the Optional is empty.

</details>

<details><summary>7. What happens if you call count() twice on the same stream?</summary>

The second call throws IllegalStateException: "stream has already been operated upon or closed".

</details>

If you get stuck on any of them, add it to [STUMBLE-LIST.md](../STUMBLE-LIST.md). When all 7 feel easy, tick J08 in the [README](../README.md) and send `next`.
