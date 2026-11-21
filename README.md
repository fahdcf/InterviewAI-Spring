# InterviewAI Spring

InterviewAI is a desktop interview preparation app. This repository contains the migration to a JavaFX desktop client and a Spring Boot REST API.

## Built by

| [Fahd Chafai](https://github.com/fahdcf) | [Hassan El Khatoury](https://github.com/hassan-elkhatoury) |
| :---: | :---: |
| <a href="https://github.com/fahdcf"><img src="https://avatars.githubusercontent.com/u/132993312?v=4&amp;s=160" width="80" height="80" alt="Fahd Chafai on GitHub"></a> | <a href="https://github.com/hassan-elkhatoury"><img src="https://avatars.githubusercontent.com/u/128185741?v=4&amp;s=160" width="80" height="80" alt="Hassan El Khatoury on GitHub"></a> |

## Features

The JavaFX client supports registration, sign-in, sign-out, course browsing, chapter selection, XP and streaks. **Interview preferences** saves interview details and an optional PDF, DOCX, or DOC CV. **Generate courses** starts a background job that produces one technical and one soft-skills course. **Generation status** reopens progress after leaving the waiting screen. The backend saves both courses together after validating every chapter and question.

Open a lesson from a selected chapter to practice multiple-choice and short-answer questions. **Review this chapter** and **Review this course** show incorrect questions. The backend grades multiple-choice answers locally and uses OpenRouter to grade short answers. **Explain answer** opens an explanation after grading. A passing lesson answer earns 10 XP once per question. Review updates question status.

**Progress and quests** displays the seven-day XP chart, estimated practice time, editable goals, leaderboard, and quest countdowns. A daily reward grants 900 XP after 20 answers and starts a new progress cycle. The monthly reward grants 30,000 XP after 12 completed chapters. The backend checks and awards each reward in one transaction. Set `APP_TIME_ZONE` to change the calendar used for streaks and quests. It defaults to `Africa/Casablanca`.

**Profile** lets candidates and administrators change their username and password. Changing a password ends existing sessions. Candidates can delete their own account after confirming the password.

Administrators can review usage and sign-up trends, filter users and courses, manage user access, reset XP or lesson progress, and archive or delete courses. Spring Security checks permissions on each administrative endpoint.

- `backend/` contains the Spring Boot controllers, account service, JDBC repository, security configuration and Flyway migrations.
- `desktop/` contains the JavaFX screens, controllers and HTTP client.
- The `legacy-baseline` tag identifies the complete original source snapshot. See the [migration plan](MIGRATION_PLAN.md) and [legacy acceptance checklist](docs/legacy-acceptance.md).

The baseline matches [InterviewAI at b5aebcf](https://github.com/fahdcf/InterviewAI/tree/b5aebcf4d1263978a36a58d8b9f48ae00364ea02).

## Run locally

Install JDK 21 and start a MySQL 8.4 development database. Create an empty schema named `interviewai_spring` and grant the backend's database user access to it. Flyway creates the tables and reference roles when the backend first starts. For an existing database, follow the [database upgrade instructions](docs/database-upgrade.md).

In PowerShell, set the database credentials and start the backend.

```powershell
$env:DB_URL = 'jdbc:mysql://127.0.0.1:3306/interviewai_spring'
$env:DB_USER = 'interviewai'
$env:DB_PASSWORD = Read-Host 'MySQL password' -MaskInput
# Optional for browsing existing courses. Required to generate new ones.
$env:GEMINI_API_KEY = Read-Host 'Gemini API key' -MaskInput
# Required for short-answer grading.
$env:OPENROUTER_API_KEY = Read-Host 'OpenRouter API key' -MaskInput
.\mvnw.cmd -pl backend spring-boot:run
```

Verify the response.

```powershell
Invoke-RestMethod http://127.0.0.1:8080/api/v1/health
```

In a second PowerShell window, launch the desktop in an interactive Windows session.

```powershell
.\mvnw.cmd -pl desktop javafx:run
```

The desktop defaults to `http://127.0.0.1:8080`. Set `INTERVIEWAI_API_URL` in the desktop terminal to choose a different server. Use the server origin, including the port if needed. To change the local backend port, set `SERVER_PORT` in the backend terminal.

Choose **Create account**, then sign in with the username or the optional email. New accounts have the candidate role. Imported administrator roles and user-management permissions are recognized at sign-in.

To provision the first administrator in a development database, register the account in the desktop app. In MySQL, assign the existing `SUPER_ADMIN` role to that specific username, then sign in again.

```sql
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u CROSS JOIN roles r
WHERE u.username = 'your-registered-username' AND r.name = 'SUPER_ADMIN';
```

The backend sends interview preferences and up to 4,000 characters of saved CV text to Gemini for generation. You can remove the saved CV in **Interview preferences**. The default endpoint uses Gemini 3.5 Flash. Set `GEMINI_API_URL` to use a different supported GenerateContent endpoint. Run one backend process per database so it can recover interrupted generation jobs on startup. [Gemini API documentation](https://ai.google.dev/api/generate-content)

Short-answer grading and multiple-choice explanations send the question, expected answer and your answer to OpenRouter. Configure the model with `OPENROUTER_MODEL` and the endpoint with `OPENROUTER_API_URL`. The default model is `openai/gpt-4.1-mini`. [OpenRouter chat completions](https://openrouter.ai/docs/api/api-reference/chat/send-chat-completion-request)

## How the two apps work together

- **Sign-in** uses Spring Security to check the account and BCrypt password. JavaFX keeps the session cookie in memory and fetches a CSRF token from the backend before each write.
- **Course generation** saves a job and returns its ID. JavaFX checks the job for progress while the backend requests and validates both courses. The backend writes the pair together when both are ready.
- **Answer submission** uses a request ID so retrying the same submission returns the existing attempt. Multiple-choice scores return immediately. JavaFX polls for short-answer grades, and the backend saves the result, progress and any XP together.
- **Quest claims** lock the candidate's account while checking eligibility and saving the reward. Concurrent requests for the same reward can award XP only once.

## Verify the build

```powershell
.\mvnw.cmd clean verify
```

The default test run uses a disposable in-memory database and local Gemini and OpenRouter fixtures. The backend HTTP suite covers accounts, sessions, course generation, lessons, review, progress, quest rewards, and administration. Desktop tests cover connection responses.

To run the backend tests against a separate MySQL database, configure `TEST_DB_URL`, `TEST_DB_USER` and `TEST_DB_PASSWORD`, then run `.\mvnw.cmd -pl backend test`. Use a disposable database since the suite inserts test accounts and changes their roles and status.

Run `.\mvnw.cmd -pl desktop -Pui-tests test` from an interactive desktop to check the JavaFX views. To build the Windows desktop ZIP and runnable backend JAR, run `.\scripts\package-windows.ps1`. See the [migration status](docs/migration-status.md) and [release checklist](docs/release-checklist.md) for verification and remaining release checks.

The Windows verification used Temurin JDK 21.0.11, Spring Boot 3.5.16, JavaFX 21.0.11, and MySQL 8.4.13. Flyway applies migrations through V7.
## API contract

The backend serves its OpenAPI JSON at `/v3/api-docs` and YAML at `/v3/api-docs.yaml`. The contract includes request and response DTOs, multipart uploads, page parameters, session and CSRF requirements, errors, and job states. Read it alongside the controller methods when implementing a desktop feature. A backend test compares documented routes with the registered controllers.
