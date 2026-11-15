---

description: "Tasks of spec 006: consent form, professional skill and screening"
---

# Tasks: Consent form, professional skill and screening

**Input**: documents in `specs/006-consent-screening/` (plan.md, spec.md, research.md, data-model.md, contracts/, quickstart.md)

**Prerequisites**: specs 004, 005 and 007 complete; Docker Desktop running; JDK 25; staging fixture consent form loadable

**Tests**: included. Constitution Principle II requires them (domain unit tests, Testcontainers, REST Assured from `/api/v1`, bundle parity, XLIFF completeness, Playwright). SC-001 to SC-005 are checked by automated tests.

**Organization**: tasks grouped by user story; each story can be tested on its own once the foundation is ready.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: can run in parallel (different files, no dependency on an incomplete task)
- **[Story]**: story of the spec (US1 to US4)
- Paths are relative to the root of the `Veredito` branch (the `Veredito` folder)
- `<pkg>` = `backend/src/main/java/io/github/diegoquirino/veredito`; `<tpkg>` = `backend/src/test/java/io/github/diegoquirino/veredito`; `<app>` = `frontend/src/app`
- Code, comments, identifiers and commit messages in English; user-facing texts in pt-BR, en-US and es-ES, following `specs/GLOSSARY.md`; every new Angular text gets its pt-BR and es-ES targets in `frontend/src/locale/messages.pt-BR.xlf` and `messages.es-ES.xlf` in the same task
- No e-mail, code or token in logs, test output, chat or screenshots; test e-mails under `example.com`
- Every text file in UTF-8; after each phase, run `scripts/_check_utf8.py` (in `TRABALHO DE TESE/scripts/`) over the changed folders
- Commit at the end of each phase on the `Veredito` branch
- Production deploy only after explicit researcher approval

---

## Phase 1: Setup

**Purpose**: version bump and packages

- [x] T001 Update `backend/pom.xml` to version `0.6.0` (research section 1; no new Maven dependency). Update `frontend/package.json` / `package-lock.json` to `0.6.0`; default `VEREDITO_VERSION` in `compose.yaml`, `SystemVersion.FALLBACK`, `Dockerfile` and `frontend/scripts/write-build-info.mjs` to `0.6.0+dev` _Record: version 0.6.0 in pom, package.json/lock, compose, Dockerfile, SystemVersion, write-build-info_
- [x] T002 [P] Create `package-info.java` in `<pkg>/onboarding` (services that coordinate consent, skill, screening and the gate) and `<pkg>/domain/onboarding` (plain-Java step order and eligibility rules) _Record: package-info in onboarding and domain.onboarding_

---

## Phase 2: Foundational

**Purpose**: fixture consent form, domain rules, repositories, gate replacement, entry-point links — used by every story

**CRITICAL**: no story starts before this phase is complete

- [x] T003 Add staging-only Flyway location `backend/src/main/resources/db/staging/V6__consent_form_fixture.sql` with a published fixture consent form (three languages, marked not CEP-approved) as in `data-model.md`; wire the location only for `staging` / test profiles so production never loads it; log incomplete published versions at startup (FR-012) _Record: db/staging/V6 fixture-1 (3 languages); flyway locations only in application-staging.yaml and ApiTestBase; ConsentFormAudit logs incomplete published forms at startup_
- [x] T004 Extend `<tpkg>/persistence/MigrationTest.java` (or a dedicated staging-location test): V1–V5 still apply; fixture appears only when the staging location is enabled; production classpath has no fixture _Record: FixtureLocationTest (fixture only with staging location, not on the production classpath); ConsentFormRepositoryTest_
- [x] T005 [P] Add Problem Details types of `contracts/problem-details.md` to `<pkg>/api/ProblemType.java` (`study-not-open` 503 or as contracted, `consent-form-outdated` 409, `step-not-pending` 409) with `problem.<slug>.title` / `.detail` in the three message bundles; extend `not-eligible` detail to carry optional `step` when present _Record: 3 problem types + bundles pt-BR/en-US/es-ES; not-eligible carries step_
- [x] T006 [P] Write domain tests first, then classes, in `<pkg>/domain/onboarding/` and `<tpkg>/domain/onboarding/` (plain Java; ArchUnit of 003 guards it): _Record: OnboardingRulesTest, SkillRulesTest, ConsentFormRulesTest (35 tests)_
  - `OnboardingRules.stepOf(...)`: order `ineligible` ? `not-open` ? `consent` ? `skill` ? `screening` ? `participation` (research §2); cover refuse-then-return, new version after accept, permanent `no`
  - `EligibilityRules` / gate truth table: eligible only with current-form acceptance + skill set + screening yes codes
  - Skill validation: codes of Appendix B.4; `other` requires non-blank description ? 100 chars
- [x] T007 Create / extend JdbcClient repositories in `<pkg>/persistence/`: _Record: ConsentFormRepository + ParticipantRepository additions; ConsentFormRepositoryTest on Testcontainers_
  - `ConsentFormRepository`: `findCurrent(now)`, `findText(version, language)`, `listIncompletePublished()`
  - Extend `ParticipantRepository`: `findOnboardingState(id)`, `recordConsent(...)`, `recordSkill(...)`, `recordScreening(...)`
  Integration test with Testcontainers + staging fixture
