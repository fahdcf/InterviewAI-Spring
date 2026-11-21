# InterviewAI migration plan

JavaFX desktop frontend and Spring Boot backend in a new repository

Prepared on 16 November 2026. Execution record updated on 21 November 2026.

The implemented flow and verification evidence are recorded in [Migration status](docs/migration-status.md), [Test evidence](docs/test-evidence.md), and the [original UI inventory](docs/ui-inventory.md). Checked tasks refer to the delivered implementation and the recorded evidence. Live-provider and remote-server acceptance are skipped at the user's request. The reference comparison uses source review, database queries and representative runtime captures. A complete manual exercise of every control in the original app is unverified.

Implementation choices are recorded in the test evidence. CV extraction happens within the authenticated multipart request, the backend uses scheduled workers and a durable database queue, and the repository/HTTP integration suites run together with Surefire. The JavaFX layouts are reorganized around the migrated features.

## 1. Outcome and starting point

Deliver two independently runnable applications in one new repository. The JavaFX desktop application communicates with a Spring Boot REST API over HTTP and JSON. The Spring Boot application is a layered monolith that owns authentication, business rules, AI integration, and MySQL persistence.

The migration reuses the existing FXML, CSS, images, Java models, SQL queries, validation rules, AI prompts, document extraction, and working interaction flows. New code supplies the HTTP boundary, Spring configuration, reliable transactions, and tests around those boundaries.

The implementation must be understandable to a junior software engineer who knows Java classes, methods, SQL and basic Spring Boot. Favor explicit steps, descriptive names and familiar framework features. Each migrated feature must be easy to trace, debug and explain from the screen action through the HTTP request to the database result. Readability and verified behavior are both completion requirements.

The source repository is `fahdcf/InterviewAI`. The reviewed source revision is `b5aebcf4d1263978a36a58d8b9f48ae00364ea02`. The proposed destination name is `InterviewAI-Spring`. This name is a planning default and can be changed when creating the repository. Repository creation is task B below. This document is the migration deliverable for the current planning task.

The original repository remains the reference application, as requested. All migration commits, infrastructure configuration, and release artifacts belong in the new repository. Development and migration rehearsal use a separate database.

The earlier review compiled all 83 Java source files with JDK 21. Maven found no automated tests. Login, registration, and onboarding FXML loaded in offline checks. Live database workflows and provider calls still need a baseline run in task A.

