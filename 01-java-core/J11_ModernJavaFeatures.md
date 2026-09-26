# J11 ⭐ · Modern Java, 8 to 21: the features interviewers ask about

**Read this first (12 min). Then run [J11_ModernJavaFeatures.java](J11_ModernJavaFeatures.java) to watch each step happen.**

Don't memorize a version list. Understand the 6 steps with four payments:

| txnId | amount | status |
|---|---|---|
| TXN1 | 1,200 | SUCCESS |
| TXN2 | 450 | FAILED |
| TXN3 | 300 | PENDING |
| TXN4 | 15,000 | SUCCESS |

Once you've seen each feature work on these, you can answer "Which Java version do you use, and what's new in it?" from experience.

---

## The problem

"Which Java version do you use? What features have you used?" is a very common opening question. A weak answer is a memorized list. A strong answer names your version and shows a couple of features you really used, **with a reason**. This lesson gives you both.

## Real-life picture: phone OS updates

Java gets a new version **every 6 months**, in March and September, like phone OS updates. Every 2 years one of them is an **LTS** (Long-Term Support) release, which companies stay on for years because it keeps getting fixes.

| LTS version | Year | Headline features |
|---|---|---|
| **8** | 2014 | lambdas, streams, Optional, default methods, java.time |
| **11** | 2018 | `var` (from 10), String helpers, HttpClient, `java File.java` |
| **17** | 2021 | records, text blocks, switch expressions, sealed classes |
| **21** | 2023 | virtual threads, pattern matching for switch, record patterns |
| **25** | 2025 | the laptop you're using runs this (Step 6) |

Most companies today are on **17 or 21**.

---

## Step by step

### Step 1 · Lambdas and functional interfaces (Java 8)

A **functional interface** has exactly **one abstract method**, so a short lambda can implement it. The four you must know, with everyday pictures:

| Interface | Shape | Everyday picture | Demo |
|---|---|---|---|
| `Predicate<T>` | T → true/false | a security guard: allow or not | `p -> p.amount() > 10_000` |
| `Function<T, R>` | T → R | a currency exchange counter: rupees in, dollars out | `Payment::txnId` |
| `Consumer<T>` | T → nothing | a post box: takes something, gives nothing back | `id -> System.out.println(id)` |
| `Supplier<T>` | nothing → T | a vending machine: gives something, takes nothing | `() -> "TXN5"` |

The demo finds large payments: of 1,200, 450, 300 and 15,000, only **TXN4** is above 10,000.

You can write your own, too:

```java
@FunctionalInterface
interface FeeRule { int fee(int amount); }

FeeRule twoPercent = amount -> amount * 2 / 100;   // twoPercent.fee(1000) = 20
```

`@FunctionalInterface` makes the compiler complain if someone adds a second abstract method. `Runnable`, `Callable` and `Comparator` are functional interfaces too. That's why you can write them as lambdas (J06, J08).

`Payment::txnId` is a **method reference**, a shorter lambda for "call this method".

### Step 2 · var, text blocks and String helpers (Java 10, 11, 15)

```java
var payments = List.of(...);          // var (10): the compiler works out List<Payment>
```

`var` is **not** dynamic typing. The type is fixed when compiling, and you just don't repeat it. It only works for local variables with a value.

```java
String json = """
        {
          "txnId": "%s",
          "amount": %d
        }""".formatted("TXN1", 1_200);    // text block (15): multi-line text, no \n or +
```

Java 11 String helpers: `"  TXN1  ".strip()` gives "TXN1", `"   ".isBlank()` gives true, and `"=".repeat(10)` gives "==========".

### Step 3 · Switch expressions (Java 14)

```java
String action = switch (p.status()) {
    case SUCCESS -> "send receipt";
    case FAILED  -> "offer retry";
    case PENDING -> "check status later";
};
```

For the four payments this gives: TXN1 → send receipt, TXN2 → offer retry, TXN3 → check status later, TXN4 → send receipt.

What's better than the old switch:
- It **returns a value**.
- It has **no `break`**, so there's no accidental fall-through.
- For an enum, the compiler checks that **every case is covered**. Add a REFUNDED status and this code stops compiling until you handle it.

### Step 4 · Records, sealed types and pattern matching (Java 16, 17, 21)

```java
sealed interface PaymentResult permits Success, Failure, Pending { }   // sealed (17): ONLY these 3
record Success(String txnId, int amount) implements PaymentResult { }  // records (16)
record Failure(String txnId, String reason) implements PaymentResult { }
record Pending(String txnId) implements PaymentResult { }

String message = switch (result) {                                     // pattern matching for switch (21)
    case Success(String id, int amount) -> id + " paid Rs " + amount;   // record pattern: unpacks the fields
    case Failure f -> f.txnId() + " failed: " + f.reason();
    case Pending p -> p.txnId() + " is pending";
};                                                                      // no default needed
```

The output is "TXN1 paid Rs 1200", "TXN2 failed: insufficient balance" and "TXN3 is pending".

- A **record** is a data class in one line: fields, constructor, getters, equals/hashCode/toString (J02, J08). It's great for DTOs.
- **sealed** means the compiler knows **every** possible type, so the switch needs no `default`. Add a fourth type and every switch that forgets it fails to compile.
- **instanceof pattern** (16): `if (x instanceof Success s)` tests the type and gives you a typed variable in one step, with no cast.

### Step 5 · Virtual threads (Java 21)

**The picture:** platform threads are **full-time employees**. They're expensive, and you only have a few desks. Virtual threads are **gig workers** who occupy a desk only while actually working. When they wait (for an HTTP reply), the desk is free for someone else.

