# Feature Specification: Continue labeling from the account menu

**Feature Branch**: `015-continue-labeling-menu`

**Created**: 2026-11-20

**Status**: Complete

**Input**: Participant account menu includes an item that returns to data labeling (`/label`).

**Depends on**: 014

## User Scenarios

### US1 - Return to labeling (P1)

1. Given a signed-in participant, When they open the account menu, Then they see Continue labeling, Profile and Sign out.
2. Given Continue labeling, When chosen, Then they navigate to `/label`.
3. Given a signed-in administrator, When they open the account menu, Then Continue labeling is not shown.

## Requirements

- FR-001: Participant account menu MUST offer Continue labeling linking to `/label`.
- FR-002: The item MUST NOT appear for the administrator.
- FR-003: App version **0.15.0**.

## Success Criteria

- SC-001: Automated tests cover the participant menu item and its absence for the administrator.
