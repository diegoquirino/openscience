# Data model: Consent form, professional skill and screening

**Feature**: `006-consent-screening` | **Date**: 2026-11-15 | **Research**: [research.md](research.md)

Spec 003 already created every table and column this feature needs in `V1`: `consent_form`,
`consent_form_text` and, on `participant`, `consent_form_id`, `consent_decision`,
`consent_language`, `consent_decided_at`, `professional_skill`, `professional_skill_other`,
`screening_answer`, `eligible` and `preferred_language`. Spec 006 **adds no column, table or
constraint**. It adds data (a staging-only fixture) and reads / writes the existing columns.

## Migration `V6__consent_form_fixture.sql` (staging and tests only)

Location: `backend/src/main/resources/db/staging/V6__consent_form_fixture.sql`.

| Where it runs | How |
|---|---|
| Local staging (`docker compose --profile staging`) | `application-staging.yaml`: `spring.flyway.locations: classpath:db/migration,classpath:db/staging` |
| API tests | `ApiTestBase` adds the same two locations through `@TestPropertySource` |
| `MigrationTest`, development profile, production (`prod`) | **Not loaded**: the default `classpath:db/migration` has `V1`..`V5` only |

Content (data only, no DDL; rollback to 0.7.0 is unaffected):

```sql
-- FIXTURE for local staging and automated tests. NOT the research consent form and NOT approved by
-- the research ethics committee (CEP). Never copy this file into db/migration.
INSERT INTO consent_form (version, reference_language, created_at, published_at)
VALUES ('fixture-1', 'pt-BR', '2000-01-01 00:00:00.000000', '2000-01-01 00:00:00.000000');

INSERT INTO consent_form_text (consent_form_id, language, text)
SELECT id, 'pt-BR', '...' FROM consent_form WHERE version = 'fixture-1';
-- the same INSERT ... SELECT for 'en-US' and 'es-ES'
```

| Fixture detail | Rule |
|---|---|
| `version` | `fixture-1` (at most 20 characters); never `v1`, which is reserved for the real form |
| `published_at` | `2000-01-01`, so any real or test version published later becomes current (research section 3) |
| Text | Three paragraphs per language: title with the words "FIXTURE - NOT APPROVED BY THE CEP", a short description of the study, the LGPD sentence about data on Google Cloud. The en-US and es-ES texts start with the translation sentence ("This is a translation; the reference version approved by the research ethics committee is the Portuguese (Brazil) one."). Plain text, paragraphs separated by a blank line |
| Encoding | UTF-8 (accents in pt-BR and es-ES) |

Tests that need other situations change the data inside the test (and the application clock):
unpublish the fixture (`UPDATE consent_form SET published_at = NULL`), insert `fixture-2` with
two texts (incomplete), insert `fixture-3` with three texts and a later `published_at`, or a form
with a future `published_at`.

### Production consent form (separate, researcher-supplied)

A normal versioned migration in `db/migration` with the next free number, for example
`V7__consent_form_v1.sql`, written by the researcher with the CEP-approved pt-BR text and the
translations: one `consent_form` row (`version = 'v1'`, `reference_language = 'pt-BR'`,
`published_at` = the publication instant) and three `consent_form_text` rows, **in one file** so the
form can not exist half-translated. Not part of this spec's code; the plan only requires that the
application picks it up with no change. A corrected or new text is a new version (`v2`), never an
`UPDATE`.

## Which form is current

```sql
-- inputs: :now (application clock, UTC)
SELECT cf.id, cf.version
FROM consent_form cf
WHERE cf.published_at IS NOT NULL
  AND cf.published_at <= :now
  AND (SELECT COUNT(*) FROM consent_form_text t WHERE t.consent_form_id = cf.id) = 3
ORDER BY cf.published_at DESC, cf.id DESC
LIMIT 1;
```

`uq_consent_form_text_language` and `ck_consent_form_text_language` guarantee that three texts mean
exactly pt-BR, en-US and es-ES. The tables are tiny (one row per version), so no new index is
needed. The same rule is written in plain Java in `ConsentFormRules` and tested without a database;
the SQL is covered by `ConsentFormRepositoryTest`.

Published-but-incomplete versions (a `published_at` with fewer than three texts) are never current;
`ConsentFormAudit` logs their missing languages at startup (FR-012).

## How spec 006 uses the tables

### `participant` (read / update)

| Column | Written by | Rule |
|---|---|---|
| `consent_form_id`, `consent_decision`, `consent_language`, `consent_decided_at` | `PUT .../consent` | Always the four together (`ck_participant_consent_complete`); only the **last** decision is kept; `consent_language` is the request locale; `consent_decided_at` is the application clock |
| `professional_skill` | `PUT .../skill` | One of the 13 codes of Appendix B.4; written once (`WHERE professional_skill IS NULL`) |
| `professional_skill_other` | `PUT .../skill` | Only with `other`; at most 100 characters (code points); stored as typed (`ck_participant_professional_skill_other`) |
| `screening_answer` | `PUT .../screening` | `yes_professional`, `yes_academic` or `no`; written once (`WHERE screening_answer IS NULL`) |
| `eligible` | `PUT .../screening` | `TRUE` for `yes_*`, `FALSE` for `no`; `NULL` until the screening is answered |
| `preferred_language` | spec 004 (unchanged) | Not touched; exported next to the consent language (009) |
| `anonymized_at` | spec 011 | A participant anonymized by 011 keeps these columns for the exports; not a 006 concern |

