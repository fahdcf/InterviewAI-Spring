# InterviewAI Spring

InterviewAI is a desktop interview preparation app. This repository contains the migration to a JavaFX desktop client and a Spring Boot REST API.

## Built by

| [Fahd Chafai](https://github.com/fahdcf) | [Hassan El Khatoury](https://github.com/hassan-elkhatoury) |
| :---: | :---: |
| <a href="https://github.com/fahdcf"><img src="https://avatars.githubusercontent.com/u/132993312?v=4&amp;s=160" width="80" height="80" alt="Fahd Chafai on GitHub"></a> | <a href="https://github.com/hassan-elkhatoury"><img src="https://avatars.githubusercontent.com/u/128185741?v=4&amp;s=160" width="80" height="80" alt="Hassan El Khatoury on GitHub"></a> |

## Current milestone

The JavaFX client can register, sign in by username or email, and sign out. Candidates can browse their courses, preview chapters, see XP and streaks, and save the chapter they want to return to after signing in again. **Interview preferences** saves interview details and an optional PDF, DOCX, or DOC CV. The backend keeps up to 4,000 characters of extracted text until the user replaces or removes it. Course generation and answering questions are upcoming steps in the migration plan.

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

## Verify the build

```powershell
.\mvnw.cmd clean verify
```

The default test run uses a disposable in-memory database for backend HTTP integration tests. It covers registration, duplicate identifiers, CSRF, sessions, suspended accounts, roles and legacy password upgrades. Desktop tests cover connection responses.

To run the backend tests against a separate MySQL database, configure `TEST_DB_URL`, `TEST_DB_USER` and `TEST_DB_PASSWORD`, then run `.\mvnw.cmd -pl backend test`. Use a disposable database since the suite inserts test accounts and changes their roles and status.
