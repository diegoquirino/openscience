# Contract: Materialize pipeline rater sheets from `veredito_<system>_completo.csv`

**Feature**: `009-exports` (pipeline side) | **Package**: `issta_tcm` in `ISSTA_2027/src`  
**Consumes**: [US3 of spec 009](../spec.md) · **Feeds**: existing `agreement`, `oracle-disagreements`, `oracle-final`

## Command

```text
python -m issta_tcm oracle-from-veredito [--config <yaml>] [--set key=value ...]
```

- Run in `ISSTA_2027/src`.
- Subject and oracle folder come from the config (`subject` key and
  `stages.s8_evaluate.oracle_dir`, default `../inputs/oracle`), same as the other
  `oracle-*` commands.
- Reads: `<oracle_dir>/veredito_<system>_completo.csv` where `<system>` is
  `cfg.subject.key`.
- Writes (overwrites): `<oracle_dir>/t1_<system>_rater1.csv` and
  `t1_<system>_rater2.csv`.
- Does **not** write `t1_<system>_adjudicated.csv` or `t1_<system>_final.csv`;
  those stay the job of `oracle-disagreements` and `oracle-final`.
- Messages in English (researcher tool, not the localized Veredito UI).

## Exit codes and messages

| Code | When | Effect on disk |
|---|---|---|
| `0` | Both rater sheets written | Rater files overwritten |
| `1` | Missing file, bad header, wrong `system`, incomplete role, invalid code, or duplicate `edit_key` | **Neither** rater file is written (previous files, if any, stay unchanged) |

On success, standard output prints the two written paths and the row count.  
On failure, standard error lists each problem as `edit_key: reason` (or a single file-level reason).

## Input file (`_completo`)

- UTF-8 without BOM; RFC 4180; header exactly as in spec 009 US3 scenario 2.
- Every data row: `system` equals `cfg.subject.key` (otherwise refuse the whole file).
- Extra columns: refused (strict header), so a wide-table export is not accepted here.
- For **each** role `N` in {1, 2}, `rater_N_change_impact` and
  `rater_N_maintenance_operation` MUST be non-empty on every row. Blank comment is
  allowed. Pseudonym and response language are **not** copied into the rater sheets.
- Codes MUST be the English identifiers: impact `LOW`|`HIGH`; operation
  `KEEP`|`UPDATE`|`REMOVE`|`CREATE`|`MERGE`|`SPLIT`|`REASSIGN`|`FLAG`.

## Output files (rater sheets)

Header (unchanged pipeline format):

```text
edit_key,uc,kind,field,origin,target,change_impact,maintenance_operation,comment
```

| Rater sheet column | Taken from `_completo` |
|---|---|
| `edit_key` … `target` | Same Appendix D fields (exact strings) |
| `change_impact` | `rater_N_change_impact` |
| `maintenance_operation` | `rater_N_maintenance_operation` |
| `comment` | `rater_N_comment` (empty string if blank) |

Row order: the order of rows in `_completo` (queue order from Veredito).  
`rater_N_pseudonym` and `rater_N_response_language` are ignored by this command.

## Recommended sequence after a successful materialization

```text
python -m issta_tcm oracle-from-veredito --config <yaml>
python -m issta_tcm oracle-disagreements --config <yaml>
# if it prints "no adjudication needed":
python -m issta_tcm oracle-final --config <yaml>
# else: fill t1_<system>_adjudicated.csv manually, then oracle-final
```

`agreement` remains optional (diagnostics). While unresolved disagreements exist,
`oracle-final` keeps refusing (`UnresolvedDisagreement`), as today.

## Relation to a direct Veredito download of `t1_*_rater*.csv`

When both roles are complete, the bytes produced by this command for a given
database state MUST match a direct US1 export from Veredito of the same system
(SC-001c of spec 009), so either path can feed `oracle-disagreements`.
