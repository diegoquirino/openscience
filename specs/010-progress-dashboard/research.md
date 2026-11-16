# Research: Data collection progress dashboard

**Feature**: `010-progress-dashboard` | **Date**: 2026-11-16 | **Spec**: [spec.md](spec.md)

This research starts from User Story 9 / FR-056 of spec 001, the task/reservation model of 007,
the onboarding fields of 006, the e-mail quota of 004, and the long export of 009 (SC-001 /
SC-003 cross-check).

## 1. Versions (checked on 2026-11-16)

| Component | Version | Note |
|---|---|---|
| Spring Boot | 4.1.2 | Unchanged |
| Angular / Material | 22.2.6 | Unchanged |
| App version | **0.10.0** | Spec number in the minor digit |
| Deploy tags | `deploy/0.10.0-*` | Production only with explicit approval |

No new Maven or npm dependency.

## 2. HAL API shape

- **Decision**:
  - Entry point (admin): `vd:dashboard` ? `GET /api/v1/dashboard`.
  - That single GET returns the full snapshot (queue progress, reservations, participants,
    languages, e-mail quota) plus `self` / `curies` and, when the study has no items, a
    `vd:batches` (or `vd:request-preview`) shortcut for Import.
  - No sub-resources and no query parameters in this spec. Refresh is a second GET of the same
    link (FR-007: no mutation).
- **Rationale**: one screen, one resource; REST level 3; matches the disabled admin home entry.
- **Alternatives considered**: separate endpoints per panel (more round-trips, no need at this
  scale); Server-Sent Events (out of scope; Refresh button is enough).

## 3. Task states for progress counts (US1)

- **Decision**:
  - **free** = `task.status = 'free'` **or** (`status = 'reserved'` and `expires_at <= now`).
  - **inProgress** = `status = 'reserved'` and `expires_at > now`.
  - **completed** = `status = 'completed'`.
  - Percentage completed = `100.0 * completed / (free + inProgress + completed)` when the
    denominator is > 0; otherwise `0`. Report with one decimal place in the JSON
    (`completedPercent`).
  - Counts are emitted **per role** (1 and 2) inside each system×batch row, plus an **overall**
    first row that sums every system and batch.
  - **firstFreeQueuePosition** = minimum `task.queue_position` among free/reclaimable tasks, or
    `null` when none remain.
- **Rationale**: matches `NextTaskChooser` / `listCandidates` reclaim semantics; expired holds
  must not inflate “in progress”.
- **Alternatives considered**: counting expired reserved as in progress (misleads the researcher);
  a fourth progress bucket (spec only asks for three).

## 4. Reservations list (US2)

- **Decision**: list every `task` with `status = 'reserved'` (active or expired), ordered by
  `expires_at ASC`. Each row: `pseudonym`, `system`, `block` (`block.number`), `role`,
  `draftCount` (responses with `state = 'draft'` for that task), `expiresAt` (ISO-8601 with
  offset), `expired` (`true` when `expires_at <= now`). No name/e-mail.
- **Rationale**: FR-002 / FR-005; expired label is a presentation flag over the same row.
- **Alternatives considered**: only active reservations (would hide stalled blocks); including
  completed tasks (noise).

## 5. Participant status buckets (US3)

- **Decision** (mutually exclusive, every non-anonymized participant counted once):

  | Bucket | Rule |
  |---|---|
  | `declinedConsent` | `consent_decision = 'refused'` |
  | `ineligible` | not declined, and (`screening_answer = 'no'` or `eligible = false` after screening) |
  | `eligible` | `eligible = true` (current consent accepted, skill set, screening yes) |
  | `incomplete` | everyone else still in the study (no consent yet, or accepted but skill/screening pending) |

  Anonymized participants (`anonymized_at IS NOT NULL`) are excluded from all dashboard totals
  (they belong to 011).

- **Skill distribution**: count by `professional_skill` code among participants who have one;
  include only codes that appear, plus omit null. UI maps codes through existing i18n labels.
- **Completed tasks per pseudonym**: `COUNT` of `task.status = 'completed'` grouped by
  `participant.pseudonym`, ordered by count DESC then pseudonym ASC; `shareOfCompleted` =
  `count / totalCompletedTasks` (0 when total is 0). “Blocks completed” in the UI copy means
  these completed tasks (block×role units), matching SC wording about share of completed tasks.
- **Rationale**: no status column exists; buckets derive from 006 fields; FR-003.
- **Alternatives considered**: a persisted status enum (DDL, out of scope); counting people who
  completed ?1 block as a fifth bucket (not requested).

## 6. Languages (FR-008 / SC-003)

- **Decision**: for each of `pt-BR`, `en-US`, `es-ES` (always all three, zeros included):
  - `participantsPreferred` = count of non-anonymized participants with that
    `preferred_language`;
  - `finalResponses` = count of `response` rows with `state = 'final'` and that `language`.
  Drafts never count (spec assumption).
- **Cross-check (SC-003)**: `finalResponses` per language equals the tally of
  `response_language` in the long export of 009.

## 7. E-mail quota (FR-004)

- **Decision**: reuse `AccessCodeRepository.countAllSince(now - 24h)` and
  `ConfigRepository.dailyEmailLimit()`. Snapshot fields:
  - `emailsSentLast24h` = that count;
  - `dailyEmailLimit` = config value;
  - `emailsRemaining` = `max(0, dailyEmailLimit - emailsSentLast24h)`;
  - `lowRemaining` = `emailsRemaining < 10`.
- **Rationale**: same rolling 24 h window already enforced by `SendLimits` / `CleanupJob`; not a
  calendar day.
- **Alternatives considered**: calendar-day reset at midnight America/Sao_Paulo (would diverge
  from the live limiter).

## 8. Empty study and Import shortcut

- **Decision**: when `COUNT(item) = 0`, every progress number is zero, reservation and
  contributor lists are empty, language zeros still appear, and the HAL document includes
  `vd:batches` (same href as the entry point) so the UI can offer “Import”.
- **Rationale**: edge case in the spec.

## 9. Security and stubs already present

- **Decision**: add `/api/v1/dashboard/**` to `SecurityConfig.ADMINISTRATION`; register
  `LinkRelations.DASHBOARD`; emit `vd:dashboard` for administrators only; update the OpenAPI
  entry-point description that still says “later releases add vd:dashboard”.
- Frontend: enable the Dashboard button; route `/admin/dashboard`; shrink `@@admin.soon` to
  participants only.

## 10. SC-001 / SC-002 measurement

- **SC-001**: `DashboardTest` seeds Minitest, completes/reserves a known mix, asserts task
  totals equal `TaskRepository` counts under the §3 mapping and final-response language totals
  equal a long-export tally (or equivalent JDBC).
- **SC-002**: recorded in quickstart against staging with a 500-item / 100-participant seed when
  available; otherwise assert the API responds under 2 s in CI with Minitest and note staging
  timing in the task record.
