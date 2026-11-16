# Data model: Export the labels

**Feature**: `009-exports` | **Date**: 2026-11-15 | **Research**: [research.md](research.md)

Spec 009 is **read-only**. It adds no table, column, constraint, index or Flyway migration.

## Tables used (all from V1)

| Table | Columns used |
|---|---|
| `item` | `id`, `system`, `edit_key`, `uc`, `kind`, `field`, `origin`, `target`, `origin_version`, `target_version`, `origin_usecase`, `target_usecase`, `block_id`, `position` |
| `block` | `id`, `number` |
| `task` | `id`, `block_id`, `role`, `status`, `participant_id`, `reserved_at`, `submitted_at` |
| `response` | `task_id`, `item_id`, `state`, `change_impact`, `maintenance_operation`, `comment`, `language` |
| `participant` | `id`, `pseudonym`, `name`, `email`, `preferred_language`, `consent_form_id`, `consent_language`, `consent_decided_at`, `professional_skill`, `anonymized_at` |
| `consent_form` | `id`, `version` |

## Join sketch (per item, both roles)

```text
item
  JOIN block ON block.id = item.block_id
  LEFT JOIN task t1 ON t1.block_id = item.block_id AND t1.role = 1 AND t1.status = 'completed'
  LEFT JOIN response r1 ON r1.task_id = t1.id AND r1.item_id = item.id AND r1.state = 'final'
  LEFT JOIN participant p1 ON p1.id = t1.participant_id
  LEFT JOIN consent_form cf1 ON cf1.id = p1.consent_form_id
  LEFT JOIN task t2 … role = 2 … (same pattern)
ORDER BY block.number, item.position
```

Draft responses (`state = draft`) and non-completed tasks are excluded by the join predicates.

## Format mapping

### Wide (Appendix C) — one row per item, all systems

Item columns + for each role N with a final response: name/email (or `anonymized`), consent
cell, professional_skill code, impact/operation codes, comment, `language` as
`rater_N_response_language`. Incomplete role ? those nine cells blank.

### Long — one row per final response

See research §7 header. `block` = `block.number`. No name/email columns.

### Completo — one row per item of one `system`

Eleven Appendix D columns (exact stored strings) + five columns per role (pseudonym, impact,
operation, comment, response language). Incomplete role ? five blanks. No PII.

### Rater N — one row per item of one `system`

Nine pipeline columns. Label cells blank when role N is incomplete. No neutralization.

## Entities (logical only)

| Entity | Identity | Notes |
|---|---|---|
| Export index | singleton for the administrator | Lists systems and download links |
| Wide / long / completo / rater file | derived | Not stored; regenerated on each GET |

## Integrity rules enforced at read time

| Rule | Source |
|---|---|
| Only `final` responses | FR-001 |
| Codes unchanged English identifiers | FR-008 |
| Captured text not translated | FR-009 |
| Neutralization only wide/long | FR-006 |
| No name/email in long, rater, completo | FR-005 |
