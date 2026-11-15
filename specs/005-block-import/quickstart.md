# Quickstart: validate spec 005

**Feature**: `005-block-import` | Contracts: [import](contracts/import.md),
[problem-details](contracts/problem-details.md), [web](contracts/web.md),
[pipeline](contracts/pipeline.md) | Data: [data-model.md](data-model.md)

All commands run from the root of the `Veredito` branch (the `Veredito` folder), in PowerShell.
Prerequisites are those of the
[quickstart of spec 003](../003-deployment-foundation/quickstart.md#prerequisites). Sign-in as
administrator follows the [quickstart of spec 004](../004-email-code-access/quickstart.md).

## 1. Automated checks (SC-002, SC-003, SC-004, SC-005, Principle II)

```powershell
cd backend; .\mvnw.cmd -B verify; cd ..
cd frontend; npm ci; npm run i18n:check; npm test; npm run build; cd ..
```

Expected: everything passes. Among the back-end tests: domain tests of `ItemRowRules`,
`ImportClassifier` and `BlockPlan` (sizes for 1, 9, 10, 11, 38, 205); `ImportTest` (preview +
confirm of `veredito_minitest_vazio.csv` with zero row errors — SC-005); read-back equality (SC-003);
localized preview of `minitest_duplicates.csv` in the three locales (SC-004); study-phase tests;
`AuthorizationTest` still denies every Administration operation to visitors and participants.

## 2. Local staging

```powershell
docker compose --profile staging down -v
docker compose --profile staging up --build -d
```

Sign in as `diegoquirino@gmail.com` (code in Mailpit at `http://localhost:8025`).

## 3. Preview of Minitest (US1, SC-005)

| Step | Expected |
|---|---|
| `/admin` ? Import | Phase banner: pilot / only Minitest; no "switch to collection" |
| Upload `backend/src/test/resources/minitest/veredito_minitest_vazio.csv` | Preview: included = all data rows, ignored = 0, errors = 0; block sizes of 10… and a remainder |
| Cancel | Preview cleared; `import_batch` still empty |
| Upload again and Confirm | Summary with block count and queue positions; batch appears in the list |

```powershell
docker compose exec mysql mysql -uveredito -pveredito-local veredito -e "SELECT COUNT(*) FROM item; SELECT number, size FROM block ORDER BY number; SELECT queue_position, role, status FROM task ORDER BY queue_position LIMIT 8"
```

## 4. Duplicates, errors and language (US1, SC-004)

| Step | Expected |
|---|---|
| Interface in es-ES; upload `minitest_duplicates.csv` | Counts and reasons in Spanish; `system` / `edit_key` unchanged |
| Repeat in en-US and pt-BR | Same file values; reasons in each language |
| Upload `minitest_missing_kind_column.csv` | Whole file refused; missing column named |
| Upload a file with `system=other` | Refused with "Only Minitest items are accepted in this phase" |

## 5. Second batch and double confirm (US2)

| Step | Expected |
|---|---|
| Confirm Minitest again (same file) | All rows ignored; previous blocks unchanged |
| Confirm a derived file with new `edit_key`s | New blocks appended after the existing queue positions |
| Two tabs confirm the same preview | Items stored once; the slower tab shows all ignored |

## 6. Study phase (US4) — automated in `StudyPhaseTest`

Manual check in staging: the switch control is absent. The production emptiness path is covered by
the Testcontainers test (import Minitest ? switch refused ? truncate study tables ? switch
accepted ? non-Minitest file accepted).

## 7. Volume (SC-001)

`ImportVolumeTest` builds a 500-line CSV from Minitest rows with unique `edit_key` suffixes, runs
preview + confirm, and asserts elapsed time &lt; 2 minutes.

## 8. Production

1. Push to `Veredito`; wait for `ci.yml`.
2. Deploy: `git push origin <commit>:refs/tags/deploy/0.5.0-1`.
3. Sign in as administrator; Import shows pilot / Minitest-only; upload the fixture; confirm;
   check the batch list and that participant flows still open `/welcome`.
