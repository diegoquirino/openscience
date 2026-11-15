# Quickstart: validate spec 007

**Feature**: `007-block-queue` | Contracts: [tasks](contracts/tasks.md),
[problem-details](contracts/problem-details.md), [web](contracts/web.md) |
Data: [data-model.md](data-model.md)

All commands run from the root of the `Veredito` branch (the `Veredito` folder), in PowerShell.
Prerequisites are those of the
[quickstart of spec 003](../003-deployment-foundation/quickstart.md#prerequisites). Participant and
administrator sign-in follow the [quickstart of spec 004](../004-email-code-access/quickstart.md).
A confirmed Minitest batch (spec 005) must exist before labeling.

## 1. Automated checks (SC-001 to SC-006, Principle II)

```powershell
cd backend; .\mvnw.cmd -B verify; cd ..
cd frontend; npm ci; npm run i18n:check; npm test; npm run build; cd ..
```

Expected: everything passes. Among the back-end tests: `NextTaskChooser` scenarios; REST Assured
reserve / draft / submit; `TaskConcurrencyTest` (30 threads, SC-001); exhaustion yields two finals
per item from different people (SC-002); `TaskExpiryTest` with lowered `reservation_minutes`;
language switch / `language` on drafts (SC-006); `AuthorizationTest` denies every Tasks operation
to visitors and the administrator.

## 2. Local staging

```powershell
docker compose --profile staging down -v
docker compose --profile staging up --build -d
```

1. Sign in as administrator; import and confirm `veredito_minitest_vazio.csv` (005).
2. Sign out; sign in as a participant (any non-admin e-mail; code in Mailpit at
   `http://localhost:8025`).

## 3. Happy path (US1–US4)

| Step | Expected |
|---|---|
| `/welcome` ? start labeling | Receives B1-R1; first item; countdown ? `reservation_minutes` |
| Answer impact + operation; Continue | Draft saved; counter advances; countdown resets |
| Reload mid-block | Same task; first unanswered item; drafts kept (SC-004) |
| Finish items ? Review ? Submit | Task completed; done screen with thanks |
| Label another block | Receives next free task the person can label (never the other role of a finished block) |

```powershell
docker compose exec mysql mysql -uveredito -pveredito-local veredito -e "SELECT id, status, role, participant_id FROM task ORDER BY queue_position LIMIT 8; SELECT state, change_impact, language FROM response LIMIT 5"
```

## 4. Concurrency and exclusivity (US1 scenarios 2, 6)

Two browsers (or the automated concurrency test): both ask at once with one free task left ? one
receives it, the other gets `none-available-for-you` or the next task. Nobody holds both roles of
the same block.

## 5. Expiry (US5) — staging

```powershell
docker compose exec mysql mysql -uveredito -pveredito-local veredito -e "UPDATE config SET reservation_minutes = 1, updated_at = UTC_TIMESTAMP(6) WHERE id = 1"
```

Reserve as A; wait past expiry without saving; ask as B ? B gets the task, A's drafts gone; A's
next save gets `404 not-found` (the task now belongs to B; `reservation-expired` is only for an
owner who already holds another valid block). If nobody took it, A's save renews and keeps the
drafts. Restore `reservation_minutes = 20` afterwards.

Covered over REST by `TaskExpiryTest` (T027): the movable clock replaces the browser wait, so no
Playwright timing test is kept.

## 6. Language (US6, SC-006)

Answer part of a block in pt-BR; switch header to en-US; confirm unsaved choices restored; save ?
`language = en-US` on that draft; earlier drafts keep `pt-BR`. Repeat with es-ES on review.

## 7. Production

1. Push to `Veredito`; wait for `ci.yml`.
2. Deploy: `git push origin <commit>:refs/tags/deploy/0.7.0-1` (only with explicit approval).
3. Administrator imports Minitest if the pilot database is empty; a participant signs in, labels one
   block, and the footer shows `0.7.0+…`.
