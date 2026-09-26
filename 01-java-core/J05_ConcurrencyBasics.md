# J05 · synchronized, volatile, and ConcurrentHashMap vs synchronizedMap

**Read this first (12 min). Then run [J05_ConcurrencyBasics.java](J05_ConcurrencyBasics.java) to watch each step happen.**

Don't memorize sentences. Understand the 6 steps and the example: **two threads each record 100,000 payments into one shared counter**, so the right answer is **200,000**. Once you get that, you can explain thread safety in your own words.

---

## The problem

In your payment system, many threads run at the same time: web requests, RabbitMQ consumers, scheduled jobs. When two threads change the **same data** at the same moment, updates get lost. There's no error. The numbers are just wrong.

Java gives you tools against this: `synchronized`, `volatile`, atomic classes and concurrent collections. Interviewers want to know what each one really guarantees.

## Real-life picture: one ledger, many cashiers

- **A race condition:** two cashiers update the same ledger. Both read "5 payments", both write "6", and one payment is lost.
- **synchronized:** the ledger sits in a room with **one key**. Only the cashier holding the key can go in. The others wait at the door.
- **volatile:** everyone reads the **live notice board**, never an old copy in their own diary. They always see the latest value, but "read, add 1, write" is still three separate steps.
- **AtomicInteger:** "write 6 only if it still says 5. If someone changed it, read again and retry."
- **synchronizedMap:** a bank branch with **one counter** for everything. Even a balance enquiry waits in the same queue.
- **ConcurrentHashMap:** a branch with **many counters**, one per section (bucket). Customers at different counters are served at the same time, and reading the display board needs no queue at all.

| Real life | Java |
|---|---|
| two cashiers overwriting each other | a race condition (`count++`) |
| a room with one key | `synchronized` |
| the live notice board | `volatile` |
| "write only if it still says 5, else retry" | `AtomicInteger` (compare-and-set) |
| one counter for everything | `Collections.synchronizedMap` |
| many counters, one per section | `ConcurrentHashMap` |

---

## Step by step

### Step 1 · The race condition: `count++` is three steps

`count++` looks like one step, but the CPU does three: **read** the value, **add 1**, **write** it back. Two threads can mix those steps up:

```text
count = 5
Thread A: read 5
Thread B: read 5          <- B reads before A has written
Thread A: write 6
Thread B: write 6         <- B overwrites A's update
count = 6, but it should be 7. One payment is lost.
```

In the demo, two threads each do `count++` 100,000 times on a plain `int`. On this laptop, three runs gave **135,300**, **118,516** and **139,589** instead of 200,000. Your numbers will be different every run, but they'll be wrong.

### Step 2 · synchronized: one thread at a time

```java
synchronized void increment() {
    value++;               // only one thread can be inside at a time
}
```

`synchronized` gives two guarantees:
1. **Mutual exclusion:** only one thread holds the lock, so read-add-write finishes before the next thread starts.
2. **Visibility:** when a thread leaves, its changes are visible to the next thread that takes the lock.

Result: **200,000** every time.

### Step 3 · volatile: always see the latest value, but still not atomic

Each CPU core can keep its own cached copy of a variable. Without `volatile`, one thread may never see another thread's change.

**The demo's stop flag:** a worker loops `while (!stop)`, and the main thread sets `stop = true` after half a second.
- Without volatile: the worker was **still running 1 second later**. It never saw the change, in all three runs.
- With `volatile boolean stop`: the worker **stopped** right away.

But volatile does **not** make `count++` safe. It only guarantees that you read the latest value; the three steps can still mix. The demo's `volatile int` counter got **171,978**, **138,466** and **132,514** instead of 200,000.

> **volatile = visibility. synchronized = visibility + one-at-a-time.**
> Use volatile for a flag that one thread writes and others read. Don't use it for counters.

### Step 4 · AtomicInteger: compare-and-set

`atomic.incrementAndGet()` doesn't take a lock. It uses the CPU's **compare-and-set** instruction:

```text
count = 5
Thread A: read 5, then "set 6 if it's still 5"  -> yes, now 6
Thread B: read 5, then "set 6 if it's still 5"  -> NO, it's 6 now
Thread B: read 6 again, then "set 7 if it's still 6" -> yes, now 7
```

