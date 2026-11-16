# Research: Item screen with differences, context and explained questions

**Feature**: `008-item-screen` | **Date**: 2026-11-15 | **Spec**: [spec.md](spec.md)

This research starts from the item screen and API of spec 007 ([research.md](../007-block-queue/research.md),
section 11 "Simple item screen vs 008") and from Appendices A, B.1 and B.2 of spec 001. It only
records what spec 008 decides on top of them.

## 1. Versions (checked on 2026-11-15)

| Component | Version | Note |
|---|---|---|
| Spring Boot | 4.1.2 | Unchanged |
| Angular / Material / CDK / localize | 22.2.6 | Unchanged; `MatDialog` and CDK `LiveAnnouncer` come with the installed packages |
| Vitest / Playwright | 5.x / 1.65.0 | Unchanged |
| App version | 0.8.0 | Spec number in the minor digit (006 → 0.6.0, 007 → 0.7.0) |

No new Maven or npm dependency. A diff library was considered and rejected (section 2).

## 2. Word-level difference

- **Decision**: a small plain-TypeScript function `diffWords(origin, target)` in
  `label/item/word-diff.ts`.
  1. **Tokenize** each text into runs of letters/digits/underscore (words), single punctuation or
     symbol characters, and runs of whitespace (a newline is part of a whitespace token), so a
     change of only spaces or punctuation still shows a difference (spec edge case).
  2. **Longest common subsequence** over the two token arrays (classic dynamic programming).
  3. Emit segments `{ kind: 'same' | 'removed' | 'added', text }`, merging neighbours of the same
     kind. The "Before" side renders `same` + `removed`; the "After" side renders `same` + `added`.
  4. **Size guard**: when `origin tokens × target tokens` exceeds 1,000,000 the function skips the
     LCS and reports the whole origin as removed and the whole target as added (never freezes the
     screen; the data is shown in full either way).
- **Rationale**: the edits to diff are single steps, titles or blocks of one use case (hundreds of
  characters; the longest Minitest `origin`/`target` are a few thousand), so a quadratic LCS is
  fast and exact. Keeping whitespace and punctuation as tokens gives the highlighting of small
  differences the spec asks for. The whole-use-case texts are **never** diffed: the modal shows
  them with the `<- edited` lines the pipeline already marked.
- **Alternatives considered**: `diff` / `diff-match-patch` packages (a dependency for ~40 lines of
  code, and character-level output by default); diffing the full use cases (noisy and slow);
  server-side diff (would add API surface that FR-005 forbids and the spec says nothing new is
  stored or served).

## 3. Layout per `kind`

- **Decision**: a pure function `buildItemLayout(item)` returns the view model the template renders:

  | `kind` | Left column | Right column |
  |---|---|---|
  | `MOD` | "Before" with `origin` (same + removed) + button "View full use case (<origin version>)" | "After" with `target` (same + added) + button for the target version |
  | `ADD` | Only the button for the origin version (takes the place of the "Before" block) | "Added text" with `target` (whole text marked as added) + button for the target version |
  | `DEL` | "Removed text" with `origin` (whole text marked as removed) + button for the origin version | Only the button for the target version (takes the place of the "After" block) |
  | `META` | "<field label>: before" with `origin` (diff) + button | "<field label>: after" with `target` (diff) + button; an empty side shows the localized "(none)" |
  | other value | as `MOD`, with the raw `kind` shown in the header | |

  Header: system, use case (`uc`) and "<field label> · <kind label>", for example "Exception flow
  step · Modification". A `field` without a label in Appendix A shows its original name, in every
  locale (FR-009). When `origin` equals `target` in the diff layouts (`MOD`, `META`), a note "No text
  difference in this part" appears above the blocks (spec edge case).
- **Rationale**: the spec fixes the shape of each kind (User Story 1 and 2). A pure function keeps
  the kind rules testable without the DOM. In `ADD` and `DEL` the whole text is the change, so it
  uses the "added" or "removed" style as a whole; the strikethrough of removed words is reserved
  for the `MOD`/`META` diff so a fully removed block stays readable (it is highlighted with the
  removed background and the heading "Removed text").
- **Alternatives considered**: always showing two blocks and leaving one empty (contradicts
  scenarios 2 and 3); strikethrough on the whole `DEL` text (hard to read long removed steps).

