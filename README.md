# Interview Sprint: Topic Tracker

**Interview:** Tue 29 Sep 2026 (Wed 30 Sep if they confirm Wednesday, see Part 9)
**JD:** Full-stack developer, Angular + Java Spring Boot
**Your plan:** [SPRINT-PLAN.md](SPRINT-PLAN.md) · **Your stumbles:** [STUMBLE-LIST.md](STUMBLE-LIST.md)

## How we work

We go one topic at a time:

1. I write the lesson for the next topic: simple words, one small example you can work out in your head.
2. You read the `.md`, then run the `.java` to watch it happen.
3. Close the files and explain it aloud **in your own words** using that example. Don't memorize a script. Add anything you stumble on to STUMBLE-LIST.md.
4. Tick the box below (`[ ]` → `[x]`) and send me `next`.

| Send me | You get |
|---|---|
| `next` | the next topic |
| `grade: <your answer>` | a score and fixes for your spoken answer |
| `quiz me` | 5 quick questions on the topics done so far |
| `again: <anything>` | the same idea explained another way |
| `start mock` | the full mock interview (Monday) |

At the end of each day, send two lines: the boxes you ticked and your weakest topic.

## Project structure

```text
Java With AI/
├── README.md                     you are here: topic list and progress
├── SPRINT-PLAN.md                your original plan (reference copy)
├── STUMBLE-LIST.md               everything you stumble on; reread it on interview day
├── .vscode/settings.json         tells VS Code which folders hold runnable Java
├── .claude/skills/teach-interview-topic/   the teaching style, saved so every topic is taught the same way
│
├── 00-intro-and-hr/              Part 0  intro, why you left, salary, resume audit   .md
├── 01-java-core/                 Part 1  Java core lessons                           .md to read + .java to run
├── 02-java8-streams/             Part 2  streams toolkit, practice, solutions        .java (runnable)
├── 03-sql/                       Part 3  schema and the 6 queries                    .sql  (PostgreSQL)
├── 04-dsa/                       Part 4  3 warm-up problems                          .java (runnable)
├── 05-spring-boot/               Part 5  Spring Boot notes                           .md with Java code
├── 06-microservices/             Part 6  microservices notes                         .md
├── 07-project-stories/           Part 7  your architecture and 5 stories             .md
├── 08-angular/                   Part 8  Angular notes and 2 hands-on builds         .md with TypeScript
└── 09-mock-and-interview-day/    Part 9  mock, questions to ask, checklists          .md
```

**Conventions**, so the project stays tidy as it grows:

- Every file is named by its topic ID (`J01_`, `Q01_`, `B01-`, ...) so files sort in lesson order.
- Java concept lessons come as a pair with the same name: a `.md` to read first and a `.java` to run. Practice problems are a single `.java`.
- Each Java file runs on its own. Helper classes live inside the lesson class, so names never clash across files.
- Every SQL file is PostgreSQL and starts with the problem in comments. Run `Q00_setup.sql` once first.
- For `.md` notes, press `Ctrl+Shift+V` in VS Code to open the preview.

## How to run

- **Java** (JDK 25 is installed): open the VS Code terminal in this folder and run `java 01-java-core/J01_HashMapInternals.java`, or click **Run** above `main()`.
- **SQL**: Postgres isn't installed on this laptop. On db-fiddle.com, choose PostgreSQL, paste `03-sql/Q00_setup.sql` into the left box and a problem file into the right box, then click Run. Each problem file lists the expected result under every query.

---

## Topics

`[x]` means you can explain it aloud without notes. A linked file means the lesson is ready; plain text means it isn't written yet. ⭐ marks topics I added that aren't in your plan (reasons at the bottom).

### Part 0 · Intro and HR answers (Fri 25 Sep, catch up if not done) → `00-intro-and-hr/`

