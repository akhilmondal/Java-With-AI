# J09 · JVM memory (stack, heap, metaspace), GC basics, OutOfMemoryError vs StackOverflowError

**Read this first (12 min). Then run [J09_JvmMemoryAndGc.java](J09_JvmMemoryAndGc.java) to watch each step happen.**

Don't memorize sentences. Understand the 6 steps and one example: **`processPayment(1500)` creates a `Payment` object.** Once you know where each part of that line lives in memory, you can explain JVM memory in your own words.

```java
void processPayment(int amount) {                    // amount = 1500
    Payment p = new Payment("TXN1001", amount);     // p and the Payment object
    validate(p);
}
```

---

## The problem

When your service slows down or crashes with `OutOfMemoryError`, you need to know **where** Java keeps things and **who** cleans them up. Interviewers ask this to check that you understand what happens under your code, not just the syntax.

## Real-life picture: a restaurant

- **Stack:** each waiter's **own notepad** (one per thread). Every order they're working on is a page (a method call). When the order is done, the page is torn off. It's small, fast and private.
- **Heap:** the **shared storeroom** where the real items (objects) are kept. Every waiter can use it.
- **Garbage collector:** the **cleaner** who throws away items that no notepad or notice board points to any more.
- **Young and old generations:** new items go to a small **arrivals shelf** (Eden). Most are thrown away quickly. Items that survive a few cleanings move to **long-term storage** (the old generation).
- **Metaspace:** the **recipe cabinet**, holding class definitions. It's kept outside the storeroom.
- **StackOverflowError:** a waiter's notepad **runs out of pages** because orders keep creating orders without ever finishing (recursion).
- **OutOfMemoryError:** the **storeroom is full** and the cleaner can't free enough space.

| Restaurant | JVM |
|---|---|
| the waiter's own notepad | the thread's **stack** |
| one page per order in progress | a stack **frame** per method call |
| the shared storeroom | the **heap** |
| the cleaner | the **garbage collector** |
| arrivals shelf / long-term storage | **young** (Eden + survivor) / **old** generation |
| the recipe cabinet | **metaspace** (class metadata) |

```text
 Thread's stack            Heap (shared by all threads)            Metaspace (outside the heap)
 +------------------+     +-------------------------------+      +------------------------+
 | validate(p)      |     |                               |      | class Payment:         |
 +------------------+     |  Payment{TXN1001, 1500} <-----+--+   |  fields, methods, ...  |
 | processPayment() |     |                               |  |   +------------------------+
 |   amount = 1500  |     |  "TXN1001" (String pool, J03) |  |
 |   p  ------------+-----+-------------------------------+--+
 +------------------+     +-------------------------------+
 | main()           |
 +------------------+
```

---

## Step by step

### Step 1 · The stack: one frame per method call

- Every thread has **its own** stack.
- Every method call **pushes a frame**. A frame holds that method's parameters and local variables: primitives like `amount = 1500`, and **references** like `p`.
- When the method returns, its frame is **popped**. Everything in it disappears on the spot, with no garbage collection needed.

The demo asks Java which frames are on the stack inside `validate`: **[validate, processPayment, main]**, three frames with the newest on top. When `validate` returns, its frame goes; when `processPayment` returns, so does `amount`.

A stack is small, around 512 KB to 1 MB per thread by default (set with `-Xss`).

### Step 2 · The heap: where every object lives

- **All objects and arrays** live on the heap: the `Payment` object, Strings (including the String pool, J03) and lists.
- The heap is **shared** by all threads, which is why the thread-safety tools in J05 exist.
- Its size is set with `-Xms` (start) and `-Xmx` (maximum). By default the maximum is **about a quarter of your RAM**.

On this laptop: RAM **15,765 MB**, and the default max heap **3,942 MB**. 15,765 ÷ 4 = 3,941, so the rule holds.

**Pass-by-value** is a classic question that becomes easy once you know stack vs heap. Java always passes a **copy**. For an object, it's a copy of the **reference** (the arrow), not a copy of the object:

| In the method | What the caller sees | Why |
|---|---|---|
| `copy.amount = 2000` | amount is **2000** | the copied arrow points to the **same** heap object |
| `copy = new Payment(...)` | still **TXN1001** | only the copied arrow moved; the caller's `p` didn't |

> **Java is always pass-by-value. For objects, the value that gets copied is the reference.**

