# Implementation Plan: Consent form, professional skill and screening

**Branch**: `006-consent-screening` | **Date**: 2026-11-15 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/006-consent-screening/spec.md`

## Summary

Spec 006 puts the ethical and scientific gate in front of the labeling loop that spec 007 already
delivered (production `0.7.0`, tag `deploy/0.7.0-1`). After sign-in a participant is taken to the
first pending step, in this order: **consent form -> professional skill -> screening ->
participation** (`/label`, spec 007).

- **Consent form:** the current version is read from `consent_form` / `consent_form_text` (V1). A
  version is *current* only when it is published (`published_at <= now`) and has the three texts
  (pt-BR reference, en-US, es-ES). The participant reads the text in the interface language (a
  translation note and a link to the pt-BR text of the same version for en-US / es-ES) and accepts or
  refuses; the decision stores version, language of the text shown and date/time on `participant`.
  A new version requires only a new acceptance (skill and screening are not asked again).
- **Skill and screening:** one `professional_skill` code of Appendix B.4 (`other` needs up to 100
  characters), then the screening of Appendix B.3 with an English example use case and test case. The
  answers are stored once, as English codes; `no` is permanent ineligibility.
- **Eligibility:** `AlwaysEligibleGate` (007) is replaced by a database-backed
  `EligibilityGate`: eligible = current consent form accepted **and** `eligible = TRUE` (screening
  answer `yes_professional` or `yes_academic`). The same gate guards the entry point link
  `vd:next-task` and every reserve / save / submit call (FR-007, SC-002).
- **Pending step:** a read-only resource `GET /api/v1/participants/me/onboarding` (`vd:onboarding`)
  names the step; the Angular `/welcome` route becomes a resolver that redirects to `/consent`,
  `/skill`, `/screening`, a closing screen or `/label`. The administrator never sees these screens or
  resources.
- **Data:** no DDL. The V1 columns already exist. `V6` only inserts a **fixture** consent form
  (placeholder text in three languages, *not* approved by the CEP) and lives in a Flyway location
  that only local staging and tests load; the real production text is a separate, researcher-supplied
  migration delivered with a deploy tag.

No new Maven or npm dependency. Decisions are in [research.md](research.md).

## Technical Context

**Language/Version**: Java 25 LTS with Spring Boot 4.1.2; TypeScript with Angular 22.2.6; Node.js
26 LTS for the build

**App version**: **0.6.0** (spec number in the minor digit; see [research.md](research.md) section 1
for the non-monotonic note against the deployed 0.7.0). Deploy tags: `deploy/0.6.0-*`.

**Primary Dependencies**:
- **Back end:** as in 007 (Spring WebMVC, HATEOAS HAL-FORMS, JDBC, Security, Flyway, springdoc,
  Jackson). No new Maven dependency.
- **Front end:** as in 007 (Angular Material radio group / form field / card, `@angular/localize`).

**Storage**: MySQL 9.7; existing `participant` (consent, skill, screening, `eligible` columns),
`consent_form`, `consent_form_text`, `config`. No DDL. Optional fixture data in
`backend/src/main/resources/db/staging/V6__consent_form_fixture.sql` ([data-model.md](data-model.md)).

**Testing**:
- **Domain:** JUnit 5 for the step order and eligibility truth table, skill / "Other" validation and
  current-form selection (plain Java, no Spring).
- **API:** REST Assured from `/api/v1` following links: consent per language, refusal and change of
  mind, new version, incomplete version, skill, screening, pending step, direct call to the Tasks
  operations (`not-eligible`), localized Problem Details; `AuthorizationTest` denies every
  Participation operation of this spec to visitors (401) and to the administrator (403).
- **Persistence:** Testcontainers MySQL: fixture only in the staging location; current-form query.
- **Front end:** Vitest for the step-to-route map, guards, skill / "Other" validation, translation
  note and unsaved-choice restore; Playwright for three e-mails in three languages (accept, refuse
  and change mind, "No" screening, resume at the first pending step).

**Target Platform**: as in 003-007 (Cloud Run + Cloud SQL; Docker Compose `staging`; GitHub-hosted
Ubuntu runners)

**Project Type**: web application (one container: Spring Boot API + three Angular builds)

**Performance Goals**:
- Interactive API calls under 2 s (Principle V). The gate is one indexed query per call.
- New participant through consent, skill and screening in under 3 minutes of interaction, not
  counting the reading of the consent form (SC-001).

**Constraints**:
- Staging fixture never reaches production: the production profile does not load `db/staging`.
- Consent text is data: stored as published, never edited, never translated by the system; the
  en-US / es-ES texts are supplied by the researcher.
- Skill and screening are asked once; changing them is not possible (US3 edge case).
- Codes are English everywhere (storage, API, export); labels live in XLIFF / `MessageSource`.
- The screening example (use case and test case) is English in every locale and lives outside XLIFF.
- Only the participant sees these resources; the administrator receives `403`.
- No schema change, so a Cloud Run rollback to 0.7.0 still starts (the old gate ignores the data).

**Scale/Scope**:
- 10 API operations on 7 resources under `/api/v1` (tag "Participation"): onboarding, consent form
  (current + reference text), consent decision, skill, screening, screening example.
- 5 new participant screens (`/consent`, `/skill`, `/screening`, closing screens, reference text);
  `/welcome` becomes the resolver of the first pending step.
- 3 new Problem Details types, one new extension on `not-eligible`, 7 new link relations.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

Constitution 2.7.1.

| Rule | How the plan meets it | Status |
|---|---|---|
| I. Stack standard and current; versions recorded | No new dependency; Spring Boot 4.1.2 / Angular 22.2.6 unchanged (research section 1) | Pass |
| I. MessageSource for backend messages | New Problem Details types in the three bundles; no user text built in code | Pass |
| I. i18n with XLIFF for the front end | Skill and screening labels, consent screen text, translation note and closing messages as `@@` ids; `i18n:check` in CI | Pass |
| I. REST level 3 | Entry point offers `vd:onboarding` / `vd:consent-form` / `vd:next-task` by state; step resources expose HAL-FORMS templates only when the step is pending ([contracts/onboarding.md](contracts/onboarding.md)) | Pass |
| I. OpenAPI complete; Swagger admin-only in production | Every operation under tag "Participation", caught by `ApiDocumentationCompletenessTest` | Pass |
| II. Domain in plain Java with unit tests | `domain.onboarding` (steps, skill and screening codes, current-form rule); ArchUnit isolation | Pass |
| II. MySQL + REST Assured from `/api/v1` following links | Tests start at the entry point -> `vd:onboarding` -> step links | Pass |
| II. Playwright for the browser flows | Onboarding spec in the three locales | Pass |
| II. i18n completeness | New bundle keys and XLIFF units; parity tests; localized Problem Details (SC-004, SC-005) | Pass |
| III. Transactions; Flyway only | Each decision is one conditional `UPDATE`; consent data only through Flyway (fixture in `db/staging`, real text in a researcher-supplied migration) | Pass |
| IV. Staging/tests/pilot only Minitest | Fixture TCLE is staging/test only and is marked as not approved; production starts with no current form until the researcher delivers the approved text | Pass |
| V. Single artifact; Material; API under 2 s | Material screens; no new service | Pass |
| Language: English in repo; UI three locales; data untranslated | Codes English; consent text and screening example are data; labels translated | Pass |
| Workflow: staging before production; deploy by tag, only with researcher approval | Quickstart in staging, then `deploy/0.6.0-*` after explicit approval and after the approved consent text exists | Pass |
| All text files UTF-8 | The fixture texts contain accents; migration and bundles are UTF-8 | Pass |

No violation.

**Post-design re-check** (after data-model and contracts):
- **Services and secrets:** none added.
- **Coverage:** every operation of the Participation tag introduced here is in `AuthorizationTest`
  (visitor 401, administrator 403); the eligibility truth table (SC-002) is covered by domain tests
  and by API tests that call the Tasks operations directly.
- **Single change point:** `EligibilityGate` stays the only place that decides eligibility.

Pass.

## Risks

| Risk | Mitigation |
|---|---|
| Fixture (fake TCLE) reaches production | Separate Flyway location `db/staging`, listed only in `application-staging.yaml` and in the API test base; `FixtureLocationTest` asserts the default and `prod` profiles do not list it and that `MigrationTest` still sees only `V1`..`V5` |
| Production has no approved consent form at deploy time, so nobody can participate | Documented rollout ([quickstart.md](quickstart.md) section 8): the researcher supplies the migration with the CEP-approved pt-BR text and the en-US / es-ES translations; until then the participant sees "The study is not open yet" (US1 scenario 6) |
| Pilot participants of 0.7.0 (no consent recorded) lose access | Expected and intended (FR-007); they pass the three steps on next sign-in. Announced in the quickstart |
| Eligibility query runs on every entry point GET | Single query over primary keys (participant, current form id); no cache so a new version applies immediately |
| Race between "No" answer and a retry | Conditional `UPDATE ... WHERE professional_skill IS NULL`; an identical retry is idempotent `200`, a different answer is `409 step-not-pending` |
| 007 tests assume everybody is eligible | `ApiTestBase` gets an `eligibleParticipantSession()` helper; 007 tests and Playwright `signInParticipant` move to it ([research.md](research.md) section 13) |
| Version `0.6.0` is lower than the deployed `0.7.0` | Explicit researcher decision recorded in research section 1; CI derives `<pom version>+<short commit>` and nothing compares versions, so the tag deploys; the footer shows `0.6.0+...` |
| Translation texts not CEP-approved | UI note and link to the pt-BR reference (FR-010); open item "confirm with the committee whether translations need approval" stays with the researcher (spec 001 research) |

## Project Structure

### Documentation (this feature)

```text
specs/006-consent-screening/
|-- plan.md
|-- research.md
|-- data-model.md
|-- quickstart.md
|-- contracts/
|   |-- onboarding.md
|   |-- problem-details.md
|   `-- web.md
|-- checklists/requirements.md
`-- tasks.md                 # phase 2 (/speckit-tasks; not created here)
```

### Source Code (root of the `Veredito` branch; new or changed in 006)

```text
backend/
|-- pom.xml                              # version 0.6.0
`-- src/
    |-- main/
    |   |-- java/io/github/diegoquirino/veredito/
    |   |   |-- domain/onboarding/       # OnboardingStep, OnboardingState, OnboardingRules,
    |   |   |                            # ProfessionalSkill, ScreeningAnswer, SkillChoice,
    |   |   |                            # ConsentFormRules (current-form selection, missing languages)
    |   |   |-- onboarding/              # OnboardingService, ConsentFormService, ScreeningExample
    |   |   |                            # (loader), OnboardingEligibilityGate, ConsentFormAudit
    |   |   |                            # (startup log, FR-012)
    |   |   |-- queue/                   # AlwaysEligibleGate removed; EligibilityGate unchanged
    |   |   |-- persistence/             # ConsentFormRepository (new); ParticipantRepository gains
    |   |   |                            # onboarding state and conditional updates
    |   |   |-- api/                     # ProblemType gains 3 slugs; not-eligible gains `step`
    |   |   |-- api/v1/                  # OnboardingController, ConsentFormController,
    |   |   |                            # ConsentDecisionController, SkillController,
    |   |   |                            # ScreeningController, ScreeningExampleController + models;
    |   |   |                            # EntryPointController gains vd:onboarding / vd:consent-form;
    |   |   |                            # LinkRelations gains 7 relations
    |   |   `-- web/SecurityConfig       # /api/v1/consent-forms/** and /screening-example: PARTICIPANT
    |   `-- resources/
    |       |-- db/staging/V6__consent_form_fixture.sql   # staging + tests only
    |       |-- screening/screening-example.json          # English example (researcher-owned)
    |       |-- application-staging.yaml                  # spring.flyway.locations adds db/staging
    |       `-- messages*.properties                      # problem.* for the 3 new types
    `-- test/
        |-- java/.../domain/onboarding/   # OnboardingRulesTest, SkillRulesTest, ConsentFormRulesTest
        |-- java/.../persistence/         # ConsentFormRepositoryTest, FixtureLocationTest
        `-- java/.../api/v1/              # OnboardingFlowTest, ConsentFormTest, ConsentDecisionTest,
                                          # SkillTest, ScreeningTest, EligibilityGateTest;
                                          # AuthorizationTest, EntryPointTest, LocalizedProblemDetailsTest
                                          # and the 007 task tests updated

frontend/src/app/
|-- onboarding/                          # consent, skill, screening, closing and reference components;
|                                        # onboarding.service.ts; step-to-route map
|-- welcome/                             # becomes the resolver of the first pending step
|-- core/guards.ts                       # onboardingStep(...) and participationOnly guards
|-- core/labels/                         # skill-labels.ts, screening-labels.ts (+ specs)
|-- core/problem-messages.ts             # slugs of the new types
|-- label/                               # 403 not-eligible -> /welcome (resolver)
frontend/src/locale/messages.pt-BR.xlf, messages.es-ES.xlf
frontend/e2e/                            # onboarding.spec.ts; support/participant.ts completes onboarding
```

**Structure Decision**: same two-folder web application. `domain.onboarding` stays free of Spring
and holds every rule that can be unit tested (step order, eligibility, skill validation, which form is
current). Package `onboarding` coordinates the repositories and implements `queue.EligibilityGate`.

## Complexity Tracking

No violations.
