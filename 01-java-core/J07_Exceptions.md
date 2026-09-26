# J07 · Checked vs unchecked exceptions, try-with-resources, custom exceptions

**Read this first (10 min). Then run [J07_Exceptions.java](J07_Exceptions.java) to watch each step happen.**

Don't memorize sentences. Understand the 7 steps and the example: **debit ₹1,500 from a balance of ₹1,000**. Once you get those, you can answer exception questions in your own words.

---

## The problem

Things go wrong in a payment flow all the time: a config file is missing, a gateway times out, a balance is too low, a value is null. Java's exceptions let you:

1. stop normal flow with a clear message,
2. handle it in the right place,
3. clean up (close connections) no matter what happened.

Interviewers check whether you know the two kinds of exceptions, how cleanup works and how to design your own.

## Real-life picture: the bank

- **Checked exception:** the loan form has a **mandatory** field, "What if the cheque bounces?" The clerk (the compiler) won't accept the form until you fill it in: you either handle it (`catch`) or pass it on (`throws`).
- **Unchecked exception:** a mistake in your own work, like dividing by zero or paying from an empty wallet (null). There's no form field for it, so fix your code.
- **Error:** the building is on fire (OutOfMemoryError). You don't handle it, you get out (let the app crash and restart).
- **finally:** switching off the office lights when you leave, whether the day went well or not.
- **try-with-resources:** a hotel key card that stops working at checkout by itself. Resources close themselves, and the **last one opened is closed first**, like a stack of plates.

| Bank | Java |
|---|---|
| a mandatory "what if" field on the form | a checked exception (`IOException`, `SQLException`) |
| a mistake in your own work | an unchecked exception (`NullPointerException`, `IllegalArgumentException`) |
| the building on fire | an `Error` (`OutOfMemoryError`, `StackOverflowError`) |
| always switching off the lights | `finally` |
| a key card that deactivates itself | try-with-resources (`AutoCloseable`) |

```text
                 Throwable
               /           \
          Error             Exception
   (OutOfMemoryError,      /            \
    StackOverflowError)   RuntimeException   IOException, SQLException, ...
                          = UNCHECKED        = CHECKED
                          (NullPointer, IllegalArgument,
                           Arithmetic, NumberFormat, ...)
```

---

## Step by step

### Step 1 · The family tree

- Everything that can be thrown is a `Throwable`.
- **Error:** serious JVM problems. Don't catch them.
- **Exception**, which splits in two:
  - **RuntimeException** and its subclasses are **unchecked**. The compiler doesn't force you to handle them.
  - **Every other Exception** is **checked**. The compiler forces you to catch it or declare it.

### Step 2 · Checked: the compiler forces you

```java
static String readGatewayConfig() throws IOException {      // must declare it...
    return Files.readString(Path.of("config/missing-gateway.properties"));
}
```

Without `throws IOException`, javac refuses to compile: *"unreported exception java.io.IOException; must be caught or declared to be thrown"*. The caller must then `catch (IOException e)` or declare it too.

The demo reads a file that doesn't exist and catches: **`NoSuchFileException: config\missing-gateway.properties`**.

Checked exceptions are for problems **outside your control** that a caller could recover from: files, network, database.

### Step 3 · Unchecked: bugs in your own code

| Code | Exception | Message in the demo |
|---|---|---|
| `1000 / 0` | ArithmeticException | `/ by zero` |
| `gatewayName.length()` when it's null | NullPointerException | `Cannot invoke "String.length()" because "J07_Exceptions.gatewayName" is null` |
| `Integer.parseInt("12a")` | NumberFormatException | `For input string: "12a"` |

The compiler doesn't force you to catch these. The right fix is **better code**: validate inputs and check for null. Since Java 14, NullPointerException messages say exactly **which** variable was null, as you can see above.

### Step 4 · try, catch, finally

```text
try     : debit Rs 1500 from a balance of Rs 1000
catch   : Balance 1000 is less than 1500
finally : always runs (close the payment log here)
```

