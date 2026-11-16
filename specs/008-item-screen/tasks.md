---

description: "Tasks of spec 008: item screen with differences, context and explained questions"
---

# Tasks: Item screen with differences, context and explained questions

**Input**: documents in `specs/008-item-screen/` (plan.md, spec.md, research.md, data-model.md, contracts/, quickstart.md)

**Prerequisites**: specs 005, 006 and 007 complete; Docker Desktop running; JDK 25 on the researcher's machine; a confirmed Minitest batch available for staging/e2e

**Tests**: included. Constitution Principle II requires them (unit tests, XLIFF completeness, Playwright). SC-001 and SC-004 are checked by automated tests; SC-002 and SC-003 need the pilot.

**Organization**: tasks grouped by user story; each story can be tested on its own once the foundation is ready. Spec 008 is front-end only: no migration, no endpoint, nothing stored changes (FR-005).

## Format: `[ID] [P?] [Story] Description`

- **[P]**: can run in parallel (different files, no dependency on an incomplete task)
- **[Story]**: story of the spec (US1 to US4)
- Paths are relative to the root of the `Veredito` branch (the `Veredito` folder)
- `<app>` = `frontend/src/app`; `<item>` = `frontend/src/app/label/item`
- Code, comments, identifiers and commit messages in English; user-facing texts in pt-BR, en-US and es-ES, following `specs/GLOSSARY.md` and Appendices A, B.1 and B.2 of spec 001; every new Angular text gets its pt-BR and es-ES targets in `frontend/src/locale/messages.pt-BR.xlf` and `messages.es-ES.xlf` in the same task
- No e-mail, code or token in logs, test output, chat or screenshots; test e-mails under `example.com`
- Every text file in UTF-8; after each phase, run `scripts/_check_utf8.py` (in `TRABALHO DE TESE/scripts/`) over the changed folders
- Commit at the end of each phase on the `Veredito` branch
- Production deploy only after explicit researcher approval (never part of the implementation run)

---

## Phase 1: Setup

**Purpose**: version bump and status

- [x] T001 Update `backend/pom.xml` to version `0.8.0` (research section 1; no new Maven or npm dependency). Update `frontend/package.json` / `package-lock.json` to `0.8.0`; default `VEREDITO_VERSION` in `compose.yaml`, `SystemVersion.FALLBACK` (and the example in `EntryPointController`), `Dockerfile` and `frontend/scripts/write-build-info.mjs` to `0.8.0+dev` _Record: version 0.8.0 in pom, package.json/lock, compose, Dockerfile, SystemVersion, EntryPointController example, write-build-info_
- [x] T002 Set `Status` of `specs/008-item-screen/spec.md` to "In progress" (stays so until the production deploy; `Complete` only after it) _Record: spec.md Status is "In progress"_

---

## Phase 2: Foundational

**Purpose**: the plain-TypeScript rules, localized texts and clock used by every story

**CRITICAL**: no story starts before this phase is complete

- [ ] T003 [P] Write tests first, then `<item>/word-diff.ts`: `diffWords(origin, target)` with tokens of words, punctuation and whitespace, LCS, merged segments, the size guard (research section 2). Tests: exact reconstruction of both sides, a replaced word, only spaces or only punctuation changed, line breaks kept, one side empty, identical texts, guard fallback
- [ ] T004 [P] Write tests first, then `<item>/usecase-text.ts`: `parseUseCase(text)` returns `{ missing, lines, firstEditedIndex }`; strips the trailing `<- edited` marker, counts indentation (tab = 4), detects the pipeline text only when it is the whole text ignoring surrounding whitespace (research section 4)
- [ ] T005 [P] Create `<app>/core/labels/item-labels.ts` with the Appendix A labels (`kind`: `MOD`, `ADD`, `DEL`, `META`; `field`: the 11 known values), the localized "(none)" and "(this use case does not exist in this version)" units, and a `unknown value is returned as is` rule (FR-009, FR-010); unit test of the fallbacks and of the text constants
- [ ] T006 [P] Create `<app>/core/labels/question-texts.ts` with the impact statement and explanations (Appendix B.1), the operation statement, the eight explanations and the precedence rule (Appendix B.2, order `FLAG > REMOVE > MERGE > SPLIT > CREATE > UPDATE > REASSIGN > KEEP`); unit test that every code has an explanation and that the precedence order lists each code once
- [ ] T007 Move the clock logic of `LabelItemComponent` into `<item>/item-clock.ts` (`remaining`, `countdown`, `expired`, `urgent` for the last 120 seconds, one announcement through the CDK `LiveAnnouncer`, re-armed when time goes above 120 s) and create `<item>/item-progress.component.ts` ("x of n" + countdown, `role="timer"`, `.counter`, alert color when `urgent`); unit tests with fake timers for urgency, the single announcement and re-arming
- [ ] T008 [P] Create `frontend/e2e/support/appendix-texts.ts` with the Appendix A labels, the B.1 statement and explanations, the B.2 statement, explanations and precedence rule, "(none)" and the missing-version message in pt-BR, en-US and es-ES, copied from `specs/001-oracle-labeling/spec.md`

