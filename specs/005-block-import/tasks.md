---

description: "Tasks of spec 005: import items by CSV and form the queue blocks"
---

# Tasks: Import items by CSV and form the queue blocks

**Input**: documents in `specs/005-block-import/` (plan.md, spec.md, research.md, data-model.md, contracts/, quickstart.md)

**Prerequisites**: spec 004 complete; Docker Desktop running; JDK 25 on the researcher's machine

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

---

## Phase 1: Setup

**Purpose**: versions, dependency, fixtures and packages

- [x] T001 Update `backend/pom.xml` to version `0.5.0` and add `org.apache.commons:commons-csv:1.14.1` (research section 1). Update `frontend/package.json` / `package-lock.json` to `0.5.0`; default `VEREDITO_VERSION` in `compose.yaml`, `SystemVersion`, `Dockerfile` and `write-build-info.mjs` to `0.5.0+dev`
- [x] T002 [P] Create `package-info.java` in `<pkg>/importing` (services that coordinate CSV reading, domain rules and persistence) and `<pkg>/domain/importing` (plain-Java import rules)
- [x] T003 [P] Add fixtures under `backend/src/test/resources/minitest/`: keep `veredito_minitest_vazio.csv`; add `minitest_duplicates.csv`, `minitest_missing_kind_column.csv`, `minitest_long_text.csv` and `other_system_sample.csv` as in `contracts/pipeline.md` and the quickstart
- [x] T004 [P] Set `spring.servlet.multipart.max-file-size` and `max-request-size` to `10MB` in `backend/src/main/resources/application.yaml` (`contracts/pipeline.md`)

---

## Phase 2: Foundational

**Purpose**: migration, problem types, domain rules, repositories and entry-point links used by every story

**CRITICAL**: no story starts before this phase is complete

- [x] T005 Create `backend/src/main/resources/db/migration/V4__import_preview.sql` exactly as in `specs/005-block-import/data-model.md` (`import_preview` table and `ix_import_preview_expires_at`)
- [x] T006 Extend `<tpkg>/persistence/MigrationTest.java`: V4 applied once; table and index exist; two-container structure comparison of 003 still passes
- [x] T007 [P] Add the six Problem Details types of `contracts/problem-details.md` to `<pkg>/api/ProblemType.java` (`csv-not-utf8` 400, `csv-invalid` 400, `csv-missing-column` 400, `minitest-only` 422, `preview-not-found` 404, `phase-switch-not-allowed` 409) with `problem.<slug>.title` / `.detail` in the three message bundles; add every `import.reason.<code>` key from that contract
- [x] T008 [P] Write domain tests first, then classes, in `<pkg>/domain/importing/` and `<tpkg>/domain/importing/` (plain Java; ArchUnit of 003 guards it):
  - `ImportColumns`: Appendix D required names
  - `ItemRowRules`: FR-052 plus model length caps (`system` 100, `edit_key` 500, `uc` 255, `field` 100, `origin`/`target` 5,000, usecases 40,000, versions 20) ? reason codes of the contract
  - `ImportClassifier`: included / ignored (`already-imported`) / error (`duplicate-in-file` and validation); first occurrence in the file wins
  - `BlockPlan`: split included rows by `block_size`; cover sizes for 1, 9, 10, 11, 38 and 205 items (SC-002)
  - `StudyPhaseGate`: staging always Minitest-only; production allows other systems only when `study_phase` is `collection`
- [x] T009 Create JdbcClient repositories in `<pkg>/persistence/`:
  - `ImportPreviewRepository`: insert, findValid(id, now), delete, deleteExpired(now)
  - `ItemRepository`: `existingKeys()` (set of (`system`,`edit_key`)), `insert` batch, `count`, `findByBatch`
  - `BlockRepository`: `maxNumber`, `insert`, `countByBatch`, `listByBatch`
  - `TaskRepository`: `maxQueuePosition`, `insertFree(blockId, role, queuePosition)`, `countByBlock`
  - `ImportBatchRepository`: `insert`, `findById`, `listNewestFirst`
  - Extend `ConfigRepository`: `blockSize`, `studyPhase`, `studyPhaseChangedAt`, `updateStudyPhase(collection, now)` under `lockForUpdate()`
  Integration test `<tpkg>/persistence/ImportRepositoriesTest.java` (Testcontainers): preview round-trip; insert one batch with 11 items ? 2 blocks (10+1) and 4 free tasks with contiguous `queue_position`
