/* ============================================================================
   Q07 (bonus)  Indexes, ACID and transaction isolation levels     (PostgreSQL)

   THE QUESTIONS
     "What is an index? When does it help, and when does it hurt?"
     "What is ACID?"  "What are isolation levels?"  "How do you stop two
     payments from debiting the same balance at the same time?"
   These are the usual follow-ups after the six query problems.

   THE STORY (why each tool exists)
     Pain: finding one payment among 1 lakh rows means reading every row
       -> Fix: an INDEX, a sorted B-tree, like the index at the back of a book
     Pain: a transfer crashes after the debit but before the credit,
           and Rs 300 vanishes
       -> Fix: a TRANSACTION with ACID: all or nothing
     Pain: running transactions one at a time is safe but far too slow, and
           running them together lets them see each other's half-done work
       -> Fix: ISOLATION LEVELS, so you choose how much safety you pay for
     Pain: two debits read the same balance of 700, and one update is lost
       -> Fix: an atomic UPDATE, SELECT ... FOR UPDATE, or a version column

   HOW TO RUN
     Paste Q00_setup.sql into db-fiddle's left box and this file into the right
     box. Parts marked "PostgreSQL only" won't run on other databases.
     If db-fiddle complains about BEGIN or COMMIT, delete those lines, the
     other statements still show the idea.
   ============================================================================ */


-- ============================================================================
-- PART A: INDEXES
-- ============================================================================
-- Real-life picture: the index at the back of a textbook. Without it, you
-- read all 500 pages to find "HashMap", with it, you jump to page 213.
-- Without an index, the database reads every row: a "Seq Scan" (sequential scan).
-- With a B-tree index it plays "higher or lower" (J01) and finds the row in a
-- few steps.

-- PostgreSQL only: build a 100,000-row payments table with generate_series.
CREATE TABLE payments AS
SELECT g                                                     AS id,
       'TXN' || g                                            AS txn_id,
       (g % 5000) + 100                                      AS amount,
       CASE WHEN g % 10 = 0 THEN 'FAILED' ELSE 'SUCCESS' END AS status
FROM generate_series(1, 100000) AS g;
ANALYZE payments;                     -- lets the planner know how big the table is

-- PostgreSQL only: look for "Seq Scan on payments" in this plan (it reads everything).
EXPLAIN SELECT * FROM payments WHERE txn_id = 'TXN77777';

CREATE INDEX idx_payments_txn_id ON payments (txn_id);

-- PostgreSQL only: now look for "Index Scan using idx_payments_txn_id" (it jumps straight there).
EXPLAIN SELECT * FROM payments WHERE txn_id = 'TXN77777';

/* COMPOSITE INDEX and the "leftmost column" rule
   An index on (status, amount) is like a phone directory sorted by surname,
   then first name:
     - "all Sharmas"              -> easy (the first column)
     - "Sharma, Rahul"            -> easy (the first + second columns)
     - "everyone named Rahul"     -> hard: first names are scattered across
                                     every surname, so the index barely helps
   So an index on (status, amount) helps  WHERE status = 'FAILED'
                                     and  WHERE status = 'FAILED' AND amount > 4000
   but usually NOT                        WHERE amount > 4000   (it skips the first column).

   WHEN AN INDEX HURTS
     - Every INSERT/UPDATE/DELETE must update the index too, so writes get slower.
     - It takes extra disk space.
     - A column with very few different values (status: 2 values here) often
       isn't worth indexing alone: the database would still read a big part of
       the table.
   INDEX THESE: primary keys (automatic), foreign keys you join on, and columns
   you search by, such as txn_id or email (UNIQUE indexes also stop duplicates, Q04). */


-- ============================================================================
-- PART B: ACID, a bank transfer
-- ============================================================================
-- A = Atomicity   : all or nothing. Debit AND credit, or neither.
-- C = Consistency : the rules always hold (here: balance can never go below 0).
-- I = Isolation   : two transfers at the same time don't see each other's half-done work.
-- D = Durability  : once COMMIT returns, the change survives a crash or power cut.
-- Real-life picture: a branch transfer slip. Both ledger lines are written, or
-- the slip is torn up, the rules are checked, other cashiers don't see a
-- half-written slip, and once the receipt is stamped, it's final.

CREATE TABLE accounts (
    id       INT PRIMARY KEY,
    owner    VARCHAR(50) NOT NULL,
    balance  INT NOT NULL CHECK (balance >= 0),     -- Consistency: no negative balance
    version  INT NOT NULL DEFAULT 0                 -- used for optimistic locking (Part C)
);
INSERT INTO accounts (id, owner, balance) VALUES (1, 'Rahul', 1000), (2, 'Priya', 500);

