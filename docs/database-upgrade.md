# Database setup and upgrade

The backend uses MySQL 8.4 and Flyway. Set `DB_URL`, `DB_USER` and `DB_PASSWORD` in the backend process environment. Use an empty MySQL schema for a fresh setup. Flyway applies V1 for the base schema, V2 for unique account emails, V3 for interview preferences and CV text, and V4 for generation jobs.

## Upgrading a copy of the original database

1. Make a verified backup of the original database and restore it into a separate development schema. Connect the new backend to the restored copy.
2. Compare the tables, columns and foreign keys in the copy with [V1](../backend/src/main/resources/db/migration/V1__initial_schema.sql). Some older installations may have missing columns that the original desktop added on startup. Reconcile those differences before recording the baseline.
3. Review the result of the checks below. Resolve duplicate real email addresses with the affected account owners. Convert legacy blank email placeholders to `NULL` in the copy if they represent omitted email addresses. Review email addresses that match a different account's username. The backend checks for a username match first at sign-in, using the database collation.
4. After verifying the copied schema is at V1, start the backend once with `SPRING_FLYWAY_BASELINE_ON_MIGRATE=true` and `SPRING_FLYWAY_BASELINE_VERSION=1`. Flyway records the existing schema at V1 and applies later migrations. Remove the baseline environment variables for subsequent runs.
5. Compare users, role assignments, course and chapter IDs, last-used navigation, progress rows and timestamps, goal starting values, and quest markers before and after the migration. Sign in with a representative account and verify its role. A successful sign-in upgrades a legacy SHA-256 hash to BCrypt when the password fits BCrypt's 72-byte limit.

```sql
SELECT email, COUNT(*) AS accounts
FROM users
WHERE email IS NOT NULL
GROUP BY email
HAVING COUNT(*) > 1;

SELECT email_owner.id AS email_owner_id, username_owner.id AS username_owner_id
FROM users AS email_owner
JOIN users AS username_owner ON email_owner.email = username_owner.username
WHERE email_owner.id <> username_owner.id;
```

New usernames use at most 100 characters and exclude the `@` sign, which keeps typical usernames and email addresses distinct. Email is optional and unique when supplied. Imported usernames continue to work.

The upgrade was rehearsed on MySQL 8.4.13 using the pinned legacy SQL and a synthetic account with course, chapter, XP, timestamps, and goal data. V2 preserved the sample data. Signing in by email verified the legacy password and upgraded its hash. Repeat the checks against a copy of your own data before an actual cutover. [Flyway baseline documentation](https://documentation.red-gate.com/flyway/reference/commands/baseline)

V5 adds answer attempt history and V6 records quest claims. The backend uses a UTC MySQL session and applies `APP_TIME_ZONE` to display and count dates. Review legacy `DATETIME` values such as chapter completion and quest resets against their original database timezone during the cutover rehearsal.

V7 adds a session version to each account so password changes, suspension and role changes can invalidate signed-in sessions. The upgrade and restore rehearsal applied V2–V7 to a synthetic legacy database, then imported a dump of the upgraded database into a second MySQL instance.

For HTTPS deployment, set `SESSION_COOKIE_SECURE=true` on the backend.
V8 increases question-update and daily-reset timestamps to microsecond precision. This preserves existing timestamps and lets a newly earned daily cycle include answers submitted in the same second as the previous claim. The MySQL calendar tests verify consecutive cycles, monthly rollover and rollback after a failed claim write.
The Docker deployment rehearsal also upgraded a V7 database to V8, reopened the candidate from JavaFX, exported the upgraded database, and restored it into a separate MySQL service. The restored copy matched two accounts, two courses, 960 questions, 1,290 XP and eight Flyway rows. The source database for a real installation must still be supplied for its actual cutover.
