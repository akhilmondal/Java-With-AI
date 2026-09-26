/* ============================================================================
   Q01  Nth highest salary                                         (PostgreSQL)

   THE QUESTION
     "Find the 2nd highest salary."  Then usually: "Now the Nth.", "Who earns it?",
     and "What if there's no Nth salary?"

   THE DATA (employees, highest first)
     name     salary
     Meera     90000
     Sneha     90000    <- a TIE at the top: this is what breaks wrong answers
     Priya     80000
     Neha      75000
     Vikram    70000
     Karan     60000
     Rahul     40000
     Amit      30000

   EXPECTED
     2nd highest salary = 80000 (Priya)
     3rd highest salary = 75000 (Neha)

   TRY IT YOURSELF FIRST (10 minutes)
     Write it twice: once with DENSE_RANK() and once with LIMIT/OFFSET.
     Then scroll down.
   ============================================================================ */




































-- ============================================================================
-- SOLUTION 1: DENSE_RANK (the best one to write in an interview)
-- ============================================================================
-- DENSE_RANK numbers the salaries from the top: equal salaries share a number,
-- and there are no gaps. 90000 -> 1, 80000 -> 2, 75000 -> 3, ...
-- A window function can't go in WHERE (it runs later, see Q00 Step 1),
-- so we rank inside a subquery and filter outside it.
SELECT DISTINCT salary
FROM (
    SELECT salary,
           DENSE_RANK() OVER (ORDER BY salary DESC) AS salary_rank
    FROM employees
) ranked
WHERE salary_rank = 2;          -- change 2 to N for the Nth highest
-- Result:
--   salary
--   80000


-- Who earns the 2nd highest salary? Same idea, keep the name.
SELECT name, salary
FROM (
    SELECT name, salary,
           DENSE_RANK() OVER (ORDER BY salary DESC) AS salary_rank
    FROM employees
) ranked
WHERE salary_rank = 2;
-- Result:
--   name   salary
--   Priya  80000


-- ============================================================================
-- SOLUTION 2: DISTINCT + ORDER BY + LIMIT/OFFSET
-- ============================================================================
-- Sort the DIFFERENT salaries from the top, skip the first one, take one.
-- For the Nth highest: OFFSET N-1.
SELECT DISTINCT salary
FROM employees
ORDER BY salary DESC
LIMIT 1 OFFSET 1;
-- Result:
--   salary
--   80000


-- THE TRAP: forget DISTINCT and the tie at the top gives the wrong answer.
SELECT salary
FROM employees
ORDER BY salary DESC
LIMIT 1 OFFSET 1;
-- Result:
--   salary
--   90000       <- WRONG: that's Sneha's 90000, the same as the highest


-- ============================================================================
-- SOLUTION 3: count the bigger salaries (works on any database, no window functions)
-- ============================================================================
-- The 2nd highest salary is the one with exactly 1 different salary above it.
-- For the Nth highest: exactly N-1 different salaries above it.
SELECT DISTINCT e1.salary
FROM employees e1
WHERE 1 = (SELECT COUNT(DISTINCT e2.salary)
           FROM employees e2
           WHERE e2.salary > e1.salary);
-- Result:
--   salary
--   80000


-- ============================================================================
-- WHY DENSE_RANK, AND NOT RANK OR ROW_NUMBER? See all three side by side:
-- ============================================================================
SELECT name, salary,
       ROW_NUMBER() OVER (ORDER BY salary DESC, name) AS row_num,
       RANK()       OVER (ORDER BY salary DESC)       AS rank_num,
       DENSE_RANK() OVER (ORDER BY salary DESC)       AS dense_rank_num
FROM employees
ORDER BY salary DESC, name;
-- Result:
--   name    salary  row_num  rank_num  dense_rank_num
--   Meera    90000     1        1          1
--   Sneha    90000     2        1          1
--   Priya    80000     3        3          2
--   Neha     75000     4        4          3
--   Vikram   70000     5        5          4
--   Karan    60000     6        6          5
--   Rahul    40000     7        7          6
--   Amit     30000     8        8          7
-- rank_num = 2 matches NO row (RANK skips 2 after the tie).
-- row_num  = 2 is Sneha with 90000 (wrong: that's the highest salary again).
-- dense_rank_num = 2 is Priya with 80000 (right).


-- ============================================================================
-- EDGE CASE: there is no Nth salary
-- ============================================================================
-- There are only 7 different salaries, so the 8th doesn't exist.
-- Wrapping the query in (SELECT ...) returns NULL instead of "no rows",
-- which is what the LeetCode version of this question expects.
SELECT (SELECT DISTINCT salary
        FROM employees
        ORDER BY salary DESC
        LIMIT 1 OFFSET 7) AS eighth_highest;
-- Result:
--   eighth_highest
--   NULL


/* ============================================================================
   HOW TO EXPLAIN IT IN THE INTERVIEW (your own words; cover these points)
     1. I rank the salaries from the top with DENSE_RANK, because ties share
        a rank with no gaps, and then I keep rank N.
     2. The window function runs after WHERE, so I rank in a subquery (or a CTE)
        and filter outside it.
     3. Another way: SELECT DISTINCT salary ... ORDER BY salary DESC LIMIT 1 OFFSET N-1.
        DISTINCT matters when salaries tie.
     4. RANK would skip numbers after a tie, and ROW_NUMBER would count the
        tied 90000 twice, so both give wrong answers here.

   Here's how it can sound:
     "I'd use DENSE_RANK over salary descending in a subquery, then pick rank 2.
      DENSE_RANK gives equal salaries the same rank without gaps, so with two
      people at 90000, 80000 is correctly rank 2. RANK would jump to 3 and
      ROW_NUMBER would treat the second 90000 as number 2. Without window
      functions I'd do SELECT DISTINCT salary, ORDER BY salary DESC,
      LIMIT 1 OFFSET 1, and the DISTINCT is what protects against ties."

   FOLLOW-UPS
     Q: Nth highest salary PER DEPARTMENT?
     A: Add PARTITION BY department_id inside OVER(...). That's Q02's pattern.
     Q: Which is faster?
     A: With an index on salary, LIMIT/OFFSET can stop early. DENSE_RANK has to
        rank every row. For a small table it doesn't matter; say that you'd check
        EXPLAIN (Q07).
     Q: FETCH FIRST?
     A: OFFSET 1 ROWS FETCH FIRST 1 ROW ONLY is the SQL-standard spelling of
        LIMIT 1 OFFSET 1, and PostgreSQL supports both.

   SELF-CHECK (answers at the very bottom)
     1. What is the 3rd highest salary, and who earns it?
     2. What does RANK() = 2 return with this data, and why?
     3. What's the OFFSET for the 5th highest salary?
     4. Why does the LIMIT/OFFSET version need DISTINCT?
   ============================================================================ */

-- Answers: 1) 75000, Neha   2) no rows, because RANK goes 1, 1, 3 after the tie
--          3) OFFSET 4       4) without it, the tied 90000 fills position 2
