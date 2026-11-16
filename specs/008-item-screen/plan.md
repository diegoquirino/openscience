# Implementation Plan: Item screen with differences, context and explained questions

**Branch**: `008-item-screen` | **Date**: 2026-11-15 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/008-item-screen/spec.md`

## Summary

Spec 008 replaces the deliberately simple item screen of spec 007 with the rich screen of User
Story 4 of spec 001. It is a **front-end only** change: the item API, the draft/submit API and the
stored codes of 007 do not change (FR-005); there is no new table, column, endpoint or Problem
Details type.

- **Edit, side by side:** `origin` and `target` are shown with word-by-word highlighting computed
  in the browser at display time (a plain-TypeScript diff of words and punctuation: removed words
  struck through on the "Before" side, added words highlighted on the "After" side). The layout
  depends on `kind`: `MOD` two blocks, `ADD` only "Added text", `DEL` only "Removed text", `META`
  "<field>: before / after" with a localized "(none)" for an empty side.
- **Context:** each version has a "View full use case (<version>)" button that opens a Material
  dialog with the whole use case in a monospaced font, the lines marked `<- edited` highlighted
  (marker hidden), scrolled to the first one. A use case that holds only the pipeline text
  "(the use case does not exist in this version)" shows a localized message instead.
- **Questions:** impact (Appendix B.1) and operation (Appendix B.2) keep their single-choice radio
  groups and stored codes, now with the definition/examples or "when to choose" text of every
  option and the precedence rule of the operation question.
- **Always-visible clock:** the counter "x of n" and the reservation countdown of 007 stay on the
  page and are repeated in the header of the dialog; the last 2 minutes use the alert color and
  are announced to screen readers once.
- **Language:** every label, question, explanation and message is an XLIFF unit in the three
  locales; use cases, edits, system and `uc` are shown exactly as captured. A language switch
  keeps restoring the unsaved impact, operation and comment (the mechanism of 007, FR-078).

Decisions are in [research.md](research.md). The component contract is in
[contracts/web.md](contracts/web.md).

## Technical Context

**Language/Version**: TypeScript with Angular 22.2.6 (application version `0.8.0`); Java 25 LTS with
Spring Boot 4.1.2 is untouched except for the version number; Node.js 26 LTS for the build

**Primary Dependencies**:
- **Front end:** as in 007 (Angular Material, `@angular/localize`). The dialog uses
  `MatDialog` (Angular Material) and the screen-reader announcement uses `LiveAnnouncer`
  (Angular CDK); both are already in the installed packages. **No new npm or Maven dependency.**
- **Back end:** unchanged.

**Storage**: none new. The item screen reads the item and the draft that 007 already returns and
saves through the same `vd:save-response` link. No Flyway migration ([data-model.md](data-model.md)).

**Testing**:
- **Vitest (unit):** word diff (`MOD` tokens, whitespace and punctuation, empty sides, size
  guard), use case text parsing (marker stripping, indentation, missing-version text, first edited
  line), layout/header rules per `kind`, countdown urgency and one-time announcement, the item
  component (all kinds, `(none)`, "no text difference", unchanged saved codes) and the dialog.
- **Playwright (staging):** one item of each `kind` in the three locales against the texts of
  Appendices A, B.1 and B.2; both modals (title, highlighted lines, scroll, Close/Esc/outside,
  focus return, choices kept); the missing-version message; narrow screen (stacked blocks,
  full-screen modal, no horizontal scroll, countdown visible with the modal open); language switch
  that restores unsaved choices on the new screen.
- **Back end:** only the existing suite (`mvnw verify`) must stay green with the version bump.

**Target Platform**: as in 003–007 (Cloud Run + Cloud SQL; Docker Compose `staging`;
GitHub-hosted Ubuntu runners)

**Project Type**: web application (one container: Spring Boot API + three Angular builds)

**Performance Goals**:
- The use case modal opens in less than 1 second (SC-001): the text is already in the item
  payload, so opening is local rendering only.
- Diffing the longest text of the Minitest batch (a few thousand characters) takes well under
  50 ms in the browser; a size guard avoids a quadratic blow-up on pathological input.

**Constraints**:
- What is stored and what the API returns do not change (FR-005); the codes stay `LOW`/`HIGH` and
  `KEEP`, `UPDATE`, … whatever the interface language (FR-011).
- Data is never translated (FR-009); the pipeline text is replaced only for display (FR-010).
- Every user-facing string exists in `messages.pt-BR.xlf` and `messages.es-ES.xlf`
  (`npm run i18n:check`).
- Onboarding of 006 is still required before labeling (route guards unchanged).
- Countdown and counter always visible, also on a phone and with the modal open (FR-007).
- No production deploy without the researcher's explicit approval.

**Scale/Scope**: one screen (`/label/items/:position`) and one dialog; the review screen of 007
keeps its simple list.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

Constitution 2.7.1.

| Rule | How the plan meets it | Status |
|---|---|---|
| I. Stack standard and current; versions recorded | No new dependency; Spring Boot 4.1.2 / Angular 22.2.6 unchanged (research §1) | Pass |
| I. REST level 3 / OpenAPI | No API change; the screen follows `vd:save-response` and `vd:items` exactly as 007 | Pass |
| II. Tests per layer | Vitest for diff, text and layout rules; Playwright for kinds, modals, locales; `mvnw verify` unchanged | Pass |
| II. i18n completeness | New XLIFF units in pt-BR and es-ES; `npm run i18n:check`; Playwright compares the three locales with Appendices A and B | Pass |
| III. Transactions; Flyway only | No schema change; no Flyway migration | Pass |
| IV. Staging/tests/pilot only Minitest | Tests and the rehearsal use the Minitest batch (and mocked item payloads) only | Pass |
| V. Single artifact; Material; accessibility | Material components (dialog, radio, button); keyboard, focus return and screen-reader announcement specified | Pass |
| Language: English in repo; UI three locales; data untranslated | FR-008 to FR-011; research §7 | Pass |
| Workflow: staging before production; deploy by tag | Quickstart, then `deploy/0.8.0-*` only after explicit approval | Pass |
| All text files UTF-8 | As in 003–007 | Pass |

No violation.

**Post-design re-check** (after data-model and contracts):
- **Services and secrets:** none added.
- **Stored data:** the unit tests assert that the `PUT` body is exactly the 007 body
  (`changeImpact`, `maintenanceOperation`, `comment`), whatever the locale.
- **Data fidelity:** use case texts, `origin` and `target` reach the DOM unchanged (the marker is
  removed and the missing-version text replaced only in the dialog's display model).

Pass.

## Risks

| Risk | Mitigation |
|---|---|
| Word diff is noisy or slow on long, near-identical use cases | Diff only `origin`/`target` (short), never the whole use cases; token-level LCS with a size guard that falls back to "whole text changed" |
| The dialog hides the countdown | The counter and the countdown are rendered in the dialog header, from the same clock as the page |
| Option explanations inside the radio label break the accessible names used by earlier e2e (`radio` named "Alto") | The explanation lives outside the label and is linked with `aria-describedby`; the radio name stays the short label |
| Language switch loses choices on the new screen | Same `keepOnSwitch` registration of 007; Playwright switches pt-BR → en-US → es-ES with choices typed |
| Pipeline text appears in the middle of a use case | Replaced only when the whole text, ignoring surrounding whitespace, is the pipeline text (spec edge case) |
| Spec 008 changes something stored by accident | Unit test on the `PUT` body; no change in `label.service.ts` request code |

## Project Structure

### Documentation (this feature)

```text
specs/008-item-screen/
|-- plan.md
|-- research.md
|-- data-model.md
|-- quickstart.md
|-- contracts/
|   `-- web.md
|-- checklists/requirements.md
`-- tasks.md
```

### Source Code (root of the `Veredito` branch; new or changed in 008)

```text
backend/
|-- pom.xml                              # version 0.8.0 (only change)
`-- src/main/java/.../config/SystemVersion.java    # FALLBACK 0.8.0+dev, and the example in EntryPointController

frontend/
|-- package.json / package-lock.json     # version 0.8.0
|-- scripts/write-build-info.mjs         # default 0.8.0+dev
|-- e2e/
|   |-- item-screen.spec.ts              # kinds, locales, modals, narrow screen (new)
|   |-- support/appendix-texts.ts        # Appendices A, B.1, B.2 in three locales (new)
|   |-- playwright.config.ts             # new project after `queue`
|   `-- queue.spec.ts                    # adapted to the new screen
`-- src/
    |-- app/
    |   |-- core/labels/
    |   |   |-- item-labels.ts           # Appendix A: kind and field labels, "(none)", missing-version text
    |   |   `-- question-texts.ts        # Appendices B.1 and B.2: statements, explanations, precedence rule
    |   `-- label/item/
    |       |-- word-diff.ts             # tokens, LCS, segments (+ spec)
    |       |-- usecase-text.ts          # lines, markers, indentation, missing-version detection (+ spec)
    |       |-- item-layout.ts           # blocks per kind, header, identical-text rule (+ spec)
    |       |-- item-clock.ts            # remaining time, urgency, one-time announcement (+ spec)
    |       |-- item-progress.component.ts   # "x of n" + countdown, used by the page and the dialog
    |       |-- usecase-dialog.component.ts  # the modal (+ spec)
    |       |-- item.component.ts/html/scss  # rebuilt screen (+ spec)
    `-- locale/messages.pt-BR.xlf, messages.es-ES.xlf
```

**Structure Decision**: the same two-folder web application. The new logic is plain TypeScript
(`word-diff`, `usecase-text`, `item-layout`) so it is unit-tested without the DOM; the components
only render it.

## Complexity Tracking

No violations.
