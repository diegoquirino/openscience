# Implementation Plan: Receive, answer and submit blocks without collision

**Branch**: `007-block-queue` | **Date**: 2026-11-15 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/007-block-queue/spec.md`

## Summary

Spec 007 opens the participant labeling loop on top of the access layer of 004 and the free
`task` rows created by 005.

- **Reserve:** an eligible participant follows `vd:next-task` to `POST /api/v1/tasks/reservations`.
  One short transaction returns their current reserved task, or reserves the first free/expired
  candidate they can label (`SELECT … FOR UPDATE SKIP LOCKED`), discarding another person's drafts
  when reclaiming an expired task. Outcomes with no task (`no-items-yet`, `database-complete`,
  `none-available-for-you`) are explicit HAL bodies, not ambiguous empty responses.
- **Draft / submit:** `PUT` saves one draft (English codes + `language` + optional comment) and
  renews the reservation; `POST …/submission` finalizes every draft at once when the block is
  complete. Idempotent retries never duplicate rows.
- **UI:** a deliberately simple item / review / done flow under `/label/*`, with a countdown derived
  from `reservationExpiresAt` and the response `Date` header (device clock corrected). Spec 008
  will replace the item layout without changing what is stored.
- **Eligibility:** a single backend gate; until 006 exists it accepts every participant with a
  session (Assumption).

No new domain tables: `task` and `response` already exist in `V1`. Decisions are in
[research.md](research.md).

## Technical Context

**Language/Version**: Java 25 LTS with Spring Boot 4.1.2; TypeScript with Angular 22.2.6; Node.js
26 LTS for the build

**Primary Dependencies**:
- **Back end:** as in 005 (Spring WebMVC, HATEOAS HAL-FORMS, JDBC, Security, Flyway, springdoc).
  No new Maven dependency.
- **Front end:** as in 005 (Angular Material, `@angular/localize`).

**Storage**: MySQL 9.7; existing `task` / `response` / `item` / `block` / `config.reservation_minutes`;
additive index migration `V5__task_queue_lookup.sql` ([data-model.md](data-model.md))

**Testing**:
- **Domain:** JUnit 5 for `NextTaskChooser` queue scenarios (Appendix E / US1) and response codes.
- **API:** REST Assured task flow, concurrency (SC-001, 30 threads), expiry with reduced
  `reservation_minutes`, language on drafts (SC-006), localized Problem Details; authorization
  denies Tasks operations to visitors and the administrator.
- **Front end:** Vitest for countdown and draft restore; Playwright for reserve → answer → review →
  submit in staging (with shortened reservation for expiry).

**Target Platform**: as in 003–005 (Cloud Run + Cloud SQL; Docker Compose `staging`; GitHub-hosted
Ubuntu runners)

**Project Type**: web application (one container: Spring Boot API + three Angular builds)

**Performance Goals**:
- Interactive API calls under 2 s (Principle V); next item under 3 s p95 (SC-003).
- Countdown skew ≤ 2 s vs server expiry with a device clock 5 minutes off (SC-005).

**Constraints**:
- Atomic reserve / save / submit; never both roles of a block to the same person (DB unique + chooser).
- Eligibility stub until 006; refusal-by-eligibility tested with 006.
- Staging may lower `reservation_minutes` for expiry tests.
- Item data never translated; category codes stay English (`LOW`/`HIGH`, `KEEP`, …).
- V5 additive so a Cloud Run rollback to 005 still starts.

**Scale/Scope**:
- Dozens of concurrent participants (SC-001 = 30).
- 6 Task resources under `/api/v1` (FR-018); simple participant screens replacing `/welcome` as the
  post-sign-in destination when a session is eligible.
- New Problem Details types for reservation / draft / submission conflicts.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

Constitution 2.7.1.

| Rule | How the plan meets it | Status |
|---|---|---|
| I. Stack standard and current; versions recorded | No new dependency; Spring Boot 4.1.2 / Angular 22.2.6 unchanged (research §1) | Pass |
| I. MessageSource for backend messages | New Problem Details types and end-message keys in the three bundles | Pass |
| I. REST level 3 | `/tasks`, `/tasks/reservations`, `/tasks/{id}`, items, responses, submission; links `vd:next-task`, `vd:reserve`, `vd:save-response`, `vd:submit`, …; HAL-FORMS ([contracts/tasks.md](contracts/tasks.md)) | Pass |
| I. OpenAPI complete; Swagger admin-only in production | Every new operation under tag Tasks; caught by `ApiDocumentationCompletenessTest` | Pass |
| II. Domain in plain Java with unit tests | `domain.queue.NextTaskChooser` (+ response code helpers); ArchUnit isolation | Pass |
| II. MySQL + REST Assured from `/api/v1` following links | Tests start at entry point → `vd:next-task` → reserve / save / submit | Pass |
| II. i18n completeness | New XLIFF units; bundle parity; localized Problem Details (SC-006) | Pass |
| III. Transactions; Flyway only | Reserve / save / submit each one transaction with row locks; `V5` additive only | Pass |
| IV. Staging/tests/pilot only Minitest | Uses batches imported under 005 rules; no new import path | Pass |
| V. Single artifact; Material; API under 2 s | Material labeling screens; SKIP LOCKED avoids multi-instance locks waiting | Pass |
| Language: English in repo; UI three locales; data untranslated | FR-015–FR-017; research §8 | Pass |
| Workflow: staging before production; deploy by tag | Quickstart then `deploy/0.7.0-*` | Pass |
| All text files UTF-8 | As in 003–005 | Pass |

No violation.

**Post-design re-check** (after data-model and contracts):
- **Services and secrets:** none added.
- **Coverage:** every Tasks operation is denied to visitors and administrators in
  `AuthorizationTest`; SC-001/SC-002 covered by automated API tests; eligibility remains one
  injectable gate for 006.

Pass.

## Risks

| Risk | Mitigation |
|---|---|
| Concurrent reserve races | `FOR UPDATE SKIP LOCKED` + unique (`block_id`, `participant_id`); 30-thread test |
| Expired task reclaim discards drafts too late | Discard drafts inside the same reserve transaction that assigns the new holder |
| Device clock skew on countdown | Client uses `Date` header vs `reservationExpiresAt` (SC-005) |
| Eligibility stub leaks past 006 | Single `EligibilityGate` interface; 006 replaces the always-true implementation |

## Project Structure

### Documentation (this feature)

```text
specs/007-block-queue/
|-- plan.md
|-- research.md
|-- data-model.md
|-- quickstart.md
|-- contracts/
|   |-- tasks.md
|   |-- problem-details.md
|   `-- web.md
|-- checklists/requirements.md
`-- tasks.md                 # phase 2 (/speckit-tasks; not created here)
```

### Source Code (root of the `Veredito` branch; new or changed in 007)

```text
backend/
|-- pom.xml                              # version 0.7.0
`-- src/
    |-- main/
    |   |-- java/io/github/diegoquirino/veredito/
    |   |   |-- domain/queue/            # NextTaskChooser, ResponseCodes, Eligibility (stub policy)
    |   |   |-- queue/                   # TaskReservationService, DraftService, SubmissionService,
    |   |   |                            # EligibilityGate (always true until 006)
    |   |   |-- persistence/             # TaskRepository / ResponseRepository / ItemRepository
    |   |   |                            # extensions for lock, drafts, submit
    |   |   |-- api/                     # ProblemType gains the new slugs
    |   |   |-- api/v1/                  # Task controllers + models; EntryPointController gains
    |   |   |                            # vd:next-task for eligible participants; LinkRelations
    |   |   `-- config/                  # (none required beyond existing)
    |   `-- resources/
    |       |-- db/migration/V5__task_queue_lookup.sql
    |       `-- messages*.properties     # problem.* and label.end.*
    `-- test/
        |-- java/.../domain/queue/
        `-- java/.../api/v1/TaskReservationTest, TaskDraftTest, TaskSubmissionTest,
                             TaskConcurrencyTest, TaskExpiryTest, TaskLanguageTest

frontend/src/app/
|-- label/                               # request / item / review / done components
|-- welcome/                             # redirects eligible participants into /label
|-- core/                                # countdown helper; unsaved-choice restore (FR-016/FR-078)
frontend/src/locale/messages.pt-BR.xlf, messages.es-ES.xlf
frontend/e2e/                            # labeling happy path (+ expiry with short reservation)
```

**Structure Decision**: same two-folder web application. Package `queue` coordinates locking and
persistence; `domain.queue` stays free of Spring and holds the next-task rule.

## Complexity Tracking

No violations.
