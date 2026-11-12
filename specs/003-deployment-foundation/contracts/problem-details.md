# Contract: Problem Details and localized messages

**Feature**: `003-deployment-foundation` | RFC 9457 | FR-009, FR-012, FR-016, FR-017

## Format

`Content-Type: application/problem+json`, `Content-Language: <resolved locale>`.

```json
{
  "type": "urn:veredito:problem:unsupported-api-version",
  "title": "Versão da API não suportada",
  "status": 400,
  "detail": "A versão \"v9\" da API não existe. Versões disponíveis: v1.",
  "instance": "/api/v9/tasks",
  "supportedVersions": ["v1"]
}
```

| Member | Rule |
|---|---|
| `type` | `urn:veredito:problem:<slug>`; never changes with the language |
| `title`, `detail` | From `MessageSource`, keys `problem.<slug>.title` and `problem.<slug>.detail`, in the request locale |
| `status` | HTTP status |
| `instance` | Request path |
| Extensions | Per type, below; names in English camelCase; values never translated |

The JSON schema used by the tests (`matchesJsonSchemaInClasspath`) is
`backend/src/test/resources/schemas/problem-details.json`.

## Locale resolution

| `Accept-Language` | Locale of `title` and `detail` |
|---|---|
| `pt-BR`, `pt`, `pt-PT` | pt-BR |
| `en-US`, `en`, `en-GB` | en-US |
| `es-ES`, `es`, `es-AR` | es-ES |
| Missing, `fr-FR`, `*` or unparsable | pt-BR |
| List with weights (`fr;q=0.9, es;q=0.8`) | First supported language by weight (es-ES) |

The Angular client always sends its build locale. The server's own locale is never used
(`spring.messages.fallback-to-system-locale=false`).

## Catalog in spec 003

Later specs add their own types and keys, in the three bundles at once.

| Slug | Status | When | Extensions |
|---|---|---|---|
| `unsupported-api-version` | 400 | Version segment not supported | `supportedVersions` |
| `authentication-required` | 401 | Protected resource without the required session (Swagger in production) | none |
| `not-found` | 404 | Unknown path under `/api`, unknown relation | none |
| `method-not-allowed` | 405 | Method not supported by the resource | none (`Allow` header) |
| `not-acceptable` | 406 | `Accept` cannot be satisfied | none |
| `invalid-request` | 400 | Malformed body or parameter | `errors`: list of `{ "field", "code" }` |
| `internal-error` | 500 | Any unexpected exception | `incidentId` (UUID), also written to the log with the stack trace |
| `service-unavailable` | 503 | Database unreachable during an operation that needs it | none |

## Message bundles

| File | Locale |
|---|---|
| `backend/src/main/resources/messages.properties` | en-US (source language and base bundle) |
| `backend/src/main/resources/messages_pt_BR.properties` | pt-BR |
| `backend/src/main/resources/messages_es_ES.properties` | es-ES |

UTF-8. The test `MessageBundleParityTest` fails unless the three files have exactly the same keys,
listing each missing key and the file where it is missing (FR-017). The test
`LocalizedProblemDetailsTest` calls `/api/v9` with each of the three `Accept-Language` values and
without the header, and checks `title`, `detail`, `Content-Language` and the unchanged `type`.

## User-facing behavior

The Angular client never shows `title` or `detail` raw for unexpected errors: it picks its own
localized message from the `type` (for `internal-error`, "Something went wrong; try again" plus the
`incidentId` so the researcher can find it in the log).
