# Feature Specification: Import items by CSV and form the queue blocks

**Feature Branch**: `005-block-import`

**Created**: 2026-11-10

**Status**: Draft

**Input**: User description: "The administrator uploads a CSV with the columns of Appendix D of spec 001, checks a preview and confirms. On confirmation, the new items are split into fixed blocks of 10 and each block enters the end of the queue with the roles of rater 1 and rater 2."

**Overall spec**: [001-oracle-labeling](../001-oracle-labeling/spec.md), User Story 7, FR-011, FR-012 and FR-051 to FR-055 · **Depends on**: 004 · **Test data**: the file of spec 002 (`veredito_minitest.csv`)

## User Scenarios & Testing *(mandatory)*

### User Story 1 - See the preview of a CSV (Priority: P1)

The administrator uploads a file and sees, before anything is stored, what goes in, what is ignored and what has errors.

**Why this priority**: Errors in the item base contaminate the oracle; the preview is the barrier.

**Independent Test**: Upload the Minitest file (`veredito_minitest.csv`) and a file with duplicates and errors (`minitest_duplicates.csv`), and check the counts and the reasons.

**Acceptance Scenarios**:

1. **Given** a UTF-8 CSV with all the columns of Appendix D, **When** the administrator uploads it, **Then** they see how many items will be included, how many ignored and how many with errors (each one with line number and reason) and how many blocks will be created, with the size of each one.
2. **Given** a CSV missing a required column (for example, `minitest_missing_kind_column.csv`), **When** it is uploaded, **Then** it is refused entirely, indicating the missing column.
3. **Given** quoted fields with commas, doubled quotes or line breaks, **When** the file is read, **Then** the texts are preserved exactly.
4. **Given** a row whose (`system`, `edit_key`) already exists in the database, **When** the preview is generated, **Then** the row appears as ignored.
5. **Given** the same (`system`, `edit_key`) repeated in the file, **When** the preview is generated, **Then** only the first occurrence goes in and the others appear as errors.
6. **Given** a row with an empty required field, an invalid `kind`, empty `origin` in `MOD` or `DEL`, empty `target` in `MOD` or `ADD`, both `origin` and `target` empty in `META`, `origin`/`target` longer than 5,000 characters, a use case longer than 40,000 characters or a version label longer than 20 characters, **When** the preview is generated, **Then** the row appears as an error with the reason.
7. **Given** the preview, **When** the administrator cancels, **Then** nothing is stored.
8. **Given** extra columns in the file, **When** it is read, **Then** they are ignored and the preview lists them.
9. **Given** local staging, or production in the `pilot` phase, **When** the administrator uploads a file with any row whose `system` is different from `minitest`, **Then** the file is refused entirely, with the message "Only Minitest items are accepted in this phase" and the rows of the other system.
10. **Given** the administrator's interface in Spanish (Spain) (or English (US), or Portuguese (Brazil)), **When** a preview with ignored rows and errors is generated, **Then** the counts, the reasons, the refusal messages and the labels of the preview are shown in that language, while the values taken from the file (`system`, `edit_key`, `uc`, `field`, `origin`, `target`, use cases, version labels, extra column names) are shown exactly as in the file, never translated.

---

### User Story 2 - Confirm and form the blocks (Priority: P1)

On confirmation, the new items are stored, split into blocks of 10 in file order, and each block enters the end of the queue with roles 1 and 2 free.

**Why this priority**: The blocks and the queue are the input of the allocation (spec 007).

**Independent Test**: Confirm 38 items and check 4 blocks (10, 10, 10 and 8) and 8 tasks in the queue; then import another batch and check that the previous ones did not change.

**Acceptance Scenarios**:

1. **Given** the preview with 38 valid items, **When** the administrator confirms, **Then** 4 blocks of 10, 10, 10 and 8 items are created, in file order, and 8 tasks in the queue: B1-R1, B1-R2, B2-R1, B2-R2, …, all free.
2. **Given** 205 valid items in a batch, **When** confirmed, **Then** there are 20 blocks of 10 and one of 5, and 42 tasks.
3. **Given** the confirmation, **When** it finishes, **Then** the batch is recorded with date, time, file name and counts, and the administrator sees the summary: blocks created and positions taken in the queue.
4. **Given** a data collection in progress with a final block of 8 items, **When** a new batch is imported, **Then** that block keeps its 8 items and the new blocks enter after all the existing tasks.
5. **Given** two confirmations of the same file (double click or two tabs), **When** processed, **Then** the items are stored only once and the second confirmation shows all rows as ignored.
6. **Given** the confirmation, **When** the items are stored, **Then** existing items, blocks, tasks and responses do not change.
7. **Given** a confirmed batch, **When** its items are read back, **Then** every stored value is identical to the file, whatever the administrator's interface language was at the time of the import.

---

### User Story 3 - Consult the imported batches (Priority: P3)

The administrator sees the list of batches with date, file, systems and number of items and blocks.

**Why this priority**: It helps audit the database, but the full dashboard comes in spec 010.

**Independent Test**: Import two batches and check the list.

**Acceptance Scenarios**:

1. **Given** imported batches, **When** the administrator opens "Import", **Then** they see the previous batches, from the most recent to the oldest.

---

### User Story 4 - End the pilot and open the real data collection (Priority: P2)

