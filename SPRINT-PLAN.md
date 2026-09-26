# Full-Stack Interview Sprint

Sep 25, 2026 · @Appu

> Reference copy of your original plan. Track progress in [README.md](README.md).

## How this plan works

The plan assumes the interview is on **Tuesday Sep 29, 2026**. If they confirm Wednesday, use the Wednesday section near the end. Tick a box only when its "Done when" line is true.

**Rules until the interview:**

- Instagram stays uninstalled.
- Send 5 job applications every day. One interview is not a pipeline.
- Say every answer out loud. Reading is not preparing.
- Sleep 7 hours. A tired brain blanks on things it knows.
- End each day by sending Claude two lines: boxes ticked, and your weakest topic.

Daily load is about 4 hours today and about 7 hours each day from Saturday to Monday.

---

## Friday 25 Sep: your story and logistics

Today you lock down everything the interview opens with. About 4 hours.

- [ ] **Confirm the slot in writing** (15 min). Ask the recruiter for date, time, mode, number of rounds, whether there is live coding, and the JD. *Done when:* you have it on WhatsApp or email.
- [ ] **Research the company** (30 min). What they sell, who pays them, the stack in the JD, and interview reviews for this role on AmbitionBox or Glassdoor. *Done when:* you can say in two lines what they do and why you fit.
- [ ] **Audit your resume** (45 min). Read every line. Next to each technology or claim, write one real example you can defend for 2 minutes. For anything you used only lightly, prepare an honest "I used it lightly, here's what I did" answer. *Done when:* every line has an example.
- [ ] **Write your 90-second intro** (45 min). Order: who you are now (3 years, Java and Spring Boot, fintech payments), then two things you built and their impact, then your Angular work, then why this role and that you can join immediately. *Done when:* said aloud 3 times under 90 seconds on a phone timer, and pasted to Claude for grading.
- [ ] **Write your "Why did you leave?" answer** (20 min). Two lines: honest, short, no blame, ending on what you want next. *Done when:* you can say it without pausing.
- [ ] **Fix your salary numbers** (10 min). Current CTC, one expected CTC number, and your private minimum. *Done when:* all three are written down.
- [ ] **Apply to 5 roles** (1 h). *Done when:* 5 applications sent.

## Saturday 26 Sep: Java, streams, SQL

Saturday covers the Java questions and the live coding most full-stack rounds include. About 7.5 hours.

- [ ] **Java core, explained aloud** (2.5 h). Give each topic a 2-minute spoken answer and note every one you stumble on. *Done when:* all covered and your stumble list is written.
  - HashMap internals: hashing, buckets, collisions, treeify when a bucket crosses 8 entries (table size 64 or more), resize past the 0.75 load factor
  - equals/hashCode contract, and what breaks if you override only one
  - String immutability, string pool, StringBuilder vs StringBuffer
  - ArrayList vs LinkedList; HashMap vs LinkedHashMap vs TreeMap
  - ConcurrentHashMap vs Collections.synchronizedMap; volatile vs synchronized
  - ExecutorService, Future and CompletableFuture basics
  - Checked vs unchecked exceptions, try-with-resources, custom exceptions
  - Comparable vs Comparator; map vs flatMap; intermediate vs terminal operations; Optional
  - JVM memory: heap, stack, metaspace; GC basics; OutOfMemoryError vs StackOverflowError
  - SOLID with one example from your own code; interface vs abstract class; how to make a class immutable
- [ ] **Java 8 streams, written and run** (2 h). Look anything up only after 10 minutes stuck. *Done when:* all 8 compile and print correct output.
  1. Character frequency in a string
  2. First non-repeated character
  3. Duplicate elements in a list
  4. Second-highest number
  5. Employees grouped by department
  6. Highest-paid employee per department
  7. Employees sorted by salary descending, then by name
  8. Numbers partitioned into even and odd
- [ ] **SQL, written and run** (1.5 h). Use local Postgres or any online SQL editor. *Done when:* all 6 return correct results.
  1. Nth highest salary, once with DENSE_RANK and once with LIMIT/OFFSET
  2. Highest salary per department
  3. Employees earning above their department average
  4. Duplicate emails with GROUP BY and HAVING
  5. Each employee with their manager's name (self-join)
  6. Departments with zero employees (LEFT JOIN)
- [ ] **DSA warm-up** (45 min). Two Sum, Valid Anagram, Longest Substring Without Repeating Characters. *Done when:* solved in Java, with time and space complexity stated aloud.
- [ ] **Apply to 5 roles** (45 min). *Done when:* 5 applications sent.

## Sunday 27 Sep: Spring Boot, microservices, your system

Sunday turns your BharatNXT work into the answers that decide this interview. About 7 hours.

- [ ] **Spring Boot, explained aloud** (2.5 h). *Done when:* all covered and your stumble list is written.
  - What @SpringBootApplication combines, and how auto-configuration picks beans (@Conditional annotations)
  - IoC and DI; constructor vs field injection and why constructor wins; @Qualifier vs @Primary
  - Bean scopes and lifecycle (@PostConstruct, @PreDestroy)
  - @Component vs @Service vs @Repository vs @RestController
  - @Transactional: rolls back only on unchecked exceptions by default; self-invocation skips the proxy; REQUIRED vs REQUIRES_NEW
  - JPA: lazy vs eager, the N+1 problem and its fix (JOIN FETCH or @EntityGraph), pagination with Pageable
  - Validation with @Valid; global error handling with @RestControllerAdvice
  - @Value vs @ConfigurationProperties; profiles
  - REST: PUT vs PATCH, idempotent methods, status codes 200, 201, 204, 400, 401, 403, 404, 409, 500
  - Spring Security with JWT: how a request moves through the filter chain
  - Actuator health and metrics
