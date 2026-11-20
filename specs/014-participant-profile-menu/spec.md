# Feature Specification: Participant profile menu

**Feature Branch**: `014-participant-profile-menu`

**Created**: 2026-11-19

**Status**: Complete

**Input**: Header name opens a menu with Profile and Sign out; profile edits name and skill.

**Depends on**: 013

## User Scenarios

### US1 - Open profile from the header (P1)

1. Given a signed-in participant with a name, When they open the account menu, Then they see Profile and Sign out.
2. Given Profile, When opened, Then name and skill can be viewed and changed.
3. Given an administrator, When Profile is opened, Then only name is editable (no skill).

### US2 - Updates apply immediately (P1)

1. Given a saved name/skill change, When the header is shown, Then the new name appears.
2. Given dashboard/export, When skill was changed, Then they use the current skill.

## Requirements

- FR-001: Account menu on the signed-in header.
- FR-002: Skill PUT remains available after onboarding (not write-once).
- FR-003: E-mail identity is immutable; anonymized participants cannot edit.
- FR-004: Admin profile edits name only.

## Success Criteria

- SC-001: Automated tests cover the account menu and a profile update.
