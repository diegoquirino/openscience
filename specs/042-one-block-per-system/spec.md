# Feature Specification: One block per system per rater

**Feature Branch**: `042-one-block-per-system`

**Created**: 2026-11-27

**Status**: Complete

**Input**: Each rater may receive or keep at most **one block per `system`** (reserved or completed), so dual-role kappa is not diluted by the same person labeling many blocks of the same subject. Cancel frees the system hold. App version **0.42.0**.

**Depends on**: 007, 005, 023

## User Scenarios

### US1 - Skip further blocks of a system already held (P1)

1. Given a participant who completed a block whose items are system `minitest`, When they request another task and only `minitest` blocks remain free, Then they get outcome `none-available-for-you` (free tasks may still exist for others).
2. Given a participant who completed a `minitest` block, When a free task exists for another system, Then they receive that task (queue order among allowed tasks).
3. Given a participant with a reserved (including expired, not reclaimed) block of system S, When they request another task of S, Then they do not receive it.

### US2 - Cancel frees the system (P1)

1. Given a participant who reserved a block of system S and cancels it (spec 023), When they request a task, Then they may receive another block of S.

### US3 - Dual-role and single reservation unchanged (P1)

1. Given the existing 007 rules, When reserving, Then a person still never gets both roles of the same block and still holds at most one reserved task at a time.

## Requirements

- **FR-001**: Held systems = distinct `item.system` of the participant's tasks with `status IN ('reserved','completed')`.
- **FR-002**: Next-task chooser skips a candidate if its block id is held **or** any of its item systems intersects held systems.
- **FR-003**: Cancel releases the task ? system no longer held; complete keeps the system held for the study.
- **FR-004**: Outcomes `more-available` / `none-available-for-you` / `database-complete` use the same filter.
- **FR-005**: Document 007 FR-014: no global block cap, but at most one block per system per rater (this spec).
- **FR-006**: App version **0.42.0**; 1.0 gated; no cloud; clean local staging.
- **FR-007**: Spec Kit golden rule Cursor file under `Veredito/.cursor/rules/`.

## Success Criteria

- **SC-001**: After one completed minitest block, the same rater cannot reserve another minitest block; another system remains reachable.
- **SC-002**: Cancel then re-reserve same system succeeds; ROADMAP 042 Complete at 0.42.0; staging clean for manual test.
