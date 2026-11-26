# Feature Specification: Icons on titles and action buttons

**Feature Branch**: `039-ui-icons`

**Created**: 2026-11-26

**Status**: Planned

**Input**: Every participant and administration screen MUST show Material Symbols Outlined icons on page titles (and relevant section/panel titles) and on action buttons/links, following the existing shell/admin-hub pattern. Decorative icons are `aria-hidden`; i18n stays on the text span.

**Depends on**: 003, 037, 038

## User Scenarios

### US1 - Titles and buttons have icons (P1)

1. Given any participant or admin page with an `h1`, When it is shown, Then a `mat-icon` with `fontSet="material-symbols-outlined"` appears beside the title.
2. Given primary/secondary action buttons (`mat-flat-button`, `mat-stroked-button`, `mat-button` used as actions), When shown, Then each has a leading icon glyph.
3. Given the shell menu and admin action cards that already have icons, When shown, Then they remain unchanged.

## Requirements

- **FR-001**: Shared CSS for `.page-title` / `.page-title-icon` and button-leading icons.
- **FR-002**: Apply icons per the 039 plan glyph map on participant and admin screens (plus not-found / unavailable).
- **FR-003**: Move `i18n` onto inner text spans when icons are added; run `i18n:check`.
- **FR-004**: App version **0.39.0**; 1.0 gated; no cloud; clean local staging.
- **FR-005**: Update unit tests that assert raw `h1`/`button` textContent.

## Success Criteria

- **SC-001**: Spot-check: sign-in, item, dashboard, export each show title and button icons.
- **SC-002**: ROADMAP 039 Complete at 0.39.0; staging clean for manual test.
