# Feature Specification: Export the labels

**Feature Branch**: `009-exports`

**Created**: 2026-11-10

**Status**: Complete (2026-11-16; production `0.9.0+1621378`, tag `deploy/0.9.0-1`)

**Input**: User description: "The administrator downloads the final labels in three formats: the wide table (one row per item, with both raters), the pseudonymized long table (one row per response) and, per system, the rater1 and rater2 files that the issta_tcm package reads. Drafts are never exported." (translated from the original Portuguese)

**Parent spec**: [001-oracle-labeling](../001-oracle-labeling/spec.md), User Story 8, FR-057, FR-058, FR-060 and Appendix C · **Depends on**: 005 and 007

**Naming with the pipeline**: the empty import file is `veredito_<system>_vazio.csv` (spec 002). This spec also exports the filled counterpart `veredito_<system>_completo.csv` (FR-057 (d)), filtered by system, so both files sit side by side in `ISSTA_2027/src/inputs/oracle/`. The **canonical** way to feed the agreement / adjudication / final-oracle commands is to place `_completo` in the oracle folder and run `python -m issta_tcm oracle-from-veredito`, which overwrites `t1_<system>_rater1.csv` and `t1_<system>_rater2.csv` from it (contract: [contracts/oracle-from-veredito.md](contracts/oracle-from-veredito.md)). Veredito may still offer a direct download of the two rater files (US1) as a convenience.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Download the pipeline files (Priority: P1)

For each system, the administrator can download `t1_<system>_rater1.csv` and `t1_<system>_rater2.csv`, ready for the pipeline (same bytes that `oracle-from-veredito` would materialize from `_completo` when both roles are complete).

**Why this priority**: It is the format the pipeline already reads; the direct download avoids an extra step when the researcher stays inside Veredito.

**Independent Test**: Export Minitest with completed blocks and run `agreement` and `oracle-disagreements` from `issta_tcm` on the files; alternatively, export only `_completo`, run `oracle-from-veredito`, and check that the materialized rater files match a direct US1 export.

**Acceptance Scenarios**:

1. **Given** a system with completed blocks, **When** the administrator exports, **Then** they receive two CSV files with the columns `edit_key,uc,kind,field,origin,target,change_impact,maintenance_operation,comment`, one row per item of the system, in queue order.
2. **Given** an item whose role has not been completed yet, **When** exported, **Then** the row exists with `change_impact`, `maintenance_operation` and `comment` blank in the file of that role.
3. **Given** the exported files, **When** read by the pipeline, **Then** they work without manual editing.
4. **Given** responses saved in different UI languages, **When** the pipeline files are exported, **Then** they keep exactly the columns listed in scenario 1 (no language column is added), `change_impact` and `maintenance_operation` hold the English codes, and `comment` holds the text exactly as the participant typed it.

---

### User Story 2 - Download the wide table (Priority: P1)

The administrator downloads one row per item, with the data of both raters side by side (Appendix C).

**Why this priority**: It is the consolidated view requested by the researcher, with identification, for internal use.

**Independent Test**: Export with completed, in-progress and free blocks and check the columns.

**Acceptance Scenarios**:

1. **Given** completed blocks, **When** exported, **Then** the table has the Appendix C header, one row per item of all systems, in queue order. The header is:
   `system,edit_key,uc,kind,field,origin,target,rater_1_name,rater_1_email,rater_1_accepted_research_terms_lgpd,rater_1_professional_skill,rater_1_change_impact_choice,rater_1_maintenance_operation_choice,rater_1_comment,rater_1_response_language,rater_2_name,rater_2_email,rater_2_accepted_research_terms_lgpd,rater_2_professional_skill,rater_2_change_impact_choice,rater_2_maintenance_operation_choice,rater_2_comment,rater_2_response_language`
2. **Given** a completed role, **When** exported, **Then** the row brings the name, e-mail, consent form acceptance (version, language and timestamp, e.g. `v1 pt-BR 2026-11-20T14:03:00-03:00`), professional skill, chosen codes, comment and response language of the rater of that role.
3. **Given** a role that has not been completed, **When** exported, **Then** the fields of that rater are blank.
4. **Given** a participant who labeled in es-ES and wrote the comment in Spanish, **When** the administrator, using the en-US interface, exports, **Then** `rater_N_professional_skill`, `rater_N_change_impact_choice` and `rater_N_maintenance_operation_choice` hold the English codes, `rater_N_comment` holds the Spanish text unchanged and `rater_N_response_language` is `es-ES`.

