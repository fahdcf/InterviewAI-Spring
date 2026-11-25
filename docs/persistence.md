# Backend persistence

The Spring Boot backend uses Spring Data JPA and Hibernate. Controllers and the JavaFX client still work with the existing REST DTOs. Services own the business rules and transaction boundaries. Repositories extend `JpaRepository` and use its built-in operations and derived queries for ordinary reads and writes.

Flyway owns the MySQL schema and its V1–V8 migrations. Hibernate starts with `spring.jpa.hibernate.ddl-auto=validate` and checks the resulting schema. Hibernate sessions close at the service or repository boundary. Queries return DTOs, projections, or entities whose fields the service maps before returning a response.

## Entities and repositories

| Table | Entity | Repository |
| --- | --- | --- |
| `users` | `UserAccount` | `AccountRepository` |
| `roles` | `Role` | `RoleRepository` |
| `permissions` | `Permission` | Read through roles |
| `generated_courses` | `Course` | `CourseRepository` |
| `chapters` | `Chapter` | `ChapterRepository` |
| `questions` | `Question` | `QuestionRepository` |
| `choices` | `Choice` | `ChoiceRepository` |
| `interview_prep` | `InterviewPrep` | `InterviewPrepRepository` |
| `onboarding_data` | `OnboardingData` | `OnboardingRepository` |
| `onboarding_cv` | `OnboardingCv` | `OnboardingCvRepository` |
| `progress` | `ProgressEvent` | `ProgressRepository` |
| `feedback` | `Feedback` | `FeedbackRepository` |
| `user_goals` | `UserGoal` | `GoalRepository` |
| `course_generation_jobs` | `GenerationJob` | `CourseJobRepository` |
| `practice_attempts` | `AnswerAttempt` | `PracticeRepository` |
| `quest_claims` | `QuestClaimRecord` | `QuestRepository` |

Mappings live in `model.entity`. `user_roles` and `role_permissions` are mapped as lazy join tables. Course content has lazy references from choice to question, question to chapter, and chapter to course. Other foreign keys are represented by ID fields and enforced by Flyway's constraints. In particular, `generated_courses.user_id` can reference a missing owner in an older dataset because the schema has no foreign key for that column. Admin course queries use a left join to include those records. Database defaults supply creation timestamps and the existing foreign keys handle cascades and `SET NULL` operations.

The account, course, generation-job, onboarding, practice, progress and quest repositories were converted to Spring Data interfaces. The previous dashboard, generated-course and admin JDBC repositories were replaced by queries and writes on the corresponding entity repositories, with the existing services coordinating each operation.

Account and goal flags use Java booleans with the existing `TINYINT` column type. The MySQL driver reports those columns as numeric so Hibernate can validate them in the same way as H2. Dates used for XP and quests retain the existing UTC database session and application time zone.

The candidate dashboard and practice views load counts, choices, and attempts in sets. Admin user pages read all roles for that page with one additional query.

## Transactions and locking

- `AccountRepository.lockById` takes a `PESSIMISTIC_WRITE` lock on the same user row that the JDBC implementation locked. The lock stays held until the enclosing transaction commits or rolls back. Generation submission, grading, goal and quest writes, profile changes, and administrator operations reuse that lock.
- The per-user request ID constraints for course jobs, answer attempts, and quest claims stay in the schema. Services check for replays while holding the user lock. Grading saves the question, chapter, course and XP changes in the same transaction as the attempt result. Quest redemption similarly commits the claim, reset date and XP together.
- Generation and grading workers use guarded updates and check the affected row count before starting queued work. Completion updates require a running row. The generation save transaction writes both course trees, the pair and navigation before marking the job successful. A failed completion check rolls the whole transaction back.
- At startup the workers mark previously running jobs and attempts as interrupted or failed. Queued work stays eligible for processing. As before, deploy one backend process per database so startup recovery treats only work from the stopped process as interrupted.
- Bulk changes to an entity that may already be managed flush pending writes and clear managed state where a later read could otherwise return stale values. Clearing the persistence context leaves the database transaction and its locks in place.

## Explicit queries

Derived methods handle simple lookups, existence checks, queues, counts and sorted lists. The following query groups use JPQL or Hibernate's HQL extensions to express joins, projections, aggregation, and atomic writes.

