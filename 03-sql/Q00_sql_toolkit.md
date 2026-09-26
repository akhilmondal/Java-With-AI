# Q00 · SQL toolkit: how a query really runs, JOINs, GROUP BY/HAVING, window functions

**Read this first (15 min).** Then paste [Q00_setup.sql](Q00_setup.sql) into db-fiddle.com (choose PostgreSQL, left box), paste any query from this page into the right box, and click Run.

Don't memorize syntax. Understand the 7 steps using the 8 employees below. Once you get those, the 6 problems (Q01 to Q06) are just these tools combined, and you can answer the theory questions ("WHERE vs HAVING?", "RANK vs DENSE_RANK?") in your own words.

| id | name | salary | department | manager |
|---|---|---|---|---|
| 1 | Meera | 90,000 | PAYMENTS | none |
| 2 | Rahul | 40,000 | PAYMENTS | Meera |
| 3 | Priya | 80,000 | PAYMENTS | Meera |
| 4 | Sneha | 90,000 | BILLING | Meera |
| 5 | Amit | 30,000 | BILLING | Sneha |
| 6 | Karan | 60,000 | BILLING | Sneha |
| 7 | Vikram | 70,000 | UI | Meera |
| 8 | Neha | 75,000 | UI | Vikram |

A fourth department, **SECURITY**, has nobody in it yet.

---

## The problem

SQL rounds test two things: **writing** a few classic queries (Q01 to Q06), and **explaining** why they work. Most wrong answers come from not knowing the order in which the database runs a query, how JOINs treat missing rows, and how NULL behaves.

## Real-life picture: an office with two registers

- The **staff register** (employees) and the **department register** (departments).
- **JOIN:** lay the two registers side by side and match the lines on the department number.
  - **INNER JOIN:** keep only the lines that match in both registers.
  - **LEFT JOIN:** keep **every** line of the left register, and leave blanks where nothing matches, like SECURITY, which has no staff.
- **GROUP BY:** make one pile per department, then write **one summary line per pile**: how many people, total salary, highest salary.
- **WHERE:** cross out single staff lines **before** making the piles.
- **HAVING:** throw away whole piles **after** they're made.
- **Window function:** write a rank next to each person **inside their pile**, without merging the lines into one.

| Office | SQL |
|---|---|
| the two registers | the `employees` and `departments` tables |
| matching lines on the department number | `JOIN ... ON e.department_id = d.id` |
| only the lines that match | `INNER JOIN` (plain `JOIN`) |
| every department, blanks if no staff | `LEFT JOIN` |
| one pile per department, one summary line each | `GROUP BY` + `COUNT / SUM / AVG / MAX / MIN` |
| crossing out lines before making piles | `WHERE` |
| throwing away whole piles | `HAVING` |
| a rank next to each person in their pile | `RANK() OVER (PARTITION BY ... ORDER BY ...)` |

---

## Step by step

### Step 1 · The order a query really runs in

You **write** `SELECT` first, but the database **runs** it almost last:

```text
written:  SELECT ... FROM ... WHERE ... GROUP BY ... HAVING ... ORDER BY ... LIMIT
runs as:  1 FROM + JOIN -> 2 WHERE -> 3 GROUP BY -> 4 HAVING -> 5 SELECT -> 6 DISTINCT -> 7 ORDER BY -> 8 LIMIT/OFFSET
```

Follow one query through, with numbers: "departments with at least 2 people earning more than 60,000":

```sql
SELECT department_id, COUNT(*) AS people
FROM employees
WHERE salary > 60000
GROUP BY department_id
HAVING COUNT(*) >= 2
ORDER BY department_id;
```

| Stage | What's left |
|---|---|
| 1 FROM | all 8 employees |
| 2 WHERE salary > 60000 | Meera, Priya, Sneha, Vikram, Neha. Rahul, Amit and Karan (exactly 60,000, not more) are gone |
| 3 GROUP BY department | dept 1: Meera, Priya · dept 2: Sneha · dept 3: Vikram, Neha |
| 4 HAVING COUNT(*) >= 2 | dept 1 (**2**) and dept 3 (**2**). Dept 2 has only 1, so the whole pile is gone |
| 5 SELECT | `department_id, people` |

