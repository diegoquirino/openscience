# Feature Specification: Dashboard as administrator home

**Feature Branch**: `017-admin-dashboard-home`

**Created**: 2026-11-20

**Status**: Complete

**Depends on**: 010, 016

## User Scenarios

### US1 - Land on the dashboard (P1)

1. Given the administrator signs in, When the session starts, Then they open `/admin/dashboard`.
2. Given `/admin`, When opened, Then they are redirected to `/admin/dashboard`.

### US2 - Actions and metrics (P1)

1. Given the dashboard, When opened, Then action cards link to Import, Export and Participants.
2. Given the dashboard with data, When opened, Then summary metric cards show completion, free/in-progress/completed tasks, eligible participants and e-mail quota remaining.
3. Given the administrator account menu, When opened, Then it lists Dashboard, Import, Export, Participants, Profile and Sign out.

## Requirements

- FR-001: Admin post-login area is `/admin/dashboard`.
- FR-002: Action cards and account-menu shortcuts for admin functions.
- FR-003: Metric summary cards use existing 010 snapshot fields only.
- FR-004: App version **0.17.0**.

## Success Criteria

- SC-001: Tests cover admin home redirect and dashboard action cards.
