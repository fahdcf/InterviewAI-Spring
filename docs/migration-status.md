# Migration status

The JavaFX desktop and Spring Boot backend are published as [version 1.0.0](https://github.com/fahdcf/InterviewAI-Spring/releases/tag/v1.0.0). The exact source revision, workflow and package hashes are in the [release record](releases/1.0.0.md). Live-provider and remote-server acceptance are skipped at the user's request on 21 November 2026.

| Plan group | Result | Verification |
| --- | --- | --- |
| A–B | Pinned source, separate repository, source inventory and original-screen comparison | Original auth and navigation queried on MySQL. Seventeen original views opened and representative screens were captured. [Reference run](legacy-acceptance.md) and [control inventory](ui-inventory.md). |
| C–E | Independent builds, structured frontend, screen disposal and OpenAPI | Dependency rules reject cross-module implementation dependencies. JavaFX tests cover screen lifecycle and unknown JSON fields. OpenAPI tests compare controller routes and security. |
| F–G | Versioned MySQL schema and transactional repositories | Flyway V1–V8, nested insert rollback, upgrade/restore rehearsal and repeated startup. [Upgrade record](database-upgrade.md). |
| H–I | Sessions, roles, dashboard, course selection and saved navigation | HTTP tests cover legacy hashes, CSRF, invalidated sessions, simultaneous users, owned IDs and archived/deleted courses. Fresh candidates open onboarding. |
| J–M | CV upload, course generation, durable jobs and reconnect | PDF, DOCX, Word tables, DOC, corrupt/empty/oversized files, bounded retries, both complete courses, second pair, invalid provider data and restart recovery. Login reopens unfinished generation. |
| N–O | Lessons, short-answer grading, review and explanations | Passing threshold, repeat and concurrent answers, once-only XP, restored unfinished answers, provider outage, deleted lessons and stale grading after a reset. |
| P–Q | XP, streaks, all goal types, quests and countdowns | Historical XP, absolute streak goals, all goal baselines, Casablanca calendar transitions, next daily cycle within one second, monthly rollover and failed-claim rollback. |
| R–T | Profiles, user/course administration and analytics | Identity and password updates, dependent-data deletion, filtering, pagination, suspension, separate XP/progress resets, course archive/restore/delete and summary statistics. |
| U–V | Frontend cleanup and automated acceptance | JavaFX smoke tests load every migrated view. The candidate and admin journeys run against a packaged backend and MySQL in CI. [Test matrix](test-evidence.md). |
| W | Local HTTP and HTTPS boundary verified | Live providers and remote-server acceptance are skipped at the user's request. Local tests use deterministic providers. |
| X | Windows app with bundled runtime, Spring Boot JAR and HTTPS deployment | A fresh Windows runner downloads and launches the package with Java removed from PATH. The local Docker deployment passes the candidate journey. [Deployment](deployment.md). |
| Y | Upgrade, backup and restore rehearsal | V8 upgrade preserves the candidate's progress. Separate MySQL restore matches users, courses, questions, XP and Flyway history. |
| Z | Versioned build and release packaging | Release packages include the desktop ZIP, backend JAR and checksums. The release tag identifies their source revision. |

The frontend uses reorganized layouts with the original dark and green palette. The original simulation and feedback controllers are placeholders, and the original prompt FXML fails to load. Those unfinished workflows are recorded in the reference inventory. This release verifies the working interview preparation flows through the migrated JavaFX entry points.

The original comparison combines source review, data queries and screen loading. A full manual exercise of every original control was not performed. The actual database and hosting environment for a production cutover have not been supplied. Local synthetic-data migration, restore and HTTPS deployment have been rehearsed.
