# Contract: Veredito import file

Contract between the pipeline (producer, spec 002) and the Veredito import (consumer, spec 005).
Source of the rules: Appendix D, FR-051, FR-052 and FR-053 of [spec 001](../../001-oracle-labeling/spec.md).

## Command

```text
python -m issta_tcm oracle-import [--config <yaml>] [--set key=value ...]
```

- Run in `ISSTA_2027/src`, after S5 of the configured run.
- Output: `<oracle_dir>/veredito_<system>.csv`; `oracle_dir` is `stages.s8_evaluate.oracle_dir`
  (default `../inputs/oracle`).
- Overwrites the existing file, because the output is deterministic.
- Standard output, on success: the written path, then the report: `N row(s)`, `by kind: ...`,
  `by field: ...`, `longest: ...` (longest `origin`, `target`, `origin_usecase`,
  `target_usecase`), then one `warning: ...` line per warning.
- Exit code: `0` written; `1` some row would be refused or pipeline artifacts are missing.
  - Refused row: nothing is written; standard output has the report, the warnings, the line
    `N error(s); the file was not written:` and one line `line N (edit_key): reason` per error.
  - Missing pipeline output: nothing is written; standard error has
    `<path> not found; run S5 first (python -m issta_tcm run --to s5 with the same --config)` or
    `N edit(s) without a T1 request: <up to 10 keys>; run S5 first (...)`.
- Messages are in English, like the code: the command is a researcher tool, not part of the
  localized Veredito UI.

## File

- UTF-8 without BOM; RFC 4180; records separated by `\r\n`; fields with a comma, quotes or a line
  break are enclosed in quotes, with inner quotes doubled.
- Exact header (constant `COLUMNS`):

```text
system,edit_key,uc,kind,field,origin,target,origin_version,target_version,origin_usecase,target_usecase
```

- One row per edit, in the order of `s3_diff/edits.csv`. Without edits, the file has the header
  only, with the warning `no edits: the file has the header only`.
- Values are copied in the language in which they were captured; nothing is translated.

## Rules that the producer guarantees and the consumer checks

| Rule | Producer (002) | Consumer (005) |
|---|---|---|
| Required non-empty columns: `system`, `edit_key`, `uc`, `kind`, `field`, `origin_version`, `target_version`, `origin_usecase`, `target_usecase` | Fails (`<column> is required`) | Refuses the row |
| `kind` in {`ADD`, `DEL`, `MOD`, `META`} | Fails (`invalid kind: <value>`) | Refuses |
| `origin` not empty in `MOD` and `DEL`; `target` not empty in `MOD` and `ADD`; in `META`, at least one of the two (one side may be empty when a metadata value is added or removed, e.g. `LoginTest\|3\|1` from no requirement to `REQ-07`) | Fails (`origin is required`, `target is required`, `origin or target is required`) | Refuses |
| `origin`, `target` <= 5,000 characters (code points) | Fails (`<column> longer than 5000 characters`) | Refuses |
| `origin_usecase`, `target_usecase` <= 40,000 characters | Fails (`<column> longer than 40000 characters`) | Refuses |
| `origin_version`, `target_version` <= 20 characters | Fails (`<column> longer than 20 characters`) | Refuses |
| (`system`, `edit_key`) unique in the file | Fails on the 2nd occurrence (`edit_key repeated in the file`) | Refuses from the 2nd on |
| (`system`, `edit_key`) already imported | - | Ignores, without changing the item |
| No `<- edited` line in either use case | Warning (`line N (edit_key): no line marked <- edited in either use case`) | Accepts |
| Extra columns | Does not generate | Ignores |

Line numbers count the header as line 1, like the Veredito preview.

## Use case content

- Canonical CLARET syntax without metadata (`claret.render(uc, include_metadata=False, mark=...)`),
  the same as the sheet `labeling_sheet_<system>.md`.
- Lines touched by the edit end in `   <- edited`.
- Use case missing in the version: the S5 text `(the use case does not exist in this version)`,
  kept as captured; the item screen (spec 008) shows a localized message instead.

## Example (abridged)

```text
system,edit_key,uc,kind,field,origin,target,origin_version,target_version,origin_usecase,target_usecase
minitest,LoginTest|1|1,LoginTest,MOD,step,"...","...",v1.0,v1.1,"systemName ""Email""
usecase ""Login User"" {
  ...   <- edited
}","systemName ""Email""
..."
```
