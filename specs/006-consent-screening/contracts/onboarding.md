# Contract: Consent form, professional skill, screening and the pending step

**Feature**: `006-consent-screening` | Rules from constitution Principle I and the API contract of
[spec 003](../../003-deployment-foundation/contracts/api.md). Access and participant session as in
[spec 004](../../004-email-code-access/contracts/access.md). Tasks as in
[spec 007](../../007-block-queue/contracts/tasks.md). Paths and relations follow the
[GLOSSARY](../../GLOSSARY.md) (`/consent-forms/current`, `vd:consent-form`).

Media types, `Content-Language`, versioning and Problem Details as in spec 003. Every operation
below is in the OpenAPI group `v1`, tag "Participation", and passes
`ApiDocumentationCompletenessTest`. Every operation is for **participants only**: visitors get `401`,
the administrator `403` (authorization matrix at the end).

Codes (`professional_skill`, screening answers, `step`) are English identifiers in every language;
display labels live in the front end and never travel in the API.

## Entry point `GET /api/v1`, participant links

In addition to the links of 004 and 007, a participant receives:

| Relation | Href | When |
|---|---|---|
| `vd:onboarding` | `/api/v1/participants/me/onboarding` | always (participant session) |
| `vd:consent-form` | `/api/v1/consent-forms/current` | a current consent form exists |
| `vd:next-task` | `/api/v1/tasks/reservations` | `EligibilityGate` true (now database-backed) |

Administrators and visitors never receive any of them. `vd:next-task` disappears for a participant
who stops being eligible (for example after a new consent form version is published).

## `GET /api/v1/participants/me/onboarding` (`vd:onboarding`)

Participant only. Where the participant is in the steps (FR-006).

**200**

```json
{
  "step": "skill",
  "eligible": false,
  "currentConsentFormVersion": "v1",
  "_links": {
    "self": { "href": "/api/v1/participants/me/onboarding" },
    "curies": [ { "name": "vd", "href": "/api/v1/rels/{rel}", "templated": true } ],
    "vd:consent-form": { "href": "/api/v1/consent-forms/current" },
    "vd:consent": { "href": "/api/v1/participants/me/consent" },
    "vd:skill": { "href": "/api/v1/participants/me/skill" },
    "vd:screening": { "href": "/api/v1/participants/me/screening" }
  }
}
```

| Field | Rule |
|---|---|
| `step` | `consent` \| `skill` \| `screening` \| `participation` \| `ineligible` \| `not-open` (first that holds, [research section 2](../research.md)) |
| `eligible` | The `EligibilityGate` answer; true only when `step = participation` |
| `currentConsentFormVersion` | Version string, or `null` when there is no current form |
| Links | `vd:consent-form` only when a current form exists; `vd:consent`, `vd:skill`, `vd:screening` always; `vd:screening-example` only when `step = screening`; `vd:next-task` only when `step = participation` |

`not-open` and `ineligible` have no next link: they are closing states. `ineligible` wins over
`not-open` and `consent` (US3 scenario 4).

**401** `authentication-required` without a session; **403** `forbidden` for the administrator.

## `GET /api/v1/consent-forms/current` (`vd:consent-form`)

Participant only. The current version in the **language of the request** (locale resolved from
`Accept-Language`, as everywhere in the API); `Content-Language` carries it.

**200**

```json
{
  "version": "v1",
  "language": "en-US",
  "referenceLanguage": "pt-BR",
  "translation": true,
  "text": "INFORMED CONSENT FORM\n\nThis is a translation; ...",
  "publishedAt": "2026-11-20T12:00:00Z",
  "_links": {
    "self": { "href": "/api/v1/consent-forms/current" },
    "curies": [ { "name": "vd", "href": "/api/v1/rels/{rel}", "templated": true } ],
    "vd:consent-text": { "href": "/api/v1/consent-forms/v1/texts/pt-BR" },
    "vd:consent": { "href": "/api/v1/participants/me/consent" }
  }
}
```

| Field | Rule |
|---|---|
| `text` | Plain text, paragraphs separated by blank lines; stored as published; never edited by the system |
| `language` | The language of `text` = the request locale (the version is always complete, so it exists) |
| `translation` | `language` is not `referenceLanguage`; the client then shows the translation note and the link `vd:consent-text` (US4 scenarios 2 and 3) |
| `vd:consent-text` | The pt-BR text of **this version**; its `version` is pinned in the path |

| Error | Status | Type | When |
|---|---|---|---|
| No current form | 404 | `not-found` | No form is published with the three texts ("The study is not open yet", US1 scenario 6); the entry point does not offer `vd:consent-form` either |
| No / invalid session | 401 | `authentication-required` | as in 004 |
| Administrator | 403 | `forbidden` | |

## `GET /api/v1/consent-forms/{version}/texts/{language}` (`vd:consent-text`)

Participant only. One text of one **published and complete** version, independent of the request
locale (this is how the pt-BR reference is read from an en-US or es-ES screen without changing the
interface language). `{language}` is `pt-BR`, `en-US` or `es-ES`.

