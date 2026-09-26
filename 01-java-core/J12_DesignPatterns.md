# J12 ⭐ · Design patterns: Singleton, Builder, Factory, Strategy (and Proxy)

**Read this first (12 min). Then run [J12_DesignPatterns.java](J12_DesignPatterns.java) to watch each step happen.**

Don't memorize definitions. Understand the 5 steps, each shown on your payment world:
- a single config object (Singleton),
- building a payment request (Builder),
- picking a gateway (Factory),
- fee rules for **₹1,000** (Strategy),
- how Spring wraps your methods (Proxy).

Once you get those, you can explain each pattern with an example from your own work.

---

## The problem

"Which design patterns have you used?" is a standard question for 3-year developers. Reciting the names of all 23 patterns doesn't impress anyone. Showing 4 or 5 you actually use, with **the problem each one solves**, does. And Spring itself is built on patterns, so you use more of them than you think.

## Real-life pictures

| Pattern | Everyday picture | Problem it solves |
|---|---|---|
| Singleton | the RBI governor: there's only one, and everyone refers to the same person | exactly one shared instance |
| Builder | ordering a custom Subway sandwich: bread first, then optional extras, then "make it" | objects with many optional fields |
| Factory | a car rental counter: you say "SUV" and get the right car; you don't build it | hide which class gets created |
| Strategy | Google Maps: car, bike or walk, the same trip calculated differently | swap an algorithm at runtime |
| Proxy | a personal assistant who handles things before and after your meeting | add work around a call without touching it |

---

## Step by step

### Step 1 · Singleton: exactly one instance

The basic idea: a `private` constructor (so nobody else can call `new`) plus one static way to get the instance.

**The trap is the lazy version without locking.** Two threads call `getInstance()` at the same moment. Both see `instance == null`, and both create one:

| Version | Instances created (demo, three runs) |
|---|---|
| lazy, no locking | **2** ❌, every run |
| double-checked locking | 1 ✅ |
| holder class | 1 ✅ |
| enum | 1 ✅ |

**Double-checked locking:**

```java
private static volatile Config instance;          // volatile: nobody sees a half-built object (J05)

static Config getInstance() {
    if (instance == null) {                        // 1st check: skip the lock once it exists
        synchronized (Config.class) {
            if (instance == null) {                // 2nd check: the other thread may have created it
                instance = new Config();
            }
        }
    }
    return instance;
}
```

**Simpler and safe:**
- The **holder class**: the JVM creates it the first time it's used, once.
- An **enum**: `enum Config { INSTANCE; }`. This is the simplest option, and even reflection and serialization can't make a second copy.

**In Spring you rarely write this yourself.** Every bean is a singleton **by default**, meaning one per Spring container (B03).

### Step 2 · Builder: many optional fields, readable code

Without a builder:

```java
new PaymentRequest("TXN1001", 1500, "INR", "SETU", null, "electricity bill")   // which null is which?
```

With a builder:

```java
PaymentRequest.builder("TXN1001", 1500)     // required fields first
        .gateway("SETU")                    // optional ones by name, in any order
        .remarks("electricity bill")
        .build();                           // currency not set, so the default INR is used
```

This gives `PaymentRequest[txnId=TXN1001, amount=1500, currency=INR, gateway=SETU, remarks=electricity bill]`.

`build()` is also the place to **check the rules**: `builder("TXN1002", 0).build()` is refused with "amount must be positive, got 0". The result can be immutable (J10). In real projects, Lombok's `@Builder` writes all of this for you.

### Step 3 · Factory: one place decides which class to create

```java
PaymentGateway gateway = PaymentGatewayFactory.get("SETU");   // the caller never writes "new SetuGateway()"
```

The demo prints "factory gave SetuGateway -> SETU-OK-1500". An unknown name, `get("STRIPE")`, is refused with "Unknown gateway: STRIPE".

Callers only know the **interface**. Adding a gateway changes one place, the factory. **In Spring, the container is the factory**: inject `Map<String, PaymentGateway>` and Spring fills it with every gateway bean, keyed by bean name.

### Step 4 · Strategy: the same job, a different rule, chosen at runtime

```java
interface FeeStrategy { int fee(int amount); }
```

| Mode | Fee rule | Fee on ₹1,000 | Total |
|---|---|---|---|
| UPI | free | ₹0 | **₹1,000** |
| CARD | 2% | ₹20 | **₹1,020** |
| NETBANKING | flat ₹10 | ₹10 | **₹1,010** |

The payment code just calls `strategy.fee(amount)`. It doesn't know or care which rule it has. A new mode is a new strategy, with no if-else to edit, which is the open/closed principle from J10.

**Factory vs Strategy:** Factory answers "**which object** should I create?" Strategy answers "**which behaviour** should I use?" They're often used together: a factory (or a map) hands you the right strategy, as `feeStrategyFor(mode)` does in the demo.

### Step 5 · Proxy: how Spring's @Transactional really works

The demo wraps a real PayU gateway in a proxy, an object that stands in front of the real one:

```text
  [proxy] begin transaction before pay()
  [proxy] commit transaction after pay()
  result: PAYU-OK-1500
```

When you put `@Transactional` on a method, Spring does exactly this. Other beans receive a **proxy** of your class. It begins the transaction, calls your real method, then commits (or rolls back on an exception). `@Async` and `@Cacheable` work the same way.

That explains a classic bug: **calling a `@Transactional` method from another method in the same class** skips the transaction. `this.method()` goes straight to the real object and never passes through the proxy (topic B05).

### Patterns you already use through Spring

