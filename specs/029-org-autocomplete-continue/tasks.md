# Tasks: 029-org-autocomplete-continue

**Input**: Design documents from `/specs/029-org-autocomplete-continue/`

**Prerequisites**: [028-lgpd-controller](../028-lgpd-controller/tasks.md) Complete (app at `0.28.0`; 1.0 still gated). Execute **strictly in task order**.

## Phase 1: Setup

- [x] T001 Bump app to `0.29.0` (pom, package.json/lock, compose, Dockerfile, SystemVersion, write-build-info)
- [x] T002 Point `.specify/feature.json` at `specs/029-org-autocomplete-continue`; add 029 to ROADMAP (after 028 Complete)

## Phase 2: Fix

- [x] T003 Skill + profile: `optionSelected` updates `organizationOther` (FR-001)
- [x] T004 Unit test: selecting a suggestion enables Continue and submits the value (FR-002)

## Phase 3: Polish

- [x] T005 Clean local staging at 0.29.0; mark ROADMAP 029 Complete; push; **no** cloud; **no** 1.0 until **promover a 1.0 e deploy**
