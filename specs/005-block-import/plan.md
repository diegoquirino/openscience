# Implementation Plan: Import items by CSV and form the queue blocks

**Branch**: `005-block-import` | **Date**: 2026-11-14 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/005-block-import/spec.md`

## Summary

Spec 005 turns the administrator's **Import** menu into a working CSV import on top of the access
layer of 004 and the schema of 003.

- **Preview:** the administrator uploads a UTF-8 RFC 4180 CSV (Appendix D); the server classifies
  each row (included / ignored / error), plans blocks of `config.block_size`, and stores a short-
  lived preview in MySQL.
- **Confirm:** one transaction appends an `import_batch`, immutable `block`/`item` rows and free
  `task` rows (roles 1 and 2) at the end of the queue. Existing data never changes.
- **Gates:** local staging and production `pilot` accept only `system=minitest`; switching to
  `collection` is a production-only action on an empty study database.
- **UI:** `/admin/import` with Material upload, localized preview, batch list and phase banner.

The only schema addition is `V4__import_preview.sql`. Decisions are in [research.md](research.md).

## Technical Context

**Language/Version**: Java 25 LTS with Spring Boot 4.1.2; TypeScript with Angular 22.2.6; Node.js
26 LTS for the build

**Primary Dependencies**:
- **Back end:** as in 004, plus Apache Commons CSV 1.14.1.
- **Front end:** as in 004 (Angular Material, `@angular/localize`).

**Storage**: MySQL 9.7; existing `import_batch` / `block` / `item` / `task` / `config`; new table
`import_preview` ([data-model.md](data-model.md))

**Testing**:
- **Domain:** JUnit 5 for row rules, classifier and block plan.
- **API:** REST Assured `ImportTest`, `StudyPhaseTest`, `ImportVolumeTest`; fixtures under
  `backend/src/test/resources/minitest/`; `LocalizedProblemDetailsTest` gains the new types.
- **Front end:** Vitest for the import screen; Playwright for upload → preview → confirm in staging.

**Target Platform**: as in 003/004 (Cloud Run + Cloud SQL; Docker Compose `staging`; GitHub-hosted
Ubuntu runners)

**Project Type**: web application (one container: Spring Boot API + three Angular builds)

**Performance Goals**:
- Interactive API calls under 2 s (Principle V).
- Preview + confirm of 500 lines under 2 minutes (SC-001).

**Constraints**:
- Staging / `pilot`: Minitest only (FR-011).
- Phase switch irreversible through the UI (FR-012).
- Imported values never translated (FR-014).
- V4 additive so a Cloud Run rollback to 004 still starts.

**Scale/Scope**:
- One administrator; hundreds to a few thousand items per batch.
- 6 new API operations (preview create/get, batch create/list/get, study-phase get/put).
- One new screen (`/admin/import`); Import menu item enabled.
- 6 new Problem Details types; a set of row-reason message keys.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

Constitution 2.7.1.

| Rule | How the plan meets it | Status |
|---|---|---|
| I. Stack standard and current; versions recorded | Commons CSV 1.14.1 checked on 2026-11-14 (research §1–2); no new Google service | Pass |
| I. MessageSource for backend messages | New Problem Details types and `import.reason.*` in the three bundles | Pass |
| I. REST level 3 | `/batches/previews`, `/batches`, `/administrator/study-phase`; links `vd:batches`, `vd:request-preview`, `vd:confirm`, `vd:study-phase`; HAL-FORMS on preview and study-phase ([contracts/import.md](contracts/import.md)) | Pass |
| I. OpenAPI complete; Swagger admin-only in production | Every new operation documented under tag Administration | Pass |
| II. Domain in plain Java with unit tests | `domain.importing` for columns, FR-052, classifier, block plan, phase gate | Pass |
| II. MySQL + REST Assured from `/api/v1` following links | Tests start at the entry point and follow `vd:batches` / `vd:request-preview` / `vd:confirm` | Pass |
| II. i18n completeness | New XLIFF units; bundle parity; localized Problem Details and preview reasons (SC-004) | Pass |
| III. Transactions; Flyway only | Confirm is one transaction with locks; `V4` additive only | Pass |
| IV. Staging/tests/pilot only Minitest | `StudyPhaseGate` + fixtures; non-Minitest sample only for refusal tests | Pass |
| V. Single artifact; Material; API under 2 s | Multipart to MySQL preview; Material import screen; SC-001 separate budget | Pass |
| Language: English in repo; UI three locales; data untranslated | FR-013/FR-014; research §9 | Pass |
| Workflow: staging before production; deploy by tag | Quickstart §2–7 then §8 with `deploy/0.5.0-*` | Pass |
| All text files UTF-8 | As in 003/004 | Pass |

No violation.

**Post-design re-check** (after data-model and contracts):
- **Services and secrets:** none added.
- **Coverage:** every new Administration operation is caught by `AuthorizationTest`; SC-005 closes
  SC-003 of spec 002; emptiness rules for the phase switch match FR-012.

Pass.

## Risks

| Risk | Mitigation |
|---|---|
| Preview JSON large for huge CSVs | 10 MB multipart cap; Minitest ~135 KB; SC-001 at 500 lines |
| Double confirm race | Unique (`system`,`edit_key`) + re-check inside the locked transaction |
| Operator switches to `collection` with leftover pilot data | Emptiness check; ROADMAP still requires recreating the Cloud SQL database before the real call |

## Project Structure

### Documentation (this feature)

```text
specs/005-block-import/
|-- plan.md
|-- research.md
|-- data-model.md
|-- quickstart.md
|-- contracts/
|   |-- import.md
|   |-- problem-details.md
|   |-- web.md
|   `-- pipeline.md
|-- checklists/requirements.md
`-- tasks.md                 # phase 2 (/speckit-tasks; not created here)
```

### Source Code (root of the `Veredito` branch; new or changed in 005)

```text
backend/
|-- pom.xml                              # version 0.5.0; commons-csv 1.14.1
`-- src/
    |-- main/
    |   |-- java/io/github/diegoquirino/veredito/
    |   |   |-- domain/importing/        # ImportColumns, ItemRowRules, ImportClassifier, BlockPlan, StudyPhaseGate
    |   |   |-- importing/               # PreviewService, BatchImportService, CsvImportReader, StudyPhaseService
    |   |   |-- persistence/             # ImportPreviewRepository, ImportBatchRepository, ItemRepository,
    |   |   |                            # BlockRepository, TaskRepository (JdbcClient); ConfigRepository gains phase helpers
    |   |   |-- api/                     # ProblemType gains the new slugs
    |   |   |-- api/v1/                  # BatchPreviewController, BatchController, StudyPhaseController + models;
    |   |   |                            # EntryPointController gains administrator links; LinkRelations
    |   |   `-- config/                  # multipart limits
    |   `-- resources/
    |       |-- db/migration/V4__import_preview.sql
    |       |-- messages*.properties     # problem.* and import.reason.*
    |       `-- application.yaml         # multipart 10MB
    `-- test/
        |-- java/.../domain/importing/
        |-- java/.../api/v1/ImportTest.java, StudyPhaseTest.java, ImportVolumeTest.java
        `-- resources/minitest/          # fixture + derivatives

frontend/src/app/
|-- admin/                               # Import enabled; admin-import route/component
frontend/src/locale/messages.pt-BR.xlf, messages.es-ES.xlf
frontend/e2e/                            # import happy path in staging
```

**Structure Decision**: same two-folder web application. Package `importing` coordinates CSV
reading, domain classification and persistence; `domain.importing` stays free of Spring.

## Complexity Tracking

No violations.
