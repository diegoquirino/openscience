# Research: Consent form, professional skill and screening

**Feature**: `006-consent-screening` | **Date**: 2026-11-15 | **Spec**: [spec.md](spec.md)

This research starts from the technology selection of spec 001
([research.md](../001-oracle-labeling/research.md)), the schema of spec 003 (`consent_form`,
`consent_form_text` and the consent / skill / screening columns of `participant` already exist in
`V1`), the access layer of 004 and the single eligibility gate that 007 left as a stub. It only
records what spec 006 decides on top of them.

## 1. Versions (checked on 2026-11-15)

| Component | Version | Note |
|---|---|---|
| Spring Boot | 4.1.2 | Unchanged |
| Angular / Material / localize | 22.2.6 | Unchanged |
| Playwright | 1.65.0 | Unchanged |
| MySQL / Testcontainers / Flyway | as in 007 | Unchanged |
| App version | 0.6.0 | Spec number in the minor digit (004 -> 0.4.x, 005 -> 0.5.0, 007 -> 0.7.0); deploy tags `deploy/0.6.0-*` |

No new Maven or npm dependency.

- **Where the version lives**: `backend/pom.xml`, `frontend/package.json` and `package-lock.json`,
  `Dockerfile`, `compose.yaml` (`VEREDITO_VERSION` default), `frontend/scripts/write-build-info.mjs`,
  `SystemVersion` default and the example in `EntryPointController`. CI builds
  `<pom version>+<short commit>`.
- **Non-monotonic note**: production already runs `0.7.0+9a00796` (tag `deploy/0.7.0-1`), because
  007 was implemented before 006 (ROADMAP, "biggest risk first"). The researcher decided that 006
  keeps its number, so the artifact built from this branch (which contains 007) reports `0.6.0`.
  Nothing in `ci.yml` or `deploy.yml` compares versions, so the tag deploys normally. The footer will
  show a lower number than before; this is recorded here so nobody "fixes" it by accident.
- **Rationale**: the minor digit identifies the spec, which is how the project reads releases.
- **Alternatives considered**: `0.8.0` (monotonic, but breaks the spec-number convention);
  `0.7.1` (reads as a patch of 007, which this is not).

## 2. Eligibility rule and the single gate

- **Decision**: replace `AlwaysEligibleGate` with `OnboardingEligibilityGate implements
  EligibilityGate` (package `onboarding`). `isEligible(participantId)` is true when **all** hold:
  1. a current consent form exists (section 3);
  2. the participant's last consent decision is `accepted` **for that form** (`consent_form_id` =
     current id);
  3. `professional_skill` is set (the steps are ordered, so a participant who skipped the skill can
     not be eligible);
  4. `screening_answer` is `yes_professional` or `yes_academic` and `eligible = TRUE`.

  One SQL statement answers it (current-form subquery + participant row). No cache: a version
  published in the database applies on the next request without restarting. The gate keeps being
  called first by `TaskReservationService`, `DraftService`, `SubmissionService` and by the entry point
  (`vd:next-task`), so FR-007 and SC-002 hold for direct calls. Refusals stay `403 not-eligible`
  (007), now with the extension `step` ([contracts/problem-details.md](contracts/problem-details.md)).
- **Rationale**: Assumption of 007 and the ROADMAP: 006 "completes the eligibility 007 left open"
  by replacing one bean. Condition 2 uses the current form id, so publishing a new version makes
  every participant "not eligible until they accept it" with no data migration (US1 scenario 5).
- **Alternatives considered**: storing a computed `eligible` and updating it on publication (needs a
  job and drifts); checking eligibility only in the controllers (scattered, misses direct calls).

### Pure rule: which step is pending

`domain.onboarding.OnboardingRules.stepOf(state, currentFormId)` is plain Java. The order is:

