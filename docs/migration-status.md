# Migration status

The migrated desktop and backend are runnable from this repository. The stable release is pending the live-provider and deployment checks below.

| Plan stages | Delivered | Verification |
| --- | --- | --- |
| A–H | Source snapshot, separate Maven modules, MySQL migrations, HTTP client, registration and sign-in | Original source builds with JDK 21. Backend session and SQL tests pass on MySQL. The legacy live UI baseline still needs a complete manual run. |
| I | Course dashboard, chapter browsing, saved navigation, ownership checks | MySQL tests cover saving a chapter, signing out and back in, inaccessible IDs, and archived courses. |
| J–M | Onboarding, optional CV, configurable Gemini integration and persistent course generation | MySQL tests cover multipart upload, preference persistence, both complete courses, invalid provider data, repeat requests and retry. Tests use a local Gemini fixture. |
| N–O | Multiple-choice and short-answer practice, saved attempts, incorrect-question review and on-demand explanations | MySQL tests cover the passing threshold, review, once-only XP, explanation eligibility and provider failure. Tests use a local OpenRouter fixture. |
| P–Q | XP chart, editable goals, leaderboard, daily and monthly quest claims with a countdown | MySQL tests cover historical XP, goal baselines after edits and simultaneous or replayed claims. |
| R–T | Profile, session invalidation, account deletion and admin views | MySQL tests cover password changes, older sessions, account deletion, permission checks, filtering, resets and course administration. |
| U–V | Independent desktop and backend builds, JavaFX smoke tests and a candidate journey | The JavaFX app connects to the packaged backend with MySQL and provider fixtures, completes a generated chapter, reviews an answer, claims XP, edits a goal, and signs in again. The manual comparison with the original app remains open. |
| W | The packaged desktop and backend run in separate processes | Live Gemini and OpenRouter tests and a second-machine HTTPS test are pending. The local environment did not have provider keys configured. |
| X | Windows application image with bundled runtime and runnable Spring Boot JAR | A clean checkout built both. The backend returned UP and the launcher opened InterviewAI from the extracted Windows ZIP. A separate clean Windows account or VM installation remains a release check. |
| Y | Upgrade and restore rehearsal | A synthetic legacy database migrated through V7. Sign-in, navigation and XP worked after upgrade. A dump restored into a second MySQL instance preserved IDs, quest marker, XP rows and dates, and the goal baseline. |
| Z | Release checklist and reproducible packaging | The stable release tag is pending remaining acceptance checks. |

The rebuilt screens use the original app's palette and core workflows. They have reorganized layouts and fewer decorative widgets. Simulation and other unfinished features remain outside the implemented migration. Use the [legacy checklist](legacy-acceptance.md) and [release checklist](release-checklist.md) to record the remaining comparisons and live checks.