| Pattern | Where in Spring |
|---|---|
| Singleton | beans are singleton-scoped by default |
| Factory | `BeanFactory` / `ApplicationContext` creates your beans |
| Proxy | `@Transactional`, `@Async`, `@Cacheable`, Spring AOP |
| Template Method | `JdbcTemplate`, `RestTemplate`, and J10's `BaseGateway.pay()` |
| Observer | `ApplicationEvent` and `@EventListener`; RabbitMQ publish/subscribe |
| Builder | `ResponseEntity.ok().header(...).body(...)`, `WebClient.builder()` |

---

## How to explain it in the interview

Use your own words. Pick 3 or 4 patterns, and for each give **the problem, then the example**:

1. **Singleton:** one shared instance. Lazy creation needs to be thread-safe: double-checked locking with volatile, a holder class, or an enum (the best). In Spring, beans are singletons by default.
2. **Builder:** for objects with many optional fields. The code is readable, `build()` validates, and it can produce immutable objects. Use Lombok `@Builder`.
3. **Factory:** hides which class gets created. Callers ask by name or type and get an interface. In Spring, inject `Map<String, PaymentGateway>`.
4. **Strategy:** interchangeable rules behind one interface, picked at runtime, like fee calculation per payment mode. It replaces if-else chains.
5. **Proxy:** Spring wraps beans in proxies for `@Transactional` and `@Async`, which is why self-invocation skips them.

**Here's how it can sound** (about a minute, simple words):

> "In my payment work, if it's true for you, the most useful patterns were strategy and factory. Each payment mode had its own fee rule behind one FeeStrategy interface, so adding a mode meant adding a class instead of editing an if-else chain, and a factory, or really a Spring-injected map, gave us the right gateway implementation by name. For request objects with many optional fields we used the builder pattern, mostly through Lombok's @Builder, and build() validated the amount. Singleton I know well, including why a lazy singleton needs double-checked locking with volatile, but in Spring I rarely write one because beans are singletons by default. And Spring's @Transactional is a proxy around the bean, which is why calling a transactional method from inside the same class doesn't start a transaction."

**Tip:** don't list 10 patterns. Three with real examples beat ten names.

---

## Follow-up questions (simple answers)

**Why is the enum singleton considered the best?**
The JVM guarantees exactly one instance, it's thread-safe with no extra code, and neither reflection nor serialization can create a second copy.

**How can a normal singleton be broken?**
By reflection (calling the private constructor), by serialization (reading it back creates a copy unless you add `readResolve`), by cloning, or by different class loaders.

**Why is `volatile` needed in double-checked locking?**
Creating an object is several steps, and without volatile they can be reordered. Another thread could then see a non-null reference to an object that isn't fully built yet (J05).

**Singleton vs a class with only static methods?**
A singleton is an object. It can implement an interface, be injected, be created lazily and be replaced with a mock in tests. A static utility class can't do any of that.

**Factory Method vs Abstract Factory?**
A factory method is one method that decides which class to create. An abstract factory creates a whole **family** of related objects, for example all the UI parts for Windows or all of them for Mac.

**Builder or constructor?**
Use a constructor for 2 or 3 required fields. Use a builder when there are many parameters, especially optional ones, or several of the same type that are easy to mix up.

**Which patterns does the JDK use?**
- Builder: `StringBuilder`, `HttpRequest.newBuilder()`.
- Factory: `List.of()`, `Executors.newFixedThreadPool()`.
- Strategy: `Comparator` (J08).
- Decorator: `BufferedReader` wrapping a `FileReader`.
- Proxy: `java.lang.reflect.Proxy`, used in this demo.

*Only if they push further:* Spring uses JDK dynamic proxies (like the demo) when your bean implements an interface. Otherwise, and by default in Spring Boot, it uses CGLIB, which creates a subclass of your class at runtime. That's why `final` methods can't be proxied.

---

## Numbers to remember

| What | Value |
|---|---|
| Unsafe lazy singleton, 2 threads | 2 instances (wrong) |
| Double-checked, holder, enum | 1 instance |
| Fee on ₹1,000: UPI / CARD / NETBANKING | ₹0 / ₹20 / ₹10 |
| Totals | ₹1,000 / ₹1,020 / ₹1,010 |

## Self-check (answer aloud, then click to check)

<details><summary>1. Two threads call a lazy getInstance() that has no locking, at the same time. How many instances can be created?</summary>

Two. Both see null before either one assigns it, as the demo showed every run.

</details>

<details><summary>2. Why must the instance field be volatile in double-checked locking?</summary>

So no thread ever sees a reference to a half-constructed object, because volatile stops the steps from being reordered.

</details>

<details><summary>3. PaymentRequest has 2 required fields and 5 optional ones. Which pattern do you use?</summary>

Builder: required fields in builder(...), optional ones by name, and build() validates.

</details>

<details><summary>4. What are the fees and totals on ₹1,000 for UPI (free), CARD (2%) and NETBANKING (flat ₹10)?</summary>

Fees ₹0, ₹20 and ₹10, so the totals are ₹1,000, ₹1,020 and ₹1,010.

</details>

<details><summary>5. Factory vs Strategy in one line each?</summary>

A factory decides which object to create. A strategy is interchangeable behaviour you plug in and use. They're often used together.

</details>

<details><summary>6. Which pattern makes @Transactional work, and what's the classic bug?</summary>

Proxy. Calling a @Transactional method from inside the same class skips the proxy, so no transaction starts.

</details>

<details><summary>7. In a Spring app, do you write getInstance() singletons?</summary>

Rarely. Beans are singleton-scoped by default, so you inject them instead.

</details>

If you get stuck on any of them, add it to [STUMBLE-LIST.md](../STUMBLE-LIST.md). When all 7 feel easy, tick J12 in the [README](../README.md) and send `next`.
