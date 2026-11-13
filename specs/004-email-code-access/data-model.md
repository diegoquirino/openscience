# Data model: Sign in with name, e-mail and code

**Feature**: `004-email-code-access` | **Date**: 2026-11-13 | **Research**: [research.md](research.md)

Spec 003 already created every table that sign-in needs (`participant`, `access_code`,
`access_session`, `config`; [data-model of 003](../003-deployment-foundation/data-model.md)). Spec
004 adds one column and two indexes in `V3__access.sql` and fixes how the existing columns are
used. V1 and V2 are not edited (Principle III).

## Migration `V3__access.sql`

| Change | Why |
|---|---|
| `config.admin_name VARCHAR(120) NULL` | The administrative area shows the administrator's name (US2 scenario 1); the administrator has no participant row, so the latest name typed is kept here (research, section 8) |
| Index `access_code (created_at)` | The daily limit counts the rows of the last 24 hours across all e-mails, and the cleanup deletes the older ones (research, sections 5 and 14) |
| Index `access_session (expires_at)` | Cleanup of expired sessions (research, section 14) |

The change only adds things: the code of 003 ignores the new column and indexes, so a rollback to
the revision of 003 still works (spec 003, *Changes after V2*).

## How spec 004 uses the tables

### `access_code`

| Column | Use in 004 |
|---|---|
| `email` | Normalized e-mail (lowercase, trimmed) of the request |
| `code_hash` | `HMAC-SHA256(CODE_HASH_KEY, email + ":" + code)`, 64 hexadecimal characters; the code itself is never stored or logged |
| `created_at` | Time of the send; base of the 60-second, hourly and daily limits |
| `expires_at` | `created_at` + 10 minutes |
| `attempts` | Wrong attempts on this code; at 5 the code is dead |
| `used_at` | Set when the code is validated; a used code is dead |

**Valid code**: the most recent row of the e-mail, with `used_at IS NULL`, `attempts < 5` and
`expires_at > now`. Older rows of the same e-mail never count (edge case "two devices").

**Lifecycle**:

```text
requested --send ok--> pending --right code--> used
                       pending --wrong code (attempts 1..4)--> pending
                       pending --5th wrong code--> dead
                       pending --10 minutes--> dead
                       pending --newer code for the same e-mail--> dead
requested --send failed--> row deleted (no code becomes valid)
any row --24 hours after created_at--> deleted by the cleanup job
```

### `access_session`

| Column | Use in 004 |
|---|---|
| `token_hash` | SHA-256 of the cookie token, 64 hexadecimal characters |
| `subject` | `participant` or `administrator` |
| `participant_id` | The participant's id; `NULL` for the administrator |
| `created_at` | Time of sign-in |
| `expires_at` | `created_at` + 24 hours; never extended |

**Valid session**: a row whose `token_hash` matches the cookie and `expires_at > now`. Sign-out
deletes the row; the cleanup job deletes expired rows.

### `participant`

| Column | Use in 004 |
|---|---|
| `email` | Normalized e-mail; the unique key that identifies the person (scenario 8) |
| `name` | Latest name typed (2 to 120 characters, trimmed), updated at each sign-in (FR-005) |
| `pseudonym` | `R-` + id zero-padded to 4 digits, set once at the first sign-in (FR-006) |
| `registered_at` | Time of the first sign-in |
| `preferred_language` | Language in use at the first sign-in; changed later only by the header switch (`PUT` on `vd:me`) |

The other columns (consent, skill, screening, anonymization) stay `NULL` until specs 006 and 011.

### `config`

| Column | Use in 004 |
|---|---|
| `admin_email` | Decides who is the administrator (FR-007); compared with the normalized e-mail |
| `admin_name` | New: latest name typed by the administrator |
| `admin_preferred_language` | Language in use at the administrator's first sign-in; changed later by the header switch |
| `daily_email_limit` | Ceiling of codes sent in any 24-hour window (FR-009) |

The row is also locked (`SELECT ... FOR UPDATE`) by each code request while the limits are counted
(research, section 5).

## Validation rules (domain layer, plain Java)

| Rule | Source |
|---|---|
| Name: trimmed, 2 to 120 characters | FR-001, US1 scenario 2 |
| E-mail: trimmed and lowercased; at most 254 characters; one `@`, a non-empty local part and a domain with a dot, no spaces | FR-001, US1 scenarios 2 and 8 |
| Code: spaces and hyphens removed; then exactly 6 digits, otherwise counted as a wrong attempt | FR-002, edge case |
| Resend: refused if the latest send of the e-mail is less than 60 s old; answer the seconds left | US1 scenario 6 |
| Hourly: refused if 5 sends of the e-mail in the last 60 minutes; answer the time of the next allowed request | US1 scenario 7 |
| Daily: refused if the sends of the last 24 hours reach `daily_email_limit` | FR-009, US1 scenario 9 |
| Pseudonym: `R-%04d` of the id | FR-006, US1 scenario 10 |
| Locale: `pt-BR`, `en-US` or `es-ES`; anything else falls back to pt-BR | FR-010, spec 003 |

## Entities outside the database

| Entity | Where | Rule |
|---|---|---|
| Session cookie | Browser, `veredito-session` | `HttpOnly`, `Secure`, `SameSite=Lax`, `Path=/`, `Max-Age=86400`; the token is never readable by JavaScript |
| Language cookie | Browser, `veredito-lang` | `pt-BR`, `en-US` or `es-ES`; `Path=/`, one year, `SameSite=Lax`; set by the root page and by each Angular build |
| Unsaved input | Browser tab, `sessionStorage` key `veredito-unsaved:<route>` | Written just before a language switch, restored and deleted after the reload (FR-013) |