| Repository | Methods | Why they use explicit queries |
| --- | --- | --- |
| `AccountRepository` | `lockById`, `findRoles`, `findPermissions`, `findAdminUsers`, `findAdminUser`, `findRolesForUsers`, `countNewAccounts` | Lock a user row, traverse role memberships, paginate filtered account summaries, load roles in one batch and group signups by date. |
| `AccountRepository` | `upgradePasswordHash`, `updateUsername`, `changePassword`, `saveLocation`, `recordDailyReset`, `recordMonthlyClaim`, `dailyReset`, `monthlyClaimDate` | Compare the old hash atomically, increment session versions in the database, and read or write specific account fields. |
| `RoleRepository` | `findNames` | Return just the sorted role names. |
| `CourseRepository` | `findAvailableCourses`, `findAvailableCourse`, `findAdminCourses`, `findAdminCourse` | Preserve nullable statuses, check ownership, and paginate summaries with owner and content counts. |
| `CourseRepository` | `completeIfReady`, `reopenCompleted`, `setCourseStatus`, `deleteByOwner`, `deleteCourse` | Change or delete course rows in place and keep the chapter-completion check inside the same update. |
| `ChapterRepository` | `findChapters`, `ownsChapter`, `completeChapter`, `startChapter`, `resetProgress` | Load chapter aggregates and check course visibility. Update chapter status while preserving the first completion time, or reset an owner's chapters in bulk. |
| `QuestionRepository`, `ChoiceRepository` | `listQuestions`, `listCourseReview`, `findQuestion`, `findCourseChoices`, `findChoices` | Project questions with archive and ownership rules and read only the non-null choice text in display order. |
| `PracticeRepository` | `latestAttempts`, `start`, `fail`, `recoverInterruptedAttempts`, `complete`, `deleteForUser` | Select the latest attempt per question, enforce valid state transitions and remove attempt history in one statement. |
| `CourseJobRepository` | `findInput`, `start`, `saveProgress`, `succeed`, `stop`, `recoverInterruptedJobs` | Read the saved input and update job state atomically, including the original recovery and CV cleanup behavior. |
| `ProgressRepository` | `findTotalXp`, `averageXp`, `findActivityTimes`, `leaderboard`, `countDailyUsage`, `resetXp`, `deleteForUser` | Read selected progress fields, aggregate analytics and leaderboard values, and reset or delete multiple XP rows together. |
| `GoalRepository` | `updateGoal`, `removeGoal`, `resetProgress` | Scope edits to an active goal owned by the user and reset goal baselines in bulk. |
| `OnboardingRepository`, `OnboardingCvRepository` | `find`, `countInterviewTypes`, `countLanguages`, `removeCv` | Join preference and CV availability data, group analytics, and delete a CV by owner ID. |
| `FeedbackRepository` | `deleteByUserOrOwnedCourse`, `deleteForCourse` | Remove related feedback before deleting a course or account. |

The remaining native statements preserve database operations with behavior the app relies on.

| Repository method | Why native SQL remains |
| --- | --- |
| `OnboardingRepository.savePreferences`, `OnboardingCvRepository.saveCv` | MySQL's `ON DUPLICATE KEY UPDATE` makes each write atomic for both new and returning users. The writes preserve existing columns such as a legacy CV path. |
| `CourseJobRepository.queue` | `INSERT … SELECT` copies preferences and CV text into the job in one statement, using the database's job defaults. |
| `QuestionRepository.updateStatus`, `QuestionRepository.resetProgress` | Normal grading explicitly uses `CURRENT_TIMESTAMP(6)` so multiple daily quest cycles can work inside one second. Admin reset uses the original `CURRENT_TIMESTAMP` precision. |

## Verify locally

Run `./mvnw clean verify` for the H2-backed Maven build. Set `TEST_DB_URL`, `TEST_DB_USER`, and `TEST_DB_PASSWORD` to use a disposable MySQL 8.4 schema with the same backend tests. Use `-Pui-tests` in an interactive desktop session to verify the JavaFX views. [Database upgrade instructions](database-upgrade.md)

A compatibility check opened a populated MySQL 8.4.13 V8 database with the packaged JPA backend. All 22 candidate and administrator read routes checked returned the same parsed JSON as the released JDBC backend. The complete MySQL dumps matched byte for byte, including the Flyway history. The fixture included accounts and roles, courses and questions, a CV, an answer attempt, a generation job, XP, a goal, a quest claim and feedback.

The JavaFX candidate and administrator journeys passed against the packaged backend using MySQL and local provider fixtures. A separate JavaFX test reopened the candidate after a backend restart and verified the saved 1,290 XP, chapter completion, review state and goal. The desktop tests used their original assertions and time limits.

The H2 2.3.232 Maven build passed with 36 offline backend tests and 10 offline desktop tests. The MySQL 8.4.13 Maven build passed with 35 offline backend tests and 10 offline desktop tests. The two administrator tests were then run against MySQL to verify the explicit search escaping, including a literal underscore in user and course searches.

Live Gemini and OpenRouter acceptance and remote deployment acceptance were skipped by request.
