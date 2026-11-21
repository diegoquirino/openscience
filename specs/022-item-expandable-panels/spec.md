# Feature Specification: Expandable panels on the item screen

**Feature Branch**: `022-item-expandable-panels`

**Created**: 2026-11-20

**Status**: Planned

**Input**: Improve the labeling item screen with expandable panels for (i) the change, (ii) impact question, (iii) maintenance-operation question, (iv) comment, and (v) action buttons.

**Depends on**: 008, 012, 016

## User Scenarios

### US1 - Participant reviews and answers in expandable sections (P1)

1. Given an item screen, When it loads, Then five expandable panels are present: change/diff, impact, operation, comment, actions.
2. Given the first view of an item, When panels render, Then **all panels start expanded** (decision 1A); the participant may collapse any panel.
3. Given collapsed panels, When the participant expands them again, Then content (diff, radios, comment, Continue) remains usable without horizontal scroll regressions on narrow viewports.

## Requirements

- FR-001: Structure the item body into five expandable panels as listed above.
- FR-002: Default expanded state is **all open**; collapse is optional and per-panel.
- FR-003: Preserve Appendix texts, required markers (012), keyboard access to radios/modals, and existing draft save behaviour.
- FR-004: Use shared visual language (016); prefer Material expansion panels or equivalent consistent with the app.
- FR-005: Stable test hooks (`data-testid` or roles) so Vitest/e2e do not depend on panels staying collapsed.
- FR-006: App version **0.22.0**.

## Success Criteria

- SC-001: Unit/layout tests still verify statements, options and required markers with panels expanded.
- SC-002: e2e item-screen scenarios pass with expandable markup.
- SC-003: i18n for panel titles in pt-BR, en-US, es-ES.
