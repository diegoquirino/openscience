# Research: Export the labels

**Feature**: `009-exports` | **Date**: 2026-11-15 | **Spec**: [spec.md](spec.md)

This research starts from User Story 8 / FR-057 of spec 001, the import CSV of 005, the final
responses of 007, and the pipeline contract [oracle-from-veredito.md](contracts/oracle-from-veredito.md).

## 1. Versions (checked on 2026-11-15)

| Component | Version | Note |
|---|---|---|
| Spring Boot | 4.1.2 | Unchanged |
| Apache Commons CSV | 1.14.1 | Already on the classpath from 005 |
| Angular / Material | 22.2.6 | Unchanged |
| App version | **0.9.0** | Spec number in the minor digit |
| Deploy tags | `deploy/0.9.0-*` | Production only with explicit approval |

No new Maven or npm dependency.

## 2. CSV writing

- **Decision**: write with `CSVFormat.RFC4180` (Commons CSV), UTF-8 **without BOM**, `\n` line
  endings. Headers and codes are always the English identifiers (FR-008).
- **Rationale**: same stack as import; RFC 4180 matches FR-006; no BOM keeps pipeline tools happy.
- **Alternatives considered**: OpenCSV (extra dependency); streaming `OutputStream` per row (kept
  as an option inside the service, but an in-memory `byte[]` / `String` buffer is enough for
  SC-002 at 500 items).

## 3. Formula neutralization (wide and long only)

- **Decision**: if a cell's text starts with `=`, `+`, `-` or `@`, prefix a single ASCII apostrophe
  `'`. Apply only to wide and long tables. Pipeline rater files and Appendix D columns of
  `_completo` are never neutralized (FR-006).
- **Rationale**: spreadsheet formula injection; pipeline must keep exact `origin`/`target` bytes.
- **Alternatives considered**: tab prefix (less visible in Excel); stripping the leading character
  (corrupts data).

## 4. Final-only rule

- **Decision**: a role contributes labels only when `task.status = completed` and the response for
  that `(task, item)` has `state = final`. Drafts never appear. Incomplete roles leave the role's
  label columns blank but the item row still appears (US1–US3).
- **Rationale**: FR-001; matches 007 submission semantics.
- **Long table**: one row per final response only (no blank role rows).

## 5. Queue order and system filter

- **Decision**: order by `block.number ASC`, `item.position ASC`. Wide/long cover all systems.
  Completo / rater1 / rater2 take a required `system` query parameter equal to `item.system`.
- **Unknown system**: `404` Problem Details type `system-not-found` (stable slug).
- **Empty system** (no items): still `200` with header-only CSV for that format (edge case).

## 6. HAL API shape

- **Decision**:
  - Entry point (admin): `vd:exports` ? `GET /api/v1/exports`.
  - Index returns `systems: string[]` (distinct `item.system` in queue order of first appearance)
    and links: `vd:export-wide`, `vd:export-long`, plus for each system
    `vd:export-completo`, `vd:export-rater1`, `vd:export-rater2` with concrete hrefs including
    `?system=…` (no client URL construction).
  - Downloads: `GET /api/v1/exports/wide|long` and
    `GET /api/v1/exports/completo|rater1|rater2?system=` producing
    `text/csv;charset=UTF-8` with `Content-Disposition: attachment; filename="…"`.
- **Rationale**: REST level 3; mirrors 001 research `GET /api/v1/exports/{type}`; per-system
  concrete links avoid templates on the Angular side.
- **Alternatives considered**: single zip of all files (spec asks for separate downloads);
  HAL-FORMS POST (unnecessary for idempotent reads).

## 7. Long-table header

- **Decision** (locked English snake_case):

```text
system,edit_key,block,role,pseudonym,professional_skill,participant_preferred_language,consent_language,change_impact,maintenance_operation,comment,response_language,reserved_at,submitted_at
```

  `block` = `block.number`; `role` = `1`|`2`; timestamps ISO-8601 with offset from
  `task.reserved_at` / `task.submitted_at`.

## 8. Wide consent and anonymized cells

- **Decision**: `rater_N_accepted_research_terms_lgpd` =
  `{consent_form.version} {participant.consent_language} {participant.consent_decided_at}` when the
  role is completed and consent was accepted; otherwise blank. When `participant.anonymized_at` is
  set, `rater_N_name` and `rater_N_email` are the glossary marker `anonymized` (not blank).
- **Decision**: `rater_N_professional_skill` / long `professional_skill` export the English **code**
  only (`other` stays `other`; free text in `professional_skill_other` is not a column in Appendix C).

## 9. Filenames

| Format | Filename |
|---|---|
| Wide | `veredito_wide.csv` |
| Long | `veredito_long.csv` |
| Completo | `veredito_<system>_completo.csv` |
| Rater 1 / 2 | `t1_<system>_rater1.csv` / `t1_<system>_rater2.csv` |

## 10. `oracle-from-veredito` (ISSTA_2027)

- **Decision**: implement in `ISSTA_2027/src/issta_tcm` next to the other `oracle-*` commands,
  registered in `cli.py`, behaviour exactly as
  [contracts/oracle-from-veredito.md](contracts/oracle-from-veredito.md). Unit tests cover success,
  incomplete roles, bad header, wrong system, invalid codes.
- **Rationale**: FR-004c and SC-001c live in the pipeline package, not in the web app.
- **Note**: the command was documented in the README but missing from the CLI before this spec.

## 11. Front end

- **Decision**: enable Admin **Export** ? `/admin/export`. Screen loads the exports index via HAL,
  lists systems, offers downloads (blob + filename from `Content-Disposition`). UI strings in XLIFF;
  CSV content never localized.
- **Rationale**: same HAL-first pattern as import (005).

## 12. Security and docs

- **Decision**: add `/api/v1/exports/**` to `SecurityConfig.ADMINISTRATION`. Document every
  operation under OpenAPI tag Administration with `text/csv` examples so
  `ApiDocumentationCompletenessTest` passes. Entry point must not accept `Accept: text/csv`
  (existing 406 behaviour stays).
