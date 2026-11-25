# Tasks: 032-datetime-picker-locale

**Input**: Design documents from `/specs/032-datetime-picker-locale/`

**Prerequisites**: [031-invite-email-settings](../031-invite-email-settings/tasks.md) Complete (app at `0.31.0`; 1.0 still gated). Execute **strictly in task order**.

## Phase 1: Setup

- [x] T001 Bump app to `0.32.0` (pom, package.json/lock, compose, Dockerfile, SystemVersion, write-build-info)
- [x] T002 Point `.specify/feature.json` at `specs/032-datetime-picker-locale`; add 032 to ROADMAP (after 031 Complete)

## Phase 2: Implementation

- [x] T003 Provide native date adapter (+ localized `MatDatepickerIntl` if needed) in `app.config.ts` (FR-001, FR-002)
- [x] T004 Replace `datetime-local` on e-mail settings with Material datepicker + time; update helpers (FR-001, FR-002)
- [x] T005 i18n for new date/time label strings (FR-004)

## Phase 3: Polish

- [x] T006 Clean local staging; mark ROADMAP 032 Complete; push; **no** cloud; **no** 1.0 until **promover a 1.0 e deploy**
