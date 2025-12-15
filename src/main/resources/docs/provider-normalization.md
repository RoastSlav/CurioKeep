# docs/provider-normalization.md

# Provider Normalization Contract (v1)

Modules must not depend on provider-specific JSON structures.

Instead, each metadata provider adapter (OpenLibrary, Google Books, TMDB, IGDB, Discogs, BGG, etc) returns a **normalized payload** in a consistent CurioKeep shape.

Module `providerMappings.path` always points into this normalized payload.

---

## 1) Goals

Normalization exists to:
- isolate provider API churn
- keep module authoring simple
- unify merging across providers
- avoid embedding JSONPath/logic inside module files

If a provider changes its response format, you update ONE adapter, not every module.

---

## 2) Normalized payload shape

A provider lookup returns:

```json
{
  "provider": "googlebooks",
  "providerId": "abc123",
  "canonicalUrl": "https://...",
  "retrievedAt": "2025-12-15T00:00:00Z",
  "confidence": 0.85,

  "data": {
    "title": "…",
    "subtitle": "…",
    "description": "…",

    "identifiers": {
      "isbn10": "…",
      "isbn13": "…",
      "upc": "…",
      "ean": "…",
      "asin": "…"
    },

    "creators": {
      "authors": ["…", "…"],
      "contributors": ["…"]
    },

    "publisher": "…",
    "publishedYear": 2020,
    "language": "en",
    "pageCount": 352,

    "images": {
      "coverSmall": "https://...",
      "coverLarge": "https://..."
    },

    "tags": ["…"],
    "extra": { "anything": "provider-specific-but-stable-after-normalization" }
  }
}
