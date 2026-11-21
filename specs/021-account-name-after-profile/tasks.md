# Tasks: 021-account-name-after-profile

**Input**: Design documents from `/specs/021-account-name-after-profile/`

**Prerequisites**: plan.md, spec.md

## Phase 1: Setup

- [x] T001 Bump app to `0.21.0` (`backend/pom.xml`, `frontend/package.json` / lock, `compose.yaml`, `Dockerfile`, `SystemVersion.FALLBACK`, `write-build-info.mjs` / `BUILD_VERSION`)
- [x] T002 Point `.specify/feature.json` at `specs/021-account-name-after-profile`; ROADMAP lists 021�023

## Phase 2: US1

- [x] T003 After successful skill save, refresh session then navigate (`skill.component.ts`)
- [x] T004 Update `skill.component.spec.ts` for refresh-after-save
- [x] T005 Confirm profile path still refreshes (no regression)

## Phase 3: Polish

- [x] T006 Mark ROADMAP 021 Complete when done; push CI; **no** `deploy/*` without approval
