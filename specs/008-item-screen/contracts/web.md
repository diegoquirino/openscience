# Contract: Rich item screen

**Feature**: `008-item-screen` | Replaces the item screen of
[spec 007](../../007-block-queue/contracts/web.md); the other labeling routes, the guards (the
onboarding of [spec 006](../../006-consent-screening/contracts/web.md) is still required) and the
language switch are unchanged.

## API: storage unchanged

Spec 008 changes no endpoint, link relation, payload or table. The screen reads
`GET /api/v1/tasks/{id}/items/{position}` and saves with `PUT` of `vd:save-response`, exactly as
007 ([tasks contract](../../007-block-queue/contracts/tasks.md)):

```json
{ "changeImpact": "HIGH", "maintenanceOperation": "UPDATE", "comment": "optional, up to 1000 characters" }
```

The stored values are the English codes whatever the interface language (FR-011).

## Route

| Route | Content | Access |
|---|---|---|
| `/label/items/:position` | Rich item screen (this document); the use case dialog opens over it | participant (labeling guard of 006) |

## Page layout (top to bottom)

| Element | Rule |
|---|---|
| Progress bar | "x of n" (counter, `.counter`) and the countdown `mm:ss` (`role="timer"`), always visible; sticky at the top of the viewport on narrow screens |
| Expiry warning | `role="alert"` paragraph at zero, as in 007; unsaved choices are kept |
| Header | System and use case exactly as captured; "<field label> · <kind label>" in the interface language (Appendix A); an unknown `field` or `kind` is shown as is |
| Edit | Layout by `kind` (next table) |
| Impact question | Statement of Appendix B.1 and one radio per option: short label, explanation below it |
| Operation question | Statement of Appendix B.2, the eight radios with their "when to choose" text, and the precedence rule |
| Comment | Optional, up to 1,000 characters, with counter |
| Error / actions | Problem Details message, "Try again" when the block was lost, Back and Continue as in 007 |

## Edit layout by kind

Two columns from 600 px up, stacked (Before above After) below that; no horizontal scrolling. Line
breaks of `origin`/`target` are kept (`white-space: pre-wrap`; long words wrap).

| `kind` | Left column (previous version) | Right column (modified version) |
|---|---|---|
| `MOD` | "Before": `origin`, removed words struck through; button "View full use case (<origin version>)" | "After": `target`, added words highlighted; button "View full use case (<target version>)" |
| `ADD` | Only the button for the origin version | "Added text": `target`; button for the target version |
| `DEL` | "Removed text": `origin`; button for the origin version | Only the button for the target version |
| `META` | "<field label>: before": `origin` with the diff; button | "<field label>: after": `target` with the diff; button; an empty side shows "(none)" |
| other | as `MOD` | |

Notes:
- Both texts equal in `MOD`/`META`: the note "No text difference in this part" (`.no-difference`,
  `role="note"`) appears above the blocks.
- An empty displayed block shows "(none)" (pt-BR "(nenhum)", en-US "(none)", es-ES "(ninguno)").
- Added and removed marks never rely on color alone: removed words use strikethrough and a
  background, added words use underline and a background (elements `del` and `ins`).

## Use case dialog

Opened by the button of each version; `MatDialog` with `role="dialog"`, labelled by its title.

| Element | Rule |
|---|---|
| Title | "<use case> · version <origin version> (previous)" or "<use case> · version <target version> (modified)", localized pattern; the use case name and the version labels as captured |
| Header | Counter and countdown (the same `ItemProgressComponent` and clock as the page) beside the title, so they are visible with the dialog open |
| Body | Whole use case in a monospaced font, one element per line; indentation kept as a hanging indent (long lines wrap without losing it); lines marked `<- edited` highlighted (background plus left border) with the marker removed; the body scrolls, the page behind does not |
| Scroll | Opens scrolled so the first highlighted line is visible; at the top when no line is marked |
| Missing version | When the stored text is only the pipeline text `(the use case does not exist in this version)`, the body shows the localized message (pt-BR "(este caso de uso não existe nesta versão)", en-US "(this use case does not exist in this version)", es-ES "(este caso de uso no existe en esta versión)"), no highlighting, no scrolling; the stored text is unchanged |
| Close | "Close" button, `Esc`, click outside; nothing is written; the impact, operation and comment of the item are untouched |
| Focus | First focus on "Close"; focus is trapped while open and returns to the originating button on close |
| Narrow screen (below 600 px) | Full screen (`100vw × 100dvh`); on wider screens at most 960 px wide and 85% of the viewport height |

