# Research: Import items by CSV and form the queue blocks

**Feature**: `005-block-import` | **Date**: 2026-11-14 | **Spec**: [spec.md](spec.md)

This research starts from the technology selection of spec 001
([research.md](../001-oracle-labeling/research.md)), the schema of spec 003 and the access layer of
spec 004. It only records what spec 005 decides on top of them.

## 1. Versions (checked on 2026-11-14)

| Component | Version | Note |
|---|---|---|
| Spring Boot | 4.1.2 | Unchanged; 4.2.x still not adopted |
| Apache Commons CSV | 1.14.1 | New; RFC 4180 parser (Maven Central latest) |
| Angular / Material / localize | 22.2.6 | Unchanged |
| Playwright | 1.65.0 | Unchanged |
| MySQL / Testcontainers / Flyway | as in 004 | Unchanged |

No other new dependency. Multipart upload uses Spring's `MultipartFile` (already on the
`spring-boot-starter-webmvc` classpath). Preview payloads are stored in MySQL, so Cloud Run
instances share them without Redis or another Google service.

## 2. CSV library

- **Decision**: Apache Commons CSV 1.14.1 with `CSVFormat.RFC4180`, UTF-8 `InputStreamReader`, and
  `CSVParser` that keeps header names. The first record is the header; required Appendix D columns
  must all be present; extra columns are collected by name and listed in the preview (US1 scenario
  8). BOM is stripped once at the start of the stream. Empty `origin`/`target` cells become `null`
  (not `""`) so the FR-052 emptiness rules stay simple.
- **Rationale**: Principle I (standard open-source piece); the constitution puts CSV validation in
  the domain; Commons CSV is the usual RFC 4180 choice on the JVM and handles quoted commas,
  doubled quotes and embedded line breaks (US1 scenario 3).
