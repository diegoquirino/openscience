# Feature Specification: Dual-panel anonymization

**Feature Branch**: `018-anonymization-panels`

**Created**: 2026-11-20

**Status**: Complete

**Depends on**: 011, 016, 017

## User Scenarios

### US1 - Browse participants (P1)

1. Given the administrator opens Anonymization of Participants, When the page loads, Then they see a panel of people with e-mail and a panel of all pseudonyms.
2. Given a person in either panel, When selected, Then the detail card and keep/discard actions appear (if not yet anonymized).

### US2 - Choose pseudonym on anonymize (P1)

1. Given confirmation, When the administrator leaves the pseudonym field empty, Then the existing `R-NNNN` is kept.
2. Given a unique new pseudonym, When confirmed, Then responses stay under that pseudonym and name/e-mail are erased.
3. Given a taken pseudonym, When confirmed, Then anonymization fails with a clear error and nothing is erased.

## Requirements

- FR-001: Screen title and nav label are Anonymization of Participants.
- FR-002: `GET /api/v1/participants` without email/pseudonym lists all cards.
- FR-003: Anonymize body accepts optional `pseudonym`; uniqueness enforced.
- FR-004: App version **0.18.0**.

## Success Criteria

- SC-001: Backend and UI tests cover list, rename-on-anonymize and conflict.