### Step 3 · Metaspace, and the heap's young and old areas

The demo lists the JVM's real memory areas. On this laptop they were:

| Area | Used | What it holds |
|---|---|---|
| G1 Eden Space | 40 MB | brand-new objects (young generation) |
| G1 Survivor Space | 0 MB | objects that survived a young GC |
| G1 Old Gen | 1 MB | long-lived objects |
| Metaspace | 10 MB | class metadata: fields and methods of every loaded class |
| Compressed Class Space, CodeHeap | small | the JVM's own bookkeeping and compiled code |

**Metaspace** replaced **PermGen** in Java 8. It lives in native memory, outside the heap, and grows as needed (you can cap it with `-XX:MaxMetaspaceSize`).

### Step 4 · Garbage collection

**Who is garbage?** Any object that can't be reached from a **GC root**. A GC root is a starting point the GC always trusts, such as local variables on a thread's stack, static fields or active threads. Follow the arrows from the roots; anything you can't reach is garbage.

**Most objects die young.** Request objects, DTOs and temporary strings are gone within milliseconds. So the heap is split:

- **Minor (young) GC:** cleans only the small young area. It's frequent and fast.
- **Major / full GC:** also cleans the old area. It's less frequent and slower.

The demo creates about **700 MB** of short-lived garbage. Meanwhile **5 young GCs** ran on their own; nobody called them.

**Unreachable means collectable.** The demo watches a Payment with a `WeakReference`, which observes an object without keeping it alive. After `payment = null` and `System.gc()`, it printed **collected**. `System.gc()` is only a **request**. Normally you never call it; the JVM decides.

**Which collector?** **G1** is the default since Java 9: it splits the heap into regions and aims for short pauses. **ZGC** is for very low pauses on big heaps. **Parallel** is for maximum throughput. A **"stop-the-world" pause** is a moment when application threads wait while the GC works.

### Step 5 · StackOverflowError: the notepad runs out of pages

```java
static void callMyselfForever() {
    depth++;
    callMyselfForever();        // every call adds a frame, and none ever returns
}
```

The demo hit `StackOverflowError` after **13,658** nested calls, and **13,102** on another run. The exact number changes with the JVM and frame sizes. The cause is almost always **recursion without a correct stopping condition**.

### Step 6 · OutOfMemoryError: the storeroom is full

The demo asks for one array **twice the size of the whole heap**:

```text
OutOfMemoryError: Java heap space (asked for 7884 MB, max heap is 3942 MB)
```

In real apps, OOM usually comes from a **memory leak**: objects that are no longer needed but are **still reachable**, so the GC can't remove them. The classic case is a static map used as a cache that only grows:

```java
static Map<String, PaymentStatus> cache = new HashMap<>();   // every txnId added, never removed
```

The fixes are a size limit and eviction (the LRU cache from J04), or a proper cache library, and removing entries you no longer need.

Other kinds of OOM: `Metaspace` (too many classes loaded), `GC overhead limit exceeded` (the GC runs all the time but frees almost nothing) and `unable to create native thread` (too many threads, J06).

### The whole topic in one table

| | Stack | Heap | Metaspace |
|---|---|---|---|
| Holds | frames: primitives and references | all objects and arrays | class metadata |
| Shared? | no, one per thread | yes | yes |
| Freed when | the method returns | the GC finds it unreachable | the class is unloaded |
| Too full | **StackOverflowError** | **OutOfMemoryError: Java heap space** | **OutOfMemoryError: Metaspace** |
| Size flag | `-Xss` | `-Xms` / `-Xmx` | `-XX:MaxMetaspaceSize` |

---

## How to explain it in the interview

Use your own words. Cover these points in this order, using `processPayment(1500)`:

1. Each **thread has a stack**. Every method call adds a frame with its local variables: primitives and references. The frame is removed when the method returns.
2. **Objects live on the heap**, which all threads share and the GC manages. It's split into a **young generation** (Eden and survivor spaces) and an **old generation**. Its size is set with `-Xms` and `-Xmx`.
3. **Metaspace** (since Java 8, replacing PermGen) holds class metadata in native memory.
4. The **GC** removes objects that can't be reached from GC roots. Most objects die young, so minor GCs are frequent and cheap. G1 is the default collector.
5. **StackOverflowError** means the thread's stack is full, usually from deep recursion. **OutOfMemoryError** means the heap or metaspace can't fit new objects, often because of a leak where objects stay referenced, like an ever-growing static map.

