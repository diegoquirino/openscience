# Contract: Problem Details and messages added by spec 007

**Feature**: `007-block-queue` | Format, locale resolution and bundles as in
[spec 003](../../003-deployment-foundation/contracts/problem-details.md).

## New types

| Slug | Status | When | Extensions |
|---|---|---|---|
| `not-eligible` | 403 | Participant session present but `EligibilityGate` is false (wired for 006; unused while the stub returns true) | none |
| `reservation-expired` | 409 | Save or submit after another person took the expired task, or caller never held it | none |
| `block-incomplete` | 409 | Submit while at least one item lacks a draft | `firstPendingPosition` (1-based) |
| `task-already-submitted` | 409 | Save attempted on a completed task | none |
| `task-conflict` | 409 | Defensive: unique (`block_id`, `participant_id`) violated | none |

`authentication-required`, `forbidden`, `invalid-request` and `not-found` of 003/004 are reused.

## End-message keys (success outcomes, not Problem Details)

Keys `label.end.<outcome>` in the three bundles / XLIFF; the API returns the stable `outcome` slug
and the UI shows the localized string.

| Slug | When |
|---|---|
| `no-items-yet` | Empty study database |
| `database-complete` | Every task completed |
| `none-available-for-you` | Tasks remain but none for this participant |
| `more-available` | After submit, another reserve would succeed (completion screen offers another block) |

## Tests

`LocalizedProblemDetailsTest` gains one case per new type in each of the three locales (the
`not-eligible` case uses a test double that forces the gate to false). Domain tests cover every
invalid code path for drafts at least once.