**200**: same body as `consent-forms/current` for that version and language, with
`self` = this address. `Content-Language` is the language of the text.

| Error | Status | Type | When |
|---|---|---|---|
| Unknown or unpublished/incomplete version, or language outside the three | 404 | `not-found` | |
| No / invalid session, administrator | 401 / 403 | `authentication-required` / `forbidden` | |

## `GET /api/v1/participants/me/consent` (`vd:consent`)

Participant only. The last recorded decision.

**200**

```json
{
  "decision": "accepted",
  "consentFormVersion": "v1",
  "language": "en-US",
  "decidedAt": "2026-11-20T17:03:00Z",
  "current": true,
  "_links": {
    "self": { "href": "/api/v1/participants/me/consent" },
    "curies": [ { "name": "vd", "href": "/api/v1/rels/{rel}", "templated": true } ],
    "vd:consent-form": { "href": "/api/v1/consent-forms/current" }
  }
}
```

Nothing recorded yet: `decision`, `consentFormVersion`, `language` and `decidedAt` are `null`
and `current` is `false`. `current` is true when the recorded version is the current one.

HAL-FORMS template `default` (`PUT`, `application/json`) is present **only when `step = consent`**:

```json
"_templates": {
  "default": {
    "title": "Record the decision on the consent form",
    "method": "PUT",
    "contentType": "application/json",
    "properties": [
      { "name": "decision", "type": "text", "required": true,
        "options": { "inline": ["accepted", "refused"], "minItems": 1, "maxItems": 1 } },
      { "name": "consentFormVersion", "type": "text", "required": true, "prompt": "v1" }
    ]
  }
}
```

## `PUT /api/v1/participants/me/consent`

Participant only (FR-001, FR-002, FR-011).

```json
{ "decision": "accepted", "consentFormVersion": "v1" }
```

**200** with the representation above (refreshed) and `Content-Location:
/api/v1/participants/me/onboarding`. Stored: the form of the version, the decision, the **request
locale** as the language of the text shown and the current instant.

Rules:
- Accepting the version already accepted: `200`, nothing changes (idempotent).
- A refusal can be replaced by an acceptance (US1 scenario 4); an acceptance of an older version by
  an acceptance of the current one (US1 scenario 5).
- After a refusal the client shows the closing "thank you"; the participant's next sign-in resolves
  to `consent` again.

| Error | Status | Type | When |
|---|---|---|---|
| Validation | 400 | `invalid-request` | `decision` missing (`required`) or not `accepted`/`refused` (`option`); `consentFormVersion` missing (`required`) |
| No current form | 409 | `study-not-open` | Nothing can be recorded |
| Version is not the current one | 409 | `consent-form-outdated` | A newer version was published while the screen was open; extension `currentVersion` |
| Refusing an accepted current version, or participant already ineligible | 409 | `step-not-pending` | extension `step` |
| No session / administrator | 401 / 403 | `authentication-required` / `forbidden` | |

## `GET /api/v1/participants/me/skill` (`vd:skill`)

**200**

```json
{
  "skill": null,
  "skillOther": null,
  "_links": {
    "self": { "href": "/api/v1/participants/me/skill" },
    "curies": [ { "name": "vd", "href": "/api/v1/rels/{rel}", "templated": true } ]
  },
  "_templates": {
    "default": {
      "title": "Choose the professional skill",
      "method": "PUT",
      "contentType": "application/json",
      "properties": [
        { "name": "skill", "type": "text", "required": true,
          "options": { "inline": ["developer", "software_engineer", "systems_analyst",
            "requirements_analyst", "test_analyst", "test_engineer", "software_architect",
            "tech_lead", "product_project_manager", "professor_researcher", "graduate_student",
            "undergraduate_student", "other"], "minItems": 1, "maxItems": 1 } },
        { "name": "skillOther", "type": "text", "required": false, "maxLength": 100 }
      ]
    }
  }
}
```

The template is present **only when `step = skill`**. Once answered, `skill` and `skillOther` show the
stored values and there is no template (the skill is asked once, FR-005).

## `PUT /api/v1/participants/me/skill`

Participant only (FR-003, FR-005, FR-014).

```json
{ "skill": "other", "skillOther": "Technical writer" }
```

**200** with the refreshed representation and `Content-Location` of the onboarding resource.
`skillOther` is stored **exactly as typed** (no trimming, no translation).

| Error | Status | Type | When |
|---|---|---|---|
| Validation | 400 | `invalid-request` | `skill` missing (`required`) or not one of the 13 codes (`option`); `skill = other` with blank `skillOther` (`required`) or more than 100 characters (`size`); `skillOther` with a skill other than `other` (`unexpected`) |
| Step is not `skill` and the body differs from what is stored | 409 | `step-not-pending` | extension `step` = the real pending step (for example `consent`) |
| No session / administrator | 401 / 403 | `authentication-required` / `forbidden` | |

An identical repeat of the stored answer is `200` (idempotent).

## `GET /api/v1/screening-example` (`vd:screening-example`)

Participant only. The fixed English example shown before the question (Appendix B.3, FR-013).
**`Content-Language: en-US` whatever `Accept-Language` says**: the example is study data, like the
items.

