# Feature Specification: Shared visual design

**Feature Branch**: `016-visual-design`

**Created**: 2026-11-20

**Status**: Complete

**Input**: Consistent h1–h3 hierarchy, cards/panels and icons on participant and admin screens.

**Depends on**: 014

## User Scenarios

### US1 - Clear page hierarchy (P1)

1. Given any main screen, When opened, Then there is one page title (`h1`) and sections use `h2`/`h3` as needed.
2. Given admin and participant areas, When viewed, Then content blocks use shared panel/card styling.

### US2 - Icons on main actions (P1)

1. Given the administration hub, When opened, Then Import, Export, Dashboard and Participants show icons.
2. Given the account menu, When opened, Then Continue labeling (participant), Profile and Sign out show icons.

## Requirements

- FR-001: Shared page/panel/heading styles in global SCSS.
- FR-002: Material icons on primary navigation actions (admin hub and account menu).
- FR-003: No HAL/API contract changes.
- FR-004: App version **0.16.0**.

## Success Criteria

- SC-001: Admin hub renders action cards with icons; shell menu items include icons; Vitest for admin still passes.
