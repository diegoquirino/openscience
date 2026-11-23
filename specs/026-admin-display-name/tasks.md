# Tasks: 026-admin-display-name

**Input**: Design documents from `/specs/026-admin-display-name/`

**Prerequisites**: [025-organization-affiliation](../025-organization-affiliation/tasks.md) Complete (app at `0.25.0`; 1.0 still gated). Execute **strictly in task order**.

## Phase 1: Setup

- [x] T001 Bump app to `0.26.0` (pom, package.json/lock, compose, Dockerfile, SystemVersion, write-build-info)
- [x] T002 Point `.specify/feature.json` at `specs/026-admin-display-name`; add 026 to ROADMAP (after 025 Complete)

## Phase 2: Data and tests

- [x] T003 Flyway `V11__admin_display_name.sql`: set `config.admin_name` to `Carlos Diego Quirino Lima` when null/blank
- [x] T004 Update migration / config tests that expected null `admin_name` after migrate (SC-001)

## Phase 3: Polish

- [x] T005 Local staging verify admin name on session/`vd:me`; mark ROADMAP 026 Complete; push CI; **no** cloud deploy; **no** 1.0 until **promover a 1.0 e deploy**