- [ ] **H01** 90-second intro, said aloud 3 times under 90 s and graded by me → `H01-intro-90-seconds.md`
- [ ] **H02** "Why did you leave?", salary numbers (current, expected, private minimum), joining date → `H02-hr-answers.md`
- [ ] **H03** Resume audit: one real example you can defend for every line → `H03-resume-audit.md`
- [ ] **H04** Company research in two lines, and the slot confirmed in writing (date, time, mode, rounds, live coding, JD) → `H04-company-and-slot.md`

### Part 1 · Java core (Sat 26 Sep, 2.5 h) → `01-java-core/`

- [ ] **J01** HashMap internals: hashing, buckets, collisions, treeify, resize → read [J01_HashMapInternals.md](01-java-core/J01_HashMapInternals.md), run [J01_HashMapInternals.java](01-java-core/J01_HashMapInternals.java)
- [ ] **J02** equals/hashCode contract, and what breaks if you override only one → read [J02_EqualsAndHashCode.md](01-java-core/J02_EqualsAndHashCode.md), run [J02_EqualsAndHashCode.java](01-java-core/J02_EqualsAndHashCode.java)
- [ ] **J03** String immutability, string pool, StringBuilder vs StringBuffer → read [J03_StringsAndStringPool.md](01-java-core/J03_StringsAndStringPool.md), run [J03_StringsAndStringPool.java](01-java-core/J03_StringsAndStringPool.java)
- [ ] **J04** ArrayList vs LinkedList; HashMap vs LinkedHashMap vs TreeMap → read [J04_ListsAndMaps.md](01-java-core/J04_ListsAndMaps.md), run [J04_ListsAndMaps.java](01-java-core/J04_ListsAndMaps.java)
- [ ] **J05** ConcurrentHashMap vs Collections.synchronizedMap; volatile vs synchronized → read [J05_ConcurrencyBasics.md](01-java-core/J05_ConcurrencyBasics.md), run [J05_ConcurrencyBasics.java](01-java-core/J05_ConcurrencyBasics.java)
- [ ] **J06** ExecutorService, Future, CompletableFuture → read [J06_ExecutorsAndCompletableFuture.md](01-java-core/J06_ExecutorsAndCompletableFuture.md), run [J06_ExecutorsAndCompletableFuture.java](01-java-core/J06_ExecutorsAndCompletableFuture.java)
- [ ] **J07** Checked vs unchecked exceptions, try-with-resources, custom exceptions → read [J07_Exceptions.md](01-java-core/J07_Exceptions.md), run [J07_Exceptions.java](01-java-core/J07_Exceptions.java)
- [ ] **J08** Comparable vs Comparator; map vs flatMap; intermediate vs terminal operations; Optional → read [J08_ComparatorStreamsOptional.md](01-java-core/J08_ComparatorStreamsOptional.md), run [J08_ComparatorStreamsOptional.java](01-java-core/J08_ComparatorStreamsOptional.java)
- [ ] **J09** JVM memory (heap, stack, metaspace), GC basics, OutOfMemoryError vs StackOverflowError → read [J09_JvmMemoryAndGc.md](01-java-core/J09_JvmMemoryAndGc.md), run [J09_JvmMemoryAndGc.java](01-java-core/J09_JvmMemoryAndGc.java)
- [ ] **J10** SOLID with an example from your own code; interface vs abstract class; immutable class → read [J10_SolidAndImmutability.md](01-java-core/J10_SolidAndImmutability.md), run [J10_SolidAndImmutability.java](01-java-core/J10_SolidAndImmutability.java)
- [ ] ⭐ **J11** Modern Java (8 to 21): lambdas, functional interfaces, records, switch expressions, virtual threads → read [J11_ModernJavaFeatures.md](01-java-core/J11_ModernJavaFeatures.md), run [J11_ModernJavaFeatures.java](01-java-core/J11_ModernJavaFeatures.java)
- [ ] ⭐ **J12** Design patterns: Singleton, Builder, Factory, Strategy → read [J12_DesignPatterns.md](01-java-core/J12_DesignPatterns.md), run [J12_DesignPatterns.java](01-java-core/J12_DesignPatterns.java)

