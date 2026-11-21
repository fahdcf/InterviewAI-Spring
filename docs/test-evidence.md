# Test evidence

Run the normal suites from the repository root with JDK 21. The wrapper runs tests during `verify`. The MySQL CI job uses a disposable MySQL 8.4.13 service. The default local suite uses H2. Maven Enforcer checks both module dependency boundaries.

| Concern | Evidence |
| --- | --- |
| Authentication, permissions, ownership and sessions | `AuthControllerTest` covers login/logout, CSRF, suspended accounts, imported SHA-256 hashes, simultaneous clients and separate progress. |
| Onboarding and uploads | `AuthControllerTest` covers multipart save, optional CV, replacement/removal and persisted preference fields. `CvTextExtractorTest` covers PDF, DOCX tables, DOC, corrupt, empty and oversized inputs and Unicode truncation. |
| Complete course generation | `AuthControllerTest` generates two 12-chapter courses containing 960 questions, adds a second pair, and rejects incomplete choices and unavailable models. |
| Database atomicity | A nested second-course insert failure rolls back both courses. A removed answer attempt makes stale grading roll back question, chapter, course and XP changes. |
| Provider boundary | `ProviderHttpClientTest` covers bounded retries, Retry-After, fenced JSON, incomplete output, invalid scores and request timeouts. The opt-in real-provider suite is skipped by request. |
| Practice and review | `AuthControllerTest` covers both question types, 69/70 passing threshold, request replay, concurrent XP deduplication, review without lesson XP and explanation eligibility/errors. |
| Restart recovery | `RestartRecoveryTest` closes and reopens the application context against the same database. Queued work finishes, running work becomes interrupted/failed, and the saved answer survives. |
| Calendar and goals | `CalendarProgressTest` covers six goal types, pre-existing progress, absolute streak goals, daily cycles in one second, claim rollback, monthly rollover, empty/gapped streaks and Casablanca offset changes. |
| Administration | `AuthControllerTest` checks pagination, role limits, separate XP/progress resets, account deletion with related data, archive, restore, deletion and analytics values. |
| Desktop HTTP | `HealthApiTest` covers typed JSON, future optional fields, 401/403/409/500/503, malformed and empty responses, disconnects and request timeout. |
| Frontend lifecycle | `DesktopViewsTest` verifies discarded callbacks, every migrated FXML view, scroll behavior, review, explanations and resuming generation at sign-in. |
| Full desktop journey | The `interviewai.journey` test registers a candidate, generates courses, completes 40 questions, reviews, claims a quest, edits a goal, signs in again, then exercises administrator actions. CI uses real MySQL and a separate packaged Spring Boot process with provider fixtures. |
| HTTPS deployment | The same candidate journey passed against the Docker Compose deployment with a trusted local CA and Secure session cookie. A separate client container reached its health endpoint over HTTPS. |
| Persistence after upgrade/restart | The `interviewai.restart.journey` test reopened a saved candidate after backend/database containers were recreated and after the V8 upgrade. XP, chapter completion, goal and review status survived. |
| Packaging | The Windows CI builds the ZIP and JAR. A fresh runner downloads that ZIP and opens the sign-in window with external Java off PATH, verifying its loaded JVM belongs to the extracted application. |
| Original comparison | [Reference run](legacy-acceptance.md), screenshots and [FXML control inventory](ui-inventory.md) record working rules, placeholder views, layout differences and deliberate repairs. |

## Readability walkthrough

- Sign-in starts in `LoginController`, calls `AuthApi` and `AuthController`, then uses Spring Security and `AccountService`/`AccountRepository`. The desktop's cookie manager retains the session.
- Generation starts in `OnboardingController`. `CourseJobService` saves the input snapshot. The scheduled worker reads a queued row, calls the two provider workflows, and invokes `SaveGeneratedCourses` to save both trees in one transaction.
- Answer submission starts in `PracticeController` and `PracticeApi`. The backend `PracticeService` checks ownership and request ID. Multiple-choice grading completes locally, while a saved short-answer attempt is picked up by `AnswerGradingWorker`. The final transaction updates attempt, question, chapter, course and XP.
- Quest claiming starts in `ProgressController`/`ProgressApi`. `QuestService` locks the account, checks the calendar/cycle and prior request, then stores the marker, claim and XP in one transaction.

The repository uses concrete classes, constructor injection, common Spring annotations, explicit SQL and a single backend instance. `ScreenController` provides task disposal shared by all frontend screens. `ProviderHttpClient` shares bounded retry handling between the two provider clients. Small immutable Java records represent response data where no setters are needed.

## Implementation choices

- Uploaded CVs are extracted in the owning multipart request. The backend stores bounded user-owned text, so the desktop does not exchange separate upload IDs or local filesystem paths.
- Two scheduled backend workers each process one persisted item at a time. The database holds the queue and per-user limits. The desktop executor has a bounded queue of 16 tasks.
- The shared practice screen handles lessons and review. The navigator builds each controller with explicit dependencies and disposes it before activating another screen.
- Surefire runs the repository and HTTP integration suites together during `verify`. CI supplies its disposable MySQL service directly.
- The application uses reorganized controls and shared styling. Source inventory, representative original captures and automated migrated journeys provide the recorded parity evidence. The full manual exercise of every original control is unverified.
