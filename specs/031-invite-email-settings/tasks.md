# Tasks: 031-invite-email-settings

**Input**: Design documents from `/specs/031-invite-email-settings/`

**Prerequisites**: [030-org-autocomplete-min3](../030-org-autocomplete-min3/tasks.md) Complete (app at `0.30.0`; 1.0 still gated). Execute **strictly in task order**.

## Phase 1: Setup

- [x] T001 Bump app to `0.31.0` (pom, package.json/lock, compose, Dockerfile, SystemVersion, write-build-info)
- [x] T002 Point `.specify/feature.json` at `specs/031-invite-email-settings`; add 031 to ROADMAP (after 030 Complete)

## Phase 2: Backend

- [x] T003 Flyway `V13__invite_email_settings.sql` + classpath mail seed files (FR-001, FR-004)
- [x] T004 Repositories + InviteMailer + settings/templates API (FR-002)
- [x] T005 Send inicial after first profile save; idempotent sent_at (FR-003)
- [x] T006 Backend tests (settings round-trip; profile triggers Mailpit/mock)

## Phase 3: Frontend

- [x] T007 Admin e-mail settings page with **date and time** fields + template editors + hub link (FR-002, FR-006)
- [x] T008 i18n pt-BR / en-US / es-ES for new UI strings

## Phase 4: Polish

- [x] T009 Clean local staging; manual path profile ? Mailpit inicial; mark ROADMAP 031 Complete; push; **no** cloud; **no** 1.0 until **promover a 1.0 e deploy**
