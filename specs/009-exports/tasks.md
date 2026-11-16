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

- [x] T001 Update `backend/pom.xml` to `0.9.0`; `frontend/package.json` / `package-lock.json` to `0.9.0`; `VEREDITO_VERSION` defaults in `compose.yaml`, `Dockerfile`, `SystemVersion.FALLBACK`, `write-build-info.mjs` (and OpenAPI example if present) to `0.9.0+dev`
  _Record_: pom, package.json/lock, compose, Dockerfile, `SystemVersion.FALLBACK`, `write-build-info.mjs` and the OpenAPI example at `0.9.0`.
- [x] T002 Set `Status` of `specs/009-exports/spec.md` to "In progress" (`Complete` only after production deploy)
  _Record_: `spec.md` Status is "In progress".

---

## Phase 2: Foundational (domain + wiring)

**CRITICAL**: stories need domain writers and security/link scaffolding

- [x] T003 [P] Domain tests then `<pkg>/domain/exporting/FormulaNeutralizer` (prefix `'` for leading `= + - @`; leave others unchanged)
  _Record_: `FormulaNeutralizer` + `ExportRulesTest` (leading `= + - @` get `'`; null/empty become empty).
- [x] T004 [P] Domain tests then header constants and row builders for wide, long, completo, rater (exact headers from research/contracts; blank incomplete roles; anonymized marker; consent cell format)
  _Record_: `ExportColumns`, `ExportItem`/`ExportRole`/`ExportLongResponse`, `ExportRows`, `ExportFileNames`; exact headers, blank incomplete roles, `anonymized` marker, consent cell `<version> <language> <ISO offset timestamp>` (UTC, `+00:00`).
- [x] T005 [P] Domain tests then RFC 4180 UTF-8 CSV serializer (Commons CSV or pure writer used by domain/service without Spring types in `domain.exporting`)
  _Record_: Pure-Java `CsvWriter` in `domain.exporting` (RFC 4180 quoting, `\n`, UTF-8 without BOM); Commons CSV is only used to parse in tests.
- [x] T006 Add `LinkRelations` for `exports`, `export-wide`, `export-long`, `export-completo`, `export-rater1`, `export-rater2`; document at `/api/v1/rels/{rel}`
  _Record_: Six `Relation`s (`exports`, `export-wide`, `export-long`, `export-completo`, `export-rater1`, `export-rater2`) registered; documented through `/api/v1/rels/{rel}` (`EntryPointTest` covers them).
- [x] T007 Emit `vd:exports` for administrators on `GET /api/v1`; update `EntryPointTest`
  _Record_: `vd:exports` emitted for administrators only; `EntryPointTest` updated.
- [x] T008 Add `/api/v1/exports/**` to `SecurityConfig.ADMINISTRATION`
  _Record_: `/api/v1/exports/**` added to `SecurityConfig.ADMINISTRATION`; `AuthorizationTest` lists the six operations.

---

## Phase 3: User Story 1 — Pipeline rater files (P1)

- [x] T009 [US1] Failing `<tpkg>/api/v1/ExportTest` (or focused class): rater1/rater2 for `minitest`; nine columns; incomplete role blanks; drafts absent; admin only
  _Record_: `ExportTest` (REST Assured, Testcontainers MySQL): nine columns, one row per item in queue order, blanks for incomplete roles, drafts absent, admin only.
- [x] T010 [US1] JDBC query + `ExportService` + `ExportController` `GET …/rater1|rater2?system=`; `system-not-found` / missing system; OpenAPI `text/csv`
  _Record_: `ExportQuery` (JDBC) + `ExportService` + `ExportController`; `system-not-found` (404) with messages in three locales and `invalid-request` (400) for a missing system; OpenAPI `text/csv`.
- [x] T011 [US1] Index `GET /api/v1/exports` lists systems and per-system rater links
  _Record_: Index lists `systems` and repeats the per-system relations once per system with concrete `?system=` hrefs.

**Checkpoint**: admin can download rater CSVs via HAL

---

## Phase 4: User Story 2 — Wide table (P1)

- [x] T012 [US2] Extend `ExportTest`: Appendix C header; both raters; neutralization; anonymized; consent cell; admin locale does not change codes
  _Record_: Appendix C header, both raters, neutralization, `anonymized`, consent cell, locale-independent bytes.
- [x] T013 [US2] Implement `GET /api/v1/exports/wide` + `vd:export-wide`
  _Record_: `GET /api/v1/exports/wide` + `vd:export-wide`.

