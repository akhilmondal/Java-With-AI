/* ============================================================================
   Q02  Highest salary per department                              (PostgreSQL)

   THE QUESTION
     "Show the highest salary in each department." Then: "and WHO earns it?"

   THE DATA (employees with their department)
     PAYMENTS : Meera 90000, Priya 80000, Rahul 40000
     BILLING  : Sneha 90000, Karan 60000, Amit 30000
     UI       : Neha 75000,  Vikram 70000
     SECURITY : nobody

   EXPECTED
     department  highest   who
     BILLING      90000    Sneha
     PAYMENTS     90000    Meera
     UI           75000    Neha

   TRY IT YOURSELF FIRST (10 minutes). Then scroll down.
   ============================================================================ */




































-- ============================================================================
-- SOLUTION 1: GROUP BY + MAX (just the amount)
-- ============================================================================
SELECT d.name        AS department,
       MAX(e.salary) AS highest_salary
FROM employees e
JOIN departments d ON d.id = e.department_id
GROUP BY d.name
ORDER BY d.name;
-- Result:
--   department  highest_salary
--   BILLING     90000
--   PAYMENTS    90000
--   UI          75000
-- SECURITY is missing: an INNER JOIN drops departments with no employees (Q06).


-- THE COMMON MISTAKE: adding the name to a GROUP BY query.
-- SELECT department_id, name, MAX(salary) FROM employees GROUP BY department_id
-- PostgreSQL error: column "employees.name" must appear in the GROUP BY clause
--                   or be used in an aggregate function
-- A department's pile has 3 names, and the database can't know which one you mean.
-- (Old MySQL settings would silently return some random name, which is even worse.)


-- ============================================================================
-- SOLUTION 2: a window function (who earns it, the best general answer)
-- ============================================================================
-- Rank people inside each department (PARTITION BY), then keep rank 1.
-- DENSE_RANK keeps BOTH people if two share the top salary in one department.
SELECT department, name, salary
FROM (
    SELECT d.name AS department,
           e.name,
           e.salary,
           DENSE_RANK() OVER (PARTITION BY e.department_id ORDER BY e.salary DESC) AS rank_in_dept
    FROM employees e
    JOIN departments d ON d.id = e.department_id
) ranked
WHERE rank_in_dept = 1
ORDER BY department;
-- Result:
--   department  name   salary
--   BILLING     Sneha  90000
--   PAYMENTS    Meera  90000
--   UI          Neha   75000


-- ============================================================================
-- SOLUTION 3: compare each person with their department's max (a correlated subquery)
-- ============================================================================
-- For each employee e, the inner query finds the max of e's own department.
SELECT d.name AS department, e.name, e.salary
FROM employees e
JOIN departments d ON d.id = e.department_id
WHERE e.salary = (SELECT MAX(e2.salary)
                  FROM employees e2
                  WHERE e2.department_id = e.department_id)
ORDER BY department;
-- Result: the same 3 rows as Solution 2.


-- ============================================================================
-- SOLUTION 4 (PostgreSQL only): DISTINCT ON
-- ============================================================================
-- Keeps the FIRST row of each department in the ORDER BY order.
-- It's short, but on a tie it keeps only one person.
SELECT DISTINCT ON (d.name)
       d.name AS department, e.name, e.salary
FROM employees e
JOIN departments d ON d.id = e.department_id
ORDER BY d.name, e.salary DESC;
-- Result: the same 3 rows.


/* ============================================================================
   HOW TO EXPLAIN IT IN THE INTERVIEW (your own words, cover these points)
     1. For just the amount: GROUP BY department with MAX(salary).
     2. For WHO earns it, a plain GROUP BY can't return the name, so I use
        DENSE_RANK() OVER (PARTITION BY department ORDER BY salary DESC)
        and keep rank 1. That also keeps ties.
     3. Other ways: a correlated subquery comparing with the department's
        MAX, or DISTINCT ON in PostgreSQL.

   Here's how it can sound:
     "If they only want the amount, GROUP BY department and MAX(salary). If they
      want the person, I rank employees within each department using
      DENSE_RANK with PARTITION BY department ordered by salary descending,
      then filter rank 1 in an outer query. That handles ties by returning both
      people. PARTITION BY is what restarts the ranking for every department."

   FOLLOW-UPS
     Q: The top 2 earners per department?
     A: The same query with rank_in_dept <= 2. Use ROW_NUMBER if you want
        exactly 2 rows even with ties.
     Q: What if a department has no employees and must still appear?
     A: Start FROM departments and LEFT JOIN employees (Q06).
     Q: The second highest per department?
     A: The same query with rank_in_dept = 2 (Q01 + PARTITION BY).

   SELF-CHECK (answers at the very bottom)
     1. Why can't you SELECT name with GROUP BY department_id?
     2. Which clause restarts the ranking for each department?
     3. If Karan also earned 90000, what would Solution 2 return for BILLING?
     4. Why is SECURITY missing from all the results?
   ============================================================================ */

-- Answers: 1) each group has several names, name must be grouped or aggregated
--          2) PARTITION BY   3) both Sneha and Karan (DENSE_RANK 1 for both)
--          4) the INNER JOIN drops departments with no matching employees

-- QUICK REVISION START
-- Q02 Highest salary per department
--   Amount only : SELECT d.name, MAX(e.salary) FROM employees e JOIN departments d ON d.id = e.department_id
--                 GROUP BY d.name                         -> BILLING 90000, PAYMENTS 90000, UI 75000
--   Who earns it: DENSE_RANK() OVER (PARTITION BY department_id ORDER BY salary DESC) in a subquery,
--                 keep rank 1                              -> Sneha, Meera, Neha (ties would show both)
--   Also        : WHERE salary = (SELECT MAX(salary) ... same department) | PostgreSQL: DISTINCT ON (dept)
--   Trap        : SELECT name with GROUP BY department_id -> error (a group has many names)
--   30-second answer: "GROUP BY department with MAX for the amount. For the person, rank inside each
--                 department with DENSE_RANK and PARTITION BY, then keep rank 1, that keeps ties too."
-- QUICK REVISION END
