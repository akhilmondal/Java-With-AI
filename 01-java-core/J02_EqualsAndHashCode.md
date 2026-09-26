# J02 · equals() and hashCode()

**Read this first (10 min). Then run [J02_EqualsAndHashCode.java](J02_EqualsAndHashCode.java) to watch each step happen.**

Don't memorize sentences. Understand the 4 cases and the example with two copies of employee **101**. Once you get those, you can answer any equals/hashCode question in your own words.

This builds on J01's line: **hashCode() picks the bucket, equals() picks the entry inside the bucket.**

---

## The problem

You create the same employee twice:

```java
Employee e1 = new Employee(101, "Rahul");
Employee e2 = new Employee(101, "Rahul");
```

For you, e1 and e2 are the same employee. For Java, by default, they are two different objects, because Java doesn't know that "same ID means same employee". You tell it by overriding `equals()` and `hashCode()`.

If you override only one of the two, HashMap and HashSet break quietly. There's no error, just wrong results.

## Real-life picture: photocopies of an ID card

Rahul's ID card is photocopied twice, and HR files every sheet in one of 16 drawers.

- **`==`** asks: is this the **same sheet of paper**? For two photocopies, no.
- **`equals()`** asks: is this the **same employee**? You decide what that means, for example the same employee ID.
- **`hashCode()`** decides **which drawer** a sheet goes in. By default, Java stamps a random-looking token number on every sheet, so the two copies end up in random drawers. If you make hashCode use the employee ID, both copies go to the same drawer.

| HR office | Java |
|---|---|
| two photocopies of Rahul's card | two objects with the same data |
| "is it the same sheet of paper?" | `==` |
| "is it the same employee?" | `equals()` |
| which drawer to use | `hashCode()` |
| a random token number on each sheet | the default `hashCode()` |

---

## Step by step: the 4 cases

In every case we do the same test. Both copies go into a `HashSet`, e1 goes into a `HashMap`, and then we search the map with e2.
The right result: the set holds **1** employee, and `map.get(e2)` finds e1's value.

### Step 1 · Override nothing

- `e1 == e2` gives **false**, because they are two different objects in memory.
- `e1.equals(e2)` gives **false**, because the default `equals()` from `Object` does exactly what `==` does.
- `e1.hashCode()` and `e2.hashCode()` give two random-looking numbers. On this laptop they were `686989583` and `259219561`, but yours may differ. The JVM makes them up for each object, and they have nothing to do with the id 101.

Result: set size **2**, and `map.get(e2)` returns **null**. Java thinks these are two employees.

### Step 2 · Override only equals()

Now `e1.equals(e2)` gives **true**. But the hashCodes are still the random ones, so they differ.

When the HashSet adds e2, it first compares hash values (J01). The hashes are different, so it decides "different key" and **never calls equals()**.

Result: set size **2**, and `map.get(e2)` returns **null**.

In the office, the clerk *could* recognise Rahul, but each copy went into a random drawer. She opens the wrong drawer and never gets to compare.

> **equals() without hashCode() is useless in a HashMap: the map never gets far enough to call it.**

### Step 3 · Override only hashCode()

Now hashCode() uses the ID. `Objects.hash(101)` gives **132** for both copies, so both go to bucket 132 % 16 = **4**. That's the right drawer.

But equals() is still the default one, which means `==`. Inside bucket 4, the map asks "is this the same object?" and the answer is no.

Result: set size **2**, and `map.get(e2)` returns **null**.

In the office, the clerk opens the right drawer, but she only checks "is it the same sheet of paper?", so she says "not found".

### Step 4 · Override both (the correct way)

Both copies have the same hashCode (132), so they land in the same bucket (4). equals() compares IDs, so it gives true.

Result: set size **1**, and `map.get(e2)` **finds** the value.

### The whole topic in one table