- [x] T008 Replace `AlwaysEligibleGate` with `<pkg>/onboarding/OnboardingEligibilityGate` implementing `EligibilityGate` (research §2); remove or demote the always-true bean so only the new gate is active; keep injectability for tests that force false _Record: OnboardingEligibilityGate replaces AlwaysEligibleGate (deleted); EligibilityGate gained pendingStep/refusal_
- [x] T009 Add participant links on `GET /api/v1` per `contracts/onboarding.md`: `vd:onboarding`, `vd:consent-form` (when current form exists), `vd:next-task` only when gate true; document relations in `LinkRelations` / `RelationController` _Record: vd:onboarding, vd:consent-form, vd:next-task only when eligible, plus consent/skill/screening links; LinkRelations_
- [x] T010 Add `eligibleParticipantSession()` (and helpers to seed consent/skill/screening) on `<tpkg>/support/ApiTestBase.java`; update every 007 API test that reserves/labels so it uses an eligible participant (research § side effects); run `.\mvnw.cmd -B verify` green with the new gate _Record: eligibleParticipantSession() and seed helpers in ApiTestBase; all 007 tests converted; mvnw verify green (481 tests)_

**Checkpoint**: foundation ready — user stories can start

---

## Phase 3: User Story 1 - Accept or refuse the consent form (Priority: P1) ?? MVP

**Goal**: participant reads the current consent form and accepts or refuses; refusal ends; new version requires re-accept

**Independent Test**: new e-mail refuse; another accept; publish new version and sign in again

- [x] T011 [P] [US1] Create failing `<tpkg>/api/v1/ConsentFormTest.java`: current form in three locales; accept records version/language/time; refuse records refusal and session ends or next onboarding is closed; refuse then accept on return; new published version forces consent again without resetting skill/screening; no current form ? study not open / `not-open` _Record: ConsentFormTest_
- [x] T012 [US1] Implement `<pkg>/onboarding/ConsentService.java` and controllers for `GET /api/v1/consent-forms/current`, `GET /api/v1/consent-forms/{version}/texts/{language}`, `GET|PUT /api/v1/participants/me/consent` per `contracts/onboarding.md` (HAL-FORMS; OpenAPI tag Participation) _Record: ConsentService, ConsentFormController, ConsentDecisionController_
- [x] T013 [US1] Implement `GET /api/v1/participants/me/onboarding` (`OnboardingController` / service) returning `step` and links _Record: OnboardingController_
- [x] T014 [US1] Secure new Participation paths as participant-only in `SecurityConfig`; extend `AuthorizationTest` _Record: SecurityConfig matcher; AuthorizationTest lists the 006 operations_
- [x] T015 [US1] Front end: `/consent` screen with scrollable text, accept/refuse, translation note + pt-BR link for en-US/es-ES; `/welcome` resolves via onboarding step; XLIFF; `npm run i18n:check` _Record: OnboardingService, step guards (onboardingStep, participationOnly), welcome resolver, /consent, /consent/declined, /consent/reference/:version, not-eligible interceptor; old welcome XLIFF units removed; i18n:check green (187 units)_
- [x] T016 [US1] Playwright: accept path reaches skill (or next); refuse shows closing message; change of mind on return _Record: e2e/onboarding.spec.ts: accept path to skill per locale, refusal then reading the form again and accepting; passed on local staging_

**Checkpoint**: US1 testable alone

---

## Phase 4: User Story 2 - State the professional skill (Priority: P1)

**Goal**: one skill code from Appendix B.4; Other needs description ? 100

**Independent Test**: proceed disabled without choice; Other empty refused; common option stored; three languages same code

- [x] T017 [P] [US2] Create failing `<tpkg>/api/v1/SkillTest.java`: PUT skill codes; Other validation; step-not-pending when consent missing; language-independent codes _Record: SkillTest_
- [x] T018 [US2] Implement `GET|PUT /api/v1/participants/me/skill` (`SkillService` / controller) per contracts _Record: SkillService, SkillController_
- [x] T019 [US2] Front end: `/skill` radio list (Appendix B.4 labels in locale), Other text field, Continue disabled until valid; XLIFF _Record: SkillComponent, skill-labels (13 codes, exhaustive Record), unsaved restore after a language switch; Vitest for validation (100/101, code points)_
- [x] T020 [US2] Playwright: choose skill in pt-BR; stored code is English; Other requires description _Record: onboarding.spec.ts: skill per locale, Other needs a description; the stored skill is the English code_

**Checkpoint**: US1 + US2

---

## Phase 5: User Story 3 - Answer the screening (Priority: P1)

**Goal**: screening with English example; Yes ? eligible ? participation; No ? permanent ineligible

**Independent Test**: each of three answers with different e-mails; ineligible cannot label; eligible skips steps on return

