# InterviewAI Spring

InterviewAI is a desktop interview preparation app. This repository contains the migration to a JavaFX desktop client and a Spring Boot REST API.

## Built by

| [Fahd Chafai](https://github.com/fahdcf) | [Hassan El Khatoury](https://github.com/hassan-elkhatoury) |
| :---: | :---: |
| <a href="https://github.com/fahdcf"><img src="https://avatars.githubusercontent.com/u/132993312?v=4&amp;s=160" width="80" height="80" alt="Fahd Chafai on GitHub"></a> | <a href="https://github.com/hassan-elkhatoury"><img src="https://avatars.githubusercontent.com/u/128185741?v=4&amp;s=160" width="80" height="80" alt="Hassan El Khatoury on GitHub"></a> |

## Current milestone

The JavaFX client supports registration, sign-in, sign-out, course browsing, chapter selection, XP and streaks. **Interview preferences** saves interview details and an optional PDF, DOCX, or DOC CV. **Generate courses** starts a background job that produces one technical and one soft-skills course. **Generation status** reopens progress after leaving the waiting screen. The backend saves both courses together after validating every chapter and question.

Open a lesson from a selected chapter to practice multiple-choice and short-answer questions. **Review this chapter** and **Review this course** show incorrect questions. The backend grades multiple-choice answers locally and uses OpenRouter to grade short answers. Feedback and explanations are displayed after an attempt. A passing lesson answer earns 10 XP once per question. Review updates question status.

**Progress and quests** displays the seven-day XP chart, estimated practice time, goals, and leaderboard. A daily reward grants 900 XP after 20 answers and starts a new progress cycle. The monthly reward grants 30,000 XP after 12 completed chapters. The backend checks and awards each reward in one transaction. Set `APP_TIME_ZONE` to change the calendar used for streaks and quests. It defaults to `Africa/Casablanca`.

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

The backend sends interview preferences and up to 4,000 characters of saved CV text to Gemini for generation. You can remove the saved CV in **Interview preferences**. The default endpoint uses Gemini 3.5 Flash. Set `GEMINI_API_URL` to use a different supported GenerateContent endpoint. Run one backend process per database so it can recover interrupted generation jobs on startup. [Gemini API documentation](https://ai.google.dev/api/generate-content)

Short-answer grading sends the question, expected answer and your answer to OpenRouter. Configure the model with `OPENROUTER_MODEL` and the endpoint with `OPENROUTER_API_URL`. The default model is `openai/gpt-4.1-mini`. [OpenRouter chat completions](https://openrouter.ai/docs/api/api-reference/chat/send-chat-completion-request)

## Verify the build

```powershell
.\mvnw.cmd clean verify
```

The default test run uses a disposable in-memory database and a local Gemini test server for backend HTTP integration tests. It covers accounts, sessions, course browsing, onboarding, generation, and generation retry. Desktop tests cover connection responses.

To run the backend tests against a separate MySQL database, configure `TEST_DB_URL`, `TEST_DB_USER` and `TEST_DB_PASSWORD`, then run `.\mvnw.cmd -pl backend test`. Use a disposable database since the suite inserts test accounts and changes their roles and status.