The result is **1 → 2** and **3 → 2**.

This order explains three classic errors:
- **You can't use `COUNT(*)` in WHERE.** WHERE runs before the piles exist. Use HAVING.
- **You can't use a SELECT alias in WHERE.** The alias doesn't exist yet at step 2.
- **You can't use a window function in WHERE.** Window functions run at step 5. Wrap the query in a subquery or CTE, then filter outside it (Q01, Q02).

### Step 2 · JOINs: which rows survive

```sql
-- INNER JOIN: only departments that have staff
SELECT d.name, COUNT(e.id) AS people
FROM departments d
JOIN employees e ON e.department_id = d.id
GROUP BY d.name
ORDER BY d.name;
-- BILLING 3 · PAYMENTS 3 · UI 2          (SECURITY is missing)

-- LEFT JOIN: every department, with blanks (NULL) where nothing matched
SELECT d.name, COUNT(e.id) AS people
FROM departments d
LEFT JOIN employees e ON e.department_id = d.id
GROUP BY d.name
ORDER BY d.name;
-- BILLING 3 · PAYMENTS 3 · SECURITY 0 · UI 2
```

| JOIN | Keeps |
|---|---|
| `INNER JOIN` | only rows that match on both sides |
| `LEFT JOIN` | every row from the left table; NULLs where the right side has no match |
| `RIGHT JOIN` | every row from the right table (the same as a LEFT JOIN with the tables swapped) |
| `FULL JOIN` | every row from both sides |
| `CROSS JOIN` | every row paired with every row: 4 departments × 8 employees = 32 rows |
| self join | a table joined to itself, like employee → manager (Q05) |

### Step 3 · GROUP BY and the aggregate functions

```sql
SELECT d.name AS department,
       COUNT(*)               AS people,
       SUM(e.salary)          AS total,
       ROUND(AVG(e.salary), 2) AS average,
       MAX(e.salary)          AS highest,
       MIN(e.salary)          AS lowest
FROM employees e
JOIN departments d ON d.id = e.department_id
GROUP BY d.name
ORDER BY d.name;
```

| department | people | total | average | highest | lowest |
|---|---|---|---|---|---|
| BILLING | 3 | 180,000 | 60,000.00 | 90,000 | 30,000 |
| PAYMENTS | 3 | 210,000 | 70,000.00 | 90,000 | 40,000 |
| UI | 2 | 145,000 | 72,500.00 | 75,000 | 70,000 |

**The rule:** every column in SELECT must be either **in the GROUP BY** or **inside an aggregate**. `SELECT department_id, name, MAX(salary) ... GROUP BY department_id` is an error in PostgreSQL, because a pile has 3 names and the database can't know which one you mean (Q02).

### Step 4 · WHERE vs HAVING

- **WHERE** filters **rows**, before grouping. It can't use aggregates.
- **HAVING** filters **groups**, after grouping. It usually uses aggregates.

Step 1's query used both: `WHERE salary > 60000` removed people, then `HAVING COUNT(*) >= 2` removed BILLING's pile.

### Step 5 · Window functions: rank inside the pile, keep every line

GROUP BY squashes each pile into one line. A **window function** computes across the pile but **keeps every row**:

```sql
SELECT name, salary,
       ROW_NUMBER() OVER (ORDER BY salary DESC, name) AS row_num,
       RANK()       OVER (ORDER BY salary DESC)       AS rank_num,
       DENSE_RANK() OVER (ORDER BY salary DESC)       AS dense_rank_num
FROM employees
ORDER BY salary DESC, name;
```

| name | salary | ROW_NUMBER | RANK | DENSE_RANK |
|---|---|---|---|---|
| Meera | 90,000 | 1 | 1 | 1 |
| Sneha | 90,000 | 2 | **1** | **1** |
| Priya | 80,000 | 3 | **3** | **2** |
| Neha | 75,000 | 4 | 4 | 3 |
| Vikram | 70,000 | 5 | 5 | 4 |
| Karan | 60,000 | 6 | 6 | 5 |
| Rahul | 40,000 | 7 | 7 | 6 |
| Amit | 30,000 | 8 | 8 | 7 |

