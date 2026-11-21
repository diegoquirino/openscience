# Feature Specification: Account name after profile

**Feature Branch**: `021-account-name-after-profile`

**Created**: 2026-11-20

**Status**: In progress

**Input**: After the participant saves name (and skill) on first access, the header account control still shows the fallback "Account" / "Conta" instead of the saved name.

**Depends on**: 014

## User Scenarios

### US1 - Name appears in the header after first profile (P1)

1. Given a new participant who just saved name and skill on the profile step, When any subsequent screen with the shell header is shown, Then the account control shows the saved name (not the "Account" fallback).
2. Given the same participant later edits the name on the profile screen, When save succeeds, Then the header continues to show the updated name (already covered by profile `session.refresh`; must remain true).

## Requirements

- FR-001: After a successful skill/profile save that stores the participant name, the in-memory session MUST include that name before or as navigation continues.
- FR-002: The shell account label MUST prefer `session.name` over the localized "Account" fallback whenever the name is non-empty.
- FR-003: No HAL/API contract changes required if `vd:session` already returns `name` after the skill PUT (refresh the session client-side).
- FR-004: App version **0.21.0**.

## Success Criteria

- SC-001: Unit test: after skill submit success, `SessionService` holds the saved name (via refresh or equivalent).
- SC-002: Shell / skill specs assert the account button text contains the name after save.
- SC-003: Manual or e2e smoke: first onboarding profile ? next screen header shows the name.
