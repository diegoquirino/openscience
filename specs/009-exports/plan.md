# Implementation Plan: Export the labels

**Branch**: `009-exports` | **Date**: 2026-11-15 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/009-exports/spec.md`

## Summary

Spec 009 turns the administrator's **Export** menu into working CSV downloads on top of the final
responses of 007 and the items of 005, and implements `oracle-from-veredito` in `ISSTA_2027`.

- **Wide / long:** all systems; formula neutralization; long is pseudonymized.
- **Completo / rater1 / rater2:** filtered by `system`; no PII on completo/rater; rater matches the
  pipeline columns; `_completo` is the filled counterpart of `_vazio`.
- **HAL:** `vd:exports` on the entry point; index lists systems and concrete download links.
- **Pipeline:** `python -m issta_tcm oracle-from-veredito` materializes rater sheets from `_completo`
  ([contracts/oracle-from-veredito.md](contracts/oracle-from-veredito.md)).
- **UI:** `/admin/export`; Export menu item enabled.

No Flyway migration. Decisions are in [research.md](research.md).

## Technical Context

**Language/Version**: Java 25 LTS with Spring Boot 4.1.2; TypeScript with Angular 22.2.6; Node.js
26 LTS for the build; Python 3 for `issta_tcm`

**App version**: **0.9.0**. Deploy tags: `deploy/0.9.0-*`.

**Primary Dependencies**:
- **Back end:** as in 008, plus Commons CSV 1.14.1 (already present).
- **Front end:** as in 008 (Angular Material, `@angular/localize`).
- **Pipeline:** existing `issta_tcm` package in `ISSTA_2027/src`.

**Storage**: MySQL 9.7; read-only over existing tables ([data-model.md](data-model.md))

**Testing**:
- **Domain:** JUnit 5 for neutralization, headers and row mapping.
- **API:** REST Assured `ExportTest`; `AuthorizationTest` / `EntryPointTest` updated.
- **Front end:** Vitest for the export screen; Playwright admin download smoke.
- **Pipeline:** unit/CLI tests for `oracle-from-veredito` (success, incomplete, bad header).

**Target Platform**: Cloud Run + Cloud SQL; Docker Compose `staging`; GitHub-hosted Ubuntu runners

**Project Type**: web application + sibling researcher CLI

**Performance Goals**: export of 500 items under 1 minute (SC-002); interactive API under 2 s

**Constraints**:
- Admin only (FR-007).
- Final responses only (FR-001).
- No DDL; rollback to 0.8.0 still starts.
- Deploy by tag only with explicit researcher approval.

**Scale/Scope**:
- One administrator; hundreds to a few thousand items.
- One new screen; five download operations + index.
- One new Problem Details type (`system-not-found`).

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

Constitution 2.7.1.

| Rule | How the plan meets it | Status |
|---|---|---|
| I. Stack standard; versions recorded | 0.9.0; Commons CSV already checked in 005 | Pass |
| I. MessageSource | `system-not-found` and any UI-adjacent messages in three bundles | Pass |
| I. REST level 3 | `vd:exports` and download relations; client follows links ([contracts/exports.md](contracts/exports.md)) | Pass |
| I. OpenAPI complete | Every export operation under Administration with `text/csv` examples | Pass |
| II. Domain plain Java | `domain.exporting` for neutralization and CSV row assembly | Pass |
| II. MySQL + REST Assured from `/api/v1` | `ExportTest` follows `vd:exports` | Pass |
| II. i18n completeness | New XLIFF units; headers/codes stay English | Pass |
| III. Transactions; Flyway only | Read-only; no migration | Pass |
| IV. Staging/tests/pilot Minitest | Fixtures reuse Minitest import | Pass |
| V. Single artifact; Material | `/admin/export`; CSV downloads | Pass |
| Language / data untranslated | FR-008/FR-009; research 񗚺 | Pass |
| Workflow: staging then tag deploy | Quickstart; `deploy/0.9.0-*` with approval | Pass |
| UTF-8 | As in 003�008 | Pass |

No violation.

**Post-design re-check**: no new secrets; Administration matcher includes `/api/v1/exports/**`.

## Risks

| Risk | Mitigation |
|---|---|
| Large CSV memory | Buffer acceptable for SC-002; stream later if needed |
| Formula neutralization corrupting pipeline | Applied only to wide/long |
| `oracle-from-veredito` missing until now | Implemented and tested in this spec |
| SC-001c byte parity | Shared column order; CRLF normalization allowed by contract |

## Project Structure

### Documentation (this feature)

```text
specs/009-exports/
??? plan.md
??? research.md
??? data-model.md
??? quickstart.md
??? contracts/
?   ??? exports.md
?   ??? web.md
?   ??? oracle-from-veredito.md
??? tasks.md
```

### Source Code (`Veredito` branch)

```text
backend/src/main/java/.../
??? domain/exporting/          # FormulaNeutralizer, headers, row builders
??? exporting/                 # ExportQuery, ExportService, CsvDownload
??? api/v1/ExportController.java

frontend/src/app/admin/export/ # export.component + service
```

### Pipeline (`ISSTA_2027/src`)

```text
issta_tcm/cli.py                         # register oracle-from-veredito
issta_tcm/evaluation/oracle_from_veredito.py
tests/...                                # CLI / unit coverage
```

## Complexity Tracking

None (no constitution violations).
