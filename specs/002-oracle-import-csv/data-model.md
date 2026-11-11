# Data Model: Generate the import CSV from the pipeline

There is no persistence of its own; the command reads pipeline artifacts and writes a CSV.

## Inputs

| Artifact | Fields used | Role |
|---|---|---|
| `s3_diff/edits.csv` | `edit_key`, `uc`, `kind`, `field`, `origin`, `target` | One row per edit; defines the order |
| `s5_blind/_private/edits_index.json` | `E-xxx -> {edit_key, k}` | Links the edit to the T1 request and to the version step |
| `s5_blind/public/requests_T1.jsonl` | `request_id` (`E-xxx`), `context` | Complete use cases, with `<- edited` |
| System configuration | `subject.key`, `subject.steps()` -> `(k, previous version, modified version)` with `label` | `system` and version labels |

## Import row (`ImportRow`)

A dictionary with the columns of Appendix D of spec 001, in this order (constant `COLUMNS`); every
value is a `str`, never `None`:

| Field | Source | Rule |
|---|---|---|
| `system` | `cfg.subject.key` | Required |
| `edit_key` | `edits.csv` (`oracle._cell`) | Required; unique in the file |
| `uc` | `edits.csv` | Required |
| `kind` | `edits.csv` | Required; `ADD`, `DEL`, `MOD` or `META` (`KINDS`) |
| `field` | `edits.csv` | Required |
| `origin` | `edits.csv` | Required in `MOD` and `DEL`; in `META`, `origin` or `target`; up to 5,000 characters (`MAX_TEXT`) |
| `target` | `edits.csv` | Required in `MOD` and `ADD`; in `META`, `origin` or `target`; up to 5,000 characters (`MAX_TEXT`) |
| `origin_version` | `"v" + label` of the previous version of step `k` | Required; up to 20 characters (`MAX_VERSION`) |
| `target_version` | `"v" + label` of the modified version of step `k` | Required; up to 20 characters (`MAX_VERSION`) |
| `origin_usecase` | Part of `context` between `### origin version` and `### target version` | Required; up to 40,000 characters (`MAX_USECASE`) |
| `target_usecase` | Part of `context` after `### target version` | Required; up to 40,000 characters (`MAX_USECASE`) |

Both use cases lose the line breaks at their ends (`strip("\n")`). All values are copied in the
language in which they were captured: a missing use case keeps the S5 text
`(the use case does not exist in this version)` (`MISSING_USECASE`), which the Veredito item screen
(spec 008) shows as a localized message. The lines touched by the edit end in `<- edited`
(`EDITED_MARK`).

## Validation result (`Report`)

| Field | Content |
|---|---|
| `errors` | List of `(line, edit_key, reason)`; `line` counts the header as 1, like the Veredito preview |
| `warnings` | Rows without `<- edited` in either use case; file without any edit |
| `total` | Number of rows |
| `by_kind`, `by_field` | Counts, in alphabetical order |
| `max_len` | Longest length of `origin`, `target`, `origin_usecase`, `target_usecase` |

Reasons (in English, like the rest of the code): `<column> is required`, `invalid kind: <value>`,
`origin is required`, `target is required`, `origin or target is required`,
`<column> longer than <limit> characters`, `edit_key repeated in the file`. Warnings:
`line N (edit_key): no line marked <- edited in either use case` and
`no edits: the file has the header only`.

With a non-empty `errors`, the file is not written.
