# Contract: Tasks, reservations, drafts and submission

**Feature**: `007-block-queue` | Rules from constitution Principle I and the API contract of
[spec 003](../../003-deployment-foundation/contracts/api.md). Access and participant session as in
[spec 004](../../004-email-code-access/contracts/access.md). Paths and relations fixed by
[GLOSSARY](../../GLOSSARY.md) and FR-018.

Media types, `Content-Language`, versioning and Problem Details as in spec 003. Every operation
below is in the OpenAPI group `v1`, tag "Tasks", and passes
`ApiDocumentationCompletenessTest`.

## Entry point `GET /api/v1`, participant links

In addition to the links of 004, an eligible participant receives:

| Relation | Href | When |
|---|---|---|
| `vd:next-task` | `/api/v1/tasks/reservations` | participant session and `EligibilityGate` true |

Visitors and administrators never receive `vd:next-task`. Until 006, every participant with a
session is eligible. `AuthorizationTest` enumerates every "Tasks" operation from the OpenAPI and
denies them to visitors and administrators (administrators keep only Administration links).

## `POST /api/v1/tasks/reservations` (`vd:next-task` / `vd:reserve`)

Participant only. Empty body. HAL-FORMS template on the entry point may title it "Request next
block".

### New reservation — `201`

Header `Location: /api/v1/tasks/{id}`. Body = task representation (below).

### Existing valid reservation — `200`

Same body and `self` link; no new `Location` required (idempotent resume).

### No task handed out — `200`

```json
{
  "outcome": "no-items-yet",
  "_links": {
    "self": { "href": "/api/v1/tasks/reservations" },
    "curies": [ { "name": "vd", "href": "/api/v1/rels/{rel}", "templated": true } ]
  }
}
```

`outcome` is one of `no-items-yet` | `database-complete` | `none-available-for-you` (research §4).
Localized end-screen copy comes from message keys / XLIFF; the slug is stable.

| Error | Status | Type | When |
|---|---|---|---|
| No / invalid session | 401 | `authentication-required` | as in 004 |
| Not a participant / not eligible | 403 | `forbidden` / `not-eligible` | administrator cookie, or gate false (006+) |
| Unique conflict on both roles | 409 | `task-conflict` | defensive; should not happen if chooser is correct |

## Task representation `GET /api/v1/tasks/{id}`

Owner only while `reserved` or `completed` by that participant; others get `404` (no leakage of
existence to the other rater).

```json
{
  "id": 41,
  "blockNumber": 1,
  "role": 1,
  "status": "reserved",
  "size": 10,
  "answeredCount": 3,
  "reservationExpiresAt": "2026-11-15T18:20:00Z",
  "reservationMinutes": 20,
  "outcome": null,
  "serverNow": "2026-11-15T18:00:00.123Z",
  "_links": {
    "self": { "href": "/api/v1/tasks/41" },
    "vd:items": { "href": "/api/v1/tasks/41/items/{position}", "templated": true },
    "vd:review": { "href": "/api/v1/tasks/41" },
    "vd:save-response": { "href": "/api/v1/tasks/41/responses/{itemId}", "templated": true },
    "vd:submit": { "href": "/api/v1/tasks/41/submission" }
  },
  "_templates": {
    "default": {
      "title": "Save response",
      "method": "PUT",
      "contentType": "application/json",
      "properties": [
        { "name": "changeImpact", "type": "text", "required": true },
        { "name": "maintenanceOperation", "type": "text", "required": true },
        { "name": "comment", "type": "text", "required": false }
      ]
    }
  }
}
```

| Field | Rule |
|---|---|
| `answeredCount` | Number of drafts (or finals when completed) for this task |
| `reservationExpiresAt` / `reservationMinutes` | Present when `status = reserved`; omitted when completed |
| `serverNow` | UTC instant used to build the response; clients may prefer the HTTP `Date` header (SC-005) |
| `outcome` | Set on completed tasks (and on reservation outcomes without a task); otherwise `null` |
| `vd:submit` | Present only when `answeredCount == size` and status is `reserved` |

## `GET /api/v1/tasks/{id}/items/{position}`

`position` is 1 .. `size`. Returns the item payload for the simple screen plus the caller's draft
if any. Never includes the other role's response or identity.

```json
{
  "position": 1,
  "itemId": 9001,
  "system": "minitest",
  "uc": "UC01",
  "kind": "MOD",
  "field": "step",
  "originVersion": "1.1",
  "targetVersion": "1.2",
  "originUsecase": "…",
  "targetUsecase": "…",
  "origin": "…",
  "target": "…",
  "draft": {
    "changeImpact": "LOW",
    "maintenanceOperation": "KEEP",
    "comment": null,
    "language": "pt-BR"
  },
  "_links": {
    "self": { "href": "/api/v1/tasks/41/items/1" },
    "vd:save-response": { "href": "/api/v1/tasks/41/responses/9001" }
  }
}
```

`draft` is `null` when unanswered. Empty optional sides are JSON `null`.

## `PUT /api/v1/tasks/{id}/responses/{itemId}` (`vd:save-response`)

```json
{
  "changeImpact": "HIGH",
  "maintenanceOperation": "UPDATE",
  "comment": "optional, up to 1000 characters"
}
```

**200** with the refreshed task representation (new `reservationExpiresAt`). Repeating the same
body is idempotent (single row).

| Error | Status | Type | When |
|---|---|---|---|
| Validation | 400 | `invalid-request` | missing/invalid codes, comment too long |
| Not holder / taken by other | 409 | `reservation-expired` | expired and reclaimed, or never held |
| Task completed | 409 | `task-already-submitted` | finals locked |
| Unknown item for task | 404 | `not-found` | item not in this block |

## `POST /api/v1/tasks/{id}/submission` (`vd:submit`)

Empty body. **200** with completed task (`status: completed`, `outcome` set, no save/submit links).
Resubmitting the same completed task by the same participant returns **200** unchanged (FR-007).

| Error | Status | Type | When |
|---|---|---|---|
| Incomplete | 409 | `block-incomplete` | extension `firstPendingPosition` |
| Not holder / expired taken | 409 | `reservation-expired` | as above |

## Authorization matrix (Tasks tag)

| Caller | Reserve / GET task / item / PUT / submit |
|---|---|
| Visitor | 401 |
| Other participant | 404 on foreign task; reserve only for self |
| Administrator | 403 |
| Owner participant | allowed when eligible |
