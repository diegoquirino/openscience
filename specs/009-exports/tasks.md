---
description: "Tasks of spec 009: export the labels"
---

# Tasks: Export the labels

**Input**: documents in `specs/009-exports/` (plan.md, spec.md, research.md, data-model.md, contracts/, quickstart.md)

**Prerequisites**: specs 005–008 complete; Docker Desktop running; JDK 25; Minitest CSV available; `ISSTA_2027` checkout beside Veredito

**Tests**: included (domain, REST Assured, Vitest, Playwright, `issta_tcm` CLI tests). SC-002 measured in tests or recorded; SC-001/SC-001b/SC-001c covered by pipeline tests where feasible.

**Organization**: foundation first; then US1–US4 in Veredito; then FR-004c in ISSTA_2027; staging/CI; polish. Deploy only with explicit approval.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: can run in parallel
- **[Story]**: US1–US4 or CLI
- Paths relative to `Veredito/` unless marked `ISSTA_2027/`
- `<pkg>` = `backend/src/main/java/io/github/diegoquirino/veredito`
- `<tpkg>` = `backend/src/test/java/io/github/diegoquirino/veredito`
- `<app>` = `frontend/src/app`
- English in repo; UI three locales; CSV headers/codes English
- UTF-8; `scripts/_check_utf8.py` after phases; commit per phase on `Veredito` (ISSTA commits on its own tree/branch as usual)
- Production deploy only after explicit researcher approval

---

## Phase 1: Setup

- [ ] T001 Update `backend/pom.xml` to `0.9.0`; `frontend/package.json` / `package-lock.json` to `0.9.0`; `VEREDITO_VERSION` defaults in `compose.yaml`, `Dockerfile`, `SystemVersion.FALLBACK`, `write-build-info.mjs` (and OpenAPI example if present) to `0.9.0+dev`
- [ ] T002 Set `Status` of `specs/009-exports/spec.md` to "In progress" (`Complete` only after production deploy)

---

## Phase 2: Foundational (domain + wiring)

**CRITICAL**: stories need domain writers and security/link scaffolding

- [ ] T003 [P] Domain tests then `<pkg>/domain/exporting/FormulaNeutralizer` (prefix `'` for leading `= + - @`; leave others unchanged)
- [ ] T004 [P] Domain tests then header constants and row builders for wide, long, completo, rater (exact headers from research/contracts; blank incomplete roles; anonymized marker; consent cell format)
- [ ] T005 [P] Domain tests then RFC 4180 UTF-8 CSV serializer (Commons CSV or pure writer used by domain/service without Spring types in `domain.exporting`)
- [ ] T006 Add `LinkRelations` for `exports`, `export-wide`, `export-long`, `export-completo`, `export-rater1`, `export-rater2`; document at `/api/v1/rels/{rel}`
- [ ] T007 Emit `vd:exports` for administrators on `GET /api/v1`; update `EntryPointTest`
- [ ] T008 Add `/api/v1/exports/**` to `SecurityConfig.ADMINISTRATION`

---

## Phase 3: User Story 1 — Pipeline rater files (P1)

- [ ] T009 [US1] Failing `<tpkg>/api/v1/ExportTest` (or focused class): rater1/rater2 for `minitest`; nine columns; incomplete role blanks; drafts absent; admin only
- [ ] T010 [US1] JDBC query + `ExportService` + `ExportController` `GET …/rater1|rater2?system=`; `system-not-found` / missing system; OpenAPI `text/csv`
- [ ] T011 [US1] Index `GET /api/v1/exports` lists systems and per-system rater links

**Checkpoint**: admin can download rater CSVs via HAL

---

## Phase 4: User Story 2 — Wide table (P1)

- [ ] T012 [US2] Extend `ExportTest`: Appendix C header; both raters; neutralization; anonymized; consent cell; admin locale does not change codes
- [ ] T013 [US2] Implement `GET /api/v1/exports/wide` + `vd:export-wide`

**Checkpoint**: wide export green

---

## Phase 5: User Story 3 — Completo (P1)

- [ ] T014 [US3] Extend `ExportTest`: completo header; Appendix D exact; five role columns; filter by system
- [ ] T015 [US3] Implement `GET /api/v1/exports/completo?system=` + index links

**Checkpoint**: completo matches import Appendix D cells for completed Minitest (SC-001b in test or quickstart)

---

## Phase 6: User Story 4 — Long table (P2)

- [ ] T016 [US4] Extend `ExportTest`: long header; one row per final; no name/email; languages; timestamps
- [ ] T017 [US4] Implement `GET /api/v1/exports/long` + `vd:export-long`

**Checkpoint**: all four formats available from index

---

## Phase 7: Admin UI

- [ ] T018 Enable Export link on admin home; update soon-text; route `/admin/export`
- [ ] T019 Build `<app>/admin/export/` (HAL index, downloads with Content-Disposition filename); XLIFF three locales; Vitest
- [ ] T020 Playwright: admin opens export and gets CSV; participant has no export

---

## Phase 8: FR-004c — `oracle-from-veredito` (ISSTA_2027)

- [ ] T021 [CLI] Implement `issta_tcm` command per `contracts/oracle-from-veredito.md` (strict header, refuse incomplete, atomic write, English messages)
- [ ] T022 [CLI] Tests: success, incomplete roles, bad header, wrong system, invalid codes; register in `cli.py` / help text

---

## Phase 9: Staging and deployment

- [ ] T023 Run `mvnw verify`; frontend `i18n:check`, `test`, `build`
- [ ] T024 Quickstart sections 2–6 against staging (and ISSTA CLI on exported `_completo`); record fixes
- [ ] T025 Push `Veredito`; wait for `ci.yml`; deploy `deploy/0.9.0-1` **only after explicit researcher approval**; quickstart §7 in production

---

## Phase 10: Polish

- [ ] T026 [P] `scripts/_check_utf8.py` over changed folders
- [ ] T027 [P] Secret scan (`gitleaks` as in CI)
- [ ] T028 Update ROADMAP line of 009 and 001 overview; mark `Complete` in `spec.md` only after T025; point `.specify/feature.json` at `specs/010-progress-dashboard`

---

## Dependencies

- Phase 2 before US1–US4
- US1 foundation (query/service/controller) before US2–US4 endpoints (same service)
- UI after index + at least one download works
- CLI independent of UI (can parallel after completo format is stable)
- T025 last for Complete status

## Parallel opportunities

- T003–T005; T006–T008 after domain starts
- T021–T022 parallel to T018–T020
- T026–T027

## Implementation strategy

1. Version + In progress
2. Domain writers + links/security
3. Rater ? wide ? completo ? long
4. Admin UI + e2e
5. ISSTA CLI
6. Staging / CI; deploy only with approval
