# Tasks: 041-suite-step-marks

**Input**: Design documents from `/specs/041-suite-step-marks/`

**Prerequisites**: [040-test-suite-panel](../040-test-suite-panel/tasks.md) Complete. Execute **strictly in task order**.

## Phase 1: Setup

- [x] T001 Bump app to `0.41.0`
- [x] T002 Point `.specify/feature.json`; add 041 to ROADMAP
- [x] T003 Spec/plan/tasks for 041

## Phase 2: Pipeline and frontend

- [x] T004 `render_txt` / `oracle-import`: mark only step/pre/post; regenerate empty CSVs + minitest fixture
- [x] T005 Enlarge test suite button + dialog `kind: 'suite'`; i18n
- [x] T006 Unit tests; `i18n:check`

## Phase 3: Polish

- [x] T007 Clean local staging; Complete; push; no cloud; no 1.0