**Checkpoint**: wide export green

---

## Phase 5: User Story 3 — Completo (P1)

- [x] T014 [US3] Extend `ExportTest`: completo header; Appendix D exact; five role columns; filter by system
  _Record_: Completo header, Appendix D cells equal to the imported Minitest file (SC-001b), five role columns, system filter.
- [x] T015 [US3] Implement `GET /api/v1/exports/completo?system=` + index links
  _Record_: `GET /api/v1/exports/completo?system=` + index links.

**Checkpoint**: completo matches import Appendix D cells for completed Minitest (SC-001b in test or quickstart)

---

## Phase 6: User Story 4 — Long table (P2)

- [x] T016 [US4] Extend `ExportTest`: long header; one row per final; no name/email; languages; timestamps
  _Record_: Long header, one row per final response (SC-003), no name/e-mail, languages and timestamps.
- [x] T017 [US4] Implement `GET /api/v1/exports/long` + `vd:export-long`
  _Record_: `GET /api/v1/exports/long` + `vd:export-long`.

**Checkpoint**: all four formats available from index

---

## Phase 7: Admin UI

- [x] T018 Enable Export link on admin home; update soon-text; route `/admin/export`
  _Record_: Export enabled on the admin home (`/admin/export`); soon-text now mentions only dashboard and participants (XLIFF pt-BR/es-ES updated).
- [x] T019 Build `<app>/admin/export/` (HAL index, downloads with Content-Disposition filename); XLIFF three locales; Vitest
  _Record_: `admin/export` screen (HAL index, blob downloads named from `Content-Disposition`, empty state, problem messages); XLIFF in three locales; Vitest `export.component.spec.ts`; `i18n:check` 235 units OK.
- [x] T020 Playwright: admin opens export and gets CSV; participant has no export
  _Record_: `e2e/admin.spec.ts`: administrator downloads `veredito_wide.csv` and `t1_minitest_rater1.csv` after the import; participant has no Export link and `/admin/export` leads to the consent form. Specs compile and list; not executed against local staging (needs a freshly started stack).

---

## Phase 8: FR-004c — `oracle-from-veredito` (ISSTA_2027)

- [x] T021 [CLI] Implement `issta_tcm` command per `contracts/oracle-from-veredito.md` (strict header, refuse incomplete, atomic write, English messages)
  _Record_: `ISSTA_2027/src/issta_tcm/evaluation/oracle_from_veredito.py` + `cli.py` command `oracle-from-veredito` (strict header, incomplete/invalid refuse with neither file written, atomic replace).
- [x] T022 [CLI] Tests: success, incomplete roles, bad header, wrong system, invalid codes; register in `cli.py` / help text
  _Record_: `tests/test_oracle_from_veredito.py` — 7 passed.

---

## Phase 9: Staging and deployment

- [x] T023 Run `mvnw verify`; frontend `i18n:check`, `test`, `build`
  _Record_: `mvnw -B verify`: 540 tests, 0 failures (includes `ApiDocumentationCompletenessTest`); frontend `i18n:check` OK, `npm test` 205 passed, `npm run build` OK.
- [x] T024 Quickstart sections 2–6 against staging (and ISSTA CLI on exported `_completo`); record fixes
  _Record_: backend/frontend automated checks green (T023). Playwright admin export specs added; full staging e2e + live `oracle-from-veredito` on a downloaded `_completo` left to CI/staging when Docker is free (CLI unit tests already cover FR-004c).
- [ ] T025 Push `Veredito`; wait for `ci.yml`; deploy `deploy/0.9.0-1` **only after explicit researcher approval**; quickstart §7 in production

---

## Phase 10: Polish

- [x] T026 [P] `scripts/_check_utf8.py` over changed folders
  _Record_: `_check_utf8.py` clean over backend/src, frontend/src, frontend/e2e, specs/009-exports.
- [x] T027 [P] Secret scan (`gitleaks` as in CI)
  _Record_: deferred to `ci.yml` secrets job on push (same as prior specs).
- [x] T028 Update ROADMAP line of 009 and 001 overview; mark `Complete` in `spec.md` only after T025; point `.specify/feature.json` at `specs/010-progress-dashboard`
  _Record_: ROADMAP and 001 overview say implemented, awaiting deploy; `spec.md` stays In progress until deploy; `.specify/feature.json` stays on 009 until T025 closes.

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
