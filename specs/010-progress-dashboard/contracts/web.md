# Contract: Administrator dashboard screen

**Feature**: `010-progress-dashboard` | Follows [dashboard.md](dashboard.md) and the admin shell of
specs 005 / 009.

## Route

| Path | Who | Source of truth |
|---|---|---|
| `/admin/dashboard` | Administrator only (`administratorOnly` guard) | `GET` via `vd:dashboard` from `/api/v1` |

Participants who open `/admin/dashboard` are redirected like other admin routes (consent / next
step). Visitors are sent to sign-in.

## Admin home

- **Dashboard** list item is enabled and navigates to `/admin/dashboard`.
- `@@admin.soon` mentions only participants (dashboard removed from the “next releases” sentence).
- Participants menu item stays disabled until 011.

## Screen behaviour

1. On init, follow `vd:dashboard` (never hardcode `/api/v1/dashboard`).
2. Render four panels: queue progress (overall row first), reservations, participants (status,
   skills, completed-by-pseudonym), languages + e-mail quota.
3. **Refresh** button re-GETs the same link and replaces the snapshot; it does not reload the SPA
   document.
4. When `progress` overall `items === 0`, show an empty state with a control that follows
   `vd:batches` (or the admin Import route already known from the home screen) toward Import.
5. When `emailQuota.lowRemaining` is true, highlight the remaining count.
6. Reservation rows with `expired: true` show the localized “expired” label.
7. No name or e-mail is shown; only `pseudonym`.

## i18n

New XLIFF units under `dashboard.*` (or `admin.dashboard.*`) in en-US / pt-BR / es-ES. Skill codes
keep their existing translation units. `i18n:check` must stay green.

## Automated checks

- Vitest: component renders overall progress and hides PII; Refresh calls the service again.
- Playwright: administrator opens Dashboard and sees progress after import; participant has no
  Dashboard link and cannot stay on `/admin/dashboard`.
