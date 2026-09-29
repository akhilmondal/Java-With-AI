/* ============================================================================
   Q06  Departments with zero employees (LEFT JOIN)                (PostgreSQL)

   THE QUESTION
     "Find the departments that have no employees."

   THE DATA
     departments: 1 PAYMENTS, 2 BILLING, 3 UI, 4 SECURITY
     employees  : 3 in PAYMENTS, 3 in BILLING, 2 in UI, nobody in SECURITY

   EXPECTED
     department
     SECURITY

   TRY IT YOURSELF FIRST (10 minutes), with a LEFT JOIN. Then scroll down.
   ============================================================================ */




































-- ============================================================================
-- WHY THESE TOOLS EXIST (the story, read it after your own try)
-- ============================================================================
-- Pain 1: an INNER JOIN keeps only departments that match an employee, so the
--         empty SECURITY department simply disappears. You can't see what
--         isn't there.
-- Fix 1 : LEFT JOIN keeps every department, with NULLs where nobody matched.
--         Then WHERE e.id IS NULL keeps exactly the empty ones.
-- Pain 2: NOT IN looks simpler, but one NULL in its list makes it return
--         nothing at all.
-- Fix 2 : NOT EXISTS asks "is there any matching employee?", and it's safe
--         with NULLs.


-- ============================================================================
-- THE IDEA: LEFT JOIN keeps every department, "no match" shows up as NULLs
-- ============================================================================
--   departments LEFT JOIN employees:
--     PAYMENTS -> Meera, Rahul, Priya
--     BILLING  -> Sneha, Amit, Karan
--     UI       -> Vikram, Neha
--     SECURITY -> NULL        <- no employee matched, so every employee column is NULL
--   Keep the rows where the employee side is NULL.


-- ============================================================================
-- SOLUTION 1: LEFT JOIN ... WHERE e.id IS NULL
-- ============================================================================
SELECT d.name AS department
FROM departments d
LEFT JOIN employees e ON e.department_id = d.id
WHERE e.id IS NULL;
-- Result:
--   department
--   SECURITY


-- ============================================================================
-- SOLUTION 2: NOT EXISTS (just as good, and very clear)
-- ============================================================================
SELECT d.name AS department
FROM departments d
WHERE NOT EXISTS (SELECT 1
                  FROM employees e
                  WHERE e.department_id = d.id);
-- Result:
--   department
--   SECURITY


-- ============================================================================
-- FOLLOW-UP 1: the head count of EVERY department, including zero
-- ============================================================================
SELECT d.name      AS department,
       COUNT(e.id) AS employees        -- COUNT(column) skips NULLs, so SECURITY gets 0
FROM departments d
LEFT JOIN employees e ON e.department_id = d.id
GROUP BY d.name
ORDER BY d.name;
-- Result:
--   department  employees
--   BILLING     3
--   PAYMENTS    3
--   SECURITY    0
--   UI          2


-- TRAP 1: COUNT(*) counts the NULL row too, so SECURITY wrongly gets 1.
SELECT d.name   AS department,
       COUNT(*) AS wrong_count
FROM departments d
LEFT JOIN employees e ON e.department_id = d.id
GROUP BY d.name
ORDER BY d.name;
-- Result:
--   department  wrong_count
--   BILLING     3
--   PAYMENTS    3
--   SECURITY    1       <- WRONG
--   UI          2


-- ============================================================================
-- TRAP 2: a condition on the RIGHT table in WHERE turns the LEFT JOIN into an INNER JOIN
-- ============================================================================
-- Task: for every department, count the people earning more than 60000.
-- WRONG: SECURITY's row has e.salary = NULL, and "NULL > 60000" isn't true,
-- so WHERE throws that row away.
SELECT d.name      AS department,
       COUNT(e.id) AS earning_over_60k
FROM departments d
LEFT JOIN employees e ON e.department_id = d.id
WHERE e.salary > 60000
GROUP BY d.name
ORDER BY d.name;
-- Result:
--   department  earning_over_60k
--   BILLING     1
--   PAYMENTS    2
--   UI          2        <- SECURITY has disappeared

-- RIGHT: put that condition in ON, so it only decides which employees match.
SELECT d.name      AS department,
       COUNT(e.id) AS earning_over_60k
