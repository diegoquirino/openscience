---

description: "Tasks of spec 007: receive, answer and submit blocks without collision"
---

# Tasks: Receive, answer and submit blocks without collision

**Input**: documents in `specs/007-block-queue/` (plan.md, spec.md, research.md, data-model.md, contracts/, quickstart.md)

**Prerequisites**: spec 005 complete; Docker Desktop running; JDK 25 on the researcher's machine; a confirmed Minitest batch available for staging/e2e

**Tests**: included. Constitution Principle II requires them (domain unit tests, Testcontainers, REST Assured from `/api/v1`, bundle parity, XLIFF completeness, Playwright). SC-001 to SC-006 are checked by automated tests.

**Organization**: tasks grouped by user story; each story can be tested on its own once the foundation is ready.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: can run in parallel (different files, no dependency on an incomplete task)
- **[Story]**: story of the spec (US1 to US6)
- Paths are relative to the root of the `Veredito` branch (the `Veredito` folder)
- `<pkg>` = `backend/src/main/java/io/github/diegoquirino/veredito`; `<tpkg>` = `backend/src/test/java/io/github/diegoquirino/veredito`; `<app>` = `frontend/src/app`
- Code, comments, identifiers and commit messages in English; user-facing texts in pt-BR, en-US and es-ES, following `specs/GLOSSARY.md`; every new Angular text gets its pt-BR and es-ES targets in `frontend/src/locale/messages.pt-BR.xlf` and `messages.es-ES.xlf` in the same task
- No e-mail, code or token in logs, test output, chat or screenshots; test e-mails under `example.com`
- Every text file in UTF-8; after each phase, run `scripts/_check_utf8.py` (in `TRABALHO DE TESE/scripts/`) over the changed folders
- Commit at the end of each phase on the `Veredito` branch

---

## Phase 1: Setup

**Purpose**: version bump and packages

- [x] T001 Update `backend/pom.xml` to version `0.7.0` (research section 1; no new Maven dependency). Update `frontend/package.json` / `package-lock.json` to `0.7.0`; default `VEREDITO_VERSION` in `compose.yaml`, `SystemVersion.FALLBACK`, `Dockerfile` and `frontend/scripts/write-build-info.mjs` to `0.7.0+dev`
- [x] T002 [P] Create `package-info.java` in `<pkg>/queue` (services that coordinate reservation, drafts and submission) and `<pkg>/domain/queue` (plain-Java next-task and response-code rules)

---

## Phase 2: Foundational

**Purpose**: migration, problem types, domain chooser, repositories, eligibility stub and entry-point link used by every story

**CRITICAL**: no story starts before this phase is complete

- [x] T003 Create `backend/src/main/resources/db/migration/V5__task_queue_lookup.sql` exactly as in `specs/007-block-queue/data-model.md` (`INDEX ix_task_status_queue ON task (status, queue_position)`)
- [x] T004 Extend `<tpkg>/persistence/MigrationTest.java`: V5 applied once; index exists; two-container structure comparison of earlier specs still passes
- [x] T005 [P] Add the Problem Details types of `contracts/problem-details.md` to `<pkg>/api/ProblemType.java` (`not-eligible` 403, `reservation-expired` 409, `block-incomplete` 409, `task-already-submitted` 409, `task-conflict` 409) with `problem.<slug>.title` / `.detail` in the three message bundles; add `label.end.<outcome>` keys for `no-items-yet`, `database-complete`, `none-available-for-you`, `more-available`
- [x] T006 [P] Write domain tests first, then classes, in `<pkg>/domain/queue/` and `<tpkg>/domain/queue/` (plain Java; ArchUnit of 003 guards it):
  - `NextTaskChooser`: given ordered candidates + blocks already held by the participant + `now`, return the first free or expired (`expires_at <= now`) task whose `block_id` is not held; cover US1 scenarios 1–4 and Appendix E style sequences; resume of own valid reservation is handled outside the chooser (service returns it before calling)
  - `ResponseCodes`: accept only `LOW`/`HIGH` and the eight `maintenance_operation` codes; reject others with stable reason codes for `invalid-request`
