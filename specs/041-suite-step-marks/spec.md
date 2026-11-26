# Feature Specification: Fine-grained suite marks and Enlarge test suite

**Feature Branch**: `041-suite-step-marks`

**Created**: 2026-11-26

**Status**: Complete

**Input**: In the Test suite TXT panel, `<- edited` must mark only the step / Preconditions / Postconditions lines tied to the current edit SIDs (not the whole test case). Each suite column must offer **Enlarge test suite**, mirroring Enlarge use case. Empty oracle CSVs are regenerated; app version **0.41.0**.

**Depends on**: 040, 008, 034, 035

## User Scenarios

### US1 - See only the affected suite snippets marked (P1)

1. Given an item whose edit touches one use-case step SID, When the Test suite panel shows Before/After, Then only the step line(s) whose `sources` intersect that SID carry `<- edited` (and the UI highlight).
2. Given an edit on preconditions or postconditions, When the suite is shown, Then only the Preconditions or Postconditions line of affected cases is marked — not Test Case ID, Summary, or the Step header.
3. Given a test case with no intersecting step/pre/post for the edit, When shown, Then none of its lines are marked (the case text remains visible).

### US2 - Enlarge the suite of one version (P1)

1. Given the Test suite panel, When the rater clicks Enlarge test suite on a column, Then a full-screen / wide dialog shows that version's suite TXT with the same edit highlights (no CLARET), scrolled to the first marked line, without changing impact/operation/comment.
2. Given a narrow viewport, When Enlarge opens, Then the dialog is full screen (same rules as Enlarge use case).

## Requirements

- **FR-001**: Pipeline `render_txt` marks by SIDs on step / PRE / POST lines only; `oracle-import` passes edit SIDs; regenerate all `veredito_*_vazio.csv` and the minitest fixture.
- **FR-002**: Item screen: Enlarge test suite button per suite column; dialog reuses the use-case dialog with `kind: 'suite'` (`parseSuiteText`, suite titles, missing-tests label).
- **FR-003**: i18n for enlarge and dialog titles; `i18n:check` clean.
- **FR-004**: App version **0.41.0**; 1.0 gated; no cloud; clean local staging.

## Success Criteria

- **SC-001**: Regenerated suite texts mark step/pre/post only for the current edit; Enlarge opens suite dialog without changing answers.
- **SC-002**: ROADMAP 041 Complete at 0.41.0; staging clean for manual test.
