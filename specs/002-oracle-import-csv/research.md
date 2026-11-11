# Research: Generate the import CSV from the pipeline

Survey done on 2026-11-11 on the run `runs/minitest-piloto` (38 edits, 12 version steps, from
`minitest_v1.0` to `minitest_v2.2`).

## 1. Where the complete use cases come from

- **Decision**: use the `context` field of the T1 requests (`s5_blind/public/requests_T1.jsonl`),
  which has the form `### origin version\n<use case>\n### target version\n<use case>`, and split it
  at the `### target version` header. The request of each edit is found through
  `edit_key -> E-xxx` in `s5_blind/_private/edits_index.json` (the same mapping as
  `oracle._key_to_eid`; the command builds it from the index it already reads to get the step `k`).
- **Rationale**: it is exactly the text that the sheet `labeling_sheet_<system>.md` shows and that
  the techniques receive: `claret.render(uc, include_metadata=False, mark=...)`, with `<- edited`
  on the elements touched by the edit, in each version (`s5_blind._context`). Reusing the text
  guarantees FR-004 without duplicating the marking logic. In Minitest, the 38 contexts have
  exactly one `### target version` header, all have at least one marked line and the longest side
  has 2,186 characters.
- **Alternatives considered**: render again with `claret.render` from `s1_version/<tag>/src`
  (would duplicate `_context` and could diverge from the sheet); read the `.md` sheet
  (presentation format, fragile to parse).

## 2. Use case that does not exist in one of the versions

- **Decision**: S5 already writes `(the use case does not exist in this version)` on the missing
  side. The command copies this text as is (constant `MISSING_USECASE`); it does not translate it.
  The Veredito item screen (spec 008) recognizes the text and shows a localized message in the
  participant's language.
- **Rationale**: meets FR-005 (short notice, never empty) and the constitution 2.7.0 rule that data
  is never translated and stays in the language in which it was captured. The use cases
  themselves are in English, as in the pipeline, and the notice is part of the same captured data.
  Since the Veredito UI is available in pt-BR, en-US and es-ES, a text fixed in a single language
  in the file would be wrong for two of the three locales anyway; localizing belongs to the item
  screen.
- **Alternatives considered**: translate the notice to Portuguese,
  `(este caso de uso não existe nesta versão)` (the earlier decision of this research, reversed:
  it changes captured data and fixes one UI language in the file); leave it empty (refused by the
  import, FR-052).

## 3. Version labels

- **Decision**: `origin_version = "v" + label` of the previous version and
  `target_version = "v" + label` of the modified version of the edit's step `k`
  (`cfg.subject.steps()`; `k` comes from `edits_index.json`). In Minitest: `v1.0`...`v2.2`.
- **Rationale**: the version declared inside the use case lags behind the release. For example,
  releases `v1.1` and `v1.2` still declare `1.0`, and an edit of step 2 would appear as
  `1.0 -> 1.0`. The release label is always distinct between the two versions and is the same one
  used in the pipeline step names (`step_label`). In addition, S5 removes the `version` metadata
  from the displayed text.
- **Alternatives considered**: version declared in the use case, with the release only as a
  fallback (original scenario 4 of the spec). It was discarded because of the problem above. The
  spec was adjusted: scenario 4 and the edge case now use the release label.

## 4. Columns copied from `edits.csv`

- **Decision**: `edit_key`, `uc`, `kind`, `field`, `origin` and `target` are read from
  `s3_diff/edits.csv` with `oracle._cell`, as in `oracle._items`, which generates the
  `t1_<system>_rater*.csv` files. The row order is that of `edits.csv`. `system` is
  `cfg.subject.key`, the same as in `t1_<system>_*.csv`.
- **Rationale**: the responses exported by Veredito (`t1_<system>_raterN.csv`, FR-057c of 001)
  must match the rows of the rater files. Copying in the same way as `oracle-template` guarantees
  this. `_cell` only strips surrounding spaces and turns `nan` into empty.
- **Alternatives considered**: read `edits.json` (same fields, but the `.csv` is the oracle
  reference).

## 5. Validation before writing

- **Decision**: mirror in Python the refusal rules of the import (FR-052 and FR-053 of 001):
  - required fields;
  - `kind` within `ADD`, `DEL`, `MOD`, `META`;
  - `origin` required in `MOD` and `DEL`, `target` in `MOD` and `ADD`, and at least one of the two
    in `META` (the pipeline generates `META` with one empty side when a metadata value is added or
    removed, e.g. `LoginTest|3|1` from no requirement to `REQ-07`);
  - `origin` and `target` with up to 5,000 characters;
  - complete use cases with up to 40,000 characters;
  - version labels with up to 20 characters;
  - (`system`, `edit_key`) unique in the file.

  On error, the command does not write the file, lists line and reason and exits with code 1. A row
  without `<- edited` on either side only produces a warning. Character counting uses Python's
  `len()` (code points), and Veredito must count the same way (`codePointCount` in Java). Messages
  are in English, like the rest of the code (the command is a researcher tool, not part of the
  localized UI).
- **Rationale**: meets US2 and FR-006 and avoids finding the error only in the 005 preview. Not
  writing prevents importing an old file by mistake alongside a new invalid one.
- **Alternatives considered**: write even with errors and only warn (the invalid file would stay in
  the folder). Requiring both sides in `META` (first version of the rule) was discarded after T020:
  it refused `LoginTest|3|1`.

## 6. File format

- **Decision**: `utils.write_csv` (`csv.DictWriter`, UTF-8 without BOM, `newline=""`): records
  separated by `\r\n`, quotes only when needed, inner quotes doubled, `\n` preserved inside fields.
  File name: `veredito_<system>.csv`, next to the `t1_*` files.
- **Rationale**: it is RFC 4180 (FR-002), it is what the pipeline already uses and it is
  deterministic for the same input (FR-007).
- **Alternatives considered**: `import_<system>.csv` (too generic a name in the oracle folder).

## 7. Risks

- **Stale T1 requests**: if `edits.csv` changes without running S5 again, some `edit_key` has no
  request. The command fails pointing to S5 (`... not found; run S5 first (python -m issta_tcm run
  --to s5 with the same --config)` via `MissingStageOutput`, or `N edit(s) without a T1 request:`
  with the list of keys without context).
- **Change in the context header**: if S5 changes `### origin version` or `### target version`,
  the split breaks (`ValueError("context is not in the S5 format")`). A test generates the context
  with S5's own `_context` and checks the split.
