# Contract: Export screen

**Feature**: `009-exports` | Locale prefixes and admin guard as in
[spec 004](../../004-email-code-access/contracts/web.md) and
[spec 005](../../005-block-import/contracts/web.md).

## Routes

| Path | Screen | Who |
|---|---|---|
| `/admin` | Administration home; **Export** enabled (link to export) | administrator |
| `/admin/export` | Export downloads | administrator |

Participants never reach these routes (existing `administratorOnly` guard).

## Administration home

- Import stays a link to `/admin/import`.
- Export becomes a link to `/admin/export` (no longer disabled).
- Dashboard and Participants stay disabled until 010 / 011.
- Helper text updated: only the still-pending items are mentioned.

## Export screen (`/admin/export`)

1. On load, `GET /api/v1` then follow `vd:exports` (HAL).
2. Show localized title and short help (UI language only; CSV headers never translated).
3. Buttons / links:
   - Download wide table ? `vd:export-wide`
   - Download long table ? `vd:export-long`
4. For each entry in `systems`, a group labeled with the system key offers:
   - Download `_completo` ? that system's `vd:export-completo` href
   - Download rater 1 / rater 2 ? that system's `vd:export-rater1` / `vd:export-rater2`
5. Each download uses the browser download of the `text/csv` body; prefer the filename from
   `Content-Disposition`.
6. Empty `systems`: still offer wide and long; show a localized empty-state for per-system files.

## i18n

New XLIFF units under `admin.export.*` (title, help, button labels, empty systems, errors). Three
locales; `npm run i18n:check` green. Filenames and CSV contents stay English identifiers.

## Playwright

Administrator signs in, opens Export, downloads wide (and one per-system file when Minitest is
imported); participant never sees the Export link.