### Part 2 · Java 8 streams (Sat, 2 h) → `02-java8-streams/`

- [ ] **S00** Streams toolkit: groupingBy, counting, partitioningBy, maxBy, toMap, plus a hint sheet → read [S00_StreamsToolkit.md](02-java8-streams/S00_StreamsToolkit.md), run [S00_StreamsToolkit.java](02-java8-streams/S00_StreamsToolkit.java)
- [ ] **S01** Practice the 8 programs yourself, 10 minutes each before looking anything up. It has a built-in checker that shows [DONE] and your score → [S01_StreamPractice.java](02-java8-streams/S01_StreamPractice.java)
  1. Character frequency in a string
  2. First non-repeated character
  3. Duplicate elements in a list
  4. Second-highest number
  5. Employees grouped by department
  6. Highest-paid employee per department
  7. Employees sorted by salary descending, then by name
  8. Numbers partitioned into even and odd
- [ ] **S02** Check against the solutions, only after you've tried. Includes a second way for some problems and the classic mistakes → [S02_StreamSolutions.java](02-java8-streams/S02_StreamSolutions.java)

### Part 3 · SQL in PostgreSQL (Sat, 1.5 h) → `03-sql/`

- [ ] **Q00** SQL toolkit (run order, JOINs, GROUP BY/HAVING, window functions, NULL) plus the setup script → read [Q00_sql_toolkit.md](03-sql/Q00_sql_toolkit.md), run [Q00_setup.sql](03-sql/Q00_setup.sql) first
- [ ] **Q01** Nth highest salary, with DENSE_RANK and with LIMIT/OFFSET → [Q01_nth_highest_salary.sql](03-sql/Q01_nth_highest_salary.sql)
- [ ] **Q02** Highest salary per department → [Q02_highest_salary_per_department.sql](03-sql/Q02_highest_salary_per_department.sql)
- [ ] **Q03** Employees earning above their department average → [Q03_above_department_average.sql](03-sql/Q03_above_department_average.sql)
- [ ] **Q04** Duplicate emails with GROUP BY and HAVING → [Q04_duplicate_emails.sql](03-sql/Q04_duplicate_emails.sql)
- [ ] **Q05** Each employee with their manager's name (self-join) → [Q05_employee_manager_self_join.sql](03-sql/Q05_employee_manager_self_join.sql)
- [ ] **Q06** Departments with zero employees (LEFT JOIN) → [Q06_departments_without_employees.sql](03-sql/Q06_departments_without_employees.sql)
- [ ] ⭐ **Q07** Indexes, ACID, transaction isolation levels → [Q07_indexes_acid_isolation.sql](03-sql/Q07_indexes_acid_isolation.sql)

### Part 4 · DSA warm-up (Sat, 45 min) → `04-dsa/`

- [ ] **D01** Two Sum → [D01_TwoSum.java](04-dsa/D01_TwoSum.java) (your attempt, checked; brute force O(n²) vs HashMap O(n))
- [ ] **D02** Valid Anagram → [D02_ValidAnagram.java](04-dsa/D02_ValidAnagram.java) (your attempt, checked; sorting O(n log n) vs counting O(n))
- [ ] **D03** Longest Substring Without Repeating Characters → [D03_LongestSubstringWithoutRepeating.java](04-dsa/D03_LongestSubstringWithoutRepeating.java) (your attempt, checked; brute force O(n²) vs sliding window O(n))

### Part 5 · Spring Boot (Sun 27 Sep, 2.5 h) → `05-spring-boot/`