---

### User Story 3 - Download the filled Veredito oracle per system (Priority: P1)

For each system, the administrator downloads `veredito_<system>_completo.csv`, the filled counterpart of `veredito_<system>_vazio.csv`.

**Why this priority**: It closes the loop with the pipeline folder: empty file in, labeled file out, same system filter, without PII.

**Independent Test**: Import Minitest from `_vazio`, complete both roles of every item, export `_completo`, and check that every Appendix D cell matches the import and that both raters' final codes are present.

**Acceptance Scenarios**:

1. **Given** a system with items, **When** the administrator exports that system's filled oracle, **Then** they receive `veredito_<system>_completo.csv` with one row per item of that system only, in queue order.
2. **Given** the file, **When** opened, **Then** the header is exactly: `system,edit_key,uc,kind,field,origin,target,origin_version,target_version,origin_usecase,target_usecase,rater_1_pseudonym,rater_1_change_impact,rater_1_maintenance_operation,rater_1_comment,rater_1_response_language,rater_2_pseudonym,rater_2_change_impact,rater_2_maintenance_operation,rater_2_comment,rater_2_response_language`.
3. **Given** a completed role, **When** exported, **Then** that role's columns hold the English codes, the comment as typed, the locale of the last save and the participant's pseudonym (`R-0001`); name and e-mail never appear.
4. **Given** a role not yet completed, **When** exported, **Then** that role's five columns are blank.
5. **Given** the export UI, **When** the administrator chooses a system, **Then** only that system's rows are in the file (same filter as the skill that generates `_vazio` for one subject).
6. **Given** `veredito_<system>_completo.csv` saved in the oracle folder with every role filled, **When** the researcher runs `python -m issta_tcm oracle-from-veredito` for that system, **Then** `t1_<system>_rater1.csv` and `t1_<system>_rater2.csv` are overwritten with the pipeline columns, and `oracle-disagreements` / `oracle-final` can run without manual editing of the rater sheets.
7. **Given** a `_completo` file with any blank `rater_N_change_impact` or `rater_N_maintenance_operation`, **When** `oracle-from-veredito` runs, **Then** it refuses to write the rater sheets, lists the incomplete `edit_key`s and exits with failure.

---

### User Story 4 - Download the pseudonymized long table (Priority: P2)

The administrator downloads one row per final response, with neither name nor e-mail, for analysis and for the replication package.

**Why this priority**: It allows computing agreement with varying raters (Fleiss' kappa or Krippendorff's alpha) and publishing the data without identifying anyone.

**Independent Test**: Export and check that no column brings name or e-mail.

**Acceptance Scenarios**:

1. **Given** final responses, **When** exported, **Then** the table brings, per response, in this order: system, edit_key, block, role, pseudonymous identifier (`R-0001`), professional skill, `participant_preferred_language`, `consent_language`, chosen codes, comment, `response_language`, and the block's start and submission timestamps.
2. **Given** the long table, **When** opened, **Then** it contains neither name nor e-mail in any column.
3. **Given** a participant whose preferred language is pt-BR, who accepted the consent form in pt-BR and answered one item of the block in en-US, **When** exported, **Then** that response's row has `participant_preferred_language` = `pt-BR`, `consent_language` = `pt-BR` and `response_language` = `en-US`.

---

### Edge Cases

