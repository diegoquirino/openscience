# Feature Specification: Distinct before/after edit highlight colors

**Feature Branch**: `035-before-after-highlight-colors`

**Created**: 2026-11-25

**Status**: Complete

**Input**: On the item labeling screen (and the enlarge use-case dialog), lines marked as edited MUST use a **different highlight color** in the **before** (previous / origin) column than in the **after** (modified / target) column, so the rater can tell at a glance which side is which. Display-only; no database or API change.

**Depends on**: 008, 034

## User Scenarios

### US1 - Distinct colors on the item Change panel (P1)

1. Given a MOD item with `<- edited` lines in both use cases, When the rater views Before and After, Then edited lines in Before use one highlight color and edited lines in After use a clearly different color.
2. Given ADD/DEL/META layouts, When a column is origin-sided (before/removed/metaBefore), Then its edited highlights use the **before** color; when target-sided (after/added/metaAfter), Then they use the **after** color.
3. Given a line that is not edited, When displayed, Then it has no edit highlight on either side.

### US2 - Same colors in the enlarge dialog (P1)

1. Given the rater opens Enlarge for the previous version, When edited lines are shown, Then they use the **before** highlight color.
2. Given Enlarge for the modified version, When edited lines are shown, Then they use the **after** highlight color.

## Requirements

- **FR-001**: Before/origin edited highlights MUST NOT share the same highlight color token as after/target edited highlights.
- **FR-002**: Apply the same pairing in the inline columns and in `UseCaseDialogComponent` (via the dialog’s `side`).
- **FR-003**: No schema, pipeline, or API payload change.
- **FR-004**: App version **0.35.0**; 1.0 gated; no cloud; clean local staging.
- **FR-005**: Unit/component tests assert distinct before/after highlight classes (or equivalent) on edited lines.

## Success Criteria

- **SC-001**: On a two-column item, an edited line under `data-side=origin` and one under `data-side=target` carry different side-specific highlight classes/styles.
- **SC-002**: Enlarge dialog for origin vs target uses the matching side highlight.
- **SC-003**: ROADMAP 035 Complete at 0.35.0; staging clean for manual test.