Look at the tie at 90,000:
- **ROW_NUMBER:** always 1, 2, 3, even for ties (the tie is broken by name here).
- **RANK:** ties share a number, then it **skips** (1, 1, 3). This is like sports, where two gold medals mean no silver.
- **DENSE_RANK:** ties share a number, with **no gaps** (1, 1, 2). This is the one for "Nth highest salary" (Q01).

**PARTITION BY** restarts the ranking in every pile:

```sql
SELECT d.name AS department, e.name, e.salary,
       RANK() OVER (PARTITION BY e.department_id ORDER BY e.salary DESC) AS rank_in_dept
FROM employees e
JOIN departments d ON d.id = e.department_id
ORDER BY department, rank_in_dept;
-- BILLING:  Sneha 1, Karan 2, Amit 3
-- PAYMENTS: Meera 1, Priya 2, Rahul 3
-- UI:       Neha 1, Vikram 2
```

### Step 6 · Subquery vs CTE (`WITH`)

A **CTE** (Common Table Expression) is a named subquery written **before** the main query, so the query reads top to bottom:

```sql
WITH dept_totals AS (
    SELECT department_id, SUM(salary) AS total
    FROM employees
    GROUP BY department_id
)
SELECT d.name, t.total
FROM dept_totals t
JOIN departments d ON d.id = t.department_id
WHERE t.total > 150000
ORDER BY d.name;
-- BILLING 180000 · PAYMENTS 210000      (UI's 145000 is filtered out)
```

It gives the same result as a subquery, and it's easier to read and reuse. `WITH RECURSIVE` can even walk a tree, like the full reporting line in Q05.

### Step 7 · NULL: "unknown", not zero

```sql
SELECT name FROM employees WHERE manager_id = NULL;     -- no rows! NULL = NULL is not true
SELECT name FROM employees WHERE manager_id IS NULL;    -- Meera
SELECT COUNT(*) AS all_rows, COUNT(manager_id) AS with_manager FROM employees;   -- 8, 7
```

- Compare with `IS NULL` / `IS NOT NULL`, never with `=`.
- `COUNT(*)` counts rows, but `COUNT(column)` **skips NULLs**. That's why Step 2's LEFT JOIN used `COUNT(e.id)`: it gives SECURITY **0**, where `COUNT(*)` would give 1 (Q06).
- `NOT IN` with a NULL in the list returns **nothing** (Q06).
- `COALESCE(x, 'default')` replaces a NULL: `COALESCE(m.name, 'No manager')` (Q05).

---

## How to explain it in the interview

Use your own words. When asked any SQL theory question, reach for these:

1. **Run order:** FROM/JOIN, WHERE, GROUP BY, HAVING, SELECT, ORDER BY, LIMIT. That's why aggregates go in HAVING, not WHERE.
2. **JOINs:** INNER keeps only matches. LEFT keeps every left row, with NULLs where there's no match (for example, departments with no employees).
3. **GROUP BY:** one row per group. Every selected column must be grouped or aggregated.
4. **Window functions:** calculate across a group but keep every row. ROW_NUMBER is always unique, RANK leaves gaps after ties, DENSE_RANK has no gaps.
5. **NULL:** use IS NULL. COUNT(column) skips NULLs. Watch out for NOT IN with NULLs.

**Here's how it can sound** ("WHERE vs HAVING", about 30 seconds):

> "WHERE filters individual rows before grouping, and HAVING filters groups after GROUP BY. For example, to find departments with at least two people earning over 60,000, I put salary > 60000 in WHERE to remove low earners first, then GROUP BY department, then HAVING COUNT(*) >= 2 to drop the departments with fewer than two. You can't use COUNT in WHERE, because WHERE runs before the groups exist."