`upsertOnSignIn` (004) never writes these columns, so signing in again changes nothing.

Transitions of the **consent** columns:

| From | Request | To |
|---|---|---|
| all `NULL` | accept current version | accepted, current version, language, now |
| all `NULL` | refuse current version | refused, same fields |
| refused (current or older version) | accept current version | accepted (changed mind, US1 scenario 4) |
| refused (current version) | refuse current version | `200` unchanged (idempotent) |
| accepted (older version) | accept or refuse current version | overwritten with the new version (US1 scenario 5) |
| accepted (current version) | accept current version | `200` unchanged (idempotent) |
| accepted (current version) | refuse current version | `409 step-not-pending` (revocation is spec 011) |

### `consent_form`, `consent_form_text` (read only)

The application never inserts, updates or deletes them (texts are immutable once published). The
current form and its texts are read per request.

### `config` (not used)

No new setting: the current form is data, not configuration (research section 3).

## Derived values (not stored)

| Value | Definition |
|---|---|
| `step` | `OnboardingRules.stepOf` (research section 2): `ineligible` -> `not-open` -> `consent` -> `skill` -> `screening` -> `participation` |
| `eligible` (gate) | current form exists AND `consent_decision = 'accepted'` AND `consent_form_id` = current id AND `professional_skill IS NOT NULL` AND `eligible = TRUE` (and `screening_answer IN ('yes_professional', 'yes_academic')`) |
| `translation` | the text language differs from `consent_form.reference_language` |
| `current` (consent resource) | the recorded `consent_form_id` is the current form id |

The gate as one statement (inputs `:participantId`, `:currentFormId`, the latter from the query above):

```sql
SELECT EXISTS (
  SELECT 1 FROM participant p
  WHERE p.id = :participantId
    AND p.consent_decision = 'accepted'
    AND p.consent_form_id = :currentFormId
    AND p.professional_skill IS NOT NULL
    AND p.screening_answer IN ('yes_professional', 'yes_academic')
    AND p.eligible = TRUE)
```

## State machine (participant, onboarding)

```text
 sign-in --> consent --accept--> skill --choose--> screening --yes_*--> participation (/label)
               |  ^                                    |
        refuse |  | change mind (next sign-in)         +--no--> ineligible (final, permanent)
               v  |
        thanks, ends (the next step is still `consent`)

 A new consent form version sends a participant in `participation` (or `skill` / `screening`)
 back to `consent` only; the skill and the screening are kept.
 No current consent form: `not-open` (nothing can be recorded); `ineligible` still wins.
```

## Validation rules (domain / service, not new CHECKs)

| Rule | Source |
|---|---|
| Consent body: `decision` in {`accepted`, `refused`}; `consentFormVersion` required and equal to the current version | FR-001, FR-002 |
| Consent language = request locale in {pt-BR, en-US, es-ES}; the text in that language exists (the form is complete) | FR-009, FR-011, FR-012 |
| `skill` in the 13 codes; `skillOther` required (not blank, at most 100 code points) iff `skill = other`; stored as typed | FR-003, FR-014 |
| `answer` in {`yes_professional`, `yes_academic`, `no`} | FR-004, FR-013 |
| Skill only at step `skill`; screening only at step `screening`; identical repeats are `200`, anything else `409` | FR-005, FR-006 |
| Administrator never reads or writes these resources | spec edge case |

## Screening example (file, not a table)

`backend/src/main/resources/screening/screening-example.json`, English, owned by the researcher
(Assumption). Loaded and validated at startup into a record; served by `GET /api/v1/screening-example`.
The first version below is a proposal (a generic domain, not one of the study systems, so no study
item is revealed) for the researcher to replace:

```json
{
  "version": "1",
  "useCase": {
    "id": "UC-S1",
    "title": "Reset password",
    "basicFlow": [
      "The user selects \"Forgot my password\" on the sign-in page.",
      "The system asks for the e-mail address of the account.",
      "The user types the e-mail address and confirms.",
      "The system sends a message with a reset link valid for 30 minutes.",
      "The user opens the link and types a new password twice.",
      "The system stores the new password and shows a confirmation."
    ],
    "exceptionFlow": {
      "id": "E1",
      "condition": "The e-mail address is not registered (step 3).",
      "steps": [
        "The system shows the same confirmation as in the basic flow, without sending any message.",
        "The use case ends."
      ]
    }
  },
  "testCase": {
    "id": "TC-S1",
    "title": "Reset request for an unregistered e-mail address",
    "derivedFrom": ["UC-S1 / E1"],
    "steps": [
      "Open the sign-in page and select \"Forgot my password\".",
      "Type an e-mail address that has no account and confirm."
    ],
    "expected": "The confirmation is shown and no message is sent."
  }
}
```

| Validation at startup | Rule |
|---|---|
| Required | `useCase.title`, non-empty `basicFlow`, `exceptionFlow.condition` and non-empty `exceptionFlow.steps`, `testCase.title`, non-empty `testCase.steps`, `testCase.expected` |
| Text | English; strings are shown as they are (never passed through `MessageSource` or XLIFF) |

## Entities (spec vocabulary)

- **Participant (additions):** consent decision (version, language, date and time), skill and "Other"
  description, screening answer, eligibility, preferred language.
- **Consent form:** a version with one text per language (pt-BR as the reference, en-US and es-ES as
  translations); current only when published and complete.
