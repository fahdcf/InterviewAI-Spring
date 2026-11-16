# Local configuration and repository hygiene

The tracked configuration contains defaults only. Set credentials in the process
environment before starting JavaFX or running the benchmark. `ConfigLoader` maps
property names to uppercase environment names, replacing punctuation with `_`.

| Property | Environment variable |
| --- | --- |
| `db.url` | `DB_URL` |
| `db.user` | `DB_USER` |
| `db.password` | `DB_PASSWORD` |
| `ai.api.keys` | `AI_API_KEYS` (comma-separated Gemini keys) |
| `openrouter.api.key` | `OPENROUTER_API_KEY` |
| `openrouter.api.key.backup` | `OPENROUTER_API_KEY_BACKUP` |

The benchmark reads `OPENROUTER_API_KEY` and exits without contacting a provider
when it is missing. A `.env` file alone is not automatically loaded by Java or
the benchmark; configure your terminal, IDE or deployment environment.

`src/main/resources/db/schema.sql` is a UTF-8 schema with static role/permission
reference data only. Accounts, hashes, onboarding answers and generated user
content are not shipped. Register a new account after initializing an empty
development database. Import this bootstrap script only into a disposable/new
database: it retains the original `DROP TABLE` statements, not a live migration.

Previously committed credentials must be revoked/rotated at their providers.
Removing current literals does not remove historical copies or revoke a key.
Do not commit local credentials, account dumps, build logs or diagnostic output.
