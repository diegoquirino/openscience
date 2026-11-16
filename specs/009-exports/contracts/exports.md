# Contract: Export downloads

**Feature**: `009-exports` | Rules from constitution Principle I and the API contract of
[spec 003](../../003-deployment-foundation/contracts/api.md). Administrator session as in
[spec 004](../../004-email-code-access/contracts/access.md).

Media types, `Content-Language`, versioning and Problem Details as in spec 003. Every operation
below is in the OpenAPI group `v1`, tag "Administration", and passes
`ApiDocumentationCompletenessTest`.

## Entry point `GET /api/v1`, administrator links

In addition to the links of 005/006/007/008, the administrator receives:

| Relation | Href | When |
|---|---|---|
| `vd:exports` | `/api/v1/exports` | always |

Participants and visitors never receive this relation.

## `GET /api/v1/exports` (`vd:exports`)

Administrator only. HAL JSON (`application/hal+json` or `application/prs.hal-forms+json`).

```json
{
  "systems": ["minitest"],
  "_links": {
    "self": { "href": "/api/v1/exports" },
    "curies": [ { "name": "vd", "href": "/api/v1/rels/{rel}", "templated": true } ],
    "vd:export-wide": { "href": "/api/v1/exports/wide" },
    "vd:export-long": { "href": "/api/v1/exports/long" },
    "vd:export-completo": { "href": "/api/v1/exports/completo?system=minitest" },
    "vd:export-rater1": { "href": "/api/v1/exports/rater1?system=minitest" },
    "vd:export-rater2": { "href": "/api/v1/exports/rater2?system=minitest" }
  }
}
```

When several systems exist, each of `vd:export-completo`, `vd:export-rater1` and
`vd:export-rater2` appears **once per system** (same relation name, distinct `href`s; the client
matches by the `system` query). `systems` lists each distinct `item.system` in the order of the
first item of that system in queue order. An empty study database yields `"systems": []` and only
`self`, `curies`, `vd:export-wide` and `vd:export-long`.

| Error | Status | Type |
|---|---|---|
| Not signed in | 401 | `authentication-required` |
| Participant | 403 | `forbidden` |

## Downloads (CSV)

All downloads: administrator only; `Content-Type: text/csv;charset=UTF-8`;
`Content-Disposition: attachment; filename="<name>"`; body UTF-8 without BOM, RFC 4180, `\n`
line endings. OpenAPI documents a `200` with `text/csv` example plus 401/403 Problem Details.

| Method | Path | Filename | Scope |
|---|---|---|---|
| GET | `/api/v1/exports/wide` | `veredito_wide.csv` | all systems |
| GET | `/api/v1/exports/long` | `veredito_long.csv` | all systems |
| GET | `/api/v1/exports/completo?system={system}` | `veredito_{system}_completo.csv` | one system |
| GET | `/api/v1/exports/rater1?system={system}` | `t1_{system}_rater1.csv` | one system |
| GET | `/api/v1/exports/rater2?system={system}` | `t1_{system}_rater2.csv` | one system |

Link relations for the downloads: `vd:export-wide`, `vd:export-long`, `vd:export-completo`,
`vd:export-rater1`, `vd:export-rater2` (documented at `/api/v1/rels/{rel}`).

### Headers

**Wide** — Appendix C of spec 001 (exact order).

**Long**:

```text
system,edit_key,block,role,pseudonym,professional_skill,participant_preferred_language,consent_language,change_impact,maintenance_operation,comment,response_language,reserved_at,submitted_at
```

**Completo** — US3 scenario 2 of the spec (exact order).

**Rater**:

```text
edit_key,uc,kind,field,origin,target,change_impact,maintenance_operation,comment
```

### Errors on downloads

| Error | Status | Type | When |
|---|---|---|---|
| Not signed in | 401 | `authentication-required` | |
| Participant | 403 | `forbidden` | |
| Missing `system` | 400 | `invalid-request` | completo / rater without query |
| Unknown `system` | 404 | `system-not-found` | no `item` with that `system` |

A known system with zero completed roles still returns `200` and the header plus one row per item
with blank label cells (rater / completo) or only final rows (long).

## Security

`/api/v1/exports/**` is part of the administrator matcher in `SecurityConfig`.
`AuthorizationTest` enumerates every Administration operation from OpenAPI, including these.
