# Lesson template (v2)

These are skeletons for the `.md` lesson, the `.java` demo, the QUICK REVISION block in code files, and the chat reply. The filled-in reference is `01-java-core/J01_HashMapInternals.md` plus its `.java`. Match its tone, depth and layout.

## `.md` skeleton

~~~markdown
# <ID> · <Topic in plain words>

> **In one line:** <the whole idea in one sentence, with no jargon that isn't explained>

| ⏱️ Read | 🧪 Run | 🎯 Asked |
|---|---|---|
| <N> min | `java <folder>/<ID>_<Name>.java` | <where it comes up; what product companies push on> |

---

## 🧩 Words you need

| Word | In one line |
|---|---|
| **<term>** | <plain meaning, tied to the example> |

---

## 🖼️ Picture it: <everyday analogy>

<2 to 4 short sentences telling the analogy as a tiny story.>

```mermaid
flowchart LR
    A["<real value from the example>"] -->|"<what happens>"| B["<result>"]
```

👀 **Notice:** <what the reader should look at in the diagram>

| <Analogy> | <Tech> |
|---|---|
| ... | ... |

---

## 🔬 How it works, step by step

### Step 1 · <action, using the running example>

<1 to 3 short sentences. Show the numbers first, then the rule.>

```mermaid
<a small diagram of this step>
```

👀 **Notice:** <the takeaway>

<Repeat for each step. Every rule answers "why?" in one sentence.>

---

## 💻 Code you should be able to write

```java
// the essential code, 12 lines or fewer, with comments that explain intent
```

**What the demo prints** (from a real run):

```text
<copied from the actual output>
```

---

## ⚠️ Traps interviewers love

| Trap | Why it's wrong | Do this instead |
|---|---|---|
| ... | ... | ... |

---

## 🎯 In the interview

**What they're really testing**
- *Service companies:* <the definition-level check>
- *Product companies:* <the deeper check: edge cases, cost, trade-offs, design>

**Say it in this order:**
1. ...
2. ...

**Sample answer** (about a minute, in your own words):

> "..."

**Product-company deep dive:**
- **Q:** ... **A:** ...

---

## ❓ Follow-up questions

**<Question>?**
<a 1 to 3 sentence answer>

---

## 🧪 Test yourself (answer aloud, then click)

<details><summary>1. <question with numbers from the example></summary>

<answer, with the working shown>

</details>

If you get stuck on one, add it to [STUMBLE-LIST.md](../STUMBLE-LIST.md). When they all feel easy, tick <ID> in the [README](../README.md) and send `next`.

---

## ⚡ Quick Revision (2 hours before the interview)

```mermaid
<ONE diagram that holds the whole idea>
```

**🧠 Must remember**
1. <fact with a number>
2. ...

**⚠️ Top traps**
- ...

**🎯 30-second answer:** "<the answer skeleton in 3 or 4 sentences>"

**🔑 Memory hook:** <one vivid line>

**🗣️ Say it aloud (no peeking):**
1. ...
2. ...
3. ...
~~~

## `.java` skeleton

~~~java
import java.util.HashMap;   // import exactly what's used
import java.util.Map;

/*
 * <ID>  <Topic>: runnable demo
 *
 * WHAT YOU WILL SEE (the numbers match <ID>_<Name>.md)
 *   Step 1  <one line: what happens and the key number>
 *   Step 2  ...
 *
 * HOW TO RUN   java <folder>/<ID>_<Name>.java   (or click "Run" above main)
 * READ FIRST   <ID>_<Name>.md
 */
public class <ID>_<Name> {

    public static void main(String[] args) {
        // Step 1: <what happens, in plain words>. Look for <the number that matters>.
        step("Step 1: <action>");
        // ... the running example, with a short comment on each important line
        System.out.println("Notice: <the takeaway, in one line>");
    }

    // Helper types are nested static classes with readable names.
    // A mistake shown on purpose says so in its comment.

    static void step(String text) {
        System.out.println();
        System.out.println("=== " + text + " ===");
    }
}
~~~

## QUICK REVISION block (in `.java` and `.sql` files)

~~~text
/*
 * QUICK REVISION START
 * <topic>
 * Must remember: ...
 * Traps: ...
 * 30-second answer: ...
 * Memory hook: ...
 * QUICK REVISION END
 */
~~~

In `.sql` files, use `--` lines between `-- QUICK REVISION START` and `-- QUICK REVISION END`.

## Chat reply skeleton (new lesson)

~~~markdown
<One line: the topic and how it connects to the last one.>

**The idea:** <one sentence>

<one small table or diagram, if it fits>

**In the interview:** 1. ... 2. ... 3. ...

**Files:** read `<ID>_<Name>.md` (ends with ⚡ Quick Revision), run `java <folder>/<ID>_<Name>.java` (you'll see ...).

**Your turn:** read → run → explain it aloud → `grade: <your answer>` or `next`.
~~~
