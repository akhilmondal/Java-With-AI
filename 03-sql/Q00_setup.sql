/* ============================================================================
   Q00  Setup: the tables and rows that every SQL problem uses     (PostgreSQL)

   HOW TO RUN (you don't need Postgres on your laptop)
     1. Open https://www.db-fiddle.com and pick PostgreSQL (any recent version).
     2. Paste THIS whole file into the left box ("Schema SQL").
     3. Paste one problem file (Q01 ... Q07) into the right box ("Query SQL").
     4. Click Run. The result of each query appears below.
   Read Q00_sql_toolkit.md first: it explains the ideas the problems use.
   ============================================================================ */

DROP TABLE IF EXISTS employees;
DROP TABLE IF EXISTS departments;
DROP TABLE IF EXISTS customers;

CREATE TABLE departments (
    id    INT PRIMARY KEY,
    name  VARCHAR(50) NOT NULL
);

CREATE TABLE employees (
    id             INT PRIMARY KEY,
    name           VARCHAR(50) NOT NULL,
    salary         INT NOT NULL,
    department_id  INT REFERENCES departments (id),
    manager_id     INT REFERENCES employees (id)    -- points to another employee (used in Q05)
);

-- Sign-ups for a payment app. Some people registered twice (used in Q04).
CREATE TABLE customers (
    id     INT PRIMARY KEY,
    email  VARCHAR(100) NOT NULL
);

INSERT INTO departments (id, name) VALUES
    (1, 'PAYMENTS'),
    (2, 'BILLING'),
    (3, 'UI'),
    (4, 'SECURITY');                     -- nobody works here yet (used in Q06)

INSERT INTO employees (id, name, salary, department_id, manager_id) VALUES
    (1, 'Meera',  90000, 1, NULL),       -- the head: she has no manager
    (2, 'Rahul',  40000, 1, 1),
    (3, 'Priya',  80000, 1, 1),
    (4, 'Sneha',  90000, 2, 1),          -- same salary as Meera: a tie at the top (used in Q01)
    (5, 'Amit',   30000, 2, 4),
    (6, 'Karan',  60000, 2, 4),
    (7, 'Vikram', 70000, 3, 1),
    (8, 'Neha',   75000, 3, 7);          -- earns more than her manager Vikram (used in Q05)

INSERT INTO customers (id, email) VALUES
    (1, 'ravi@mail.com'),
    (2, 'anu@mail.com'),
    (3, 'ravi@mail.com'),
    (4, 'kiran@mail.com'),
    (5, 'anu@mail.com'),
    (6, 'ravi@mail.com');

/* ============================================================================
   HOW THE TABLES CONNECT

     departments (id, name)
          1
          |  department_id
          *
     employees (id, name, salary, department_id, manager_id)
          ^                                          |
          +------------- manager_id (self) ---------+     an employee's manager is another employee

     customers (id, email)                                a separate table, used in Q04

   THE DATA AT A GLANCE

   departments           employees
   id  name              id  name    salary  department    manager
   1   PAYMENTS          1   Meera    90000  1 PAYMENTS    -
   2   BILLING           2   Rahul    40000  1 PAYMENTS    1 Meera
   3   UI                3   Priya    80000  1 PAYMENTS    1 Meera
   4   SECURITY          4   Sneha    90000  2 BILLING     1 Meera
                         5   Amit     30000  2 BILLING     4 Sneha
                         6   Karan    60000  2 BILLING     4 Sneha
   customers             7   Vikram   70000  3 UI          1 Meera
   id  email             8   Neha     75000  3 UI          7 Vikram
   1   ravi@mail.com
   2   anu@mail.com      Per department:
   3   ravi@mail.com       PAYMENTS  3 people, total 210000, average 70000
   4   kiran@mail.com      BILLING   3 people, total 180000, average 60000
   5   anu@mail.com        UI        2 people, total 145000, average 72500
   6   ravi@mail.com       SECURITY  nobody
   ============================================================================ */
