# Contract: Administrator import screens

**Feature**: `005-block-import` | Builds on the shell and `/admin` of
[spec 004](../../004-email-code-access/contracts/web.md).

## Routes (under each locale prefix)

| Route | Content | Access |
|---|---|---|
| `/admin` | Administration home; **Import** is enabled and links to `/admin/import`; Export, Dashboard and Participants stay disabled | administrator |
| `/admin/import` | Study-phase banner (when `minitestOnly`), upload control, preview panels, confirm/cancel, list of previous batches | administrator |

Guards: same as `/admin` in 004 (participant ? `/welcome`, visitor ? `/participate`).

## Import screen

| Element | Rule |
|---|---|
| Phase banner | When `minitestOnly` is true: current phase label and the warning that only Minitest is accepted (US4 scenario 1); texts from XLIFF |
| Switch to collection | Shown only when `GET study-phase` has `switchAllowed`; confirm dialog; on success the banner updates (US4 scenarios 2–3). Absent in staging (US4 scenario 5) |
| File input | Accepts `.csv`; sending calls `vd:request-preview`; progress indicator while waiting |
| Preview | Counts (included / ignored / errors / blocks); block sizes; tables of ignored and error rows (`line`, localized `reason`, `system`, `editKey`); list of extra columns; file values never translated (US1 scenario 10) |
| Confirm | Enabled only when `includedCount > 0` and `vd:confirm` is present; posts `{ previewId }`; shows the batch summary (blocks and queue positions) |
| Cancel | Clears the preview from the view; does not call confirm (US1 scenario 7) |
| Batch list | From `GET /api/v1/batches`, newest first: date/time, file name, systems, item and block counts (US3) |

All labels and messages follow the administrator's interface language (FR-013).

## Header / menu

No new header controls. The administrator menu item "Import" stops being `disabled` and becomes a
router link.
