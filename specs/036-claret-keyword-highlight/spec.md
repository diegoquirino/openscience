# Feature Specification: CLARET keyword (IDE-style) highlighting

**Feature Branch**: `036-claret-keyword-highlight`

**Created**: 2026-11-25

**Status**: Complete

**Input**: On the item labeling screen (and the enlarge use-case dialog), CLARET **language tokens** MUST be visually distinguished from developer-written content, in the style of an IDE or of the thesis `lstdefinestyle{CLARET}` keywords — so raters can separate structure (`usecase`, `step`, …) from natural-language descriptions in quotes. Display-only; no database or API change.

**Depends on**: 008, 034, 035

## User Scenarios

### US1 - Keywords vs content on the Change panel (P1)

1. Given a use case line such as `step 1 emailUser "launches the login screen"`, When it is shown, Then `step` is styled as a language keyword and the quoted phrase is styled as a string (developer content), while `emailUser` stays plain text.
2. Given keywords from the CLARET set (`systemName`, `usecase`, `actor`, `preCondition`, `postCondition`, `basic`, `alternative`, `exception`, `step`, `af`, `ef`, `bs`), When they appear outside quotes, Then each is keyword-styled (case-sensitive).
3. Given the same word inside a double-quoted string, When displayed, Then it is NOT keyword-styled (it stays part of the string).
4. Given an edited line with before/after highlight colors (035), When keywords are shown, Then keyword/string styles remain visible on top of the line highlight.

### US2 - Same tokenization in Enlarge (P1)

1. Given Enlarge for either version, When the use case is shown, Then the same keyword/string highlighting rules apply.

## Requirements

- **FR-001**: Tokenize each visible use case line into `keyword` | `string` | `text` using the CLARET keyword list aligned with the thesis listing style; do not match keywords inside `"..."`.
- **FR-002**: Render tokens as spans with distinct CSS (keyword vs string vs plain); keep indentation and 035 edit highlights.
- **FR-003**: Apply in inline columns and `UseCaseDialogComponent`.
- **FR-004**: No schema, pipeline, or API payload change; stored text unchanged (marker still stripped only for display).
- **FR-005**: App version **0.36.0**; 1.0 gated; no cloud; clean local staging.
- **FR-006**: Unit tests for the tokenizer; component tests assert keyword/string classes in the DOM.

## Success Criteria

- **SC-001**: On a sample login use case, `usecase` / `step` / `exception` appear under `.claret-keyword` and quoted descriptions under `.claret-string`.
- **SC-002**: A keyword substring inside quotes is not marked as `.claret-keyword`.
- **SC-003**: ROADMAP 036 Complete at 0.36.0; staging clean for manual test.