**Checkpoint**: foundation ready — user stories can start

---

## Phase 3: User Story 1 - Read the edit with the differences highlighted (Priority: P1) 🎯 MVP

**Goal**: the rater sees what changed without comparing texts in their head

**Independent Test**: one item of each `kind` on a computer and on a phone

- [ ] T009 [P] [US1] Write tests first, then `<item>/item-layout.ts`: `buildItemLayout(item)` per `kind` (`MOD`, `ADD`, `DEL`, `META`, unknown), header strings, `identical` flag, empty sides (research section 3)
- [ ] T010 [US1] Rebuild `<item>/item.component.ts/html/scss`: progress bar with `ItemProgressComponent` (sticky on narrow screens), header, edit blocks with `<del>`/`<ins>` segments, the "No text difference in this part" note, stacking below 600 px; keep the 007 behavior of draft restore, save, review navigation and expiry warning untouched
- [ ] T011 [US1] Update `<item>/item.component.spec.ts` for the new markup: all four kinds, `(none)` for an empty `META` side, the "no text difference" note, `PUT` body identical to 007, counter and countdown still present
- [ ] T012 [US1] Extract and translate the new units (`item.kind.*`, `item.field.*`, `item.block.*`, `item.none`, `item.noDifference`, `item.progress.*`) with `scripts/_add_xlf_units_008.py`; `npm run i18n:check`
- [ ] T013 [US1] Add the Playwright project `item` (after `queue`) and `frontend/e2e/item-screen.spec.ts` for the four kinds on desktop and phone: header, layout per kind, highlighted words, stacked blocks and no horizontal scroll on 360 px

**Checkpoint**: US1 testable alone (rich edit view, simple questions as in 007)

---

## Phase 4: User Story 2 - Look up the full use case in each version (Priority: P1)

**Goal**: a button per version opens the whole use case at the edited line

**Independent Test**: open both modals in an item of each `kind`, close them in each way and check that the choices are still there

- [ ] T014 [P] [US2] Write tests first, then `<item>/usecase-dialog.component.ts`: title pattern, header with progress, lines with indentation and highlight, no marker in the DOM, scroll to the first edited line, localized missing-version message, Close button
- [ ] T015 [US2] Wire the two "View full use case" buttons in the item component (`MatDialog`, full screen below 600 px, restore focus, `Esc`, backdrop); the choices of the item are untouched when it closes; component test that the choices survive open and close
- [ ] T016 [US2] Extract and translate the units of the dialog (`item.usecase.*`, `item.missingUsecase`); `npm run i18n:check`
- [ ] T017 [US2] Playwright: both modals for one item of each `kind` (title with versions, highlighted lines, no marker text, scrolled to the first edited line), closing with Close, `Esc` and a click outside, focus back on the button, choices kept; full-screen dialog on 360 px with wrapped lines keeping the indentation and counter and countdown visible

**Checkpoint**: US1 + US2 deliver the edit and its context

---

## Phase 5: User Story 3 - Answer with the options explained (Priority: P1)

**Goal**: each question brings the definition and examples of each option, and the precedence rule

**Independent Test**: check the texts against Appendices B.1 and B.2 and answer an item