Here's the math for 1,000 tasks that each wait 100 ms:

| Executor | Math | Demo (three runs) |
|---|---|---|
| 50 platform threads | 1,000 / 50 = 20 rounds × 100 ms ≈ **2,000 ms** | 2,022 · 2,024 · 2,028 ms |
| virtual threads | all 1,000 wait at the same time ≈ **100 ms** | 114 · 114 · 111 ms |

Virtual threads are **cheap**, so you can have one per task, even millions. They help code that **waits a lot** (HTTP, DB). They don't make CPU-heavy work faster. In Spring Boot 3.2+ you can turn them on with `spring.threads.virtual.enabled=true`.

### Step 6 · Which Java is this?

The demo prints `Runtime.version()`: **25.0.3+9-LTS**, the latest LTS. In an interview, talk about the version you used **at work**. It's good to add that you've tried the newer ones.

---

## How to explain it in the interview

Use your own words. Cover these points in this order:

1. Java ships every 6 months, and the LTS versions are 8, 11, 17, 21 and 25. Say **which version you use at work** (only what's true for you).
2. **Java 8** was the big shift: lambdas, functional interfaces, streams, Optional, default methods and java.time.
3. Features you use from newer versions, **with a reason**: records for DTOs (less boilerplate), switch expressions (no fall-through, exhaustive), text blocks (JSON in tests), `var`.
4. **Java 17 and 21:** sealed classes plus pattern matching for switch let the compiler check that every case is handled. Virtual threads make blocking I/O scale with cheap threads.

**Here's how it can sound** (about a minute, simple words; change the version to yours):

> "At work we were on Java 17, and I've also tried 21. From Java 8 I use lambdas and streams every day, plus Optional for repository results. From the newer versions, I use records for DTOs, because one line gives me fields, getters, equals and hashCode. I use switch expressions for things like mapping a payment status to an action, since there's no fall-through and the compiler checks all enum values are covered. Text blocks are handy for JSON in tests. Java 21 adds virtual threads, which are very cheap threads, so a service that mostly waits on HTTP or DB calls can handle many more requests without a huge thread pool. It also adds pattern matching for switch, which together with sealed interfaces lets the compiler check that every result type is handled."

**Tip:** only claim the version and features you actually used. The next question is usually "where did you use it?"

---

## Follow-up questions (simple answers)

**What's a functional interface?**
An interface with exactly one abstract method (default and static methods are allowed), for example Runnable, Callable, Comparator, Predicate or Function. `@FunctionalInterface` makes the compiler enforce it.

**Lambda vs anonymous class?**
A lambda is shorter, and inside it `this` means the surrounding class. An anonymous class creates a new class, and its `this` is itself. Both can only use local variables that never change after they're set ("effectively final").

**What kinds of method references are there?**
- A static method: `Integer::parseInt`.
- A method on a specific object: `System.out::println`.
- A method on whatever object comes in: `String::length`.
- A constructor: `ArrayList::new`.

**Can a record extend a class, or be a JPA entity?**
It can't extend a class (records already extend `Record`), but it can implement interfaces. It's not good as a JPA entity, because entities need a no-arg constructor and changeable fields. Records are perfect for DTOs and API responses.

**Sealed class rules?**
`permits` lists the allowed subtypes. Each subtype must be `final`, `sealed` or `non-sealed`.

**When should you not use virtual threads?**
For CPU-heavy work, which gets no speed-up. Don't put them in a pool either, since they're cheap to create. In Java 21 to 23, a virtual thread waiting inside a `synchronized` block kept its carrier thread busy. Java 24 fixed that.

*Only if they push further (Java 25):* a small program can now be just `void main() { ... }`, with no class around it. Constructors can run statements before `super(...)`. You can import a whole module with one line.

---

## Numbers to remember

| What | Value |
|---|---|
| LTS versions | 8, 11, 17, 21, 25 |
| New version every | 6 months (March, September) |
| Large payments (> 10,000) | TXN4 only |
| 1,000 tasks × 100 ms on 50 platform threads | about 2,000 ms |
| The same on virtual threads | about 100 ms |

## Self-check (answer aloud, then click to check)

<details><summary>1. Which Java versions are LTS?</summary>

8, 11, 17, 21 and 25.

</details>

<details><summary>2. Which functional interface fits "is this payment above ₹10,000?"</summary>

Predicate&lt;Payment&gt;, which takes a payment and returns true or false.

</details>

<details><summary>3. What does a switch expression give you that the old switch statement didn't?</summary>

It returns a value, has no break or fall-through, and the compiler checks that every enum case is covered.

</details>

<details><summary>4. Why doesn't the switch over PaymentResult need a default branch?</summary>

PaymentResult is sealed, so the compiler knows the only possible types: Success, Failure and Pending.

</details>

<details><summary>5. 1,000 tasks of 100 ms each: about how long on 50 platform threads, and on virtual threads?</summary>

About 2,000 ms (20 rounds of 100 ms) on 50 platform threads, and about 100 ms on virtual threads (all waiting together).

</details>

<details><summary>6. Is var dynamic typing?</summary>

No. The type is fixed at compile time; you just don't write it out. It only works for local variables with a value.

</details>

<details><summary>7. Where would you use a record in a Spring Boot app?</summary>

DTOs, request and response bodies, and events. Not JPA entities.

</details>

If you get stuck on any of them, add it to [STUMBLE-LIST.md](../STUMBLE-LIST.md). When all 7 feel easy, tick J11 in the [README](../README.md) and send `next`.