| You override | Same bucket? | equals() gives | HashSet size | map.get(copy) |
|---|---|---|---|---|
| nothing | no (random numbers) | false | 2 ❌ | null ❌ |
| only equals() | no (random numbers) | true, but it never gets called | 2 ❌ | null ❌ |
| only hashCode() | yes (bucket 4) | false (still `==`) | 2 ❌ | null ❌ |
| both | yes (bucket 4) | true | 1 ✅ | found ✅ |

### Step 5 · The rules (the "contract")

**Rule 1: if two objects are equal, their hashCodes must be equal.**
Otherwise they land in different buckets and the map never compares them (Step 2).

**Rule 2: equal hashCodes do NOT mean the objects are equal.**
That's just a collision (J01). "Aa" and "BB" both have hashCode 2112, but they are different strings. The map sorts it out with equals().

**Rule 3: use the same fields in both.**
Say equals() compares only the ID, but hashCode() uses the ID and the name. Then (101, "Rahul") and (101, "Rahul Sharma") are equal, yet they get different hashCodes, which breaks Rule 1. hashCode may use fewer fields than equals, but never more.

**Rule 4: don't change those fields while the object is inside a map or set.**
That's J01 Step 8: the hashCode changes and the entry gets lost. Make the fields `final`.

Interviewers sometimes ask for "the equals contract" itself:

| Rule | Meaning | Everyday version |
|---|---|---|
| reflexive | `a.equals(a)` is true | you are the same as yourself |
| symmetric | if `a.equals(b)`, then `b.equals(a)` | if copy 1 matches copy 2, copy 2 matches copy 1 |
| transitive | if a equals b and b equals c, then a equals c | copy 1 = copy 2 and copy 2 = copy 3, so copy 1 = copy 3 |
| consistent | same answer every time, if nothing changed | asking twice doesn't change the answer |
| null | `a.equals(null)` is false, never an exception | a real card never matches "no card" |

### Step 6 · How to write them

```java
public class Employee {
    private final int id;          // final: can't change after creation (J01 Step 8)
    private final String name;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;                                   // 1. same object? then equal
        if (o == null || getClass() != o.getClass()) return false;   // 2. null or another class? not equal
        Employee other = (Employee) o;                                // 3. now the cast is safe
        return id == other.id;                                        // 4. compare what defines "same"
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);                                      // the same field(s) as equals()
    }
}
```

To compare object fields such as a String, use `Objects.equals(name, other.name)`. It handles null safely.

Shortcuts you'll use at work:
- **VS Code:** right-click, then Source Action, then "Generate hashCode() and equals()". **IntelliJ:** Alt+Insert.
- **Lombok:** `@EqualsAndHashCode`, or `@Data`, which includes it.
- **Java 16+ record:** `record Employee(int id, String name) {}` generates equals() and hashCode() from **all** of its fields.

---

## How to explain it in the interview

Use your own words. Cover these points in this order, using the two copies of employee 101:

1. By default, `equals()` checks whether two references point to the same object, just like `==`. `hashCode()` comes from the object's identity, not its data.
2. HashMap and HashSet use `hashCode()` to pick the bucket and `equals()` to find the entry.
3. The contract: equal objects must have equal hashCodes. Equal hashCodes don't have to mean equal objects.
4. If you override only equals, equal objects get different hashCodes and land in different buckets. You get duplicates in a HashSet, and `get()` returns null.
5. If you override only hashCode, they land in the same bucket, but equals is still `==`, so they are still treated as different.
6. So override both, using the same fields. In practice, generate them with the IDE or Lombok, or use a record.

**Here's how it can sound** (about a minute, simple words):

> "By default, equals just checks whether two references point to the same object, and hashCode comes from the object's identity. HashMap and HashSet use hashCode to find the bucket and equals to find the entry inside it. So the contract is: if two objects are equal, they must have the same hashCode, but the same hashCode doesn't mean they're equal. Say I create employee 101 twice. If I override only equals, the two copies are equal but get different hashCodes, so they go to different buckets. A HashSet keeps both, and map.get with the copy returns null. If I override only hashCode, they go to the same bucket, but equals still compares references, so again they're treated as different. That's why we always override both, using the same fields. In practice I generate them with the IDE or Lombok, or use a record."

