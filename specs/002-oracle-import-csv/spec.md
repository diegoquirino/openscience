# Feature Specification: Generate the import CSV from the pipeline

**Feature Branch**: `002-oracle-import-csv`

**Created**: 2026-11-10

**Status**: Complete (2026-11-11; SC-003 is checked in spec 005, SC-005)

**Input**: User description: "Generate, from the ISSTA 2027 pipeline data, the CSV file that Veredito imports (Appendix D of spec 001), one file per system, with the complete use case of each version and the edited lines marked." (translated from the original Portuguese)

**Overall spec**: [001-oracle-labeling](../001-oracle-labeling/spec.md) | **Depends on**: nothing

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Generate the import file of a system (Priority: P1)

The researcher runs a command for a system already processed by the pipeline (for example, `minitest`) and gets a CSV with one row per edit, ready to be imported into Veredito.

**Why this priority**: It is the contract between the pipeline and Veredito. With a real file at hand from the start, the import (005) and the item screen (008) are tested with real long texts, quotes and line breaks.

**Independent Test**: Generate the Minitest file and check it, row by row, against `edits.csv` and the system's labeling sheet.

**Acceptance Scenarios**:

1. **Given** a system whose edits were already computed by the pipeline, **When** the researcher generates the file, **Then** they get a CSV with one row per edit, in the same order as the pipeline edits (38 rows for Minitest), with the columns of Appendix D of spec 001, in that order.
2. **Given** the generated file, **When** the researcher compares a row with the original edit, **Then** `edit_key`, `uc`, `kind`, `field`, `origin` and `target` are identical to the pipeline's, character by character, and `system` holds the system identifier.
3. **Given** an edit, **When** the file is generated, **Then** `origin_usecase` holds the whole use case in the previous version and `target_usecase` in the modified version, in the same format as the labeling sheet, with `<- edited` only on the lines touched by that edit.
4. **Given** an edit between two versions of the system, **When** the file is generated, **Then** `origin_version` and `target_version` hold the labels of those versions in the pipeline configuration (for example, `v1.1` and `v1.2`), and not the version declared inside the use case, which may lag behind the system version.
5. **Given** a use case created in the modified version (it did not exist in the previous one), **When** the file is generated, **Then** `origin_usecase` holds the pipeline's short notice that the use case did not exist in that version, kept as captured; the reverse holds for a removed use case.
6. **Given** the same system, **When** the file is generated twice, **Then** the two files are identical.

---

### User Story 2 - Check the file before importing (Priority: P2)

When generating the file, the researcher sees a summary and is warned about any row that Veredito would refuse.

**Why this priority**: It avoids finding problems only in the import preview, with the data collection already under way.

**Independent Test**: Generate the file of a system with an artificially long edit and check the error message.

**Acceptance Scenarios**:

1. **Given** the generated file, **When** the command finishes, **Then** it shows the total number of rows, the count per `kind` and per `field` and the longest length of `origin`, `target` and the complete use case.
2. **Given** a row that violates any import validation rule (FR-052 of spec 001), **When** the file is generated, **Then** the command lists the row and the reason and ends indicating failure.
3. **Given** a row in which neither use case has a line marked as edited, **When** the file is generated, **Then** the command warns, without failing.

---

### Edge Cases

- `META` edit of `requirements`: the marked line is the requirements link line.
- `META` edit that adds or removes a metadata value (in Minitest, `LoginTest|3|1` goes from no requirement to `REQ-07`): one of `origin` and `target` is empty, and the row is valid.
- Use case that declares a version different from the system version (in Minitest, versions 1.1 and 1.2 still declare 1.0): the system version label prevails.
- Text with commas, quotes or line breaks: it is enclosed in quotes, and inner quotes are doubled.
- System without any edit: the file has the header only and the command warns.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The command MUST generate one CSV per system, with one row per edit, in the order of the pipeline edits.
- **FR-002**: The CSV MUST have exactly the columns of Appendix D of spec 001, in that order, in UTF-8 and following RFC 4180.
- **FR-003**: `edit_key`, `uc`, `kind`, `field`, `origin` and `target` MUST be copied unchanged.
- **FR-004**: The complete use cases MUST follow the format of the labeling sheet, marking with `<- edited` only the lines touched by the edit of the row.
- **FR-005**: When the use case does not exist in one of the versions, the text of that version MUST be the pipeline's short notice `(the use case does not exist in this version)`, copied as captured (data is never translated) and never empty; the Veredito item screen (spec 008) shows a localized message instead.
- **FR-006**: The command MUST validate each row with the Veredito import rules and fail if any is violated, listing row and reason.
- **FR-007**: The generation MUST be deterministic.
- **FR-008**: The file MUST NOT contain rater labels, technique outputs or blind identifiers.

### Key Entities

- **Edit**: oracle unit produced by the pipeline; becomes an item in Veredito.
- **Versioned use case**: complete text of a use case in one version of the system.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: The Minitest file is generated in less than 1 minute.
- **SC-002**: 100% of the Minitest rows have at least one line marked as edited in one of the two use cases.
- **SC-003**: The Minitest file goes through the Veredito import preview (spec 005) without any row in error. Checked in spec 005 (its SC-005); it does not block the start of 003.

## Assumptions

- The command is part of the ISSTA 2027 pipeline and uses the same per-system configurations; the file is written to the oracle folder, next to the `t1_*` files.
- The system identifier is the same one used in the names `t1_<system>_*.csv`.
- Repeating the complete use case in each row is acceptable for a few hundred edits per system.
- Values are copied in the language in which they were captured (the Minitest use cases are in English).
