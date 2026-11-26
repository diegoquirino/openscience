# Tasks: 036-claret-keyword-highlight

**Input**: Design documents from `/specs/036-claret-keyword-highlight/`

**Prerequisites**: [035-before-after-highlight-colors](../035-before-after-highlight-colors/tasks.md) Complete. Execute **strictly in task order**.

## Phase 1: Setup

- [x] T001 Bump app to `0.36.0`
- [x] T002 Point `.specify/feature.json`; add 036 to ROADMAP

## Phase 2: Tokenize and render

- [x] T003 `tokenizeClaret` + wire into `parseUseCase` (FR-001)
- [x] T004 Item + dialog templates/styles (FR-002, FR-003)
- [x] T005 Unit and component tests (FR-006)

## Phase 3: Polish

- [x] T006 Clean local staging; Complete; push; no cloud; no 1.0