**Tip:** if they ask you to write it, write the Step 6 code and say each line's job out loud as you go.

---

## Follow-up questions (simple answers)

**What do equals() and hashCode() do by default?**
equals() is the same as `==`: is it the same object or not. hashCode() is a number tied to the object itself, so two objects with the same data usually get different numbers.

**Two objects have the same hashCode. Are they equal?**
Not necessarily. That's a collision: "Aa" and "BB" share hashCode 2112.

**Two objects are equal. Can they have different hashCodes?**
No. That breaks the contract, and HashMap can't find them (Step 2).

**What if hashCode() returns the same number, say 1, for every object?**
It's legal, because equal objects still get equal hashCodes. But every key lands in one bucket, so the map becomes slow (J01 Step 6).

**== vs equals()?**
`==` compares references: is it the same object? equals() compares content, if the class overrides it. For Strings, always use equals() (J03).

**getClass() or instanceof inside equals()?**
getClass() only accepts the exact same class. That's the safe default, and it's what IDEs generate. instanceof also accepts subclasses. JPA code often uses instanceof, because Hibernate creates proxy subclasses of your entities.

**Why the number 31 in hashCode?**
Objects.hash multiplies by 31 at each step, so Objects.hash(101) = 31 × 1 + 101 = 132. 31 is an odd prime, so it spreads values well, and 31 × x is fast to compute as (x << 5) - x.

**Where does this matter in real work?**
It matters for any object you use as a HashMap key or keep in a HashSet: request objects, cache keys, value objects. In Spring/JPA, it also matters for entities kept in `Set` collections.

*Only if they push further (JPA entities):* the database id is null until you save, so a hashCode based on the id changes after save. That's the J01 Step 8 problem. Use a business key that exists from the start, like a transaction ID. Also avoid Lombok `@Data` on entities: its equals/hashCode use every field, including lazy relations.

---

## Rules to remember

| Rule | Why |
|---|---|
| equal objects → same hashCode | otherwise they land in different buckets |
| same hashCode → maybe not equal | collisions are allowed |
| same fields in both | otherwise equal objects can get different hashCodes |
| don't change those fields after put() | the entry gets lost (J01 Step 8) |
| a.equals(null) → false | never throw an exception |

## Self-check (answer aloud, then click to check)

<details><summary>1. Two new Employee(101, "Rahul"), nothing overridden. What do == and equals() give?</summary>

Both give false. The default equals() is the same as ==, and these are two different objects.

</details>

<details><summary>2. Only equals() is overridden. You add both copies to a HashSet. What's the size, and why?</summary>

The size is 2. Their hashCodes are different random numbers, so the HashSet sees different hashes and never calls equals().

</details>

<details><summary>3. Only hashCode() is overridden, with Objects.hash(id). Which bucket does employee 101 go to in a 16-bucket map? What's the set size?</summary>

Objects.hash(101) = 132, and 132 % 16 = 4, so it goes to bucket 4. The set size is still 2, because equals() is still ==.

</details>

<details><summary>4. Two objects both have hashCode 132. Must they be equal?</summary>

No. A shared hashCode only means the same bucket, which is a collision. equals() decides.

</details>

<details><summary>5. equals() uses only the id, but hashCode() uses the id and the name. What breaks?</summary>

(101, "Rahul") and (101, "Rahul Sharma") are equal but get different hashCodes. A HashSet keeps both, and get() with one of them can't find the other.

</details>

<details><summary>6. Write equals() and hashCode() for Employee, using the id.</summary>

Write the Step 6 code: check this == o, check for null and a different class, cast, compare the ids, and return Objects.hash(id).

</details>

<details><summary>7. For record Employee(int id, String name), are (101, "Rahul") and (101, "Rahul Sharma") equal?</summary>

No. A record compares all of its fields, and the names are different.

</details>

If you get stuck on any of them, add it to [STUMBLE-LIST.md](../STUMBLE-LIST.md). When all 7 feel easy, tick J02 in the [README](../README.md) and send `next`.