| # | Condition (first that holds wins) | `step` |
|---|---|---|
| 1 | `screening_answer = 'no'` (permanent for the e-mail) | `ineligible` |
| 2 | no current consent form | `not-open` |
| 3 | last decision is not `accepted` for the current form (never decided, refused, or older version) | `consent` |
| 4 | `professional_skill` is null | `skill` |
| 5 | `screening_answer` is null | `screening` |
| 6 | otherwise | `participation` |

`ineligible` is checked before `not-open` and `consent` on purpose: US3 scenario 4 says an
ineligible participant always sees the same closing message, even after a new consent form version.
A participant who refused sees `consent` again on the next sign-in (US1 scenario 4); the "thank you"
screen right after a refusal is shown by the client from the answer of the `PUT`, it is not a step.

Because skill and screening are ordered after consent but never reset, a returning participant with
a new consent version gets `consent` and then goes straight to `participation` (US1 scenario 5).

## 3. Where consent forms come from (FR-008, FR-009, FR-012)

- **Decision**: consent forms are **data delivered by Flyway**. There is no administration screen
  (Assumption) and no new table.
  - A version is current when `published_at IS NOT NULL AND published_at <= now` **and** it has
    exactly the three texts. Among the candidates the current one has the greatest `published_at`
    (ties broken by `id`). `now` is the application clock, so tests move it.
  - `domain.onboarding.ConsentFormRules` chooses the current form from a list of snapshots (id,
    version, published_at, set of languages) and reports the missing languages of a published but
    incomplete version. No JDBC in it.
  - `onboarding.ConsentFormAudit` runs once after startup (`ApplicationReadyEvent`) and logs:
    `INFO` the current version; `WARN` per published-but-incomplete version with the missing
    languages ("Consent form v2 is published but lacks texts in: es-ES; it is not current");
    `WARN` when there is no current form ("The study is not open yet"). That is FR-012 and US4
    scenario 6. The gate and the API evaluate the rule on every request, so a form fixed later
    becomes current without a restart; the log is only the operator's early warning.
  - **Fixture for local staging and tests**: `V6__consent_form_fixture.sql` inserts version
    `fixture-1` with `published_at = '2000-01-01'` and the three placeholder texts (clearly marked
    as a fixture, not approved by the CEP). The old date guarantees that any real version supersedes
    it. It is not in `db/migration`: it sits in `classpath:db/staging`, a Flyway location listed
    only by `application-staging.yaml` and by the API test base ([data-model.md](data-model.md)).
  - **Production**: the CEP-approved pt-BR text and the translations are supplied by the researcher
    as a normal versioned migration (next free number, for example
    `V7__consent_form_v1.sql`, version `v1`), reviewed, merged and deployed with an approved
    `deploy/0.6.0-*` tag. Texts are stored as published and never edited; a corrected text is a new
    version (research of spec 001).
- **Rationale**: Principle III ("Flyway only"); an immutable, auditable, versioned record of which
  text was approved; no code path that can insert a half-translated form by accident. Spec 003
  said "No consent form is published by V2": `MigrationTest` keeps asserting it for `db/migration`.
- **Alternatives considered**:
  - `V6` in `db/migration` guarded by a Flyway placeholder (`WHERE '${environment}' <> 'production'`):
    one location, but a missing or wrong profile would publish a fake TCLE in production.
  - Texts as files read at startup (`consent-forms/v1/pt-BR.txt`) and inserted by a runner: needs
    new loading and hashing code and a second source of truth; Flyway already does it.
  - A row in `config` pointing at the current version: unnecessary, the latest complete
    published version is enough and cannot point at an incomplete one.
  - An administrator screen to edit the form: out of scope (Assumption), and editing contradicts
    "stored as published".

## 4. Consent text, language and the translation note (US4)

- **Text format**: plain text, paragraphs separated by blank lines, no HTML and no Markdown. The
  client renders it with `white-space: pre-line` inside a scrollable region, so there is no
  sanitizer, no XSS surface and no new dependency. Headings are written in capital letters or
  numbered lines in the text itself.