- **Alternatives considered**: OpenCSV (similar, less clearly RFC-labelled); a hand-written parser
  (rejects the constitution's "no reinvention" rule for a format with line breaks).

## 3. Preview then confirm (API shape)

- **Decision**: follow the critical operations already recorded in spec 001 research:
  - `POST /api/v1/batches/previews` (`vd:request-preview` from the entry point / `vd:batches`):
    multipart field `file`; returns `201` with `Location` of the preview and the counts, row lists
    and planned block sizes.
  - `POST /api/v1/batches` with body `{ "previewId": <id> }` (`vd:confirm` on the preview): one
    transaction that re-checks uniqueness against the database, inserts the batch, blocks, items
    and free tasks, and returns `201` with the batch summary.
  - `GET /api/v1/batches` (`vd:batches`): list of confirmed batches, newest first (US3).
  - `GET /api/v1/batches/{id}`: one batch summary (systems derived from its items).
- **Rationale**: two steps match FR-004; storing the preview server-side avoids a second upload and
  works across the four Cloud Run instances; confirming by id matches the HAL-FORMS pattern of 004.
- **Alternatives considered**: client re-uploads the file on confirm (simpler storage, worse UX and
  a race on a changed file); keeping the preview only in the browser (cannot guarantee atomicity
  or identical re-parse).

## 4. Preview storage and lifetime

- **Decision**: migration `V4__import_preview.sql` adds table `import_preview` (see
  [data-model.md](data-model.md)): file name, counts, JSON for ignored/error rows and extra column
  names, and a `MEDIUMTEXT` JSON array of the rows that would be inserted (exact strings from the
  file). Previews expire after 30 minutes (`expires_at`). A confirm of an expired or unknown
  preview answers `404` `preview-not-found`. Expired rows are deleted when a new preview is created
  and at the start of confirm (no scheduled job).
- **Rationale**: the included-row payload for Minitest is on the order of the CSV itself (~135 KB);
  500 lines (SC-001) stay well under `MEDIUMTEXT`. Sharing through MySQL keeps the single-artifact
  rule (Principle V).
- **Alternatives considered**: `BLOB` of the raw CSV (re-parse on confirm; risk of drift); in-memory
  cache (wrong with several Cloud Run instances).

## 5. Row validation and splitting (domain)

- **Decision**: plain-Java package `domain.importing` (ArchUnit-guarded like `domain.access`):
  - `ImportColumns`: the Appendix D header names and the required set.
  - `ItemRowRules`: FR-052 plus the length caps of the 003 data model (`system` 100, `edit_key`
    500, `uc` 255, `field` 100) reported as row errors, never as insert failures.
  - `ImportClassifier`: given the parsed rows and the set of (`system`, `edit_key`) already in the
    database, classifies each data line as included, ignored (already in DB) or error (validation
    or duplicate within the file); the first occurrence in the file wins (FR-053).
  - `BlockPlan`: splits the included list into chunks of `config.block_size` (default 10); returns
    the sizes (SC-002 cases 1, 9, 10, 11, 38, 205).
  - `StudyPhaseGate`: whether a non-`minitest` `system` is allowed (`staging` profile always no;
    production only when `study_phase = collection`).
- **Rationale**: Principle II (rules in plain Java with unit tests in the same commit).
- **Alternatives considered**: validating only in SQL `CHECK` (cannot produce per-line preview
  reasons); validating in the controller (would break ArchUnit and the constitution).

## 6. Confirmation transaction and double submit

- **Decision**: one `@Transactional` method locks `config` (`SELECT ... FOR UPDATE`), reads
  `max(block.number)` and `max(task.queue_position)` (or 0), re-runs the uniqueness filter against
  the current `item` table, inserts `import_batch`, then for each planned block inserts `block`,
  its `item` rows and two `task` rows (`role` 1 then 2, `status` `free`, consecutive
  `queue_position`). Existing rows never update (FR-007, FR-055). Two concurrent confirms of the
  same content: the unique key on (`system`, `edit_key`) plus the re-check means the second
  transaction stores nothing new and answers a summary with every row ignored (US2 scenario 5).
- **Rationale**: matches spec 001 research ("one transaction per confirmation; the next queue
  position is read with a lock").
- **Alternatives considered**: application-level advisory locks only (weaker than the unique key);
  inserting tasks before items (FK order would break).

## 7. Study phase switch

- **Decision**: resource `GET`/`PUT /api/v1/administrator/study-phase` (`vd:study-phase`),
  administrator only. `GET` returns `{ "phase", "changedAt", "switchAllowed" }` and, when the
  phase is `pilot`, a HAL-FORMS `PUT` template only if the database has zero rows in `item`,
  `participant` and `response` and the active profile is not `staging`. `PUT` body
  `{ "phase": "collection" }` only; any other value is `400`. Refusal when not empty:
  `409` `phase-switch-not-allowed`. In the `staging` profile the link and template are absent and
  a direct `PUT` answers `404` (US4 scenario 5). There is no action to go back to `pilot`.
- **Rationale**: FR-011 and FR-012; recreating the Cloud SQL database is an operations step outside
  the app (ROADMAP pilot), so the UI only flips the flag on an empty database.
- **Alternatives considered**: a CLI flag (not available to the administrator in production); allowing
  the switch with leftover `access_code` / `access_session` rows (accepted: those tables are not in
  FR-012's emptiness check).

## 8. Multipart limits and time

- **Decision**: `spring.servlet.multipart.max-file-size` and `max-request-size` = `10MB` (enough for
  SC-001's 500-line file and for a full real-system CSV later; Minitest is ~135 KB). Confirm of 500
  included rows must finish under 2 minutes (SC-001); the measured path is a single JDBC batch-friendly
  loop inside one transaction. Cloud Run's request timeout stays at the platform default (? 2 min).
- **Rationale**: Principle V (API under 2 s for interactive calls; SC-001 is an explicit longer
  budget for the whole preview+confirm of 500 lines).
- **Alternatives considered**: streaming to Cloud Storage (new service, rejected by Principle V).

## 9. Localization of preview messages

- **Decision**: row reason codes are stable English slugs (`empty-system`, `invalid-kind`,
  `duplicate-in-file`, `already-imported`, …). The API returns each error/ignored row with
  `line`, `code` and, for display, a localized `reason` from MessageSource
  (`import.reason.<code>`). The Angular screen shows the localized `reason` and never translates
  file values (FR-013, FR-014, SC-004). Missing column, not UTF-8, not CSV and study-phase refusal
  are Problem Details types (see [contracts/problem-details.md](contracts/problem-details.md)).
- **Rationale**: same pattern as 004 (`type` + client text / `detail` arguments).
- **Alternatives considered**: free-text reasons only in the administrator's language (harder to
  test SC-004 across locales).

## 10. Fixtures

- **Decision**: keep `backend/src/test/resources/minitest/veredito_minitest.csv` as the reference
  (from spec 002). Add derived fixtures beside it: `minitest_duplicates.csv`,
  `minitest_missing_kind_column.csv`, `minitest_long_text.csv`, and a small non-minitest sample for
  the phase-gate tests. Volume for SC-001 is generated in the test (repeat Minitest rows with a
  suffix on `edit_key`), not stored as a 500-line file in git.
- **Rationale**: constitution Principle IV (staging and tests only with Minitest and derivatives).

## 11. Front end

- **Decision**: enable the "Import" item in the administrator menu; route `/admin/import` with
  Material file input, preview tables (included / ignored / errors / planned blocks), confirm and
  cancel, batch list, and the study-phase panel (hidden in staging). Cancel only drops the local
  preview view (and may `DELETE` the preview resource if exposed); nothing is stored until confirm
  (US1 scenario 7).
- **Rationale**: US1 to US4; Material only (Principle V).

## 12. Version bump

- **Decision**: application version `0.5.0` (pom and `package.json`); production deploy tags
  `deploy/0.5.0-*`.
- **Rationale**: same pattern as 003 ? 0.3.0 and 004 ? 0.4.0.
