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

- [ ] T001 Update `backend/pom.xml` to `0.10.0`; `frontend/package.json` / `package-lock.json` to `0.10.0`; `VEREDITO_VERSION` defaults in `compose.yaml`, `Dockerfile`, `SystemVersion.FALLBACK`, `write-build-info.mjs` (and OpenAPI example if present) to `0.10.0+dev`
- [ ] T002 Set `Status` of `specs/010-progress-dashboard/spec.md` to "In progress" (`Complete` only after production deploy); update ROADMAP line of 010 to In progress

---

## Phase 2: Foundational (domain + wiring)

**CRITICAL**: stories share one snapshot resource; domain + HAL wiring first

- [ ] T003 [P] Domain tests then `<pkg>/domain/dashboard/` helpers: task-bucket classification (free / inProgress / completed per research §3), `completedPercent`, first-free queue position
- [ ] T004 [P] Domain tests then participant status buckets (research §5), language tallies (always three locales), e-mail remaining / `lowRemaining`
- [ ] T005 Add `LinkRelations.DASHBOARD`; document at `/api/v1/rels/dashboard`
- [ ] T006 Emit `vd:dashboard` for administrators on `GET /api/v1`; update OpenAPI description that still says “later releases”; update `EntryPointTest`
- [ ] T007 Add `/api/v1/dashboard/**` to `SecurityConfig.ADMINISTRATION`

**Checkpoint**: entry point offers `vd:dashboard`; domain rules green

---

## Phase 3: User Story 1 — Queue progress (P1)

- [ ] T008 [US1] Failing `<tpkg>/api/v1/DashboardTest`: overall + per system/batch progress; role buckets; `firstFreeQueuePosition`; admin only; empty study zeros + `vd:batches`
- [ ] T009 [US1] `DashboardQuery` + `DashboardService` + `DashboardController` `GET /api/v1/dashboard` returning progress section (HAL model); OpenAPI Administration tag
- [ ] T010 [US1] Assert SC-001 task totals against seeded free/reserved/completed (expired reserved counts as free)

**Checkpoint**: admin can read queue progress via HAL

---

## Phase 4: User Story 2 — Reservations (P2)

- [ ] T011 [US2] Extend `DashboardTest`: reserved list with pseudonym, block, role, draftCount, expiresAt, expired flag; no name/email fields in JSON
- [ ] T012 [US2] Query + map reservations into the snapshot (ordered by `expires_at`)

**Checkpoint**: stalled and active reservations visible

---

## Phase 5: User Story 3 — Participants, languages, quota (P2)

- [ ] T013 [US3] Extend `DashboardTest`: status buckets, bySkill, completedByPseudonym shares, languages (zeros included), emailQuota; SC-003 vs final-response language counts
- [ ] T014 [US3] Query + map participants / languages / emailQuota into the snapshot (`AccessCodeRepository` + `ConfigRepository`)

**Checkpoint**: full snapshot green in `DashboardTest`

---

## Phase 6: Admin UI

- [ ] T015 Enable Dashboard link on admin home; update `@@admin.soon` to participants only; route `/admin/dashboard`
- [ ] T016 Build `<app>/admin/dashboard/` (follow `vd:dashboard`, four panels, Refresh, empty?Import, low-remaining highlight, expired label); XLIFF three locales; Vitest
- [ ] T017 Playwright: admin opens dashboard after import; participant has no dashboard

---

## Phase 7: Staging and deployment

- [ ] T018 Run `mvnw verify`; frontend `i18n:check`, `test`, `build`
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
