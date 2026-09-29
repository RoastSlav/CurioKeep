# Configuration

Settings live in `src/main/resources/application.yml` and can be overridden with environment variables. Spring Boot maps a property such as `curiokeep.assets.dir` to the variable `CURIOKEEP_ASSETS_DIR`.

## Database

| Variable | Default | Meaning |
|---|---|---|
| `DB_HOST` | `localhost` | PostgreSQL host |
| `DB_PORT` | `5432` | PostgreSQL port |
| `DB_NAME` | `curiokeep` | Database name |
| `DB_USER` | `curiokeep` | Database user |
| `DB_PASS` | `curiokeep` | Database password |

The schema is created and upgraded automatically by Flyway when the application starts. Hibernate only validates it (`ddl-auto: validate`).

During development, `./mvnw spring-boot:run` starts the database from `compose.yaml` through Spring Boot's Docker Compose support. That container keeps its data inside the container unless you uncomment the volume in `compose.yaml`.

## Application settings

| Property | Environment variable | Default | Meaning |
|---|---|---|---|
| `server.port` | `SERVER_PORT` | `8080` | HTTP port |
| `curiokeep.assets.dir` | `CURIOKEEP_ASSETS_DIR` | `./data/assets` | Where item cover images are stored |
| `curiokeep.modules.import-dir` | `CURIOKEEP_MODULES_IMPORT_DIR` | `./data/modules-imported` | Where uploaded module XML files are kept; they are loaded again on every start |
| `curiokeep.modules.schema` | `CURIOKEEP_MODULES_SCHEMA` | `classpath:schema/module-schema-v1.xsd` | XSD that module files are validated against; leave it alone unless you are developing the module format |
| `LOG_DIR` | `LOG_DIR` | `./logs` | Directory for the rolling log file `curiokeep.log` |

Relative paths are resolved against the working directory. In the Docker image that is `/app`, so the defaults become `/app/data/assets`, `/app/data/modules-imported` and `/app/logs`. Mount a volume on `/app/data` to keep images and imported modules across container restarts.

Logging levels are set under `logging.level` in `application.yml`; the application's own packages log at `DEBUG` by default. Every log line carries a request id (`rid`) that is also useful when correlating a client error with the log.

## Provider credentials

Some [providers](providers.md) need an API key or account. Keys are entered in the web UI (Providers page, admin only) and stored in the database, encrypted (AES-256-GCM) with a password and salt from configuration:

| Property | Environment variable | Default |
|---|---|---|
| `curiokeep.providers.credentials.encryption.password` | `CURIOKEEP_PROVIDERS_CREDENTIALS_ENCRYPTION_PASSWORD` | `changeme` |
| `curiokeep.providers.credentials.encryption.salt` | `CURIOKEEP_PROVIDERS_CREDENTIALS_ENCRYPTION_SALT` | `5f4dcc3b5aa765d6` |

- **Set your own password before you save any key.** The default is public, so it protects nothing. The application logs a warning at start-up while the default password is in use.
- **The salt must be an even-length hexadecimal string of at least 16 characters.** With anything else the application refuses to start.
- **Changing the password or salt later makes already-stored keys unreadable.** Re-enter them in the UI afterwards.

## Reverse proxy and HTTPS

The application does not terminate TLS. Put a reverse proxy in front of it and forward to port 8080. Sessions use a cookie, so serve the site over HTTPS on the public internet.
