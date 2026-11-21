# Legacy acceptance checklist

The `legacy-baseline` tag contains the Git tree from [InterviewAI at b5aebcf](https://github.com/fahdcf/InterviewAI/tree/b5aebcf4d1263978a36a58d8b9f48ae00364ea02). The snapshot compiles and packages with JDK 21. Maven reports zero legacy tests. The prior offline review loaded the login, registration and onboarding FXML. The live legacy checks below are pending a disposable MySQL instance and provider configuration.

Use synthetic accounts, courses and CVs. Record the actual result and any existing defects, then repeat each check against the migrated app after the corresponding stage in the [migration plan](../MIGRATION_PLAN.md).

| ID | Setup and action | Expected behavior to confirm |
| --- | --- | --- |
| AUTH-01 | Register a candidate. Try username and email sign-in, wrong credentials and a suspended account. | Valid credentials open the candidate flow. Invalid and suspended accounts get a visible error. |
| AUTH-02 | Sign in as an admin, sign out, then sign in as a candidate. | Each account gets the right screen and its own navigation state. |
| COURSE-01 | Onboard with and without an uploaded CV. | The intended flow shows progress and saves technical and soft-skills courses. The optional-CV failure and retired default model are legacy defects. |
| COURSE-02 | Use synthetic PDF, DOCX and DOC uploads in separate runs. | Text extraction works and generated chapters, questions and choices belong to the right course. |
| COURSE-03 | Switch courses and chapters, then close and reopen the app. | The last-used course and chapter are restored. |
| COURSE-04 | Create a second course pair from the dashboard. | Both pairs remain accessible. |
| PRACTICE-01 | Submit a correct and an incorrect multiple-choice answer. | Status, feedback and XP follow the existing rules. |
| PRACTICE-02 | Submit short answers with scores below and at 70. | Score and feedback appear. A passing answer updates status and XP. |
| PRACTICE-03 | Complete a chapter and reopen the app. | Completion and course progress persist. |
| REVIEW-01 | Answer incorrectly. Open course and chapter review, request an explanation, then retry correctly. | The question appears in the relevant review list. Record how a correct retry changes status and XP. |
| PROGRESS-01 | Seed XP on several dates, questions in different states and another ranked user. | Charts, streak, accuracy and leaderboard agree with the data. Record the study time estimate. |
| GOAL-01 | Add a goal of each type. Perform a qualifying action, then remove a goal. | The relevant progress changes and the removed goal leaves the active list. Record starting-value behavior. |
| QUEST-01 | Reach 20 answered questions. Claim the daily reward and qualify again. | The claim grants 900 XP and resets progress. Record differences between the dashboard and dedicated quest screen. |
| QUEST-02 | Reach 12 completed chapters in one month and claim. | The claim grants 30,000 XP and stays claimed for that period. |
| PROFILE-01 | Change candidate username and password, then delete a synthetic account. | The new credentials work and deletion removes the intended data. |
| ADMIN-01 | Search, filter and page through users. Suspend, activate and edit a user. | The results, details and account access reflect each action. |
| ADMIN-02 | Reset XP and progress separately on a seeded account. | The candidate dashboard reflects each change. Record the exact reset scope. |
| ADMIN-03 | Search, filter, view, archive, restore and delete a course. | The course list and affected candidate navigation reflect each change. |
| ADMIN-04 | Check admin analytics and change the administrator's name and password. | Analytics agree with fixtures and updated credentials work. |
| UI-01 | Open reachable simulation, feedback, course-map, prompt and password-help views. | Record placeholders and help text so the migration reflects the original behavior. |

For reward checks, record XP before and after the action. Keep the daily quest reset cycle distinct from the monthly period.
## Reference run on 21 November 2026

The pinned original source was compiled and opened against a disposable MySQL 8.4.13 database populated with synthetic accounts and courses. Its authentication DAO accepted username and email sign-in, rejected a wrong password, and restored the saved course/chapter IDs. The original progress DAO returned 40 XP, one answered question and 17 estimated minutes for one incorrect answer plus one positive XP event. These values agree with the migrated calculation.

Seventeen original FXML views loaded with database-backed session data, including candidate lessons, review, progress, quests and profile, and administrator overview, users, courses and profile. The prompt view failed to load because its FXML uses `VBox` without importing it. Simulation has an empty controller, feedback has a TODO controller, and the course map has no database-backed interaction in the captured view. The migrated app retains the working interview preparation flows and password-help message.

The screenshots below use synthetic data. The original and migrated captures have different seeded counts, so compare layout and available controls using their matching scenario data.

| View | Original capture | Migrated capture |
| --- | --- | --- |
| Dashboard | [Original](reference/original-dashboard.png) | [Migrated](reference/migrated-dashboard.png) |
| Progress | [Original](reference/original-progress.png) | [Migrated](reference/migrated-progress.png) |
| Administration | [Original](reference/original-admin.png) | [Migrated](reference/migrated-admin.png) |

The migrated layouts organize controls by feature and use the original dark and green palette. Profile statistics and the admin answered-question and average-XP metrics are included. The original dashboard displays a hard-coded longest streak and level text. The original admin service labels memory and provider-health values as placeholders. The migrated views show the persisted learning statistics.

The reference run verified data queries, sign-in and screen loading. The full action-by-action manual exercise of every original control remains unverified. Migrated candidate and administrator actions are exercised by `DesktopViewsTest` with a packaged backend, MySQL and provider fixtures. Real provider calls are skipped at the user's request.
## Recorded rule choices and corrections

- The dedicated quests screen uses 20 answered questions for 900 XP and 12 completed chapters in a month for 30,000 XP. The legacy `ProgressDAO.getDailyQuests` also defines dashboard descriptions for earning 50 XP, answering five questions and extending a streak, with nominal rewards of 50, 30 and 20 XP. The migrated claim API follows the dedicated quests screen. The original dashboard handler for opening quests only logs a message.
- The estimate adds two minutes per answered question and fifteen minutes per positive XP event. The migrated UI labels it as estimated time.
- Streak goals remain absolute even when imported data contains a nonzero starting value. The other goal types subtract their stored baseline.
- Completing the last chapter marks the course complete in the same transaction. An administrator's progress reset reopens a completed course. Archived courses retain their archive state.
- XP reset sets historical XP amounts to zero while retaining their event dates. Full progress reset also clears question/chapter completion, saved attempts and goal baselines. Existing quest claims and calendar markers remain, so replaying an earlier request cannot grant its reward again.
- V8 preserves subsecond ordering between answers and a daily quest reset. The original second-precision columns could miss newly earned answers in that same second.
- Optional CVs, invalid uploads, provider failures and partial course saves have explicit recovery behavior. Uploaded files are extracted during their owning request and discarded, with only bounded text stored for that user.
