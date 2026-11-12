# Quickstart: validate spec 003

**Feature**: `003-deployment-foundation` | Contracts: [api](contracts/api.md),
[problem-details](contracts/problem-details.md), [web-routing](contracts/web-routing.md),
[pipeline](contracts/pipeline.md) | Data: [data-model.md](data-model.md)

All commands run from the root of the `Veredito` branch (the `Veredito` folder), in PowerShell.

## Prerequisites

| Tool | Version | Note |
|---|---|---|
| Docker Desktop | 29.x, running | Local staging and Testcontainers |
| JDK | 25 (Temurin) | Only to run `./mvnw` outside Docker |
| Node.js / npm | 26 LTS / 11.x | Only to run the front end outside Docker |
| Google Cloud CLI | current | Only for the one-time cloud setup and rollback |

No secret is needed for local staging.

## 1. Automated checks (FR-008, FR-013, FR-016, FR-017, SC-007, SC-008)

```powershell
cd backend; .\mvnw.cmd -B verify; cd ..
cd frontend; npm ci; npm run i18n:check; npm test; npm run build; cd ..
```

Expected: everything passes. The back end reports, among others, `MigrationTest`,
`MessageBundleParityTest`, `LocalizedProblemDetailsTest`, `ApiDocumentationCompletenessTest`,
`WebRoutingTest`, `DomainIsolationTest`; `backend/target/openapi/openapi-v1.json` exists.

**Negative checks (SC-008)**, each one reverted afterwards:

1. Delete the `<target>` of one unit in `frontend/src/locale/messages.es-ES.xlf` and run
   `npm run i18n:check`: it fails naming the unit and `es-ES`.
2. Remove one key from `backend/src/main/resources/messages_pt_BR.properties` and run
   `.\mvnw.cmd -B test -Dtest=MessageBundleParityTest`: it fails naming the key and the file.
3. Remove the `description` of one operation and run
   `.\mvnw.cmd -B test -Dtest=ApiDocumentationCompletenessTest`: it fails naming the
   operation (US5 scenario 4).

## 2. Local staging (US4, SC-001)

```powershell
docker compose --profile staging down -v
Measure-Command { docker compose --profile staging up --build -d }
docker compose --profile staging ps
```

Expected: the first build and start take less than 10 minutes; `app`, `mysql` and `mailpit` are
running; `http://localhost:8080` answers; the Mailpit inbox is at `http://localhost:8025`.

## 3. Home page and locales (US1, US6)

| Step | Expected |
|---|---|
| Open `http://localhost:8080/` in a private window | Language-choice page with "Português (Brasil)", "English (US)", "Español (España)" |
| Choose each language | `/pt-BR/`, `/en-US/`, `/es-ES/`: header "Veredito", one sentence about the study, "Participar"/"Participate"/"Participar", footer with the version, banner "AMBIENTE DE ENSAIO"/"STAGING ENVIRONMENT"/"ENTORNO DE PRUEBAS" |
| Reload `http://localhost:8080/es-ES/admin` | Spanish shell, not another locale (US6 scenario 2) |
| Open `/en-us/` | Redirected to `/en-US/` |
| Open `/fr-FR/` and `/xyz` | Redirected to `/` |
| Use a footer language link | The same route opens under the other prefix, with a page reload (US6 scenario 4) |
| Browser developer tools, phone viewport (360 px wide) | No horizontal scrolling (US1 scenario 2) |

The Playwright smoke tests automate this table:

```powershell
cd frontend; npx playwright install chromium; npm run e2e; cd ..
```

## 4. API (US5, US6 scenario 5)

```powershell
curl.exe -s http://localhost:8080/api
curl.exe -s http://localhost:8080/api/v1
curl.exe -s -H "Accept-Language: es-ES" http://localhost:8080/api/v9/tasks
curl.exe -s -H "Accept-Language: en-US" http://localhost:8080/api/v9/tasks
curl.exe -s http://localhost:8080/api/v9/tasks
```

Expected:

- `/api` lists `v1` with the link `vd:v1`.
- `/api/v1` has `available: true`, `environment: "staging"`, `systemVersion` equal to the footer,
  and among its links only `self`, `curies` and `vd:request-code`.
