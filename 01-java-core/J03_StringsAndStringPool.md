# J03 · String immutability, the String pool, StringBuilder vs StringBuffer

**Read this first (10 min). Then run [J03_StringsAndStringPool.java](J03_StringsAndStringPool.java) to watch each step happen.**

Don't memorize sentences. Understand the 6 steps and the example with the string **"PAYU"**. Once you get those, you can answer any String question in your own words.

---

## The problem

Strings are everywhere in your code: transaction IDs, statuses like "SUCCESS", gateway names like "PAYU", JSON. Java does two things with them that surprise people:

1. A String **can never change**. Every "change" makes a new String.
2. The same text written in your code is **shared**, as one object, through the String pool.

Because of this, `==` sometimes says true and sometimes says false for the same text. It also means building a long String with `+` in a loop is slow. Interviewers ask about all of it.

## Real-life picture: the society notice board

- A **String** is a printed notice. You can't edit a printed notice. To change it, you print a new one.
- The **String pool** is the society notice board. There's one copy of each notice, and everyone reads that same copy.
- **`new String("PAYU")`** is asking for your own photocopy: a separate sheet, even though the words are the same.
- A **StringBuilder** is a whiteboard. You keep writing on the same board.
- A **StringBuffer** is a whiteboard in a room with a lock, so only one person can write at a time.

