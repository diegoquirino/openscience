# Data model: Data collection progress dashboard

**Feature**: `010-progress-dashboard` | **Date**: 2026-11-16 | **Research**: [research.md](research.md)

Spec 010 is **read-only**. It adds no table, column, constraint, index or Flyway migration.

## Tables used (all from V1–V5)

| Table | Columns used |
|---|---|
| `item` | `id`, `system`, `block_id`, `batch_id` |
| `block` | `id`, `batch_id`, `number`, `size` |
| `import_batch` | `id`, `file_name`, `imported_at` |
| `task` | `id`, `block_id`, `role`, `queue_position`, `status`, `participant_id`, `reserved_at`, `expires_at`, `submitted_at` |
| `response` | `task_id`, `item_id`, `state`, `language` |
| `participant` | `id`, `pseudonym`, `preferred_language`, `consent_decision`, `professional_skill`, `screening_answer`, `eligible`, `anonymized_at` |
| `config` | `daily_email_limit` |
| `access_code` | `created_at` (ledger for the rolling 24 h send count) |

Name and e-mail columns of `participant` are **never** selected for this feature (FR-005).

## Logical snapshot (API resource)

| Entity | Identity | Notes |
|---|---|---|
| Dashboard snapshot | singleton for the administrator | Regenerated on each GET |
| Progress row | `system` + `batchId` (overall row uses `system=null`, `batchId=null`) | Items, blocks, per-role free/inProgress/completed, `completedPercent` |
| Reservation row | `task.id` | Pseudonym, block, role, drafts, expiry, `expired` |
| Participant totals | singleton section | Status buckets + skill map |
| Contributor row | `pseudonym` | Completed-task count and share |
| Language row | `pt-BR` \| `en-US` \| `es-ES` | Preferred participants + final responses |
| E-mail quota | singleton section | Sent last 24 h, limit, remaining, `lowRemaining` |

## Progress aggregation sketch

```text
item
  JOIN block ON block.id = item.block_id
  JOIN task ON task.block_id = block.id
GROUP BY item.system, block.batch_id, task.role
-- free / inProgress / completed per research §3 (evaluate expires_at against "now")
```

Item and block counts per system×batch: `COUNT(DISTINCT item.id)` and
`COUNT(DISTINCT block.id)`.

## Reservation sketch

```text
task
  JOIN block ON block.id = task.block_id
  JOIN item ON item.block_id = block.id   -- pick system via MIN(item.system) / any item
  JOIN participant ON participant.id = task.participant_id
  LEFT JOIN response ON response.task_id = task.id AND response.state = 'draft'
WHERE task.status = 'reserved'
GROUP BY task.id
ORDER BY task.expires_at
```

## Validation rules

- Snapshot is consistent with a single `Instant now` for all expiry and quota windows.
- `completedPercent` uses the §3 formula; never NaN.
- Language array always length 3 in fixed order: `pt-BR`, `en-US`, `es-ES`.
- Anonymized participants omitted from every person-related tally.
