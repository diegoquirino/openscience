# Contract: API index, entry point and relations

**Feature**: `003-deployment-foundation` | Rules from constitution Principle I and
[research 001](../../001-oracle-labeling/research.md#rest-api-richardson-level-3-glory-of-rest).

Media types: `application/prs.hal-forms+json` (default for `Accept: */*` and
`application/hal+json`), `application/problem+json` for errors. Every response carries
`Content-Language` with the resolved locale.

## `GET /api` (version index)

Outside any version; not part of the contract of `v1`. Documented in the OpenAPI group `index`.

**200**

```json
{
  "versions": [
    { "version": "v1", "deprecated": false }
  ],
  "_links": {
    "self": { "href": "/api" },
    "curies": [ { "name": "vd", "href": "/api/v1/rels/{rel}", "templated": true } ],
    "vd:v1": { "href": "/api/v1" }
  }
}
```

A deprecated version would carry `"deprecated": true` and `"sunset"` (ISO 8601 date); none exists in
003.

## `GET /api/v1` (entry point)

No session in 003 (sessions arrive in spec 004). Documented in the OpenAPI group `v1`, tag
"Entry point".

**200**, database available

```json
{
  "systemVersion": "0.3.0+1a2b3c4",
  "environment": "staging",
  "available": true,
  "_links": {
    "self": { "href": "/api/v1" },
    "curies": [ { "name": "vd", "href": "/api/v1/rels/{rel}", "templated": true } ],
    "vd:request-code": { "href": "/api/v1/access-codes" }
  }
}
```

**200**, database unreachable: same body with `"available": false` and only `self` and `curies`
in `_links`.

| Field | Type | Meaning |
|---|---|---|
| `systemVersion` | string | `<project version>+<short commit>` (research, section 9) |
| `environment` | string | `production`, `staging` or `development` |
| `available` | boolean | `false` when the database does not answer the validation query within 2 seconds |

Links by caller, now and in later specs:

| Caller | Links | Since |
|---|---|---|
| Visitor (no session) | `self`, `curies`, `vd:request-code` | 003 (the operation behind `vd:request-code` comes in 004; until then it answers `404` Problem Details) |
| Participant | `vd:me`, `vd:consent-form`, `vd:next-task` (when eligible) | 004, 006, 007 |
| Administrator | `vd:me`, `vd:batches`, `vd:exports`, `vd:dashboard`, `vd:participants` | 004, 005, 009, 010, 011 |

No administrative link ever appears for a visitor (US5 scenario 2).

## `GET /api/v1/rels/{rel}`

Documents a `vd:` relation; `rel` without the prefix (`request-code`).

**200**

```json
{
  "rel": "vd:request-code",
  "description": "Request a sign-in code by e-mail.",
  "method": "POST",
  "documentation": "/swagger-ui.html#/Access",
  "_links": { "self": { "href": "/api/v1/rels/request-code" } }
}
```

**404** Problem Details `not-found` for an unknown relation. The text is English documentation for
developers, not a user-facing message.

## Versioning rules

| Request | Result |
|---|---|
| `GET /api/v1/...` | Handled by `v1`; every link in the response stays under `/api/v1/` |
| `GET /api/v9/...` or `/api/vx/...` | `400` Problem Details `unsupported-api-version`, extension `supportedVersions: ["v1"]` |
| A version marked deprecated (none in 003) | Normal response plus `Deprecation` (RFC 9745) and `Sunset` (RFC 8594) headers |
| `GET /api/v1/unknown` | `404` Problem Details `not-found` |
| Unsupported method on a known resource | `405` Problem Details `method-not-allowed`, with `Allow` |
| `Accept` that cannot be satisfied | `406` Problem Details `not-acceptable` |

## API documentation

| Address | Profile `staging` and development | Profile `prod` |
|---|---|---|
| `/swagger-ui.html` | open | `401` Problem Details `authentication-required` without an administrator session (always in 003) |
| `/v3/api-docs/v1`, `/v3/api-docs.yaml/v1` | open | same as above |
| `/v3/api-docs/index` | open | same as above |

Every operation in each group MUST have `summary`, `description`, at least one success example and
every error response it can return with the Problem Details schema
(`ApiDocumentationCompletenessTest`, FR-013, SC-007). OpenAPI `info`: title "Veredito API",
`info.version` = API version (`v1`), `info.description` with the study description and the
`systemVersion`, contact and license.
