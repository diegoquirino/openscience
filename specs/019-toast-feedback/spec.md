# Feature Specification: Toast feedback

**Feature Branch**: `019-toast-feedback`

**Created**: 2026-11-20

**Status**: Complete

**Input**: Transient action/API feedback (error, warn, info, success) is shown as a toast for at least 10 seconds with a dismiss control.

**Depends on**: 018

## User Scenarios

### US1 - Admin sees toast feedback (P1)

1. Given an administrator action fails (import, export, dashboard load, anonymization list/anonymize), When the API returns an error, Then a toast of type error (or warn when appropriate) appears for at least 10 seconds with a Close action.
2. Given anonymization succeeds, When the operation completes, Then a success toast may confirm it; structured result on the card may remain.
3. Given field validation on a form, When the field is invalid, Then `mat-error` stays inline (not a toast).

### US2 - Participant sees toast feedback (P1)

1. Given sign-in, code, onboarding, profile, item or review, When an API/action error occurs, Then a typed toast appears (?10 s, dismissible).
2. Given contextual page warnings (expired reservation banner, quota/export advisories on anonymization), When the page shows them, Then they remain inline, not toasts.
3. Given a chunk-load failure after deploy, When navigation fails, Then the reload offer uses the same toast duration and dismiss pattern (Reload as the primary action).

## Requirements

- FR-001: A shared toast API supports kinds `error`, `warn`, `info` and `success`.
- FR-002: Toasts MUST remain visible for at least 10?000 ms and MUST offer dismiss (Close), i18n in pt-BR, en-US and es-ES.
- FR-003: Visual styling MUST distinguish the four kinds (typical toast colours).
- FR-004: Transient action/API messages MUST use toasts; form `mat-error` and contextual page banners MUST stay inline.
- FR-005: No HAL/API contract changes.
- FR-006: App version **0.19.0**.

## Success Criteria

- SC-001: Unit tests cover ToastService duration, kind classes and dismiss label.
- SC-002: Screen tests that previously asserted inline `[role=alert]` for API errors assert toast instead (or snackbar panel).
- SC-003: i18n completeness check passes for the Close string.