- [ ] **B01** What @SpringBootApplication combines; auto-configuration and @Conditional → `B01-auto-configuration.md`
- [ ] **B02** IoC and DI; constructor vs field injection; @Qualifier vs @Primary → `B02-ioc-and-dependency-injection.md`
- [ ] **B03** Bean scopes and lifecycle (@PostConstruct, @PreDestroy) → `B03-bean-scopes-and-lifecycle.md`
- [ ] **B04** @Component vs @Service vs @Repository vs @RestController → `B04-stereotype-annotations.md`
- [ ] **B05** @Transactional: rollback rules, self-invocation, REQUIRED vs REQUIRES_NEW → `B05-transactional.md`
- [ ] **B06** JPA: lazy vs eager, the N+1 problem and its fix, pagination with Pageable → `B06-jpa-n-plus-1-and-pagination.md`
- [ ] **B07** Validation with @Valid; global error handling with @RestControllerAdvice → `B07-validation-and-error-handling.md`
- [ ] **B08** @Value vs @ConfigurationProperties; profiles → `B08-configuration-and-profiles.md`
- [ ] **B09** REST: PUT vs PATCH, idempotent methods, status codes → `B09-rest-api-design.md`
- [ ] **B10** Spring Security with JWT: how a request moves through the filter chain → `B10-spring-security-jwt.md`
- [ ] **B11** Actuator health and metrics → `B11-actuator.md`
- [ ] ⭐ **B12** Testing: JUnit 5, Mockito, @WebMvcTest, @SpringBootTest → `B12-testing.md`

### Part 6 · Microservices (Sun, 2 h) → `06-microservices/`