| Society | Java |
|---|---|
| a printed notice (can't be edited) | a `String` (immutable) |
| the notice board, one shared copy | the String pool |
| your own photocopy | `new String("PAYU")`, a separate object |
| a whiteboard | `StringBuilder` |
| a whiteboard in a locked room | `StringBuffer` (synchronized) |

```text
      String pool (the notice board)
a ──┐    ┌────────┐
    ├───>│ "PAYU" │      a and b share ONE object
b ──┘    └────────┘

c ──────> "PAYU"         new String("PAYU"): a separate object
```

---

## Step by step

### Step 1 · A String never changes

```java
String gateway = "PAYU";
gateway.toLowerCase();              // makes a NEW String "payu", but we didn't keep it
System.out.println(gateway);        // PAYU, unchanged
```

To "change" it, you point the variable at the new String:

```java
String other = gateway;             // another variable pointing to "PAYU"
gateway = gateway.toLowerCase();    // gateway now points to the new "payu"
System.out.println(other);          // still PAYU
```

```text
before:  gateway ──> "PAYU" <── other
after:   gateway ──> "payu"     other ──> "PAYU"   (the old String is untouched)
```

> **The variable moved. The String object never changed.**
> Every "changing" method (toLowerCase, replace, substring, trim, concat) returns a **new** String.

### Step 2 · The String pool

```java
String a = "PAYU";                 // a literal goes into the pool
String b = "PAYU";                 // the pool already has it, so b gets the SAME object
String c = new String("PAYU");     // "new" always creates a separate object
```

| Check | Result | Why |
|---|---|---|
| `a == b` | **true** | the same object from the pool |
| `a == c` | **false** | c is a separate object |
| `a.equals(c)` | **true** | the same characters |
| `a == c.intern()` | **true** | `intern()` returns the pool's copy |

`==` asks "the same object?" and `equals()` asks "the same text?" (J02). **For Strings, always use `equals()`.**

The pool saves memory. If 50 classes in your code use the literal "SUCCESS", there's still only **one** "SUCCESS" object.

### Step 3 · Why Strings are immutable

**Reason 1: the pool depends on it.** a and b share one object. If a could change it to "SETU", b would suddenly say "SETU" too. Sharing is only safe because nobody can change the shared copy. On a notice board that anyone could scribble on, nobody could trust any notice.

**Reason 2: safe HashMap keys (J01).** A key must not change after `put()` (J01 Step 8). A String can't change, so it's the perfect key. It even saves its hashCode after calculating it once.

**Reason 3: thread-safe for free.** Many threads can read the same String with no locks, because nobody can modify it.

**Reason 4: security.** A DB username, a file path or a URL can't be changed after your code checks it.

### Step 4 · Joined by the compiler vs joined while running

```java
"PA" + "YU" == "PAYU"         // true: the compiler joins them BEFORE the program runs, so it's the pool's "PAYU"

String pa = "PA";
pa + "YU" == "PAYU"           // false: joined WHILE running, which creates a new object

final String fpa = "PA";
fpa + "YU" == "PAYU"          // true: a final variable holding a literal is a constant, so the compiler joins it
```

> **The compiler joins only fixed text. Anything joined while the program runs is a new object.**
> This is another reason never to compare Strings with `==`.

### Step 5 · Joining in a loop: String vs StringBuilder

Say you append the 7-character ID "TXN0001" five times with `+=`. Every round creates a **brand-new** String and copies all the characters so far into it:

| Round | New String length | Characters copied |
|---|---|---|
| 1 | 7 | 7 |
| 2 | 14 | 14 |
| 3 | 21 | 21 |
| 4 | 28 | 28 |
| 5 | 35 | 35 |
| **total** | | **105** |

A StringBuilder writes into one growing buffer, so it writes each character once: **35**.
With n rounds, `+=` does about n × n / 2 work, while StringBuilder does about n. The demo times 50,000 rounds. On this laptop, `+=` took about 170 to 270 ms and StringBuilder about 1 ms. Your numbers will be different, but the gap will still be huge.

> **Joining in a loop? Use StringBuilder.**
> A single line like `"Hello " + name` is fine, because Java already makes it efficient.

### Step 6 · StringBuilder vs StringBuffer

They have the same methods: `append`, `insert`, `reverse`, `delete`. The only difference is locking:

- **StringBuffer** (old, Java 1.0): every method is `synchronized`, so one thread at a time. It's thread-safe, but slower.
- **StringBuilder** (Java 5): no locking. It's faster, but not safe to share between threads.

The demo has two threads each append "x" 100,000 times to the same object. The correct length is 200,000:

- **StringBuffer:** 200,000 every time.
- **StringBuilder:** fewer, and a different number each run. On this laptop three runs gave 113,739, 119,494 and 118,697. Appends get lost because both threads write to the same internal array at the same time. In rare runs it can even crash with `ArrayIndexOutOfBoundsException`.

In real code you build a String inside one method, with one thread. So **StringBuilder is the default choice**. StringBuffer is almost never needed.

### The whole topic in one table

| | String | StringBuilder | StringBuffer |
|---|---|---|---|
| Can change? | no | yes | yes |
| Thread-safe? | yes (nobody can change it) | **no** | yes (synchronized) |
| Speed for many joins | slow (new object each time) | fastest | a bit slower (locking) |
| Use it for | fixed text, keys, constants | building text in a method or loop | rarely: text shared between threads |

---

## How to explain it in the interview

Use your own words. Cover these points in this order, using "PAYU":

1. A String is **immutable**. Every change creates a new object, and the old one stays the same.
2. String literals live in the **String pool** and are shared. `new String()` always creates a separate object. So compare Strings with **equals()**, not `==`.
3. Immutability is what makes the pool safe. It also makes Strings safe **HashMap keys**, thread-safe and secure.
4. Joining in a loop with `+` creates a new String every round, so use **StringBuilder**.
5. **StringBuilder** isn't synchronized, so it's fast. **StringBuffer** is synchronized, so it's thread-safe but slower. StringBuilder is the default.

**Here's how it can sound** (about a minute, simple words):

> "String in Java is immutable. If I call toLowerCase on "PAYU", I get a new String "payu", and the original "PAYU" doesn't change. String literals are stored in the String pool, so if I write "PAYU" twice, both variables point to the same object. But new String("PAYU") always creates a separate object. That's why we compare Strings with equals, not ==. Immutability is what makes this sharing safe: if one reference could change the pooled "PAYU", every other reference would see the change. It also makes Strings good HashMap keys and thread-safe. Because every change creates a new object, joining strings in a loop with + is slow, so I use StringBuilder. StringBuffer does the same thing but its methods are synchronized, so it's thread-safe and slower. In normal code StringBuilder is the right choice."

**Tip:** if you're asked "a == b?" questions, draw the pool box with arrows, as in the picture at the top, then answer.

---

## Follow-up questions (simple answers)

**How many objects does `new String("PAYU")` create?**
Up to two. One is the literal "PAYU" in the pool, created only if it isn't already there. The other is the new object on the heap. If the pool already has "PAYU", only one new object is created.

**Where is the String pool?**
In the heap, since Java 7. Before that it was in PermGen (J09).

**What does `intern()` do?**
It returns the pool's copy of that text, adding it to the pool first if it's missing.

**Why is the String class `final`?**
So nobody can write a subclass that changes the text. That would break the pool and every guarantee above.

**Why store a password in a `char[]` and not a String?**
You can't erase a String. It stays in memory until garbage collection and might even sit in the pool. You can overwrite a `char[]` with zeros right after use: `Arrays.fill(password, '0')`.

**Does `a + b` use StringBuilder internally?**
Older Java turned `+` into StringBuilder calls. Since Java 9, it uses a faster built-in mechanism. Either way, each `+` expression makes one new String. So in a loop, still use StringBuilder.

**equals() vs equalsIgnoreCase() vs compareTo()?**
equals() checks the exact text. equalsIgnoreCase() ignores case, so "payu" matches "PAYU". compareTo() is for sorting: it returns a negative number, 0 or a positive number (J08).

*Only if they push further:* since Java 9, a String of plain Latin-1 characters stores 1 byte per character instead of 2 ("compact strings"), which halves the memory for most text. String also caches its hashCode in a field after the first call.

---

## Numbers to remember

| What | Value |
|---|---|
| Two literals with the same text | one shared object (`==` is true) |
| `new String("x")` | always a new object, plus the pool copy if it's missing |
| Comparing Strings | `equals()`, never `==` |
| 5 joins of 7 characters with `+=` | 105 characters copied |
| The same with StringBuilder | 35 characters written |
| StringBuilder | not synchronized, fastest |
| StringBuffer | synchronized, thread-safe |

## Self-check (answer aloud, then click to check)

<details><summary>1. a = "PAYU", b = "PAYU", c = new String("PAYU"). What are a == b, a == c, a.equals(c) and a == c.intern()?</summary>

They are true, false, true and true. a and b share the pool object, c is separate, the text is the same, and intern() returns the pool copy.

</details>

<details><summary>2. String s = "PAYU"; s.toLowerCase(); System.out.println(s); What prints, and why?</summary>

PAYU prints. toLowerCase() returned a new String, and nobody kept it. s still points to the original.

</details>

<details><summary>3. What do "PA" + "YU" == "PAYU", then pa + "YU" == "PAYU" (with a normal variable pa = "PA"), then the same with final pa give?</summary>

They give true, false and true. The compiler joins fixed text, and a final variable holding a literal counts as fixed text. Joining a normal variable happens while the program runs, which creates a new object.

</details>

<details><summary>4. You append "TXN0001" 4 times with +=. How many characters are copied in total?</summary>

7 + 14 + 21 + 28 = 70. StringBuilder would write only 28.

</details>

<details><summary>5. new String("SETU"), when the pool doesn't have "SETU" yet: how many objects are created?</summary>

Two: the pool literal "SETU" and the separate heap object.

</details>

<details><summary>6. You build a CSV line from 1,000 transactions inside a method. Which class do you use?</summary>

StringBuilder. It's a loop, so avoid String +=, and it runs in one thread, so StringBuffer's locking isn't needed.

</details>

<details><summary>7. Why does the String pool need Strings to be immutable?</summary>

Many variables share one pooled object. If one of them could change it, all the others would see the change.

</details>

If you get stuck on any of them, add it to [STUMBLE-LIST.md](../STUMBLE-LIST.md). When all 7 feel easy, tick J03 in the [README](../README.md) and send `next`.
