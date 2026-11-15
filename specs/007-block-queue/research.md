# Research: Receive, answer and submit blocks without collision

**Feature**: `007-block-queue` | **Date**: 2026-11-15 | **Spec**: [spec.md](spec.md)

This research starts from the API and concurrency notes of spec 001
([research.md](../001-oracle-labeling/research.md)), the schema of spec 003 and the free queue
produced by spec 005. It only records what spec 007 decides on top of them.

## 1. Versions (checked on 2026-11-15)

| Component | Version | Note |
|---|---|---|
| Spring Boot | 4.1.2 | Unchanged |
| Angular / Material / localize | 22.2.6 | Unchanged |
| Playwright | 1.65.0 | Unchanged |
| MySQL / Testcontainers / Flyway | as in 005 | Unchanged |
| App version | 0.7.0 | Spec number in the minor digit (004 ? 0.4.x, 005 ? 0.5.0) |

No new Maven or npm dependency.

## 2. Next-task rule (domain)

- **Decision**: plain-Java package `domain.queue` with `NextTaskChooser`. Given an ordered list of
  candidate snapshots (queue position, block id, role, status, holder id, `expires_at`) plus the
  set of block ids the participant already holds (any role, reserved or completed) and `now`, it
  returns the first candidate that is (a) free, or reserved with `expires_at <= now`, and (b) not
  in a block the participant already has. Unit tests cover the Appendix E / US1 sequences (B1-R1,
  B1-R2, skip own other role, resume own reservation before choosing).
- **Rationale**: Constitution Principle II and spec 003 FR-008; the ROADMAP assumes this rule is
  testable in isolation before going live. The chooser does not talk to JDBC.
- **Alternatives considered**: choosing inside SQL only (harder to unit-test the “never both roles”
  rule without a database); a priority queue in Redis (extra service, Principle V).

## 3. Concurrency and locking

- **Decision**: follow the critical path of spec 001 research:
  1. **Reserve** (`POST /tasks/reservations`): if the participant already has a `reserved` task
     with `expires_at > now`, return it (`200`). Otherwise lock candidates with
     `SELECT … FROM task WHERE … FOR UPDATE SKIP LOCKED` in `queue_position` order, apply
     `NextTaskChooser`, then update the chosen row to `reserved` (set holder, `reserved_at`,
     `expires_at = now + reservation_minutes`). If the row was expired under another holder,
     `DELETE` that holder's drafts for the task in the same transaction. Concurrent requests get
     different rows without waiting.
  2. **Save draft**: lock the task (`FOR UPDATE`), accept only the holder or an expired reservation
     with no new holder (which renews it for the caller), upsert on (`task_id`, `item_id`), renew
     `expires_at`.
  3. **Submit**: lock the task, require a draft for every item of the block, set all responses to
     `final`, set `status = completed` and `submitted_at`. Resubmit by the same holder ? `200` no-op.
  4. **Expiry**: computed on read; no scheduled job.
- **Rationale**: Principle III (short transactions + constraints); `SKIP LOCKED` matches multi-
  instance Cloud Run; unique (`block_id`, `participant_id`) is the last line of defence.
- **Alternatives considered**: `LockService`-style application lock (does not span instances);
  `SELECT FOR UPDATE` without `SKIP LOCKED` (serializes contenders and hurts SC-001).

## 4. Outcomes when no task is handed out

- **Decision**: `POST /api/v1/tasks/reservations` never uses bare `204`. When nothing is reserved
  it returns `200` with a HAL body `{ "outcome": "<slug>" }` and no task `Location`:
  | Outcome | When |
  |---|---|
  | `no-items-yet` | There is no `item` row (and therefore no task) |
  | `database-complete` | Every task is `completed` |
  | `none-available-for-you` | Free/expired tasks remain, but none the participant can label |
- **Rationale**: FR-011 needs three distinct end messages; `204` cannot carry the distinction.
  Spec 001's `204` sketch is refined here without changing the resource path (FR-018).
- **Alternatives considered**: three Problem Details types (wrong for success); a separate
  `GET /tasks/availability` (extra round-trip).

After a successful submit, the completed task representation includes the same `outcome` hint for
the completion screen (recomputed for that participant), so the UI can offer "Label another block"
only when a later reserve would succeed.

## 5. Eligibility gate (stub until 006)