Nothing is lost. Result: **200,000** every time. This is the best fit for counters.

### Step 5 · Three maps, two threads

The demo has two threads put **50,000 different keys each** into the same map, so the right size is **100,000**:

| Map | Size (three runs on this laptop) | Why |
|---|---|---|
| `HashMap` | 79,155 · 83,250 · 79,879 ❌ | not thread-safe: entries get lost when both threads write or resize at once |
| `Collections.synchronizedMap` | 100,000 ✅ | every method holds **one lock for the whole map** |
| `ConcurrentHashMap` | 100,000 ✅ | locks only the **bucket** being written; reads don't lock |

synchronizedMap and ConcurrentHashMap are both correct. The difference is **how much waiting** they cause:
- **synchronizedMap:** a thread writing to bucket 3 blocks a thread that only wants to read bucket 9, because there's just one lock.
- **ConcurrentHashMap (Java 8+):** an empty bucket is filled with compare-and-set (no lock). A busy bucket is locked on its own. Reads never lock. So many threads work at the same time.

### Step 6 · A thread-safe map does NOT make your logic thread-safe

```java
map.put("SUCCESS", map.get("SUCCESS") + 1);     // two separate calls
```

Each call is safe on its own, but another thread can run between the `get` and the `put`. That's the same lost update as Step 1. With two threads adding 50,000 each on a ConcurrentHashMap, the demo got about **52,000** instead of 100,000.

The fix is one atomic call:

```java
map.merge("SUCCESS", 1, Integer::sum);           // "add 1 to whatever is there", as one step
```

Result: **100,000**. Other atomic "check and act" calls are `putIfAbsent`, `computeIfAbsent` and `compute`.

> **Thread-safe methods don't make a sequence of calls safe. Use the one-step methods.**

### The whole topic in one table

| Tool | One at a time? | Always sees the latest value? | Use it for |
|---|---|---|---|
| `synchronized` | yes | yes | a block of code that must not mix |
| `volatile` | **no** | yes | a flag one thread writes and others read |
| `AtomicInteger` / `AtomicLong` | yes, for one variable (compare-and-set) | yes | counters, IDs |
| `synchronizedMap` | yes, one lock for the whole map | yes | rarely; low traffic only |
| `ConcurrentHashMap` | a lock per bucket, reads without a lock | yes | shared maps: caches, counters with `merge` |

---

## How to explain it in the interview

Use your own words. Cover these points in this order, using the shared payment counter:

1. `count++` is read, add, write. Two threads can interleave these steps and lose updates. That's a **race condition**.
2. **synchronized** lets one thread at a time into the block, and makes its changes visible to the next thread.
3. **volatile** only guarantees visibility: every read sees the latest write. It doesn't make `count++` atomic. It's good for flags, not counters. For counters, use **AtomicInteger** (compare-and-set) or synchronized.
4. **HashMap** isn't thread-safe. **synchronizedMap** uses one lock for everything. **ConcurrentHashMap** locks per bucket and reads without locks, so it scales much better. It doesn't allow nulls.
5. Even on a ConcurrentHashMap, **get-then-put is not atomic**. Use `merge`, `compute` or `putIfAbsent`.

**Here's how it can sound** (about a minute, simple words):

> "count++ is actually three steps, read, add and write, so two threads can both read 5 and both write 6, and one update is lost. synchronized fixes that by letting only one thread into the block at a time, and it also makes the changes visible to the next thread. volatile only solves visibility: a thread always sees the latest value, so it's good for something like a stop flag, but it doesn't make count++ atomic. For counters I'd use AtomicInteger, which uses compare-and-set. For maps, HashMap isn't thread-safe. Collections.synchronizedMap is safe but uses one lock for the whole map. ConcurrentHashMap locks only the bucket being updated and doesn't lock for reads, so it performs much better under load. One catch: even with ConcurrentHashMap, doing get and then put isn't atomic, so I use merge or computeIfAbsent for things like counting payment statuses."

**Tip:** draw the "read 5, read 5, write 6, write 6" timeline. It makes the race obvious in five seconds.

