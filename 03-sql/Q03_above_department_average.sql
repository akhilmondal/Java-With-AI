/* ============================================================================
   Q03  Employees earning above their department's average         (PostgreSQL)

   THE QUESTION
     "List employees whose salary is higher than the average salary of THEIR
      department."

   THE DATA, with each department's average worked out
     PAYMENTS : Meera 90000, Priya 80000, Rahul 40000   avg = 210000 / 3 = 70000
     BILLING  : Sneha 90000, Karan 60000, Amit 30000    avg = 180000 / 3 = 60000
     UI       : Neha 75000,  Vikram 70000               avg = 145000 / 2 = 72500

   EXPECTED
     department  name   salary  dept_avg
     BILLING     Sneha  90000   60000
     PAYMENTS    Meera  90000   70000
     PAYMENTS    Priya  80000   70000
     UI          Neha   75000   72500
   Karan earns exactly 60000, which is NOT above BILLING's 60000, so he's out.

   TRY IT YOURSELF FIRST (10 minutes). Then scroll down.
   ============================================================================ */




































-- ============================================================================
-- SOLUTION 1: a correlated subquery (short, and easy to say out loud)
-- ============================================================================
-- For each employee e, the inner query works out the average of e's OWN
-- department, then we compare.
SELECT e.name, e.salary, e.department_id
FROM employees e
WHERE e.salary > (SELECT AVG(e2.salary)
                  FROM employees e2
                  WHERE e2.department_id = e.department_id)
ORDER BY e.department_id, e.salary DESC;
-- Result:
--   name   salary  department_id
--   Meera  90000   1
--   Priya  80000   1
--   Sneha  90000   2
--   Neha   75000   3


-- ============================================================================
-- SOLUTION 2: a CTE with the averages, then a join (clear and fast)
-- ============================================================================
-- Step 1 (the CTE): one row per department with its average.
-- Step 2: join every employee to their department's average, and compare.
WITH dept_avg AS (
    SELECT department_id, AVG(salary) AS avg_salary
    FROM employees
    GROUP BY department_id
)
SELECT d.name                  AS department,
       e.name,
       e.salary,
       ROUND(a.avg_salary, 2)  AS dept_avg
FROM employees e
JOIN dept_avg a    ON a.department_id = e.department_id
JOIN departments d ON d.id = e.department_id
WHERE e.salary > a.avg_salary
ORDER BY department, e.salary DESC;
-- Result:
--   department  name   salary  dept_avg
--   BILLING     Sneha  90000   60000.00
--   PAYMENTS    Meera  90000   70000.00
--   PAYMENTS    Priya  80000   70000.00
--   UI          Neha   75000   72500.00


-- ============================================================================
-- SOLUTION 3: a window function, AVG() OVER (PARTITION BY ...)
-- ============================================================================
-- Puts the department average on EVERY row without grouping, then we filter
-- outside, because window functions can't go in WHERE.
SELECT name, salary, dept_avg
FROM (
    SELECT e.name,
           e.salary,
           ROUND(AVG(e.salary) OVER (PARTITION BY e.department_id), 2) AS dept_avg
    FROM employees e
) with_avg
WHERE salary > dept_avg
ORDER BY salary DESC, name;
-- Result:
--   name   salary  dept_avg
--   Meera  90000   70000.00
--   Sneha  90000   60000.00
--   Priya  80000   70000.00
--   Neha   75000   72500.00


/* ============================================================================
   HOW TO EXPLAIN IT IN THE INTERVIEW (your own words, cover these points)
     1. I need each department's average, then compare every employee with
        the average of their own department.
     2. The simplest version is a correlated subquery: WHERE salary >
        (SELECT AVG(salary) ... WHERE same department).
     3. A cleaner version: a CTE with GROUP BY department_id, joined back on
        department_id.
     4. Or AVG() OVER (PARTITION BY department_id) in a subquery, then filter.
     5. ">" vs ">=": Karan equals BILLING's average exactly, so he's out.

   Here's how it can sound:
     "I'd first compute the average salary per department, in a CTE grouped by
      department_id, then join employees to it on department_id and keep the
      rows where salary is greater than that average. For example, PAYMENTS
      averages 70000, so Meera and Priya qualify but Rahul doesn't. A correlated
      subquery also works, but it can re-run the inner query for every row, so
      on big tables I prefer the CTE or a window function."

   FOLLOW-UPS
     Q: Above the COMPANY average instead?
     A: Compare with a plain subquery: WHERE salary > (SELECT AVG(salary) FROM employees).
        The company average is 535000 / 8 = 66875.
     Q: Correlated subquery vs join: which is faster?
     A: A correlated subquery can run once per outer row. The CTE computes each
        average once. PostgreSQL often rewrites both well, and EXPLAIN tells you (Q07).
     Q: Why ROUND?
     A: AVG of integers returns a decimal like 72500.000000, ROUND(x, 2) just
        makes it readable.

   SELF-CHECK (answers at the very bottom)
     1. What is UI's average, and who is above it?
     2. Why isn't Karan in the result?
     3. Which window function puts the department average on every row?
     4. Who earns more than the company average of 66875?
   ============================================================================ */

-- Answers: 1) 72500, only Neha (75000)   2) 60000 is not greater than 60000
--          3) AVG(salary) OVER (PARTITION BY department_id)
--          4) Meera, Priya, Sneha, Vikram, Neha (Karan 60000, Rahul, Amit are below)

-- QUICK REVISION START
-- Q03 Employees earning above their department's average
--   Averages : PAYMENTS 210000/3 = 70000 | BILLING 180000/3 = 60000 | UI 145000/2 = 72500
--   Result   : Meera 90000, Priya 80000 (PAYMENTS), Sneha 90000 (BILLING), Neha 75000 (UI)
--   Way 1    : WHERE e.salary > (SELECT AVG(salary) FROM employees e2 WHERE e2.department_id = e.department_id)
--   Way 2    : WITH dept_avg AS (SELECT department_id, AVG(salary) avg_salary ... GROUP BY department_id)
--              then JOIN on department_id WHERE salary > avg_salary
--   Way 3    : AVG(salary) OVER (PARTITION BY department_id) in a subquery, filter outside
--   Trap     : Karan = 60000 equals BILLING's average -> ">" leaves him out
--   30-second answer: "Compute each department's average in a CTE, join it back on department_id and
--              keep salary > average. A correlated subquery also works but can run once per row."
-- QUICK REVISION END
