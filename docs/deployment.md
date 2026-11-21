# Run the HTTPS deployment

The Docker Compose stack runs one Spring Boot backend, MySQL 8.4.13 and a Caddy HTTPS proxy. Named volumes retain database data and certificates. Each service uses the `unless-stopped` restart policy. The backend waits for the database health check before starting. [Compose services](https://docs.docker.com/reference/compose-file/services/)

## Start on this machine

Install Docker with Compose. From the repository root, copy the example settings and replace the database password in `deploy/.env`.

```powershell
Copy-Item deploy/.env.example deploy/.env
docker compose --env-file deploy/.env -f deploy/compose.yaml up -d --build
docker compose --env-file deploy/.env -f deploy/compose.yaml ps
```

The local API origin is `https://localhost:8443`. Caddy issues a local certificate for this hostname. Copy its root certificate so the desktop's Java runtime can trust this local test server. [Caddy local HTTPS](https://caddyserver.com/docs/automatic-https#local-https)

```powershell
docker compose --env-file deploy/.env -f deploy/compose.yaml cp proxy:/data/caddy/pki/authorities/local/root.crt ./target/local-root.crt
keytool -importcert -alias interviewai-local -file ./target/local-root.crt -keystore ./target/local-trust.p12 -storetype PKCS12
```

Choose a trust-store password when prompted. Set the following values in the desktop terminal using that password and the absolute trust-store path.

```powershell
$env:INTERVIEWAI_API_URL = 'https://localhost:8443'
$env:JAVA_TOOL_OPTIONS = '-Djavax.net.ssl.trustStore=C:/absolute/path/local-trust.p12 -Djavax.net.ssl.trustStorePassword=your-trust-store-password'
.\mvnw.cmd -pl desktop javafx:run
```

The packaged Windows launcher also reads these environment variables. The backend sets secure session cookies. Registration, course browsing, profile, progress and admin actions are available. Course generation, grading and explanations use the configured backend provider keys.

## Run on your server

Copy this repository to the server. Give `APP_HOST` your backend DNS name, set `PROXY_BIND=0.0.0.0`, `HTTP_PORT=80` and `HTTPS_PORT=443` in its environment file, and point the DNS record at the server. Make ports 80 and 443 reachable, then use the same Compose command. Caddy obtains and renews the certificate for that hostname. Set each desktop's `INTERVIEWAI_API_URL` to the HTTPS origin. [Caddy HTTPS setup](https://caddyserver.com/docs/quick-starts/https)

Keep one backend instance connected to this database. Startup recovery marks interrupted work and processes persisted queued work. Check service logs and API health during deployment.

```powershell
docker compose --env-file deploy/.env -f deploy/compose.yaml logs --tail 100 backend
Invoke-RestMethod https://your-backend.example/api/v1/health
```

The README describes provisioning the first administrator. Live-provider acceptance is skipped at the user's request. Remote deployment acceptance is skipped at the user's request.

## Upgrade the backend

Build the new image, then recreate the backend container so it uses that image. The database volume and certificate volume stay attached to their services.

```powershell
docker compose --env-file deploy/.env -f deploy/compose.yaml build backend
docker compose --env-file deploy/.env -f deploy/compose.yaml up -d --force-recreate backend
```

## Stop and restart

```powershell
docker compose --env-file deploy/.env -f deploy/compose.yaml restart backend
docker compose --env-file deploy/.env -f deploy/compose.yaml down
docker compose --env-file deploy/.env -f deploy/compose.yaml up -d
```

The named volumes retain the database when these commands stop and recreate the containers. Use the [database upgrade and backup instructions](database-upgrade.md) before upgrading an installation or restoring an existing database.
## Verification on 21 November 2026

The candidate JavaFX journey ran against this stack through trusted HTTPS with local provider fixtures. It generated both courses, completed the first chapter, reviewed an incorrect answer, claimed the daily reward and saved a goal. The backend restarted after its process exited. After the containers were removed and recreated with the same volumes, a new desktop session restored the candidate's 1,290 XP, completed chapter, saved goal and empty review list. A separate client container verified the API over HTTPS with the local CA certificate.

These checks used a local Docker deployment. Remote deployment acceptance is skipped at the user's request.
## Back up and restore

Create a consistent logical backup using the database service account. Keep the backup in the ignored `backups` directory and copy it to your backup storage.

```powershell
New-Item -ItemType Directory -Path backups -Force
docker compose --env-file deploy/.env -f deploy/compose.yaml exec -T database sh -c 'MYSQL_PWD="$MYSQL_PASSWORD" mysqldump -u "$MYSQL_USER" --single-transaction --no-tablespaces --set-gtid-purged=OFF "$MYSQL_DATABASE"' > backups/interviewai.sql
```

Restore into a separate Compose project to rehearse recovery.

```powershell
docker compose -p interviewai-restore --env-file deploy/.env -f deploy/compose.yaml up -d --wait database
Get-Content -Raw backups/interviewai.sql | docker compose -p interviewai-restore --env-file deploy/.env -f deploy/compose.yaml exec -T database sh -c 'MYSQL_PWD="$MYSQL_PASSWORD" mysql -u "$MYSQL_USER" "$MYSQL_DATABASE"'
```

Check user, course, chapter and XP counts on the restored copy before using it for cutover. For an upgrade, use the schema and timestamp checks in [Database setup and upgrade](database-upgrade.md). A restored database should retain its Flyway history, including migrations that already ran.