- `/api/v9/tasks` returns `400` Problem Details `urn:veredito:problem:unsupported-api-version`,
  in Spanish, in English, and in Portuguese without the header, always with
  `supportedVersions: ["v1"]`.
- `http://localhost:8080/swagger-ui.html` opens and lists the groups `v1` and `index`, with
  every operation documented (US5 scenario 1).

## 5. Database (US3, SC-003)

```powershell
docker compose exec mysql mysql -uveredito -p veredito -e "SHOW TABLES; SELECT * FROM config; SELECT version, description, installed_on, success FROM flyway_schema_history;"
```

Expected: the tables of [data-model.md](data-model.md) plus `flyway_schema_history`; `config` with
`diegoquirino@gmail.com`, `NULL` admin language, 20, 10, 450, `pilot`; migrations V1 and V2
applied once.

| Check | How | Expected |
|---|---|---|
| Repeat the preparation (US3 scenario 3) | `docker compose --profile staging restart app` | No new row in `flyway_schema_history`; data unchanged |
| Same structure twice (SC-003) | `MigrationTest` migrates two empty containers and compares `information_schema` | Identical |
| Locale columns (US3 scenario 6) | Try `INSERT` of a response with `language = 'fr-FR'` | Refused by the `CHECK` constraint |
| Database down (edge case) | `docker compose stop mysql`, reload the home page | "Sistema temporariamente indisponível" (or the locale's text); `/api/v1` has `available: false` and no `vd:request-code`; `docker compose start mysql` restores it |
| Start over (US4 scenario 4) | `docker compose --profile staging down -v` and `up` again | Empty database, prepared again |

## 6. One-time cloud setup (researcher, by hand)

Done once, with the researcher's own Google account; billing and budget steps are done in the
console by the researcher.

1. Create the project `veredito-issta2027`, link the billing account and create the budget
   "Veredito mensal" (R$ 200, alerts at 50%, 90% and 100%, no action) (SC-005).
2. Enable the APIs, create the Artifact Registry repository with the cleanup policy, the Cloud SQL
   instance (`MYSQL_9_7`, `--edition=ENTERPRISE`, `db-f1-micro`, no authorized networks,
   `--connector-enforcement=REQUIRED`, daily backups), the database `veredito`, the user
   `veredito_app`, the two secrets, the three service accounts and the Workload Identity pool and
   provider, as listed in [research.md](research.md#19-google-cloud-resources) and
   [contracts/pipeline.md](contracts/pipeline.md).
3. Put the non-secret identifiers in the GitHub repository variables.

The exact `gcloud` commands go in `docs/cloud-setup.md`, written during implementation; secret
values are typed only into the console or prompted by `gcloud`, never pasted into files.

## 7. Deployment, rollback and URL stability (US2, SC-001, SC-002)

```powershell
git push origin <commit>:refs/tags/deploy/0.3.0-1
```

| Check | Expected |
|---|---|
| The `deploy` workflow | Finishes in less than 15 minutes; refuses a commit whose `ci` run failed |
| Production URL | Footer shows `0.3.0+<commit>`, the same as in local staging (US4 scenario 3) |
| `GET <url>/api/v1` | `environment: "production"`, `available: true`, no staging banner on the page |
| `<url>/swagger-ui.html` and `<url>/v3/api-docs/v1` | `401` Problem Details (US5 scenario 3) |
| Three deployments in a row (SC-002) | Deploy three commits; the URL never changes; with a record created in `config` (e.g. `updated_at`) before the first one, the row is unchanged after the third |
| Rollback (US2 scenario 3) | `gcloud run services update-traffic veredito --region=southamerica-east1 --to-revisions=<previous>=100`; same URL, previous version in the footer |
| Database not public (US3 scenario 5) | `mysql -h <public IP> -u veredito_app -p` from the researcher's machine is refused; only the Cloud SQL Auth Proxy with the researcher's IAM identity gets in |
| Home page time (SC-004) | With the service warm, the home page loads in less than 3 seconds (browser network panel) |

## 8. Secrets (FR-011, SC-006)

The `secrets` job of `ci.yml` (gitleaks over the branch history) is green, and GitHub secret
scanning shows no alert for the repository.
