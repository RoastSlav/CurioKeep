# Modules

CurioKeep is a generic catalog engine; **modules** are what make it useful. A module is an XML file that describes one kind of collectible:

- the **fields** an item has (title, ISBN, condition, …),
- the **states** an item can be in (owned, wishlist, lent out, …),
- which metadata **providers** can fill the fields, and how,
- the **workflows** (add-item wizards) that make sense for it.

Modules are **data, not code**. They contain no logic, scripts or URLs. They only reference field keys, state keys, provider keys and a fixed set of workflow step types, so a module can be shared without any plugin-execution risk. The backend validates, compiles and stores modules; providers are Java adapters (see [providers](providers.md)); the frontend renders forms, filters and wizards straight from the compiled module.

Two modules ship with the app: `books` and `comics` (`src/main/resources/modules/`). Use them as examples.

## Adding and changing modules

| Way | What happens |
|---|---|
| **Import in the UI** (Modules page, admin only) or `POST /api/admin/modules/import` (multipart file) | The XML is validated, stored in the database and saved in the import directory (the file is named after the key and version). A module whose key already exists is rejected (`409`). |
| **Drop a file in the import directory** (`curiokeep.modules.import-dir`) and use **Scan** (`POST /api/admin/modules/scan`) | Every file whose key is not yet known is imported; the response lists what was imported, skipped (key exists) and failed (with the reason). |
| **Restart** | Bundled modules and every file in the import directory are validated and upserted again, so replacing a file and restarting updates that module. |

Deleting an imported module (`DELETE /api/admin/modules/{key}`) is refused while it is enabled in any collection or has items. Bundled modules cannot be deleted.

**Any invalid module stops the application from starting.** The error lists every module that failed and why. Import through the UI or API validates first, so a bad file can only get into the import directory by being placed there by hand. The one exception is an imported module whose `minAppVersion` is newer than the running application (for example after going back to an older release): it is skipped with a warning in the log and the application starts without it.

