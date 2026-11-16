# Contract: Participant labeling screens

**Feature**: `007-block-queue` | Builds on the shell and session of
[spec 004](../../004-email-code-access/contracts/web.md).

## Routes (under each locale prefix)

| Route | Content | Access |
|---|---|---|
| `/welcome` | Short thanks; primary action starts labeling (`vd:next-task`) or shows the outcome message when the reserve returns an `outcome` | participant |
| `/label` | Resolves the current reservation (POST reserve) and redirects to the first unanswered item, the review, or the done screen | participant |
| `/label/items/:position` | Simple item screen (system, use case, kind, field, origin, target, impact, operation, comment, counter, countdown) | participant |
| `/label/review` | List of all items with choices and comments; open any item to edit; Submit when complete | participant |
| `/label/done` | End message from `outcome`; actions "Label another block" (when `more-available`) and "Sign out" | participant |

Guards: same as `/welcome` in 004 (administrator ? `/admin`, visitor ? `/participate`).

## Item screen (simple; replaced by 008)

| Element | Rule |
|---|---|
| Fields | `system`, use case texts, `kind`, `field`, `origin`, `target` with line breaks preserved; never translated |
| Impact | Single choice `LOW` / `HIGH` with localized short labels (Appendix B.1 display names) |
| Operation | Single choice of the eight codes with localized short labels (Appendix B.2) |
| Comment | Optional, max 1,000 characters |
| Continue | Disabled until both questions answered; on click `PUT` draft then navigate to next pending or review |
| Counter | "x of n" in the UI language (FR-009, FR-017) |
| Countdown | `mm:ss` from `reservationExpiresAt` corrected with server time (FR-013); on zero shows the expiry warning and keeps unsaved choices |
| Back | Allowed to a previous item; changing and continuing replaces the draft |

## Language switch

Header language control (004) reloads under the new locale prefix onto the same `/label/...` URL.
Unsaved impact / operation / comment of the current item are restored from tab `sessionStorage`
(FR-016, FR-078). The switch itself does not call save and does not renew the reservation; the
countdown keeps the remaining time from the last known `reservationExpiresAt`.

## Review and done

| Screen | Rule |
|---|---|
| Review | Shows every item's two choices and comment; edit opens `/label/items/:position`; Submit calls `vd:submit` |
| Incomplete submit | Server `block-incomplete` ? navigate to `firstPendingPosition` |
| Done | Thanks + localized text for `outcome`; "Label another block" only when `more-available` / successful re-reserve path |

## Header / menu

No new administrator controls. Participant shell keeps language + sign-out; labeling routes reuse
the same shell.
