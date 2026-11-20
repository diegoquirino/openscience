# Tasks: 020-admin-visual-consistency

**Input**: Design documents from `/specs/020-admin-visual-consistency/`

## Phase 1: Setup

- [x] T001 Bump app to `0.20.0` (pom, package.json/lock, compose, Dockerfile, SystemVersion, write-build-info)
- [x] T002 Point `.specify/feature.json` at `specs/020-admin-visual-consistency`

## Phase 2: US1 - Dashboard

- [x] T003 Add `page-panel` to reservations, participants and languages sections; keep progress/empty consistent in `dashboard.component.html`
- [x] T004 [P] Shared `.admin-page` / `.back` utilities in `styles.scss`

## Phase 3: US2 - Other admin screens

- [x] T005 Wrap import preview, summary and history in `page-panel`; align page shell
- [x] T006 Wrap export all-systems and system sections in `page-panel`; align page shell
- [x] T007 Align participants and admin hub (max-width, back, intro, panels)

## Phase 4: Polish

- [x] T008 Update tests if markup/selectors require it; mark ROADMAP 020 Complete; push CI; **no** deploy without approval