**200**

```json
{
  "version": "1",
  "useCase": {
    "id": "UC-S1",
    "title": "Reset password",
    "basicFlow": [ "The user selects \"Forgot my password\" on the sign-in page.", "..." ],
    "exceptionFlow": {
      "id": "E1",
      "condition": "The e-mail address is not registered (step 3).",
      "steps": [ "The system shows the same confirmation as in the basic flow, without sending any message.", "The use case ends." ]
    }
  },
  "testCase": {
    "id": "TC-S1",
    "title": "Reset request for an unregistered e-mail address",
    "derivedFrom": [ "UC-S1 / E1" ],
    "steps": [ "Open the sign-in page and select \"Forgot my password\".", "..." ],
    "expected": "The confirmation is shown and no message is sent."
  },
  "_links": {
    "self": { "href": "/api/v1/screening-example" },
    "curies": [ { "name": "vd", "href": "/api/v1/rels/{rel}", "templated": true } ],
    "vd:screening": { "href": "/api/v1/participants/me/screening" }
  }
}
```

The question, the three option labels and the instructions around the example are interface text
(Appendix B.3) and come from the front end in the interface language.

## `GET /api/v1/participants/me/screening` (`vd:screening`)

**200**

```json
{
  "answer": null,
  "eligible": null,
  "_links": {
    "self": { "href": "/api/v1/participants/me/screening" },
    "curies": [ { "name": "vd", "href": "/api/v1/rels/{rel}", "templated": true } ],
    "vd:screening-example": { "href": "/api/v1/screening-example" }
  },
  "_templates": {
    "default": {
      "title": "Answer the screening question",
      "method": "PUT",
      "contentType": "application/json",
      "properties": [
        { "name": "answer", "type": "text", "required": true,
          "options": { "inline": ["yes_professional", "yes_academic", "no"], "minItems": 1, "maxItems": 1 } }
      ]
    }
  }
}
```

The template is present **only when `step = screening`**. Answered: `answer` and `eligible` show the
stored values (`eligible` is `true` for the two `yes_*`, `false` for `no`) and there is no template.

## `PUT /api/v1/participants/me/screening`

Participant only (FR-004, FR-005).

```json
{ "answer": "yes_academic" }
```

**200** with the refreshed representation and `Content-Location` of the onboarding resource.
`yes_professional` and `yes_academic` make the participant eligible (the next onboarding read says
`participation`); `no` makes them permanently ineligible for that e-mail (`ineligible`).

| Error | Status | Type | When |
|---|---|---|---|
| Validation | 400 | `invalid-request` | `answer` missing (`required`) or not one of the three codes (`option`) |
| Step is not `screening` and the body differs from what is stored | 409 | `step-not-pending` | extension `step` (US3 edge case: back button after the screening) |
| No session / administrator | 401 / 403 | `authentication-required` / `forbidden` | |

## Effect on the Tasks operations (spec 007)

`EligibilityGate` is the single check of FR-007. The operations that already call it in 007
(`POST /api/v1/tasks/reservations`, `PUT .../responses/{itemId}` and `POST .../submission`) answer
`403 not-eligible` for a participant who is not eligible, now with the extension `step`
([problem-details.md](problem-details.md)). The read operations of a task keep the 007 behavior
(owner only, `404` otherwise); a participant who is no longer eligible has nothing new to read
because `vd:next-task` is gone. Nothing else of the 007 contract changes.

## Relations added to `/api/v1/rels/{rel}`

| Relation | Method | Description |
|---|---|---|
| `vd:onboarding` | `GET` | The step the participant is at: consent form, skill, screening or participation |
| `vd:consent-form` | `GET` | The current consent form in the language of the request |
| `vd:consent-text` | `GET` | One language of one version of the consent form (used for the pt-BR reference) |
| `vd:consent` | `GET`, `PUT` | The participant's decision on the consent form; `PUT` records it |
| `vd:skill` | `GET`, `PUT` | The participant's professional skill; `PUT` records it once |
| `vd:screening` | `GET`, `PUT` | The participant's screening answer; `PUT` records it once |
| `vd:screening-example` | `GET` | The fixed English use case and test case shown before the screening question |

Each is registered in `LinkRelations` with a description and the `/swagger-ui.html#/Participation`
anchor, as the 004 and 007 relations are.

## Authorization matrix (Participation tag, this spec)

| Resource | Visitor | Participant | Administrator |
|---|---|---|---|
| `GET /participants/me/onboarding` | 401 | yes | 403 |
| `GET /consent-forms/current`, `GET /consent-forms/{version}/texts/{language}` | 401 | yes | 403 |
| `GET`, `PUT /participants/me/consent`, `/skill`, `/screening` | 401 | yes | 403 |
| `GET /screening-example` | 401 | yes | 403 |

`SecurityConfig` adds `/api/v1/consent-forms/**` and `/api/v1/screening-example` as `PARTICIPANT`
(`/api/v1/participants/**` already is). `AuthorizationTest` enumerates every "Participation"
operation of this table from the OpenAPI document.
