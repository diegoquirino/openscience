# Tasks: 033-invite-bulk-schedule

**Input**: Design documents from `/specs/033-invite-bulk-schedule/`

**Prerequisites**: [032-datetime-picker-locale](../032-datetime-picker-locale/tasks.md) Complete (app at `0.32.0`; 1.0 still gated). Execute **strictly in task order**.

## Phase 1: Setup

- [x] T001 Bump app to `0.33.0` (pom, package.json/lock, compose, Dockerfile, SystemVersion, write-build-info)
- [x] T002 Point `.specify/feature.json` at `specs/033-invite-bulk-schedule`; add 033 to ROADMAP (after 032 Complete)

## Phase 2: Backend

- [x] T003 Recipient queries + claim/clear for lembrete and agradecimento (FR-002, FR-003)
- [x] T004 `InviteBulkSendJob` every minute; send due templates via InviteMailer (FR-002)
- [x] T005 Backend tests with MovableClock + Mailpit (FR-005)

## Phase 3: Frontend

- [x] T006 Reorder moments start ? reminder ? end; 24-hour `HH:mm` time fields (FR-001)
- [x] T007 i18n for any new strings (time hint / validation)

## Phase 4: Polish

- [x] T008 Clean local staging; mark ROADMAP 033 Complete; push; **no** cloud; **no** 1.0 until **promover a 1.0 e deploy**