After the pilot, with the production database recreated empty, the administrator switches the study phase from `pilot` to `collection` and can then import the real systems.

**Why this priority**: It ensures that the pilot, in production, uses only Minitest and that the pilot data does not mix with the real data collection.

**Independent Test**: In an automated test with the production configuration (MySQL in a container): import Minitest in the `pilot` phase, try to switch to `collection` (refused), clear the database, switch to `collection` (accepted) and import a file of another system (accepted).

**Acceptance Scenarios**:

1. **Given** production in the `pilot` phase, **When** the administrator opens "Import", **Then** they see the current phase and the warning that only Minitest items are accepted.
2. **Given** the `pilot` phase with any item, participant or response in the database, **When** the administrator tries to switch to `collection`, **Then** the change is refused, explaining that the database must be recreated first.
3. **Given** the `pilot` phase with the database empty, **When** the administrator confirms the switch to `collection`, **Then** the phase changes, the change is recorded with date and time and the import starts accepting any system.
4. **Given** the `collection` phase, **When** someone tries to go back to `pilot`, **Then** there is no such action: the only way is to recreate the database.
5. **Given** local staging (profile `staging`), **When** the administrator opens "Import", **Then** the action to switch to `collection` does not exist and only Minitest is accepted.

---

### Edge Cases

- Empty file or file with only the header: preview with zero items and confirmation disabled.
- File with BOM, Windows or Unix line endings: read the same way.
- File that is not CSV or not in UTF-8: refused with a clear message, in the administrator's interface language.
- File with 500 lines: preview and confirmation finish without exceeding the execution time.
- Import while participants request blocks: nobody receives a half-formed block.
- File whose texts are in a language different from the administrator's interface (for example, use cases in English with the interface in Portuguese (Brazil)): the texts are stored and shown as they are; only the preview labels and messages follow the interface language.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The import MUST accept UTF-8 CSV in the RFC 4180 standard, including fields with line breaks, with the columns of Appendix D of spec 001; extra columns are ignored.
- **FR-002**: The import MUST apply the row validation rules of FR-052 of spec 001.
- **FR-003**: The pair (`system`, `edit_key`) MUST be unique in the database; rows that repeat an existing item are ignored without altering it; repetitions within the file are errors from the second one on.
- **FR-004**: The import MUST show the preview and only store after confirmation.
- **FR-005**: On confirmation, the system MUST split the new items, in file order, into consecutive blocks of the configured size (10); the last block of the batch gets the remainder.
- **FR-006**: Each block MUST generate two tasks, role 1 and role 2, appended to the end of the queue in that order.
- **FR-007**: Blocks, items and queue positions MUST NOT change after they are created.
- **FR-008**: The confirmation MUST be atomic with respect to the other operations: it either stores the whole batch or nothing.
- **FR-009**: The batch MUST be recorded with date, time, file name and counts of included, ignored and error rows.
- **FR-010**: Only the administrator MUST be able to import.
- **FR-011**: In local staging, always, and in production, while the study phase (`study_phase`) is `pilot`, the import MUST refuse entirely a file that has any row with `system` different from `minitest`.
- **FR-012**: The administrator MUST be able to switch the phase from `pilot` to `collection` only in production and only with the database without items, participants and responses; the change is recorded and cannot be undone through the interface.
- **FR-013**: The preview, the summary and every error or refusal message of the import (row reasons, missing column, file not CSV or not UTF-8, refusal by study phase, phase switch refused) MUST be shown in the administrator's interface language (pt-BR, en-US or es-ES).
- **FR-014**: Imported values MUST be stored exactly as in the file, with no translation, whatever the administrator's interface language; the language of the interface MUST NOT affect what is stored.

### Key Entities

- **Import batch**: file, date/time, counts.
- **Item**: (`system`, `edit_key`), `uc`, `kind`, `field`, `origin`, `target`, full versions and use cases, batch, block and position in the block.
- **Block**: consecutive items of a batch; size from 1 to 10.
- **Task (block-role)**: role 1 or 2 of a block; position in the queue; starts free.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Importing 500 lines (those of Minitest repeated with a suffix in `edit_key`), with preview and confirmation, takes less than 2 minutes.
- **SC-002**: In all splitting tests (1, 9, 10, 11, 38 and 205 items), the number and size of the blocks match the rule.
- **SC-003**: A text imported and read back is identical to the one in the file in 100 % of the Minitest rows.
- **SC-004**: The preview of `minitest_duplicates.csv` generated with the interface in each of the three languages shows all its labels and messages in that language and the values of the file unchanged, in 100 % of the tests.
- **SC-005**: The file `veredito_minitest.csv` generated by spec 002 goes through the import preview in local staging without any row in error (closes SC-003 of spec 002).

## Assumptions

- The tests and staging use only the Minitest CSV generated by spec 002 (`veredito_minitest.csv`) and files derived from it (`minitest_duplicates.csv`, `minitest_missing_kind_column.csv`, `minitest_long_text.csv`, repetition for volume), as required by the constitution.
- The blocks are formed within each batch; a batch may end in a block smaller than 10 even if other batches come later.
- Items are not edited or deleted through the interface. Fixing an item before the start of the data collection requires clearing the database and importing again; after the start, the fix is recorded as a decision of the researcher, outside the system.
- Imported data stays in the language in which it was captured (the use cases of each system are in their original language); only the interface is translated.
