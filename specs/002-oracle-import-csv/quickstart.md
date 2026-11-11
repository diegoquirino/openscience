# Quickstart: validate the import CSV

## Prerequisites

- `ISSTA_2027` folder with the Minitest run already processed up to S5
  (`src/runs/minitest-piloto/s5_blind/public/requests_T1.jsonl` exists).
- The researcher's Python: `C:\Users\diego\AppData\Local\Python\bin\python.exe` with the package
  installed in editable mode (`pip install -e .[dev]`).

All commands run in `ISSTA_2027/src`.

## 1. Automated tests

```powershell
python -m pytest tests/test_veredito_import.py -q
```

Expected: all pass (54 tests), covering the context split, the missing use case text kept as
captured, the version labels, the validation (each rule of the [contract](contracts/import-csv.md)),
quotes and line breaks, and two identical generations. Recorded on 2026-11-11: 54 new tests; the
full suite (`python -m pytest -q`) has 128 passed (74 before this spec).

## 2. Generate the Minitest file (US1, SC-001)

```powershell
python -m issta_tcm oracle-import --config config/experiment-minitest.yaml
```

Expected, in a few seconds:

- `inputs/oracle/veredito_minitest.csv` written;
- report with `38 row(s)`, counts per `kind` and `field` and the longest lengths;
- exit code 0, without `no line marked <- edited` warnings (SC-002).

Recorded on 2026-11-11: 38 rows, exit 0, no warnings, about 1.2 s; `by kind: ADD 6, DEL 3,
META 2, MOD 27`.

## 3. Check against the pipeline (US1, scenarios 1 to 3)

- The columns `edit_key` ... `target` match, row by row and in the same order,
  `inputs/oracle/t1_minitest_rater1.csv`.
- For some rows, `origin_usecase` and `target_usecase` are identical to the two blocks of the
  corresponding item in `inputs/oracle/labeling_sheet_minitest.md`, with no exception: nothing is
  translated, not even the missing use case text.
- `origin_version` and `target_version` follow the configuration steps: the `|1|` edits have
  `v1.0` and `v1.1`, and the `|12|` edits have `v2.1` and `v2.2`.

## 4. Determinism (scenario 6)

Generate again and compare the hash:

```powershell
(Get-FileHash inputs/oracle/veredito_minitest.csv).Hash
python -m issta_tcm oracle-import --config config/experiment-minitest.yaml
(Get-FileHash inputs/oracle/veredito_minitest.csv).Hash
```

Expected: the two hashes are equal (recorded on 2026-11-11: identical hash on two runs).

## 5. Validation failure (US2)

The automated test with a 5,001-character edit covers this case. Expected: exit code 1,
`line N (edit_key): origin longer than 5000 characters` after
`N error(s); the file was not written:`, and no file written (a previous file stays identical).

## 6. Preview in Veredito (SC-003)

Done in spec 005: import `veredito_minitest.csv` in local staging (Docker) and check that the
preview shows 38 included, 0 ignored and 0 in error.
