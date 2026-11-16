---
description: "Tasks of spec 010: data collection progress dashboard"
---

# Tasks: Data collection progress dashboard

**Input**: documents in `specs/010-progress-dashboard/` (plan.md, spec.md, research.md, data-model.md, contracts/, quickstart.md)

**Prerequisites**: specs 005–009 complete; Docker Desktop running; JDK 25; Minitest CSV available

**Tests**: included (domain, REST Assured, Vitest, Playwright). SC-001/SC-003 in API tests; SC-002 measured or recorded in staging/CI.

**Organization**: foundation first; then US1–US3 on one snapshot resource; admin UI; staging/CI; polish. Deploy only with explicit approval.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: can run in parallel
- **[Story]**: US1–US3
- Paths relative to `Veredito/`
- `<pkg>` = `backend/src/main/java/io/github/diegoquirino/veredito`
- `<tpkg>` = `backend/src/test/java/io/github/diegoquirino/veredito`
- `<app>` = `frontend/src/app`
- English in repo; UI three locales
- UTF-8; `scripts/_check_utf8.py` after phases; commit per phase on `Veredito`
- Production deploy only after explicit researcher approval

---

## Phase 1: Setup

- [x] T001 Update `backend/pom.xml` to `0.10.0`; `frontend/package.json` / `package-lock.json` to `0.10.0`; `VEREDITO_VERSION` defaults in `compose.yaml`, `Dockerfile`, `SystemVersion.FALLBACK`, `write-build-info.mjs` (and OpenAPI example if present) to `0.10.0+dev`
  _Record_: pom, package.json/lock, compose, Dockerfile, `SystemVersion.FALLBACK`, `write-build-info.mjs` and the OpenAPI example at `0.10.0`.
- [x] T002 Set `Status` of `specs/010-progress-dashboard/spec.md` to "In progress" (`Complete` only after production deploy); update ROADMAP line of 010 to In progress
  _Record_: `spec.md` Status is "In progress"; ROADMAP line of 010 says In progress (0.10.0).

---

## Phase 2: Foundational (domain + wiring)

**CRITICAL**: stories share one snapshot resource; domain + HAL wiring first

- [x] T003 [P] Domain tests then `<pkg>/domain/dashboard/` helpers: task-bucket classification (free / inProgress / completed per research §3), `completedPercent`, first-free queue position
  _Record_: `domain/dashboard`: `TaskBucket`, `TaskCounts`, `DashboardProgress` (rows, first free queue position); `DashboardRulesTest` green.
- [x] T004 [P] Domain tests then participant status buckets (research §5), language tallies (always three locales), e-mail remaining / `lowRemaining`
  _Record_: `ParticipantBucket`, `ParticipantTotals`, `LanguageTally`, `EmailQuota`; covered by `DashboardRulesTest` (33 cases).
- [x] T005 Add `LinkRelations.DASHBOARD`; document at `/api/v1/rels/dashboard`
  _Record_: `LinkRelations.DASHBOARD` (documented at `/api/v1/rels/dashboard`).
- [x] T006 Emit `vd:dashboard` for administrators on `GET /api/v1`; update OpenAPI description that still says “later releases”; update `EntryPointTest`
  _Record_: `vd:dashboard` for the administrator; OpenAPI text updated; `EntryPointTest` green.
- [x] T007 Add `/api/v1/dashboard/**` to `SecurityConfig.ADMINISTRATION`
  _Record_: `/api/v1/dashboard/**` in `SecurityConfig.ADMINISTRATION`; `AuthorizationTest` covers it.

**Checkpoint**: entry point offers `vd:dashboard`; domain rules green

---

## Phase 3: User Story 1 — Queue progress (P1)

- [x] T008 [US1] Failing `<tpkg>/api/v1/DashboardTest`: overall + per system/batch progress; role buckets; `firstFreeQueuePosition`; admin only; empty study zeros + `vd:batches`
  _Record_: `DashboardTest` (18 cases): overall and per system/batch rows, empty study with `vd:batches`, admin only.
- [x] T009 [US1] `DashboardQuery` + `DashboardService` + `DashboardController` `GET /api/v1/dashboard` returning progress section (HAL model); OpenAPI Administration tag
  _Record_: `DashboardQuery`, `DashboardService`, `DashboardController` + `DashboardModel`; tag Administration; `ApiDocumentationCompletenessTest` green.