-- Transfer 300 from Rahul to Priya, all or nothing (Atomicity).
BEGIN;
UPDATE accounts SET balance = balance - 300 WHERE id = 1;
UPDATE accounts SET balance = balance + 300 WHERE id = 2;
COMMIT;

SELECT id, owner, balance FROM accounts ORDER BY id;
-- Result:
--   id  owner  balance
--   1   Rahul  700
--   2   Priya  800        (the total is still 1500)

-- Changed your mind in the middle? ROLLBACK undoes the whole transaction.
BEGIN;
UPDATE accounts SET balance = balance - 700 WHERE id = 1;     -- Rahul would go to 0
ROLLBACK;

SELECT id, owner, balance FROM accounts ORDER BY id;
-- Result: still Rahul 700, Priya 800

-- Consistency in action (commented out, because the error would stop the rest of the file):
-- UPDATE accounts SET balance = balance - 5000 WHERE id = 1
-- ERROR: new row for relation "accounts" violates check constraint "accounts_balance_check"


-- ============================================================================
-- PART C: ISOLATION LEVELS, and stopping a double debit
-- ============================================================================
/* Isolation levels answer the question: while my transaction runs, how much of
   OTHER transactions' work can I see? Each problem below is shown as a timeline
   with two sessions, A and B, working on Rahul's balance of 700.

   1. DIRTY READ: reading work that is not committed yet
        A: UPDATE balance to 100 (not committed yet)
        B: reads 100                  <- a "dirty" value
        A: ROLLBACK                   <- B used a balance that never existed
   2. NON-REPEATABLE READ: the same row, read twice, gives two answers
        A: reads 700
        B: UPDATE to 400, COMMIT
        A: reads again -> 400         <- changed in the middle of A's work
   3. PHANTOM READ: the same query, run twice, finds new rows
        A: SELECT COUNT(*) FROM payments WHERE status = 'FAILED'  -> 10000
        B: INSERT a FAILED payment, COMMIT
        A: the same count -> 10001    <- a "phantom" row appeared

   Level                          dirty read   non-repeatable read   phantom read
   READ UNCOMMITTED               possible*    possible              possible
   READ COMMITTED (PG default)    no           possible              possible
   REPEATABLE READ                no           no                    possible**
   SERIALIZABLE                   no           no                    no
   *  PostgreSQL never allows dirty reads, READ UNCOMMITTED acts like READ COMMITTED.
   ** by the SQL standard, PostgreSQL's REPEATABLE READ also blocks phantoms.
   Stricter levels are safer but slower: more waiting, more retries.

   THE PAYMENTS PROBLEM: a LOST UPDATE (double debit)
     A: reads balance 700               B: reads balance 700
     A: writes 700 - 100 = 600          B: writes 700 - 200 = 500
     Final balance: 500, but the right answer is 400. A's debit is lost.

   THREE FIXES
     1. Let the database do the math in ONE statement (atomic):
          UPDATE accounts SET balance = balance - 100 WHERE id = 1 AND balance >= 100
     2. Pessimistic lock: lock the row while you work on it (PostgreSQL):
          BEGIN
          SELECT balance FROM accounts WHERE id = 1 FOR UPDATE   -- others wait here
          UPDATE accounts SET balance = balance - 100 WHERE id = 1
          COMMIT
     3. Optimistic lock: a version number. Update only if nobody changed the row
        since you read it. In Spring/JPA, this is the @Version annotation. */

-- Optimistic locking, step by step. Both sessions read Rahul's row at version 0.
-- Session A updates first, with "AND version = 0", and succeeds:
UPDATE accounts SET balance = balance - 100, version = version + 1
WHERE id = 1 AND version = 0;
-- 1 row updated -> balance 600, version 1

-- Session B also read version 0, so its update now matches NOTHING:
UPDATE accounts SET balance = balance - 200, version = version + 1
WHERE id = 1 AND version = 0;
-- 0 rows updated -> B must re-read the row and retry. No debit is lost.

SELECT id, owner, balance, version FROM accounts ORDER BY id;
-- Result:
--   id  owner  balance  version
--   1   Rahul  600      1
--   2   Priya  800      0

-- PostgreSQL only: how to choose a level for one transaction:
BEGIN ISOLATION LEVEL REPEATABLE READ;
SELECT balance FROM accounts WHERE id = 1;
COMMIT;


