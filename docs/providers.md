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
2. For each identifier a provider **supports**, its `fetch` is called. A provider that fails or has no credentials is logged and skipped; the others still run. Then the provider's `<chain>` declarations in the module are followed: a result's normalized field is looked up as an identifier in another provider of the module (for `comics`, Metron's `comicvine_id` is looked up in Comic Vine). See [chaining providers](modules.md#chaining-providers).
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

## Chaining providers

A module can make one provider hand a value from its result to another provider (`<chain from="…" to="…" idType="…"/>`, see [chaining providers in modules.md](modules.md#chaining-providers) for the syntax). Whether a chain makes sense depends on two facts about the providers, which are listed here: what the source provider **emits** (`from` must be one of its normalized keys) and what the target **accepts** for the `idType` you choose.

### What each provider hands on

Only keys that can identify a record somewhere else are listed. Every provider also emits the descriptive keys from the vocabulary above.

| Provider | Emits (`from`) | The value is |
|---|---|---|
| `openlibrary` | `isbn10`, `isbn13` | the ISBN of the edition found |
| `googlebooks` | `isbn10`, `isbn13` | the ISBN of the edition found |
| `metron` | `comicvine_id` | a Comic Vine issue id, `4000-<number>` |
| | `metron_issue_id` | Metron's own issue id |
| | `isbn`, `isbn10`, `isbn13`, `upc`, `ean` | the barcodes Metron lists for the issue, when it has them |
| `comicvine` | `comicvine_id`, `comicvine_issue_id`, `comicvine_volume_id` | Comic Vine ids (`4000-<number>` for `comicvine_id`) |
| `discogs` | `upc`, `ean`, `discogs_id` | the barcode searched, and the Discogs release id |
| `musicbrainz` | `upc`, `ean`, `musicbrainz_id` | the barcode searched, and the MusicBrainz **release** id (an MBID) |
| `coverartarchive` | `musicbrainz_id` | the release MBID |
| `openproduct` | `gtin` | the product's GTIN (an 8 to 14 digit barcode) |
| `brickset` | `set_number`, `brickset_id` | the bare set number such as `75192` |
| `rebrickable` | `set_number`, `rebrickable_id` | the set number with its variant, such as `75192-1` |
| `anilist`, `boardgamegeek`, `igdb`, `internetarchive`, `pokeapi`, `rawg`, `scryfall`, `tmdb`, `tvmaze` | `anilist_id`, `bgg_id`, `igdb_id`, `internet_archive_id`, `pokeapi_id`, `rawg_id`, `scryfall_id`, `tmdb_id`, `tvmaze_id` | the service's own id. None of these providers lists a reference to another service, so nothing can be chained *from* them today |

### What each provider accepts

| Provider | `idType` | The value it can use |
|---|---|---|
| `openlibrary`, `googlebooks` | `ISBN10`, `ISBN13` | an ISBN, with or without hyphens |
| `metron` | `ISBN10`, `ISBN13`, `UPC` | the barcode |
| | `CUSTOM` | digits only: a Metron issue id; anything else is a title search |
| `comicvine` | `CUSTOM` | `12345` or `4000-12345` (an issue id); anything else is a title search. Needs an API key |
| `discogs`, `musicbrainz` | `UPC`, `EAN` | a barcode |
| `coverartarchive` | `CUSTOM` | a MusicBrainz release id (an MBID, 36 characters with hyphens) |
| `openproduct` | `UPC`, `CUSTOM` | 8 to 14 digits |
| `brickset` | `CUSTOM` | a set number, letters, digits and hyphens |
| `rebrickable` | `CUSTOM` | a set number with its variant, such as `75192-1` |
| `anilist`, `boardgamegeek`, `igdb`, `tmdb`, `tvmaze` | `CUSTOM` | digits only: the service's own id |
| `pokeapi`, `rawg` | `CUSTOM` | the service's id or name/slug (letters, digits and hyphens) |
| `internetarchive`, `scryfall` | `CUSTOM` | the service's own item or card id |

A target that does not support the `idType` you name is skipped, and a value it cannot read makes it return nothing. Neither breaks the lookup, but neither fills any field either.

### Chains that work today

| Chain | What it does |
|---|---|
| `metron` → `comicvine`, `from="comicvine_id"` | Metron names the Comic Vine issue; Comic Vine's fuller description and credits fill fields Metron lacks. This is what the bundled `comics` module declares |
| `musicbrainz` → `coverartarchive`, `from="musicbrainz_id"` | fetches the cover images of the release MusicBrainz found. MusicBrainz already tries the Cover Art Archive itself, so this mainly matters when you want its result as a separate candidate |
| `discogs` → `musicbrainz`, `from="upc"`, `idType="UPC"` (and the reverse) | asks the second music database for the same barcode. Both are usually asked directly with the barcode already, so a chain only adds something when the first provider's barcode differs from the one entered |
| `metron` → `openlibrary` or `googlebooks`, `from="isbn13"`, `idType="ISBN13"` | enriches a comic found by title or UPC with the book databases' records for its ISBN, when Metron lists one |
| `openlibrary` ↔ `googlebooks`, `from="isbn13"`, `idType="ISBN13"` | asks the other book database for the ISBN-13 the first one reported. Both are asked with the entered ISBN anyway, so this helps when you looked up with an ISBN-10 or the record carries a different ISBN-13 |
| `rebrickable` → `brickset`, `from="set_number"` | asks Brickset for the set. Not verified against Brickset's live API: Brickset's own `set_number` has no variant suffix, so this direction is more reliable than the reverse |

Chains to avoid: `brickset` → `rebrickable` (Brickset's bare set number is not the `75192-1` form Rebrickable expects), and anything from the games, film and TV providers, which emit only their own ids.

### Writing a new chain

1. Pick the source provider and a key from the "hands on" table that is present in its result. (A key that is missing simply skips the chain.)
2. Pick the target and an `idType` from the "accepts" table. Declare the target in the same module, with its own `priority` and `providerMappings`, or its result will not fill any field.
3. Add `<chain from="…" to="…" idType="…"/>` under the source provider.
4. Look up an item in the add-item wizard or with `POST /api/providers/lookup`: the response's `results` lists every provider that answered, chained ones included.

If you add a provider that knows another service's id, emit it under that service's key (for example `comicvine_id`) so other modules can chain it, and add it to the tables above.

## Adding a provider

1. Add a class under `src/main/java/org/rostislav/curiokeep/providers/impl/` annotated `@Component` that implements `MetadataProvider`: `key()` (permanent: modules refer to it), `supports(idType)`, `fetch(idType, value)`, and `credentialFields()` if it needs a key.
2. Use the shared `RestClient` (5 s connection and 10 s read timeouts). Treat the response as untrusted: tolerate missing fields and empty bodies, and return `Optional.empty()` instead of throwing when the service has nothing or is unavailable. Read credentials through `ProviderCredentialLookup` and skip the lookup when they are missing.
3. Build `normalizedFields` from the vocabulary above and add a new key only when no existing one has the same meaning. Return cover images as assets and a `confidence`.
4. If the provider can name records in other services, or can be looked up by an identifier other providers emit, list it in the [chaining tables](#chaining-providers).
5. Add a profile (display name, summary, links) in `ProviderKnowledgeBase`, and add the required attribution, if any, in `frontend/src/features/providers/providerAttribution.ts`.
6. Write a test with `MockRestServiceServer` covering a hit, a miss and a malformed or error response (see the existing tests in `src/test/java/.../providers`).
7. Reference the provider from a module's `<providers>` and add `providerMappings`.

Never send user identifiers to a service, and never put credentials in logs.
