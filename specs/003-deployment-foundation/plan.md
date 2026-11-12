# Implementation Plan: Deploy Veredito and prepare the database

**Branch**: `003-deployment-foundation` | **Date**: 2026-11-12 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/003-deployment-foundation/spec.md`

## Summary

Spec 003 builds the skeleton that every later spec extends. The output is a single container
(Spring Boot 4 on Java 25, with three Angular 22 builds, one per locale) that serves:

- the language-choice page at `/`;
- an empty application shell under `/pt-BR/`, `/en-US/` and `/es-ES/`: header, home page with
  "Participate", footer with the deployed version, and the staging banner;
- a level 3 REST API: the version index `/api`, the entry point `/api/v1` and its relation
  documentation;
- localized Problem Details;
- Swagger, open in staging and refused in production.

Flyway creates all the tables of the data model of spec 001 and the default configuration on
MySQL 9.7.

The same `Dockerfile` serves two environments:
- **Local staging:** Docker Compose, profile `staging`, with MySQL and Mailpit.
- **Production:** Cloud Run plus Cloud SQL, reached through the Cloud SQL Java connector, with no
  authorized networks.

GitHub Actions does the rest:
- **On every push:** tests, translation completeness, secret search, image build and end-to-end
  smoke tests.
- **On a pushed tag `deploy/*`:** deploys the already built image of that commit to the same Cloud
  Run service, which keeps the URL.

Decisions and versions are in [research.md](research.md).

## Technical Context

**Language/Version**: Java 25 LTS (Temurin) with Spring Boot 4.1.2 (4.2.x after its GA on
2026-11-19, before the first production deployment); TypeScript with Angular 22.2.6; Node.js 26 LTS
for the front-end build only

**Primary Dependencies**:
- **Back end:** Spring Web MVC, Spring HATEOAS (HAL-FORMS), Spring JDBC (`JdbcClient`), Spring Mail
  (configured only; used from 004), Flyway 13.12.1 with `flyway-mysql`, springdoc-openapi 3.2.0,
  and the Cloud SQL Java connector 1.30.2 (production).
- **Front end:** Angular Material 22.2.6 (Material Design 3) and `@angular/localize` 22.2.6.

**Storage**: MySQL Community 9.7 (Cloud SQL Enterprise edition, `db-f1-micro`, in production;
`mysql:9.7` in Compose and Testcontainers); schema in `V1__create_schema.sql` and default
configuration in `V2__insert_default_config.sql` ([data-model.md](data-model.md))

**Testing**:
- **Back end:** JUnit 5; ArchUnit 1.5.1 (domain isolation); Spring Boot Test with Testcontainers on
  MySQL 9.7 (migrations).
- **API:** REST Assured 6.3.0 on a random port (entry point, versioning, localized Problem Details,
  web routing, documentation completeness). A JUnit test checks message-bundle parity.
- **Front end:** Angular's default runner (Vitest), and the XLIFF completeness script
  `npm run i18n:check`.
- **End to end:** Playwright 1.65.0 against local staging.

**Target Platform**: Linux container. In production: Google Cloud Run (`southamerica-east1`) plus
Cloud SQL. In staging: Docker Desktop on the researcher's Windows machine. CI runs on GitHub-hosted
Ubuntu runners.

**Project Type**: web application (Spring Boot back end serving an Angular front end and a REST API
from one container)

**Performance Goals**:
- Home page in less than 3 s once warm (SC-004).
- Each API call in less than 2 s under normal use (Principle V).
- Local staging up in less than 10 min on the first build (SC-001).
- A rehearsed version in production in less than 15 min (SC-001).

**Constraints**:
- **Cost:** under R$ 200 per month, with budget alerts (SC-005).
- **Production limits:** at most 4 Cloud Run instances × 5 connections, within the 25 of
  `db-f1-micro`.
- **Secrets:** none in git (FR-011, SC-006).
- **Rollback:** every migration from V3 on stays compatible with the previous code version.
- **Language:** everything in English; UI texts in three locales; data never translated.

**Scale/Scope**:
- One service and one database.
- About 11 tables.
- Three UI locales.
- A shell of four Angular screens: home, provisional "Participate", unavailable and not found.
- Four API resources: `/api`, `/api/v1`, `/api/v1/rels/{rel}` and the documentation.
- A few hundred volunteers in the data collection.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

Constitution 2.7.1.

| Rule | How the plan meets it | Status |
|---|---|---|
| I. Stack: Angular + Material, `@angular/localize` one build per locale | Angular 22.2.6, Material 3, `localize: true` with `pt-BR`, `en-US` (source), `es-ES` | Pass |
| I. Spring Boot on Java LTS with Maven; MessageSource | Spring Boot 4.1.2, Java 25, Maven Wrapper; bundles `messages*.properties` | Pass |
| I. REST level 3: HAL-FORMS, path versioning with native support, single entry point, Problem Details | `/api` index, `/api/v1` entry point with links by caller, `vd:` CURIEs, Spring Framework 7 API versioning, RFC 9457 ([contracts/api.md](contracts/api.md), [contracts/problem-details.md](contracts/problem-details.md)) | Pass |
| I. OpenAPI 3.1 per version with complete operations; Swagger open in staging, admin-only in production | springdoc groups `v1` and `index`; completeness test; `401` in production until the admin session of 004 | Pass |
| I. MySQL Community + Flyway; latest stable versions recorded | MySQL 9.7, Flyway 13.12.1; versions table in research section 1 | Pass |
| II. Domain in plain Java with unit tests | Package `domain` created, guarded by an ArchUnit test; rules arrive with 005/007/009 | Pass |
| II. Integration tests on MySQL of the production version; REST Assured from the entry point following links | Testcontainers `mysql:9.7`; API tests start at `/api/v1` and follow `vd:` relations | Pass |
| II. Test fails on incomplete OpenAPI; CI keeps `openapi-v1.json` | `ApiDocumentationCompletenessTest`; artifact upload in `ci.yml` | Pass |
| II. i18n completeness in CI (XLIFF targets, bundle parity, localized Problem Details) | `npm run i18n:check`, `MessageBundleParityTest`, `LocalizedProblemDetailsTest` | Pass |
| III. Invariants as database constraints; schema only through Flyway; no edits to applied migrations | Unique keys and `CHECK` constraints of [data-model.md](data-model.md); V1/V2 never edited after the first deployment | Pass |
| IV. No secrets or participant data in git; Secret Manager; WIF without long-lived keys | `.env` ignored, `.env.example` without values; secrets in Secret Manager; Workload Identity Federation; gitleaks in CI | Pass |
| IV. Codes and session ids only as hashes; cookie flags | Columns `code_hash` and `token_hash`; cookies come with 004 | Pass |
| IV. Database not exposed; Minitest-only data in staging and tests | No authorized networks + connector enforcement; fixture `veredito_minitest.csv` copied to test resources | Pass |
| V. Single artifact; only Cloud Run, Cloud SQL, Secret Manager, Artifact Registry | One image; IAM, Workload Identity Federation and the billing budget are account features, not running services | Pass |
| V. Budget R$ 200 with alerts only | "Veredito mensal" at 50/90/100 %, created by the researcher | Pass |
| V. Material only, responsive; API under 2 s | Material components; phone viewport in Playwright; one cheap query per entry point call | Pass |
| Language: English everywhere; GLOSSARY; data as captured; English codes | Identifiers per GLOSSARY (`study_phase`, `access_session` added); `CHECK` constraints on English codes | Pass |
| Workflow: `Veredito` orphan branch; deployment by GitHub Actions to the same Cloud Run service; staging before production | Workflows live in the branch; deployment by tag `deploy/*` on a commit whose CI passed | Pass |
| All text files UTF-8 | `.editorconfig` with `charset = utf-8`; bundles read as UTF-8 | Pass |

No violation. Two naming adjustments to earlier documents are recorded in research sections 7 and 8:
- `study-phase` becomes the column `config.study_phase`;
- `session` becomes `access_session`.

The GLOSSARY and specs 001, 003 and 005 are already updated to match.

**Post-design re-check** (after data-model and contracts):
- **Services and secrets:** the design adds no service beyond those allowed, and no secret
  reaches the repository.
- **Codes and documentation:** every stored code has its `CHECK` constraint, and every API resource
  is documented and covered by REST Assured.
- **Deployment trigger:** tag-triggered deployment keeps the workflows inside the orphan branch.

Pass.

## Project Structure

### Documentation (this feature)

```text
specs/003-deployment-foundation/
|-- plan.md               # this file
|-- research.md           # phase 0
|-- data-model.md         # phase 1
|-- quickstart.md         # phase 1
|-- contracts/
|   |-- api.md            # /api, /api/v1, /api/v1/rels/{rel}, versioning, documentation access
|   |-- problem-details.md
|   |-- web-routing.md    # root page, locale prefixes, SPA fallback, caching, shell texts
|   `-- pipeline.md       # image, variables, Compose, CI, deployment, IAM
`-- tasks.md              # phase 2 (/speckit-tasks; not created here)
```

### Source Code (root of the `Veredito` branch)

```text
backend/
|-- pom.xml                              # version 0.3.0; Spring Boot parent 4.1.2
|-- mvnw, mvnw.cmd, .mvn/wrapper/
`-- src/
    |-- main/
    |   |-- java/io/github/diegoquirino/veredito/
    |   |   |-- VereditoApplication.java
    |   |   |-- domain/                  # plain Java; ArchUnit-guarded (rules from 005 on)
    |   |   |-- api/                     # version index, API versioning, Problem Details advice, locale resolver
    |   |   |   `-- v1/                  # entry point, relation docs, representation models
    |   |   |-- web/                     # locale routing filter, cache headers, API docs access filter
    |   |   |-- persistence/             # JdbcClient access (database availability in 003)
    |   |   `-- config/                  # OpenAPI groups, application properties (environment, version)
    |   `-- resources/
    |       |-- application.yaml, application-staging.yaml, application-prod.yaml
    |       |-- db/migration/V1__create_schema.sql, V2__insert_default_config.sql
    |       |-- messages.properties, messages_pt_BR.properties, messages_es_ES.properties
    |       `-- static/index.html, language.css, language.js   # root page; Angular builds added by the Dockerfile
    `-- test/
        |-- java/io/github/diegoquirino/veredito/
        |   |-- support/                 # Testcontainers MySQL, REST Assured specifications
        |   |-- domain/DomainIsolationTest.java
        |   |-- persistence/MigrationTest.java
        |   |-- i18n/MessageBundleParityTest.java
        |   |-- api/ApiIndexTest.java, LocalizedProblemDetailsTest.java, ApiDocumentationCompletenessTest.java
        |   |-- api/v1/EntryPointTest.java
        |   `-- web/WebRoutingTest.java
        `-- resources/
            |-- minitest/veredito_minitest.csv
            |-- schemas/problem-details.json
            `-- static/{pt-BR,en-US,es-ES}/index.html   # stubs for the routing tests

frontend/
|-- package.json, angular.json, tsconfig*.json
|-- scripts/check-i18n.mjs               # XLIFF completeness (FR-017)
|-- scripts/write-build-info.mjs         # VEREDITO_VERSION -> src/app/build-info.ts
|-- src/
|   |-- app/
|   |   |-- core/                        # entry point client (HAL), Accept-Language interceptor, chunk-error handler
|   |   |-- shell/                       # header, footer (version, language links), staging banner
|   |   |-- home/                        # home page with "Participate"
|   |   |-- participate/                 # provisional page until spec 004
|   |   `-- unavailable/, not-found/
|   `-- locale/messages.pt-BR.xlf, messages.es-ES.xlf
`-- e2e/                                 # Playwright: playwright.config.ts, locale-smoke.spec.ts

Dockerfile, .dockerignore, compose.yaml, .env.example, .editorconfig, .gitignore
.github/workflows/ci.yml, deploy.yml
docs/cloud-setup.md                      # one-time gcloud commands (no secret values)
```

**Structure Decision**: a two-folder web application (`backend/`, `frontend/`) packaged into one
image, as set by the constitution (single artifact) and the layout of research 001.
- **Back end:** a single Maven module. The domain package stays isolated by an ArchUnit test
  instead of a separate module.
- **Front end:** a single Angular application with three localized builds.
- **End-to-end tests:** live in `frontend/e2e/` to share the Node toolchain.

## Complexity Tracking

No violations.