- [x] T007 Create / extend JdbcClient repositories in `<pkg>/persistence/`:
  - `TaskRepository`: `findReservedByParticipant(participantId)`, `lockCandidatesForUpdateSkipLocked(now)`, `reserve(taskId, participantId, reservedAt, expiresAt)`, `renewExpiry(taskId, expiresAt)`, `complete(taskId, submittedAt)`, `findByIdForUpdate(taskId)`, `countByStatus`, `existsAnyItem`
  - `ResponseRepository`: `upsertDraft(...)`, `deleteByTaskId(taskId)`, `countByTaskId`, `listByTaskId`, `finalizeAll(taskId)`, `findByTaskAndItem`
  - `ItemRepository`: `listByBlockOrdered(blockId)`, `findByBlockAndPosition(blockId, position)`
  - Extend `ConfigRepository`: `reservationMinutes()` under lock when reserving/renewing
  Integration test `<tpkg>/persistence/TaskQueueRepositoriesTest.java` (Testcontainers): insert free tasks (reuse 005 helpers or confirm fixture), reserve with SKIP LOCKED, upsert draft, finalize
- [x] T008 Implement `<pkg>/queue/EligibilityGate.java` with always-true stub `AlwaysEligibleGate` (Assumption until 006); injectable so tests can force `false` for `not-eligible`
- [x] T009 Add participant link `vd:next-task` ? `/api/v1/tasks/reservations` on `GET /api/v1` in `<pkg>/api/v1/EntryPointController.java` when the caller is a participant and the gate is true; document relations in `LinkRelations` / `RelationController` (`contracts/tasks.md`)
- [x] T010 Run `.\mvnw.cmd -B verify` in `backend/`: every test of 005 still passes with V5 present

  **Record (2026-11-15):** focused suite `QueueRulesTest`, `MigrationTest`, `TaskQueueRepositoriesTest`, `MessageBundleParityTest`, `EntryPointTest`, `DomainIsolationTest` green with V5.

**Checkpoint**: foundation ready — user stories can start

---

## Phase 3: User Story 1 - Receive the next block from the queue (Priority: P1) ?? MVP

**Goal**: participant asks for a block and receives an exclusive reserved task (or an explicit outcome)

**Independent Test**: with 38 imported items (8 tasks), ask in sequence and in parallel; order and exclusivity as in US1 scenarios 1–7

- [x] T011 [P] [US1] Create failing `<tpkg>/api/v1/TaskReservationTest.java`: follow `vd:next-task` as participant after importing Minitest; first caller gets B1-R1 (`201`); second gets B1-R2; same caller with valid reservation gets `200` same task; empty DB ? `outcome=no-items-yet`; visitor 401 / administrator 403
- [x] T012 [P] [US1] Create failing `<tpkg>/api/v1/TaskConcurrencyTest.java` for SC-001: 30 threads reserve/save/submit against a small imported set; assert no task given to two people and nobody holds both roles of a block (also supports SC-002 when the set is exhausted)
- [x] T013 [US1] Implement `<pkg>/queue/TaskReservationService.java`: resume own reservation; else lock candidates with `FOR UPDATE SKIP LOCKED`, apply `NextTaskChooser`, reclaim expired (delete prior drafts), set holder/expiry; compute `outcome` when none (`contracts/tasks.md` research §4)
- [x] T014 [US1] Implement `<pkg>/api/v1/TaskReservationController.java` and task models: `POST /api/v1/tasks/reservations`, `GET /api/v1/tasks/{id}` per `contracts/tasks.md` (HAL links; OpenAPI tag Tasks complete)
- [x] T015 [US1] Secure `/api/v1/tasks/**` as participant-only in `<pkg>/web/SecurityConfig.java`; extend `<tpkg>/api/v1/AuthorizationTest.java` so every Tasks operation is denied to visitors and administrators
- [x] T016 [US1] Front end: routes `/label` and wiring from `/welcome` to start labeling; call `vd:next-task`; on task redirect to first item placeholder or done with `outcome`; XLIFF units; `npm run i18n:check`
- [ ] T017 [US1] Playwright e2e: after Minitest import, two participants receive different first tasks; empty DB shows `no-items-yet`

**Checkpoint**: US1 testable alone (reserve only)

---

## Phase 4: User Story 2 - Answer the items as drafts (Priority: P1)

**Goal**: each item answer is saved as a draft; reload resumes; countdown renews on save

**Independent Test**: answer part of a block, reload, continue; save the same response twice; countdown present

