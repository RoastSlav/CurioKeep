# Deployment

## The Docker image

`roastslav/curiokeep` is a multi-arch (amd64 and arm64) image based on `eclipse-temurin:21-jre-alpine`. It runs the application jar, which contains the built frontend, on port 8080. Extra JVM options can be passed in `JAVA_OPTS`.

To build it yourself:

```bash
docker build -t curiokeep .
```

The `Dockerfile` builds in a Maven container with `mvn -DskipTests -Pfrontend clean package` (which downloads its own Node.js and builds the frontend) and copies only the jar into the runtime image.

A complete example (application plus PostgreSQL) is in the [README](../README.md#quick-start-with-docker). Configuration is by environment variable; see [configuration](configuration.md).

## What to persist

| What | Where | How |
|---|---|---|
| The database | PostgreSQL | a volume on the database container, or a managed PostgreSQL |
| Cover images | `/app/data/assets` | a volume on `/app/data` |
| Imported modules | `/app/data/modules-imported` | the same `/app/data` volume |
| Logs (optional) | `/app/logs` | a volume, or read the container output |

Bundled modules are part of the image and need no persistence. Imported modules are stored twice, in the database and as files, and the files are loaded again at every start, so keep `/app/data`.

## Backups

Back up both the database and the data directory, and keep them together:

```bash
docker compose exec db pg_dump -U curiokeep curiokeep > curiokeep.sql
docker compose exec app tar czf - -C /app/data . > curiokeep-data.tgz
```

Provider API keys are stored in the database encrypted with `CURIOKEEP_PROVIDERS_CREDENTIALS_ENCRYPTION_PASSWORD`. **Keep that password with your backups**; without it the stored keys cannot be read after a restore.

## Upgrading

1. Back up (above).
2. Pull the new image and restart: `docker compose pull && docker compose up -d`.
3. The application applies pending database migrations (Flyway) on start and reloads the bundled and imported modules. If a module fails validation the application refuses to start and the log lists the reason.

Downgrading is not supported: migrations are never reversed.

## Health checks and monitoring

Nearly every endpoint requires a login, including `/api/health`. For a container health check or a reverse-proxy probe use the public `GET /api/setup/status`: it returns `200` with `{"setupRequired": false}` once the instance is set up, and it touches the database, so it also fails when the database is unreachable.

Logs go to the console and to a rolling file (`curiokeep.log` in `LOG_DIR`, compressed daily). Each line has a request id (`rid`).

## Reverse proxy

Terminate TLS in a reverse proxy and forward to port 8080. Sessions use a cookie, so serve the site over HTTPS whenever it is reachable from the internet.

## Troubleshooting

| Symptom | Likely cause |
|---|---|
| The application exits at start with "Module load failed for N module(s)" | A bundled or imported module is invalid. The log lists each failing file and reason. Fix or remove the file in `/app/data/modules-imported`. |
| Flyway reports a checksum mismatch on `V1__init.sql` | The database was created by an early development build whose `V1` differed. Recreate the database, or run Flyway's `repair` against it after checking the schema by hand. |
| Provider lookups return nothing for a service that needs a key | No credentials stored for it (Providers page, admin only) or the encryption password changed since they were stored. Re-enter the key. |
| Every page redirects to the setup screen | No admin exists yet (or the database was replaced). Create the admin on the setup page. |

## Continuous integration

The `Jenkinsfile` describes the pipeline used for this project:

- **Pull requests**: `mvn -B clean verify` (backend build and tests; the frontend is not built or tested by this job).
- **Other builds**: `mvn -B -DskipTests -Pfrontend clean package`, which builds the frontend into the jar.
- **Docker image**: on `master`, when the version in `pom.xml` differs from the one recorded by the previous run, a multi-arch image is built and pushed with the tags `latest` and `v<version>`. Bump the POM version to publish a release.
