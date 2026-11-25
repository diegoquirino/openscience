# Feature Specification: Full use case in before/after + Today on date pickers

**Feature Branch**: `034-full-usecase-inline`

**Created**: 2026-11-25

**Status**: Complete

**Input**: On the item labeling screen, Before/After MUST show the **complete use case** of each version involved in the change, with the existing pipeline highlights (`<- edited` lines), so the rater sees context and does not judge from the isolated fragment alone. The database and stored item fields stay unchanged — display only. Separately, every admin date picker MUST offer a **Today** shortcut.

**Depends on**: 008, 022, 032, 033

## User Scenarios

### US1 - Full use case with highlights inline (P1)

1. Given an item with `originUsecase` / `targetUsecase`, When the rater opens the Change panel, Then the Before column shows the full previous-version use case and the After column the full modified-version use case.
2. Given lines marked `<- edited` in the stored use case text, When displayed, Then those lines are highlighted and the marker is not shown (same rules as the existing modal).
3. Given a missing use case in one version, When that column is shown, Then the localized missing-use-case message appears.
4. Given ADD/DEL/META/MOD, When the layout is built, Then both columns still present the full use case of their version (not only the field fragment).

### US2 - Today shortcut on date pickers (P1)

1. Given any send-settings date field, When the administrator clicks **Today**, Then that field’s date becomes the current local calendar day (time unchanged if already set).

## Requirements

- **FR-001**: Item Change panel Before/After (and ADD/DEL/META columns) render full `originUsecase` / `targetUsecase` with highlight rules from `parseUseCase`; field-level `origin`/`target` word-diff is no longer the primary before/after body.
- **FR-002**: No schema or API payload change; display-only.
- **FR-003**: Date pickers on e-mail send settings expose a Today control.
- **FR-004**: App version **0.34.0**; 1.0 gated; no cloud; clean local staging.
- **FR-005**: Update unit/component tests for the new layout; keep fullscreen dialog as optional enlarge.

## Success Criteria

- **SC-001**: On a MOD item, both columns show multi-line use case text with at least one highlighted edited line when markers exist.
- **SC-002**: Today sets the date field to today without leaving the page.
- **SC-003**: ROADMAP 034 Complete at 0.34.0; staging clean for manual test.
