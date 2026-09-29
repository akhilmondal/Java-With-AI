/* ============================================================================
   Q04  Duplicate emails with GROUP BY and HAVING                  (PostgreSQL)

   THE QUESTION
     "Find the emails that appear more than once." Then: "show those rows",
     and "delete the duplicates but keep one of each".

   THE DATA (customers: sign-ups for a payment app)
     id  email
     1   ravi@mail.com
     2   anu@mail.com
     3   ravi@mail.com
     4   kiran@mail.com
     5   anu@mail.com
     6   ravi@mail.com

   EXPECTED
     email          times
     anu@mail.com   2
     ravi@mail.com  3

   TRY IT YOURSELF FIRST (10 minutes). Then scroll down.
   ============================================================================ */




































-- ============================================================================
-- WHY THESE TOOLS EXIST (the story, read it after your own try)
-- ============================================================================
-- Pain 1: you want the emails that appear more than once. WHERE COUNT(*) > 1
--         is an error, because WHERE runs before the piles (and their counts)
--         exist.
-- Fix 1 : GROUP BY email, then HAVING COUNT(*) > 1. HAVING exists for exactly
--         this: it filters the piles after they're counted.
-- Pain 2: GROUP BY shows each email once, but to delete the extra copies
--         you need their ids.
-- Fix 2 : number the copies with ROW_NUMBER() OVER (PARTITION BY email ...),
--         or keep MIN(id) per email and delete the rest (follow-ups 2 and 3).
-- Pain 3: cleaning up once doesn't stop new duplicates tomorrow.
-- Fix 3 : a UNIQUE constraint, so the database itself refuses them.


-- ============================================================================
-- SOLUTION: GROUP BY email, HAVING COUNT(*) > 1
-- ============================================================================
-- Make one pile per email, count each pile, keep the piles with more than 1.
SELECT email, COUNT(*) AS times
FROM customers
GROUP BY email
HAVING COUNT(*) > 1
ORDER BY email;
-- Result:
--   email          times
--   anu@mail.com   2
--   ravi@mail.com  3


-- THE COMMON MISTAKE: filtering the count in WHERE.
-- SELECT email FROM customers WHERE COUNT(*) > 1 GROUP BY email
-- ERROR: aggregate functions are not allowed in WHERE
-- WHERE runs BEFORE the piles exist (Q00 Step 1), so the count must go in HAVING.


-- ============================================================================
-- FOLLOW-UP 1: show every duplicate row, with its id
-- ============================================================================
SELECT id, email
FROM customers
WHERE email IN (SELECT email
                FROM customers
                GROUP BY email
                HAVING COUNT(*) > 1)
ORDER BY email, id;
-- Result:
--   id  email
--   2   anu@mail.com
--   5   anu@mail.com
--   1   ravi@mail.com
--   3   ravi@mail.com
--   6   ravi@mail.com


-- ============================================================================
-- FOLLOW-UP 2: which rows are the EXTRA copies (keep the lowest id of each email)?
-- ============================================================================
-- ROW_NUMBER numbers the copies of each email: 1 for the first id, 2 for the next...
-- Every row numbered above 1 is an extra copy.
SELECT id, email
FROM (
    SELECT id, email,
           ROW_NUMBER() OVER (PARTITION BY email ORDER BY id) AS copy_number
    FROM customers
) numbered
WHERE copy_number > 1
ORDER BY id;
-- Result:
--   id  email
--   3   ravi@mail.com
--   5   anu@mail.com
--   6   ravi@mail.com


-- ============================================================================
-- FOLLOW-UP 3: delete the extra copies (commented out so the data stays for the queries above)
-- ============================================================================
-- Keep the smallest id of each email, and delete every other row:
--
-- DELETE FROM customers
-- WHERE id NOT IN (SELECT MIN(id) FROM customers GROUP BY email)
--
-- It deletes ids 3, 5 and 6. Left: 1 ravi, 2 anu, 4 kiran.
--
-- PostgreSQL also has a join-style delete that does the same thing:
-- DELETE FROM customers a USING customers b
-- WHERE a.email = b.email AND a.id > b.id
--
-- To stop duplicates for good: ALTER TABLE customers ADD CONSTRAINT uq_customers_email UNIQUE (email)
-- (It only works after the duplicates are gone.)


/* ============================================================================
   HOW TO EXPLAIN IT IN THE INTERVIEW (your own words, cover these points)
     1. GROUP BY email makes one group per email, COUNT(*) counts each group.
     2. HAVING COUNT(*) > 1 keeps only the duplicates. It must be HAVING, not
        WHERE, because WHERE runs before grouping.
     3. To see the rows: WHERE email IN (that query). To find the extra copies:
        ROW_NUMBER() OVER (PARTITION BY email ORDER BY id) > 1.
     4. To delete: keep MIN(id) per email and delete the rest. Then add a UNIQUE
        constraint so it can't happen again.

   Here's how it can sound:
     "I group the customers by email and count each group, then use HAVING
      COUNT(*) > 1 to keep only the emails that repeat. Here ravi appears 3
      times and anu twice. It has to be HAVING, not WHERE, because WHERE filters
      rows before the grouping happens. If they want to delete duplicates, I
      keep the lowest id per email and delete the others, and then add a unique
      constraint on email."

   FOLLOW-UPS
     Q: Case differences, like Ravi@Mail.com vs ravi@mail.com?
     A: Group by LOWER(email). A unique index on LOWER(email) blocks them in future.
     Q: Duplicates in a payments table, like the same gateway callback saved twice?
     A: The same idea: GROUP BY txn_id HAVING COUNT(*) > 1. Prevent it with a
        UNIQUE constraint on txn_id, which is an idempotency guard (topic M07).

   SELF-CHECK (answers at the very bottom)
     1. How many rows does the main query return?
     2. Why does "WHERE COUNT(*) > 1" fail?
     3. Which ids does the delete remove?
     4. What stops new duplicates in future?
   ============================================================================ */

-- Answers: 1) 2 (anu 2, ravi 3)   2) WHERE runs before GROUP BY, aggregates go in HAVING
--          3) 3, 5, 6              4) a UNIQUE constraint (or unique index) on email

-- QUICK REVISION START
-- Q04 Duplicate emails
--   Story   : WHERE can't filter on COUNT -> HAVING filters piles -> GROUP BY hides the extra rows' ids ->
--             ROW_NUMBER or MIN(id) to delete them -> a UNIQUE constraint stops new ones
--   Find    : SELECT email, COUNT(*) FROM customers GROUP BY email HAVING COUNT(*) > 1
--             -> anu@mail.com 2, ravi@mail.com 3
--   Extras  : ROW_NUMBER() OVER (PARTITION BY email ORDER BY id) > 1 -> ids 3, 5, 6 are the extra copies
--   Delete  : DELETE FROM customers WHERE id NOT IN (SELECT MIN(id) FROM customers GROUP BY email)
--             leaves 1 ravi, 2 anu, 4 kiran
--   Prevent : a UNIQUE constraint on email (for payments: UNIQUE on txn_id = idempotency guard)
--   Trap    : WHERE COUNT(*) > 1 -> error, WHERE runs before GROUP BY, so use HAVING
--   30-second answer: "GROUP BY email, HAVING COUNT(*) > 1. HAVING, not WHERE, because WHERE runs
--             before grouping. To clean up, keep MIN(id) per email, delete the rest, add a unique constraint."
-- QUICK REVISION END
