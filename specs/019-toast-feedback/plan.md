# Plan: 019-toast-feedback

## Summary

Introduce `ToastService` wrapping Angular Material `MatSnackBar` with typed panel classes, duration ?10?000 ms and a Close action. Migrate transient `message` / failure alerts on admin and participant screens. Keep field errors and contextual banners inline. Route chunk-load reload offer through the same duration/dismiss conventions. Version **0.19.0**.

## Technical approach

- `frontend/src/app/core/toast.service.ts`: `show(kind, message)`, constants for duration and `panelClass` (`toast-error`, `toast-warn`, `toast-info`, `toast-success`).
- Global SCSS in `frontend/src/styles.scss` for panel colours.
- Replace `message.set(...)` used for API/action feedback with `toast.error(...)` (etc.); remove corresponding `@if (message())` alert blocks from templates.
- `chunk-error.ts`: open via ToastService or MatSnackBar with same duration; keep Reload as action (Reload dismisses and reloads).
- Tests: Vitest for ToastService; update component specs that queried `[role=alert]` for API errors.
- i18n: `@@toast.close` (and reuse existing new-version strings).

## Contracts

No API/HAL changes.

## Target platform

Local Docker staging; production Cloud Run (deploy only with approval).
