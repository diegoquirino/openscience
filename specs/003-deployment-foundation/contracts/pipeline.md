# Contract: Image, local staging, CI and deployment

**Feature**: `003-deployment-foundation` | FR-007, FR-010, FR-011, SC-001, SC-002, SC-005, SC-006

## Image (`Dockerfile`)

| Item | Contract |
|---|---|
| Build argument | `VEREDITO_VERSION` (default `0.3.0+dev`) |
| Stages | `node:26-bookworm-slim` (front end) -> `eclipse-temurin:25-jdk` (jar, tests skipped) -> `eclipse-temurin:25-jre` (runtime) |
| Runtime user | non-root |
| Port | `8080` (Cloud Run sets `PORT`; the application reads it) |
| Profile | from `SPRING_PROFILES_ACTIVE`: `prod` (Cloud Run) or `staging` (Compose) |
| Contents | the jar with `static/index.html`, the root page assets and `static/{pt-BR,en-US,es-ES}/` |

## Environment variables

No value below is ever committed; `.env.example` lists the names with harmless defaults.

| Variable | `staging` (Compose) | `prod` (Cloud Run) |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `staging` | `prod` |
| `DB_URL` | `jdbc:mysql://mysql:3306/veredito` | `jdbc:mysql:///veredito?cloudSqlInstance=<project>:southamerica-east1:<instance>&socketFactory=com.google.cloud.sql.mysql.SocketFactory` |
| `DB_USER` | `veredito` | `veredito_app` |
| `DB_PASSWORD` | local value from `.env` (default in `compose.yaml` for a throwaway database) | secret `veredito-db-password` |
| `SMTP_HOST`, `SMTP_PORT` | `mailpit`, `1025` | `smtp.gmail.com`, `587` |
| `SMTP_USER` | none | `diegoquirino@gmail.com` |
| `SMTP_PASSWORD` | none | secret `veredito-smtp-password` (used from spec 004 on) |

## Local staging (`compose.yaml`)

| Command | Effect |
|---|---|
| `docker compose --profile staging up --build` | Builds the image, starts `mysql` (9.7), `mailpit` and `app`; Flyway prepares the database; app at `http://localhost:8080`, inbox at `http://localhost:8025` |
| `docker compose --profile staging down -v` | Stops everything and deletes the database volume (US4 scenario 4) |
| `docker compose up -d mysql mailpit` | Development only: services for `./mvnw spring-boot:run` and `ng serve` (Spring Boot's Docker Compose support does this on its own) |

## CI (`.github/workflows/ci.yml`)

Trigger: push to the branch `Veredito`.

| Job | Steps | Fails when |
|---|---|---|
| `backend` | JDK 25, `./mvnw -B verify` (unit, ArchUnit, Testcontainers MySQL 9.7, REST Assured, documentation completeness, bundle parity); upload `openapi-v1.json`, `openapi-index.json` | Any test fails |
| `frontend` | Node 26, `npm ci`, `npm run i18n:check`, `npm test`, `npm run build` | Missing or empty translation, failing unit test, build error (including `i18nMissingTranslation: "error"`) |
| `secrets` | gitleaks over the branch history | Any secret found (SC-006) |
| `image` (after the three above) | Compute `VEREDITO_VERSION`; build; push `southamerica-east1-docker.pkg.dev/veredito-issta2027/veredito/app:<commit>` through Workload Identity Federation | Build or push fails |
| `e2e` (after `image`) | Compose `staging` with that image; Playwright smoke tests per locale | Any smoke test fails |

## Deployment (`.github/workflows/deploy.yml`)

Trigger: push of a tag `deploy/*`. The researcher runs, for a commit already rehearsed in local
staging:

```text
git push origin <commit>:refs/tags/deploy/<version>
```

| Step | Contract |
|---|---|
| Check | `ci.yml` succeeded for the tagged commit and the image `app:<commit>` exists; otherwise the workflow fails without deploying |
| Deploy | `gcloud run deploy veredito --image <image>@<digest>` in `southamerica-east1`, service account `veredito-run`, profile `prod`, secrets from Secret Manager, 1 vCPU, 1 GiB, min 0, max 4, startup CPU boost |
| Result | Same URL; the footer and `GET /api/v1` show the new `systemVersion` (US2 scenario 1) |
| Database | Flyway applies only pending migrations at startup; `flyway clean` is disabled (US2 scenario 2, US3 scenario 3) |
| Rollback | `gcloud run services update-traffic veredito --to-revisions=<previous revision>=100`; same URL (US2 scenario 3) |

## Identity and access (Workload Identity Federation)

| Principal | Allowed from | Roles |
|---|---|---|
| `veredito-ci` | `refs/heads/Veredito` of `diegoquirino/openscience` | Artifact Registry writer on the `veredito` repository |
| `veredito-deploy` | `refs/tags/deploy/*` of `diegoquirino/openscience` | Cloud Run developer on `veredito`; Service Account User on `veredito-run`; Artifact Registry reader |
| `veredito-run` | Cloud Run runtime | Cloud SQL client; Secret Manager accessor on `veredito-db-password` and `veredito-smtp-password` |

GitHub stores only non-secret identifiers (project number, pool and provider names, service
account e-mails) as repository variables.
