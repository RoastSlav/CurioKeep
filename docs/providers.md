# Metadata providers

A **provider** is a backend adapter for one external service (Open Library, Metron, TMDb, …). Given an identifier or a search text it returns descriptive metadata and cover images, normalized into one common shape so that [modules](modules.md) never depend on any service's own JSON.

## Available providers

Whether a provider is used for a collection depends on the module: a module lists the providers it may use and maps their results onto its fields. The bundled `books` module uses `openlibrary` and `googlebooks`; `comics` uses `metron` and `comicvine`.

| Key | Service | Looks up by | Credentials |
|---|---|---|---|
| `openlibrary` | [Open Library](https://openlibrary.org) | ISBN-10, ISBN-13 | none |
| `googlebooks` | [Google Books](https://books.google.com) | ISBN-10, ISBN-13 | none |
| `internetarchive` | [Internet Archive](https://archive.org) | `CUSTOM` | none |
| `metron` | [Metron](https://metron.cloud) (comics) | ISBN-10, ISBN-13, UPC | Metron username and password |
| `comicvine` | [Comic Vine](https://comicvine.gamespot.com) (comics) | `CUSTOM` (title search or Comic Vine issue id) | API key |
| `anilist` | [AniList](https://anilist.co) (anime, manga) | `CUSTOM` | none |
| `boardgamegeek` | [Board Game Geek](https://boardgamegeek.com) | `CUSTOM` | bearer token |
| `brickset` | [Brickset](https://brickset.com) (LEGO sets) | `CUSTOM` | API key |
| `rebrickable` | [Rebrickable](https://rebrickable.com) (LEGO) | `CUSTOM` | API key |
| `discogs` | [Discogs](https://discogs.com) (music releases) | UPC, EAN | token |
| `musicbrainz` | [MusicBrainz](https://musicbrainz.org) | UPC, EAN | none |
| `coverartarchive` | [Cover Art Archive](https://coverartarchive.org) | `CUSTOM` | none |
| `igdb` | [IGDB](https://www.igdb.com) (video games) | `CUSTOM` | Twitch client id and bearer token |
| `rawg` | [RAWG](https://rawg.io) (video games) | `CUSTOM` | API key |
| `tmdb` | [TMDb](https://www.themoviedb.org) (film, TV) | `CUSTOM` | API key |
| `tvmaze` | [TVmaze](https://www.tvmaze.com) (TV) | `CUSTOM` | none |
| `pokeapi` | [PokéAPI](https://pokeapi.co) | `CUSTOM` | none |
| `scryfall` | [Scryfall](https://scryfall.com) (Magic: The Gathering) | `CUSTOM` | none |
| `openproduct` | [Open Food Facts](https://world.openfoodfacts.org) (products) | UPC, `CUSTOM` | none |

`CUSTOM` is the catch-all identifier type: a free-text search (the search box of the add-item wizard) or an id that only that service understands. Providers that list only `CUSTOM` are the ones to use for title searches. The Providers page in the app shows each provider's status and lets an admin store its credentials. A provider that needs credentials and has none is **skipped**, so its module falls back to the remaining providers.

Each service has its own terms of use, rate limits and attribution rules; you are responsible for complying with them when you enter your own keys. The app shows the required attribution for providers that demand it (for example the "Powered by BGG" badge).

### Credentials

Keys are entered on the Providers page (admin only) and stored encrypted; see [configuration](configuration.md#provider-credentials) for the encryption password. They can be set, cleared and checked from the UI but never read back.

## How a lookup works

`POST /api/providers/lookup` with a module id and either identifiers or a search text (a search text is sent as a `CUSTOM` identifier):

1. The module's declared providers that are **enabled** (and, if the request names any, in the request's provider list) are visited in the order the module declares them.
2. For each identifier a provider **supports**, its `fetch` is called. A provider that fails or has no credentials is logged and skipped; the others still run. If Metron returns a Comic Vine reference and the module also uses `comicvine`, the Comic Vine record is fetched too.
3. Every result's normalized fields are read through the module's `providerMappings`, and the values are merged into one set of suggested attributes: for each module field, the first provider in the module's declared order that produced a value wins.
4. The **best** result (shown as the primary match) is the one from the provider with the highest declared `priority`, ties broken by the result's confidence score.
5. Cover images from all providers are collected in module order and duplicates are removed.

The response contains every provider's result, the best one, the merged attributes and the assets. Nothing is saved: the frontend lets the user choose what to apply and creates the item afterwards.

## Normalized result

Every provider returns a `ProviderResult`:

| Part | Meaning |
|---|---|
| `providerKey` | the provider's key |
| `rawData` | the service's own response, kept alongside for debugging; **modules must not map into it** |
| `normalizedFields` | a flat object of CurioKeep keys, the only thing mappings read |
| `assets` | cover and thumbnail images (`COVER`, `THUMBNAIL`, each with a URL) |
| `confidence` | a numeric score used to break priority ties, and a short reason (for example "ISBN match") |

`normalizedFields` is **flat**: a module maps `/title`, `/isbn13` or `/published_year`, not a nested path. Providers reuse the same key names wherever the meaning is the same:

| Kind | Keys |
|---|---|
| Descriptive | `title`, `subtitle`, `description`, `authors`, `publisher`, `published_year`, `language`, `pages`, `series`, `canonical_url`, `attribution` |
| Identifiers | `isbn`, `isbn10`, `isbn13`, `upc`, `ean`, `gtin` |
| Comics | `writers`, `artists`, `cover_artists`, `issue_number`, `issue_title`, `volume_number`, `cover_date` |
| Games and toys | `min_players`, `max_players`, `playing_time`, `min_age`, `weight`, `pieces`, `minifigs` |
| Service ids | one per service, such as `bgg_id`, `comicvine_id`, `metron_issue_id`, `tmdb_id`, `musicbrainz_id`, `igdb_id` |

A key that a provider does not know is simply absent. Values may be text, numbers or lists; use a mapping `transform` (`JOIN_COMMA`, `FIRST`, `TRIM`, `TO_INT`) to shape a list or a number for a text field.

## Adding a provider

1. Add a class under `src/main/java/org/rostislav/curiokeep/providers/impl/` annotated `@Component` that implements `MetadataProvider`: `key()` (permanent: modules refer to it), `supports(idType)`, `fetch(idType, value)`, and `credentialFields()` if it needs a key.
2. Use the shared `RestClient` (5 s connection and 10 s read timeouts). Treat the response as untrusted: tolerate missing fields and empty bodies, and return `Optional.empty()` instead of throwing when the service has nothing or is unavailable. Read credentials through `ProviderCredentialLookup` and skip the lookup when they are missing.
3. Build `normalizedFields` from the vocabulary above and add a new key only when no existing one has the same meaning. Return cover images as assets and a `confidence`.
4. Add a profile (display name, summary, links) in `ProviderKnowledgeBase`, and add the required attribution, if any, in `frontend/src/features/providers/providerAttribution.ts`.
5. Write a test with `MockRestServiceServer` covering a hit, a miss and a malformed or error response (see the existing tests in `src/test/java/.../providers`).
6. Reference the provider from a module's `<providers>` and add `providerMappings`.

Never send user identifiers to a service, and never put credentials in logs.
