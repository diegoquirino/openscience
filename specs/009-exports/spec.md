# Feature Specification: Export the labels

**Feature Branch**: `009-exports`

**Created**: 2026-11-10

**Status**: Draft

**Input**: User description: "The administrator downloads the final labels in three formats: the wide table (one row per item, with both raters), the pseudonymized long table (one row per response) and, per system, the rater1 and rater2 files that the issta_tcm package reads. Drafts are never exported." (translated from the original Portuguese)

**Parent spec**: [001-oracle-labeling](../001-oracle-labeling/spec.md), User Story 8, FR-057, FR-058, FR-060 and Appendix C · **Depends on**: 005 and 007

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Download the pipeline files (Priority: P1)

For each system, the administrator downloads `t1_<system>_rater1.csv` and `t1_<system>_rater2.csv`, ready for the pipeline.

**Why this priority**: It is the final product of the study and the format the pipeline already reads.

**Independent Test**: Export Minitest with completed blocks and run `agreement` and `oracle-disagreements` from `issta_tcm` on the files.

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

### User Story 3 - Download the pseudonymized long table (Priority: P2)

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
- **FR-005**: The long table and the pipeline files MUST NOT contain name or e-mail.
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
- **SC-002**: Exporting a database of 500 items takes less than 1 minute.
- **SC-003**: The number of responses in the long table matches the number of completed roles multiplied by the size of each block.
- **SC-004**: 100% of the rows with a final response in the wide and long tables have a response language in {`pt-BR`, `en-US`, `es-ES`}, and 100% of the exported comments are identical to the saved ones (apart from the formula neutralization of FR-006 in the wide and long tables).

## Assumptions

- "Role 1" becomes `rater1` and "role 2" becomes `rater2`, even though people vary from block to block (see the note on agreement in spec 001).
- The pipeline files are not meant to be opened in a spreadsheet; whoever opens them should import them as text.
- `consent_language` is the language of the consent form acceptance in force when the participant submitted the block; `participant_preferred_language` is the participant's preferred language at export time.
