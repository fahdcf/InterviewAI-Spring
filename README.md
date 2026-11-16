# InterviewAI Spring

InterviewAI is a desktop interview preparation app. This repository contains the migration to a JavaFX desktop client and a Spring Boot REST API.

## Built by

| [Fahd Chafai](https://github.com/fahdcf) | [Hassan El Khatoury](https://github.com/hassan-elkhatoury) |
| :---: | :---: |
| <a href="https://github.com/fahdcf"><img src="https://avatars.githubusercontent.com/u/132993312?v=4&amp;s=160" width="80" height="80" alt="Fahd Chafai on GitHub"></a> | <a href="https://github.com/hassan-elkhatoury"><img src="https://avatars.githubusercontent.com/u/128185741?v=4&amp;s=160" width="80" height="80" alt="Hassan El Khatoury on GitHub"></a> |

## First milestone

The desktop shows the configured backend address and checks it through `GET /api/v1/health`. A JavaFX task runs the request off the UI thread. The screen shows the result and offers a retry. Sign-in and the database-backed features are subsequent milestones.

- `backend/` contains the Spring Boot entry point and REST controllers. Services, repositories and models will live here as features move over.
- `desktop/` contains the JavaFX screen, its controller and the HTTP client.
- The `legacy-baseline` tag identifies the complete original source snapshot. See the [migration plan](MIGRATION_PLAN.md) and [legacy acceptance checklist](docs/legacy-acceptance.md).

The baseline matches [InterviewAI at b5aebcf](https://github.com/fahdcf/InterviewAI/tree/b5aebcf4d1263978a36a58d8b9f48ae00364ea02).

## Run locally

Install JDK 21. The Maven wrapper downloads the pinned Maven version and dependencies on first use.

In one PowerShell window, start the backend.

```powershell
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

## Verify the build

```powershell
.\mvnw.cmd clean verify
```

Tests exercise the backend over real HTTP and check how the desktop client handles ready, unavailable and malformed responses.
