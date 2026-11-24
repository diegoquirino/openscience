# Tasks: 030-org-autocomplete-min3

**Input**: Design documents from `/specs/030-org-autocomplete-min3/`

**Prerequisites**: [029-org-autocomplete-continue](../029-org-autocomplete-continue/tasks.md) Complete (app at `0.29.0`; 1.0 still gated). Execute **strictly in task order**.

## Phase 1: Setup

- [x] T001 Bump app to `0.30.0` (pom, package.json/lock, compose, Dockerfile, SystemVersion, write-build-info)
- [x] T002 Point `.specify/feature.json` at `specs/030-org-autocomplete-min3`; add 030 to ROADMAP (after 029 Complete)

## Phase 2: UI and seed

- [x] T003 Helper + skill/profile: suggestions only when trimmed query length >= 3 (FR-001, FR-002)
- [x] T004 Unit tests for min-3 filter (SC-001)
- [x] T005 Staging `V12__organization_other_seed.sql` with 50 orgs including the six named; update FixtureLocationTest (FR-003, FR-004)

## Phase 3: Polish

- [x] T006 Clean local staging; verify suggestions API count >= 50 and min-3 behaviour; mark ROADMAP 030 Complete; push; **no** cloud; **no** 1.0 until **promover a 1.0 e deploy**
