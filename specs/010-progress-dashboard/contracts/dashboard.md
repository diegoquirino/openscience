# Contract: Progress dashboard

**Feature**: `010-progress-dashboard` | Rules from constitution Principle I and the API contract of
[spec 003](../../003-deployment-foundation/contracts/api.md). Administrator session as in
[spec 004](../../004-email-code-access/contracts/access.md).

Media types, `Content-Language`, versioning and Problem Details as in spec 003. Every operation
below is in the OpenAPI group `v1`, tag "Administration", and passes
`ApiDocumentationCompletenessTest`.

## Entry point `GET /api/v1`, administrator links

In addition to the links of 005�009, the administrator receives:

| Relation | Href | When |
|---|---|---|
| `vd:dashboard` | `/api/v1/dashboard` | always |

Participants and visitors never receive this relation.

## `GET /api/v1/dashboard` (`vd:dashboard`)

Administrator only. HAL JSON (`application/hal+json` or `application/prs.hal-forms+json`).
Read-only: the operation MUST NOT insert, update or delete any row (FR-007).

```json
{
  "generatedAt": "2026-11-16T12:00:00Z",
  "firstFreeQueuePosition": 3,
  "progress": [
    {
      "system": null,
      "batchId": null,
      "batchFileName": null,
      "items": 20,
      "blocks": 2,
      "role1": { "free": 1, "inProgress": 1, "completed": 0, "completedPercent": 0.0 },
      "role2": { "free": 2, "inProgress": 0, "completed": 0, "completedPercent": 0.0 }
    },
    {
      "system": "minitest",
      "batchId": 1,
      "batchFileName": "veredito_minitest_vazio.csv",
      "items": 20,
      "blocks": 2,
      "role1": { "free": 1, "inProgress": 1, "completed": 0, "completedPercent": 0.0 },
      "role2": { "free": 2, "inProgress": 0, "completed": 0, "completedPercent": 0.0 }
    }
  ],
  "reservations": [
    {
      "pseudonym": "R-0001",
      "system": "minitest",
      "block": 1,
      "role": 1,
      "draftCount": 3,
      "expiresAt": "2026-11-16T12:30:00Z",
      "expired": false
    }
  ],
  "participants": {
    "eligible": 2,
    "ineligible": 1,
    "declinedConsent": 0,
    "incomplete": 3,
    "bySkill": { "software_engineer": 2, "researcher": 1 },
    "completedByPseudonym": [
      { "pseudonym": "R-0001", "completedTasks": 2, "shareOfCompleted": 1.0 }
    ]
  },
  "languages": [
    { "language": "pt-BR", "participantsPreferred": 4, "finalResponses": 10 },
    { "language": "en-US", "participantsPreferred": 1, "finalResponses": 0 },
    { "language": "es-ES", "participantsPreferred": 1, "finalResponses": 0 }
  ],
  "emailQuota": {
    "emailsSentLast24h": 12,
    "dailyEmailLimit": 450,
    "emailsRemaining": 438,
    "lowRemaining": false
  },
  "_links": {
    "self": { "href": "/api/v1/dashboard" },
    "curies": [ { "name": "vd", "href": "/api/v1/rels/{rel}", "templated": true } ]
  }
}
```

### Field rules

| Field | Rule |
|---|---|
| `progress[0]` | Overall total: `system` and `batchId` are JSON `null` |
| Later `progress` rows | One per distinct `(item.system, block.batch_id)` present in the study, ordered by first appearance in queue order |
| Task buckets | Research �3 (`free` includes expired reserved) |
| `firstFreeQueuePosition` | Minimum `queue_position` among free/reclaimable tasks; JSON `null` when none |
| `reservations` | Every `status = reserved` task; `expired` when `expiresAt <= generatedAt` |
| `participants.*` | Research �5; anonymized excluded |
| `languages` | Always the three locales in that order, zeros allowed |
| `emailQuota` | Rolling 24 h vs `config.daily_email_limit`; `lowRemaining` when `emailsRemaining < 10` |
| Empty study | Zeros / empty lists; `_links` also include `vd:batches` ? `/api/v1/batches` |

No `name`, `email` or other PII fields appear anywhere in the document.

As elsewhere in the API, the curies link appears only when the document carries a d: relation, so a populated snapshot has self alone and an empty study has self, curies and d:batches.

| Error | Status | Type |
|---|---|---|
| Not signed in | 401 | `authentication-required` |
| Participant | 403 | `forbidden` |

## Link relation

| Relation | Method | Documentation |
|---|---|---|
| `dashboard` | GET | Documented at `/api/v1/rels/dashboard` |

## Security

`/api/v1/dashboard/**` is part of the administrator matcher in `SecurityConfig`.