/* ============================================================================
   HOW TO EXPLAIN IT IN THE INTERVIEW (your own words, cover these points)
     Indexes:
       1. An index is like a book's index: a sorted B-tree, so a lookup takes
          a few steps instead of reading every row. EXPLAIN shows Seq Scan vs
          Index Scan.
       2. A composite index follows the leftmost-column rule.
       3. Indexes cost write speed and space. Index the columns you search and join on.
     ACID:
       4. Atomicity (all or nothing), Consistency (rules hold), Isolation
          (no seeing half-done work), Durability (committed = permanent).
     Isolation:
       5. The levels trade safety for speed: READ COMMITTED (PostgreSQL's
          default), REPEATABLE READ, SERIALIZABLE. They block dirty,
          non-repeatable and phantom reads step by step.
       6. For payments, stop lost updates with an atomic UPDATE,
          SELECT ... FOR UPDATE, or optimistic locking (@Version).

   Here's how it can sound:
     "An index is a sorted structure, usually a B-tree, that lets the database
      find rows without scanning the whole table, like a book's index. I'd add
      one on columns I filter or join on, like txn_id, and check with EXPLAIN
      that it's used. Indexes slow down writes, so I don't add them everywhere.
      ACID means a transaction is all or nothing, keeps the data valid, is
      isolated from other transactions, and is durable once committed. For
      something like debiting a wallet, the classic bug is a lost update, where
      two requests read the same balance. I avoid it with a single atomic
      UPDATE, or by locking the row with SELECT FOR UPDATE, or with optimistic
      locking using a version column, which is @Version in JPA."

   FOLLOW-UPS
     Q: Clustered vs non-clustered index?
     A: A clustered index stores the table rows themselves in index order (MySQL
        InnoDB's primary key). PostgreSQL keeps rows separate (a "heap"), so every
        PostgreSQL index is non-clustered. The CLUSTER command reorders rows once.
     Q: Why might the database ignore my index?
     A: On a small table, or when the query matches a large part of the rows, a
        Seq Scan is cheaper. Wrapping the column in a function, like
        WHERE LOWER(email) = ..., also skips a normal index.
     Q: What does @Transactional have to do with this?
     A: Spring's @Transactional runs your method inside BEGIN ... COMMIT, or
        ROLLBACK on an unchecked exception (J07, B05). You can set
        isolation = Isolation.REPEATABLE_READ on it.
     Q: Optimistic or pessimistic locking?
     A: Optimistic (a version column) when conflicts are rare: no waiting, just
        retry on conflict. Pessimistic (FOR UPDATE) when conflicts are common,
        or a retry is expensive.

   SELF-CHECK (answers at the very bottom)
     1. An index exists on (status, amount). Does WHERE amount > 4000 use it well?
     2. Rahul has 700. Two sessions both read 700, then write 700-100 and 700-200. What's the final balance, and what should it be?
     3. What is PostgreSQL's default isolation level?
     4. With optimistic locking, what happens to the second update that still uses version = 0?
   ============================================================================ */

-- Answers: 1) usually not: amount isn't the leftmost column
--          2) 500 (the last write wins), it should be 400. That's a lost update
--          3) READ COMMITTED
--          4) it updates 0 rows, so the app re-reads the row and retries

-- QUICK REVISION START
-- Q07 Indexes, ACID, isolation
--   Story     : reading every row is slow -> index | a crash mid-transfer loses money -> ACID transaction |
--               one at a time is too slow, all together is unsafe -> isolation levels |
--               two debits read the same 700 -> atomic UPDATE, FOR UPDATE or @Version
--   Index     : a sorted B-tree, like a book's index -> EXPLAIN shows "Index Scan" instead of "Seq Scan".
--               Composite (status, amount) helps "status = ?" and "status = ? AND amount > ?",
--               not "amount > ?" alone (the leftmost-column rule). Costs: slower writes, more disk.
--   ACID      : Atomicity (all or nothing) | Consistency (rules like balance >= 0 hold) |
--               Isolation (no half-done work seen) | Durability (committed = survives a crash)
--   Levels    : READ UNCOMMITTED < READ COMMITTED (PostgreSQL default) < REPEATABLE READ < SERIALIZABLE
--               they stop dirty reads, then non-repeatable reads, then phantoms
--   Lost update (double debit): both read 700, write 700-100 and 700-200 -> 500 instead of 400
--   Fixes     : UPDATE ... SET balance = balance - 100 (atomic) | SELECT ... FOR UPDATE (lock) |
--               a version column (optimistic, JPA @Version) -> the second update matches 0 rows, so retry
--   30-second answer: "An index is a sorted B-tree so lookups skip the full scan, I index the columns I
--               filter and join on and check EXPLAIN. ACID keeps transactions all-or-nothing, valid,
--               isolated and durable. For wallet debits I prevent lost updates with an atomic UPDATE,
--               SELECT FOR UPDATE, or optimistic locking with @Version."
-- QUICK REVISION END
