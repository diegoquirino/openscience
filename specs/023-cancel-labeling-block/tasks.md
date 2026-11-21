# Tasks: 023-cancel-labeling-block

**Input**: Design documents from `/specs/023-cancel-labeling-block/`

**Prerequisites**: plan.md, spec.md; **022 complete**

## Phase 1: Setup

- [x] T001 Bump app to `0.23.0`
- [x] T002 Point `.specify/feature.json` at `specs/023-cancel-labeling-block`

## Phase 2: Backend

- [x] T003 Domain/service: cancel held task ? free + delete drafts for that task
- [x] T004 REST + HAL link + OpenAPI; tests (free again, drafts gone)

## Phase 3: Frontend

- [x] T005 Cancel + confirmation on item actions panel
- [x] T006 Done outcome with icon cards (another block / leave)
- [x] T007 i18n + Vitest/e2e smoke

## Phase 4: Polish

- [x] T008 Mark ROADMAP 023 Complete; push CI; **no** cloud deploy without approval; local staging clean launch when requested