**Tip:** for any query on a whiteboard, say the run order out loud. It shows you understand it, not just the syntax.

---

## Follow-up questions (simple answers)

**DELETE vs TRUNCATE vs DROP?**
- `DELETE` removes chosen rows (it can have a WHERE), fires triggers and can be rolled back.
- `TRUNCATE` quickly empties the whole table.
- `DROP` removes the table itself.

**UNION vs UNION ALL?**
`UNION` removes duplicate rows, which costs a sort or hash. `UNION ALL` keeps everything and is faster. Use UNION ALL unless you really need de-duplication.

**PRIMARY KEY vs UNIQUE?**
Both reject duplicates. A table has **one** primary key, which also can't be NULL. It can have many UNIQUE constraints, and in PostgreSQL a UNIQUE column can hold several NULLs.

**What does a FOREIGN KEY do?**
It makes sure a value exists in another table. `employees.department_id` must be a real `departments.id`, so you can't point at a department that doesn't exist.

**Normalization, in one line each?**
- **1NF:** one value per cell, no lists in a column.
- **2NF:** every column depends on the whole key.
- **3NF:** no column depends on another non-key column. For example, don't store `department_name` in employees; store `department_id` and join.

**What's a view?**
A saved query that you can use like a table. It stores no data itself, unless it's a materialized view.

**CHAR vs VARCHAR?**
CHAR(n) is always padded to n characters. VARCHAR(n) stores only what you give it, up to n. In PostgreSQL, TEXT and VARCHAR perform the same.

*Only if they push further:* PostgreSQL has `DISTINCT ON` (Q02) and `FILTER`, as in `COUNT(*) FILTER (WHERE salary > 60000)`. Both are shortcuts that aren't in standard SQL.

---

## Numbers to remember

| What | Result |
|---|---|
| People per department | PAYMENTS 3, BILLING 3, UI 2, SECURITY 0 |
| Average salary | PAYMENTS 70,000, BILLING 60,000, UI 72,500 |
| Tie at the top | Meera and Sneha, 90,000 each |
| RANK / DENSE_RANK for Priya | 3 / 2 |
| `COUNT(*)` vs `COUNT(manager_id)` | 8 vs 7 |

## Self-check (answer aloud, then click to check)

<details><summary>1. In what order does a query run: SELECT, WHERE, GROUP BY, HAVING, FROM, ORDER BY?</summary>

FROM, WHERE, GROUP BY, HAVING, SELECT, ORDER BY. After that come DISTINCT (before ORDER BY) and LIMIT/OFFSET (at the very end).

</details>

<details><summary>2. Why is "WHERE COUNT(*) > 1" an error?</summary>

WHERE runs before GROUP BY, so the groups, and their counts, don't exist yet. Use HAVING COUNT(*) > 1.

</details>

<details><summary>3. Departments LEFT JOIN employees, then COUNT(e.id) per department: what does SECURITY get? And with COUNT(*)?</summary>

0 with COUNT(e.id). With COUNT(*) it gets 1, because the LEFT JOIN makes one row for SECURITY with NULL employee columns.

</details>

<details><summary>4. What are RANK and DENSE_RANK for Priya (80,000), who comes after two people at 90,000?</summary>

RANK 3 (it skips 2), DENSE_RANK 2 (no gaps).

</details>

<details><summary>5. What's the average salary in BILLING, and who earns more than it?</summary>

(90,000 + 30,000 + 60,000) / 3 = 60,000. Only Sneha. Karan is exactly 60,000, which isn't "more than".

</details>

<details><summary>6. How many rows does "departments CROSS JOIN employees" give?</summary>

4 × 8 = 32.

</details>

<details><summary>7. What does WHERE manager_id = NULL return, and what should you write instead?</summary>

It returns no rows, because NULL = NULL is unknown, not true. Write WHERE manager_id IS NULL instead, which gives Meera.

</details>

If you get stuck on any of them, add it to [STUMBLE-LIST.md](../STUMBLE-LIST.md). When all 7 feel easy, tick Q00 in the [README](../README.md) and start Q01.
