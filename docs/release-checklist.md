# Release checks

## Build and package

On Windows with JDK 21, run the following from a clean checkout.

```powershell
.\mvnw.cmd clean verify
.\mvnw.cmd -pl desktop -Pui-tests test
.\scripts\package-windows.ps1
```

The packaging script creates a Spring Boot JAR and a ZIP containing the desktop application and its Java runtime. Extract the full ZIP and run `InterviewAI.exe`. The ZIP's directory layout is required by the launcher. The backend runs with `java -jar` and the database and provider variables in the README. [Java packaging tool](https://docs.oracle.com/en/java/javase/21/docs/specs/man/jpackage.html)

The `Verify` workflow publishes both packages as the `interviewai-windows-build` artifact for seven days. A fresh Windows runner downloads that artifact, extracts the ZIP in a new folder, and checks that the sign-in window opens with the bundled JVM and Java removed from the process PATH.

## Release acceptance

- [x] Build from a clean checkout and migrate a disposable MySQL schema.
- [x] Re-run backend tests on MySQL with `TEST_DB_URL`, `TEST_DB_USER` and `TEST_DB_PASSWORD` set to a disposable database.
- [x] Load the migrated candidate and administrator views with the JavaFX smoke profile.
- [x] Start the packaged Spring Boot JAR. Extract and start the Windows desktop application using its bundled runtime.
- [x] Download and launch the packaged desktop app on a separate Windows runner. The sign-in window opened with `JAVA_HOME` unset, Java removed from `PATH`, and the bundled JVM loaded. [Windows installation check](https://github.com/fahdcf/InterviewAI-Spring/actions/runs/43962100088/job/133633055383)
- [x] Exercise the full candidate flow over HTTP on MySQL using local provider fixtures, from registration and course generation through completing a 40-question chapter, reviewing a missed answer, checking XP, and signing out.
- [x] Exercise the candidate flow through the JavaFX application against a separate packaged Spring Boot process, MySQL, and local provider fixtures. Verify both generated courses, all 40 answers in the first chapter, missed-answer review, explanations, a daily reward, a saved goal, and progress after signing out and signing back in.
- [x] Send two requests for the same daily quest reward and verify the account receives one award.
- [ ] Repeat the full candidate flow from the JavaFX application using the deployed backend.
- [ ] Use one synthetic CV to verify a live Gemini job and test grading and explanations with OpenRouter. Record the configured model IDs and outcome while keeping credentials in the backend environment.
- [ ] Sign in from a second machine using the deployed API address. The deployment must use HTTPS and secure session cookies.
- [x] Repeat the candidate journey through a local HTTPS reverse proxy with a certificate trusted by the test JVM. Verify the backend sends a session cookie with the `Secure` attribute.
- [x] Exercise administration in the migrated JavaFX app against MySQL. Verify analytics, candidate search, email edits, suspension and reactivation, separate XP and progress resets, and course archive, restore, and deletion.
- [ ] Sign in as an administrator and verify user, course and analytics actions using the legacy acceptance scenarios.
- [x] Rehearse upgrading and restoring a synthetic copy of the legacy database. Follow the [upgrade instructions](database-upgrade.md) again with the data selected for cutover.
- [ ] Review the parity gaps listed in the migration status before tagging a stable release.

## Cutover and restore

1. Stop writes to the old application while taking the final database export. Keep the original installation and export available during the cutover.
2. Restore the export to a new schema, reconcile schema differences, then apply Flyway migrations to the copy. Compare counts, user-role assignments and representative course, chapter, XP and goal rows.
3. Point the new backend at the migrated schema. Run the release acceptance checks against it before directing the desktop clients there.
4. If rollback is needed, stop writes, export the migrated data separately, and return users to the original application and its compatible database. Reconcile new data from the separate export before attempting another cutover.

The README covers required environment variables and development startup. Use a configured single backend process for job recovery.

The `Verify` workflow runs the full desktop flow against a disposable backend and starts the same provider fixtures used by the backend integration tests. To run just the desktop tests against a separately started disposable backend, set `INTERVIEWAI_API_URL` to its origin and run `.\mvnw.cmd -pl desktop -Pui-tests "-Dinterviewai.journey=true" test` in an interactive Windows session. The fixture responses provide predictable choices and grades for this scenario. To include the administrator steps, provision a synthetic administrator with the password `synthetic-password` on the disposable database and set `INTERVIEWAI_TEST_ADMIN` to that username. The test changes only the candidate and courses created during that run.

For a remote deployment, put an HTTPS reverse proxy on the backend host, forward requests to the backend on loopback, and set `SESSION_COOKIE_SECURE=true`. Configure the desktop with the proxy's HTTPS origin using `INTERVIEWAI_API_URL`. Keep the database on persistent storage and configure your service manager to restart the backend after a failure.

## Live providers

Add `GEMINI_API_KEY` and `OPENROUTER_API_KEY` as repository Actions secrets and run **Provider acceptance** from the Actions tab when ready to use provider quota. The workflow extracts text from a synthetic PDF, asks Gemini for both complete courses, and checks OpenRouter grading and explanations. It logs model identifiers and elapsed times. Repeat a learning flow against the deployed backend for the separate deployment acceptance check.

To use local backend credentials or different supported models, configure the values described in the README and run:

```powershell
.\mvnw.cmd -pl backend "-Dtest=ProviderAcceptanceTest" "-Dinterviewai.provider.acceptance=true" test
```
