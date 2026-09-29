# CurioKeep

**A self-hosted, web-based application for cataloging physical collections of any type, built around extensible modules, strong metadata import, and full data ownership.**

Key principles:

- Web-first, mobile-friendly
- Self-hosted only (no SaaS mode)
- Extensible via modules
- Metadata-driven, not manual-entry-driven
- Long-term data ownership

## What it does

- **Collections with roles.** Group items into collections and share them with other users as owner, admin, editor or viewer.
- **Modules define what you collect.** A module is an XML file that declares the fields, states and add-item workflows for one kind of collectible. Books and comics ship with the app; you can import your own. A module can ship migrations that a collection admin previews and accepts to bring existing items up to a new version.
- **Metadata lookup.** Add an item by ISBN, UPC/EAN or a title search and CurioKeep fills the fields from external providers (Open Library, Google Books, Metron, Comic Vine and [others](docs/providers.md)).
- **Barcode scanning** from the browser camera, item states (owned, wishlist, lent out, …), cover images stored locally, filters, sorting and batch actions.
- **Your data stays yours.** Everything lives in your own PostgreSQL database and data directory, and any collection can be exported as JSON (importable again) or as a CSV per module for a spreadsheet.
- **Invite-only accounts.** The first visit creates the admin; everyone else joins through an invite.

## Quick start with Docker

The image is published as `roastslav/curiokeep`. A minimal `compose.yaml`:

```yaml
services:
  db:
    image: postgres:16
    environment:
      POSTGRES_DB: curiokeep
      POSTGRES_USER: curiokeep
      POSTGRES_PASSWORD: change-me
    volumes:
      - db-data:/var/lib/postgresql/data

  app:
    image: roastslav/curiokeep:latest
    depends_on:
      - db
    environment:
      DB_HOST: db
      DB_NAME: curiokeep
      DB_USER: curiokeep
      DB_PASS: change-me
      # Encrypts provider API keys stored in the database. Set it before saving any key.
      CURIOKEEP_PROVIDERS_CREDENTIALS_ENCRYPTION_PASSWORD: pick-a-long-random-string
      # Optional: creating the first admin then needs this token. Useful if the server is reachable before you finish setup.
      # CURIOKEEP_SETUP_TOKEN: pick-another-long-random-string
    ports:
      - "8080:8080"
    volumes:
      - app-data:/app/data

volumes:
  db-data:
  app-data:
```

Open <http://localhost:8080>. The first page asks you to create the admin account. See [deployment](docs/deployment.md) for backups and upgrades and [configuration](docs/configuration.md) for every setting.

## Development

You need Java 25 and Docker (for the development database). The frontend needs Node.js 24 (the `frontend` Maven profile downloads its own Node 24).

```bash
# backend: starts the compose.yaml database automatically, then the API on :8080
./mvnw spring-boot:run

# frontend: Vite dev server with /api proxied to :8080
cd frontend && npm ci && npm run dev
```

Checks:

```bash
./mvnw -B verify                                   # backend build and tests
cd frontend && npm run lint && npm test && npm run build
```

The full application jar, with the frontend bundled in, is built with `./mvnw clean package -Pfrontend`.

Interactive API documentation is served at `/swagger-ui.html` while the backend runs.

## Documentation

- [Architecture](docs/architecture.md): components, packages, data model, security model
- [Modules](docs/modules.md): the module XML format and how modules are loaded
- [Providers](docs/providers.md): metadata providers, credentials, the normalized payload
- [Configuration](docs/configuration.md): environment variables and properties
- [Deployment](docs/deployment.md): Docker, data directories, backups, CI
- [Contributing](CONTRIBUTING.md)

## License

[PolyForm Noncommercial License 1.0.0](LICENSE).
