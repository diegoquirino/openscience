# Feature Specification: Item screen with differences, context and explained questions

**Feature Branch**: `008-item-screen`

**Created**: 2026-11-10

**Status**: Draft

**Input**: User description: "The item screen shows the edit comfortably (before and after side by side, with the differences highlighted), buttons that open in a modal the full use case in the previous and in the modified version, and the two questions with the explanation of each option."

**Parent spec**: [001-oracle-labeling](../001-oracle-labeling/spec.md), User Stories 4 and 11, FR-030 to FR-033, FR-070, FR-075 to FR-077, Appendices A, B.1 and B.2 · **Depends on**: 003 (can be built with sample items from the file of spec 002) · **Integrates with**: 007

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Read the edit with the differences highlighted (Priority: P1)

The rater sees what changed without having to compare the texts in their head.

**Why this priority**: The validity of the oracle depends on the rater understanding the edit.

**Independent Test**: Display one item of each `kind` (MOD, ADD, DEL, META) from Minitest on a computer and on a mobile phone.

**Acceptance Scenarios**:

1. **Given** a `MOD` item, **When** it is displayed, **Then** "Before" (origin) and "After" (target) appear side by side, with the removed words struck through and the added ones highlighted.
2. **Given** an `ADD` item, **When** it is displayed, **Then** only "Added text" appears, with the target.
3. **Given** a `DEL` item, **When** it is displayed, **Then** only "Removed text" appears, with the origin.
4. **Given** a `META` item of `requirements`, **When** it is displayed, **Then** it appears as "Requirements link: before / after"; if one of the sides is empty (metadata added or removed), it appears as "(none)".
5. **Given** any item, **When** it is displayed, **Then** the header shows system, use case and the location of the edit in natural language (Appendix A), for example "Exception flow step · Modification".
6. **Given** a text with several lines or with more than 400 characters, **When** it is displayed, **Then** it appears in full, with the line breaks kept.
7. **Given** a narrow screen, **When** the item is displayed, **Then** "Before" and "After" are placed one below the other, without horizontal scrolling.
8. **Given** a `field` without a translation, **When** the item is displayed, **Then** the original name appears.

---

### User Story 2 - Look up the full use case in each version (Priority: P1)

Next to "Before" and "After", a button opens in a modal the full use case of that version, already at the edited line.

**Why this priority**: Many edits only make sense in the context of the whole flow.

**Independent Test**: Open both modals in an item of each `kind`, close them in each way and check that the choices of the item are still there.

**Acceptance Scenarios**:

1. **Given** any item, **When** it is displayed, **Then** next to "Before" there is the button "View full use case (v1.1)" and next to "After", "View full use case (v1.2)", with the version labels of the item; in an `ADD` the first button takes the place of the "Before" block, and in a `DEL` the second takes the place of the "After" block.
2. **Given** the button of the previous version, **When** clicked, **Then** it opens the modal "<use case> · version v1.1 (previous)" with the full text, in a monospaced font and with the indentation preserved.
3. **Given** the button of the modified version, **When** clicked, **Then** it opens the modal "<use case> · version v1.2 (modified)".
4. **Given** lines marked with `<- edited`, **When** the modal opens, **Then** they appear highlighted, the marker is not displayed and the modal opens already scrolled to the first of them.
5. **Given** a use case without a marked line, **When** the modal opens, **Then** it opens at the top, without highlighting.
6. **Given** an open modal, **When** the person clicks "Close", presses Esc or clicks outside, **Then** the modal closes and the choices of the item are still there.
7. **Given** a narrow screen, **When** the modal opens, **Then** it takes up the whole screen and long lines wrap without losing the initial indentation.

---

### User Story 3 - Answer with the options explained (Priority: P1)

Each question brings the definition and examples of each option, and the precedence rule of the operation.

**Why this priority**: It reduces disagreements caused by different understandings of the concepts.

**Independent Test**: Check the texts against Appendices B.1 and B.2 and answer an item.

**Acceptance Scenarios**:

1. **Given** the impact question, **When** displayed, **Then** it shows the statement and the explanation of Low and High from Appendix B.1, with a single choice.
2. **Given** the operation question, **When** displayed, **Then** it shows the eight options with the explanation from Appendix B.2, with a single choice, and the precedence rule.
3. **Given** both questions answered, **When** the person moves on, **Then** it stores exactly the same codes and comment as the simple screen of spec 007.
4. **Given** a keyboard, **When** the person navigates the screen, **Then** they reach buttons, options and modals by keyboard, and the focus returns to the originating button when the modal closes.

---

### User Story 4 - See the item screen in the chosen language (Priority: P1)

All the texts of the item screen follow the interface language (pt-BR, en-US or es-ES), while the use cases and edits appear exactly as captured.

**Why this priority**: The questions and explanations must be understood in the person's own language for the labels to be valid, and the data must not be altered.

**Independent Test**: Display one item of each `kind`, and an item whose previous version does not exist, in each of the three locales; open both modals; compare every label, question and explanation against Appendices A, B.1 and B.2 of spec 001 in that locale.

**Acceptance Scenarios**:

1. **Given** the interface in pt-BR, en-US or es-ES, **When** an item is displayed, **Then** the `kind` and `field` labels (Appendix A), the block titles ("Before", "After", "Added text", "Removed text"), the buttons, the modal titles, the question statements, the option labels and their explanations, the precedence rule (Appendices B.1 and B.2), the counter and the warnings appear in that language.
2. **Given** any locale, **When** an item is displayed, **Then** the system, the use case name, `origin`, `target` and the full use case texts appear exactly as captured, without translation.
3. **Given** a `META` item with `origin` or `target` empty, **When** it is displayed, **Then** the empty side shows pt-BR "(nenhum)", en-US "(none)" or es-ES "(ninguno)", according to the interface language.
4. **Given** an item whose full use case text of a version (`origin_usecase` or `target_usecase`) holds the pipeline text `(the use case does not exist in this version)`, **When** the person opens the modal of that version, **Then** it shows, without line highlighting, pt-BR "(este caso de uso não existe nesta versão)", en-US "(this use case does not exist in this version)" or es-ES "(este caso de uso no existe en esta versión)", according to the interface language; the stored text does not change.
5. **Given** a `field` without a translation, **When** the item is displayed in any locale, **Then** the original name appears, the same in every locale.
6. **Given** an item on the screen, **When** the person switches the language in the header, **Then** the page reloads with the same item in the new language, the use case texts are unchanged, and the impact, operation and comment chosen but not saved yet are restored (spec 001, FR-078); the option labels change language, the stored codes (`LOW`, `HIGH`, `KEEP`, `UPDATE`, ...) do not.

---

### Edge Cases

- Edit that only changes spaces or punctuation: the highlighting shows the difference, even if small.
- Identical text before and after (for example, metadata): the screen warns "No text difference in this part".
- Use case with dozens of steps: the modal scrolls without freezing the screen.
- Use case that does not exist in a version (spec 002): the modal shows the localized message of FR-010 instead of the pipeline text.
- Use case text that contains the pipeline text in the middle of other lines: shown as captured; only a text that is entirely the pipeline text (ignoring surrounding whitespace) is replaced by the localized message.
- Long labels in es-ES or pt-BR: wrap without overlapping the counter or the buttons, also on a mobile phone.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The item header MUST show system, use case and the kind and location of the edit in natural language (Appendix A).
- **FR-002**: The system MUST show `origin` and `target` side by side with word-by-word highlighting (only `target` in `ADD`, only `origin` in `DEL`; in `META`, the empty side appears as the localized "(none)" of FR-010), keeping line breaks and stacking the blocks on narrow screens.
- **FR-003**: The system MUST offer the two full use case buttons, labeled with the versions of the item, opening modals as in User Story 2.
- **FR-004**: The questions MUST bring the explanation of each option (Appendices B.1 and B.2), with a single choice; moving on is only available with both answered.
- **FR-005**: The screen MUST NOT change what is stored compared to spec 007.
- **FR-006**: The screen MUST be usable by keyboard and readable on a mobile phone.
- **FR-007**: The screen MUST keep the reservation countdown of spec 007, in minutes and seconds, always visible next to the block progress ("x of 10"), also on a mobile phone and with a use case modal open; in the last 2 minutes, it changes to the alert color of the theme and is announced to screen readers only once.
- **FR-008**: All the texts of the item screen MUST be shown in the interface language (pt-BR, en-US or es-ES): the `kind` and `field` labels of Appendix A, the question texts, option labels, explanations and precedence rule of Appendices B.1 and B.2, block titles, buttons, modal titles, countdown warnings and screen reader announcements, taken from spec 001 for that locale.
- **FR-009**: The use case data (system, use case name, `origin`, `target`, full use case texts) MUST be shown as captured, in every locale, without translation; a `field` without a label in Appendix A MUST be shown with its original name.
- **FR-010**: When `origin_usecase` or `target_usecase` holds only the pipeline text `(the use case does not exist in this version)`, the screen MUST show instead pt-BR "(este caso de uso não existe nesta versão)", en-US "(this use case does not exist in this version)" or es-ES "(este caso de uso no existe en esta versión)"; the empty side of a `META` edit MUST be shown as pt-BR "(nenhum)", en-US "(none)" or es-ES "(ninguno)". Both follow the interface language and change nothing that is stored.
- **FR-011**: The option labels MUST be translated, but the values chosen and stored MUST be the fixed English codes (`LOW`/`HIGH`; `KEEP`, `UPDATE`, ... of Appendix B.2), whatever the interface language, as they are analyzed as categories after the collection. A language switch MUST restore the impact, operation and comment chosen on the item and not saved yet (spec 001, FR-078).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: The use case modal opens in less than 1 second.
- **SC-002**: A block of 10 items is completed, on average, in less than 20 minutes in the pilot.
- **SC-003**: In the pilot, at least 80% of the volunteers say they understood the edit without outside help.
- **SC-004**: In a test with one item of each `kind`, a `META` item with an empty side and an item with a missing use case version, the screen works in each of the three locales: every label, question, explanation and message appears in that locale with no untranslated string, the use case texts appear as captured, and the stored codes are the same in every locale.

## Assumptions

- The texts of Appendices A, B.1 and B.2 of spec 001, in pt-BR, en-US and es-ES, are fixed in the system; changing them requires a new published version.
- The difference highlighting is computed at display time, from `origin` and `target`; nothing new is stored.
- The pilot mentioned in the success criteria is the trial run with a few volunteers planned in the [ROADMAP](../ROADMAP.md), before the call for participants.
- The use cases are English data from the ISSTA_2027 dataset; participants read them in English whatever the interface language.