- **Language of the text**: the interface locale resolved for the request (`Accept-Language`,
  already sent by the Angular interceptor, same mechanism as `language` on 007 drafts). The
  representation carries `language` (the text returned) and `Content-Language`. The API never takes
  the language from the body.
- **Translation note**: shown by the **client** whenever `translation` is true (`language` differs
  from `referenceLanguage`), so it can never be forgotten by a text. FR-010 also asks the translated
  *texts* to say so; the fixture and the supplied translations include that sentence, and the
  screen adds the note and the link above the text.
- **Reference link**: `vd:consent-text` on the representation points to
  `GET /api/v1/consent-forms/{version}/texts/pt-BR`, which pins the version (a new version published
  while the participant reads cannot change what the link shows). The Angular route
  `/consent/reference/:version` opens it in a new tab **under the current locale prefix**, so the
  interface language does not change (US4 scenario 3).
- **Language switch** on the consent screen reloads under the other prefix, reads the same version
  in the new language and records nothing (US4 scenario 5): the decision is a `PUT`, only triggered
  by the two buttons.
- **Rationale**: simplest thing that satisfies US4 and keeps the `translation` flag derived, not
  stored.
- **Alternatives considered**: Markdown rendering (needs a library and sanitizing); a single
  `GET /consent-forms/current?language=pt-BR` for the reference text (the version could change
  between the two reads); storing a `translation` boolean in `consent_form_text` (redundant with
  `consent_form.reference_language`).

## 5. Recording the decision (FR-001, FR-002, FR-011)

- **Decision**: `PUT /api/v1/participants/me/consent` with `{ "decision": "accepted|refused",
  "consentFormVersion": "v1" }`. In one transaction: resolve the current form; if there is none,
  `409 study-not-open`; if the body version is not the current one, `409 consent-form-outdated`
  (extension `currentVersion`); then one `UPDATE participant SET consent_form_id, consent_decision,
  consent_language, consent_decided_at WHERE id = ?`. `consent_language` is the resolved request
  locale, `consent_decided_at` the application clock. The four columns are always written together
  (`ck_participant_consent_complete`).
- **Rules**:
  - Only the **last** decision is kept (the schema has one set of columns): a refusal can be
    changed to an acceptance (US1 scenario 4); accepting a new version overwrites the previous
    acceptance. History of earlier versions is not kept; the long export of 009 and the withdrawal of
    011 work with the last decision (spec 001 FR-074 only asks for the accepted version).
  - **Accepting is idempotent**: accepting the version already accepted returns `200` unchanged
    (same timestamp and language); a double click or a retry creates no second record.
  - **An acceptance is final for that version**: refusing a version already accepted is
    `409 step-not-pending`. Revoking consent is the LGPD withdrawal of spec 011, not a flip here.
  - **Ineligible participants** (screening `no`) can not record anything: `409 step-not-pending`
    with `step = ineligible`.
  - Acceptance is per version, not per language: switching the interface language later requires
    nothing (edge case).
- **Rationale**: matches the existing columns and keeps the decision a single atomic write.
- **Alternatives considered**: a `consent_decision` history table (extra DDL for a requirement the
  spec does not state); body field `language` (spoofable, can disagree with the text actually shown).

## 6. Professional skill (FR-003, FR-005, FR-014)

