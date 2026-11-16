# Quickstart: validate spec 009

**Feature**: `009-exports` | Contracts: [exports](contracts/exports.md), [web](contracts/web.md),
[oracle-from-veredito](contracts/oracle-from-veredito.md) | Data: [data-model.md](data-model.md)

## 1. Automated checks

```powershell
cd backend; .\mvnw.cmd -B verify
cd ..\frontend; npm ci; npm run i18n:check; npm test; npm run build
```

In `ISSTA_2027/src`, run the package tests that cover `oracle-from-veredito`.

## 2. Local staging

```powershell
docker compose --profile staging down -v
docker compose --profile staging up --build -d
```

Footer / `/api/v1` show `0.9.0+…`. Sign in as administrator (`diegoquirino@gmail.com`; code in
Mailpit at `http://localhost:8025`).

## 3. Seed labeled data

1. Import `veredito_minitest_vazio.csv` (005).
2. As two eligible participants, complete both roles of at least one block (006–008), or use the
   test helpers / SQL seed the automated `ExportTest` already applies.
3. Leave at least one block with only drafts to prove drafts are absent from exports.

## 4. Downloads (US1–US4)

On `/admin/export`:

| Action | Expect |
|---|---|
| Wide | Appendix C header; completed roles filled; drafts absent; neutralization on leading `=` |
| Long | Header of research §7; no name/email; one row per final response |
| Completo (`minitest`) | US3 header; Appendix D cells match import; incomplete roles blank |
| Rater 1 / 2 | Nine pipeline columns; incomplete role blanks |

`GET /api/v1` as admin has `vd:exports`; as participant it does not.

## 5. Pipeline (SC-001, SC-001b, SC-001c)

```powershell
cd ..\ISSTA_2027\src
# place veredito_minitest_completo.csv in inputs/oracle/
python -m issta_tcm oracle-from-veredito --set name=minitest
python -m issta_tcm agreement --set name=minitest
python -m issta_tcm oracle-disagreements --set name=minitest
```

Incomplete `_completo` must exit `1` and leave previous rater files untouched. When both roles are
complete, materialized raters match a direct Veredito rater download (apart from CRLF).

## 6. Playwright

```powershell
cd ..\Veredito\frontend
$env:E2E_BASE_URL="http://localhost:8080"
npm run e2e
```

## 7. Production

1. Push to `Veredito`; wait for `ci.yml`.
2. Deploy: `git push origin <commit>:refs/tags/deploy/0.9.0-1` (**only with the researcher's
   explicit approval**).
3. Footer shows `0.9.0+…`; admin Export works; participant cannot open it.
