# Contract: Access codes, sessions and profiles

**Feature**: `004-email-code-access` | Rules from constitution Principle I and the API contract of
[spec 003](../../003-deployment-foundation/contracts/api.md).

Media types, `Content-Language`, versioning and Problem Details as in spec 003. Every operation
below is in the OpenAPI group `v1` and passes `ApiDocumentationCompletenessTest`. Tags: "Access"
for codes and sessions, "Participation" for the participant profile, "Administration" for the
administrator profile.

## Entry point `GET /api/v1`, links by caller

| Caller | Links (besides `self` and `curies`) |
|---|---|
| Visitor (no valid session) | `vd:request-code` |
| Participant | `vd:session`, `vd:me` (`/api/v1/participants/me`), `vd:sign-out` |
| Administrator | `vd:session`, `vd:me` (`/api/v1/administrator/me`), `vd:sign-out` |

Later specs add their links to the same table (spec 003, *Links by caller*). An expired or unknown
session cookie is treated as no session; the entry point never answers `401`.

## `POST /api/v1/access-codes` (`vd:request-code`)

Public. Sends a 6-digit code to the e-mail, in the language of `Accept-Language`.

**Request**

```json
{ "name": "Ana Souza", "email": "Ana@Example.com " }
```

**200**

```json
{
  "email": "ana@example.com",
  "validityMinutes": 10,
  "resendAfterSeconds": 60,
  "_links": {
    "self": { "href": "/api/v1/access-codes" },
    "curies": [ { "name": "vd", "href": "/api/v1/rels/{rel}", "templated": true } ],
    "vd:sign-in": { "href": "/api/v1/access-sessions" },
    "vd:request-code": { "href": "/api/v1/access-codes" }
  }
}
```

`200` and not `201`: the code is not a resource the client can read back.

| Error | Status | Type | When |
|---|---|---|---|
| Invalid field | 400 | `invalid-request` | `errors`: `{ "field": "name", "code": "size" }`, `{ "field": "email", "code": "email" }` (US1 scenario 2) |
| Too soon | 429 | `code-resend-too-soon` | Latest code of the e-mail less than 60 s old; `retryAfterSeconds`, header `Retry-After` |
| Hourly limit | 429 | `code-hourly-limit` | 5 codes of the e-mail in the last hour; `retryAt` (ISO 8601 UTC), header `Retry-After` |
| Daily limit | 429 | `daily-email-limit` | Codes of the last 24 h reached `config.daily_email_limit` |
| Not sent | 503 | `code-not-sent` | SMTP failure or timeout; no code is valid |

## `POST /api/v1/access-sessions` (`vd:sign-in`)

Public. Validates the code and signs in.

**Request**

```json
{ "email": "ana@example.com", "code": "123 456" }
```

**201**, header `Location: /api/v1/access-sessions/current` and
`Set-Cookie: veredito-session=<token>; Path=/; Max-Age=86400; HttpOnly; Secure; SameSite=Lax`.
Body: the session representation below.

| Error | Status | Type | When |
|---|---|---|---|
| Invalid field | 400 | `invalid-request` | Missing e-mail or code |
| Wrong code | 422 | `invalid-code` | The code does not match and the code is still alive; `attemptsLeft` (1 to 4) |
| Dead code | 422 | `code-expired` | Expired, used, invalidated by 5 wrong attempts, replaced by a newer code, or no code at all for this e-mail; the client offers a new code (US1 scenarios 4 and 5) |

Neither message tells whether the e-mail is registered (edge case).

## `GET /api/v1/access-sessions/current` (`vd:session`)

Participant or administrator.

**200**, participant

```json
{
  "subject": "participant",
  "name": "Ana Souza",
  "pseudonym": "R-0001",
  "preferredLanguage": "es-ES",
  "expiresAt": "2026-11-14T17:03:00Z",
  "_links": {
    "self": { "href": "/api/v1/access-sessions/current" },
    "curies": [ { "name": "vd", "href": "/api/v1/rels/{rel}", "templated": true } ],
    "vd:me": { "href": "/api/v1/participants/me" },
    "vd:sign-out": { "href": "/api/v1/access-sessions/current" }
  }
}
```

**200**, administrator: `subject` `administrator`, no `pseudonym`, `vd:me` pointing to
`/api/v1/administrator/me`.

**401** `authentication-required` without a valid session.

## `DELETE /api/v1/access-sessions/current` (`vd:sign-out`)

Participant or administrator. **204**, deletes the session row and sends
`Set-Cookie: veredito-session=; Path=/; Max-Age=0; HttpOnly; Secure; SameSite=Lax`.
**401** without a valid session.

## `GET /api/v1/participants/me` (`vd:me` of a participant)

Participant only.

**200**

```json
{
  "pseudonym": "R-0001",
  "name": "Ana Souza",
  "preferredLanguage": "es-ES",
  "registeredAt": "2026-11-13T17:03:00Z",
  "_links": { "self": { "href": "/api/v1/participants/me" } },
  "_templates": {
    "default": {
      "method": "PUT",
      "properties": [
        { "name": "preferredLanguage", "required": true,
          "options": { "inline": ["pt-BR", "en-US", "es-ES"], "minItems": 1, "maxItems": 1 } }
      ]
    }
  }
}
```

**401** without a session; **403** `forbidden` for the administrator (US2 scenario 4).

## `PUT /api/v1/participants/me`

Participant only. Body `{ "preferredLanguage": "en-US" }`. **200** with the representation above.
**400** `invalid-request` for a value outside the three locales (`code`: `locale`). Idempotent.

## `GET /api/v1/administrator/me` and `PUT /api/v1/administrator/me` (`vd:me` of the administrator)

Administrator only; tag "Administration".

**200**

```json
{
  "name": "Diego Quirino",
  "email": "diegoquirino@gmail.com",
  "preferredLanguage": "pt-BR",
  "_links": { "self": { "href": "/api/v1/administrator/me" } },
  "_templates": { "default": { "method": "PUT", "properties": [ { "name": "preferredLanguage" } ] } }
}
```

`PUT` takes `{ "preferredLanguage": "es-ES" }` and stores it in
`config.admin_preferred_language`. **401** without a session; **403** `forbidden` for a
participant (logged, US2 scenario 3).

## Relations added to `/api/v1/rels/{rel}`

| Relation | Method | Description |
|---|---|---|
| `vd:sign-in` | `POST` | Validate an access code and sign in |
| `vd:session` | `GET` | The current session: who is signed in, the profile language and the expiry |
| `vd:sign-out` | `DELETE` | End the current session |
| `vd:me` | `GET`, `PUT` | Own profile; `PUT` changes the preferred language |

`vd:request-code` (spec 003) now points to a working operation.

## Authorization rules

| Resource | Visitor | Participant | Administrator |
|---|---|---|---|
| `GET /api`, `GET /api/v1`, `GET /api/v1/rels/**` | yes | yes | yes |
| `POST /api/v1/access-codes`, `POST /api/v1/access-sessions` | yes | yes | yes |
| `GET`, `DELETE /api/v1/access-sessions/current` | `401` | yes | yes |
| `/api/v1/participants/**` | `401` | yes | `403` |
| `/api/v1/administrator/**` and every operation tagged "Administration" | `401` | `403` | yes |
| Swagger UI and `/v3/api-docs/**` in production | `401` | `401` | yes |

Swagger keeps answering `401` to a participant in production, as in spec 003: the documentation is
for the administrator only. Every refusal on an administrative resource is logged at `WARN`
(caller kind, pseudonym if any, method, path) without e-mail or token.