- [x] T021 [P] [US3] Create failing `<tpkg>/api/v1/ScreeningTest.java`: answers set `eligible`; `no` permanent; return always `ineligible`; yes paths reach participation / gate true; Tasks reserve denied until eligible _Record: ScreeningTest, EligibilityGateTest, OnboardingFlowTest_
- [x] T022 [US3] Add English screening example resource (`classpath` JSON) and `GET /api/v1/screening-example`; implement `GET|PUT /api/v1/participants/me/screening` _Record: screening/screening-example.json, ScreeningExampleController, ScreeningService/Controller_
- [x] T023 [US3] Front end: `/screening` with English UC/TC example, localized question/options, closing screens for refuse / ineligible / not-open; after Yes navigate to `/label`; XLIFF _Record: ScreeningComponent (English example cards, localized question and answers), closing screens (ClosingComponent), /not-open; resolver sends yes to /label_
- [x] T024 [US3] Playwright: Yes professional ? can reserve; No ? closing message and cannot open `/label` / next-task; resume after reload _Record: onboarding.spec.ts: No leads to the closing screen and every route returns to it, API 403 not-eligible; typed /label returns to the pending step; reload keeps the step_

**Checkpoint**: full onboarding gate works

---

## Phase 6: User Story 4 - Consent form language (Priority: P1)

**Goal**: three languages; translation note; record language of text shown; incomplete version never becomes current

- [x] T025 [P] [US4] Extend consent/onboarding tests: accept in each locale records `consent_language`; incomplete published version skipped + logged; language switch on consent screen does not record until choose _Record: ConsentFormTest (per-locale language, incomplete form skipped and logged, switch records nothing) in the first backend commit; OnboardingFlowTest checks consent_language for the 3 locales_
- [x] T026 [US4] Front end: translation note + open pt-BR reference without changing UI locale; language switch reloads consent text; XLIFF _Record: consent translation note + pt-BR reference link (new tab, same interface language); Vitest for note only when translation=true_
- [x] T027 [US4] Playwright smoke: one accept per locale records matching language; en-US/es-ES show note and reference link _Record: onboarding.spec.ts: one accept per locale; stored consent language matches; note and reference link in en-US and es-ES; language switch records nothing_

**Checkpoint**: i18n consent complete

---

## Phase 7: Staging rehearsal and deployment

- [x] T028 Run quickstart sections against local staging; record any fix _Record: partly: local staging rebuilt (down -v, up --build); full Playwright suite passed (77 tests, plus queue project 5/5 and one rerun of a goto flake); incomplete fixture-3 logged a WARN naming en-US, es-ES; sections 5 (new version) and not-open covered by ConsentFormTest, not by hand_
- [x] T029 [P] Confirm SC-002 (direct Tasks call while ineligible) and onboarding e2e pass in CI
  **Record (2026-11-15):** `ci.yml` run [43770449843](https://github.com/diegoquirino/openscience/actions/runs/43770449843) green on `2a78d48` (backend, frontend, secrets, image, e2e). Earlier `65cc349` e2e failed on `queue-empty` before the welcome-resolver fix in `2a78d48`.
- [ ] T030 Push to `Veredito`; wait for `ci.yml`; deploy `deploy/0.6.0-1` **only after the researcher's approval**; quickstart production checks
  **Left open on purpose (2026-11-15):** CI is green on `2a78d48`. Deploy tag waits for the researcher's explicit approval.

---

## Phase 8: Polish & Cross-Cutting Concerns

- [x] T031 [P] Run `scripts/_check_utf8.py` over the changed Veredito folders _Record: _check_utf8.py clean over backend/src, frontend/src, frontend/e2e, specs/006-consent-screening_
- [x] T032 [P] Confirm secret scanning still clean; no new secrets introduced _Record: no new secrets: no credential patterns in the diff; fixture is a placeholder text; gitleaks not installed locally, CI job runs it_
- [ ] T033 Mark spec 006 complete: `Status` in `spec.md`; ROADMAP line of 006; update 001 overview if needed; point `.specify/feature.json` at next spec (`specs/008-item-screen`); commit and push

---

## Dependencies & Execution Order

- Setup ? Foundational (blocks every story)
- US1 ? US2 ? US3 (ordered steps); US4 can finish after US1 screens exist
- Staging / deploy after all stories
- Polish after staging

### Parallel Opportunities

- T001 with T002; T005 with T006; T011 with domain polish; T017 with late US1 UI; T021 with US2 wrap-up; T031 with T032

---

## Implementation Strategy

1. Phases 1–2 (version, fixture, gate, 007 test helpers)
2. US1 consent + onboarding redirect (MVP)
3. US2 skill ? US3 screening ? gate opens labeling
4. US4 language polish
5. Staging, CI, production tag only with approval
6. Polish and status

## Notes

- No DDL in production Flyway for 006; fixture only under `db/staging`.
- Real CEP-approved TCLE for production is a separate researcher-supplied migration later.
- Deploy tags only after explicit researcher approval.
- Screening example UC/TC are English fixtures; labels of skills/answers follow Appendices B.3/B.4.
