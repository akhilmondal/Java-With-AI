# J10 · SOLID, interface vs abstract class, immutable class

**Read this first (15 min). Then run [J10_SolidAndImmutability.java](J10_SolidAndImmutability.java) to watch each step happen.**

Don't memorize sentences. Understand the 7 steps and one example: **paying ₹1,000 through payment gateways**. The fees are made up for the example: PayU charges **2%**, Setu a **flat ₹5**, and Razorpay (added later) **1.5%**. Once you get those, you can explain SOLID with a real example instead of textbook lines.

If it's true for you, say in the interview that your own gateway code (PayU, Setu) follows the same shape. Interviewers love "SOLID with an example from my own code".

---

## The problem

Payment code keeps changing: new gateways, new rules, new notifications. Badly structured code means every change touches many places and breaks something. **SOLID** is five rules for structuring classes so that changes stay small and safe. Interfaces, abstract classes and immutable classes are the tools you use to follow those rules.

## Real-life pictures

| Real life | Principle |
|---|---|
| a chef who also takes orders, cleans tables and does the billing | breaks **S**: too many jobs |
| a phone charging port: you plug in a new device without rewiring the socket | **O**: extend without modifying |
| a duplicate house key that looks right but doesn't open the door | breaks **L**: not a real substitute |
| a thali that forces every guest to take every dish | breaks **I**: an interface that's too big |
| a wall socket: any appliance with the standard plug works | **D**: depend on the standard (the interface), not the brand |

---

## Step by step: SOLID

### Step 1 · S: Single Responsibility (one class, one job)

Bad: one `PaymentService` that validates, calls PayU, saves to the DB and sends an SMS. A change to the SMS provider means editing the same class that moves money. That's four reasons to change one class.

Good: each job in its own class, and the service only **coordinates**:

```text
PaymentValidator   : amount 1000 is valid
PaymentGateway     : PAYU-OK-1020
PaymentRepository  : saved TXN1001 -> PAYU-OK-1020
NotificationService: SMS sent for TXN1001
```

> **A class should have one reason to change.**

### Step 2 · O: Open for extension, closed for modification

Bad:

```java
if (gateway.equals("PAYU")) { ... } else if (gateway.equals("SETU")) { ... }   // add Razorpay = edit and retest this
```

Good: one interface, and one class per gateway:

```java
interface PaymentGateway { String name(); int fee(int amount); String pay(int amount); }
```

| Gateway | Fee on ₹1,000 | pay(1000) returns |
|---|---|---|
| PayU (2%) | 20 | PAYU-OK-**1020** |
| Setu (flat ₹5) | 5 | SETU-OK-**1005** |
| Razorpay (1.5%), added later | 15 | RAZORPAY-OK-**1015** |

Adding Razorpay was **a new class only**. `PaymentService` wasn't touched.

> **New behaviour = new code, not edits to working code.**

### Step 3 · L: Liskov Substitution (a child must work wherever the parent works)

```java
static void payBill(Account from, int amount) { from.withdraw(amount); }   // written for ANY Account
```

- `payBill(new Account(5000), 1200)` leaves a balance of 5,000 - 1,200 = **3,800**.
- `payBill(new FixedDepositAccount(5000), 1200)` throws **"FD is locked until maturity"**.

`FixedDepositAccount extends Account` but can't do what Account promises (withdraw). So code written for Account breaks when it gets an FD. It's like the duplicate key.

The fix: an FD shouldn't **be** a withdrawable account. Split the types, for example `Account` for the balance and a `Withdrawable` interface that only savings accounts implement.

> **If a subclass throws "not supported" for a parent method, the inheritance is wrong.**

### Step 4 · I: Interface Segregation (small interfaces)

If `PaymentGateway` also had `refund()` and `emi()`, Setu (no refunds) would be forced to write a method that just throws. Instead, refund gets its own small interface:

```java
interface Refundable { String refund(String paymentId); }
```

```text
PAYU: refunded P1
SETU: no refunds (doesn't implement Refundable)
RAZORPAY: refunded P1
```

> **Don't force a class to implement methods it can't do.**

### Step 5 · D: Dependency Inversion (depend on the interface)

