# Contract: Problem Details and messages added by spec 006

**Feature**: `006-consent-screening` | Format, locale resolution and bundles as in
[spec 003](../../003-deployment-foundation/contracts/problem-details.md); types of 007 as in
[its contract](../../007-block-queue/contracts/problem-details.md).

## New types

| Slug | Status | When | Extensions |
|---|---|---|---|
| `study-not-open` | 409 | `PUT /participants/me/consent` while there is no current consent form (none published, or none with the three texts) | none |
| `consent-form-outdated` | 409 | `PUT /participants/me/consent` with a `consentFormVersion` that is not the current one (a newer version was published while the screen was open) | `currentVersion` (string) |
| `step-not-pending` | 409 | `PUT` of consent (refusing an already accepted current version, or any decision by an ineligible participant), skill or screening when that step is not the pending one and the body differs from what is stored | `step` (the real pending step) |

Slug, URI (`urn:veredito:problem:<slug>`) and status never change with the language; `title` and
`detail` come from `MessageSource` in the language of the request. Keys:
`problem.<slug>.title` and `problem.<slug>.detail`, in `messages.properties`,
`messages_pt_BR.properties` and `messages_es_ES.properties` (bundle parity test).

| Slug | en-US title | en-US detail |
|---|---|---|
| `study-not-open` | The study is not open yet | The study has no consent form in force yet. Please come back later. |
| `consent-form-outdated` | The consent form changed | A newer version of the consent form was published. Read it and decide again. |
| `step-not-pending` | This step is not available | This step was already completed or is not the next one. Continue from where you are. |

| Slug | pt-BR title | es-ES title |
|---|---|---|
| `study-not-open` | O estudo ainda não está aberto | El estudio aún no está abierto |
| `consent-form-outdated` | O termo mudou | El consentimiento cambió |
| `step-not-pending` | Esta etapa não está disponível | Este paso no está disponible |

(The `detail` texts of the two other languages follow the same sentences; the final wording is
reviewed with the rest of the bundle.)

## Changed type (spec 007)

| Slug | Status | Change |
|---|---|---|
| `not-eligible` | 403 | Gains the extension **`step`**: the pending step of the participant (`consent`, `skill`, `screening`, `ineligible` or `not-open`), so the client sends them to the right screen without another request. The existing `title` / `detail` already mention the consent form and the screening and do not change. Before 006 the gate was always true and the type was unused |

## Reused types

`authentication-required` (401), `forbidden` (403, administrator on a participant resource),
`not-found` (404, no current consent form, unknown version or language) and `invalid-request` (400).

### Field codes added to `invalid-request` `errors`

| Field | Code | When |
|---|---|---|
| `decision` | `required` / `option` | missing / not `accepted` or `refused` |
| `consentFormVersion` | `required` | missing |
| `skill` | `required` / `option` | missing / not one of the 13 codes of Appendix B.4 |
| `skillOther` | `required` / `size` / `unexpected` | `skill = other` with blank text / more than 100 characters / given with another skill |
| `answer` | `required` / `option` | missing / not `yes_professional`, `yes_academic` or `no` |

`required`, `size` and `locale` already exist in 004; `option` and `unexpected` are new codes. The
client maps each `field` + `code` to a localized message (`fieldMessage`, as in 004 and 005).

Examples:

```json
{
  "type": "urn:veredito:problem:consent-form-outdated",
  "title": "The consent form changed",
  "status": 409,
  "detail": "A newer version of the consent form was published. Read it and decide again.",
  "instance": "/api/v1/participants/me/consent",
  "currentVersion": "v2"
}
```

```json
{
  "type": "urn:veredito:problem:not-eligible",
  "title": "Not eligible to label",
  "status": 403,
  "detail": "You are not eligible to request or label blocks with the current consent and screening.",
  "instance": "/api/v1/tasks/reservations",
  "step": "consent"
}
```

## Front-end messages

`core/problem-messages.ts` gains the three slugs; `consent-form-outdated` reloads the consent form,
`step-not-pending` and `not-eligible` navigate to `/welcome` (the resolver) instead of only showing a
message, `study-not-open` navigates to `/not-open`. Labels of the screens (consent, skill,
screening, closing messages, translation note) are XLIFF units with `@@` ids, listed in
[web.md](web.md).

## Tests

- `LocalizedProblemDetailsTest` gains one case per new type in each of the three locales, and the
  `not-eligible` case now uses a **real** not-onboarded participant (no test double) and checks
  `step`.
- `MessageBundleParityTest` (existing) covers the new keys in the three bundles.
- Field codes `option` and `unexpected` are checked in `SkillTest`, `ScreeningTest` and
  `ConsentDecisionTest`; each type's extension (`currentVersion`, `step`) is asserted in the API
  tests that provoke it.
