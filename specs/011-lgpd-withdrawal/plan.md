# Implementation Plan: Handle a data withdrawal request (LGPD)

**Branch**: `011-lgpd-withdrawal` | **Date**: 2026-11-16 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/011-lgpd-withdrawal/spec.md`

## Summary

Spec 011 turns the administrator's **Participants** menu into the LGPD withdrawal flow on top of
the participant/session model of 004–006, the queue of 007, and the exports of 009.

- **Search:** exact e-mail or exact pseudonym; card with status, counts and impact preview.
- **Keep responses:** erase name/e-mail, release reservation, drop drafts, keep finals linked to
  the pseudonym; wide export shows `anonymized`.
- **Discard responses:** also erase finals and return that person's completed tasks to `free` at
  the same `queue_position`.
- **E-mail:** confirmation in `preferred_language` before erasure; if quota is exhausted, warn
  and continue.
- **Audit:** `withdrawal_log` without PII.
- **HAL:** `vd:participants` / `vd:anonymize`; security split from `/participants/me`.
- **UI:** `/admin/participants`; Participants menu enabled.

Flyway `V2__withdrawal_log.sql`. Decisions are in [research.md](research.md).

## Technical Context

**Language/Version**: Java 25 LTS with Spring Boot 4.1.2; TypeScript with Angular 22.2.6; Node.js
26 LTS for the build

**App version**: **0.11.0**. Deploy tags: `deploy/0.11.0-*`.

**Primary Dependencies**:
- **Back end:** as in 010 (Spring HATEOAS, JDBC, springdoc-openapi, JavaMailSender).
- **Front end:** as in 010 (Angular Material, `@angular/localize`).

**Storage**: MySQL 9.7; new `withdrawal_log` table; mutate `participant` / `task` / `response` /
`access_session` / `access_code` ([data-model.md](data-model.md))

**Testing**:
- **Domain:** JUnit 5 for keep vs discard task/response rules.
- **API:** REST Assured `ParticipantWithdrawalTest`; `AuthorizationTest` / `EntryPointTest`
  updated; mail asserted via Mailpit in staging tests where applicable.
- **Front end:** Vitest for the participants screen; Playwright admin smoke + participant denied.

**Target Platform**: Cloud Run + Cloud SQL; Docker Compose `staging`; GitHub-hosted Ubuntu runners

**Project Type**: web application

**Performance Goals**: withdrawal handled under 5 minutes wall-clock for the administrator
(SC-002); interactive API under 2 s

**Constraints**:
- Admin only for search/anonymize; `/participants/me/**` remains participant-only.
- E-mail before erase when quota allows (FR-007).
- Atomic vs reserve/save/submit (FR-006).
- No new runtime dependency.
- Deploy by tag only with explicit researcher approval.

**Scale/Scope**:
- Rare operation; one administrator; one new screen; one Flyway migration.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

Constitution 2.7.1.

| Rule | How the plan meets it | Status |
|---|---|---|
| I. Stack standard; versions recorded | 0.11.0; no new dependency | Pass |
| I. MessageSource | Withdrawal mail keys in three bundles | Pass |
| I. REST level 3 | `vd:participants`, `vd:anonymize` ([contracts/participants.md](contracts/participants.md)) | Pass |
| I. OpenAPI complete | Administration tag | Pass |
| II. Domain plain Java | `domain.withdrawal` for option rules / impact counts | Pass |
| II. MySQL + REST Assured from `/api/v1` | Follow links from entry point | Pass |
| II. i18n completeness | XLIFF + mail bundles | Pass |
| III. Transactions; Flyway only | One TX; `V2__withdrawal_log.sql` | Pass |
| IV. Privacy / LGPD | Erase PII; audit without PII; Minitest in staging | Pass |
| V. Single artifact; Material | `/admin/participants` | Pass |
| Workflow: staging then tag deploy | Quickstart; `deploy/0.11.0-*` with approval | Pass |
| UTF-8 | As in 003–010 | Pass |

No violation.

**Post-design re-check**: security path split documented; quota counting includes
`withdrawal_log.email_sent`; exports unchanged (009 marker).

## Risks

| Risk | Mitigation |
|---|---|
| Security matcher steals `/participants/me` | Research §9: me/** stays PARTICIPANT; admin paths listed explicitly |
| Race with reserve/submit | FOR UPDATE on participant and their tasks |
| Quota / SMTP mid-flight | Send before mutate; SMTP failure aborts; quota exhausted continues |
| Returning completed tasks breaks CHECK | Clear all free-state columns including `submitted_at` |
| Unique e-mail after NULL | MySQL allows multiple NULL; covered by re-sign-in test |

## Project Structure

### Documentation (this feature)

```text
specs/011-lgpd-withdrawal/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   ├── participants.md
│   └── web.md
└── tasks.md
```

### Source Code (`Veredito` branch)

```text
backend/.../db/migration/V2__withdrawal_log.sql
backend/.../domain/withdrawal/
backend/.../withdrawal/          # query + service + mailer
backend/.../api/v1/ParticipantsController.java (admin)
frontend/src/app/admin/participants/
```

**Structure Decision**: mirror 010 (`domain.*` + application package + HAL controller + one admin
screen). No Option 2/3 layout.

## Complexity Tracking

> None — no constitution violations.
