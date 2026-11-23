# Tasks: 027-acknowledgment-choice

**Input**: Design documents from `/specs/027-acknowledgment-choice/`

**Prerequisites**: [026-admin-display-name](../026-admin-display-name/tasks.md) Complete (app at `0.26.0`; 1.0 still gated). Execute **strictly in task order**; do not start a later task before the previous is checked.

## Phase 1: Setup

- [x] T001 Bump app to `0.27.0` (pom, package.json/lock, compose, Dockerfile, SystemVersion, write-build-info)
- [x] T002 Point `.specify/feature.json` at `specs/027-acknowledgment-choice`; add 027 to ROADMAP (after 026 Complete)

## Phase 2: Consent UI and copy

- [x] T003 Consent screen: replace checkbox with required positive radios (Sim named / Não anonymous); Accept disabled until choice; map to `acknowledgmentAnonymous` (SC-001)
- [x] T004 i18n en-US / pt-BR / es-ES for legend + radio labels (FR-002)
- [x] T005 Update staging TCLE fixture section VII (and en/es) to describe the platform radio choice (US3 / FR-004)

## Phase 3: Polish

- [x] T006 Confirm Não does not set `anonymized_at` and does not block labeling path (SC-002); mark ROADMAP 027 Complete; push CI; **no** cloud deploy; **no** 1.0 until **promover a 1.0 e deploy**
