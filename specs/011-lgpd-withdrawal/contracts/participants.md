# Contract: Administrator participants and LGPD withdrawal

**Feature**: `011-lgpd-withdrawal` | Rules from constitution Principle I and the API contract of
[spec 003](../../003-deployment-foundation/contracts/api.md). Administrator session as in
[spec 004](../../004-email-code-access/contracts/access.md). Participant self routes under
`/api/v1/participants/me` stay as in 006 and are out of scope here except for security split.

Media types, `Content-Language`, versioning and Problem Details as in spec 003. Every operation
below is in the OpenAPI group `v1`, tag "Administration", and passes
`ApiDocumentationCompletenessTest`.

## Entry point `GET /api/v1`, administrator links

In addition to the links of 005–010, the administrator receives:

| Relation | Href | When |
|---|---|---|
| `vd:participants` | `/api/v1/participants` | always |

Participants and visitors never receive this relation.

## `GET /api/v1/participants` (`vd:participants`)

Administrator only. Search collection.

Query: exactly one of:

| Param | Match |
|---|---|
| `email` | Exact, case-insensitive against the stored address |
| `pseudonym` | Exact against `participant.pseudonym` |

```json
{
  "_embedded": {
    "vd:participant": [
      {
        "id": 12,
        "pseudonym": "R-0007",
        "email": "ana@example.com",
        "preferredLanguage": "es-ES",
        "anonymizedAt": null,
        "completedTasks": 2,
        "reserved": { "block": 3, "role": 1, "expiresAt": "2026-11-16T15:00:00-03:00" },
        "finalResponseCount": 20,
        "draftResponseCount": 3,
        "tasksReturnableOnDiscard": 2,
        "emailCanBeSent": true,
        "_links": {
          "self": { "href": "/api/v1/participants/12" },
          "vd:anonymize": { "href": "/api/v1/participants/12/anonymizations" }
        }
      }
    ]
  },
  "_links": {
    "self": { "href": "/api/v1/participants?email=ana%40example.com" },
    "curies": [ { "name": "vd", "href": "/api/v1/rels/{rel}", "templated": true } ]
  }
}
```

Rules:

- Zero matches → `"vd:participant": []` (still `200`).
- Already anonymized (search by pseudonym) → card with `anonymizedAt` set, `email` omitted, no
  `vd:anonymize`.
- `reserved` is JSON `null` when the person holds no `status='reserved'` task.
- `emailCanBeSent` is `false` when the rolling 24 h quota has no remaining slot.
- Both `email` and `pseudonym`, or neither → `400` with a validation Problem Details.

| Error | Status | Type |
|---|---|---|
| Not signed in | 401 | `authentication-required` |
| Participant session | 403 | `forbidden` |
| Bad query | 400 | `validation-failed` (or project equivalent) |

## `GET /api/v1/participants/{id}`

Same card as the embedded item. `404` when missing. Administrator only.

## `POST /api/v1/participants/{id}/anonymizations` (`vd:anonymize`)

Administrator only. Body:

```json
{ "option": "keep_responses" }
```

or `"discard_responses"`.

Behaviour (research §4): attempt confirmation e-mail in `preferredLanguage` before mutating;
then atomic anonymization; write `withdrawal_log`.

`201 Created` with `Location: /api/v1/participants/{id}` and body:

```json
{
  "pseudonym": "R-0007",
  "option": "discard_responses",
  "emailSent": true,
  "finalResponsesDiscarded": 20,
  "tasksReturned": 2,
  "occurredAt": "2026-11-16T14:30:00-03:00",
  "_links": {
    "self": { "href": "/api/v1/participants/12" },
    "vd:participants": { "href": "/api/v1/participants" }
  }
}
```

| Error | Status | Type |
|---|---|---|
| Not signed in | 401 | `authentication-required` |
| Participant | 403 | `forbidden` |
| Unknown id | 404 | `not-found` |
| Already anonymized | 409 | `conflict` (or project equivalent) |
| Invalid option | 400 / 422 | validation |
| SMTP failure when a send was required (quota had room) | 503 / problem | `code-not-sent` or a dedicated withdrawal mail type |

When quota is exhausted, the operation still succeeds with `"emailSent": false`.

## Security path split

| Path | Role |
|---|---|
| `/api/v1/participants/me`, `/api/v1/participants/me/**` | `PARTICIPANT` |
| `/api/v1/participants`, `/api/v1/participants/{id}`, `/api/v1/participants/{id}/anonymizations` | `ADMINISTRATOR` |