- The line after `debit(...)` inside `try` **never runs**, because the exception jumps straight to `catch`.
- `finally` runs **every time**: after success, after a catch, and even after a `return` inside try. The only exceptions are `System.exit()` or the JVM crashing.

**The trap:** if both try and finally return a value, finally wins:

```java
try { return 1; } finally { return 2; }     // the method returns 2
```

A return in finally can even hide an exception, so never return from finally.

### Step 5 · try-with-resources

```java
try (DbConnection db = new DbConnection();     // opened 1st
     AuditLog log = new AuditLog()) {           // opened 2nd
    db.save("TXN1001");
    log.write("debit TXN1001");
}                                               // closed automatically: log 1st, then db
```

Any class that implements `AutoCloseable` can go in the brackets. It closes automatically **in reverse order**, whether the body succeeded or threw.

**If both the body and close() throw**, you don't lose either. The body's exception is thrown, and the close() error is attached to it as **suppressed**:

```text
caught    : debit failed
suppressed: audit log close failed
```

Before Java 7 this took a finally block with its own try/catch inside. try-with-resources is shorter and never forgets.

### Step 6 · A custom exception

```java
class InsufficientBalanceException extends RuntimeException {
    final long balance, amount;
    InsufficientBalanceException(long balance, long amount) {
        super("Balance " + balance + " is less than " + amount);   // a clear message
        this.balance = balance;
        this.amount = amount;
    }
}
```

`debit(1000, 1500)` throws it, and the demo prints "Balance 1000 is less than 1500" and "short by Rs 500".

**Checked or unchecked?** In Spring apps, business errors are usually **unchecked** (extend RuntimeException), for two reasons:
- The code stays clean: you don't need `throws` on every method up the chain.
- `@Transactional` rolls back automatically **only for unchecked exceptions** (topic B05), which is exactly what you want when a debit fails.

A global `@RestControllerAdvice` then turns it into a clean HTTP error (topic B07).

### Step 7 · Wrap it, but keep the cause

```java
catch (IOException e) {
    throw new PaymentFailedException("Could not load PayU gateway config", e);   // e = the cause
}
```

The caller gets **your** clear message, and the original error is still inside, so logs show a "Caused by: NoSuchFileException" line. If you don't pass `e`, the real reason is lost forever.

### Good habits in one list

- Catch **specific** exceptions, not `Exception`.
- Never leave a catch block empty.
- Keep the **cause** when you wrap.
- **Throw early** (validate at the start) and **catch late** (at the boundary, like a controller advice).
- Log an exception **once**, not at every level.
- Use try-with-resources for anything that needs closing.

---

## How to explain it in the interview

Use your own words. Cover these points in this order, using the ₹1,000 balance and the ₹1,500 debit:

1. All exceptions come from **Throwable**, which has two branches. **Error** is for JVM problems you shouldn't catch. **Exception** is for problems your code can handle.
2. **Checked** exceptions (IOException, SQLException) must be caught or declared, because the compiler forces it. **Unchecked** exceptions (RuntimeException and its subclasses: NullPointerException, IllegalArgumentException) usually mean a bug and aren't forced.
3. **finally** always runs, which makes it the place for cleanup. Never return from it.
4. **try-with-resources** closes AutoCloseable resources automatically, in reverse order, and keeps close() errors as suppressed exceptions.
5. **Custom exceptions:** in Spring, usually extend RuntimeException with a clear message, like InsufficientBalanceException. @Transactional rolls back on them by default. When wrapping, keep the original as the **cause**.

**Here's how it can sound** (about a minute, simple words):