- **Decision**: one application service `EligibilityGate.isEligible(participantId)`. In 007 the
  implementation returns `true` for every existing participant with a session. Entry point offers
  `vd:next-task` to every signed-in participant. Spec 006 replaces the implementation and adds the
  refusal scenarios; the reserve / save / submit services always call the gate first and answer
  `403` `not-eligible` when it returns false.
- **Rationale**: Assumption of the spec; keeps a single change point for 006.
- **Alternatives considered**: hard-coding “always eligible” in each controller (scattered);
  delaying 007 until 006 (contradicts ROADMAP risk order).

## 6. API shape (FR-018)

- **Decision**:
  | Operation | Request | Success |
  |---|---|---|
  | Reserve | `POST /api/v1/tasks/reservations` via `vd:next-task` | `201` + `Location` + task (new); `200` + task (existing reservation); `200` + `{ outcome }` (none) |
  | Read task | `GET /api/v1/tasks/{id}` | `200` task with items summary, `reservationExpiresAt`, `reservationMinutes`, links |
  | Read item | `GET /api/v1/tasks/{id}/items/{position}` | `200` item fields (untranslated) + current draft if any; never the other role |
  | Save draft | `PUT /api/v1/tasks/{id}/responses/{itemId}` via `vd:save-response` | `200` task (new expiry) |
  | Submit | `POST /api/v1/tasks/{id}/submission` via `vd:submit` | `200` completed task + `outcome` |
- **HAL-FORMS**: a reserved task owned by the caller exposes `vd:items`, `vd:save-response`,
  `vd:review`; `vd:submit` only when every item has a draft. A completed task exposes `self` and,
  when `outcome` allows, `vd:next-task` again.
- **Rationale**: matches glossary paths and the 001 research table; state decides links.
- **Alternatives considered**: RPC-style `/label/next` (breaks level 3); embedding all item bodies
  in the task (heavy payloads for SC-003).

## 7. Countdown and server time

- **Decision**: every task representation that is `reserved` includes `reservationExpiresAt`
  (UTC ISO-8601) and `reservationMinutes`. Angular computes remaining ms as
  `expiresAt - (Date.now() + (serverDateHeader - Date.now()))`, i.e. corrects the device clock from
  the HTTP `Date` header of the last task/draft response, and recomputes after every save. On zero
  it shows the localized expiry warning without clearing the unsaved choices of the current item.
- **Rationale**: FR-013, FR-016 scenario 3, SC-005.
- **Alternatives considered**: trusting `Date.now()` alone (fails SC-005); a WebSocket tick (extra
  complexity).

## 8. Language and codes

- **Decision**: draft body accepts only the English codes of Appendices B.1/B.2; `language` is taken
  from the request locale (`Content-Language` / Accept-Language resolution already used by Message-
  Source), not from a free-form field. Switching locale reloads the SPA under the new prefix; the
  tab restores unsaved impact/operation/comment via `sessionStorage` keyed by task id + position
  (FR-016, FR-078). Imported item text is shown as stored.
- **Rationale**: constitution language rules; FR-015–FR-017; SC-006.
- **Alternatives considered**: letting the client send `language` in the body (spoofable, drifts from
  UI); translating item text (forbidden).

## 9. Schema change

- **Decision**: additive `V5__task_queue_lookup.sql` adds
  `INDEX ix_task_status_queue (status, queue_position)` to speed candidate scans under lock. No
  column changes; `task` / `response` CHECKs from `V1` already match FR-006–FR-008.
- **Rationale**: rollback to 0.5.x keeps working; index is safe to leave if rolled forward.
- **Alternatives considered**: no migration (full table scans under contention); changing CHECK
  constraints (unnecessary).

## 10. Staging reservation minutes

- **Decision**: staging (and tests) may `UPDATE config SET reservation_minutes = N` (within 1–240)
  to exercise expiry; production stays at 20. No admin UI in this spec.
- **Rationale**: Assumption of the spec; keeps production behaviour untouched.
- **Alternatives considered**: a feature flag endpoint (out of scope); sleeping 20 minutes in CI
  (too slow).

## 11. Simple item screen vs 008

- **Decision**: 007 ships a minimal item view: system, use case, `kind`, `field`, `origin`,
  `target` (preserving line breaks), impact radios, operation radios, comment, counter, countdown.
  No use-case modal, no Appendix B explanations beyond short option labels. Spec 008 replaces the
  component without changing request/response JSON.
- **Rationale**: ROADMAP risk order; Assumption of the spec.
- **Alternatives considered**: implementing the rich screen now (blocks 007 on 008 scope).