- [x] T010 [US1] Assert SC-001 task totals against seeded free/reserved/completed (expired reserved counts as free)
  _Record_: `bucketsMatchTheTablesAndAnExpiredReservationCountsAsFree` compares totals with the task table.

**Checkpoint**: admin can read queue progress via HAL

---

## Phase 4: User Story 2 — Reservations (P2)

- [x] T011 [US2] Extend `DashboardTest`: reserved list with pseudonym, block, role, draftCount, expiresAt, expired flag; no name/email fields in JSON
  _Record_: Reservation list test: pseudonym, block, role, `draftCount`, `expired`; no name/e-mail anywhere.
- [x] T012 [US2] Query + map reservations into the snapshot (ordered by `expires_at`)
  _Record_: Reservations ordered by `expires_at` in `DashboardQuery.reservations()`.

**Checkpoint**: stalled and active reservations visible

---

## Phase 5: User Story 3 — Participants, languages, quota (P2)

- [x] T013 [US3] Extend `DashboardTest`: status buckets, bySkill, completedByPseudonym shares, languages (zeros included), emailQuota; SC-003 vs final-response language counts
  _Record_: Tests for status buckets, `bySkill`, `completedByPseudonym`, languages, `emailQuota`; final language counts equal the long export tally (SC-003).
- [x] T014 [US3] Query + map participants / languages / emailQuota into the snapshot (`AccessCodeRepository` + `ConfigRepository`)
  _Record_: Participants, languages and quota mapped in `DashboardService` via `AccessCodeRepository.countAllSince` and `ConfigRepository.dailyEmailLimit`.

**Checkpoint**: full snapshot green in `DashboardTest`

---

## Phase 6: Admin UI

- [x] T015 Enable Dashboard link on admin home; update `@@admin.soon` to participants only; route `/admin/dashboard`
  _Record_: Dashboard is a link to `/admin/dashboard`; `admin.soon` mentions only participants in en-US, pt-BR and es-ES.
- [x] T016 Build `<app>/admin/dashboard/` (follow `vd:dashboard`, four panels, Refresh, empty?Import, low-remaining highlight, expired label); XLIFF three locales; Vitest
  _Record_: `frontend/src/app/admin/dashboard/` (service follows `vd:dashboard`, four panels, Refresh, empty state with Import, low-quota highlight, Expired label); XLIFF pt-BR/es-ES; Vitest 216/216; `npm run i18n:check` and `npm run build` green.
- [x] T017 Playwright: admin opens dashboard after import; participant has no dashboard
  _Record_: `e2e/admin.spec.ts`: administrator opens the dashboard after import and refreshes; participant has no link and is sent to consent; visitor is sent to sign-in. Written but not run here (needs the staging stack).

---

## Phase 7: Staging and deployment

- [x] T018 Run `mvnw verify`; frontend `i18n:check`, `test`, `build`
  _Record_: Backend: `DashboardRulesTest`, `DashboardTest`, `EntryPointTest`, `AuthorizationTest`, `ApiDocumentationCompletenessTest`, `SecurityRulesTest` green (110 tests). Full `mvnw verify` not run. Frontend: `i18n:check`, `test`, `build` green.
- [ ] T019 Quickstart sections 2–6 against staging; record SC-002 timing if measured
- [ ] T020 Push `Veredito`; wait for `ci.yml`; deploy `deploy/0.10.0-1` **only after explicit researcher approval**; quickstart §7 in production

---

## Phase 8: Polish

- [ ] T021 [P] `scripts/_check_utf8.py` over changed folders
- [ ] T022 [P] Secret scan deferred to `ci.yml` (same as prior specs)
- [ ] T023 Update ROADMAP line of 010 and 001 overview; mark `Complete` in `spec.md` only after T020; point `.specify/feature.json` at `specs/011-lgpd-withdrawal`

---

## Dependencies

- Phase 2 before US1–US3
- US1 controller/service before US2–US3 fields (same resource)
- UI after snapshot covers US1 at least (prefer full snapshot)
- T020 last for Complete status

## Parallel opportunities

- T003–T004; T005–T007 after domain starts
- T015 can start once T006 exists (stub empty UI), but prefer after T014
- T021–T022

## Implementation strategy

1. Version + In progress
2. Domain + links/security
3. Progress ? reservations ? participants/languages/quota on one GET
4. Admin UI + e2e
5. Staging / CI; deploy only with approval
