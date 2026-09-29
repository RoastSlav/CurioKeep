# Contributing

Thanks for helping out. This page covers how to get set up, what a change should include, and where to read more.

## Set up

Follow [Development](README.md#development) in the README. In short: Java 21, Docker for the development database, and `./mvnw spring-boot:run` for the backend plus `npm ci && npm run dev` in `frontend/`.

Read [Architecture](docs/architecture.md) first if you are new to the code base: the backend and frontend are both organised by feature, and a new feature is a new package, not another file in a shared `service/` or `components/` folder.

## Before you open a pull request

Run the checks for the part you touched:

| Part | Command |
|---|---|
| Backend | `./mvnw -B verify` |
| Frontend | `cd frontend && npm run lint && npm test && npm run build` |

`npm run build` runs the TypeScript compiler as well as Vite. All of these should pass without new warnings.

A change should come with:

- **A test** for the behaviour it adds or fixes. Backend tests use JUnit 5 and Mockito (provider adapters use `MockRestServiceServer`, never a live service); frontend tests use Vitest and Testing Library, querying by role and label. Tests must not depend on the network, the clock or each other.
- **Updated documentation** when behaviour, configuration, the module format or the API changes. The [docs](docs/) and the OpenAPI annotations on controllers are part of the change.
- **A migration** for any schema change (see below). After the first release, never an edit to an existing one.

## Conventions

**Backend**

- Controller → service → repository. Controllers are thin, services enforce access and rules, and entities are never returned from the API; map them to response records.
- Constructor injection, immutable fields, no Lombok.
- Every outbound HTTP call goes through the shared `RestClient` (it has timeouts) and treats the response as untrusted.
- Errors returned to clients use the existing `ApiError` shape and must not leak stack traces, SQL, paths or upstream responses.
- No secrets in source, logs or committed configuration.

**Database migrations** live in `src/main/resources/db/migration` as `V<n>__short_description.sql`.

- **Once a release exists, never edit a migration that shipped.** Add a new one. Databases in the wild have already applied the old text, and Flyway will refuse to start if its checksum changes. Until the first release, migrations may be edited or squashed in place; recreate your local database afterwards.
- One logical change per file. Declare the constraints the domain implies (`NOT NULL`, `CHECK`, foreign keys with an explicit `ON DELETE`).
- A new query pattern ships with its index, with a comment naming the query it serves. Timestamps are `TIMESTAMPTZ`.
- The automated tests run on H2 with Flyway disabled, so try a new migration against the compose PostgreSQL yourself.

**Frontend**

- TypeScript in strict mode: no `any` (use `unknown` and narrow it), no `as` to silence an error, no non-null `!`.
- Named exports; page components loaded with `React.lazy` may keep a default export.
- Derive values while rendering instead of copying them into state; use `useEffect` only to synchronise with something outside React.
- Components do not call `fetch`. Use the feature's API module, which is built on `src/api/client.ts`.
- Every data-driven view has real loading, error and empty states. Colour is never the only signal, controls are keyboard reachable, and interactive elements have accessible names.
- Build screens from the shadcn/Tailwind components in `src/components/ui`.

**Comments** explain *why*, not *what*: a workaround (and what would remove it), non-obvious business logic, a deliberate departure from the usual approach, or a gotcha. Delete comments that restate the code.

## Modules and providers

- To add or change a **module**, read [Modules](docs/modules.md). The XML format is defined in five places that must change together (the XSD, the parser records, the compiled contract, the JSON schema and the frontend types); the doc lists them. Published module keys are permanent.
- To add a **provider**, follow [Adding a provider](docs/providers.md#adding-a-provider). Reuse the existing normalized key names.

## Commits and pull requests

- `develop` is the working branch; `master` receives merges from it and is what gets built and published. Branch from `develop`.
- One logical change per commit. Start the subject with a type (`feat:`, `fix:`, `docs:`, `refactor:`, `test:`, `chore:`, `perf:`) and write it in the imperative, for example `fix: skip providers that have no credentials`.
- Never commit secrets, build output (`target/`, `frontend/dist`, `frontend/node`), local data (`data/`, `logs/`) or IDE settings.

## License

The project is under the [PolyForm Noncommercial License 1.0.0](LICENSE).
