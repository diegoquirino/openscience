# Data model: Import items by CSV and form the queue blocks

**Feature**: `005-block-import` | **Date**: 2026-11-14 | **Research**: [research.md](research.md)

Spec 003 already created `import_batch`, `block`, `item`, `task` and `config.study_phase` /
`study_phase_changed_at` in `V1`/`V2`. Spec 005 **writes** those tables on confirmation and adds
only the temporary preview table in `V4__import_preview.sql`.

## Migration `V4__import_preview.sql`

Additive only (rollback to the revision of 004 must keep working):

```sql
CREATE TABLE import_preview (
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    file_name         VARCHAR(255) NOT NULL,
    created_at        DATETIME(6)  NOT NULL,
    expires_at        DATETIME(6)  NOT NULL,
    included_count    INT          NOT NULL,
    ignored_count     INT          NOT NULL,
    error_count       INT          NOT NULL,
    block_count       INT          NOT NULL,
    block_size        TINYINT      NOT NULL,
    extra_columns_json TEXT        NOT NULL,
    included_json     MEDIUMTEXT   NOT NULL,
    ignored_json      MEDIUMTEXT   NOT NULL,
    errors_json       MEDIUMTEXT   NOT NULL,
    block_sizes_json  VARCHAR(2000) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT ck_import_preview_counts CHECK (
        included_count >= 0 AND ignored_count >= 0 AND error_count >= 0 AND block_count >= 0
    )
);

CREATE INDEX ix_import_preview_expires_at ON import_preview (expires_at);
```

No change to `import_batch`, `block`, `item`, `task` or `config`.

## How spec 005 uses the tables

### `import_preview` (new)

One row per successful upload that produced a preview. Not part of the study data; safe to delete
after expiry or after confirm.

| Field | Content |
|---|---|
| `file_name` | Original multipart file name (truncated to 255) |
| `expires_at` | `created_at + 30 minutes` |
| `extra_columns_json` | JSON array of extra header names (may be `[]`) |
| `included_json` | JSON array of objects with the Appendix D fields, exact file strings |
| `ignored_json` | JSON array of `{ "line", "code", "system", "edit_key" }` |
| `errors_json` | JSON array of `{ "line", "code", "system", "edit_key" }` (identifiers when present) |
| `block_sizes_json` | JSON array of integers, e.g. `[10,10,10,8]` |

Confirm does **not** mark the row; it deletes it at the end of the successful transaction (or leaves
it to expire if the confirm stored zero new items after the uniqueness re-check). A second confirm
that still finds the preview re-filters `included_json` against the database and typically stores
nothing.

### `import_batch` (write)

On confirm, one row: `file_name`, `imported_at` (UTC now), `included_count`, `ignored_count`,
`error_count` from the **confirm-time** classification (so a double confirm can record a batch with
`included_count = 0` only if the product chooses to; this plan records a batch only when
`included_count > 0`, and otherwise returns the summary without a new batch row — see contract).

Decision recorded in the contract: **a batch row is created only when at least one item is
inserted**; a double confirm answers `200` with a summary body (all ignored) and no `Location`.

### `block` / `item` / `task` (write)

| Rule | Detail |
|---|---|
| Block `number` | `max(existing) + 1, + 2, …` in confirm order |
| Block `size` | Number of items in that block (1 to `block_size`) |
| Item fields | Exact strings from the file; empty optional sides stored as SQL `NULL` |
| Item `position` | 1 .. size within the block |
| Task | Two rows per block, `role` 1 then 2, `status` `free`, `queue_position` contiguous after the current max |
| Never updated | Existing blocks, items, tasks and responses (FR-007, FR-055) |

### `config` (read / conditional write)

| Use | Rule |
|---|---|
| `block_size` | Read under the same lock as the confirm; used by `BlockPlan` |
| `study_phase` | Read for the Minitest gate; written only by the phase-switch operation |
| `study_phase_changed_at` | Set to UTC now on a successful switch to `collection` |

## Validation rules (domain, not new CHECK constraints)

| Rule | Source |
|---|---|
| Required columns present or refuse the whole file | US1 scenario 2 |
| FR-052 emptiness / kind / length rules ? row error | FR-002, FR-052 |
| Model length caps (`system` 100, …) ? row error | 003 data model |
| (`system`, `edit_key`) already in DB ? ignored | FR-003 |
| Duplicate (`system`, `edit_key`) in file ? error from the 2nd | FR-003 |
| Any non-`minitest` system while staging or `pilot` ? refuse file | FR-011 |
| Phase switch only with zero `item`, `participant`, `response` | FR-012 |

## State transitions added in behaviour

```text
import_preview:  created --confirm (items inserted)--> deleted
                 created --expire or replace--> deleted
config.study_phase: pilot --PUT collection (empty DB, not staging)--> collection
```