- **Decision**: `PUT /api/v1/participants/me/skill` with `{ "skill": "test_analyst" }` or
  `{ "skill": "other", "skillOther": "Technical writer" }`.
  - `domain.onboarding.ProfessionalSkill` is an enum with exactly the 13 codes of Appendix B.4 (the
    same list as `ck_participant_professional_skill`). Unknown code: `400 invalid-request`,
    `errors[].code = option`.
  - `other` requires a non-blank `skillOther` of **at most 100 characters counted as code points**
    (MySQL `VARCHAR(100)` counts characters, not UTF-16 units); blank or whitespace-only is
    `required`, longer is `size`. The text is stored **exactly as typed**, no trimming, no case
    change, no translation (FR-014).
  - `skillOther` on any code other than `other` is `400` with `code = unexpected` (the CHECK would
    refuse it anyway; failing early gives a clear message).
  - Allowed only when the pending step is `skill`. Written with
    `UPDATE ... SET professional_skill = ?, professional_skill_other = ? WHERE id = ? AND
    professional_skill IS NULL`. If zero rows changed, the stored value is read: **identical answer
    -> `200` (idempotent)**, different answer or wrong step -> `409 step-not-pending` (FR-005, "asked
    only once").
- **Rationale**: the two-step "update where null, then compare" is race free without a lock and
  mirrors the idempotent resubmission rule of 007 (FR-007).
- **Alternatives considered**: letting the participant change the skill later (contradicts
  FR-005); trimming the description (violates "as typed").

## 7. Screening (FR-004, FR-013) and its fixed example

- **Decision**: `PUT /api/v1/participants/me/screening` with `{ "answer": "yes_professional" |
  "yes_academic" | "no" }`. `domain.onboarding.ScreeningAnswer` maps the code to eligibility
  (`yes_*` -> eligible). One conditional write sets `screening_answer` and `eligible` where
  `screening_answer IS NULL`; same idempotency rule as the skill. Allowed only when the pending step
  is `screening`; the "Back" button after answering (edge case) just meets the resolver, which sends
  the participant to the closing screen or to `/label`.
- **Example content**: `GET /api/v1/screening-example` serves a fixed English document (use case
  with one basic flow and one exception flow; one test case derived from it) loaded at startup from
  `backend/src/main/resources/screening/screening-example.json` into a Java record and validated
  (required parts present, at least one step each). Startup fails with a clear message if the file is
  malformed, so a broken example can never reach a participant. The response always has
  `Content-Language: en-US` regardless of `Accept-Language`, an explicit, documented exception to
  the localized-representation rule (FR-013: the example is study data, like the items).
- **Why not XLIFF / MessageSource**: those hold translatable interface text; the example must stay
  English in every locale, and keeping it out of them also keeps it out of the translators'
  workflow. Why not the database: it never changes at run time, and a file in the repository is
  reviewed like code. The researcher owns the file; the first version is the proposal in
  [data-model.md](data-model.md).
- **Question, options and instructions** are interface text: question and option labels from
  Appendix B.3 live in XLIFF (`@@screening.question`, `@@screening.answer.yes_professional`, ...);
  the API only carries the codes (HAL-FORMS `options.inline`).
- **Alternatives considered**: hard-coding the example in the Angular component (not testable from
  the API, and `i18n` tooling would try to extract it); a `screening_example` table (DDL for fixed
  content).

## 8. Pending step, resolver and routing (FR-006)

- **Decision**:
  - `GET /api/v1/participants/me/onboarding` (`vd:onboarding`) returns `step` (section 2), `eligible`
    and `currentConsentFormVersion`, with links to the resources of the step. The entry point offers
    `vd:onboarding` to every participant, `vd:consent-form` while a current form exists and
    `vd:next-task` only when eligible.
  - `/welcome` (the post-sign-in area of `areaOf('participant')`, unchanged) becomes a **resolver**:
    it follows `vd:onboarding` and navigates with `replaceUrl` to `/consent`, `/skill`, `/screening`,
    `/screening/closed`, `/not-open` or `/label`. Eligible participants therefore go **straight to
    participation** (US3 scenario 5), not through a landing page.
  - The route guards `onboardingStep('consent' | 'skill' | 'screening')` and `participationOnly`
    (for `/label/**`) read the same resource and redirect to the route of the real step. This makes
    the browser Back button and typed URLs harmless (edge cases) without duplicating the rules in the
    client; the backend remains the authority (`403 not-eligible`).
  - A `not-eligible` answer on a Tasks call carries `step`; the label screens send the participant to
    `/welcome`, which resolves it. This covers a participant with a reserved block who must accept a
    new consent version: they accept it and the normal reservation rules of 007 apply (the block may
    have expired).
  - The administrator never reaches any of it: `participantOnly` sends the administrator to
    `/admin`, and the API answers `403 forbidden` on every participant resource.
- **Rationale**: one query, one source of truth for "where am I", level 3 navigation by link.
- **Alternatives considered**: returning the pending step in the session body (004's `vd:session` is
  read once per page load and would go stale after each step); redirecting on the server (the SPA
  owns routing under the locale prefixes).

## 9. API shape

| Operation | Request | Success |
|---|---|---|
| Pending step | `GET /api/v1/participants/me/onboarding` (`vd:onboarding`) | `200` step + links |
| Current consent form | `GET /api/v1/consent-forms/current` (`vd:consent-form`) | `200` text in the request locale; `404 not-found` when none |
| Reference text | `GET /api/v1/consent-forms/{version}/texts/{language}` (`vd:consent-text`) | `200` text of that version and language |
| Decision | `GET` / `PUT /api/v1/participants/me/consent` (`vd:consent`) | `200` |
| Skill | `GET` / `PUT /api/v1/participants/me/skill` (`vd:skill`) | `200` |
| Screening | `GET` / `PUT /api/v1/participants/me/screening` (`vd:screening`) | `200` |
| Screening example | `GET /api/v1/screening-example` (`vd:screening-example`) | `200` (English) |

- `PUT` answers `200` with the refreshed resource and `Content-Location` of the onboarding resource;
  it is a replacement of a participant-owned singleton, not a creation, so no `201`.
- State decides the affordance: the HAL-FORMS template of a step resource exists only while that
  step is pending, so a client that follows templates can not offer a step that would answer `409`.
- Codes in templates: `options.inline` lists the 13 skills and the 3 screening answers; display
  labels never travel in the API (spec 001 research, "Codes and labels").
- **Alternatives considered**: one `PUT /participants/me` carrying everything (the three steps have
  different preconditions and idempotency); RPC paths such as `/consent/accept` (breaks level 3).

## 10. Authorization (administrator never sees these)

- `SecurityConfig` already protects `/api/v1/participants/**` for `PARTICIPANT`. 006 adds
  `/api/v1/consent-forms/**` and `/api/v1/screening-example` with `hasRole(PARTICIPANT)`.
  Visitors get `401 authentication-required`, the administrator `403 forbidden` (logged like the
  other refusals of 004).
- `AuthorizationTest` enumerates every operation tagged "Participation" in the OpenAPI document of
  this spec and asserts both denials; the entry point never offers a participant link to the
  administrator (`EntryPointTest`).
- **Why participant-only for the consent text**: the TCLE is not secret, but nothing in the study
  needs it before sign-in, and a participant-only rule keeps the table of 004 simple. (The
  informational text a visitor may need, such as the research description, is spec 003 / 004.)

## 11. Problem Details

New: `study-not-open` (409), `consent-form-outdated` (409, `currentVersion`), `step-not-pending`
(409, `step`). Changed: `not-eligible` (007) gains `step`. Reused: `authentication-required`,
`forbidden`, `invalid-request` (new field codes `option` and `unexpected`), `not-found`. Details in
[contracts/problem-details.md](contracts/problem-details.md).

## 12. Front end

- **Screens** ([contracts/web.md](contracts/web.md)): consent (scrollable text, translation note,
  sticky buttons, closing "thank you" after a refusal), skill (Material radio group of 13 options
  and an "Other" text field with a 100-character counter), screening (card with the English example,
  radio group of three, submit), closing screens for "No" and for "study not open yet".
- **Labels**: `core/labels/skill-labels.ts` and `screening-labels.ts` map each code to a
  `$localize` string with a stable `@@` id (same pattern as `response-labels.ts` of 007); the
  English example is rendered from the API with `lang="en"` and `translate="no"`.
- **Unsaved input across a language switch**: the choice (and the "Other" text) are kept in
  `sessionStorage` under `veredito-unsaved:<route>` (glossary) and restored after the reload; the
  switch itself calls nothing (US4 scenario 5, edge case "switches the language between steps").
- **Accessibility**: radio groups with legends, the consent buttons stay in a sticky footer so they
  are visible with a very long text (edge case), focus moves to the heading on route change, and
  nothing requires scrolling to the end before the buttons enable (the spec does not ask for it, and
  forced scrolling hurts accessibility).

## 13. Impact on spec 007 (already deployed)

- **Tests**: `ApiTestBase.participantSession()` returns a **new, not onboarded** participant (what
  004 tests need). A new `eligibleParticipantSession()` accepts the current (fixture) form, sets a
  skill and a `yes_professional` screening by direct SQL, and returns the session. The 007 API tests
  (`TaskReservationTest`, `TaskDraftTest`, `TaskSubmissionTest`, `TaskConcurrencyTest`,
  `TaskExpiryTest`, `TaskLanguageTest`, `LocalizedProblemDetailsTest` for `not-eligible`) move to
  it; the `not-eligible` case no longer needs a test double and uses a fresh participant.
  `EntryPointTest` asserts the new participant links.
- **Playwright**: `support/participant.ts#signInParticipant` completes the onboarding over the API
  (so the 007 specs keep testing labeling); the label specs no longer click "Start labeling" because
  `/welcome` redirects. `onboarding.spec.ts` covers the real UI path once per locale.
- **Welcome page**: the 007 text ("You can request a block to label." and the "Start labeling"
  button) is removed together with its XLIFF units; `i18n:check` must stay green.
- **Backwards compatibility of the API**: `not-eligible` only gains an extension; no other 007
  contract changes.

## 14. Deployment and rollback

- No DDL and no change to `db/migration` for the fixture, so a rollback to 0.7.0 starts normally
  (the old always-true gate ignores the new data). The production consent migration (section 3) is
  plain `INSERT`s and is also harmless to 0.7.0.
- Order for production: (1) CEP-approved pt-BR text and translations delivered; (2) migration
  reviewed and merged; (3) staging run of [quickstart.md](quickstart.md); (4) researcher approves;
  (5) `git push origin <commit>:refs/tags/deploy/0.6.0-1`. Pilot participants of 0.7.0 pass the
  three steps on their next sign-in.
- **Alternatives considered**: deploying 006 before the text exists with the staging fixture
  enabled (never: fake TCLE, Principle IV and the ethics approval).

## 15. Testing strategy (Principle II)

| Level | What |
|---|---|
| Domain (JUnit) | `OnboardingRulesTest`: the truth table of every combination of consent (none / refused / accepted old / accepted current), skill (null / set), screening (null / yes / no) and form (none / current) -> step and eligibility (SC-002); `SkillRulesTest`: all 13 codes, `other` empty, 100 vs 101 code points (including an emoji), `skillOther` with another code; `ConsentFormRulesTest`: latest complete published wins, incomplete version skipped with its missing languages, future `published_at` ignored |
| Persistence (Testcontainers) | `ConsentFormRepositoryTest`; `FixtureLocationTest` (staging location only; default and `prod` configuration do not list it; `MigrationTest` keeps `V1`..`V5`) |
| API (REST Assured) | consent in each language with `translation`, `language` recorded (SC-004); refuse -> sign in again -> accept; new version -> only consent asked again; incomplete version not current; study not open; skill and screening in three languages store identical codes (SC-005); idempotent retries and `409 step-not-pending`; direct `POST /tasks/reservations` is `403 not-eligible` for every non-eligible combination (SC-002); administrator and visitor denied |
| Front end (Vitest) | step -> route map, resolver, guards, skill form validation, translation note, unsaved restore |
| E2E (Playwright, staging) | three new e-mails, one per locale: accept (check the recorded language in MySQL), refuse and change mind, "No" and re-sign-in; reference link opens pt-BR without changing the locale |
