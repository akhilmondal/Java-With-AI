# J06 · ExecutorService, Future and CompletableFuture

**Read this first (12 min). Then run [J06_ExecutorsAndCompletableFuture.java](J06_ExecutorsAndCompletableFuture.java) to watch each step happen.**

Don't memorize sentences. Understand the 7 steps and one example: fetching bills from **3 billers**. Electricity is ₹1,200, water ₹450 and gas ₹300, so the total is **₹1,950**. Each call takes **300 ms**. Once you get that, you can explain async Java in your own words.

---

## The problem

For each biller, your service makes an HTTP call and waits for the answer. Done one after another, 3 calls × 300 ms = **900 ms**, and the user waits the whole time. The calls don't depend on each other, so they could run **at the same time**, in about **300 ms**.

The tools for that are a **thread pool** (ExecutorService), a **receipt for the result** (Future), and a **chain of steps that run when the result arrives** (CompletableFuture).

## Real-life picture: a food court

- **Thread pool:** the kitchen has a fixed number of cooks (threads). New orders (tasks) wait in a queue, and a free cook picks up the next one. Hiring a new cook for every single order would be slow and would crash the kitchen at rush hour.
- **Future:** the token they give you when you order. You can go and sit down, but `get()` means standing at the counter until your food is ready.
- **CompletableFuture:** the buzzer, plus instructions you gave up front. "When the pizza is ready, add toppings (`thenApply`), then bring it to table 5 (`thenAccept`)." Nobody stands and waits.
  - `thenCombine`: when the burger AND the fries are both ready, put them on one tray.
  - `allOf`: buzz me when all 3 of my orders are ready.
  - `exceptionally`: if the pizza oven breaks, give me a sandwich instead.

| Food court | Java |
|---|---|
| cooks in the kitchen | threads in the pool |
| the queue of orders | the pool's task queue |
| an order | a task (`Runnable` or `Callable`) |
| your token | `Future` |
| standing at the counter | `future.get()`, which blocks |
| a buzzer with instructions | `CompletableFuture` with `thenApply` / `thenAccept` |

---

## Step by step

### Step 1 · One after another

```java
for (String biller : billers) {
    total += fetchBill(biller);      // each call waits 300 ms
}
```

3 × 300 = **900 ms**. The demo measured 902 ms.

### Step 2 · A thread pool runs them at the same time

```java
ExecutorService pool = Executors.newFixedThreadPool(3);   // 3 cooks
Future<Integer> f = pool.submit(() -> fetchBill("WATER")); // returns at once with a token
int amount = f.get();                                       // wait for this one result
```

Do the time math by hand. With 3 calls of 300 ms each:

| Threads in the pool | What happens | Time |
|---|---|---|
| 1 | one, then the next, then the next | 900 ms |
| 2 | two together (300), then the last one (300) | **600 ms** |
| 3 | all three together | **300 ms** |

The demo measured **302 ms** with 3 threads and **602 ms** with 2.

**Why a pool, and not `new Thread()` for every task?** Every thread costs memory (its own stack, often about 1 MB) and time to create. 10,000 requests would mean 10,000 threads, and the server falls over. A pool **reuses** a fixed number of threads, and extra tasks wait in the queue.

### Step 3 · Future: the token, and its limits

- `submit(Callable)` gives back a `Future`. A **Callable** returns a value and can throw checked exceptions. A **Runnable** returns nothing.
- `future.get()` **blocks** until the result is ready.
- `future.get(200, MILLISECONDS)` waits at most 200 ms, then throws `TimeoutException`. The demo does this with a 1-second call, then calls `cancel(true)` to stop that task.
- The limits: `get()` makes you wait, and doing "when A is done, do B" or combining A and B means writing your own waiting code.

### Step 4 · CompletableFuture: chain, combine, all

**Chain**, where nobody waits in the middle:

```java
CompletableFuture
    .supplyAsync(() -> fetchBill("ELECTRICITY"), pool)   // 1200, runs in the pool
    .thenApply(amount -> amount + 10)                     // add the ₹10 fee: 1210
    .thenAccept(amount -> System.out.println("pay " + amount));
```