- [x] T010 Add administrator links `vd:batches`, `vd:request-preview` and `vd:study-phase` on `GET /api/v1` in `<pkg>/api/v1/EntryPointController.java` and document the relations in `LinkRelations` / `RelationController` (`contracts/import.md`)
- [x] T011 Run `.\mvnw.cmd -B verify` in `backend/`: every test of 004 still passes with V4 present

**Checkpoint**: foundation ready — user stories can start

---

## Phase 3: User Story 1 - See the preview of a CSV (Priority: P1) ?? MVP

**Goal**: administrator uploads a CSV and sees included / ignored / errors / planned blocks before anything is stored

**Independent Test**: upload `veredito_minitest_vazio.csv` (SC-005: zero row errors) and `minitest_duplicates.csv`; missing column and non-Minitest refusals

- [x] T012 [P] [US1] Create failing `<tpkg>/api/v1/ImportPreviewTest.java`: follow `vd:request-preview` as administrator; Minitest preview 201 with zero errors; missing column ? `csv-missing-column`; non-Minitest in pilot ? `minitest-only`; visitor 401 / participant 403
- [x] T013 [P] [US1] Extend `<tpkg>/api/LocalizedProblemDetailsTest.java` with the new types in the three locales
- [x] T014 [US1] Implement `<pkg>/importing/CsvImportReader.java` (Commons CSV RFC 4180, UTF-8, BOM strip) and `<pkg>/importing/PreviewService.java`: classify, plan blocks, persist `import_preview` (30 min expiry), delete expired on create; throw ProblemException for whole-file refusals
- [x] T015 [US1] Implement `<pkg>/api/v1/BatchPreviewController.java` and models: `POST /api/v1/batches/previews`, `GET /api/v1/batches/previews/{id}` per `contracts/import.md` (HAL-FORMS `vd:confirm` when `includedCount > 0`); OpenAPI complete
- [x] T016 [US1] Secure the new paths as administrator-only in `<pkg>/web/SecurityConfig.java` (same pattern as `/api/v1/administrator/**`)
- [x] T017 [US1] Front end: enable Import in `<app>/admin/admin.component.*`; add route `/admin/import` and `<app>/admin/import/` component (file input, counts, ignored/error tables with localized `reason`, extra columns, cancel); XLIFF units; `npm run i18n:check`
- [x] T018 [US1] Playwright e2e: administrator opens Import, uploads Minitest, sees zero errors and block sizes; cancel leaves no `import_batch`

**Checkpoint**: US1 testable alone (preview only)

---

## Phase 4: User Story 2 - Confirm and form the blocks (Priority: P1)

**Goal**: confirmation stores items, blocks and free queue tasks atomically

**Independent Test**: confirm 38 items ? 4 blocks (10,10,10,8) and 8 tasks; second confirm all ignored; existing data unchanged

- [x] T019 [P] [US2] Extend `ImportPreviewTest` / add `<tpkg>/api/v1/ImportBatchTest.java`: confirm Minitest ? 201 with `Location`, blocks and queue positions; read-back of every stored field equals the file (SC-003); double confirm ? 200 all ignored; concurrent confirms store items once
- [x] T020 [US2] Implement `<pkg>/importing/BatchImportService.java`: lock config; re-classify included JSON against DB; if `includedCount > 0` insert batch/blocks/items/tasks in one transaction and delete the preview; otherwise return the all-ignored summary without a new batch (`contracts/import.md`)
- [x] T021 [US2] Implement `POST /api/v1/batches` on `<pkg>/api/v1/BatchController.java` with OpenAPI; wire `_templates` from the preview
- [x] T022 [US2] Front end: Confirm button posts `{ previewId }`, shows summary (blocks, queue positions); disable confirm when `includedCount = 0`
- [x] T023 [US2] Playwright: confirm Minitest; DB has expected block sizes; confirm again ? all ignored

**Checkpoint**: US1 + US2 deliver a full import

---

## Phase 5: User Story 4 - End the pilot and open collection (Priority: P2)

**Goal**: production-only switch from `pilot` to `collection` on an empty study database; staging stays Minitest-only

**Independent Test**: `StudyPhaseTest` as in the spec Independent Test and quickstart section 6

