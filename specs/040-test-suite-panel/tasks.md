# Tasks: 040-test-suite-panel

**Input**: Design documents from `/specs/040-test-suite-panel/`

**Prerequisites**: [039-ui-icons](../039-ui-icons/tasks.md) Complete. Execute **strictly in task order**.

## Phase 1: Setup

- [x] T001 Bump app to `0.40.0`
- [x] T002 Point `.specify/feature.json`; add 040 to ROADMAP
- [x] T003 Spec/plan/tasks for 040

## Phase 2: Backend and frontend

- [x] T004 Flyway + import rules + API/persistence for `origin_tests` / `target_tests`
- [x] T005 Item panel Test suite between Impact and Operation; i18n
- [x] T006 Unit tests; `i18n:check`

## Phase 3: Pipeline and polish

- [x] T007 Extend `oracle-import`; regenerate empty CSVs + minitest fixture
- [x] T008 Clean local staging; Complete; push; no cloud; no 1.0
