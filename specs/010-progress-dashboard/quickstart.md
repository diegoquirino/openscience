# Quickstart: validate spec 010

**Feature**: `010-progress-dashboard` | Contracts: [dashboard](contracts/dashboard.md),
[web](contracts/web.md) | Data: [data-model.md](data-model.md)

## 1. Automated checks

```powershell
cd backend; .\mvnw.cmd -B verify
cd ..\frontend; npm ci; npm run i18n:check; npm test; npm run build
```

## 2. Local staging

```powershell
docker compose --profile staging down -v
docker compose --profile staging up --build -d
```

Footer / `/api/v1` show `0.10.0+…`. Sign in as administrator (`diegoquirino@gmail.com`; code in
Mailpit at `http://localhost:8025`).

## 3. Seed labeled data

1. Import `veredito_minitest_vazio.csv` (005).
2. As two eligible participants, reserve and partially answer one block; complete another role so
   the dashboard shows free, in progress and completed together.
3. Optionally leave a reservation past `expires_at` (short reservation minutes) to see `expired`.

## 4. Dashboard (US1–US3)

On `/admin/dashboard`:

| Check | Expect |
|---|---|
| Overall row | First progress row; totals match the sum of system×batch rows |
| Per system / batch | Items, blocks, role 1 / 2 free·inProgress·completed and percent |
| First free | Matches the lowest reclaimable `queue_position` |
| Reservations | Pseudonym only; draft counts; expired flagged |
| Participants | Status buckets + skills + completed-by-pseudonym shares |
| Languages | pt-BR / en-US / es-ES always present |
| E-mail quota | Remaining vs limit; highlight when &lt; 10 |
| Refresh | Numbers reload without a full page reload |
| Empty DB | Zeros + shortcut to Import |

`GET /api/v1` as admin has `vd:dashboard`; as participant it does not.

## 5. Cross-check (SC-001 / SC-003)

Export the long table (009) and confirm final-response language totals and completed-task counts
match the dashboard. Task free/inProgress/completed match research §3 against the `task` table.

## 6. Playwright

```powershell
cd frontend
$env:E2E_BASE_URL="http://localhost:8080"
npm run e2e
```

## 7. Production

1. Push to `Veredito`; wait for `ci.yml`.
2. Deploy: `git push origin <commit>:refs/tags/deploy/0.10.0-1` (**only with the researcher's
   explicit approval**).
3. Footer shows `0.10.0+…`; admin Dashboard works; participant cannot open it.