- Drafts of in-progress blocks: they do not appear in any export.
- Text starting with `=`, `+`, `-` or `@`: in the wide and long tables, exported in a way that does not become a formula when opened in a spreadsheet; in the pipeline files, kept as is.
- Texts with commas, quotes or line breaks: they are quoted and preserved.
- System with no completed block: the pipeline files come out with all responses blank.
- Database with hundreds of items: the export finishes without exceeding the execution time limit.
- Participant who switched the UI language in the middle of a block: each response carries the language in use when it was saved, so the rows of the same block may have different `response_language` values.
- Administrator's UI language: it changes neither the headers nor the contents of any export; headers and codes are always the English identifiers.
- Comments, names and imported use cases in any language or with accented characters: exported exactly as captured, never translated or transliterated.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST export only final responses.
- **FR-002**: The system MUST export the wide table of Appendix C of spec 001.
- **FR-003**: The system MUST export the long table with a pseudonymous identifier.
- **FR-004**: The system MUST export, per system, `t1_<system>_rater1.csv` and `t1_<system>_rater2.csv` in the format of the `issta_tcm` package, in queue order.
- **FR-004b**: The system MUST export, per system, `veredito_<system>_completo.csv` with the Appendix D columns (exact stored values) plus, for each role, `rater_N_pseudonym`, `rater_N_change_impact`, `rater_N_maintenance_operation`, `rater_N_comment` and `rater_N_response_language` (blank when that role is not completed); the file MUST contain only rows of the chosen system.
- **FR-004c**: The `issta_tcm` command `oracle-from-veredito` MUST materialize `t1_<system>_rater1.csv` and `t1_<system>_rater2.csv` from `veredito_<system>_completo.csv` as specified in [contracts/oracle-from-veredito.md](contracts/oracle-from-veredito.md) (canonical path from the oracle folder into agreement / adjudication / final).
- **FR-005**: The long table, the pipeline files and `veredito_<system>_completo.csv` MUST NOT contain name or e-mail.
- **FR-006**: All CSV files MUST be UTF-8 and RFC 4180. The wide and long tables MUST neutralize texts starting with `=`, `+`, `-` or `@`; the pipeline files MUST NOT change any text, so that they stay identical to the pipeline's edits.
- **FR-007**: Only the administrator MUST be able to export.
- **FR-008**: Codes in every export (`professional_skill`, `change_impact`, `maintenance_operation`) MUST be the English identifiers stored in the database, never the translated display labels; column headers MUST be in English, whatever the administrator's UI language.
- **FR-009**: Captured data (comments, names, e-mails and the imported use cases and edits) MUST be exported exactly as captured, in the language in which it was captured, without translation.
- **FR-010**: The wide table MUST bring, for each rater, `rater_N_response_language` (the UI language in use when the response was saved) and, in `rater_N_accepted_research_terms_lgpd`, the version, language and timestamp of the accepted consent form (e.g. `v1 pt-BR 2026-11-20T14:03:00-03:00`).
- **FR-011**: The long table MUST bring `participant_preferred_language` and `consent_language` right after the professional skill (they describe the participant) and `response_language` right after the comment (it describes the response); values are the locale codes `pt-BR`, `en-US` or `es-ES`.
- **FR-012**: The pipeline files (`t1_<system>_rater1.csv`, `t1_<system>_rater2.csv`) MUST keep their columns unchanged; they MUST NOT receive any language column.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: The `t1_<system>_rater*.csv` files are read by the `agreement` and `oracle-disagreements` commands of `issta_tcm` without manual editing.
- **SC-001b**: For every Minitest item, the Appendix D cells of `veredito_minitest_completo.csv` match the imported `veredito_minitest_vazio.csv` character by character, and both rater columns are filled after both roles are completed.
- **SC-001c**: After `oracle-from-veredito` on a complete Minitest `_completo`, the materialized `t1_minitest_rater*.csv` files are byte-identical (apart from optional CRLF normalization already used by the pipeline) to a direct US1 export of the same database state, and `oracle-disagreements` followed by `oracle-final` succeeds when there are no disagreements (or after adjudication when there are).
- **SC-002**: Exporting a database of 500 items takes less than 1 minute.
- **SC-003**: The number of responses in the long table matches the number of completed roles multiplied by the size of each block.
- **SC-004**: 100% of the rows with a final response in the wide and long tables have a response language in {`pt-BR`, `en-US`, `es-ES`}, and 100% of the exported comments are identical to the saved ones (apart from the formula neutralization of FR-006 in the wide and long tables).

## Assumptions

- "Role 1" becomes `rater1` and "role 2" becomes `rater2`, even though people vary from block to block (see the note on agreement in spec 001).
- The pipeline files are not meant to be opened in a spreadsheet; whoever opens them should import them as text.
- `consent_language` is the language of the consent form acceptance in force when the participant submitted the block; `participant_preferred_language` is the participant's preferred language at export time.
- Recommended researcher loop for one system: generate `_vazio` (002) → import and label in Veredito (005–008) → download `_completo` into `inputs/oracle/` → `oracle-from-veredito` → `oracle-disagreements` → if the adjudication sheet is empty, `oracle-final`; otherwise fill the ties manually and then `oracle-final`. The `agreement` command remains optional diagnostics.
- `oracle-from-veredito` is implemented in the `ISSTA_2027` package (not in the Veredito web app); this spec defines its contract because `_completo` is produced here.