## Question blocks

| Element | Rule |
|---|---|
| Impact | `mat-radio-group` named `impact`; options `LOW`, `HIGH` with the Appendix B.1 explanation of each (definition and examples) |
| Operation | `mat-radio-group` named `operation`; options in the order `KEEP`, `UPDATE`, `REMOVE`, `CREATE`, `MERGE`, `SPLIT`, `REASSIGN`, `FLAG`, each with its Appendix B.2 "when to choose" text; then the precedence rule: "If more than one applies, choose the one with the greatest consequence:" followed by the localized labels of `FLAG` > `REMOVE` > `MERGE` > `SPLIT` > `CREATE` > `UPDATE` > `REASSIGN` > `KEEP` |
| Accessible names | The radio name is the short label ("High", "Update"); the explanation is the description (`aria-describedby`) |
| Stored value | The code (`LOW`, `HIGH`, `KEEP`, …) in every locale |
| Continue | Disabled until both are answered (FR-004) |

## Countdown and announcement

| Rule | Detail |
|---|---|
| Source | `reservationExpiresAt` corrected with the server `Date` header, as in 007 |
| Last 2 minutes | The countdown uses the theme alert color (`urgent`), also in the dialog |
| Announcement | Once, when the last 2 minutes begin: "Less than 2 minutes left in this reservation" (localized) through a global polite live region; again only if a save renewed the time and it runs low again |
| Zero | Red `00:00` and the expiry warning (`role="alert"`), as in 007 |

## Language switch

The header control (spec 004) reloads under the new prefix onto the same
`/label/items/:position`. The unsaved impact, operation and comment are restored from the tab
`sessionStorage` (FR-078, spec 007); a dialog that was open is closed by the reload. The labels,
statements, explanations and messages change language; use case texts, `origin`, `target`, system
and `uc` do not.

## Strings (XLIFF ids)

New units in `messages.pt-BR.xlf` and `messages.es-ES.xlf` (checked by `npm run i18n:check`):

| Prefix | Content |
|---|---|
| `item.kind.*`, `item.field.*` | Appendix A labels (4 kinds, 11 fields) |
| `item.block.*` | "Before", "After", "Added text", "Removed text", "<field>: before", "<field>: after" |
| `item.usecase.*` | Button text with version, the two modal titles, "Close" |
| `item.missingUsecase` | Missing-version message |
| `item.none`, `item.noDifference` | "(none)" and "No text difference in this part" |
| `item.impact.*`, `item.operation.*` | Statements (`.question`), explanations (`.LOW`, `.KEEP`, ...) and the precedence sentence (`item.operation.precedence`) of Appendices B.1 and B.2 |
| `item.progress.lowTime` | The last-2-minutes announcement; the counter and "Time left" keep the ids `label.item.counter` and `label.item.timeLeft` of 007, and the header keeps `label.item.system` and `label.item.usecase` |

## Stable hooks for tests

`.counter`, `[role=timer]`, `.location`, `.system`, `.uc`, `.column[data-side=origin|target]` with `.block`, `.block-title` and `.text`, `button.open-usecase`, `.no-difference`, `.impact` and `.operation` fieldsets (`#impact-<CODE>`, `#operation-<CODE>` explanations), `.precedence .intro` and `.precedence .order`; in the dialog `.dialog-title`, `.body`, `.usecase-line` (`.edited`), `.missing-usecase`.
