# Lesson template

These are skeletons for a Java concept lesson (a `.md` + `.java` pair) and for the chat reply. Other topic types (SQL, Spring Boot, Angular) reuse the sections where they fit: the problem, the picture, steps with one running example, how to explain it, follow-ups, and the self-check.

The filled-in example is `01-java-core/J01_HashMapInternals.md` and `.java`.

## .md skeleton

~~~markdown
# <ID> · <Topic in plain words>

**Read this first (<N> min). Then run [<ID>_<Name>.java](<ID>_<Name>.java) to watch each step happen.**

Don't memorize sentences. Understand the <K> steps and the example with <running example>. Once you get those, you can explain <topic> in your own words.

<If it builds on an earlier topic, one line linking it, e.g. J01's "hashCode picks the bucket, equals picks the entry".>

---

## The problem

<Before: what goes wrong or is slow without it, with a number.> <After: what it does instead.>

## Real-life picture: <analogy>

<3–5 bullets telling the analogy as a small story.>

| <Analogy> | Java |
|---|---|
| ... | ... |

```text
<small ASCII picture of the running example>
```

---

## Step by step

<One sentence that sets up the running example.>

### Step 1 · <action>

<What happens, with the numbers worked out.>

```text
<state after this step>
```

> **<one-line takeaway worth remembering>**

### Step N · <a rule>

<Numbers first, then the analogy, then why. Never state a bare rule.>

<If there are several cases, finish with one summary table of all of them.>

---

## How to explain it in the interview

Use your own words. Cover these points in this order, using <the running example>:

1. ...
2. ...

**Here's how it can sound** (about a minute, simple words):

> "..."

**Tip:** <one practical move, e.g. "ask if you can explain with a small example and draw it">

---

## Follow-up questions (simple answers)

**<Question>?**
<A 1–3 sentence answer, with a tiny example where possible.>

*Only if they push further:* <a deeper detail, e.g. JDK internals or a JPA/Spring angle>

---

## Numbers to remember   (or "Rules to remember")

| What | Value |
|---|---|
| ... | ... |

## Self-check (answer aloud, then click to check)

<details><summary>1. <a question with numbers from the running example></summary>

<the answer, with the working shown>

</details>

If you get stuck on any of them, add it to [STUMBLE-LIST.md](../STUMBLE-LIST.md). When all <M> feel easy, tick <ID> in the [README](../README.md) and send `next`.
~~~

## .java skeleton

~~~java
import java.util.HashMap;   // import exactly what's used
import java.util.Map;

/*
 * <ID>  <Topic>: runnable demo
 *
 * Read <ID>_<Name>.md first. This file runs the same steps so you can see
 * them happen. The step numbers match the .md file.
 *
 * Run it:  java <folder>/<ID>_<Name>.java
 *          (or click "Run" above main() in VS Code)
 */
public class <ID>_<Name> {

    public static void main(String[] args) {
        step("Step 1: <action>");
        // the running example, with a short comment on every line that matters
        // and the expected value where it helps, e.g.  // 101 % 16 = 5

        step("Step 2: <action>");
        // ...
    }

    // Helper types are nested static classes with readable names.
    // A mistake shown on purpose says so in its comment.

    static void step(String text) {
        System.out.println();
        System.out.println("=== " + text + " ===");
    }
}
~~~

## Chat reply skeleton (new lesson)

~~~markdown
<One line: which topic this is and how it connects to the last one.>

## <Topic> in simple words

**Why it exists:** ...
**The picture:** <the analogy in one or two lines>

Now follow <running example>:
1. ...
2. ...

**In the interview:** <the ordered points or the one-minute sample>

## Files
- **Read first:** `<folder>/<ID>_<Name>.md`: <what's inside>
- **Then run:** `java <folder>/<ID>_<Name>.java`: <what they'll see>

**Your turn:** read → run → explain it aloud using <example> → `grade: <your answer>` or `next`.
~~~
