# Feature Specification: Admin visual consistency

**Feature Branch**: `020-admin-visual-consistency`

**Created**: 2026-11-20

**Status**: Complete

**Input**: All administrator screens share the same visual language (page title, intro, content panels).

**Depends on**: 016, 017, 019

## User Scenarios

### US1 - Dashboard panels match (P1)

1. Given the dashboard, When opened, Then Queue progress, Reservations, Participants and Languages/quota sections each use the shared `page-panel` styling.
2. Given Reservations previously looked flat, When compared to Queue progress, Then both share the same panel chrome.

### US2 - Import, export, anonymization and hub match (P1)

1. Given Import, When preview/summary/history are shown, Then each content block uses `page-panel` (or equivalent shared panel class).
2. Given Export, When all-systems and per-system sections are shown, Then they use the same panel styling.
3. Given Anonymization and the admin hub, When opened, Then width, back link, `h1`, `page-intro` and panels align with the dashboard/import/export look.

## Requirements

- FR-001: Shared admin page utilities (`page-panel`, intro, back) applied on dashboard, import, export, participants and hub.
- FR-002: No HAL/API or metric contract changes; presentation only.
- FR-003: App version **0.20.0**.

## Success Criteria

- SC-001: Dashboard reservations (and other content sections) render inside `page-panel`.
- SC-002: Import/export content sections use `page-panel`; Vitest/e2e selectors remain stable.
