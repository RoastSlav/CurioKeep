# docs/modules.md

# CurioKeep Modules (XML): Philosophy, Contract, and Lifecycle

CurioKeep is a generic catalog engine. **Modules** are what make it useful.

A module is a **declarative contract** (an XML file) that describes:
- which **fields** exist for a type of collectible
- which **states** an item can be in (e.g., OWNED/WISHLIST)
- which **metadata providers** can enrich it (OpenLibrary/TMDB/etc)
- which **workflows** (wizards) make sense for adding/updating items

Modules are **data**, not code.

This design keeps the core app stable and secure while letting the community extend it with new collectible types without plugin execution risks.

---

## 1) Key principles

### 1.1 Modules describe *schema and behavior*, not implementation
Modules do NOT contain business logic, API calls, or scripts. They only reference:
- known step types (workflow)
- known providers (by key)
- known field/state definitions

Implementation lives in CurioKeep:
- backend validates, compiles, stores module definitions
- backend has provider adapters that fetch and normalize metadata
- frontend renders UI from module definitions

### 1.2 Provider mappings target normalized data
Module authors **never** map to raw provider JSON.

Providers return a **normalized payload** (CurioKeep contract).
Module mappings use JSON Pointer paths into that normalized payload.

This ensures:
- API changes in providers don’t break modules
- module authors don’t need to understand each provider’s JSON structure
- merging across providers is consistent

(See `docs/provider-normalization.md`.)

### 1.3 OWNED is mandatory
Every module must define an `OWNED` state. CurioKeep assumes it exists and may default to it.

---

## 2) File locations and sources

### Built-in modules (shipped with the app)
Placed in:
- `src/main/resources/modules/*.xml`

### User modules (added at runtime)
Option A (recommended): stored in DB as USER modules via UI upload.
Option B: a mounted folder like `/data/modules/*.xml` that CurioKeep imports.

In all cases, CurioKeep stores:
- the raw XML (`xml_raw`)
- checksum (`sha256`)
- compiled/normalized definition (`definition_json`)
- materialized states/fields rows (optional, but recommended for querying)

---

## 3) Module lifecycle in the backend

On startup (and on module upload), CurioKeep does:

1. **Load**
    - built-ins from classpath
    - user modules from DB or folder

2. **Structural validation (XSD)**
    - catch “typos”: missing elements, wrong attributes, invalid enums, etc.

3. **Semantic validation (CurioKeep rules)**
   Examples:
    - module key conforms to naming rules
    - field keys unique
    - state keys unique
    - OWNED exists
    - workflows reference existing fields/providers
    - mapping paths start with `/`
    - field `type=ENUM` has enumValues
    - identifier fields declare identifiers if they should populate `item_identifier`

4. **Compile**
    - convert XML -> internal DTO -> JSON (`definition_json`)
    - compute maps for fast access (fieldByKey/stateByKey/providerByKey)
    - compute defaults (default state, default workflows)

5. **Persist**
    - upsert module_definition row by module_key
    - replace module_state/module_field rows for that module_key (transaction)
    - (optional) keep old versions for auditing later

---

## 4) What each XML section means

### `<module key="" version="">`
- `key`: immutable identifier for the module (used in DB, exports, URLs)
- `version`: semver-ish string for humans and for future migrations

**Never change `key` after publishing.** Treat it like a stable API.

### `<name>`, `<description>`
Display-only.

### `<meta>`
Metadata about the module itself (not item data):
- authors, license, repo, min app version, tags, icon

Used for:
- “module marketplace” UX later
- compatibility warnings
- attribution

### `<states>`
Declares allowed states for items in this module.
- state keys should be stable and uppercase snake case

Used for:
- validation (DB FK can enforce `(module_id, state_key)` validity)
- UI filters and counts
- workflows (optional later)

**Rule:** `OWNED` must exist.

### `<providers>`
Declares which metadata providers apply to this module.
A provider entry includes:
- key
- enabled default
- priority (used when multiple providers can fill the same fields)
- supported identifiers (helps UI decide which workflows are possible)

This is *declarative only*. The provider implementation is in the backend adapter layer.

### `<fields>`
Defines the module’s item schema.

Field flags:
- `required`: enforced at save time
- `searchable/filterable/sortable`: used to build UI and query capabilities
- `order`: controls UI ordering

Fields also define:
- identifiers they represent (ISBN/UPC/EAN/etc)
- enum values (for ENUM type)
- provider mappings (how metadata fills this field)

**Storage model:** item values live in `item.attributes` JSONB. The module definition tells CurioKeep how to interpret that JSON.

### `<providerMappings>`
A mapping says:

> “If provider X returns normalized data at `path`, use it to populate this field.”

Mappings are evaluated when you run a lookup workflow or a metadata refresh.

Paths are JSON Pointer into **normalized provider payload** (not raw API response).
Example:
- `/title`
- `/identifiers/isbn13`
- `/images/coverLarge`

Optionally a mapping can specify a simple `transform` (trim/join/etc). Keep transforms small and deterministic.

### `<workflows>`
Workflows are guided wizards that describe how you add/update items.

They exist for UX consistency and future clients (mobile, CLI).

Workflows are not code. They are a list of known step types such as:
- PROMPT (ask user for field)
- LOOKUP_METADATA (call providers)
- APPLY_METADATA (apply mappings)
- SAVE_ITEM

If you remove workflows entirely, CurioKeep can still operate with manual add/edit, but workflows make the app feel polished.

---

## 5) How the frontend uses modules

At runtime the frontend asks the backend for:
- enabled modules per collection
- module fields + UI hints
- module states
- workflows

The frontend renders:
- “Add item” dialog dynamically from fields
- filter panels dynamically from filterable fields
- state chips from states
- “Quick add” actions from workflows

The frontend does not hardcode “books” behavior beyond presentation defaults.

---

## 6) Authoring rules (what module authors should do)

- Keep modules focused (v1: 10–20 fields max per module)
- Don’t map to raw provider JSON; always use normalized paths
- Add identifiers only where they truly apply
- Keep field keys stable
- Provide at least:
    - title (required)
    - OWNED state
    - one workflow (manual add is fine)

---

## 7) Editor validation (IntelliJ-friendly)

Use XSD so authors get red squiggles immediately.

Add this to the `<module>` element:

```xml
<module
  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xsi:noNamespaceSchemaLocation="curiokeep-module-v1.xsd"
  key="books"
  version="1.0.0">