- [ ] T018 [P] [US2] Create failing `<tpkg>/api/v1/TaskDraftTest.java`: `GET` item; `PUT` draft renews `reservationExpiresAt`; double PUT keeps one row; incomplete codes ? `invalid-request`; foreign task ? 404; other role's data never appears
- [ ] T019 [P] [US2] Extend `<tpkg>/api/LocalizedProblemDetailsTest.java` with the new 007 types in the three locales (force gate false for `not-eligible`)
- [ ] T020 [US2] Implement `<pkg>/queue/DraftService.java`: lock task; holder or renew-own-expired; upsert draft (`change_impact`/`maintenance_operation` NOT NULL codes, comment ? 1,000, `language` from request locale, `saved_at`); renew expiry by `reservation_minutes`
- [ ] T021 [US2] Implement `GET /api/v1/tasks/{id}/items/{position}` and `PUT /api/v1/tasks/{id}/responses/{itemId}` on `<pkg>/api/v1/TaskItemController.java` / `TaskResponseController.java` with OpenAPI and HAL-FORMS `vd:save-response`
- [ ] T022 [US2] Front end: simple item screen `<app>/label/item/` (`/label/items/:position`) with fields, impact/operation radios, comment, counter, Continue disabled until both answered; countdown helper from `reservationExpiresAt` + `Date` header (SC-005); sessionStorage restore of unsaved choices; XLIFF
- [ ] T023 [US2] Playwright: answer two items, reload, land on third with drafts kept (SC-004)

**Checkpoint**: US1 + US2 deliver reserve and draft labeling

---

## Phase 5: User Story 5 - Release abandoned blocks (Priority: P1)

**Goal**: reservation expires after configured idle time; reclaim discards prior drafts; UI warns at zero

**Independent Test**: reserve, let expire with reduced `reservation_minutes` in staging/tests, ask with another person

- [ ] T024 [P] [US5] Create failing `<tpkg>/api/v1/TaskExpiryTest.java`: lower `reservation_minutes`; A reserves without saving past expiry; B receives the task and A's drafts are gone; A's save ? `reservation-expired`; A's save when nobody took renews for A
- [ ] T025 [US5] Ensure `TaskReservationService` / `DraftService` implement reclaim and renew-own-expired paths (FR-003 to FR-005); no scheduled job
- [ ] T026 [US5] Front end: countdown reaches zero ? localized expiry warning; keep unsaved choices; on save either resume or show `reservation-expired` and offer a new block (`contracts/web.md`)
- [ ] T027 [US5] Playwright (or REST-only if timing brittle): short reservation expiry path covered; document in quickstart section 5

**Checkpoint**: expiry behaviour covered

---

## Phase 6: User Story 3 - Review and submit the whole block (Priority: P1)

**Goal**: review all answers, submit atomically to finals; incomplete refused; resubmit idempotent

**Independent Test**: try submit with pending item; submit complete block; change after submit refused

- [ ] T028 [P] [US3] Create failing `<tpkg>/api/v1/TaskSubmissionTest.java`: submit complete ? `completed` + finals; incomplete ? `block-incomplete` with `firstPendingPosition`; resubmit ? 200 no-op; save after submit ? `task-already-submitted`
- [ ] T029 [US3] Implement `<pkg>/queue/SubmissionService.java`: lock; require draft per item; finalize all; set `submitted_at` / `status=completed`; attach `outcome` for the completion screen
- [ ] T030 [US3] Implement `POST /api/v1/tasks/{id}/submission` on `<pkg>/api/v1/TaskSubmissionController.java`; expose `vd:submit` only when `answeredCount == size`
- [ ] T031 [US3] Front end: review screen `<app>/label/review/` listing choices/comments; edit navigates to item; Submit; on incomplete jump to pending position; XLIFF
- [ ] T032 [US3] Playwright: complete a small block (or last block of fixture), submit, confirm finals in DB and done navigation

**Checkpoint**: full block lifecycle works

---

## Phase 7: User Story 4 - Know what to do next (Priority: P2)

**Goal**: completion and reserve outcomes show the right end message and actions

**Independent Test**: exhaust a 2-block database with 3 people; check the three messages

- [ ] T033 [P] [US4] Extend reservation/submission tests: after last possible task for a person ? `none-available-for-you`; when every task completed ? `database-complete`; after submit with more work ? `more-available`
- [ ] T034 [US4] Front end: done screen `<app>/label/done/` with thanks, localized `label.end.*`, "Label another block" only when appropriate, "Sign out"; welcome uses the same outcomes when reserve returns no task
- [ ] T035 [US4] Playwright: exhaust a tiny imported set (or API-driven) and assert the three end states appear