## 4. Use case text in the modal

- **Decision**: `parseUseCase(text)` in `label/item/usecase-text.ts` returns `{ missing, lines }`.
  - `missing` is true only when the whole text, ignoring surrounding whitespace, equals the
    pipeline text `(the use case does not exist in this version)` (FR-010 and the edge case about
    that text in the middle of other lines: then it is shown as captured).
  - Each line becomes `{ indent, content, edited }`: a trailing `<- edited` marker (with the
    spaces before it) is removed from `content` and sets `edited`; `indent` is the number of
    leading whitespace columns (a tab counts as 4).
  - `firstEditedIndex` is the index of the first edited line, or `-1`.
- **Rendering**: a monospaced block, one element per line. The leading whitespace is rendered as
  `padding-left: <indent>ch` and the content with `white-space: pre-wrap; overflow-wrap: anywhere`, so
  long lines wrap on a phone **keeping the indentation** (hanging indent), and an edited line
  uses a highlighted background plus a left border (not color alone). The marker never reaches the
  DOM.
- **Scroll**: after the dialog is rendered, the scroll container is moved so the first edited line
  sits near the top third of the visible area (`scrollTop` computed from the line and container
  rectangles; no `scrollIntoView`, which would also scroll the page). With no edited line, or a
  missing use case, the dialog stays at the top.
- **Rationale**: FR-033 of spec 001, User Story 2 scenarios 4, 5 and 7. Per-line elements allow
  highlighting and hanging indentation with plain CSS; dozens of steps are a few dozen elements, so
  the modal does not freeze.
- **Alternatives considered**: a `<pre>` with `<mark>` spans (wrapped lines lose the indentation);
  a code-editor component (a heavy dependency for read-only text).

## 5. The modal

- **Decision**: Angular Material `MatDialog` with a dedicated `UseCaseDialogComponent`.
  - Title "<use case> · version v1.1 (previous)" / "(modified)", with the version labels as
    stored on the item (they already carry the `v`, for example `v1.1`).
  - Closes with the "Close" button, `Esc` and a click on the backdrop (the Material defaults);
    nothing is written when it closes, and the item component owns the choices, so they are
    untouched (scenario 6).
  - Focus returns to the originating button when it closes (`restoreFocus`, the Material default),
    focus is trapped while open, and the first focus lands on the "Close" button. Both buttons stay
    reachable by keyboard.
  - **Narrow screens** (below 600 px, evaluated when it opens with the CDK `BreakpointObserver`):
    full screen (`100vw × 100dvh`, no border radius); wider screens use at most 960 px and 85% of the
    viewport height. The body scrolls inside the dialog.
  - The **header of the dialog repeats the counter and the countdown** (section 6).
- **Rationale**: spec 001 FR-033 and spec 008 FR-003/FR-006/FR-007; `MatDialog` already provides
  the focus trap, Esc, backdrop and focus restoration the spec requires, with no new dependency.
- **Alternatives considered**: a native `<dialog>` (weaker Material integration, theme and focus
  behaviour to rebuild); an in-page expandable panel (the spec asks for a modal).
- **A language switch while the modal is open** reloads the page, which closes the modal (it is not
  state that must be restored; the unsaved choices are, FR-011).

## 6. Countdown, urgency and announcement

- **Decision**: the clock logic moves from `LabelItemComponent` to an `ItemClock` service provided
  by the item component, so the page and the dialog show the same values:
  - `remaining`, `countdown` (`mm:ss`) and `expired` as in 007;
  - `urgent` is true while 0 < remaining ≤ 120 seconds; the progress component then uses the theme
    alert color (`--mat-sys-error`), the same color as at zero;
  - the first time `urgent` becomes true, the clock announces the localized "Less than 2 minutes
    left in this reservation" **once** through the CDK `LiveAnnouncer` (a global live region,
    which keeps working while the page behind the modal is hidden from assistive technology);
    when the remaining time goes above 120 seconds again (a save renewed the reservation), the next
    low-time period is announced once more.
  - The timer element keeps `role="timer"` (it does not announce every second).
- **Component**: `ItemProgressComponent` renders "x of n" and the countdown; the page header and
  the dialog header both use it.