- [ ] T018 [US3] Rebuild the question blocks of `<item>/item.component.html`: statement, radios with the short label, the explanation as `aria-describedby` description, the precedence rule with localized labels; keyboard navigation of radios, buttons and dialogs; component tests that the stored codes and comment are exactly those of 007 for every locale-independent choice
- [ ] T019 [US3] Extract and translate the units (`item.impact.*`, `item.operation.*`); `npm run i18n:check`
- [ ] T020 [US3] Playwright: the impact and operation statements, every option explanation and the precedence rule against `appendix-texts.ts` in one locale, answering both questions and Continue stores the English codes (read back over the API); the radios are still found by their short names

**Checkpoint**: US1 to US3 deliver the whole screen in one locale

---

## Phase 6: User Story 4 - See the item screen in the chosen language (Priority: P1)

**Goal**: every text follows the interface language; data stays as captured

**Independent Test**: one item of each `kind`, an item whose previous version does not exist and a `META` item with an empty side, in each locale; open both modals; language switch with unsaved choices

- [ ] T021 [US4] Playwright (`item-screen.spec.ts`): for pt-BR, en-US and es-ES check labels, titles, buttons, statements, explanations, precedence rule and `(none)` against `appendix-texts.ts`; the system, use case name, `origin`, `target` and use case texts equal the API values in every locale; the stored codes equal across locales
- [ ] T022 [US4] Playwright: an item whose `target_usecase` is the pipeline text (item response rewritten with `page.route`) shows the localized message in the modal in each locale without highlighting; an unknown `field` shows its original name in every locale
- [ ] T023 [US4] Adapt `frontend/e2e/queue.spec.ts` and `frontend/e2e/support/participant.ts` to the new screen; the language switch pt-BR → en-US → es-ES keeps restoring the unsaved impact, operation and comment and the reservation

**Checkpoint**: all four stories work in the three locales

---

## Phase 7: Staging rehearsal and deployment

- [ ] T024 Run `.\mvnw.cmd -B verify` in `backend/`; `npm ci`, `npm run i18n:check`, `npm test`, `npm run build` in `frontend/`
- [ ] T025 Run quickstart sections 2 to 6 against local staging (rebuild the image, `down -v`, `npm run e2e`); record any fix
- [ ] T026 Push to `Veredito`; wait for `ci.yml`; deploy `deploy/0.8.0-1` **only after the researcher's explicit approval**; quickstart section 8 in production (not part of the implementation run)

---

## Phase 8: Polish & Cross-Cutting Concerns

- [ ] T027 [P] Run `scripts/_check_utf8.py` over the changed Veredito folders
- [ ] T028 [P] Run secret scanning (`gitleaks`, as in `ci.yml`); no new secrets introduced
- [ ] T029 Update the ROADMAP line of 008 and the status line of the 001 overview ("implemented, awaiting deploy"); mark `Complete` in `spec.md` only after T026

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: no dependencies
- **Foundational (Phase 2)**: depends on Setup; blocks every story
- **US1 (Phase 3)**: depends on Phase 2 — MVP
- **US2 (Phase 4)**: depends on US1 (buttons live in the edit layout)
- **US3 (Phase 5)**: depends on US1 (same component); independent of US2
- **US4 (Phase 6)**: depends on US1 to US3 (checks everything in three locales)
- **Staging / deploy (Phase 7)**: depends on all stories
- **Polish (Phase 8)**: depends on Phase 7 (T029 completion note only after T026)

### Parallel Opportunities

- T003 to T006 and T008 are independent files; T007 depends on the 007 component only
- T009 with T010 start (layout first)
- T014 with the end of T012
- T027 and T028

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Phases 1 and 2
2. Phase 3 (rich edit view)
3. **Stop and validate**: one item of each `kind` on desktop and phone

### Incremental Delivery

1. Setup and foundation (diff, text parsing, labels, clock)
2. US1: edit view
3. US2: modals
4. US3: explained questions
5. US4: three-locale proof, queue e2e adapted
6. Staging, CI, production tag `0.8.0-*` after approval
7. Polish and status

---

## Notes

- No Flyway migration; the last one stays `V5__task_queue_lookup.sql`.
- The review screen of 007 is not changed by this spec.
- Nothing computed for display is stored or sent; the `PUT` body is the 007 body.
- No task calls models; the sequential execution rule of `ISSTA_2027` does not apply.
- Deploy tags only after explicit researcher approval (constitution workflow).
