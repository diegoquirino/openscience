# Data model: Receive, answer and submit blocks without collision

**Feature**: `007-block-queue` | **Date**: 2026-11-15 | **Research**: [research.md](research.md)

Spec 003 already created `task` and `response` (and `block` / `item` / `config`) in `V1`/`V2`.
Spec 005 fills free tasks on import. Spec 007 **updates** those rows on reserve / draft / submit
and adds only a lookup index in `V5__task_queue_lookup.sql`.

## Migration `V5__task_queue_lookup.sql`

Additive only (rollback to the revision of 005 must keep working):

```sql
CREATE INDEX ix_task_status_queue ON task (status, queue_position);
```

No change to columns or CHECK constraints of `task` or `response`.

## How spec 007 uses the tables

### `task` (read / update)

| Transition | Rule |
|---|---|
| `free` ? `reserved` | Reserve sets `participant_id`, `reserved_at = now`, `expires_at = now + config.reservation_minutes` |
| `reserved` (expired, other holder) ? `reserved` (new holder) | Same fields rewritten; previous holder's drafts for this `task_id` deleted in the same transaction |
| `reserved` ? `reserved` (renew) | Save draft or reclaim-by-owner updates `expires_at` only (and may set holder if reclaiming own expired row still free of a new holder) |
| `reserved` ? `completed` | Submit sets `submitted_at = now`; `expires_at` may stay as last renewed value (CHECK allows it on completed) |
| Resume | Participant with own `reserved` and `expires_at > now` receives the same row; never a second reservation |

`queue_position` never changes (FR-014: expired tasks return to their original position by becoming
candidates again, not by reordering).

Unique (`block_id`, `participant_id`) rejects assigning both roles to the same person even if the
chooser were wrong.

### `response` (insert / update)

| Rule | Detail |
|---|---|
| Key | Unique (`task_id`, `item_id`); upsert on save |
| Draft | `state = draft`; both codes required; `comment` optional ? 1,000; `language` = UI locale of the save; `saved_at = now`; `participant_id` = holder |
| Final | Submit sets every draft of the task to `final` in one statement; no further participant writes |
| Isolation | Queries for a task never join the other role's responses (FR-010) |
| Discard | On reclaim of an expired task, `DELETE FROM response WHERE task_id = ?` before the new holder starts |

### `item` / `block` (read only)

Item screen and review read `system`, `uc`, `kind`, `field`, `origin`, `target`, versions and use-
case texts as stored. Block `size` drives the counter ("x of n").

### `config` (read)

| Field | Use |
|---|---|
| `reservation_minutes` | Written into every new/renewed `expires_at` and returned as `reservationMinutes` |
| (staging only) | Tests may lower the value temporarily; production default remains 20 |

### `participant` (read)

Used for session ? `participant_id`, eligibility gate input, and FK on `task` / `response`. Consent
and screening columns stay unused by the 007 gate.

## State machine (task)

```text
free --> reserved --> completed
          ^    |
          |    +-- (expiry on read: still "reserved" until reclaimed or renewed)
          |
          +-- reclaim expired by another participant (drafts cleared)
```

## Validation rules (domain / service, not new CHECKs)

| Rule | Source |
|---|---|
| Never both roles of a block for one person | FR-002; chooser + unique constraint |
| Draft requires `LOW`/`HIGH` and one of the eight operations | FR-006, Appendices B.1/B.2 |
| Comment ? 1,000 characters | FR-008 |
| Submit only with a draft for every item of the block | FR-006 |
| Save/submit only by reservation holder (or renew own expired) | FR-004 |
| `language` ? {`pt-BR`,`en-US`,`es-ES`} | FR-015; existing CHECK |

## Entities (spec vocabulary)

- **Task (block-role):** free, reserved (owner, start, expiry) or completed (owner, start, submission).
- **Response:** one participant's choice for one item in one role; draft or final; records
  `language` of the last save.