FROM departments d
LEFT JOIN employees e ON e.department_id = d.id AND e.salary > 60000
GROUP BY d.name
ORDER BY d.name;
-- Result:
--   department  earning_over_60k
--   BILLING     1
--   PAYMENTS    2
--   SECURITY    0
--   UI          2


-- ============================================================================
-- TRAP 3: NOT IN with a NULL in the list returns NOTHING
-- ============================================================================
SELECT name FROM departments WHERE id NOT IN (1, 2, 3);
-- Result: SECURITY

SELECT name FROM departments WHERE id NOT IN (1, 2, 3, NULL);
-- Result: no rows at all!
-- For SECURITY, the database asks "4 <> 1 AND 4 <> 2 AND 4 <> 3 AND 4 <> NULL".
-- "4 <> NULL" is unknown, so the whole test is unknown and the row is dropped.
-- If employees.department_id ever holds a NULL, then
-- "WHERE id NOT IN (SELECT department_id FROM employees)" silently returns nothing.
-- NOT EXISTS doesn't have this problem, which is why it's the safer choice.


/* ============================================================================
   HOW TO EXPLAIN IT IN THE INTERVIEW (your own words, cover these points)
     1. Start from departments and LEFT JOIN employees, so every department
        stays in the result.
     2. A department with no employees gets NULLs on the employee side, so I
        keep WHERE e.id IS NULL.
     3. NOT EXISTS is an equally good answer. NOT IN is risky with NULLs.
     4. For counts, use COUNT(e.id), not COUNT(*). A condition on the employee
        table belongs in ON, not WHERE, or the LEFT JOIN turns into an INNER JOIN.

   Here's how it can sound:
     "I'd LEFT JOIN departments to employees, so every department comes back
      even without a match, and then filter WHERE e.id IS NULL. Those are the
      departments with no employees, SECURITY here. NOT EXISTS works just as
      well. I'd avoid NOT IN, because if the subquery returns a NULL, NOT IN
      returns no rows at all. And when counting, I use COUNT(e.id), since
      COUNT(*) would count the empty department as 1."

   FOLLOW-UPS
     Q: Employees with no department?
     A: The mirror image: employees e LEFT JOIN departments d ... WHERE d.id IS NULL.
     Q: LEFT JOIN vs NOT EXISTS: which is faster?
     A: PostgreSQL usually plans both the same way (an "anti-join"). Choose the
        clearest, check with EXPLAIN (Q07).
     Q: RIGHT JOIN?
     A: The same idea with the sides swapped. Most people write every outer
        join as a LEFT JOIN, for readability.

   SELF-CHECK (answers at the very bottom)
     1. Why does an INNER JOIN never find SECURITY?
     2. What count does SECURITY get with COUNT(*), and with COUNT(e.id)?
     3. Where does the "salary > 60000" condition go to keep every department?
     4. Why is NOT IN risky here?
   ============================================================================ */

-- Answers: 1) an INNER JOIN keeps only matching rows, and SECURITY has none
--          2) 1 and 0   3) in the ON clause
--          4) one NULL in the list makes NOT IN return no rows

-- QUICK REVISION START
-- Q06 Departments with zero employees
--   Story   : INNER JOIN hides unmatched rows -> LEFT JOIN keeps them, with NULLs -> keep e.id IS NULL ->
--             NOT IN breaks on a NULL -> NOT EXISTS
--   Query   : SELECT d.name FROM departments d LEFT JOIN employees e ON e.department_id = d.id
--             WHERE e.id IS NULL                                    -> SECURITY
--   Same    : WHERE NOT EXISTS (SELECT 1 FROM employees e WHERE e.department_id = d.id)
--   Counts  : COUNT(e.id) with the LEFT JOIN -> SECURITY 0 (COUNT(*) wrongly gives 1)
--   Trap 2  : a condition on employees in WHERE turns the LEFT JOIN into an INNER JOIN, put it in ON
--   Trap 3  : NOT IN (..., NULL) returns nothing -> prefer NOT EXISTS
--   30-second answer: "LEFT JOIN departments to employees so every department stays, then keep the rows
--             where the employee side is NULL. NOT EXISTS works too, NOT IN is risky with NULLs."
-- QUICK REVISION END
