# Data model: LGPD withdrawal

**Feature**: `011-lgpd-withdrawal` | **Date**: 2026-11-16 | **Research**: [research.md](research.md)

## Existing entities (unchanged DDL except new table)

### `participant`

| Column | Role in 011 |
|---|---|
| `id`, `pseudonym` | Stable identity after anonymization |
| `name`, `email` | Cleared to NULL on withdrawal |
| `anonymized_at` | Set to operation time; CHECK already enforces NULL name/email iff set |
| `preferred_language` | Locale of the confirmation e-mail; kept (not PII contact) |

Re-registration with the same e-mail creates a **new** row (new pseudonym): unique `email`
allows multiple NULLs in MySQL.

### `task` / `response`

| Option | `task` | `response` |
|---|---|---|
| `keep_responses` | Release `reserved` ? `free`; leave `completed` | Delete drafts; keep finals |
| `discard_responses` | Release `reserved` and return this participant's `completed` ? `free` (clear holder timestamps; keep `queue_position`) | Delete drafts and finals of this participant |

Other role on the same block is untouched.

### `access_session` / `access_code`

All sessions of the participant id are deleted. All access codes for the e-mail are deleted
while the address is still known.

## New table: `withdrawal_log`

Flyway `V7__withdrawal_log.sql`.

| Column | Type | Notes |
|---|---|---|
| `id` | BIGINT PK AI | |
| `occurred_at` | DATETIME(6) NOT NULL | Audit timestamp |
| `pseudonym` | VARCHAR(16) NOT NULL | Copy at withdrawal time |
| `option_code` | VARCHAR(20) NOT NULL | `keep_responses` \| `discard_responses` (`option` is reserved in MySQL) |
| `email_sent` | BOOLEAN NOT NULL | Whether the confirmation e-mail was sent |
| `final_responses_discarded` | INT NOT NULL | 0 when keep |
| `tasks_returned` | INT NOT NULL | Completed tasks returned to the queue; 0 when keep |
| `participant_id` | BIGINT NULL | FK to `participant(id)`; retained for support |

No name or e-mail columns. CHECK on `option`.

## Validation rules

- Search requires exactly one of `email` or `pseudonym`.
- `POST …/anonymizations` rejected with `409` if already anonymized.
- `option` must be one of the two enums (`400`/`422` otherwise).
- Withdrawal is atomic with respect to reserve / save / submit (row locks).

## State transitions

```text
active participant
  --[anonymize keep]--> anonymized (responses kept, reserved released)
  --[anonymize discard]--> anonymized (responses gone, own completed tasks free again)
```

Anonymized is terminal for that row's personal data; no "deanonymize".
