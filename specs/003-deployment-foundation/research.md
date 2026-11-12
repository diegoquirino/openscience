# Research: Deploy Veredito and prepare the database

**Feature**: `003-deployment-foundation` | **Date**: 2026-11-12 | **Spec**: [spec.md](spec.md)

This research starts from the technology selection of spec 001
([research.md](../001-oracle-labeling/research.md)) and only records what spec 003 decides on top of
it: the versions checked today, the open points that 001 left to this plan, and the design choices
that the spec does not fix.

## 1. Versions (checked on 2026-11-12)

| Component | Version | Note |
|---|---|---|
| Java | 25 LTS (Eclipse Temurin) | Images `eclipse-temurin:25-jdk` (build) and `eclipse-temurin:25-jre` (runtime). The researcher's machine has JDK 17; install Temurin 25 to run `./mvnw` outside Docker |
| Maven | 3.9.16 through the Maven Wrapper | |
| Spring Boot | 4.1.2 | 4.2.0 is at RC2 (2026-11-05), GA due on 2026-11-19. Start on 4.1.2; moving to 4.2.x is a minor update (allowed by Principle I) and is done before the first production deployment if 4.2.0 is out. Neither `RestTemplate` nor `TestRestTemplate` is used, so the 4.2 deprecations do not affect the plan |
| Spring HATEOAS, Spring JDBC, Spring Mail, Testcontainers, Jackson | managed by Spring Boot | |
| springdoc-openapi | 3.2.0 (`springdoc-openapi-starter-webmvc-ui`, Swagger UI 5.34) | Built on Spring Boot 4.1.2 |
| Flyway (open source) | 13.12.1 with `flyway-mysql` | Certified up to MySQL 9.4; MySQL 9.7 is confirmed by the Testcontainers tests (see section 3) |
| MySQL Community | 9.7 (Cloud SQL 9.7.2; image `mysql:9.7`) | |
| Cloud SQL Java connector | 1.30.2 (`com.google.cloud.sql:mysql-socket-factory-connector-j-8`) | Production only |
| REST Assured | 6.3.0 (`rest-assured`, `json-schema-validator`) | |
| ArchUnit | 1.5.1 (`archunit-junit5`) | Keeps the domain layer free of Spring, JDBC and HTTP |
| Angular, Angular CDK, Angular Material, `@angular/localize` | 22.2.6 | 22.3.0 is at RC; minor updates are free |
| Angular unit tests | default runner of Angular 22 (Vitest) | |
| Node.js | 26 LTS (26.12.1 on the researcher's machine; image `node:26-bookworm-slim`) | Front-end build only |
| Playwright | 1.65.0 (`@playwright/test`, Chromium) | End-to-end smoke tests per locale |
| Mailpit | v1.33.1 (image `axllent/mailpit:v1.33.1`) | Local staging and development |
| gitleaks | latest 8.x image, pinned in the workflow | Secret search in CI (SC-006) |

Tools still missing on the researcher's machine: JDK 25 and the Google Cloud CLI (`gcloud`).
Docker 29.8.1 is installed (Docker Desktop must be running).

## 2. Cloud SQL edition and machine

- **Decision**: one Cloud SQL for MySQL 9.7 instance created with `--edition=ENTERPRISE` and
  `--tier=db-f1-micro`, in `southamerica-east1`, daily backup with 7-day retention.
- **Rationale**: since 2026-08-13, Cloud SQL creates MySQL 9.7 instances in the Enterprise Plus
  edition by default, and Enterprise Plus only offers dedicated machines (N2, C4A, C4, C4D), far
  above the budget. The Enterprise edition accepts MySQL 9.7 with shared-core machines, but the
  edition must be given explicitly. MySQL 9.7 no longer accepts `mysql_native_password`; Connector/J
  already uses `caching_sha2_password`.
- **Alternatives considered**: Enterprise Plus (no shared-core machine; cost); MySQL 8.4 on
  `db-f1-micro` (kept as the fallback, together with `db-g1-small`, about US$ 26 per month, still
  under the R$ 200 budget, if 0.6 GB of RAM is not enough for MySQL 9.7 in the pilot).

## 3. Flyway with MySQL 9.7

- **Decision**: Flyway 13.12.1 with `flyway-mysql`; the migration tests run against `mysql:9.7`
  with Testcontainers, so any incompatibility appears in CI before production.
- **Rationale**: Flyway lists MySQL up to 9.4 as certified; newer versions produce only a warning.
  The same migrations run in the tests, in local staging and in production.
- **Alternatives considered**: Liquibase (the constitution fixes Flyway); MySQL 8.4 (fallback only
  if the tests fail on 9.7).

## 4. Database connection in production

- **Decision**: the application connects through the Cloud SQL Java connector
  (`socketFactory=com.google.cloud.sql.mysql.SocketFactory`, `cloudSqlInstance=...`). The instance
  has a public IP with **no authorized networks** and `--connector-enforcement=REQUIRED`, so only
  clients authorized by IAM (`roles/cloudsql.client`) and using the connector get in. The
  application uses a built-in database user, `veredito_app`, whose password is in Secret Manager
  (`veredito-db-password`). The researcher connects with the Cloud SQL Auth Proxy and their own
  IAM identity, using a separate database user created by hand, whose password stays outside the
  repository. Connection pool: HikariCP with at most 5 connections per instance.
- **Rationale**: meets FR-006 (database not reachable from the internet without IAM) with no extra
  network service. The connector handles TLS and certificate rotation.
- **Alternatives considered**: private IP (needs a VPC and a Serverless VPC Access connector or
  Direct VPC egress: more pieces, Principle V); IAM database authentication (no password at all,
  but the user name is derived from the service account and the researcher would need a second
  path; can be adopted later without changing the schema).

## 5. Data access in the application

- **Decision**: Spring JDBC (`JdbcClient`), with no JPA/Hibernate. Flyway owns the schema; the
  application never generates DDL.
- **Rationale**: the critical operations of later specs need explicit SQL
  (`SELECT ... FOR UPDATE SKIP LOCKED`, `INSERT ... ON DUPLICATE KEY UPDATE`, short transactions,
  Principle III); an ORM would hide exactly what must be visible and tested.
- **Alternatives considered**: Spring Data JPA (entity state and lazy loading get in the way of
  the locking rules); jOOQ (code generation and one more tool to learn).

## 6. Shape of the schema

- **Decision**: one migration `V1__create_schema.sql` with every table of the data model of spec 001
  and one `V2__insert_default_config.sql` with the default configuration
  ([data-model.md](data-model.md)). Character set `utf8mb4`; default collation
  `utf8mb4_0900_ai_ci`; identifiers compared exactly (`item.system`, `item.edit_key`, e-mails,
  hashes) use `utf8mb4_0900_bin`. Instants are `DATETIME(6)` in UTC (Connector/J with
  `connectionTimeZone=UTC` and `preserveInstants=true`). Codes and locales have `CHECK`
  constraints with their English values. From V3 on, every migration must be compatible with the
  previous version of the code (expand first, contract later), so that a rollback of Cloud Run
  never meets a schema it cannot read.
- **Rationale**: FR-004 asks for all the tables at once; separating schema and data keeps V1 pure
  DDL. `CHECK` constraints keep the stored codes in English whatever the UI language (FR-019) and
  are enforced by MySQL since 8.0.16.
- **Alternatives considered**: one migration per table (more files, same result); enums
  (`ENUM(...)`) instead of `VARCHAR` with `CHECK` (adding a value rewrites the column definition).

## 7. The `config` table

- **Decision**: a single-row table (`id = 1`, enforced by `CHECK`) with one typed column per
  parameter: `admin_email`, `admin_preferred_language`, `reservation_minutes`, `block_size`,
  `daily_email_limit`, `study_phase`, `study_phase_changed_at`. The specs that wrote the study
  phase setting as `study-phase` (001 research, 003, 005, GLOSSARY) now name it `study_phase`, the
  column; the values stay `pilot` and `collection`. In JSON, the field is `studyPhase`.
- **Rationale**: typed columns allow `CHECK` and `NOT NULL` constraints (Principle III), and the
  name matches `config.admin_preferred_language`, already used by specs 001 and 004. A key-value
  table would store every value as text, without constraints.
- **Alternatives considered**: key-value table with kebab-case keys (`study-phase`); no type or
  range checks in the database.

## 8. Table name for sign-in sessions

- **Decision**: `access_session` instead of `session`.
- **Rationale**: `SESSION` is a MySQL keyword (not reserved, but it appears in `SET SESSION` and
  system variables) and the spec entity is "Access session". The other table names of spec 001
  stay as they are.
- **Alternatives considered**: `session` with backquotes everywhere.

## 9. Deployed version shown in the footer

- **Decision**: the version string is `<project version>+<short commit>`, for example
  `0.3.0+1a2b3c4`. The project version is the one in `backend/pom.xml`, raised at the end of each
  spec (0.3.0 for spec 003). CI computes the string and passes it to the image build as the
  argument `VEREDITO_VERSION`; the front-end build writes it into a generated constant and the
  back-end build into `BuildProperties`. The footer shows the front-end constant (it works even
  when the API is down); the API entry point and the OpenAPI `info.description` show the back-end
  one. Outside CI, the version is `0.3.0+dev`.
- **Rationale**: FR-003 and SC-002 need a version that changes on every deployment and that the
  researcher can compare between local staging and production; the commit identifies the code
  exactly.
- **Alternatives considered**: build date (does not identify the code); reading the version from
  the API only (the footer would go blank when the database is down).

## 10. Environment banner and database availability

- **Decision**: the entry point `GET /api/v1` returns, besides its links, the properties
  `systemVersion`, `environment` (`production`, `staging` or `development`) and `available`. It
  checks the database with a cheap validation query; when the database is unreachable,
  `available` is `false` and no action link is offered. The Angular shell shows the localized
  "STAGING ENVIRONMENT" banner when `environment` is not `production`, and the localized "System
  temporarily unavailable" message when `available` is `false` or the call fails.
- **Rationale**: the same image runs in staging and production, so the environment can only come
  from the server at run time. Offering no action links while the database is down is the level 3
  way to say "nothing can be done now" (edge case "Database deleted or unreachable").
- **Alternatives considered**: a separate health endpoint for the front end (duplicates the entry
  point); Spring Boot Actuator health exposed publicly (exposes more than needed).

## 11. Serving the three locales and the root page

- **Decision**: the Angular builds go into `static/pt-BR/`, `static/en-US/` and `static/es-ES/` of
  the jar; the language-choice page is a static `static/index.html` (plain HTML, CSS and
  JavaScript, texts in the three languages). A single servlet filter, before Spring MVC, applies
  the routing rules of [contracts/web-routing.md](contracts/web-routing.md):
  canonical prefix (case-insensitive match, `301` to the canonical form), SPA fallback per prefix
  (paths without a file extension are forwarded to that locale's `index.html`), and `302` to `/`
  for any other path outside `/api`, the API documentation and the static files of the root.
  `index.html` files go out with `Cache-Control: no-cache`; hashed bundles with
  `Cache-Control: public, max-age=31536000, immutable`.
- **Rationale**: one rule set, tested with REST Assured, covers FR-014, FR-015 and the edge cases.
  `no-cache` on `index.html` and immutable bundles mean that after a deployment the next page load
  gets the new version, while an open page keeps its bundles until reload. If the open page asks
  for a lazy chunk that the new version no longer has, the Angular router error handler shows the
  localized "A new version is available; reload the page" message (edge case "Page open for hours
  before a deployment").
- **Alternatives considered**: one `@Controller` per locale with `forward:` mappings (does not
  cover case and unsupported prefixes in one place); serving the front end from a separate service
  or bucket (more pieces, CORS).

## 12. API versioning, index and entry point

- **Decision**: the native API versioning of Spring Framework 7, with the version as the path
  segment after `/api`, only `v1` supported and detection of other versions off; an unsupported
  version answers `400` Problem Details with the list of supported versions. `GET /api` (outside
  any version) lists the versions. `GET /api/v1` is the entry point: in 003, a visitor gets
  `self`, `curies` and `vd:request-code`; the operation behind `vd:request-code`
  (`POST /api/v1/access-codes`) is delivered by spec 004, so in 003 the link is advertised and the
  address answers `404` Problem Details. `GET /api/v1/rels/{rel}` documents each `vd:` relation.
  Details in [contracts/api.md](contracts/api.md).
- **Rationale**: US5 scenario 2 asks the entry point to offer only the visitor link from spec 003
  on; the REST Assured tests of 003 check its presence, and spec 004 adds the operation without
  changing the link.
- **Alternatives considered**: leaving the entry point without links until 004 (US5 scenario 2
  would fail); a temporary `501` operation (would be documented and then replaced).

## 13. Problem Details and localized messages

- **Decision**: `@RestControllerAdvice` extending `ResponseEntityExceptionHandler`, which turns
  every error (Spring's and the application's) into `application/problem+json` with a type
  `urn:veredito:problem:<slug>`. `title` and `detail` come from `MessageSource` with the keys
  `problem.<slug>.title` and `problem.<slug>.detail`. Locale resolution:
  `AcceptHeaderLocaleResolver` with supported locales `pt_BR`, `en_US`, `es_ES` (language-only
  match: `pt` gives pt-BR) and default `pt_BR`; `spring.messages.fallback-to-system-locale=false`.
  Bundles: `messages.properties` (en-US, base), `messages_pt_BR.properties`,
  `messages_es_ES.properties`, UTF-8. Unexpected errors return `500` with an `incidentId`
  extension, and the stack trace is logged with the same id. Catalog in
  [contracts/problem-details.md](contracts/problem-details.md).
- **Rationale**: FR-009, FR-012 and FR-016 with standard Spring pieces only. Keys of our own (not
  the `problemDetail.*` codes of Spring) keep one key set for all errors, which the bundle parity
  test checks. A URN type is stable and does not depend on a domain that the study does not own.
- **Alternatives considered**: `about:blank` types (the client could not tell errors apart);
  `https://` type URIs (no domain of our own; the Cloud Run URL may change if a custom domain is
  added).

## 14. Swagger access and the documentation test

- **Decision**: springdoc with one `GroupedOpenApi` per API version (`v1`, paths `/api/v1/**`) and
  one for the version index (`index`, path `/api`). In the `staging` and development profiles,
  Swagger UI and `/v3/api-docs/**` are open. In the `prod` profile, a filter refuses them with
  `401` Problem Details unless there is an administrator session; since sessions only exist from
  spec 004 on, in 003 production always refuses. The test `ApiDocumentationCompletenessTest`
  (REST Assured) reads every group and fails, naming the operation, when an operation lacks
  `summary`, `description`, a success example or an error response with the Problem Details
  schema. CI saves `openapi-v1.json` and `openapi-index.json` as workflow artifacts.
- **Rationale**: FR-013, SC-007 and US5 scenarios 3 and 4.
- **Alternatives considered**: disabling springdoc in production (the administrator would lose the
  documentation in 004); Spring Security in 003 only for this filter (the session design belongs
  to 004).

## 15. Front-end internationalization checks

- **Decision**: `angular.json` with `i18n.sourceLocale` `{ code: "en-US", subPath: "en-US" }`,
  locales `pt-BR` and `es-ES` (XLIFF 2.0 files in `frontend/src/locale/`, each with its `subPath`),
  `localize: true` and `i18nMissingTranslation: "error"`. The npm script `i18n:check` runs
  `ng extract-i18n --format=xlf2` to a temporary folder and a small Node script (no dependency)
  compares the unit ids with `messages.pt-BR.xlf` and `messages.es-ES.xlf`, failing on a missing
  unit or an empty `<target>`, with the unit id, its source text and the locale in the message.
  The back-end bundle parity is a JUnit test.
- **Rationale**: FR-017 and SC-008; the build option alone does not report every missing unit at
  once, and the check also catches empty targets.
- **Alternatives considered**: a third-party XLIFF tool (one more dependency for a short script).

## 16. End-to-end smoke tests

- **Decision**: Playwright 1.65.0 with Chromium, in `frontend/e2e/`, run against local staging
  (`http://localhost:8080`). In 003, per locale: open `/`, choose the language, check the prefix,
  the `lang` attribute and a translated label of the home page and footer; reload a deep link
  under the prefix; plus `/fr-FR/` going to `/` and `/en-us/` going to `/en-US/`, and a phone
  viewport without horizontal scrolling. Spec 004 extends the same tests with the sign-in.
- **Rationale**: 001 left the choice of the browser automation tool to this plan. Playwright is
  open source, runs on Node 26 and on the GitHub runners, and also covers the phone viewport of
  US1 scenario 2.
- **Alternatives considered**: Cypress (single-tab model, heavier in CI); Selenium (more setup for
  the same checks).

## 17. Image, local staging and development

- **Decision**: a multi-stage `Dockerfile` (Node 26 builds the front end; JDK 25 packages the jar
  with the three builds in `static/` and skips the tests, which CI has already run; JRE 25 runs as
  a non-root user with `-XX:MaxRAMPercentage=75`). `compose.yaml` has `mysql` (`mysql:9.7`) and
  `mailpit` without a profile (development: Spring Boot's Docker Compose support starts them), and
  `app` in the Compose profile `staging`, built from the same `Dockerfile` and run with the Spring
  profile `staging`, at `http://localhost:8080`, with its own volume. `docker compose --profile
  staging up --build` is the single start command; `down -v` starts over.
- **Rationale**: FR-007, US4 and SC-001. The image of production is built by CI from the same
  commit and the same `Dockerfile`; the footer version (commit) lets the researcher confirm that
  staging and production run the same code (US4 scenario 3).
- **Alternatives considered**: pulling the CI image from Artifact Registry for staging (needs
  Google Cloud credentials on the machine for every rehearsal); Spring Boot buildpacks (less
  control over the front-end stage).

## 18. CI and deployment

- **Decision**: two workflows in the `Veredito` branch.
  - `ci.yml`, on every push to `Veredito`: back-end job (`./mvnw -B verify`, with Testcontainers;
    uploads the OpenAPI files), front-end job (`npm ci`, `npm run i18n:check`, `npm test`,
    `npm run build`), secret search (gitleaks over the branch history), then image job (build with
    `VEREDITO_VERSION`, push to Artifact Registry as `.../veredito/app:<commit>`), then e2e job
    (Compose `staging` with that image, Playwright).
  - `deploy.yml`, on a pushed tag `deploy/*`: resolves the tagged commit, checks that `ci.yml`
    succeeded for it and that its image exists, and runs `gcloud run deploy veredito` with that
    image digest. The researcher deploys with a single command:
    `git push origin <commit>:refs/tags/deploy/<version>`.
  - Authentication: Workload Identity Federation, pool `github`, provider restricted to the
    repository `diegoquirino/openscience` and to the refs `refs/heads/Veredito` and
    `refs/tags/deploy/*`. No service account key anywhere.
  - Rollback: `gcloud run services update-traffic veredito --to-revisions=<previous>=100`.
- **Rationale**: FR-010 and SC-001. A `workflow_dispatch` trigger would require the workflow file
  on the default branch of the repository, and `Veredito` is an orphan branch that must stay apart
  from the other studies; a tag push runs the workflow file of the tagged commit itself.
- **Alternatives considered**: `workflow_dispatch` (needs a file on the default branch); deploying
  on every push (production would receive versions not rehearsed in local staging).

## 19. Google Cloud resources

| Resource | Configuration |
|---|---|
| Project | `veredito-issta2027`, linked to the researcher's billing account (done by the researcher) |
| APIs | Cloud Run, Cloud SQL Admin, Secret Manager, Artifact Registry, IAM Credentials, Cloud Billing Budget |
| Artifact Registry | Docker repository `veredito` in `southamerica-east1`; cleanup policy keeping the 5 most recent images |
| Cloud Run | Service `veredito`, `southamerica-east1`, public (`--allow-unauthenticated`), 1 vCPU, 1 GiB, min 0, max 4 instances, startup CPU boost, Spring profile `prod` |
| Cloud SQL | Section 2; database `veredito`; user `veredito_app` |
| Secret Manager | `veredito-db-password`; `veredito-smtp-password` (the Gmail app password, created now and used from spec 004 on) |
| Service accounts | `veredito-run` (runtime: Cloud SQL client, access to the two secrets), `veredito-ci` (Artifact Registry writer), `veredito-deploy` (Cloud Run developer, acts as `veredito-run`) |
| Budget | "Veredito mensal", R$ 200 per month for this project only, alerts at 50%, 90% and 100%, no action on the service (SC-005; created by the researcher in the console) |

Billing, budget and any step that needs payment data or a second factor are done by the
researcher by hand; the plan only lists them.

## 20. Domain layer from the start

- **Decision**: the package `...veredito.domain` is created in 003, with an ArchUnit test that fails
  if any class in it depends on Spring, `java.sql`, `jakarta.servlet` or the web and persistence
  packages. The Minitest fixture `veredito_minitest.csv` is copied to
  `backend/src/test/resources/minitest/` in 003.
- **Rationale**: FR-008 and Principle II; the rules themselves (CSV validation, blocks, queue,
  exports) come with specs 005, 007 and 009, which find the rule and the fixture already in place.
- **Alternatives considered**: a separate Maven module for the domain (stronger isolation, but a
  multi-module build for a single small application).
