# Release checks

## Build and package

On Windows with JDK 21, run the following from a clean checkout.

```powershell
.\mvnw.cmd clean verify
.\mvnw.cmd -pl desktop -Pui-tests test
.\scripts\package-windows.ps1
```

The packaging script creates a Spring Boot JAR and a ZIP containing the desktop application and its Java runtime. Extract the full ZIP and run `InterviewAI.exe`. The ZIP's directory layout is required by the launcher. The backend runs with `java -jar` and the database and provider variables in the README. [Java packaging tool](https://docs.oracle.com/en/java/javase/21/docs/specs/man/jpackage.html)

## Release acceptance

- [x] Build from a clean checkout and migrate a disposable MySQL schema.
- [x] Re-run backend tests on MySQL with `TEST_DB_URL`, `TEST_DB_USER` and `TEST_DB_PASSWORD` set to a disposable database.
- [x] Load the migrated candidate and administrator views with the JavaFX smoke profile.
- [x] Start the packaged Spring Boot JAR. Extract and start the Windows desktop application using its bundled runtime.
- [x] Exercise the full candidate flow over HTTP on MySQL using local provider fixtures, from registration and course generation through completing a 40-question chapter, reviewing a missed answer, checking XP, and signing out.
- [x] Exercise the candidate flow through the JavaFX application against a separate packaged Spring Boot process, MySQL, and local provider fixtures. Verify both generated courses, all 40 answers in the first chapter, missed-answer review, explanations, a daily reward, a saved goal, and progress after signing out and signing back in.
- [x] Send two requests for the same daily quest reward and verify the account receives one award.
- [ ] Repeat the full candidate flow from the JavaFX application using the deployed backend.
- [ ] Use one synthetic CV to verify a live Gemini job and test grading and explanations with OpenRouter. Record the configured model IDs and outcome while keeping credentials in the backend environment.
- [ ] Sign in from a second machine using the deployed API address. The deployment must use HTTPS and secure session cookies.
- [ ] Sign in as an administrator and verify user, course and analytics actions using the legacy acceptance scenarios.
- [x] Rehearse upgrading and restoring a synthetic copy of the legacy database. Follow the [upgrade instructions](database-upgrade.md) again with the data selected for cutover.
- [ ] Review the parity gaps listed in the migration status before tagging a stable release.

## Cutover and restore

1. Stop writes to the old application while taking the final database export. Keep the original installation and export available during the cutover.
2. Restore the export to a new schema, reconcile schema differences, then apply Flyway migrations to the copy. Compare counts, user-role assignments and representative course, chapter, XP and goal rows.
3. Point the new backend at the migrated schema. Run the release acceptance checks against it before directing the desktop clients there.
4. If rollback is needed, stop writes, export the migrated data separately, and return users to the original application and its compatible database. Reconcile new data from the separate export before attempting another cutover.

The README covers required environment variables and development startup. Use a configured single backend process for job recovery.

The `Verify` workflow runs the full desktop flow against a disposable backend and starts the same provider fixtures used by the backend integration tests. To run just the desktop tests against a separately started disposable backend, set `INTERVIEWAI_API_URL` to its origin and run `.\mvnw.cmd -pl desktop -Pui-tests "-Dinterviewai.journey=true" test` in an interactive Windows session. The fixture responses provide predictable choices and grades for this scenario.

For a remote deployment, put an HTTPS reverse proxy on the backend host, forward requests to the backend on loopback, and set `SESSION_COOKIE_SECURE=true`. Configure the desktop with the proxy's HTTPS origin using `INTERVIEWAI_API_URL`. Keep the database on persistent storage and configure your service manager to restart the backend after a failure.