Bad: `private PayUGateway gateway = new PayUGateway();` inside the service. The service is welded to PayU and can't be tested without calling PayU.

Good: the service asks for **a `PaymentGateway`** in its constructor, and someone else (Spring) passes one in:

```java
PaymentService(List<PaymentGateway> gateways) { ... }   // Spring injects every gateway bean
```

In a unit test, you pass a `FakeGateway`: `FAKE-OK-1000`, with no real PayU call. It's like the wall socket: the wiring doesn't care which brand of appliance you plug in.

> **High-level code depends on an interface. Spring's dependency injection (B02) does the plugging.**

---

## Step 6 · Interface vs abstract class

The demo uses both:
- `PaymentGateway` is an **interface**: the contract, plus a `default` method `describe()` that prints "PAYU charges Rs 20 on Rs 1000".
- `BaseGateway` is an **abstract class**: shared code (the fixed `pay()` recipe) **and state** (a `calls` counter: after two payments, calls = **2**). Each gateway only fills in `callApi()`.

| | Interface | Abstract class |
|---|---|---|
| How many can a class use? | **many** (`implements A, B`) | **one** (`extends X`) |
| State (instance fields) | no, only constants | **yes** |
| Constructors | no | yes (called with `super()`) |
| Methods | abstract by default; `default`, `static`, `private` since Java 8/9 | abstract and normal, any access level |
| Meaning | "**can do**": a capability (Refundable, Comparable) | "**is a**": a base type with shared code (BaseGateway) |

**When to use which:**
- Use an **interface** for a capability that unrelated classes can share.
- Use an **abstract class** when closely related classes share code **and** state.
- They combine well: an interface for the contract, plus an abstract base class for the shared code, just like the demo.

---

## Step 7 · An immutable class

**The six rules:**
1. Make the class `final`, so nobody can subclass it and add changeable behaviour.
2. Make every field `private final`.
3. Add no setters.
4. Set everything in the constructor.
5. Make **defensive copies** of anything mutable (like a List), both coming in and going out.
6. For a "change", return a **new** object: `withAmount(1500)`.

**Why rule 5 matters.** The demo creates both classes with the list [UPI, BBPS], then the caller adds "REFUND" to **its own** list:

| Class | tags afterwards |
|---|---|
| `LeakyPaymentRequest` (stores the caller's list) | [UPI, BBPS, **REFUND**], changed from outside! |
| `PaymentRequest` (stores `List.copyOf(tags)`) | [UPI, BBPS], unchanged |

`safe.tags().add("HACK")` throws UnsupportedOperationException, because the copy is unmodifiable. `safe.withAmount(1500)` gives a new object with 1500, and `safe` still has **1000**.

Note that `final List<String> tags` alone is **not** enough. `final` stops you pointing the field at a different list, but the list itself can still change.

**Why bother?** Immutable objects are thread-safe with no locks (J05), safe as HashMap keys (J01) and safe to cache and share. That's exactly why String is immutable (J03).

---

## How to explain it in the interview

Use your own words. Cover these points in this order, using the gateways:

1. **S:** one job per class. Validation, gateway call, saving and SMS are separate, and the service coordinates.
2. **O:** a PaymentGateway interface with one class per gateway, so adding Razorpay means a new class with no edits.
3. **L:** a subclass must work wherever the parent works. A FixedDeposit that throws on withdraw breaks it.
4. **I:** small interfaces, like a separate Refundable, so Setu isn't forced to fake refunds.
5. **D:** the service depends on the interface, and Spring injects the implementation, which also makes testing easy.
6. **Interface vs abstract class:** an interface is a contract ("can do", many allowed, no state). An abstract class is a base with shared code and state ("is a", only one).
7. **Immutable class:** final class, private final fields, no setters, defensive copies, and "changes" return new objects. It's thread-safe and a safe key.

**Here's how it can sound** (about a minute, simple words):

> "I can explain SOLID with payment gateways. Single responsibility: validation, the gateway call, saving and notifications are separate classes, and the service only coordinates. Open/closed: I have a PaymentGateway interface and one class per gateway, so adding a new gateway like Razorpay is a new class, with no changes to the existing code. Liskov: a subclass must work wherever the parent is used; for example, a fixed deposit account that throws on withdraw shouldn't extend a normal account. Interface segregation: refunds are a separate Refundable interface, because not every gateway supports refunds. Dependency inversion: my service depends on the PaymentGateway interface and Spring injects the implementations, which also makes it easy to test with a fake. For shared code I use an abstract base class, and for things like request objects I make them immutable: final fields, no setters, and defensive copies of lists."

**Tip:** if they say "SOLID with an example from your code", start with O and D. The gateway interface story covers both in 20 seconds.

---

## Follow-up questions (simple answers)

**Can an interface have fields or a constructor?**
It can't have a constructor. Its only fields are constants (`public static final`).

**Why did Java 8 add default methods?**
To add new methods to existing interfaces without breaking every class that implements them. That's how `List.sort()` and `Collection.stream()` were added. If two interfaces give the same default method, your class must override it and choose, for example with `Refundable.super.refund(...)`.

**Can an abstract class have a constructor? Can it have no abstract methods?**
Yes to both. Subclasses call its constructor with `super(...)`. A class with no abstract methods can still be marked abstract, just so nobody can create it directly.

**Are records immutable?**
Only shallowly. Their fields are final, but a List inside can still change. Copy it in the record's compact constructor:

```java
record PaymentRequest(String txnId, int amount, List<String> tags) {
    PaymentRequest { tags = List.copyOf(tags); }
}
```

**What immutable classes does the JDK have?**
String, Integer and the other wrappers, BigDecimal, LocalDate/LocalDateTime, and `List.of()` / `Map.of()`.

**final vs immutable?**
`final` on a variable only stops reassigning it. Immutable means the object's contents can never change. A final List can still be modified (the leaky demo).

**Which SOLID principles does Spring help with?**
D, through dependency injection. And O: inject `List<PaymentGateway>` or `Map<String, PaymentGateway>`, and a new gateway bean extends the behaviour without editing the service.

**Composition over inheritance?**
Prefer "has a" (a field of another type) over "is a" (extends). Deep inheritance trees break easily, as the FixedDeposit example shows. That's also how Strategy works (J12).

---

## Rules to remember

| Letter | One line | Payment example |
|---|---|---|
| S | one class, one reason to change | validator, gateway, repository, notifier |
| O | add code, don't edit working code | Razorpay = a new class |
| L | a child works wherever the parent works | FD account must not extend a withdrawable account |
| I | small interfaces | a separate `Refundable` |
| D | depend on interfaces; they get injected | `PaymentService(List<PaymentGateway>)` |

## Self-check (answer aloud, then click to check)

<details><summary>1. One PaymentService validates, calls PayU, saves to the DB and sends an SMS. Which principle does it break, and how do you fix it?</summary>

S (single responsibility). Split it into a validator, a gateway, a repository and a notifier, and let the service only coordinate.

</details>

<details><summary>2. Adding Razorpay means editing an if-else chain in PaymentService. Which principle is broken?</summary>

O (open/closed). Use a PaymentGateway interface and one class per gateway, so a new gateway is a new class.

</details>

<details><summary>3. payBill(new FixedDepositAccount(5000), 1200) throws. Which principle is broken?</summary>

L (Liskov). A subclass can't do what the parent promises, so code written for the parent breaks.

</details>

<details><summary>4. Setu can't refund, but the interface forces it to have refund(). Which principle is broken, and what's the fix?</summary>

I (interface segregation). Move refund into a small Refundable interface that only refund-capable gateways implement.

</details>

<details><summary>5. What's wrong with PaymentService doing new PayUGateway() inside itself?</summary>

It breaks D. The service is tied to PayU, and you can't swap it or test with a fake. Depend on the interface and inject it.

</details>

<details><summary>6. You pay ₹1,000 through PayU (2%), Setu (₹5) and Razorpay (1.5%). What are the totals?</summary>

₹1,020, ₹1,005 and ₹1,015.

</details>

<details><summary>7. A class stores the constructor's List directly in a private final field. Is it immutable?</summary>

No. The caller can still change that list from outside. Store List.copyOf(list) instead.

</details>

If you get stuck on any of them, add it to [STUMBLE-LIST.md](../STUMBLE-LIST.md). When all 7 feel easy, tick J10 in the [README](../README.md) and send `next`.