- [x] T024 [P] [US4] Create failing `<tpkg>/api/v1/StudyPhaseTest.java`: GET phase; PUT refused with items present (`phase-switch-not-allowed`); after clearing `item`/`participant`/`response`, PUT `collection` succeeds; non-Minitest preview then accepted; with `staging` profile, PUT ? 404 and no switch control contract
- [x] T025 [US4] Implement `<pkg>/importing/StudyPhaseService.java` and `<pkg>/api/v1/StudyPhaseController.java` (`GET`/`PUT /api/v1/administrator/study-phase`) per `contracts/import.md`; `PreviewService` uses `StudyPhaseGate`
- [x] T026 [US4] Front end: phase banner and switch dialog on `/admin/import` when `switchAllowed`; hidden when staging / `minitestOnly` without switch; XLIFF units

**Checkpoint**: phase gate complete

---

## Phase 6: User Story 3 - Consult the imported batches (Priority: P3)

**Goal**: list of previous batches on the Import screen

**Independent Test**: import two batches; list shows newest first with file, systems, counts

- [x] T027 [P] [US3] Extend `ImportBatchTest`: `GET /api/v1/batches` newest first; `GET /api/v1/batches/{id}` summary with `systems`
- [x] T028 [US3] Implement list and get on `BatchController`; systems = distinct `item.system` of the batch
- [x] T029 [US3] Front end: batch list on `/admin/import` (date/time, file, systems, items, blocks)

**Checkpoint**: all stories independently useful

---

## Phase 7: Staging rehearsal and deployment

- [ ] T030 Run quickstart sections 2 to 5 against local staging; record any fix
- [ ] T031 [P] Create `<tpkg>/api/v1/ImportVolumeTest.java` for SC-001 (500 lines, preview+confirm &lt; 2 minutes)
- [x] T032 Push to `Veredito`; wait for `ci.yml`; deploy `deploy/0.5.0-1` after the researcher's approval; quickstart section 8 in production (pilot / Minitest-only import)

  **Record (2026-11-15):** `ci.yml` run [43735720800](https://github.com/diegoquirino/openscience/actions/runs/43735720800) green on `4187da8` (backend, frontend, secrets, image, e2e). Tag `deploy/0.5.0-1` on `4187da8`; deploy run [43737171761](https://github.com/diegoquirino/openscience/actions/runs/43737171761) succeeded. Revision `veredito-00008-wnk` takes 100% of the traffic. Production footer: `0.5.0+4187da8`.

---

## Phase 8: Polish & Cross-Cutting Concerns

- [ ] T033 [P] Run `scripts/_check_utf8.py` over the changed Veredito folders
- [ ] T034 [P] Confirm secret scanning still clean; no new secrets introduced
- [ ] T035 Mark spec 005 complete: `Status` in `spec.md`; ROADMAP line of 005; note in spec 002 that SC-003 was closed by SC-005 of 005; update 001 overview status to "002 to 005 complete; 007 next"; commit and push

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: no dependencies
- **Foundational (Phase 2)**: depends on Setup; blocks every story
- **US1 (Phase 3)**: depends on Phase 2 — MVP
- **US2 (Phase 4)**: depends on US1 (confirm uses preview id)
- **US4 (Phase 5)**: depends on Phase 2; uses PreviewService gate (integrate after US1 reader exists)
- **US3 (Phase 6)**: depends on US2 (needs confirmed batches)
- **Staging / deploy (Phase 7)**: depends on all stories
- **Polish (Phase 8)**: depends on Phase 7

### Story order versus priority

US1 and US2 are both P1: preview before confirm. US4 (P2) before US3 (P3) so the Import screen's phase banner lands before the batch list polish. US3 only needs confirmed batches.

### Parallel Opportunities

- T002, T003, T004 with T001
- T007 and T008 inside Phase 2
- T012 and T013; T019 and domain already done; T024 with late US1
- T033 and T034

---

## Parallel Example: Phase 2

```text
Task: "Problem types and import.reason.* bundles" (T007)
Task: "Domain importing rules and tests" (T008)
```

## Parallel Example: User Story 1

```text
Task: "ImportPreviewTest" (T012)
Task: "LocalizedProblemDetailsTest new types" (T013)
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Phases 1 and 2
2. Phase 3 (preview API + Import screen without confirm persistence)
3. **Stop and validate**: quickstart preview steps

### Incremental Delivery

1. Setup and foundation (V4, domain, repos, links)
2. US1: preview
3. US2: confirm and queue tasks
4. US4: study phase
5. US3: batch list
6. Staging, volume test, production tag `0.5.0-*`
7. Polish and status

---

## Notes

- V1 to V4 are immutable once deployed; later schema changes are V5+.
- No new Secret Manager secret.
- `AuthorizationTest` of 004 must keep denying every new Administration operation without an administrator session.
- No task calls models; the sequential execution rule of `ISSTA_2027` does not apply.