- [ ] **M01** Monolith vs microservices, and when not to split → `M01-monolith-vs-microservices.md`
- [ ] **M02** Sync calls (REST, Feign, WebClient) vs async messaging (RabbitMQ) → `M02-sync-vs-async-communication.md`
- [ ] **M03** RabbitMQ: exchange types, manual ack, redelivery, dead-letter queue, idempotent consumers → `M03-rabbitmq.md`
- [ ] **M04** API gateway, service discovery, config server → `M04-gateway-discovery-config.md`
- [ ] **M05** Timeouts, retry with backoff, circuit breaker states → `M05-resilience-patterns.md`
- [ ] **M06** Database per service; Saga (choreography vs orchestration); why not 2PC → `M06-database-per-service-and-saga.md`
- [ ] **M07** Idempotency keys on payment APIs → `M07-idempotency-keys.md`
- [ ] **M08** Correlation IDs, centralized logs, distributed tracing → `M08-observability.md`
- [ ] ⭐ **M09** Docker and deployment basics (lowest priority: your JD doesn't mention it) → `M09-docker-basics.md`

### Part 7 · Your system and 5 stories (Sun, 2 h) → `07-project-stories/`

- [ ] **P00** Your BharatNXT architecture: services, queues, Postgres, and the PayU, Setu and BBPS callbacks (also draw it on paper) → `P00-architecture.md`
- [ ] **P01** A BBPS bill payment end to end: fetch, pay, status → `P01-bbps-payment-flow.md`
- [ ] **P02** A duplicate PayU or Setu callback, and how idempotency handles it → `P02-duplicate-callback.md`
- [ ] **P03** A gateway timeout with unknown status: PENDING, status check, reconciliation → `P03-timeout-and-reconciliation.md`
- [ ] **P04** A RabbitMQ consumer crashing mid-message: ack, redelivery, dead-letter queue → `P04-consumer-crash-and-dlq.md`
- [ ] **P05** The hardest production bug you fixed → `P05-hardest-production-bug.md`

### Part 8 · Angular (Mon 28 Sep, 3.5 h) → `08-angular/`

- [ ] **A01** Lifecycle hooks and their order; constructor vs ngOnInit → `A01-lifecycle-hooks.md`
- [ ] **A02** Component communication: @Input/@Output, shared service with BehaviorSubject, @ViewChild → `A02-component-communication.md`
- [ ] **A03** Observable vs Promise; Subject vs BehaviorSubject vs ReplaySubject → `A03-observables-and-subjects.md`
- [ ] **A04** switchMap vs mergeMap vs concatMap vs exhaustMap, with one real use case each → `A04-rxjs-mapping-operators.md`
- [ ] **A05** Stopping subscription leaks: async pipe, takeUntilDestroyed, takeUntil → `A05-unsubscribing.md`
- [ ] **A06** Change detection: Default vs OnPush → `A06-change-detection.md`
- [ ] **A07** DI and providedIn: 'root' → `A07-dependency-injection.md`
- [ ] **A08** Routing: lazy loading, guards, resolvers → `A08-routing-guards-resolvers.md`
- [ ] **A09** HTTP interceptors: attach the JWT and handle 401 in one place → `A09-http-interceptors.md`
- [ ] **A10** Reactive vs template-driven forms; one custom validator → `A10-forms-and-custom-validators.md`
- [ ] **A11** Pure vs impure pipes → `A11-pipes.md`
- [ ] **A12** Recent Angular: standalone components, signals, @if/@for, @defer → `A12-modern-angular.md`
- [ ] **A13** Hands-on: search box with debounceTime, distinctUntilChanged and switchMap → `A13-hands-on-search-box.md`
- [ ] **A14** Hands-on: reactive form with a custom validator → `A14-hands-on-reactive-form.md`
- [ ] ⭐ **A15** JavaScript and TypeScript basics: let/const/var, closures, event loop, `this`, async/await → `A15-javascript-typescript-basics.md`

### Part 9 · Mock and interview day (Mon to Tue) → `09-mock-and-interview-day/`

- [ ] **F01** Full mock interview: send `start mock` (1 h) → results in `F01-mock-results.md`
- [ ] **F02** Redo every weak answer from the mock aloud until it comes out clean (1 h)
- [ ] **F03** Write 2 questions to ask them → `F03-questions-to-ask.md`
- [ ] **F04** Test your setup: laptop charged, camera, mic, internet, meeting app, quiet room, a test call with a friend
- [ ] **F05** Readiness check at 6 PM Monday (below)
- [ ] **F06** Interview-day checklist → `F06-interview-day.md`
- [ ] **F07** Within 30 minutes after: write down every question they asked and send it to me → `F07-questions-they-asked.md`

If the interview moves to Wednesday, Tuesday is for fixing, not new topics: a second mock on your two weakest areas, redo the stream and SQL problems you got wrong, and tell your intro and 5 stories aloud once more.

---

## Readiness check (Monday 6 PM)

Every line should be a yes. Anything still a no gets the time until 9 PM.

- [ ] Intro comes out clean in under 90 seconds
- [ ] All 5 project stories told aloud, each under 3 minutes
- [ ] 8 stream programs and 6 SQL queries written without help
- [ ] Every topic list in this plan explained aloud once
- [ ] One full mock done and its weak answers redone
- [ ] Slot confirmed in writing and setup tested

## Daily non-negotiables

| | Fri 25 | Sat 26 | Sun 27 | Mon 28 | Tue 29 |
|---|---|---|---|---|---|
| 5 applications sent | | | | | |
| Slept 7 hours | | | | | |
| Check-in sent to Claude | | | | | |

## ⭐ Why I added these topics

Your plan already covers the core. These come up often in 3-year Java full-stack rounds. Do them after that day's core topics.

- **J11 Modern Java:** "Which Java version do you use, and what's new in it?" is a common opener.
- **J12 Design patterns:** Strategy and Factory fit payment-gateway code (PayU vs Setu) naturally, so you can answer from your own work.
- **Q07 Indexes and isolation levels:** the usual follow-up once the SQL queries are done.
- **B12 Testing:** "How do you test this service?" comes up in most Spring rounds.
- **M09 Docker:** only if time is left over. Your JD doesn't mention Docker or Kubernetes.
- **A15 JavaScript and TypeScript:** your JD needs Angular, and the UI round often starts with these basics before moving to Angular.
