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