**Combine two parallel calls:** the bill (₹1,200) and the fee (₹10) are fetched **at the same time**, then added together:

```java
bill.thenCombine(fee, Integer::sum)      // ₹1,210 in about 300 ms, not 600
```

The demo measured 302 ms.

**All three billers:**

```java
CompletableFuture.allOf(call1, call2, call3).join();   // wait for all
// then add call1.join() + call2.join() + call3.join() = ₹1,950
```

The demo got ₹1,950 in **301 ms**.

`join()` is like `get()`, but it throws an unchecked exception, so you don't need a try/catch.

### Step 5 · Errors and timeouts

**A biller is down.** Use `exceptionally` to give a fallback value:

```java
supplyAsync(() -> fetchBill("GAS"))          // throws "GAS biller is down"
    .exceptionally(error -> 0)               // use 0 instead, and the chain carries on
```

The error arrives wrapped in a `CompletionException`, so `error.getCause()` is the real exception. `handle((result, error) -> ...)` sees both the success and the failure case.

**A biller is slow.** Use `completeOnTimeout` (Java 9+):

```java
supplyAsync(() -> fetchSlowBill())                    // takes 1,000 ms
    .completeOnTimeout(0, 400, MILLISECONDS)          // after 400 ms, just use 0
```

`orTimeout(400, MILLISECONDS)` fails with a TimeoutException instead of using a default value.

### Step 6 · thenApply vs thenCompose

- **`thenApply`** is for a normal next step, where a value goes in and a value comes out: `amount -> amount + 10`.
- **`thenCompose`** is for a next step that is **itself async**, where a value goes in and a CompletableFuture comes out: `amount -> payAsync(amount)`.

If you used thenApply for an async step, you'd get a `CompletableFuture<CompletableFuture<String>>`, a box inside a box. thenCompose flattens it. It's the same idea as map vs flatMap in streams (J08).

The demo fetches the water bill (₹450), then pays it asynchronously and gets the ID **PAY-WATER-450**.

### Step 7 · Always shut the pool down

- `shutdown()`: accept no new tasks, and let the running and queued tasks finish.
- `shutdownNow()`: also interrupt the running tasks, and return the ones still waiting.
- `awaitTermination(5, SECONDS)`: wait for it to finish.

If you forget `shutdown()`, the pool's threads keep the program running even after `main` ends.

### The whole topic in one table

| Tool | What it gives you | Watch out for |
|---|---|---|
| `ExecutorService` | a reusable pool of threads | call `shutdown()`; size the pool sensibly |
| `Future` | a token for one result | `get()` blocks; combining is manual |
| `CompletableFuture` | chaining, combining, error handling and timeouts without blocking | without an executor it uses a shared pool, so pass your own pool for slow I/O calls |

---

## How to explain it in the interview

Use your own words. Cover these points in this order, using the 3 billers:

1. Creating a thread per task is expensive, so use a **thread pool** (ExecutorService). It reuses a fixed set of threads, and extra tasks wait in a queue. With 3 threads, 3 calls of 300 ms take 300 ms instead of 900.
2. `submit()` returns a **Future**. `get()` blocks until the result is ready, and `get(timeout)` gives up after a while. Callable returns a value, Runnable doesn't.
3. **CompletableFuture** lets you chain steps without blocking (`thenApply`, `thenAccept`), combine calls (`thenCombine`, `allOf`), handle errors (`exceptionally`, `handle`) and add timeouts (`orTimeout`, `completeOnTimeout`).
4. `thenCompose` is for a step that is itself async, so the result doesn't become a future inside a future.
5. Pass your **own executor** for blocking calls, and always **shut down** the pool.

**Here's how it can sound** (about a minute, simple words):

> "Creating a new thread for every task is expensive, so we use an ExecutorService, which is a pool of reusable threads. For example, if I need bills from three billers and each call takes 300 ms, doing them one by one takes 900 ms, but with a pool of three they run in parallel in about 300 ms. When I submit a task I get a Future back, but future.get() blocks, and combining several futures is manual. CompletableFuture solves that: I can say supplyAsync to fetch the bill, thenApply to add the fee, and thenAccept to use the result, without blocking. thenCombine joins two parallel calls, allOf waits for many, exceptionally gives a fallback if a biller is down, and completeOnTimeout handles a slow biller. For blocking I/O I pass my own executor, and I always shut the pool down."