**Here's how it can sound** (about a minute, simple words):

> "Every thread has its own stack. Each method call adds a frame with its parameters and local variables, and it's removed when the method returns. So in processPayment(1500), amount and the reference p are on the stack, while the Payment object itself is on the heap, which is shared by all threads. The heap is managed by the garbage collector and split into young and old generations. New objects go into Eden, and since most objects die young, minor GCs clean that area often and cheaply. Class metadata lives in Metaspace, which replaced PermGen in Java 8. The GC removes objects that are no longer reachable from GC roots like stack variables and static fields. StackOverflowError means a thread's stack is full, usually from infinite recursion. OutOfMemoryError means the heap is full, often due to a leak, for example a static map that keeps growing."

**Tip:** draw the three boxes (stack, heap, metaspace) with an arrow from `p` to the object. It answers five follow-ups at once.

---

## Follow-up questions (simple answers)

**Is Java pass-by-value or pass-by-reference?**
Always by value. For objects, the value is a copy of the reference. The method can change the object's fields, but it can't make the caller's variable point to a different object (Step 2).

**Where do static variables live?**
The values of static fields live on the heap (since Java 8), together with the class's `Class` object. The class's metadata lives in metaspace.

**When does an object become eligible for GC?**
When no chain of references from a GC root reaches it. For example, after `p = null`, or when the method holding the only reference returns.

**Can you force garbage collection?**
No. `System.gc()` is only a request the JVM may ignore, and calling it in production code is a smell.

**How do you find the cause of an OutOfMemoryError?**
Start the app with `-XX:+HeapDumpOnOutOfMemoryError` so the JVM saves a heap dump when it crashes. Then open the dump in a tool like Eclipse MAT or VisualVM and see which objects fill the heap and who references them.

**What are common memory leaks in Java?**
Static collections that only grow, caches without eviction, connections and streams that aren't closed (use try-with-resources, J07), listeners that are never removed, and ThreadLocal values left behind in pooled threads.

**What does -Xms vs -Xmx mean?**
-Xms is the starting heap size. -Xmx is the maximum. In containers they're often set to the same value to avoid resizing.

*Only if they push further:* G1 splits the heap into many equal regions and collects the ones with the most garbage first, hence the name "Garbage First". Objects bigger than half a region are "humongous" and go straight into old-generation regions.

---

## Numbers to remember

| What | Value |
|---|---|
| Default max heap | about 1/4 of RAM (here: 15,765 MB RAM → 3,942 MB) |
| Stack per thread | about 512 KB to 1 MB by default (`-Xss`) |
| Frames inside validate() | 3: validate, processPayment, main |
| StackOverflowError in the demo | after about 13,000 nested calls (varies) |
| Default collector | G1 (since Java 9) |
| PermGen → Metaspace | Java 8 |

## Self-check (answer aloud, then click to check)

<details><summary>1. In processPayment(1500), where do amount, p and the Payment object live?</summary>

amount and p (the reference) are in processPayment's frame on the stack. The Payment object is on the heap.

</details>

<details><summary>2. Where is the metadata for the class Payment (its fields and methods) stored?</summary>

In metaspace (native memory, since Java 8).

</details>

<details><summary>3. After p = null, is the Payment object deleted right away?</summary>

No. It only becomes eligible. The GC removes it later, when it runs.

</details>

<details><summary>4. A method calls itself with no stopping condition. Which error do you get, and why?</summary>

StackOverflowError. Every call adds a frame to the thread's stack until the stack is full.

</details>

<details><summary>5. A static HashMap caches every transaction and never removes any. What happens eventually?</summary>

OutOfMemoryError: Java heap space. The entries stay reachable, so the GC can never free them. That's a leak.

</details>

<details><summary>6. A laptop has 16 GB of RAM. About how big is the default max heap?</summary>

About 4 GB, a quarter of RAM.

</details>

<details><summary>7. A method sets p.amount = 2000 and then does p = new Payment(...). What does the caller see?</summary>

amount 2000 on the caller's original object. The reassignment only changed the method's own copy of the reference.

</details>

If you get stuck on any of them, add it to [STUMBLE-LIST.md](../STUMBLE-LIST.md). When all 7 feel easy, tick J09 in the [README](../README.md) and send `next`.
