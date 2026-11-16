# Data model: Item screen with differences, context and explained questions

**Feature**: `008-item-screen` | **Date**: 2026-11-15 | **Research**: [research.md](research.md)

Spec 008 is **display-only**. It adds no table, column, constraint, index or Flyway migration (the
last migration stays `V5__task_queue_lookup.sql`), and it changes no API payload. A rollback to the previous revision needs nothing at the data level.

## Data read (unchanged from spec 007)

The screen renders the item returned by `GET /api/v1/tasks/{id}/items/{position}` (`ItemBody` in
`label.service.ts`):

| Field | Source column | How the screen uses it | Translated? |
|---|---|---|---|
| `position`, task `size` | `item` position in the block, `block.size` | Counter "x of n" | Counter text yes; numbers no |
| `system`, `uc` | `item.system`, `item.uc` | Header | No |
| `kind` | `item.kind` (`MOD`, `ADD`, `DEL`, `META`) | Layout; label of Appendix A in the header | Label yes; an unknown value is shown as is |
| `field` | `item.field` | Label of Appendix A in the header and in `META` block titles | Label yes; a field without a label is shown as is |
| `origin`, `target` | `item.origin`, `item.target` | Edit blocks with word-level highlighting | No |
| `originVersion`, `targetVersion` | `item.origin_version`, `item.target_version` | Button text and modal title ("v1.1") | No |
| `originUsecase`, `targetUsecase` | `item.origin_usecase`, `item.target_usecase` | Modal text; pipeline text replaced by a localized message when it is the whole text | Only the replaced message |
| `draft` | `response` (own, state `draft`) | Initial impact, operation and comment | No |

## Data written (unchanged)

Continue still sends `PUT` of the `vd:save-response` link with exactly
`{ "changeImpact", "maintenanceOperation", "comment" }`: `LOW`/`HIGH`, one of the eight codes of
Appendix B.2 (`KEEP`, `UPDATE`, `REMOVE`, `CREATE`, `MERGE`, `SPLIT`, `REASSIGN`, `FLAG`) and the
comment (up to 1,000 characters, `null` when empty). `language` is still taken by the server from
the request locale. Nothing computed for display (diff segments, line flags, labels) is stored or
sent.

## Display models (TypeScript, in memory only)

These types are plain interfaces in `frontend/src/app/label/item/`; they live as long as the screen.

### `DiffSegment` (`word-diff.ts`)

| Field | Type | Meaning |
|---|---|---|
| `kind` | `'same' \| 'removed' \| 'added'` | `same` text is on both sides; `removed` only in `origin`; `added` only in `target` |
| `text` | `string` | Exact characters of the tokens (line breaks included) |

Invariants: concatenating the `same` and `removed` segments gives `origin` exactly; concatenating
`same` and `added` gives `target` exactly; neighbouring segments never share a `kind`.

### `UseCaseView` (`usecase-text.ts`)

| Field | Type | Meaning |
|---|---|---|
| `missing` | `boolean` | The whole text is the pipeline text `(the use case does not exist in this version)` |
| `lines` | `UseCaseLine[]` | One entry per line of the stored text (empty when `missing`) |
| `firstEditedIndex` | `number` | Index in `lines` of the first edited line, `-1` when none |

`UseCaseLine`: `{ indent: number; content: string; edited: boolean }`. `content` is the line without
its leading whitespace and without a trailing `<- edited` marker.

### `ItemLayout` (`item-layout.ts`)

| Field | Type | Meaning |
|---|---|---|
| `header` | `{ system, uc, fieldLabel, kindLabel }` | Strings for the header (`fieldLabel` and `kindLabel` from Appendix A) |
| `columns` | `[ItemColumn, ItemColumn]` | Left (previous version) and right (modified version) |
| `identical` | `boolean` | `origin` equals `target` in a diff layout (shows the "No text difference" note) |

`ItemColumn`: `{ side: 'origin' \| 'target'; title: string \| null; segments: DiffSegment[] \| null;
marking: 'diff' \| 'added' \| 'removed'; empty: boolean; versionLabel: string \| null }`. `title` and
`segments` are `null` for the column that only holds a button (`ADD` left, `DEL` right); `empty`
makes the template show the localized "(none)".

## Localized text (static, in the application)

The Appendix texts are not data: they are XLIFF units of the three builds, changed only by a
published version (Assumption of the spec).

| Content | Source | Units |
|---|---|---|
| `kind` labels (4) and `field` labels (11 known values) | Appendix A | `item.kind.*`, `item.field.*` |
| Impact statement; Low and High explanations | Appendix B.1 | `item.impact.question`, `item.impact.LOW`, `item.impact.HIGH` |
| Operation statement; eight "when to choose" explanations; precedence rule | Appendix B.2 | `item.operation.question`, `item.operation.<CODE>`, `item.operation.precedence` |
| Option labels | Appendices B.1 / B.2 (unchanged units of 007) | `changeImpact.*`, `maintenanceOperation.*` |
| "(none)", missing-version message, "No text difference in this part" | FR-010, spec edge cases | `item.none`, `item.missingUsecase`, `item.noDifference` |
| Block titles, buttons, modal titles, countdown announcement | FR-008 | `item.block.*`, `item.usecase.*`, `item.progress.*` |

## Validation rules (display only)

| Rule | Source |
|---|---|
| Continue enabled only with both questions answered and a comment of at most 1,000 characters | FR-004 (as in 007) |
| The pipeline text is replaced only when it is the whole text, ignoring surrounding whitespace | FR-010, edge case |
| The `<- edited` marker is never shown; its line is highlighted | User Story 2, scenario 4 |
| An unknown `kind` or `field` is shown with its original value | FR-009 |

## Entities (spec vocabulary)

- **Item:** one use-case edit, read-only on this screen.
- **Response (draft):** the participant's choices for the item, written by the 007 operation only.
