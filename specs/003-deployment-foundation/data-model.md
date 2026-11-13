# Data model: Deploy Veredito and prepare the database

**Feature**: `003-deployment-foundation` | **Date**: 2026-11-12 | **Research**: [research.md](research.md)

Spec 003 creates every table of the data model of spec 001
([research.md](../001-oracle-labeling/research.md#data-model-mysql)) in the migration
`V1__create_schema.sql`, and the default configuration in `V2__insert_default_config.sql`
(FR-004, FR-005, FR-018). Later specs only add what is new, in their own migrations (V3 on).

## Conventions

| Item | Rule |
|---|---|
| Engine and character set | InnoDB, `utf8mb4`; default collation `utf8mb4_0900_ai_ci` |
| Exact comparison | `utf8mb4_0900_bin` for `item.system`, `item.edit_key`, e-mails and hashes |
| Keys | `id BIGINT AUTO_INCREMENT PRIMARY KEY` (except `config`); foreign keys without cascading actions (MySQL forbids referential actions on columns used in `CHECK`) |
| Instants | `DATETIME(6)` in UTC; the JDBC driver runs with `connectionTimeZone=UTC` and `preserveInstants=true`; the API sends ISO 8601 with `Z` |
| Locales | `CHAR(5)` with `CHECK (... IN ('pt-BR', 'en-US', 'es-ES'))` |
| Codes | `VARCHAR` with `CHECK` on the English values (FR-019); display labels live only in the translation files and message bundles |
| Text lengths | `VARCHAR(n)` counts characters; `TEXT` holds up to 65,535 bytes (5,000 characters of `utf8mb4` use at most 20,000); `MEDIUMTEXT` holds the 40,000-character use cases |
| Migration history | Flyway's `flyway_schema_history` records each migration applied, its checksum and when (US3 scenario 4) |
| Changes after V2 | Expand first, contract later: a migration must not break the previous version of the code (rollback of Cloud Run) |

## Tables

### `import_batch`

One row per confirmed import (spec 005).

| Column | Type | Rule |
|---|---|---|
| `id` | `BIGINT` | PK |
| `file_name` | `VARCHAR(255)` | NOT NULL |
| `imported_at` | `DATETIME(6)` | NOT NULL |
| `included_count` | `INT` | NOT NULL, `>= 0` |
| `ignored_count` | `INT` | NOT NULL, `>= 0` |
| `error_count` | `INT` | NOT NULL, `>= 0` |

### `block`

One row per block of up to `config.block_size` items; never changes after creation.

| Column | Type | Rule |
|---|---|---|
| `id` | `BIGINT` | PK |
| `batch_id` | `BIGINT` | NOT NULL, FK `import_batch` |
| `number` | `INT` | NOT NULL, **unique**; 1, 2, 3, ... in queue order (labels `B1-R1`, `B1-R2`) |
| `size` | `TINYINT` | NOT NULL, `>= 1` |

### `item`

One row per imported edit (columns of Appendix D of spec 001).

| Column | Type | Rule |
|---|---|---|
| `id` | `BIGINT` | PK |
| `system` | `VARCHAR(100)` bin | NOT NULL |
| `edit_key` | `VARCHAR(500)` bin | NOT NULL |
| `uc` | `VARCHAR(255)` | NOT NULL |
| `kind` | `VARCHAR(4)` | NOT NULL, `ADD`, `DEL`, `MOD`, `META` |
| `field` | `VARCHAR(100)` | NOT NULL |
| `origin` | `TEXT` | NULL; at most 5,000 characters (checked by the import) |
| `target` | `TEXT` | NULL; at most 5,000 characters |
| `origin_version` | `VARCHAR(20)` | NOT NULL |
| `target_version` | `VARCHAR(20)` | NOT NULL |
| `origin_usecase` | `MEDIUMTEXT` | NOT NULL; at most 40,000 characters |
| `target_usecase` | `MEDIUMTEXT` | NOT NULL; at most 40,000 characters |
| `batch_id` | `BIGINT` | NOT NULL, FK `import_batch` |
| `block_id` | `BIGINT` | NOT NULL, FK `block` |
| `position` | `TINYINT` | NOT NULL, `>= 1` |

Constraints: **unique** (`system`, `edit_key`); **unique** (`block_id`, `position`);
`CHECK (origin IS NOT NULL OR target IS NOT NULL)`. The per-kind rules of FR-052 (empty `origin`
in `MOD`/`DEL`, empty `target` in `MOD`/`ADD`) stay in the domain validation of spec 005, which
reports them per row. The lengths of `system` (100), `edit_key` (500), `uc` (255) and `field` (100)
are not in FR-052; spec 005 refuses longer values as a row error instead of letting the insert
fail. In Minitest the longest values are far below these limits. `SYSTEM` is a reserved word in
MySQL 8 and later, so every SQL statement, in V1 and in later code, writes the column as `` `system` ``.

### `task`

One row per block and role (block-role); the queue order is `queue_position`.

| Column | Type | Rule |
|---|---|---|
| `id` | `BIGINT` | PK |
| `block_id` | `BIGINT` | NOT NULL, FK `block` |
| `role` | `TINYINT` | NOT NULL, `1` or `2` |
| `queue_position` | `INT` | NOT NULL, **unique** |
| `status` | `VARCHAR(9)` | NOT NULL, `free`, `reserved`, `completed`; default `free` |
| `participant_id` | `BIGINT` | NULL, FK `participant` |
| `reserved_at` | `DATETIME(6)` | NULL |
| `expires_at` | `DATETIME(6)` | NULL |
| `submitted_at` | `DATETIME(6)` | NULL |

Constraints: **unique** (`block_id`, `role`); **unique** (`block_id`, `participant_id`), the
database guarantee that nobody takes both roles of a block (several `NULL`s are allowed);
state consistency:

- `free`: `participant_id`, `reserved_at`, `expires_at` and `submitted_at` are `NULL`;
- `reserved`: `participant_id`, `reserved_at` and `expires_at` are set, `submitted_at` is `NULL`
  (an expired reservation keeps this state until someone else takes the task; expiry is computed
  on read, research 001);
- `completed`: `participant_id`, `reserved_at` and `submitted_at` are set.

### `response`

One row per item in a role; draft until the block is submitted, then final.

| Column | Type | Rule |
|---|---|---|
| `id` | `BIGINT` | PK |
| `task_id` | `BIGINT` | NOT NULL, FK `task` |
| `item_id` | `BIGINT` | NOT NULL, FK `item` |
| `participant_id` | `BIGINT` | NOT NULL, FK `participant` |
| `state` | `VARCHAR(5)` | NOT NULL, `draft` or `final` |
| `change_impact` | `VARCHAR(4)` | NOT NULL, `LOW` or `HIGH` |
| `maintenance_operation` | `VARCHAR(8)` | NOT NULL, `KEEP`, `UPDATE`, `REMOVE`, `CREATE`, `MERGE`, `SPLIT`, `REASSIGN`, `FLAG` |
| `comment` | `VARCHAR(1000)` | NULL; stored as typed |
| `language` | `CHAR(5)` | NOT NULL, locale; UI language of the last save (FR-018) |
| `saved_at` | `DATETIME(6)` | NOT NULL |

Constraint: **unique** (`task_id`, `item_id`). A response is only saved with both questions
answered (scenario 16 of User Story 3 of spec 001), so both codes are `NOT NULL`.

### `participant`

One row per person (identified by e-mail until anonymized).

| Column | Type | Rule |
|---|---|---|
| `id` | `BIGINT` | PK |
| `pseudonym` | `VARCHAR(16)` | NOT NULL, **unique** |
| `name` | `VARCHAR(120)` | NULL only after anonymization |
| `email` | `VARCHAR(254)` bin | **unique**; NULL only after anonymization; stored lowercase |
| `registered_at` | `DATETIME(6)` | NOT NULL |
| `preferred_language` | `CHAR(5)` | NULL, locale (FR-018) |
| `consent_form_id` | `BIGINT` | NULL, FK `consent_form`; version of the last decision |
| `consent_decision` | `VARCHAR(8)` | NULL, `accepted` or `refused` |
| `consent_language` | `CHAR(5)` | NULL, locale of the text shown |
| `consent_decided_at` | `DATETIME(6)` | NULL |
| `professional_skill` | `VARCHAR(32)` | NULL, codes of Appendix B.4 of spec 001 |
| `professional_skill_other` | `VARCHAR(100)` | NULL; stored as typed |
| `screening_answer` | `VARCHAR(16)` | NULL, `yes_professional`, `yes_academic`, `no` |
| `eligible` | `BOOLEAN` | NULL until the screening |
| `anonymized_at` | `DATETIME(6)` | NULL |

Constraints:

- the four consent columns are all `NULL` or all set;
- `professional_skill_other` only with `professional_skill = 'other'`;
- before anonymization, `name` and `email` are set; after it, both are `NULL`
  (`(anonymized_at IS NULL) = (name IS NOT NULL AND email IS NOT NULL)`), so a search for name or
  e-mail finds nothing (spec 011, SC-001).

### `access_code`

One row per code sent (spec 004).

| Column | Type | Rule |
|---|---|---|
| `id` | `BIGINT` | PK |
| `email` | `VARCHAR(254)` bin | NOT NULL; index (`email`, `created_at`) |
| `code_hash` | `VARCHAR(128)` bin | NOT NULL; the code itself is never stored |
| `created_at` | `DATETIME(6)` | NOT NULL |
| `expires_at` | `DATETIME(6)` | NOT NULL |
| `attempts` | `TINYINT` | NOT NULL, default 0, `>= 0` |
| `used_at` | `DATETIME(6)` | NULL |

### `access_session`

One row per sign-in session (spec 004). Named `access_session` because `SESSION` is a MySQL keyword
(research, section 8).

| Column | Type | Rule |
|---|---|---|
| `id` | `BIGINT` | PK |
| `token_hash` | `VARCHAR(128)` bin | NOT NULL, **unique**; the token itself is never stored |
| `subject` | `VARCHAR(13)` | NOT NULL, `participant` or `administrator` |
| `participant_id` | `BIGINT` | NULL, FK `participant`; set if and only if `subject = 'participant'` |
| `created_at` | `DATETIME(6)` | NOT NULL |
| `expires_at` | `DATETIME(6)` | NOT NULL |

### `consent_form`

One row per consent form version (spec 006).

| Column | Type | Rule |
|---|---|---|
| `id` | `BIGINT` | PK |
| `version` | `VARCHAR(20)` | NOT NULL, **unique** |
| `reference_language` | `CHAR(5)` | NOT NULL, always `pt-BR` |
| `created_at` | `DATETIME(6)` | NOT NULL |
| `published_at` | `DATETIME(6)` | NULL; a version becomes current only when published and with the three texts (spec 006, FR-012) |

### `consent_form_text`

One row per consent form version and language (FR-018).

| Column | Type | Rule |
|---|---|---|
| `id` | `BIGINT` | PK |
| `consent_form_id` | `BIGINT` | NOT NULL, FK `consent_form` |
| `language` | `CHAR(5)` | NOT NULL, locale |
| `text` | `MEDIUMTEXT` | NOT NULL; stored as published, never translated or edited afterwards |

Constraint: **unique** (`consent_form_id`, `language`).

### `config`

A single row with the study parameters (FR-005; research, section 7).

| Column | Type | Rule | Default (V2) |
|---|---|---|---|
| `id` | `TINYINT` | PK, `CHECK (id = 1)` | `1` |
| `admin_email` | `VARCHAR(254)` bin | NOT NULL | `diegoquirino@gmail.com` |
| `admin_preferred_language` | `CHAR(5)` | NULL, locale; set at the first administrator sign-in (spec 004) | `NULL` |
| `reservation_minutes` | `SMALLINT` | NOT NULL, 1 to 240 | `20` |
| `block_size` | `TINYINT` | NOT NULL, 1 to 50 | `10` |
| `daily_email_limit` | `SMALLINT` | NOT NULL, 0 to 2,000 | `450` |
| `study_phase` | `VARCHAR(10)` | NOT NULL, `pilot` or `collection` | `pilot` |
| `study_phase_changed_at` | `DATETIME(6)` | NULL; set when the phase switches to `collection` (spec 005, FR-012) | `NULL` |
| `updated_at` | `DATETIME(6)` | NOT NULL | time of V2 |

No consent form is published by V2 (US3 scenario 2): `consent_form` and `consent_form_text` start
empty.

## Relationships

```text
import_batch 1--* block 1--* item
block 1--2 task (role 1, role 2)
task 0..*--0..1 participant          (holder of a reservation or of a completed task)
task 1--* response *--1 item
response *--1 participant
participant *--0..1 consent_form 1--* consent_form_text (one per language)
access_session *--0..1 participant
```

## State transitions (used by later specs, enforced here by `CHECK`)

```text
task:       free --reserve--> reserved --submit--> completed
            reserved --expires, taken by another--> reserved (new holder, drafts of the old one discarded)
            completed --withdrawal with discard (011)--> free (same queue_position)
response:   draft --submit--> final
config:     study_phase pilot --switch (005, empty database)--> collection   (no way back)
participant: active --anonymize (011)--> anonymized (name and e-mail erased)
```

## Not in V1

These come with the spec that needs them, in their own migrations:

| What | Spec |
|---|---|
| Counter of e-mails sent per day | 004 |
| Storage of an import preview between preview and confirmation, if it is not kept in memory | 005 |
| Record of withdrawal operations without personal data, if the application log is not enough | 011 |