**Tip:** do the "3 calls × 300 ms" math out loud. Interviewers remember a candidate who can reason about timing.

---

## Follow-up questions (simple answers)

**Runnable vs Callable?**
Runnable's `run()` returns nothing and can't throw checked exceptions. Callable's `call()` returns a value and can throw them.

**execute() vs submit()?**
`execute(Runnable)` returns nothing, so you can't get a result back. `submit()` returns a Future, which you can use for the result, for the exception, or to cancel.

**What types of pools does `Executors` give?**
- `newFixedThreadPool(n)`: n threads.
- `newCachedThreadPool()`: grows as needed and reuses idle threads.
- `newSingleThreadExecutor()`: one thread, so tasks run in order.
- `newScheduledThreadPool(n)`: runs tasks after a delay or repeatedly.
- `newVirtualThreadPerTaskExecutor()` (Java 21): one cheap virtual thread per task (J11).

**Why is `newFixedThreadPool` risky in production?**
Its queue has no limit. If tasks arrive faster than they finish, the queue grows until you run out of memory. In production, create a `ThreadPoolExecutor` with a bounded queue and a rejection policy (for example `CallerRunsPolicy`).

**How many threads should a pool have?**
For CPU-heavy work, about the number of CPU cores. For I/O-heavy work like HTTP and DB calls, more, because threads spend most of their time waiting. Measure under load.

**Which pool does CompletableFuture use by default?**
`ForkJoinPool.commonPool()`, which is shared by the whole JVM and sized to the CPU cores. Blocking calls there can starve everyone else, so pass your own executor: `supplyAsync(task, pool)`.

**get() vs join()?**
Both wait. `get()` throws checked exceptions (InterruptedException, ExecutionException). `join()` throws an unchecked CompletionException.

**How does this look in Spring?**
If it's true for you: an `@Async` method returning `CompletableFuture<T>`, running on a `ThreadPoolTaskExecutor` bean that you configure. For many parallel HTTP calls, WebClient is another option.

*Only if they push further:* `CompletableFuture.anyOf(...)` completes as soon as the first call finishes. That's useful when you ask two gateways and take the first answer.

---

## Numbers to remember

| What | Value |
|---|---|
| 3 calls × 300 ms, one after another | 900 ms |
| The same with 2 threads | 600 ms (2 together, then 1) |
| The same with 3 threads, or allOf | 300 ms |
| Electricity + fee with thenCombine | ₹1,210 in about 300 ms |
| All three bills | ₹1,950 |

## Self-check (answer aloud, then click to check)

<details><summary>1. 4 calls of 300 ms each on a pool of 2 threads. About how long do they take?</summary>

600 ms: two together (300), then the other two together (300).

</details>

<details><summary>2. 5 calls of 300 ms each on a pool of 2 threads?</summary>

900 ms: 2, then 2, then 1, which is 3 rounds of 300.

</details>

<details><summary>3. Why not create a new Thread for every request?</summary>

Threads are expensive (memory and startup time), and thousands of them can crash the server. A pool reuses a fixed number.

</details>

<details><summary>4. What's the problem with Future.get()?</summary>

It blocks the calling thread, and combining several futures, or doing "when A is done, then B", is manual.

</details>

<details><summary>5. Bill ₹1,200 and fee ₹10 are fetched at the same time and joined with thenCombine. What's the result, and how long does it take?</summary>

₹1,210, in about 300 ms, because both calls run at the same time.

</details>

<details><summary>6. thenApply or thenCompose for "after fetching the bill, call payAsync(bill)"?</summary>

thenCompose, because payAsync returns a CompletableFuture. thenApply would give a future inside a future.

</details>

<details><summary>7. The GAS biller throws an error. How do you continue with 0?</summary>

Add .exceptionally(error -> 0) to that CompletableFuture.

</details>

If you get stuck on any of them, add it to [STUMBLE-LIST.md](../STUMBLE-LIST.md). When all 7 feel easy, tick J06 in the [README](../README.md) and send `next`.
