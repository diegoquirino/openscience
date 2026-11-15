# Contract: Import batches, previews and study phase

**Feature**: `005-block-import` | Rules from constitution Principle I and the API contract of
[spec 003](../../003-deployment-foundation/contracts/api.md). Access and administrator session as in
[spec 004](../../004-email-code-access/contracts/access.md).

Media types, `Content-Language`, versioning and Problem Details as in spec 003. Every operation
below is in the OpenAPI group `v1`, tag "Administration", and passes
`ApiDocumentationCompletenessTest`.

## Entry point `GET /api/v1`, administrator links

In addition to the links of 004, the administrator receives:

| Relation | Href | When |
|---|---|---|
| `vd:batches` | `/api/v1/batches` | always |
| `vd:request-preview` | `/api/v1/batches/previews` | always |
| `vd:study-phase` | `/api/v1/administrator/study-phase` | always (body tells whether a switch is possible) |

Participants and visitors never receive these relations. `AuthorizationTest` of 004 already
enumerates every "Administration" operation from the OpenAPI.

## `POST /api/v1/batches/previews` (`vd:request-preview`)

Administrator only. Multipart form field `file` (CSV).

**201**, header `Location: /api/v1/batches/previews/{id}`

```json
{
  "id": 12,
  "fileName": "veredito_minitest_vazio.csv",
  "expiresAt": "2026-11-14T23:30:00Z",
  "includedCount": 38,
  "ignoredCount": 0,
  "errorCount": 0,
  "blockCount": 4,
  "blockSizes": [10, 10, 10, 8],
  "extraColumns": [],
  "ignored": [],
  "errors": [],
  "_links": {
    "self": { "href": "/api/v1/batches/previews/12" },
    "curies": [ { "name": "vd", "href": "/api/v1/rels/{rel}", "templated": true } ],
    "vd:confirm": { "href": "/api/v1/batches" },
    "vd:batches": { "href": "/api/v1/batches" }
  },
  "_templates": {
    "default": {
      "title": "Confirm import",
      "method": "POST",
      "contentType": "application/json",
      "properties": [
        { "name": "previewId", "type": "number", "required": true, "value": 12 }
      ]
    }
  }
}
```

`ignored` / `errors` entries: `{ "line": 15, "code": "already-imported", "reason": "…", "system": "minitest", "editKey": "…" }`.
`reason` is localized; `code` is the stable slug. The preview does **not** embed the included row
payloads in the HTTP response (only counts and block sizes); the server keeps them in
`import_preview.included_json`.

Empty file or header-only: `201` with zero counts; `_templates` omitted and confirm disabled on the
client (edge case).

| Error | Status | Type | When |
|---|---|---|---|
| Not administrator | 401 / 403 | `authentication-required` / `forbidden` | as in 004 |
| Missing file | 400 | `invalid-request` | `errors`: `{ "field": "file", "code": "required" }` |
| Not UTF-8 | 400 | `csv-not-utf8` | decode failure |
| Not CSV / unreadable | 400 | `csv-invalid` | parser failure |
| Missing required column | 400 | `csv-missing-column` | extension `column` |
| Non-Minitest while staging/`pilot` | 422 | `minitest-only` | extensions `systems` (distinct foreign values) |
| File too large | 400 | `invalid-request` | multipart limit |

## `GET /api/v1/batches/previews/{id}`

Administrator only. Same body as `201` if the preview exists and has not expired; otherwise
`404` `preview-not-found`.

## `POST /api/v1/batches` (`vd:confirm`)

Administrator only.

**Request**

```json
{ "previewId": 12 }
```

**201** when at least one item is inserted; header `Location: /api/v1/batches/{id}`

```json
{
  "id": 3,
  "fileName": "veredito_minitest_vazio.csv",
  "importedAt": "2026-11-14T23:05:00Z",
  "includedCount": 38,
  "ignoredCount": 0,
  "errorCount": 0,
  "blockCount": 4,
  "blockSizes": [10, 10, 10, 8],
  "queuePositions": { "from": 1, "to": 8 },
  "systems": ["minitest"],
  "_links": {
    "self": { "href": "/api/v1/batches/3" },
    "curies": [ { "name": "vd", "href": "/api/v1/rels/{rel}", "templated": true } ],
    "vd:batches": { "href": "/api/v1/batches" }
  }
}
```

**200** when the uniqueness re-check inserts nothing (double confirm / two tabs): same shape without
`id` / `Location` / `importedAt` of a new batch, with `includedCount: 0` and every former included
row listed under `ignored` with code `already-imported` (US2 scenario 5).

| Error | Status | Type | When |
|---|---|---|---|
| Invalid body | 400 | `invalid-request` | missing `previewId` |
| Unknown / expired preview | 404 | `preview-not-found` | |
| Not administrator | 401 / 403 | as above | |

Confirm is atomic: either all new rows of the batch are visible or none are (FR-008).

## `GET /api/v1/batches` (`vd:batches`)

Administrator only. Newest first.

```json
{
  "_embedded": {
    "vd:batches": [
      {
        "id": 3,
        "fileName": "veredito_minitest_vazio.csv",
        "importedAt": "2026-11-14T23:05:00Z",
        "includedCount": 38,
        "ignoredCount": 0,
        "errorCount": 0,
        "blockCount": 4,
        "systems": ["minitest"],
        "_links": { "self": { "href": "/api/v1/batches/3" } }
      }
    ]
  },
  "_links": {
    "self": { "href": "/api/v1/batches" },
    "vd:request-preview": { "href": "/api/v1/batches/previews" }
  }
}
```

## `GET /api/v1/batches/{id}`

Administrator only. Full summary as in the `201` body; `404` if missing.

## `GET` / `PUT /api/v1/administrator/study-phase` (`vd:study-phase`)

Administrator only.

**GET 200**

```json
{
  "phase": "pilot",
  "changedAt": null,
  "switchAllowed": false,
  "minitestOnly": true,
  "_links": {
    "self": { "href": "/api/v1/administrator/study-phase" }
  }
}
```

`switchAllowed` is `true` only when `phase` is `pilot`, the profile is not `staging`, and
`item`, `participant` and `response` are empty. When `switchAllowed` is true, the representation
includes a HAL-FORMS `PUT` template with property `phase` (required).

**PUT** body `{ "phase": "collection" }` ? **200** with `phase: "collection"` and `changedAt` set.

| Error | Status | Type | When |
|---|---|---|---|
| Staging profile | 404 | `not-found` (or omit the resource) | US4 scenario 5; prefer no link + `404` on direct call |
| Not empty / not pilot | 409 | `phase-switch-not-allowed` | US4 scenario 2 |
| Wrong value | 400 | `invalid-request` | `phase` ? `collection` |

## Authorization matrix (new operations)

| Operation | Visitor | Participant | Administrator |
|---|---|---|---|
| All of the above | 401 | 403 | as documented |

## Link relations to document at `/api/v1/rels/{rel}`

`batches`, `request-preview`, `confirm`, `study-phase`.