[Reviewed source](https://github.com/fahdcf/InterviewAI/tree/b5aebcf4d1263978a36a58d8b9f48ae00364ea02)

## 2. Architecture decisions

| Area | Planned approach | Purpose |
| --- | --- | --- |
| Repository | One new repository with `backend` and `desktop` Maven modules | Keep related changes reviewable while building each app independently |
| Backend | Spring Boot, Spring MVC, Spring Security, Bean Validation | Provide REST endpoints, authentication, and request validation |
| Persistence | Spring JDBC with `JdbcTemplate`, explicit SQL and simple row-mapping methods | Reuse the current SQL and make database operations visible |
| Transactions | Service methods using Spring transactions | Commit each business operation consistently |
| Database changes | Flyway versioned SQL migrations | Reproduce installations and migrate existing data safely |
| Desktop | JavaFX, existing FXML/CSS, constructor-injected controllers | Preserve the screens and simplify controller responsibilities |
| HTTP client | One reusable Java `HttpClient`, called inside JavaFX `Task` workers | Keep HTTP code sequential and the screen responsive |
| Authentication | Spring Security sessions and a desktop cookie manager | Keep authentication straightforward for one backend |
| API contract | Versioned `/api/v1` routes, explicit request/response DTOs, OpenAPI | Make the boundary understandable and testable |
| Background generation | A job table, one bounded Spring task executor and one backend instance | Track long AI calls with explicit steps and simple restart recovery |
| Initial platform | JDK 21 and Windows desktop packaging | Match the current development environment |
| Test stack | JUnit, Spring HTTP tests, MySQL integration tests, fake AI HTTP server, JavaFX smoke tests | Verify behavior at each boundary |

At setup time, pin a supported Spring Boot release compatible with JDK 21 and the selected dependencies. Pin a compatible JavaFX release and normalize the FXML declarations. Record exact versions in the POMs and Maven wrapper. Use Spring's dependency management for compatible library versions. Spring JDBC participates in Spring-managed transactions through its managed connection handling. [Spring Boot requirements](https://docs.spring.io/spring-boot/system-requirements.html) · [Spring JDBC connections and transactions](https://docs.spring.io/spring-framework/reference/data-access/jdbc/connections.html)

### Backend structure

```text
backend/
  pom.xml
  src/main/java/com/interviewai/backend/
    InterviewAiApplication.java
    config/                  Database, AI properties, executor, clock
    security/                SecurityFilterChain, authenticated user, permissions
    controller/              Auth, Profile, Course, Practice, Progress, Admin
    dto/
      request/               Validated API input
      response/              Explicit API output
    service/                 Business workflows and transaction boundaries
    repository/              SQL queries and row mapping
    model/                   User, Course, Chapter, Question, Goal, Attempt, Job
    integration/
      ai/                    Gemini and OpenRouter clients, response parsers
      document/              PDF and Word extraction
    exception/               Application errors and HTTP exception handler
  src/main/resources/
    application.yml
    db/migration/
  src/test/java/
  src/test/resources/
```

Use concrete classes with constructor injection. Add interfaces at replaceable external boundaries such as AI providers when they improve testing. Keep classes specific to their responsibility. A service such as `AnswerSubmissionService` is easier to follow than a large general-purpose `ApplicationService`.

### Readable implementation rules

Apply these rules to every task from A through Z.

- Use familiar Java classes with explicit constructors, private fields, getters and setters where needed. Use simple DTO classes with clear field names and validation annotations. Keep mapping between models and DTOs in ordinary named methods.
- Use constructor injection with a single constructor and final dependency fields. This makes each class's dependencies visible at the top of the file. Create concrete services such as `CourseService` directly through Spring.
- Write methods as a clear sequence of actions. For example, load the question, check ownership, validate the answer, grade it, save the result, and return feedback. Use guard clauses for invalid input and named helper methods for meaningful steps.
- Prefer ordinary `for` loops and explicit `if` statements when processing involves validation, branching or several actions. Use short lambdas only where a framework callback needs one and its purpose is immediately clear.
- Choose names such as `currentUserId`, `questionRepository` and `completedChapters`. Use consistent names between API fields, DTOs, service parameters and SQL columns where practical.
- Keep classes focused on a feature or responsibility. Split a long method at a real boundary, such as parsing provider output or saving a course, and preserve the readable sequence in the caller.
- Share code when two existing flows genuinely need the same behavior. Use a concrete helper or service with explicit parameters. Keep the amount of abstraction proportional to the current app.
- Add a short comment for a business rule, transaction boundary or concurrency check that needs explanation. Method names and code should carry the routine meaning.
- Return clear results and throw descriptive exceptions on failure. Handle expected errors close to the relevant workflow, and map them to HTTP responses in the central exception handler.
- Explain framework configuration in plain language. A reader should be able to identify where a dependency is created, where a request is authenticated, and where a database transaction begins.
- Use Spring MVC, JDBC, ordinary service calls and one backend process as the implementation scope. Additional patterns or libraries need a concrete problem in this app and an explanation of how they improve the implementation.
- Verify data integrity, authorization and error recovery with tests even when the implementation is small. A short method can still require a transaction or a duplicate-request check.

### Common annotations and where to use them

| Annotation | Use in this project | Plain-language meaning |
| --- | --- | --- |
| `@SpringBootApplication` | Backend main class | Start and configure the Spring Boot app |
| `@RestController` | HTTP controllers | Accept HTTP requests and return JSON |
| `@RequestMapping`, `@GetMapping`, `@PostMapping`, `@PutMapping`, `@PatchMapping`, `@DeleteMapping` | Controller routes | Connect a URL and HTTP method to a Java method |
| `@RequestBody`, `@PathVariable`, `@RequestParam` | Controller parameters | Read input from the request body, URL or query string |
| `@Valid`, `@NotBlank`, `@NotNull`, `@Size`, `@Email`, `@Min`, `@Max` | Request DTO validation where appropriate | Check input before the workflow runs |
| `@Service` | Business workflow classes | Register a class that owns application rules |
| `@Repository` | JDBC repository classes | Register a class that reads and writes database data |
| `@Transactional` | Public service methods that save related data | Commit related database writes together or roll them back on failure |
| `@Configuration`, `@Bean` | Security, HTTP client and executor configuration | Create and configure shared application objects |
| `@ConfigurationProperties` | Related database/provider/application settings | Read a named group of configuration values |
| `@RestControllerAdvice`, `@ExceptionHandler` | Central API error handler | Turn application exceptions into predictable HTTP errors |
| `@AuthenticationPrincipal` | Protected controller methods | Obtain the signed-in user supplied by Spring Security |
| `@FXML` | JavaFX controllers | Connect Java fields and handlers to FXML controls |
| `@Test`, `@BeforeEach`, `@SpringBootTest` | Appropriate test classes | Define tests, prepare test data and load the backend for integration checks |

Use each annotation where its behavior is required and explain any additional annotation when introducing it. Configure permissions in one readable `SecurityFilterChain` and perform resource-ownership checks in services. Keep password encoding, authentication and session handling in Spring Security's standard components.

For `@Transactional`, use the usual default transaction behavior and let write failures propagate. A background worker should call a separate injected service's public save method so Spring can start the transaction. Provider HTTP calls happen before that save method. A rollback test must demonstrate that the transaction actually applies. [Spring transaction annotations](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/annotations.html)

### Keep the difficult parts small and explicit

| Concern | Concrete implementation to aim for | Verification |
| --- | --- | --- |
| Typical request | `GoalController` calls `GoalService`, which calls `GoalRepository` using `JdbcTemplate` | Follow one goal creation from request to inserted row |
| HTTP DTOs | Ordinary Java request and response classes with explicit fields | Read a JSON example next to the matching DTO |
| JavaFX network work | `Task.call()` performs the HTTP call, `setOnSucceeded` displays the result, `setOnFailed` displays the error | Delay the response and verify the UI remains responsive |
| SQL mapping | A small method reads named columns from a `ResultSet` into one model | Compare the returned model with a known row |
| Background jobs | A `CourseJobService` saves a row, an executor runs a `CourseGenerationWorker`, and a repository saves progress | Read the worker top to bottom and test each terminal outcome |
| Restart recovery | One startup recovery method marks unfinished running work interrupted and requeues saved queued work | Restart the single backend process during generation |
| Duplicate requests | A request ID, a database uniqueness rule and a stored result | Send the same request twice and verify one reward/result |
| Concurrent claims | A short transaction locks the user's claim state, checks eligibility and saves the claim with XP | Submit two concurrent claims and verify the correct total |
| Shared lesson/review behavior | Both screens call one `PracticeApi` and the backend's shared answer service | Run lesson and review scenarios against the same service |

These mechanisms should have small, named methods and short comments explaining why they are needed. Review each one with a debugger and a focused test before reusing it in another feature.

| Layer | Responsibility | Example |
| --- | --- | --- |
| Controller | Accept validated DTOs, obtain the authenticated principal, call a service, return an HTTP response | `PracticeController.submitAnswer()` |
| Service | Enforce ownership and business rules, coordinate integrations, manage transactions | `AnswerSubmissionService`, `QuestService` |
| Repository | Execute parameterized SQL and map results | `QuestionRepository`, `ProgressRepository` |
| Model | Represent application data and domain states | `Question`, `QuestionStatus`, `CourseGenerationJob` |
| DTO | Define data exposed at the HTTP boundary | `SubmitAnswerRequest`, `AttemptResponse` |
| Integration | Translate external provider requests and responses | `GeminiCourseClient`, `OpenRouterGradingClient` |

Controllers call services. Services call repositories and integrations. Repositories use the configured data source. Backend classes remain independent of JavaFX. HTTP responses expose selected DTO fields, including feedback only at the appropriate point in the learning flow.

### Frontend structure

```text
desktop/
  pom.xml
  src/main/java/com/interviewai/desktop/
    DesktopApplication.java
    config/                  API URL and application dependency wiring
    navigation/              Routes, navigator, screen lifecycle
    session/                 Current user, cookies, active course/chapter
    api/
      ApiClient.java         Shared HTTP, JSON, timeout, and error handling
      AuthApi.java
      ProfileApi.java
      CourseApi.java
      PracticeApi.java
      ProgressApi.java
      AdminApi.java
      dto/                   Simple request and response classes matching the API
    feature/
      auth/                  Login and registration controllers
      onboarding/            Preferences, upload, waiting screen
      dashboard/             Course selection and summary
      practice/              Lesson and review controllers
      progress/              Charts, goals, quests
      profile/               Candidate profile
      admin/                 Admin screens and profile
    component/               Shared sidebar, question panel, feedback panel
  src/main/resources/
    fxml/                    Existing screens, organized by feature as migrated
    css/
    images/
  src/test/java/
```

Frontend controllers read controls, validate basic form input, call an API client, and render the result. UI-only state belongs in the controller or a small view model where necessary. Grading, XP, permissions, and persistence belong in backend services. The desktop depends on the API contract and its own DTOs. It builds independently from backend implementation classes.

Use `FXMLLoader.setControllerFactory` to construct controllers with their API dependencies. Keep a single application-owned session object and navigator. Cancel timers and polling on screen disposal, and ignore responses belonging to an older screen or signed-out session. [FXMLLoader controller factory](https://openjfx.io/javadoc/21/javafx.fxml/javafx/fxml/FXMLLoader.html) · [Java HttpClient](https://docs.oracle.com/en/java/javase/21/docs/api/java.net.http/java/net/http/HttpClient.html)

Keep the controller factory as an explicit mapping from a known controller class to its constructor. For a network action, read input controls on the JavaFX thread, copy the values into the request DTO, and create a `Task` that calls the feature API. Use the task's success and failure handlers for UI updates. Submit tasks to one application-owned executor with a bounded queue. Keep only one pending poll per job or attempt, and check the active screen/session before displaying a response. JavaFX `Task` delivers its lifecycle event handlers on the JavaFX Application Thread. [JavaFX Task](https://openjfx.io/javadoc/21/javafx.graphics/javafx/concurrent/Task.html)

## 3. Existing functionality and code reuse map

Paths below are relative to `src/main/java/com/interviewai` in the original repository. FXML and styling are under `src/main/resources`.

| Existing capability | Existing code to reuse | Destination |
| --- | --- | --- |
| Login, registration, inactive-account handling, roles | `LoginController`, `CreateAccountController`, `AuthService`, `UserDAO`, `RoleDAO` | Auth UI, `AuthController`, security, user and role repositories |
| Onboarding preferences and CV upload | `OnboardingController`, `OnboardingDAO`, `OnboardingData` | Onboarding UI and API, upload DTO, onboarding repository |
| Technical and soft-skills generation | `MultiStageAIService`, `AIService`, `ChapterOutline`, `GeneratedCourse` | Generation service, provider client, parsers, persisted jobs |
| PDF, DOCX, DOC text extraction | `MultiStageAIService.extractText` | Backend document integration |
| Course, chapter, question and choice storage | `CourseDAO`, `CourseProgressDAO`, `QuestionDAO` | Course, chapter, question and progress repositories |
| Dashboard, course selection, resume last location | `DashboardController`, `MainLayoutController`, `InterviewPrepDAO`, last-used methods in `UserDAO` | Dashboard API and UI, navigation-state service |
| Multiple-choice and short-answer lessons | `LessonController`, `Question`, `OpenRouterAIService` | Practice UI, answer submission service, grading client |
| Incorrect-question review and explanations | `ReviewController`, `reviewQuestionsController`, `QuestionDAO` | Review UI, shared practice service, explanation endpoint |
| XP, streaks, charts, accuracy, leaderboard | `ProgressController`, `ProgressDAO` | Progress and dashboard services and read repositories |
| Personal goals | `EditGoalsDialogController`, `GoalDAO`, `UserGoal` | Goal UI, controller, service and repository |
| Daily and monthly quests | `QuestsController`, quest methods in `ProgressDAO` | Quest UI, claim service and repository |
| Candidate profile and account deletion | `ProfileController`, `UserDAO` | Profile API and UI, account service |
| Admin overview and analytics | `AdminController`, `AnalyticsService`, `AnalyticsDAO` | Admin overview API and UI, analytics service |
| User search, filters, pagination, suspension, activation and resets | `UserManagementController`, `UserManagementDAO` | Admin user API and UI, admin user service |
| Course search, filters, details, archive and delete | `CoursesManagementController`, `CourseManagementDAO` | Admin course API and UI, admin course service |
| Admin name, password and logout | `AdminProfileController`, `AdminSidebarController` | Profile API, admin profile UI and navigation |
| Shared visual resources | FXML, CSS, images, animations, sidebars | Desktop resources and components |

The source also contains placeholders for simulation, course map, prompt management, feedback, achievements, and notifications. The password-help screen describes an unavailable reset flow. The stored two-factor flag does not establish an implemented second-factor challenge. Task A records the actual reachable behavior of these items, including static displays, before assigning parity checks. Any newly requested functionality should have its own acceptance criteria.

## 4. API outline

All routes use `/api/v1`. The server derives the acting user from authentication. Every course, chapter, question, goal, attempt, and generation job access includes an ownership or administrative-permission check.

| Area | Proposed routes | Response and behavior |
| --- | --- | --- |
| Authentication | `GET /auth/csrf`, `POST /auth/register`, `POST /auth/login`, `POST /auth/logout`, `GET /me` | Session cookie, CSRF token, safe user profile |
| Profile | `PATCH /me`, `PUT /me/password`, `POST /me/account-deletion` | Validated changes, password confirmation for sensitive actions |
| Navigation | `PUT /me/navigation` | Persist an owned course/chapter pair |
| Onboarding | `GET /me/onboarding`, `PUT /me/onboarding`, `POST /me/cv` | Preferences and a user-owned upload ID |
| Generation | `POST /course-jobs`, `GET /course-jobs/{id}`, `GET /course-jobs?status=active`, `POST /course-jobs/{id}/retry` | `202` with job ID, progress and terminal status |
| Courses | `GET /courses`, `GET /courses/{id}`, `GET /courses/{id}/chapters` | User's course pairs, metadata and progress |
| Practice | `GET /chapters/{id}/questions`, `POST /questions/{id}/attempts`, `GET /attempts/{id}` | Safe question DTOs and persisted grading results |
| Explanations | `POST /questions/{id}/explanations` | Explanation for an eligible question or completed attempt |
| Review | `GET /courses/{id}/review`, `GET /chapters/{id}/review` | Incorrect questions and review summary |
| Dashboard | `GET /dashboard` | Course summary, next lesson, XP, streak and visible widgets |
| Progress | `GET /progress`, `GET /leaderboard` | Charts, metrics, course progress and rankings |
| Goals | `GET /goals`, `POST /goals`, `PATCH /goals/{id}`, `DELETE /goals/{id}` | Goal targets, baselines and calculated progress |
| Quests | `GET /quests`, `POST /quests/daily/claims`, `POST /quests/monthly/claims` | Eligibility, period/reset data and awarded XP |
| Admin users | `GET /admin/users`, `GET /admin/users/{id}`, `PATCH /admin/users/{id}` | Search, filters, pagination, profile and activation state |
| Admin resets | `POST /admin/users/{id}/xp-reset`, `POST /admin/users/{id}/progress-reset` | Explicit, permission-checked reset operations |
| Admin courses | `GET /admin/courses`, `GET /admin/courses/{id}`, `PATCH /admin/courses/{id}`, `DELETE /admin/courses/{id}` | Course details, filters, archive and deletion |
| Admin analytics | `GET /admin/overview`, `GET /admin/analytics` | Existing overview cards and charts |

Use consistent JSON errors with `code`, `message`, `fieldErrors`, and `requestId`. Document `400`, `401`, `403`, `404`, `409`, upload size errors, and provider failures. `401` prompts sign-in. `403` displays an access error. Mutating operations that may be retried use a client-generated request ID, scoped to the authenticated user and operation, with a unique database constraint. Reusing a request ID with different input returns `409`.

Course generation always runs as a background job. Answer submission can return an immediate completed attempt for local grading and `202` plus an attempt ID for AI grading. JavaFX polls pending attempts. This avoids holding an HTTP request open through long provider retries and allows recovery after a response is lost.

## 5. Step-by-step implementation tasks

Follow tasks A through Z in order. Complete each verification gate before marking that task done. Each feature migration includes backend implementation, JavaFX connection, and a working end-to-end check.

Each exit gate also includes a readability walkthrough. A junior engineer should be able to find the entry point, explain the main steps in order, identify the data being passed, and show the success and failure tests. Reviewers should simplify confusing code before extending it to the next feature.

### A. Record the baseline and acceptance scenarios

- [x] Obtain the pinned source revision in a separate reference checkout and record its SHA.
- [x] Run the original application against a disposable development database. Create synthetic candidate and administrator accounts.
- [x] Inventory every reachable screen, button, form, filter, chart, validation message and navigation path. Record working behavior, known defects and placeholders.
- [x] Capture representative screen references and synthetic sample data for the original learning and admin flows, and inventory the remaining controls. Record the limits of the reference run.
- [x] Record scoring rules, question status transitions, chapter completion, review rewards, XP values, timezone behavior and quest resets. The current quest screen specifies 20 answers for 900 XP and 12 chapters for 30,000 XP. Its daily claim resets progress and can become eligible again. Preserve this behavior unless an explicit rule change is recorded.
- [x] Record differences between dashboard quest widgets and the dedicated quests screen, and the distinction between estimated study time and measured time.

**Verification and tests**

- [x] Produce a parity checklist with a scenario ID, starting data, user actions and expected result for every working feature.
- [x] Verify each scenario on the reference application or label it unverified with the concrete environmental blocker.
- [x] Mark corrections such as optional-CV handling, data-loss prevention and provider failure handling explicitly so reviewers can distinguish intentional repairs from regressions.

**Exit gate** — The migration has an executable feature checklist and known expected behavior.

### B. Create and seed the new repository

- [x] Create a standalone repository under the user's account using the agreed destination name, initially `InterviewAI-Spring`.
- [x] Import the reviewed working-tree snapshot as the migration baseline. Preserve both developers' README credit and record the source URL and commit SHA in the new README.
- [x] Inspect imported files for credentials and personal data, including `schema2.sql`, helper scripts and sample dumps. Use synthetic fixtures for tests.
- [x] Use a clean snapshot import because earlier history contains documented credential exposure. Record source provenance in the initial commit message and README.
- [x] Configure `origin` to the new repository and perform all migration pushes there.

**Verification and tests**

- [x] Confirm the destination owner, name, standalone status and remote URL.
- [x] Compare the imported snapshot to the reviewed source and account for exclusions.
- [x] Confirm the original repository head matches the recorded value after setup.

**Exit gate** — The new repository contains a traceable baseline with the developers credited.

### C. Establish builds and runtime versions

- [x] Create the parent POM, `backend/pom.xml`, `desktop/pom.xml`, and Maven wrapper.
- [x] Use the readable implementation rules above as the code-review checklist. Start with ordinary classes, standard Spring annotations and explicit constructor injection.
- [x] Pin JDK, Spring Boot, JavaFX and database versions. Inspect the original database engine/version because the SQL dumps contain dialect-specific expressions.
- [x] Add Spring Web, Security, Validation, JDBC, MySQL, Flyway and test dependencies to the backend.
- [x] Move JavaFX, FXML and desktop JSON dependencies to the desktop module. Scope Spring Boot packaging to the backend and JavaFX packaging to the desktop.
- [x] Create development configuration using environment variables and a configurable desktop API base URL.

**Verification and tests**

- [x] Both modules compile from a fresh checkout using the wrapper.
- [x] Backend starts and serves a simple health endpoint. Desktop starts an initial view.
- [x] Dependency checks establish that JavaFX belongs to the desktop module and database/provider libraries belong to the backend module.

**Exit gate** — Two independently runnable application skeletons exist.

### D. Create the structured JavaFX shell

- [x] Copy FXML, CSS, images and animations into desktop resources and verify their paths.
- [x] Move controllers into the frontend feature packages and update `fx:controller` references.
- [x] Introduce an application-owned navigator and session. Replace static controller-instance lookups as screens migrate.
- [x] Build controllers through a controller factory and explicit dependency wiring.
- [x] Give screens a disposal path for timers, polling, asynchronous callbacks and event listeners.

**Verification and tests**

- [x] Load every reachable FXML with controlled API fixtures. Verify injected controls and event handlers.
- [x] Compare layout, scroll behavior, resizing, CSS and images with baseline captures.
- [x] Navigate repeatedly between screens and verify disposed screens stop polling and receiving updates.

**Exit gate** — The UI structure and navigation work with controlled data.

### E. Establish the API contract and shared HTTP behavior

- [x] Document endpoint DTOs, pagination, sort fields, validation, job states and errors in OpenAPI.
- [x] Add backend DTO validation and a central exception handler.
- [x] Implement `ApiClient` with a reusable `HttpClient`, cookie manager, CSRF support, timeouts and JSON serialization. Write sequential HTTP calls with `send()` and invoke them from JavaFX `Task.call()` on the desktop executor.
- [x] Implement feature clients such as `CourseApi` and `PracticeApi`, returning typed results.
- [x] Apply UI updates on the JavaFX Application Thread and ignore late results after navigation or sign-out.

**Verification and tests**

- [x] Contract tests cover request/response JSON, validation errors, empty results and unknown optional fields.
- [x] HTTP fixture tests cover connection failure, timeout, malformed JSON, `401`, `403`, `409` and server failure.
- [x] A deliberately delayed HTTP response leaves the desktop responsive.
- [x] Trace one request through the UI handler, task, API method, backend controller, service and repository. Verify each method has an obvious responsibility and clear input/output.

**Exit gate** — Every feature can use one consistent HTTP boundary.

### F. Prepare database creation and data migration

- [x] Create a separate migration database and rehearse with a copy of existing data when available.
- [x] Derive `V1__initial_schema.sql` from the actual source schema, ordering tables and foreign keys for the chosen MySQL version.
- [x] Move startup schema adjustments into explicit migrations, including quest, chapter-completion and goal columns.
- [x] Add versioned tables for jobs, attempts and claim deduplication. Preserve existing user/course/chapter/question IDs, hashes, goals, course pairs, progress timestamps and last-used navigation.
- [x] For an existing database copy, compare its schema to the intended V1 state and reconcile drift before explicitly baselining it. Apply only later migrations after that point.
- [x] Define the application timezone and date boundaries, with a configurable default of `Africa/Casablanca`. Assess legacy timestamps before conversion.

**Verification and tests**

- [x] Fresh installation creates the full schema and reference roles/permissions.
- [x] Upgrade rehearsal preserves row counts, relationships, XP totals, password hashes, goals and representative timestamps.
- [x] A second startup applies zero duplicate migrations and preserves user data.
- [x] Restore a database backup into a separate instance and verify it opens successfully.

**Exit gate** — Both fresh and migrated databases are reproducible. Flyway baseline records which earlier migrations are already represented in an existing database. [Flyway baseline](https://documentation.red-gate.com/flyway/reference/commands/baseline)

### G. Port DAOs into repositories

- [x] Move existing parameterized SQL and row mapping into layer-based repository classes.
- [x] Replace `DriverManager` connections with the Spring-managed data source and `JdbcTemplate`. Keep SQL beside the repository method that uses it and use small explicit row-mapping methods.
- [x] Let write failures propagate to the service transaction. Close remaining explicitly managed resources.
- [x] Move business calculations and cross-repository orchestration into services, including goal-progress calculations currently in `GoalDAO`.
- [x] Batch question/choice reads where the current code opens a query or connection per question.

**Verification and tests**

- [x] Compare repository query results against baseline fixtures for all migrated DAOs.
- [x] Use the real target MySQL engine for foreign keys, SQL syntax and rollback tests.
- [x] Force a nested insert failure and verify the service transaction rolls back all writes.
- [x] Repeated reads and writes return connections to the pool.

**Exit gate** — Existing persistence behavior is available through tested repositories.

### H. Migrate authentication and authorization

- [x] Reuse registration and password verification behavior through Spring Security. Preserve legacy SHA-256 login compatibility where present and upgrade a verified legacy hash to BCrypt on successful login.
- [x] Implement JSON login with persisted security context, session rotation, logout and `/me`.
- [x] Fetch CSRF tokens before login and state-changing calls, refreshing them after authentication changes. Configure secure cookies for HTTPS deployment.
- [x] Enforce inactive-account and permission rules on the backend. Invalidate or reject existing sessions after suspension and sensitive account changes.
- [x] Connect login, registration and logout screens to the API, including administrator routing.

**Verification and tests**

- [x] Test valid, invalid, duplicate and inactive accounts, username/email login and imported password formats.
- [x] Verify login survives the next request, logout invalidates the session, and session expiry returns `401` JSON.
- [x] Verify CSRF protection and that ordinary candidates receive `403` on administrative operations.
- [x] Remove password-bearing logs and confirm test log output excludes credentials.

**Exit gate** — Candidate and administrator authentication work from JavaFX. Custom Spring Security authentication must save the security context for subsequent requests. [Session persistence](https://docs.spring.io/spring-security/reference/servlet/authentication/session-management.html)

### I. Migrate course browsing, dashboard and resume

- [x] Reuse course-pair and chapter queries for authenticated course listing and dashboard summaries.
- [x] Connect the persistent sidebar, course switcher, chapter list and next-lesson action.
- [x] Persist last-used course and chapter through the navigation endpoint and validate that they belong together and to the user.
- [x] Handle fresh accounts, archived/deleted active courses, missing legacy course-pair links and empty chapters explicitly.

**Verification and tests**

- [x] Sign in with existing seeded courses and reproduce dashboard cards, selected course and next lesson.
- [x] Switch technical/soft-skills courses, close the app, sign in again and verify the saved location.
- [x] Verify a fresh account reaches onboarding and cross-user IDs are rejected.

**Exit gate** — Login through dashboard is a working HTTP-based application flow.

### J. Migrate onboarding and CV upload

- [x] Reuse all onboarding selections, validation, context and description fields.
- [x] Upload CV bytes through the authenticated multipart request. Extract the file during that request and save bounded text for its owner, with no client-local path dependency.
- [x] Reuse PDFBox and POI extraction in the backend with file size/type validation and bounded extraction.
- [x] Accept an omitted CV and return a clear error for unreadable uploads. Keep extracted text out of routine logs and define upload cleanup after extraction/job completion.
- [x] Persist preferences and establish a stable input snapshot for the generation job.

**Verification and tests**

- [x] Test onboarding with no CV, PDF, DOCX, DOC, corrupt files, oversized files and text extraction returning empty content.
- [x] Test with desktop and backend using different working directories or machines.
- [x] Verify one user cannot reference another user's upload.

**Exit gate** — The original onboarding flow works across a real network boundary.

### K. Port AI integrations and validate output

- [x] Reuse the chapter-outline and question prompts, existing parsers, grading prompts and explanation prompts.
- [x] Configure supported model IDs and provider URLs. Verify provider availability during setup because the checked-in Gemini experimental default was retired.
- [x] Separate course generation and answer-grading clients and keep provider keys in backend environment configuration.
- [x] Validate chapter numbers, expected chapter counts, question types, choices, answer compatibility and score bounds before accepting output.
- [x] Use bounded retries for transient failures, honor rate-limit delay when supplied, and represent exhausted retries as explicit failures.
- [x] Make provider clients safe for concurrent users. Key rotation and per-request state need concurrency tests when shared as Spring beans.

**Verification and tests**

- [x] A fake provider covers valid JSON, fenced JSON, partial output, invalid score, missing choices, timeout, rate limiting and unavailable models.
- [x] Existing prompt behavior is represented by recorded synthetic examples.
- [x] Provider failure produces an error state and preserves the learner's ungraded answer.

**Exit gate** — AI integration behavior is testable without network credentials. [Gemini model status](https://ai.google.dev/gemini-api/docs/models/gemini-2.0-flash)

### L. Implement durable course-generation jobs

- [x] Refactor the existing two-course, multi-stage workflow into `CourseJobService`, a readable `CourseGenerationWorker` and a scheduled worker that reads the durable database queue. Configure a single backend instance for this migration. Let the worker call named methods for outlines, chapter batches, validation and saving in that order.
- [x] Persist job owner, request ID, input snapshot, state, current stage, progress, timestamps and error details before dispatching work.
- [x] Use `QUEUED`, `RUNNING`, `SUCCEEDED`, `FAILED` and `INTERRUPTED` states. Claim queued work atomically and bound queued work per user.
- [x] Keep provider calls outside database transactions. Once both courses validate, save both complete course trees, their interview-prep link and the successful job result in one short transaction.
- [x] Add a straightforward startup recovery method for the single backend instance. Mark previous `RUNNING` jobs as `INTERRUPTED`, reload saved `QUEUED` jobs and submit them as executor capacity becomes available. Expose a retry action for interrupted work.
- [x] Make repeated submission and retry requests safe through stored request IDs and atomic job-state transitions.

**Verification and tests**

- [x] Generate both courses with deterministic AI fixtures and verify chapter/question/choice relationships.
- [x] Force a failure in the second course and during the final database save. Verify the user sees a retryable job and a consistent course list.
- [x] Double-submit a request, lose the HTTP response, retry it and verify a single resulting course pair.
- [x] Restart the backend during queued and running jobs and verify discoverable, actionable states.
- [x] Record a code walkthrough of progress, error handling and transactional saving. Test per-user pending-work limits and recovery of queued work after restarting the backend.

**Exit gate** — Course generation survives retries and reports its actual result.

### M. Connect the waiting screen and generation lifecycle

- [x] Submit a generation job and poll its status using a bounded polling interval.
- [x] Display a progress control and the stage messages from the generation workflow.
- [x] Display a retry action on failed/interrupted jobs and navigate to the generated courses only on success.
- [x] On login or reopening the app, discover the user's active jobs and reconnect to progress.
- [x] Stop screen polling on navigation or logout while keeping backend work associated with its owner.

**Verification and tests**

- [x] Complete fresh-account onboarding through to both courses on the dashboard.
- [x] Create an additional course pair from the dashboard and verify selection and history.
- [x] Disconnect the network, close the app, reopen it and verify the job can be rediscovered.
- [x] A stale callback after switching accounts leaves the new account's screen and data intact.

**Exit gate** — The full onboarding-to-dashboard flow works through HTTP.

### N. Migrate answer submission and lesson progress

- [x] Extract answer comparison, status transitions, completion rules and rewards from `LessonController` into a backend answer service.
- [x] Save attempts with user, question, mode, answer, request ID and grading state. Return immediate results for local grading and pollable pending attempts for AI grading.
- [x] Execute grading before the final transaction, then atomically save the result, question/chapter/course updates and earned XP.
- [x] Preserve the existing pass threshold and reward behavior captured in task A, including rules for repeat attempts and review. Resolve discrepancies in the parity record.
- [x] Enforce ownership, question eligibility and duplicate-submission protection on the server.
- [x] Replace desktop grading and database calls with `PracticeApi` calls and shared result rendering.

**Verification and tests**

- [x] Test multiple-choice and short-answer success, incorrect answers, skipped questions, navigation and retry behavior.
- [x] Test scores immediately below and at the current 70-point passing threshold.
- [x] Repeat an identical request after a lost response and verify identical feedback with a single XP award.
- [x] Submit concurrently for one question and verify the intended reward rule and consistent chapter completion.
- [x] A provider outage or database failure leaves an actionable attempt status and consistent progress.

**Exit gate** — Completing a lesson updates all related state correctly through the API.

### O. Migrate review and explanations

- [x] Reuse incorrect-question filters and course/chapter review navigation.
- [x] Rename and simplify `reviewQuestionsController`, using the shared question and feedback components where behavior matches lessons.
- [x] Route review answers through the same backend submission service with explicit review-mode rules.
- [x] Move explanation generation to the backend and preserve the existing reveal/explanation interactions at the correct time.

**Verification and tests**

- [x] Answer incorrectly, open review, correct the answer and verify review membership and status changes.
- [x] Verify review rewards match the agreed behavior and retries never multiply the same award.
- [x] Test both question types, empty review lists and explanation-provider failure.

**Exit gate** — Lesson and review workflows share tested backend logic while retaining their interactions.

### P. Migrate XP, streaks, charts and goals

- [x] Move progress queries and calculations into services with an injected clock and explicit timezone.
- [x] Preserve historical `progress` rows as XP events. The current schema permits multiple rows per user/course and streak queries depend on event dates. Keep that history when adding award deduplication.
- [x] Reuse chart labels, leaderboard presentation, accuracy calculations and course summaries, recording any metric corrections found during baseline comparison.
- [x] Migrate goal types `QUESTIONS`, `XP`, `CHAPTERS`, `TIME`, `COURSES` and `STREAK`. Preserve per-goal starting values and the absolute-versus-incremental calculation rules.
- [x] Connect goal creation, supported edits, deletion and progress refresh to the API.

**Verification and tests**

- [x] Compare imported XP totals, activity dates and leaderboard order to the baseline fixtures.
- [x] Test no activity, today/yesterday activity, gaps, month boundaries and timezone transitions.
- [x] Test every goal type with pre-existing progress and after a new qualifying activity.
- [x] Confirm the UI labels estimated study time accurately and existing historical data supports each reported metric.

**Exit gate** — Progress screens agree with persisted activity and baseline rules.

### Q. Migrate quest claims and timers

- [x] Move daily/monthly eligibility and reward values into `QuestService`.
- [x] Preserve the daily screen's repeatable progress-reset cycle recorded in task A. Use a claim-cycle ID or reset-version lock to allow each newly earned reward once.
- [x] Enforce monthly claim uniqueness per user and calendar period, and deduplicate all HTTP retries by request ID.
- [x] Write the successful claim, reset marker and XP award in one transaction after locking/rechecking eligibility.
- [x] Return server-derived eligibility and timing data so JavaFX renders progress and countdowns consistently.

**Verification and tests**

- [x] Test below-threshold, threshold, next earned daily cycle, repeated monthly claim and period rollover.
- [x] Send concurrent claims from two clients and verify one award for the same eligible cycle.
- [x] Interrupt between claim and XP writes and verify rollback preserves eligibility.
- [x] Compare dedicated quests and dashboard widgets to their recorded behavior.

**Exit gate** — Quest rewards are consistent across retries, clients and date boundaries.

### R. Migrate candidate and administrator profiles

- [x] Reuse username editing, password forms, validation and confirmation interactions.
- [x] Put profile and password updates behind authenticated endpoints and verify the current password for sensitive operations.
- [x] Implement account deletion as a backend transaction covering dependent data and owned files, with explicit handling of reference relationships.
- [x] Refresh desktop identity after edits and invalidate relevant sessions after password changes or deletion.

**Verification and tests**

- [x] Test duplicate usernames, invalid input, wrong current password and successful password change.
- [x] Delete a synthetic account with courses, jobs, attempts, progress and goals and verify referential integrity.
- [x] Verify candidate and admin profile screens display their updated identity and statistics.

**Exit gate** — Profile operations behave consistently through backend validation.

### S. Migrate admin user management

- [x] Reuse user-management SQL for search, filtering, pagination, details, roles, displayed badges and statistics that exist in the baseline.
- [x] Implement suspension, activation, supported profile edits, XP reset and progress reset through permission-checked services.
- [x] Specify reset scope across questions, chapters, attempts, XP, claims and goals so subsequent calculations are consistent.
- [x] Connect existing tables, detail panels, action buttons and confirmation dialogs to `AdminApi`.

**Verification and tests**

- [x] Compare filters, pagination and details against known fixtures.
- [x] Suspend a logged-in candidate and verify their next protected request is rejected.
- [x] Reset XP and progress separately, refresh the candidate dashboard and verify the documented scope.
- [x] Exercise each endpoint directly with insufficient permissions and expect rejection.

**Exit gate** — Admin user actions work with server-enforced permissions.

### T. Migrate admin courses and analytics

- [x] Reuse course-management and analytics queries and existing screen layouts.
- [x] Connect search, status filtering, pagination, details, archive/unarchive and deletion.
- [x] Apply course changes consistently to course-pair references, last-used navigation, active attempts and generation jobs where relevant.
- [x] Reproduce admin overview cards and charts using API DTOs.

**Verification and tests**

- [x] Compare statistics and filters against baseline fixtures.
- [x] Archive, restore and delete seeded courses and check the learner's resulting navigation.
- [x] Verify a grading result for a deleted course cannot save progress, and the desktop returns from an unavailable lesson to the dashboard.
- [x] Test administrative permissions independently for users, courses and analytics.

**Exit gate** — Administrative course and overview workflows pass parity checks.

### U. Complete frontend cleanup and dependency checks

- [x] Extract duplicated question rendering, feedback presentation, loading/error handling and sidebar behavior into focused components.
- [x] Remove migrated SQL, password handling, provider prompts and backend credentials from the desktop artifact.
- [x] Check every route, resource URL, FXML handler and CSS reference after package moves.
- [x] Remove obsolete singleton controller access and unmanaged background threads as their replacements are verified.
- [x] Retain existing useful empty/help states and document the baseline placeholders in the feature checklist.

**Verification and tests**

- [x] Automated dependency rules assert desktop independence from JDBC, backend classes and AI integrations, and backend independence from JavaFX.
- [x] Run UI smoke tests for every reachable screen, including scrolling, resizing, keyboard actions and repeated navigation.
- [x] Inspect the packaged desktop configuration and dependencies for accidental backend credentials or database drivers.

**Exit gate** — The frontend has clear responsibilities and communicates through the API.

### V. Run the complete parity and failure suite

- [x] Run the migrated candidate and admin scenarios with deterministic data. Record the source-derived rule comparison and the limits of the original manual verification.
- [x] Test two simultaneous users to detect leaked session state, ownership failures and shared provider-client races.
- [x] Exercise network disconnects, delayed responses, backend restart, database failure, malformed provider replies and expired sessions.
- [x] Verify fresh registration, imported users and existing partially completed courses.
- [x] Compare baseline screenshots and data outputs, recording each intentional correction and unresolved discrepancy.

**Verification and tests**

- [x] Implemented learning and administrative workflows have recorded automated evidence. Record placeholders and intentional presentation changes separately.
- [x] Every critical failure path provides a recoverable UI state and consistent persisted data.
- [x] No high-severity data-loss, authentication, ownership or duplicate-reward issue remains open.

**Exit gate** — The complete app passes functional parity and failure recovery checks.

### W. Verify the real AI and remote-client path

- **Skipped by user** — Configure backend provider credentials and supported models in a development environment.
- **Skipped by user** — Run one complete technical/soft-skills generation using a synthetic CV and interview context.
- **Skipped by user** — Exercise real short-answer grading and explanations and check job timing and rate-limit behavior.
- [x] Run JavaFX against the backend in a separate process and through the local HTTPS deployment. A separate client container verifies its HTTPS address. Remote-server acceptance is skipped by request.

**Verification and tests**

- **Skipped by user** — Real generated output satisfies the schema and can be completed/reviewed through the UI.
- [x] Runtime responses remain responsive and meaningful during slow provider calls.
- [x] Confirm all database/provider traffic originates from the backend and credentials are loaded there.

**Exit gate** — Real integrations and the HTTP deployment boundary work end to end.

### X. Package and document the runnable applications

- [x] Produce an executable Spring Boot JAR and a Windows JavaFX application image/installer with its required runtime.
- [x] Provide backend configuration examples with variable names and placeholders, plus desktop API URL configuration.
- [x] Provide documented commands or scripts for starting MySQL, the backend and the desktop, plus a development admin provisioning procedure.
- [x] Include database migration, backup, restore, troubleshooting and provider configuration instructions.
- [x] Provision and verify a local Docker deployment with HTTPS, service restart behavior, bounded job execution and persistent database storage. Remote deployment acceptance is skipped by request.

**Verification and tests**

- [x] Install and launch the desktop on a clean Windows account or test VM.
- [x] Start the packaged backend against a fresh database, provision users and complete a learning flow.
- [x] Restart both packaged applications and verify persisted state and actionable interrupted jobs.

**Exit gate** — A second developer can run the final app from the documented setup.

### Y. Rehearse existing-data cutover and rollback

- [x] Export the source data under a documented consistent-snapshot/write-pause procedure when an existing installation is being migrated.
- [x] Import into the destination database, reconcile the schema, run migrations, and verify counts, relationships and representative user journeys.
- [x] Reconcile uploads whose old records contain local filesystem paths. Existing course access should remain usable, and future regeneration can request a new upload where needed.
- [x] Validate password compatibility, last-used course/chapter, goals, XP, streaks, quest claims and administrative state.
- [x] Rehearse switching the desktop API URL to a previous working backend with its compatible database snapshot. Record how data created after a cutover would be preserved before rollback.

**Verification and tests**

- [x] Migration is repeatable from the backup into a fresh destination.
- [x] The pre-migration and post-migration reconciliation report accounts for every change.
- [x] A restore rehearsal successfully opens representative accounts and courses.

**Exit gate** — Existing users have a verified migration path and recovery procedure.

### Z. Release the completed migration

- [x] Run the final CI build, repository/HTTP integration suite, UI smoke suite and packaged-app acceptance checks.
- [x] Complete a final readability review of login, course generation, answer submission and quest claiming. Record a short plain-language explanation of each flow in the relevant setup or developer notes.
- [x] Review the parity checklist, deliberate fixes and deployment instructions against the original objective.
- [ ] Publish the backend JAR, desktop ZIP and SHA-256 checksums in the new repository and tag the verified implementation release `v1.0.0`, noting the skipped external acceptance checks.
- [x] Include source provenance, both developers' credit, supported functionality, setup instructions and exact tested versions in the new README.
- [x] Record the final source SHA, migration release SHA and database migration version.

**Final acceptance**

- [x] Candidate registration/login, onboarding, course generation, lessons, review, progress, quests, goals, profile operations and admin workflows all pass.
- [x] Two users can use the app concurrently with isolated data and consistent rewards.
- [x] Existing synthetic/imported data survives migration and application restarts.
- [x] Backend and frontend build and run independently and communicate through the documented HTTP API.
- [x] A clean installation and a migrated installation both reach a working final application.

## 6. Required test organization

| Suite | What it proves | Execution |
| --- | --- | --- |
| Service unit tests | Scoring, transitions, goal calculations, date boundaries, permission decisions | Every change |
| MySQL repository tests | SQL behavior, foreign keys, migrations, rollback, locking, duplicate constraints | Backend CI |
| HTTP integration tests | Authentication, CSRF, DTOs, errors, permissions, ownership, idempotency | Backend CI |
| Fake-provider tests | Generation and grading against slow, malformed, partial and failed AI responses | Backend CI |
| Desktop API tests | JSON mapping, cookies, errors, retries and response cancellation | Desktop CI |
| JavaFX smoke tests | FXML wiring, navigation, controls, loading/error states | Windows test runner |
| End-to-end tests | Complete candidate and admin flows through HTTP with real MySQL and deterministic AI | Feature gates and release |
| Real-provider acceptance | Provider credentials, current model compatibility and usable generated content | Controlled release check |
| Packaging acceptance | Installer, bundled runtime and configurable remote backend | Release |

CI provisions a disposable MySQL container service for the repository and HTTP integration suites. Surefire runs these suites during `verify`, with the JavaFX journey as an explicit opt-in profile. Local runs can use a separate disposable MySQL instance or the default H2 test database. Record an unavailable integration environment as an incomplete check. [Spring Boot Testcontainers support](https://docs.spring.io/spring-boot/reference/testing/testcontainers.html)

The repository should make these Windows commands work after the corresponding setup tasks. Define the `ui-tests` profile and document its desktop-session requirement before using it in CI.

```powershell
# Full automated build, including configured backend integration tests
.\mvnw.cmd clean verify

# Backend and its required modules
.\mvnw.cmd -pl backend -am verify

# Desktop API and unit tests
.\mvnw.cmd -pl desktop -am test

# JavaFX smoke checks on a Windows desktop test runner
.\mvnw.cmd -pl desktop -am verify -Pui-tests

# Package both applications before the installer/release checks
.\mvnw.cmd package
```

Use deterministic AI fixtures for automated acceptance. Reuse the same synthetic users and course data in the baseline and migrated app when comparing outcomes. Failure-injection tests should assert persisted state as well as the returned HTTP response.

Write tests in an obvious arrange, act, assert sequence with descriptive names such as `claimingTheSameMonthlyRewardTwiceAwardsXpOnce`. Prefer a few explicit synthetic users and questions that make the expected outcome easy to calculate. Use ordinary JUnit assertions, focused Mockito mocks when an external dependency must be controlled, and direct HTTP tests against Spring Boot for endpoint behavior. Keep test setup small and visible. Explain the scheduling steps in concurrency tests and use synchronization primitives to coordinate requests reliably.

## 7. Migration details that affect correctness

- Preserve legacy XP rows and timestamps. Adding a unique user/course key to the existing progress table would change its effective event-history behavior and affect streaks and charts.
- Record historical question status as imported state. The original database cannot reconstruct every past answer or AI score. New attempts begin collecting that detail after migration.
- Make transaction ownership explicit. Provider calls happen outside a transaction, while final related database writes run through a Spring-managed service transaction. A transaction must be invoked through the managed service boundary so annotation handling applies.
- Move JavaFX callbacks out of backend services. Generation services report domain progress into persisted jobs, and desktop polling converts that progress into UI updates.
- Make the backend derive user identity and check ownership on every protected resource. Background workers use the authenticated job owner captured at submission.
- Store a completed attempt result before responding. Repeated HTTP requests then retrieve the recorded result and reward instead of executing the operation again.
- Persist upload/job references and define cleanup on success, failure and account deletion. Bind file access to the upload owner.
- Verify API failure handling before migrating the next screen. Backend unavailability should preserve entered form/answer data where possible and allow a meaningful retry.
- Keep source credit and code provenance visible in the new repository. The clean snapshot approach avoids copying previously exposed credentials through Git history.

## 8. Review checkpoint template

Copy this checklist into each feature migration pull request or work log.

- [x] Source classes and reusable behavior are listed.
- [x] A junior engineer can explain the feature from the JavaFX action through the API to its database result.
- [x] Names, method order, ordinary control flow and explicit constructors make the code easy to read and debug.
- [x] Spring annotations come from the documented common set, with any additions explained where introduced.
- [x] Each new helper, interface, dependency or abstraction solves a specific current problem.
- [x] Request and response DTOs, SQL mapping and exception handling are explicit and easy to inspect.
- [x] API request, response and error behavior are documented.
- [x] Backend controller, service and repository responsibilities are clear.
- [x] Ownership, permissions and transaction behavior are tested.
- [x] Desktop calls the API and remains responsive during delays.
- [x] Migrated functional scenarios pass with recorded evidence, and limits of the original reference comparison are documented.
- [x] Retry, failure and application-restart behavior are covered where relevant.
- [x] Intentional behavior corrections are documented.
- [x] Build and relevant tests pass on the pinned versions.
- [x] The migrated feature works through its normal JavaFX entry point.