**Checkpoint**: end-of-queue UX complete

---

## Phase 8: User Story 6 - Label a block in the chosen language (Priority: P2)

**Goal**: UI strings follow the locale; drafts record `language` of last save; switch keeps reservation and unsaved choices

**Independent Test**: answer part in pt-BR, switch to en-US then es-ES; check `language` per response and no lost drafts/choices (SC-006)

- [ ] T036 [P] [US6] Create failing `<tpkg>/api/v1/TaskLanguageTest.java`: saves in three locales record `language`; switch does not require renew by itself (expiry unchanged until save)
- [ ] T037 [US6] Confirm draft `language` comes from request locale resolution (not a free-form body field); item payloads untranslated
- [ ] T038 [US6] Front end: all labeling strings in XLIFF (counter, countdown warning, end messages, refusals); language switch restores unsaved choices via sessionStorage without calling save; countdown keeps remaining time (FR-016)
- [ ] T039 [US6] Playwright smoke: mid-block language switch across locales keeps drafts and reservation (SC-006)

**Checkpoint**: i18n labeling complete

---

## Phase 9: Staging rehearsal and deployment

- [ ] T040 Run quickstart sections 2 to 6 against local staging; record any fix
- [ ] T041 [P] Confirm `TaskConcurrencyTest` (SC-001) and exhaustion (SC-002) pass in CI; next-item latency smoke under 3 s where practical (SC-003)
- [ ] T042 Push to `Veredito`; wait for `ci.yml`; deploy `deploy/0.7.0-1` after the researcher's approval; quickstart section 7 in production

---

## Phase 10: Polish & Cross-Cutting Concerns

- [ ] T043 [P] Run `scripts/_check_utf8.py` over the changed Veredito folders
- [ ] T044 [P] Confirm secret scanning still clean; no new secrets introduced
- [ ] T045 Mark spec 007 complete: `Status` in `spec.md`; ROADMAP line of 007; update 001 overview status to "002 to 005 and 007 complete; 006 next"; point `.specify/feature.json` at `specs/006-consent-screening`; commit and push

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: no dependencies
- **Foundational (Phase 2)**: depends on Setup; blocks every story
- **US1 (Phase 3)**: depends on Phase 2 — MVP
- **US2 (Phase 4)**: depends on US1 (needs a reserved task)
- **US5 (Phase 5)**: depends on US1; draft reclaim paths integrate with US2
- **US3 (Phase 6)**: depends on US2 (needs drafts)
- **US4 (Phase 7)**: depends on US1 outcomes + US3 submit
- **US6 (Phase 8)**: depends on US2 (drafts + UI); can finish after US3 screens exist
- **Staging / deploy (Phase 9)**: depends on all stories
- **Polish (Phase 10)**: depends on Phase 9

### Story order versus priority

US1 ? US2 ? US5 ? US3 keeps all P1 risk (concurrency, drafts, expiry, submit) before P2 polish (end messages, language). US4 needs submit outcomes; US6 needs the item/review screens.

### Parallel Opportunities

- T001 with T002
- T005 and T006 inside Phase 2
- T011 and T012; T018 and T019; T024 with late US2; T028 with US5 wrap-up
- T043 and T044

---

## Parallel Example: Phase 2

```text
Task: "Problem types and label.end.* bundles" (T005)
Task: "Domain queue rules and tests" (T006)
```

## Parallel Example: User Story 1

```text
Task: "TaskReservationTest" (T011)
Task: "TaskConcurrencyTest" (T012)
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Phases 1 and 2
2. Phase 3 (reserve API + start-labeling navigation)
3. **Stop and validate**: quickstart reserve steps / concurrency test skeleton

### Incremental Delivery

1. Setup and foundation (V5, chooser, repos, eligibility, links)
2. US1: reserve
3. US2: drafts and simple item screen
4. US5: expiry / reclaim
5. US3: review and submit
6. US4: end messages
7. US6: language
8. Staging, concurrency proof, production tag `0.7.0-*`
9. Polish and status

---

## Notes

- V1 to V5 are immutable once deployed; later schema changes are V6+.
- No new Secret Manager secret.
- `AuthorizationTest` must deny every new Tasks operation without a participant session (and to the administrator).
- Eligibility refusals stay stubbed until 006; keep the gate as the single change point.
- No task calls models; the sequential execution rule of `ISSTA_2027` does not apply.
- Deploy tags only after explicit researcher approval (constitution workflow).
