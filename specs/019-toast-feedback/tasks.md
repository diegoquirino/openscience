# Tasks: 019-toast-feedback

**Input**: Design documents from `/specs/019-toast-feedback/`

**Prerequisites**: plan.md, spec.md

## Phase 1: Setup

- [x] T001 Bump app to `0.19.0` (`backend/pom.xml`, `frontend/package.json` / lock, `compose.yaml`, `Dockerfile`, `SystemVersion.FALLBACK`, `write-build-info.mjs`)
- [x] T002 Point `.specify/feature.json` at `specs/019-toast-feedback`; ROADMAP lists 019

## Phase 2: Foundation

- [x] T003 Create `frontend/src/app/core/toast.service.ts` with kinds `error`|`warn`|`info`|`success`, duration >=10000, Close action (`$localize` `@@toast.close`)
- [x] T004 [P] Add toast panel styles in `frontend/src/styles.scss`
- [x] T005 [P] Unit tests in `frontend/src/app/core/toast.service.spec.ts`

## Phase 3: US1 - Admin toasts

- [x] T006 Migrate dashboard / import / export transient errors to ToastService; remove inline API alert markup
- [x] T007 Migrate participants list/anonymize failure (and success toast if applicable); keep contextual quota/export warnings inline
- [x] T008 Update admin component specs that asserted `[role=alert]` for API errors

## Phase 4: US2 - Participant + chunk

- [x] T009 Migrate sign-in, code, consent, skill, screening, profile, item, review, welcome transient errors to ToastService
- [x] T010 Align `chunk-error.ts` with >=10s toast pattern (Reload action retained)
- [x] T011 Update participant / chunk-error specs

## Phase 5: Polish

- [x] T012 Run `npm run i18n:extract` / `i18n:check` for Close string in pt-BR, en-US, es-ES
- [x] T013 Mark ROADMAP 019 Complete; mark tasks done; push CI with 020; **no** `deploy/*` without approval