- **Rationale**: FR-007 and the Edge Case of the modal; one clock avoids two intervals drifting.
- **Alternatives considered**: `aria-live` on the timer (announces every tick); a separate live
  region inside the dialog (duplicates the page's).

## 7. Localized texts

- **Decision**: all strings are `$localize` / `i18n` units with explicit ids, extracted to the two
  translation files:
  - **Appendix A** (kind and field labels) in `core/labels/item-labels.ts`, one function per
    code like `core/labels/response-labels.ts` of 007; an unknown `field` or `kind` is returned
    as is (FR-009).
  - **Appendices B.1 and B.2** (statement, option explanations, operation statement and precedence
    rule) in `core/labels/question-texts.ts`. The option labels stay in `response-labels.ts` (007).
    The precedence rule is stored as the sentence "If more than one applies, choose the one with the
    greatest consequence:" and the sequence `FLAG > REMOVE > MERGE > SPLIT > CREATE > UPDATE >
    REASSIGN > KEEP` is rendered with the **localized option labels** (so a Spanish reader sees
    "Señalar > Eliminar > Unir …"); the stored codes and their order are those of the appendix.
  - "(none)" and "(this use case does not exist in this version)" are units used by the layout and
    the dialog; the stored text is never changed.
- **Explanations outside the radio label**: each option is a Material radio button with the short
  label as its content and the explanation in a paragraph linked by `aria-describedby`, so the
  accessible name of the radio stays "Alto" (the e2e of 007 keep finding it) and screen readers
  read the explanation as the description.
- **Rationale**: FR-008, FR-009, FR-011 and constitution Principle II (i18n completeness); the
  XLIFF check fails the build if a unit is missing in a locale.
- **Alternatives considered**: loading the Appendix texts from the back end (new API, contradicts
  FR-005 and "fixed in the system" assumption); translating on the fly from one language (never).

## 8. Unsaved choices and what is stored

- **Decision**: unchanged from 007: `unsavedKey(taskId, position)`, `keepOnSwitch`, `takeUnsaved`,
  `clearUnsaved` and the `PUT` body `{ changeImpact, maintenanceOperation, comment }` are reused as
  they are. The rich screen only changes the template and the display model.
- **Rationale**: FR-005 and FR-011; the restore after a language switch already works and is covered
  by 007's e2e, which this spec adapts to the new screen.

## 9. Test plan

- **Vitest**: `word-diff.spec.ts`, `usecase-text.spec.ts`, `item-layout.spec.ts`,
  `item-clock.spec.ts`, `usecase-dialog.component.spec.ts` and an updated `item.component.spec.ts`
  (kinds, `(none)`, "no text difference", unchanged `PUT` body).
- **Playwright** (`e2e/item-screen.spec.ts`, a project that runs after `queue`): the Minitest
  batch has all four kinds (rows 1 `MOD`, 3 `ADD`, 8 and 25 `META`, 21 `DEL`), so the test reserves
  the blocks still free after `queue.spec.ts`, finds one item of each kind through the item API, and
  checks it on screen in pt-BR, en-US and es-ES against the Appendix texts kept in
  `e2e/support/appendix-texts.ts`. The missing-version message is checked with the item response
  rewritten by `page.route` (no row of the Minitest batch lacks a version; the import tests of 005
  cover the pipeline text itself). A phone-size context checks stacking, the full-screen modal and
  the visible countdown.
- **Language switch with choices**: `queue.spec.ts` (already switching pt-BR → en-US → es-ES with
  unsaved choices) runs against the new screen unchanged except for the selectors.
- **Rationale**: SC-001 to SC-004 are measurable in these tests; SC-002 and SC-003 need the pilot
  and are only recorded in the quickstart.

## 10. Schema and API change

- **Decision**: none. Spec 008 adds no Flyway migration, no endpoint, no link relation and no field
  to any payload; it reads `GET /api/v1/tasks/{id}/items/{position}` and writes
  `PUT …/responses/{itemId}` exactly as 007 does.
- **Rationale**: FR-005; a rollback to `0.7.0` needs nothing at the data level.
- **Alternatives considered**: returning pre-computed diffs from the API (violates "nothing new is
  stored" and adds a contract to maintain).