---

## Follow-up questions (simple answers)

**Why doesn't ConcurrentHashMap allow null keys or values?**
If `get(key)` returns null, you couldn't tell "not there" from "stored null". In a multi-threaded map, you can't safely check with `containsKey` in between, because another thread might change it.

**How did ConcurrentHashMap work in Java 7?**
It split the map into 16 segments, each with its own lock. Java 8 dropped segments: it locks a single bucket and uses compare-and-set for empty buckets, and long buckets become trees, just like HashMap (J01).

**How do you loop over a synchronizedMap safely?**
Wrap the loop in `synchronized (map) { ... }`, otherwise another thread can change it mid-loop. ConcurrentHashMap's iterators never throw ConcurrentModificationException. They may or may not show changes made during the loop.

**Hashtable vs ConcurrentHashMap?**
Hashtable locks the whole table for every call, like synchronizedMap. ConcurrentHashMap is the modern choice.

**synchronized method vs synchronized block?**
A synchronized method locks `this` (or the class, if it's static) for the whole method. A block can lock a smaller piece of code, or a different object, so threads wait less.

**What's a deadlock?**
Thread A holds lock 1 and waits for lock 2, while thread B holds lock 2 and waits for lock 1, so both wait forever. You avoid it by always taking locks in the same order, or by using `ReentrantLock.tryLock` with a timeout.

**ReentrantLock vs synchronized?**
ReentrantLock adds `tryLock`, timeouts, fairness and interruptible waiting. You must call `unlock()` in a `finally` block. synchronized releases the lock automatically.

**Where does this show up in payments?**
If it's true for you: counting statuses, in-memory caches of biller details, or a quick duplicate check with `processed.putIfAbsent(txnId, true)`, where only the first thread gets null back and processes that transaction.

*Only if they push further:* this is the "happens-before" rule. A write to a volatile variable is visible to every later read of it. Releasing a lock makes your changes visible to the next thread that takes the same lock.

---

## Numbers to remember

| What | Value |
|---|---|
| Two threads × 100,000 `count++` on a plain int | wrong, e.g. 118,516 to 139,589 |
| The same with volatile | still wrong |
| The same with synchronized or AtomicInteger | exactly 200,000 |
| HashMap with 2 threads × 50,000 keys | lost entries (about 80,000) |
| synchronizedMap / ConcurrentHashMap | exactly 100,000 |
| get-then-put on ConcurrentHashMap | wrong (about 52,000); `merge` gives 100,000 |

## Self-check (answer aloud, then click to check)

<details><summary>1. count is 5, and two threads run count++ at the same moment. What values can count end with?</summary>

6 or 7. If their read-add-write steps overlap, one update is lost and it ends at 6.

</details>

<details><summary>2. Does making the counter volatile fix count++?</summary>

No. volatile only makes the latest value visible. The read, add and write can still interleave.

</details>

<details><summary>3. One thread sets a stop flag, and a worker thread loops until it sees it. What do you use?</summary>

A volatile boolean. Without volatile, the worker may never see the change, as the demo showed.

</details>

<details><summary>4. Two threads each call incrementAndGet() 1,000 times on the same AtomicInteger. What's the final value?</summary>

2,000. Compare-and-set retries instead of losing updates.

</details>

<details><summary>5. Which map lets two threads write to different buckets at the same time: synchronizedMap or ConcurrentHashMap?</summary>

ConcurrentHashMap, because it locks per bucket. synchronizedMap has one lock for the whole map.

</details>

<details><summary>6. On a ConcurrentHashMap, is map.put(k, map.get(k) + 1) safe from two threads?</summary>

No. Each call is safe, but a thread can sneak in between them. Use map.merge(k, 1, Integer::sum).

</details>

<details><summary>7. Why doesn't ConcurrentHashMap allow null?</summary>

A null from get() would be ambiguous (missing, or a stored null?), and you can't safely double-check while other threads are changing the map.

</details>

If you get stuck on any of them, add it to [STUMBLE-LIST.md](../STUMBLE-LIST.md). When all 7 feel easy, tick J05 in the [README](../README.md) and send `next`.
