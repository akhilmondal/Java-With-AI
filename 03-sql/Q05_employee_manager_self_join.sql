/* ============================================================================
   Q05  Each employee with their manager's name (self-join)        (PostgreSQL)

   THE QUESTION
     "Show every employee next to their manager's name."
     The manager is ALSO an employee, in the same table: manager_id points to
     another row's id.

   THE DATA (employees)
     id  name    manager_id
     1   Meera   NULL        <- the head: no manager
     2   Rahul   1
     3   Priya   1
     4   Sneha   1
     5   Amit    4
     6   Karan   4
     7   Vikram  1
     8   Neha    7

   EXPECTED (every employee, Meera shows "No manager")
     employee  manager
     Meera     No manager
     Rahul     Meera
     Priya     Meera
     Sneha     Meera
     Amit      Sneha
     Karan     Sneha
     Vikram    Meera
     Neha      Vikram

   TRY IT YOURSELF FIRST (10 minutes). Then scroll down.
   ============================================================================ */




































-- ============================================================================
-- WHY THIS TOOL EXISTS (the story, read it after your own try)
-- ============================================================================
-- Pain 1: a manager is also an employee. A separate managers table would
--         store the same person twice. Sneha would be in both tables, because
--         she reports to Meera AND manages Amit and Karan.
-- Fix 1 : keep one table, where manager_id points to another row's id.
-- Pain 2: now the manager's NAME is in another row of the SAME table.
-- Fix 2 : a self-join. Use the table twice with two aliases, e (the employee)
--         and m (the manager), and match m.id = e.manager_id.
-- Pain 3: an INNER JOIN drops Meera, because she has no manager to match.
-- Fix 3 : LEFT JOIN keeps her, and COALESCE turns her NULL into "No manager".


-- ============================================================================
-- THE IDEA: use the same table twice, with two different aliases
-- ============================================================================
--   e = "the employee" copy,   m = "the manager" copy
--   Match: the employee's manager_id = the manager's id
--
--   e: Neha (manager_id 7)  ---->  m: id 7 = Vikram


-- ============================================================================
-- SOLUTION 1: INNER JOIN (only employees who HAVE a manager)
-- ============================================================================
SELECT e.name AS employee,
       m.name AS manager
FROM employees e
JOIN employees m ON m.id = e.manager_id
ORDER BY e.id;
-- Result (7 rows: Meera is missing, because her manager_id is NULL):
--   employee  manager
--   Rahul     Meera
--   Priya     Meera
--   Sneha     Meera
--   Amit      Sneha
--   Karan     Sneha
--   Vikram    Meera
--   Neha      Vikram


-- ============================================================================
-- SOLUTION 2: LEFT JOIN + COALESCE (everyone, the usual expected answer)
-- ============================================================================
SELECT e.name                        AS employee,
       COALESCE(m.name, 'No manager') AS manager
FROM employees e
LEFT JOIN employees m ON m.id = e.manager_id
ORDER BY e.id;
-- Result (8 rows):
--   employee  manager
--   Meera     No manager
--   Rahul     Meera
--   Priya     Meera
--   Sneha     Meera
--   Amit      Sneha
--   Karan     Sneha
--   Vikram    Meera
--   Neha      Vikram


-- ============================================================================
-- FOLLOW-UP 1: how many people report to each manager?
-- ============================================================================
SELECT m.name   AS manager,
       COUNT(*) AS reports
FROM employees e
JOIN employees m ON m.id = e.manager_id
GROUP BY m.name
ORDER BY reports DESC, manager;
-- Result:
--   manager  reports
--   Meera    4
--   Sneha    2
--   Vikram   1


-- ============================================================================
-- FOLLOW-UP 2: employees who earn MORE than their manager
-- ============================================================================
SELECT e.name   AS employee,
       e.salary,
       m.name   AS manager,
       m.salary AS manager_salary
