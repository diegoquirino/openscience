# Tasks: 025-organization-affiliation

**Input**: Design documents from `/specs/025-organization-affiliation/`

**Prerequisites**: [024-release-1.0](../024-release-1.0/tasks.md) **T001?T005 Complete** (app at `0.24.0`; 1.0 still gated). Execute **strictly in task order**; do not start a later task before the previous is checked.

## Phase 1: Setup

- [x] T001 Bump app to `0.25.0` (pom, package.json/lock, compose, Dockerfile, SystemVersion, write-build-info)
- [x] T002 Point `.specify/feature.json` at `specs/025-organization-affiliation`; add 025 to ROADMAP (after 024 Complete)

## Phase 2: Backend

- [x] T003 Flyway migration: `organization` / `organization_other` + CHECKs on `participant`
- [x] T004 Domain: organization codes + validation; onboarding rule (missing org ? `profile`); repository read/write
- [x] T005 Skill API: extend request/model/PUT; `vd:organization-suggestions`; OpenAPI
- [x] T006 Export columns for organization (+ other when `other`)
- [x] T007 Backend tests (SC-001)

## Phase 3: Frontend

- [x] T008 Labels + skill screen: radios + Outros(as) autocomplete
- [x] T009 Profile screen: same organization controls
- [x] T010 i18n pt-BR / en-US / es-ES + Vitest/e2e smoke (SC-002, SC-003)

## Phase 4: Docs and polish

- [x] T011 Amend 001 appendix / GLOSSARY for organization codes
- [x] T012 Local staging verify migration + UI; mark ROADMAP 025 Complete; push CI; **no** cloud deploy; **no** 1.0 until **promover a 1.0 e deploy**
