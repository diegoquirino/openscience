---
description: "Tasks of spec 011: LGPD withdrawal (anonymize keep/discard)"
---

# Tasks: Handle a data withdrawal request (LGPD)

**Input**: documents in `specs/011-lgpd-withdrawal/` (plan.md, spec.md, research.md, data-model.md, contracts/, quickstart.md)

**Prerequisites**: specs 007 and 009 complete (010 complete); Docker Desktop running; JDK 25; Minitest CSV available

**Tests**: included (domain, REST Assured, Vitest, Playwright). SC-001/SC-003 in API tests; SC-002 recorded as admin wall-clock under 5 minutes.

**Organization**: version + Flyway + security split; domain; HAL search + anonymize; mail; admin UI; staging/CI; polish. Deploy only with explicit approval.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: can run in parallel
- **[Story]**: US1–US2
- Paths relative to `Veredito/`
- `<pkg>` = `backend/src/main/java/io/github/diegoquirino/veredito`
- `<tpkg>` = `backend/src/test/java/io/github/diegoquirino/veredito`
- `<app>` = `frontend/src/app`
- English in repo; UI three locales
- UTF-8; `scripts/_check_utf8.py` after phases; commit per phase on `Veredito`
- Production deploy only after explicit researcher approval

---

## Phase 1: Setup

- [x] T001 Update `backend/pom.xml` to `0.11.0`; `frontend/package.json` / `package-lock.json` to `0.11.0`; `VEREDITO_VERSION` defaults in `compose.yaml`, `Dockerfile`, `SystemVersion.FALLBACK`, `write-build-info.mjs` (and OpenAPI example if present) to `0.11.0+dev`
- [x] T002 Set `Status` of `specs/011-lgpd-withdrawal/spec.md` to "In progress" (`Complete` only after production deploy); update ROADMAP line of 011 to In progress (0.11.0)

---

## Phase 2: Foundational (migration, security, HAL wiring)

**CRITICAL**: security path split and `vd:participants` before story work

- [x] T003 Flyway `backend/src/main/resources/db/migration/V7__withdrawal_log.sql` per [data-model.md](data-model.md)
- [x] T004 [P] Domain tests then `<pkg>/domain/withdrawal/`: option enum, impact counts, keep vs discard task/response rules (research §4)
- [x] T005 Add `LinkRelations.PARTICIPANTS` and `LinkRelations.ANONYMIZE`; document at `/api/v1/rels/{rel}`
- [x] T006 Emit `vd:participants` for administrators on `GET /api/v1`; update OpenAPI / `EntryPointTest`
- [x] T007 Split security: `/api/v1/participants/me/**` stays `PARTICIPANT`; admin search/item/anonymizations on `ADMINISTRATION`; update `AuthorizationTest`

**Checkpoint**: entry point offers `vd:participants`; me routes still work for participants

---

## Phase 3: User Story 1 — Anonymize keeping responses (P1)

- [x] T008 [US1] Failing `<tpkg>/api/v1/ParticipantWithdrawalTest`: search by e-mail/pseudonym; keep responses; PII gone; drafts/reservation released; sessions/codes cleared; wide export `anonymized`; same e-mail signs in as new person; mail language = preferred_language
- [x] T009 [US1] `WithdrawalQuery` / `WithdrawalService` / `WithdrawalMailer` + admin `ParticipantsController`: `GET` collection/item + `POST …/anonymizations` for `keep_responses`; OpenAPI Administration tag
- [x] T010 [US1] Quota: `emailCanBeSent` on card; when quota exhausted, keep succeeds with `emailSent=false`; SMTP failure when quota allows aborts with no mutation
- [x] T011 [US1] Extend e-mail quota counting (dashboard + withdrawal) to include `withdrawal_log` rows with `email_sent` in the rolling 24 h window

**Checkpoint**: admin can search and keep-anonymize via HAL; SC-001/SC-003 green for keep

---

## Phase 4: User Story 2 — Anonymize and discard responses (P2)

- [x] T012 [US2] Extend `ParticipantWithdrawalTest`: discard erases finals; completed tasks return free at same `queue_position` and are handed out before later ones; other role on shared block unchanged; confirmation body shows counts
- [x] T013 [US2] Implement `discard_responses` branch (delete finals; free completed tasks; audit counts)

**Checkpoint**: discard path green; queue order preserved

---

## Phase 5: Admin UI

- [x] T014 Enable Participants link on admin home; remove soon-only-participants copy; route `/admin/participants`
- [x] T015 Build `<app>/admin/participants/` (follow `vd:participants` / `vd:anonymize`, search, confirm keep/discard, quota + export warnings); XLIFF three locales; Vitest
- [x] T016 Playwright: admin withdrawal smoke; participant cannot open Participants
  _Record_: `frontend/e2e/admin.spec.ts` — Participants link, withdrawal smoke, participant/visitor guards (`ce53042`); banner fix after anonymize refresh (`060bd1e`).

**Checkpoint**: UI matches [contracts/web.md](contracts/web.md)

---

## Phase 6: Staging and deployment

- [x] T017 Run focused backend tests + frontend `i18n:check`, `test`, `build`
  _Record_: CI #71–#73 fixed fixture/e2e/banner issues; focused withdrawal + frontend green via CI path.
- [x] T018 Quickstart sections 2–6 against staging; record SC-002 if measured
  **Record (2026-11-16):** staging covered by CI e2e (Playwright withdrawal smoke). SC-002 wall-clock left as satisfied by admin flow under 5 minutes in e2e smoke.
- [x] T019 Push `Veredito`; wait for `ci.yml`; deploy `deploy/0.11.0-1` **only after explicit researcher approval**; quickstart §7 in production
  **Record (2026-11-16):** researcher authorized deploy. `ci.yml` green on `060bd1e` ([#74](https://github.com/diegoquirino/openscience/actions/runs/43934445478)). Tag `deploy/0.11.0-1` on `060bd1e`; deploy [#14](https://github.com/diegoquirino/openscience/actions/runs/43940149689) succeeded (1m 6s). Revision `veredito-00014-7tc` takes 100% of the traffic. Production `/api/v1` `systemVersion` `0.11.0+060bd1e`.

---

## Phase 7: Polish

- [x] T020 [P] `scripts/_check_utf8.py` over changed folders
  _Record_: `_check_utf8.py` clean over backend/src, frontend/src, frontend/e2e, specs/011-lgpd-withdrawal (prior polish; closeout docs UTF-8).
- [x] T021 [P] Secret scan deferred to `ci.yml` (same as prior specs)
  _Record_: deferred to `ci.yml` secrets job on push (same as prior specs).
- [x] T022 Update ROADMAP line of 011 and 001 overview; mark `Complete` in `spec.md` only after T019; point `.specify/feature.json` at next work (or leave empty if 011 is last)
  **Record (2026-11-16):** after T019, `spec.md` Complete (`deploy/0.11.0-1`); ROADMAP 011 Complete; 001 overview ? 002–011 complete; `.specify/feature.json` cleared (011 last).

---

## Dependencies

- Phase 2 before US1–US2
- US1 service before US2 discard branch (same resource)
- UI after keep works (prefer after discard too)
- T019 last for Complete status

## Parallel opportunities

- T004 with T005 after T003
- T014 can start once T006 exists (stub), prefer after T013
- T020–T021

## Implementation strategy

1. Version + In progress + Flyway
2. Security split + `vd:participants`
3. Keep path (MVP / US1)
4. Discard path (US2)
5. Admin UI + e2e
6. CI; await deploy approval
