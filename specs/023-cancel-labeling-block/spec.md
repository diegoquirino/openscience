# Feature Specification: Cancel labeling block

**Feature Branch**: `023-cancel-labeling-block`

**Created**: 2026-11-20

**Status**: Planned

**Input**: Add a Cancel control next to Continue on the item screen, with confirmation that the participant wants to abandon the block and that all classifications in that block will be discarded. After cancel, show a done-style screen with two icon cards: request another block, or leave.

**Depends on**: 007, 019, 022

## User Scenarios

### US1 - Participant cancels the current block (P1)

1. Given a held reservation with optional drafts, When the participant chooses Cancel next to Continue, Then a confirmation dialog explains that all answers in this block will be lost/deleted.
2. Given confirmation, When the API succeeds, Then drafts for **this task only** are deleted, the task returns to `free` in the queue (decision 2A), and the participant leaves the item screen.
3. Given cancel completed, When the done screen appears, Then two **cards with icons** offer: (a) request another block, (b) leave/exit (decision 3A).
4. Given the participant dismisses the confirmation, When they stay, Then the reservation and drafts are unchanged.

## Requirements

- FR-001: Cancel control adjacent to Continue on the item (actions panel).
- FR-002: Confirmation MUST warn that classifications in the current block will be discarded.
- FR-003: Backend MUST release the held task to free and delete responses/drafts for that task only (not previously submitted blocks).
- FR-004: HAL link for cancel (e.g. `vd:cancel-reservation` / DELETE on the held task) documented in OpenAPI.
- FR-005: Post-cancel UI: outcome screen with icon cards for **another block** and **leave**.
- FR-006: Transient success/error via toast (019).
- FR-007: App version **0.23.0**.

## Success Criteria

- SC-001: Backend tests: cancel frees task, deletes drafts, another participant can reserve the same task.
- SC-002: Frontend: confirm dialog; cancel button; done cards navigations.
- SC-003: e2e smoke: reserve ? answer one item ? cancel ? confirm ? cards visible.