Changing a module does not rewrite stored items. Item attributes are kept as they were saved, and saving an item in the web form keeps every value the form does not show. A field you remove simply stops being shown and is no longer validated; its values stay in the database and in exports, but users cannot see them in the app any more. See [changing a module without losing data](#changing-a-module-without-losing-data).

## How a module is loaded

For each module file, in this order (`ModuleLoadTx`):

1. **XSD validation** against `schema/module-schema-v1.xsd`: structure, attribute patterns, enums and uniqueness of state, provider and field keys.
2. **Parse** the XML into records.
3. **Compile** into the module contract (defaults filled in, lists normalised) and serialise it to JSON.
4. **JSON-schema validation** of that contract against `schema/module-contract-v1.schema.json`.
5. **Semantic checks** that need the whole module (see the table below).
6. **Persist** in one transaction. If a module with the same key and checksum is already stored nothing is written. Otherwise the `module_definition` row is upserted, states are upserted (a state that is no longer declared is removed only when no item uses it), and the field rows are replaced.

### What is checked where

| Rule | Checked by |
|---|---|
| Module, provider and field keys match their patterns; state and enum keys are upper snake case | XSD |
| State, provider and field keys are unique | XSD |
| Provider mapping `path` starts with `/` | XSD |
| Field types, identifier types, workflow step types and transforms come from the allowed lists | XSD |
| A module has at least one state and one field | XSD |
| The module defines the state `OWNED` | semantic check |
| Every provider mapping refers to a provider the module declares | semantic check |
| Workflow steps refer only to declared fields and providers | semantic check |
| A `PROMPT` step names a field, or has a `query`; `query` is allowed only on `PROMPT` | semantic check |
| The application is at least the module's `<minAppVersion>` (a `-SNAPSHOT` suffix on the application version is ignored; nothing is refused when the version is unknown, as when running from an IDE) | semantic check |
| A `<chain>` names a provider the module declares other than its own, and chains do not lead back to where they started | semantic check |
| Migration versions are unique, are not newer than the module's `version`, and each step carries exactly the attributes its `op` uses | XSD, semantic check |
| A migration step's target field is declared and live (not deprecated or inactive); `MAP` is used on `ENUM` or `TAGS` fields, maps to declared values, and lists each source value once; `DEFAULT` holds a value the field accepts; `DROP` names a field that is no longer live | semantic check |

Not enforced: a field of type `ENUM` without `enumValues` is accepted (it renders an empty choice list), and `identifiers` on a field are not cross-checked against anything.

At run time the API enforces valid state keys, that `required` fields are present, and that every declared field holds a value of its type, answering `400 INVALID_FIELD_<key>` otherwise:

| Type | Accepted |
|---|---|
| `TEXT`, `LINK` | a string of at most 20,000 characters that satisfies `minLength`, `maxLength` and `pattern` |
| `NUMBER` | a JSON number within `min` and `max` |
| `BOOLEAN` | `true` or `false` |
| `DATE` | a string `YYYY`, `YYYY-MM` or `YYYY-MM-DD` |
| `ENUM` | one of the declared values (a list of them when `multi` is set); anything when none are declared |
| `TAGS` | a list of strings |
| `JSON` | anything |

Keys the module does not declare are left alone, and the attributes as a whole may not exceed 256 KB.

A field with `uniqueWithinCollection="true"` (`TEXT`, `LINK`, `NUMBER` or `DATE`) may hold a given value in only one item of the module per collection. Values are compared ignoring case and surrounding spaces, and an empty value never conflicts. A save that would repeat a value is refused with `409 DUPLICATE_FIELD_<key>`, and an import skips such an item with the same reason. Two saves at the same moment can both pass, and duplicates that already exist are left alone: editing an item that keeps its current value is never blocked by them.

## Changing a module without losing data

A module is updated by replacing its file (same `key`, a new `version`). Existing items are not touched, so the way you retire a field decides whether users keep their data. The app does not lose data on its own; what happens to a removed field's values is between the module author and the people using the module.

1. **Never change `key`s that are already in use.** A different key is a different field.
2. **Deprecate before you remove.** To rename or replace a field, keep the old one, mark it `deprecated="true"` and add the new one. Name the successor with `replacedBy="new_key"` when a value can simply move across. Release that version.
3. **What users see.** A deprecated field is not offered for new items and is not required. On an item that still holds a value in it, the edit form shows the field in a "Deprecated fields" section with a **Move to …** button when `replacedBy` is set. The collection page shows how many items still use each deprecated field and can list exactly those items (`<fieldKey>.has=true`). The values stay put until the user moves or clears them.
4. **Remove it in a later version.** Once users have had time to move their values, drop the field. Values that were not moved stay in the database and in JSON exports, but the app no longer shows or validates them, and they are lost to the user in practice.

A deprecated `required` field never blocks saving. States can be deprecated in the same way (`deprecated="true"` on `<state>`): a deprecated state is not offered in the state menus, and items already in it keep it.

### Migrating items automatically

A module can also say how to bring items saved under an older version up to date, so users do not have to move values by hand. Every item remembers the module version it was last brought up to. When the module's `version` is higher than an item's and the module has a `<migration>` for the versions in between, the collection page tells its admins how many items can be migrated. An admin opens a preview (how many items are affected and a few examples of what would change), and accepts it. Only then are the items rewritten; ordinary members never trigger it and nothing changes on its own.

Accepting is a plain rewrite: there is no undo and no history. Items a user edited in the meantime are migrated like the others. Steps are conservative on purpose (see [`<migrations>`](#migrations-optional)): they never overwrite a value the item already has and never store a value the new field would reject, so an item a step does not fit is left alone. Items the module has no migration for keep their old version and stay usable, and a later version's migration still applies to them.

## The XML format

`key` is the module's permanent identity (used in the database and URLs). **Never change it after publishing.**

```xml
<module xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:noNamespaceSchemaLocation="module-schema-v1.xsd"
        key="books" version="1.0.0">
    <name>Books</name>
    <description>Physical paper books.</description>

    <meta> ... </meta>
    <states> ... </states>
    <providers> ... </providers>
    <fields> ... </fields>
    <workflows> ... </workflows>
    <migrations> ... </migrations>
</module>
```

To get validation and completion in your editor, point `xsi:noNamespaceSchemaLocation` at a local copy of `module-schema-v1.xsd`. (The bundled modules use `../schema/module-schema-v1.xsd`. The attribute is ignored when the application validates the file.)

### `<module>`

| Item | Notes |
|---|---|
| `key` (attribute) | `[a-z][a-z0-9_-]{1,63}`, required |
| `version` (attribute) | `major.minor.patch` with an optional suffix, required |
| `<name>` | display name, required |
| `<description>` | optional |

### `<meta>` (optional)

Information about the module itself: `<authors><author name="" email="" url=""/></authors>`, `<license>`, `<homepage>`, `<repository>`, `<icon>`, `<tags><tag>…</tag></tags>`, `<minAppVersion>`. It is shown in the Modules page and used for filtering. `<minAppVersion>` (`major.minor.patch`) is enforced: a module that needs a newer CurioKeep than the one running is refused with a message saying which version it needs.

### `<states>`

At least one `<state key="OWNED" label="Owned" order="1"/>`. Keys are upper snake case (`[A-Z][A-Z0-9_]{1,63}`). `active` and `deprecated` are optional booleans. **`OWNED` must exist.** The first state (by declaration order) is the default for new items. States drive the state chips, filters and batch actions.

### `<providers>` (optional)

```xml
<provider key="openlibrary" enabled="true">
    <supports>
        <identifier type="ISBN10"/>
        <identifier type="ISBN13"/>
    </supports>
    <priority>1</priority>
</provider>
```

Declares which providers this module may use. `enabled` defaults to on. `priority` (integer, **higher wins**) picks the "best" result when several providers answer. `supports` tells the UI which identifiers make a workflow possible. This is declarative only; the implementation lives in the backend.

#### Chaining providers

A provider's result often names a record in another service. A `<chain>` tells the lookup to follow that reference:

```xml
<provider key="metron" enabled="true">
    <supports> ... </supports>
    <priority>1</priority>
    <chain from="comicvine_id" to="comicvine"/>
</provider>
```

After `metron` returns a result, the lookup takes that result's normalized field `comicvine_id` and asks `comicvine` for it as a `CUSTOM` identifier. The Comic Vine result is then merged like any other, using its own priority and field mappings. Set `idType` (`ISBN10`, `ISBN13`, `UPC`, `EAN`, `ASIN` or `CUSTOM`, default `CUSTOM`) when the target expects another kind of identifier, for example `<chain from="isbn13" to="googlebooks" idType="ISBN13"/>`. A provider can declare several chains.

To add a chain: check in [providers.md](providers.md#chaining-providers) that the source provider emits the field you want in its normalized fields (the "what each provider hands on" table) and that the target accepts the identifier type and value format (the "what each provider accepts" table), declare the target in the same module, and add the `<chain>` line. Nothing else needs changing. The same page lists the chains that make sense between the bundled providers and the ones to avoid.

A chain is skipped, and the lookup carries on, when the result has no value for `from`, when the target is disabled or not in the request's provider list, when it does not support `idType`, or when it fails. A provider is asked for a given identifier at most once per lookup, and chains are followed at most 3 hops deep. Because the value is passed as it is, the target provider is responsible for reading the format it is given (Comic Vine accepts `12345` and `4000-12345`).

### `<fields>`

```xml
<field key="isbn13" label="ISBN-13" type="TEXT" searchable="true" filterable="true" order="20">
    <identifiers><identifier type="ISBN13"/></identifiers>
    <providerMappings>
        <map provider="openlibrary" path="/isbn13"/>
    </providerMappings>
</field>
```

| Attribute | Meaning |
|---|---|
| `key`, `label`, `type` | required. Key: `[a-z][a-z0-9_]{1,63}`. Type: `TEXT`, `NUMBER`, `DATE`, `BOOLEAN`, `ENUM`, `TAGS`, `LINK` or `JSON`. |
| `required` | enforced when an item is saved |
| `searchable`, `filterable`, `sortable` | `searchable` fields are searched, together with the title, by the items search; `filterable` fields get a filter in the filter dialog; `sortable` fields appear in the sort menu. The server enforces this: filtering or sorting on a field without the flag is refused with `400` |
| `order` | position in forms and lists |
| `active` | default `true`. A field with `active="false"` is not offered in forms and is no longer required |
| `deprecated` | default `false`. See [changing a module without losing data](#changing-a-module-without-losing-data) |
| `replacedBy` | on a deprecated field: the key of the field that takes over from it. The UI offers to move the value across. The target must be another field of the module that is not itself deprecated |

Children, all optional:

- `<identifiers>`: marks the field as holding an identifier (`ISBN10`, `ISBN13`, `UPC`, `EAN`, `ASIN`, `CUSTOM`). Identifier fields are what the add-item lookup sends to providers (the value entered is matched to a provider that supports that identifier type). The values are stored as ordinary attributes; the API can also record separate item identifiers, but the web app does not send them.
- `<enumValues><value key="HARDCOVER" label="Hardcover"/>…</enumValues>`: choices for an `ENUM` field.
- `<constraints min max minLength maxLength pattern multi uniqueWithinCollection/>`: `min` and `max` apply to `NUMBER`, `minLength`, `maxLength` and `pattern` (a regular expression, written as it is, so `\d` and not `\\d`) to `TEXT` and `LINK`. Both the web form and the server check them. `uniqueWithinCollection` is enforced by the server (see the table above). A constraint that cannot work (a pattern on a number, `min` above `max`, an invalid expression) is ignored and logged as a warning when the module loads. `multi` makes an `ENUM` field multi-select. `uniqueWithinCollection` is not enforced yet.
- `<ui widget group hidden>` with `<placeholder>` and `<helpText>`: presentation hints. Fields with the same `group` are shown together; `hidden` removes a field from the form.
- `<providerMappings>`: see below.
- `<defaultValue>`: the value a new item starts with. It is read as the field's type; one that cannot be read as such (text in a `NUMBER` field) is ignored with a warning.

**Storage.** Item values live in the `item.attributes` JSONB column, keyed by field key.

### Provider mappings

```xml
<map provider="comicvine" path="/writers" transform="JOIN_COMMA"/>
```

"If provider X returned a value at `path`, use it for this field." `path` is a JSON Pointer into the provider's **normalized result**, never its raw API response. It is a flat key such as `/title`, `/isbn13` or `/published_year`; the keys providers emit are listed in [providers](providers.md#normalized-result).

For a field the first mapping whose provider produced a value wins, and providers are consulted in the order the module declares them. Mappings apply when a lookup workflow runs.

The optional `transform` reshapes the value:

| Transform | Effect |
|---|---|
| `TRIM` | text without surrounding whitespace; blank text counts as "no value" |
| `JOIN_COMMA` | array of strings joined with `, `; blank entries dropped; empty array counts as "no value" |
| `FIRST` | first non-blank element of an array |
| `TO_INT` | integer from a number or numeric text; text that is not a number counts as "no value" |

A transform that does not fit the value's type (for example `JOIN_COMMA` on text) leaves the value unchanged. "No value" means the mapping is skipped and the next one is tried. Without a transform, an array or object is kept as its JSON text, so use `JOIN_COMMA` for list-valued providers that feed a text field.

### `<workflows>` (optional)

Workflows are guided add-item wizards. They are a list of known step types, not code:

| Step | Attributes | What the user sees |
|---|---|---|
| `PROMPT` | `field` (ask for that field) or `query` (ask for a free-text search, `query` is the placeholder); optional `label` | one input |
| `PROMPT_ANY` | `fields="isbn13,isbn10"` (comma-separated field keys); optional `label` | one input that accepts a scan or typed value for any of the listed identifier fields |
| `LOOKUP_METADATA` | `providers="a,b"` (comma-separated provider keys, all declared by the module) | runs the lookup with the identifiers or query entered so far |
| `APPLY_METADATA` | none | shows suggested values that differ from what is entered; the user applies all or some |
| `SAVE_ITEM` | none | shows the form for the remaining fields and saves |

The web app automatically inserts a cover-selection step before `SAVE_ITEM` (it says so when the providers returned no images). Without any workflows the app still offers a manual add form, but workflows make adding items much faster.

### `<migrations>` (optional)

Each `<migration to="2.0.0">` lists the steps that bring an item from any earlier version up to version `2.0.0`. When an item is migrated, the migrations after its version, up to the module's current `version`, run oldest first, and the steps of each run in the order written. `to` must be unique and not newer than the module's `version`.

```xml
<migrations>
    <migration to="2.0.0">
        <step op="MOVE" from="author_text" to="authors" transform="TRIM"/>
        <step op="MAP" field="binding"><mapping from="PERFECT" to="GLUED"/></step>
        <step op="DEFAULT" field="format" value="PAPERBACK"/>
        <step op="DROP" field="legacy_notes"/>
    </migration>
</migrations>
```

| Step | Attributes | What it does to one item |
|---|---|---|
| `MOVE` | `from`, `to`, optional `transform` | Puts the value of `from` into `to` and removes `from`. `to` must be a declared, live field; `from` can be a deprecated or already removed key |
| `COPY` | same as `MOVE` | The same, but keeps `from` |
| `MAP` | `field`, one or more `<mapping from="" to=""/>` | Replaces the listed values of an `ENUM` or `TAGS` field; values not listed stay as they are |
| `DEFAULT` | `field`, `value` | Sets the field when it is empty. `value` is read as the field's type (a number for `NUMBER`, `true` or `false` for `BOOLEAN`, comma separated entries for `TAGS`) |
| `DROP` | `field` | Deletes a key that is no longer a live field, so its data is gone for good |

`transform` is one of the [mapping transforms](#provider-mappings) (`TRIM`, `JOIN_COMMA`, `FIRST`, `TO_INT`), for when the old and new fields differ in type.

A step leaves an item unchanged instead of guessing when: `MOVE` or `COPY` would overwrite a value in `to`; the value, after the transform, is not valid for `to` (wrong type, outside its constraints, not a declared choice); or the source is empty (a `MOVE` still removes an empty source). Use a `transform` to convert, for example `TO_INT` to turn a text page count into a number.

## Authoring guidelines

- Keep a module focused: a handful of core fields plus what collectors of that thing actually track.
- Always map to normalized keys, never to raw provider JSON.
- Declare `identifiers` only where they truly apply, and give every module a `title` field, the `OWNED` state and at least a manual-add workflow.
- Treat keys as a stable API: never rename a field or state key you have published; add a new one instead.
- Change a published module additively within `v1` (new optional fields, states, mappings). A breaking change to the format itself needs a new schema version.
- Validate before you share: import the file into a development instance and check the load result.

## Where the format is defined

| File | Purpose |
|---|---|
| `src/main/resources/schema/module-schema-v1.xsd` | XML structure of a module file |
| `src/main/java/.../modules/xml/` | Records the XML is parsed into |
| `src/main/java/.../modules/contract/`, `ModuleCompiler` | The compiled contract and how XML becomes it |
| `src/main/resources/schema/module-contract-v1.schema.json` | JSON schema the compiled contract must satisfy |
| `frontend/src/features/modules/moduleTypes.ts` | The contract types the web app uses |

A change to the format touches all of them.