> "In Java all exceptions come from Throwable. Errors like OutOfMemoryError are JVM problems we don't catch. Exceptions split into checked and unchecked. Checked ones, like IOException, must be caught or declared, because they're about things outside our control, like a missing file. Unchecked ones extend RuntimeException, like NullPointerException, and usually mean a bug, so the compiler doesn't force us. finally always runs, so it's for cleanup, and I avoid returning from it because it overrides the try's return. For resources like connections I use try-with-resources, which closes them automatically in reverse order. For business rules I create custom unchecked exceptions, for example InsufficientBalanceException when the balance is 1,000 and the debit is 1,500. In Spring that also means @Transactional rolls back by default, and a @RestControllerAdvice turns it into a proper HTTP response."

**Tip:** if asked "checked or unchecked for your custom exception?", answer with the reason: unchecked, because of clean code and @Transactional rollback.

---

## Follow-up questions (simple answers)

**throw vs throws?**
`throw` actually throws one exception object: `throw new X(...)`. `throws` in a method signature declares which checked exceptions the method may throw.

**final vs finally vs finalize?**
`final` is a keyword: a variable that can't be reassigned, a method that can't be overridden, a class that can't be extended. `finally` is the block that always runs. `finalize()` was an old cleanup method the garbage collector called. It's deprecated, so don't use it.

**Can you have try without catch?**
Yes: `try { } finally { }`, or try-with-resources on its own.

**What's multi-catch?**
`catch (IOException | SQLException e)` handles several exception types with one block.

**Does the order of catch blocks matter?**
Yes, specific before general. If `catch (Exception e)` comes first, a later `catch (IOException e)` could never run, so javac rejects it.

**Can an overriding method throw more exceptions?**
Not broader **checked** exceptions than the parent method declares. It can throw fewer, narrower or unchecked ones.

**Is catching Exception or Throwable ever OK?**
Only at the outermost boundary, like a global handler that logs the error and returns a clean response. Never catch `Error` to keep running.

**What happens to an exception thrown inside a thread pool task?**
It's stored in the Future, and `future.get()` throws an `ExecutionException` with your exception as its cause (J06).

*Only if they push further:* `@Transactional` does **not** roll back for checked exceptions by default; the transaction commits. If your service throws a checked exception on failure, add `@Transactional(rollbackFor = Exception.class)`. This matters a lot in payment code.

---

## Rules to remember

| Rule | Why |
|---|---|
| checked = catch or declare | the compiler forces it (files, network, DB) |
| unchecked = fix the code | usually a bug: null, bad input, a broken business rule |
| finally always runs | cleanup; never `return` from it |
| try-with-resources closes in reverse order | the last one opened is closed first; close errors become suppressed |
| custom business exceptions extend RuntimeException | clean code, and @Transactional rolls back |
| wrap with the cause | `new MyException("clear message", e)` |

## Self-check (answer aloud, then click to check)

<details><summary>1. Is IOException checked or unchecked? And NullPointerException?</summary>

IOException is checked. NullPointerException is unchecked, because it extends RuntimeException.

</details>

<details><summary>2. A method calls Files.readString(...). What must it do?</summary>

It must catch IOException, or declare throws IOException. Otherwise it won't compile.

</details>

<details><summary>3. try returns 1 and finally returns 2. What does the method return?</summary>

2. The finally's return replaces the try's, which is why you should never return from finally.

</details>

<details><summary>4. try-with-resources opens a DB connection first, then a LOG. In what order do they close?</summary>

LOG first, then the DB connection, in reverse order.

</details>

<details><summary>5. debit(balance 500, amount 800): which exception, and what message?</summary>

InsufficientBalanceException: "Balance 500 is less than 800", and the account is short by ₹300.

</details>

<details><summary>6. You catch an IOException and throw your own exception. How do you keep the real reason?</summary>

Pass it as the cause: new PaymentFailedException("clear message", e).

</details>

<details><summary>7. Why are custom business exceptions usually unchecked in Spring apps?</summary>

You don't need throws everywhere, and @Transactional rolls back automatically only for unchecked exceptions.

</details>

If you get stuck on any of them, add it to [STUMBLE-LIST.md](../STUMBLE-LIST.md). When all 7 feel easy, tick J07 in the [README](../README.md) and send `next`.
