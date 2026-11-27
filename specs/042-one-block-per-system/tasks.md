# Tasks: 042-one-block-per-system

**Input**: Design documents from `/specs/042-one-block-per-system/`

**Prerequisites**: [041-suite-step-marks](../041-suite-step-marks/tasks.md) Complete. Execute **strictly in task order**.

## Phase 1: Setup

- [x] T001 Create `Veredito/.cursor/rules/veredito-spec-kit.mdc`
- [x] T002 Bump app to `0.42.0`
- [x] T003 Point `.specify/feature.json`; add 042 to ROADMAP; amend 007 FR-014
- [x] T004 Spec/plan/tasks for 042

## Phase 2: Queue

- [x] T005 `heldSystems` + candidate systems; extend `NextTaskChooser` and reservation service
- [x] T006 Unit and API tests (same system skip; other system ok; cancel frees)

## Phase 3: Polish

- [x] T007 Clean local staging; Complete; push; no cloud; no 1.0