FROM employees e
JOIN employees m ON m.id = e.manager_id
WHERE e.salary > m.salary;
-- Result:
--   employee  salary  manager  manager_salary
--   Neha      75000   Vikram   70000


-- ============================================================================
-- FOLLOW-UP 3 (only if they push): the whole reporting line, up to the top
-- ============================================================================
-- WITH RECURSIVE: start with Neha, then keep joining to "my manager" until
-- nobody is left.
WITH RECURSIVE chain AS (
    SELECT id, name, manager_id, 0 AS level
    FROM employees
    WHERE name = 'Neha'
    UNION ALL
    SELECT m.id, m.name, m.manager_id, c.level + 1
    FROM employees m
    JOIN chain c ON m.id = c.manager_id
)
SELECT level, name
FROM chain
ORDER BY level;
-- Result:
--   level  name
--   0      Neha
--   1      Vikram
--   2      Meera


/* ============================================================================
   HOW TO EXPLAIN IT IN THE INTERVIEW (your own words, cover these points)
     1. The manager is a row in the same table, so I join employees to itself
        with two aliases: e for the employee and m for the manager.
     2. The join condition is m.id = e.manager_id.
     3. An INNER JOIN drops the top boss (manager_id is NULL), so I use a LEFT
        JOIN, with COALESCE to show "No manager".
     4. From there: GROUP BY m.name to count reports, or compare e.salary with
        m.salary.

   Here's how it can sound:
     "Since managers are also employees, I do a self-join: employees e LEFT JOIN
      employees m ON m.id = e.manager_id, and select e.name and m.name. I use a
      LEFT JOIN so the top person, whose manager_id is NULL, still appears, and
      COALESCE to print 'No manager'. The same join answers follow-ups like
      counting direct reports, or finding people who earn more than their
      manager, which is Neha here."

   FOLLOW-UPS
     Q: Why two aliases?
     A: The same table plays two roles. The aliases tell the database which copy
        each column comes from.
     Q: The whole chain up to the CEO?
     A: WITH RECURSIVE, as in follow-up 3.
     Q: Employees with no reports (the leaves)?
     A: WHERE NOT EXISTS (SELECT 1 FROM employees r WHERE r.manager_id = e.id).
        That gives Rahul, Priya, Amit, Karan and Neha.

   SELF-CHECK (answers at the very bottom)
     1. How many rows does the INNER JOIN version return, and who is missing?
     2. What's the join condition for employee -> manager?
     3. How many people report to Sneha?
     4. Who earns more than their manager?
   ============================================================================ */

-- Answers: 1) 7, Meera (no manager)   2) m.id = e.manager_id
--          3) 2 (Amit, Karan)          4) Neha (75000 vs Vikram's 70000)

-- QUICK REVISION START
-- Q05 Employee with manager's name (self-join)
--   Story   : a managers table would store people twice -> manager_id in the same table ->
--             the name is in another row -> self-join with aliases e and m ->
--             INNER JOIN drops the top boss -> LEFT JOIN + COALESCE
--   Picture : employees e (the employee)  --  e.manager_id = m.id  -->  employees m (the manager)
--             Neha (manager_id 7)  ---->  id 7 = Vikram
--   Query   : SELECT e.name, COALESCE(m.name, 'No manager') FROM employees e
--             LEFT JOIN employees m ON m.id = e.manager_id       -> 8 rows, Meera = No manager
--   INNER   : JOIN instead of LEFT JOIN -> 7 rows (the top boss disappears)
--   Extras  : reports per manager: GROUP BY m.name -> Meera 4, Sneha 2, Vikram 1
--             earns more than the manager: WHERE e.salary > m.salary -> Neha (75000 > 70000)
--             the whole chain up: WITH RECURSIVE -> Neha, Vikram, Meera
--   30-second answer: "The manager is a row in the same table, so I join employees to itself with
--             aliases e and m on m.id = e.manager_id, as a LEFT JOIN so the top boss still appears."
-- QUICK REVISION END