- [ ] **Microservices, explained aloud** (2 h). Tie each point to your own system wherever you can. *Done when:* all covered and your stumble list is written.
  - Monolith vs microservices, and when not to split
  - Sync calls (REST, Feign, WebClient) vs async messaging (RabbitMQ)
  - RabbitMQ: exchange types, manual ack, redelivery, dead-letter queue, idempotent consumers
  - API gateway, service discovery, config server: the problem each one solves
  - Timeouts, retry with backoff, circuit breaker states (closed, open, half-open)
  - Database per service; Saga (choreography vs orchestration), and why not 2PC
  - Idempotency keys on payment APIs
  - Correlation IDs, centralized logs, distributed tracing
- [ ] **Your architecture and 5 stories** (2 h). Draw your BharatNXT system on paper: services, queues, Postgres, and the PayU, Setu and BBPS callbacks. Write each story in 5 to 6 lines: situation, what you did, result. Use what actually happened. If a case never came up, explain how your system handles it. *Done when:* diagram drawn, 5 stories written, each told aloud in under 3 minutes.
  1. A BBPS bill payment end to end: fetch, pay, status
  2. A duplicate PayU or Setu callback, and how idempotency handles it
  3. A gateway timeout with unknown payment status: PENDING, status check, reconciliation
  4. A RabbitMQ consumer crashing mid-message: ack, redelivery, dead-letter queue
  5. The hardest production bug you fixed
- [ ] **Apply to 5 roles** (45 min). *Done when:* 5 applications sent.

## Monday 28 Sep: Angular and the full mock

Monday closes the Angular gap and tests everything under interview conditions. About 7 hours.

- [ ] **Angular, explained aloud** (2.5 h). Start from your Infosys prep notes. *Done when:* all covered and your stumble list is written.
  - Lifecycle hooks and their order; constructor vs ngOnInit
  - Component communication: @Input/@Output, a shared service with BehaviorSubject, @ViewChild
  - Observable vs Promise; Subject vs BehaviorSubject vs ReplaySubject
  - switchMap vs mergeMap vs concatMap vs exhaustMap, with one real use case each
  - Stopping subscription leaks: async pipe, takeUntilDestroyed or takeUntil
  - Change detection: Default vs OnPush
  - DI and providedIn: 'root'
  - Routing: lazy loading, guards, resolvers
  - HTTP interceptors: attach the JWT and handle 401 in one place
  - Reactive vs template-driven forms; one custom validator
  - Pure vs impure pipes
  - Recent Angular: standalone components, signals, @if and @for control flow, @defer
- [ ] **Angular hands-on** (1 h). In StackBlitz or locally, build a search box that calls an API using debounceTime, distinctUntilChanged and switchMap. Then build a reactive form with one custom validator. *Done when:* both work in the browser.
- [ ] **Full mock interview with Claude** (1 h). Type "start mock" in your Claude chat. It covers intro, project, Java, Spring, microservices, Angular and one live coding question. *Done when:* finished, with a score and a list of weak answers.
- [ ] **Fix the weak answers** (1 h). Re-answer each one aloud until it comes out clean. *Done when:* every weak answer is redone.
- [ ] **Write 2 questions to ask them** (10 min). For example: "What does the team's architecture and release process look like?" and "What does a strong first 90 days look like in this role?" *Done when:* both written.
- [ ] **Test your setup** (20 min). Laptop charged, camera, mic, internet, the meeting app, a quiet room, resume and pen at hand. *Done when:* a test call with a friend works.
- [ ] **Apply to 5 roles** (45 min). *Done when:* 5 applications sent.
- [ ] **Stop at 9 PM, sleep by 11.** If the interview is Tuesday, nothing new tonight.

## Interview day

Interview day is for recall, not learning.

**Before:**

- [ ] Reread for 1 hour at most. Your intro, 5 stories and stumble lists. No new topics.
- [ ] Join 10 minutes early. Phone on silent, water, resume and pen at hand.

**During:**

- On coding questions, talk through your approach before you type.
- If you don't know something, say what you do know and how you would find the rest. Never bluff.
- If salary comes up, say your expected CTC once, calmly, and that you can join immediately.
- Ask your 2 questions at the end.

**After:**

- [ ] Write down every question within 30 minutes. Mark where you were weak and send the list to Claude.
- [ ] Apply to 5 roles the same day. The pipeline keeps running whatever the result.

## If the interview is Wednesday

If they confirm Wednesday 30 Sep, Tuesday 29 Sep is for fixing and rehearsing, not new topics. About 3.5 hours.

- [ ] Second mock with Claude on your two weakest areas (1 h). *Done when:* both areas scored and the weak answers noted.
- [ ] Redo the streams and SQL problems you got wrong (1 h). *Done when:* all run correctly without help.
- [ ] Tell your intro and 5 stories aloud once more (30 min). *Done when:* each one fits its time limit.
- [ ] Apply to 5 roles (45 min). *Done when:* 5 applications sent.
- [ ] Stop at 9 PM, sleep by 11.

## Readiness check: Monday 6 PM

Run this at 6 PM on Monday. Every line should be a yes. Anything still a no gets the time until 9 PM.

- [ ] Intro comes out clean in under 90 seconds
- [ ] All 5 project stories told aloud, each under 3 minutes
- [ ] 8 stream programs and 6 SQL queries written without help
- [ ] Every topic list in this plan explained aloud once
- [ ] One full mock done and its weak answers redone
- [ ] Slot confirmed in writing and setup tested
