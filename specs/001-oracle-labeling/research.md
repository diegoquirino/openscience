# Technology selection: Veredito

**Feature**: `001-oracle-labeling` | **Date**: 2026-11-11 | **Spec**: [spec.md](./spec.md) | **Constitution**: 2.7.0

This revision replaces the 2026-11-10 decision (Apps Script with Google Sheets), discarded because of doubts about the free environment.

## Decision

**Angular with Angular Material on the front end, Spring Boot on the back end and MySQL Community with Flyway, in a single container deployed to Cloud Run, with the database on Cloud SQL. The code lives in the orphan branch `Veredito` of the public repository `diegoquirino/openscience`.**

**Environments**: **production** is Google Cloud; **staging** (testing) is **local, in Docker**, on the researcher's machine. There is no cloud staging.

## Versions (checked on 2026-11-11)

| Component | Version | Note |
|---|---|---|
| Angular, Angular CDK and Angular Material | 22.2.x | Material Design 3 |
| `@angular/localize` | 22.2.x | Official Angular internationalization; always on the same version line as Angular (see *Internationalization*) |
| Node.js (only to build the front end) | 26 LTS | |
| Java | 25 LTS (Eclipse Temurin) | |
| Spring Boot | 4.1.2 | 4.2.0 is at RC2, with the final release expected in November 2026; use 4.2.x if it is already out when the plan for spec 003 is written |
| Spring `MessageSource` | part of Spring Framework (managed by Spring Boot) | Back-end messages in the user's language (Problem Details, e-mails); no extra dependency |
| Maven | 3.9.x or later | Through the Maven Wrapper |
| MySQL Community | 9.7 LTS | Also the version of the development and test container |
| Spring HATEOAS | managed by Spring Boot (`spring-boot-starter-hateoas`) | HAL-FORMS hypermedia for the API |
| springdoc-openapi | 3.2.0 (`springdoc-openapi-starter-webmvc-ui`, Swagger UI 5.34) | OpenAPI 3.1 and Swagger UI. 3.2.x is built for Spring Boot 4.1.x; with Spring Boot 4.2, use the matching 3.x. With the Spring Boot parent in Maven, Jackson stays at the version managed by Boot |
| Flyway (open source) | 13.12.1 | With the `flyway-mysql` module; override the version managed by Spring Boot if it is older |
| Testcontainers | managed by Spring Boot | MySQL 9.7 in a container for integration tests |
| REST Assured | 6.3.0 (`rest-assured` and `json-schema-validator`, test scope) | API tests. Supports Spring Boot 4, Jackson 3 and JSON Schema 2020-12; override the version managed by Spring Boot (`rest-assured.version`) if it is older |

## Architecture

```text
Browser (Angular + Angular Material, one build per locale)
        |  HTTPS, HttpOnly session cookie, Accept-Language = UI locale
        v
Cloud Run: single container
  Spring Boot 4 (Java 25)
   |- /             -> static language-choice page (redirects when cookie veredito-lang is set)
   |- /pt-BR/, /en-US/, /es-ES/ -> compiled Angular, one build per locale (SPA fallback per prefix)
   |- /api/v1/**    -> level 3 REST API (HAL-FORMS, Spring HATEOAS), versioned in the path;
   |                   Problem Details and e-mails localized through MessageSource
   |- domain in plain Java (CSV, blocks, queue, exports)
   '- Flyway runs the migrations at startup
        | Cloud SQL connector (IAM)             | SMTP (TLS)
        v                                       v
Cloud SQL for MySQL 9.7                     smtp.gmail.com
  database: veredito                        (diegoquirino@gmail.com)
Secret Manager: database password, Gmail app password, session key
```

### Layout of the `Veredito` branch

```text
/                       root of the orphan branch (content of the Veredito folder)
|- backend/             Spring Boot (Maven Wrapper)
|  |- src/main/resources/db/migration/   Flyway migrations V1__..., V2__...
|  '- src/main/resources/messages*.properties   MessageSource bundles (en-US, pt-BR, es-ES)
|- frontend/            Angular + Angular Material
|  '- src/locale/       messages.pt-BR.xlf, messages.es-ES.xlf (XLIFF 2.0)
|- compose.yaml         MySQL 9.7 (development) and, in the `staging` profile, app + MySQL + Mailpit
|- Dockerfile           multi-stage build: Node 26 -> Maven/JDK 25 -> JRE 25 (the same for staging and production)
|- .env.example         local staging variables, no secrets (the real `.env` is ignored by git)
|- .github/workflows/   tests, image and deployment
|- specs/, .specify/, .cursor/
'- .gitignore
```

- In development, `ng serve` forwards `/api` to Spring Boot at `localhost:8080`, and Spring Boot's Docker Compose support starts MySQL on its own. `ng serve` serves one locale at a time (the source locale en-US by default; a translated locale through its build configuration, e.g. `ng serve --configuration=pt-BR`).
- In the image, the three compiled Angular builds go into Spring Boot's static resources, which serves everything from the same URL (no CORS).

## Local staging (Docker)

The test environment runs on the researcher's machine with Docker Compose (`docker compose --profile staging up --build`):

| Component | Configuration |
|---|---|
| `app` | Image built by the same `Dockerfile` as production, Spring profile `staging`, at `http://localhost:8080`; a "STAGING ENVIRONMENT" banner on screen (localized like the rest of the UI) |
| `mysql` | MySQL Community 9.7 (same version as Cloud SQL), its own volume; Flyway runs the same migrations at startup |
| `mailpit` | Test mail server ([Mailpit](https://mailpit.axllent.org/), open source): receives the access codes without actually sending them; inbox at `http://localhost:8025` |
| Data | Only the Minitest items (see "Staging and pilot data"), with fictitious participants; nothing from production. The import refuses any other system in this profile |
| Secrets | No real secret is needed; to test sending through Gmail, the app password goes into a local `.env`, ignored by git |
| Start over | `docker compose --profile staging down -v` deletes the database volume |

- The 30-concurrent-participant test and the other integration tests run with Testcontainers, without depending on Compose.

### Staging and pilot data: Minitest only

Local staging, the automated tests and the pilot in production use only items of the system `minitest` (38 edits, versions 1.0 to 2.2, tags `minitest_v*` of this same public repository).

| Use | Data |
|---|---|
| Reference file | `veredito_minitest.csv` generated by spec 002, copied to `backend/src/test/resources/minitest/veredito_minitest.csv` and versioned. When the pipeline generates the file again, the copy is replaced in the same commit |
| Error cases | Files derived from the reference file, in the same folder, named after the defect (for example, `minitest_duplicates.csv`, `minitest_missing_kind_column.csv`, `minitest_long_text.csv`) |
| Volume cases | Generated in memory by the tests, repeating the Minitest rows with a suffix in `edit_key` (`LoginTest\|1\|1#r07`), up to 205 or 500 rows; not written to a file |
| Participants | Fictitious, with e-mails in the reserved domain `example.com`; in local staging the codes arrive in Mailpit |
| Local staging | The researcher imports the reference file through the administrator screen; the `staging` profile refuses any file whose `system` is not `minitest` |
| Pilot in production | The "study phase" setting (`study-phase`) starts at `pilot` on a new database; in this phase, the import also refuses any other system. After the pilot, the database is recreated empty (again at `pilot`), the administrator switches the phase to `collection`, which is only accepted with no items, participants or responses in the database, and then imports the real dataset |

- Staging uses the same image and the same migrations as production; only the variables change (database, SMTP, staging banner).

## Production on Google Cloud

| Component | Configuration |
|---|---|
| Region | `southamerica-east1` (Sao Paulo), close to the volunteers; `us-central1` is cheaper if cost gets tight |
| Cloud Run | A single service, `veredito`; minimum 0 instances, maximum 4; *startup CPU boost* on |
| Cloud SQL | One MySQL 9.7 instance, Enterprise edition, the smallest shared-core machine (`db-f1-micro`, 0.6 GB, 25 connections); a single database, `veredito`, with its own user; automatic daily backup with 7-day retention |
| Database access | No authorized networks; only Cloud Run gets in, through the Cloud SQL connector with IAM |
| Connection pool | At most 5 per Cloud Run instance, to fit within the 25 connections |
| Secrets | Secret Manager, injected as Cloud Run environment variables |
| Images | Artifact Registry, keeping only the 5 most recent images |
| Project | `veredito-issta2027`, linked to the existing billing account (in reais) |
| Budget | "Veredito mensal": R$ 200 per month for this project only, alerts only, at 50%, 90% and 100% |
| URL | The Cloud Run default (`https://veredito-....run.app`); a custom domain is optional |

**Estimated cost**: about US$ 10 to 13 per month, almost all of it Cloud SQL (`db-f1-micro` costs about US$ 7.70 per month in `us-central1`, plus disk and backup; Sao Paulo is a little more expensive). Cloud Run, Artifact Registry, Secret Manager and GitHub Actions (public repository) stay within the free tier. New accounts get US$ 300 of credit for 90 days.

## Deployment

GitHub Actions on the `Veredito` branch:

1. **On every push**: back-end tests (`./mvnw verify`, with Testcontainers, including the API documentation test) and front-end tests (including the translation completeness check, see *Internationalization*); the OpenAPI specification of each API version (`openapi-v1.json`, ...) kept as a workflow artifact; image build from the `Dockerfile`; push to Artifact Registry tagged with the commit. Nothing is deployed.
2. **Staging**: the researcher runs the same commit in local Docker (section *Local staging*) and checks the spec's success criteria.
3. **Production**: manually triggered workflow, given the staged commit; deploys to `veredito` the image of that commit already pushed to Artifact Registry. The URL does not change.
4. **Authentication**: Workload Identity Federation between GitHub and Google Cloud, with no service account key stored in GitHub.
5. **Rollback**: Cloud Run keeps the previous revisions; just route the traffic to the previous one. For this reason, every Flyway migration must be compatible with the previous version of the code (add first, remove later).
6. **End of the pilot**: the pilot with colleagues runs in production, with only the Minitest items (phase `pilot`). Before the call for volunteers, the `veredito` database is dropped and created again in Cloud SQL, and the next deployment prepares it empty through Flyway. The administrator switches the phase to `collection` and imports the real dataset. `flyway clean` stays disabled in production.

## Access code by e-mail

- Spring Mail through Gmail SMTP (`smtp.gmail.com:587`, STARTTLS), with an app password of `diegoquirino@gmail.com`; the account needs two-step verification.
- Limit of about 500 recipients per day on a regular Gmail account. The system counts the day's sends and stops at a configurable limit (default 450), showing the message from spec 004.
- The e-mail may land in spam; the screen warns about it.
- The e-mail is written in the language of the request (the UI locale sent in `Accept-Language`), from per-locale templates (see *Internationalization*).

## Internationalization

### Decision

**Angular official i18n (`@angular/localize`), one build per locale (pt-BR, en-US, es-ES) served by the same Spring Boot under `/pt-BR/`, `/en-US/` and `/es-ES/`; back-end messages through Spring `MessageSource` in the user's language. Data is never translated.**

| Topic | Decision |
|---|---|
| Front-end source and translations | `@angular/localize` with source locale **en-US**: the texts in the templates (`i18n` attributes) and in the code (`$localize`) are written in English. `ng extract-i18n --format=xlf2 --output-path src/locale` extracts the source file; the translation files `messages.pt-BR.xlf` and `messages.es-ES.xlf` (XLIFF 2.0) are derived from it, with a `<target>` for every unit, and are versioned in `frontend/src/locale/` |
| Build | `angular.json`: block `i18n` with `sourceLocale` `en-US` (`baseHref` `/en-US/`) and `locales` `pt-BR` and `es-ES` (each with its `translation` file and `baseHref` `/pt-BR/` or `/es-ES/`); the build option `localize: true` produces three builds, one folder per locale; `i18nMissingTranslation: "error"` makes the build fail if a translation is missing |
| Locale in the front end | Each build has its own `LOCALE_ID` (set by the localized build, with the locale data of pt-BR, en-US or es-ES); the Angular date, number and percentage pipes format by that locale; the Angular Material date adapter receives the same locale (`MAT_DATE_LOCALE` from `LOCALE_ID`) |
| Serving | The same Spring Boot jar serves the three builds as static resources (`static/pt-BR/`, `static/en-US/`, `static/es-ES/`). SPA fallback per locale prefix: an unknown path under `/xx-YY/` (other than a static file) returns that locale's `index.html`, so deep links and page reloads work; `/api/**` is never caught by the fallback |
| Root `/` and first screen | `/` is a small static page (plain HTML and JavaScript, outside Angular, with its texts in the three languages) where the person chooses the language before signing in. It preselects the first supported language in `navigator.languages` (fallback pt-BR). When the cookie `veredito-lang` is already set, it redirects at once to `/<locale>/` |
| Switching language | From the header: the front end sets the cookie `veredito-lang` (path `/`, one year, `SameSite=Lax`), saves the language in the profile when signed in (`preferredLanguage` through the `vd:me` template: `/api/v1/participants/me` for a participant, `/api/v1/administrator/me` for the administrator, stored in `config.admin_preferred_language`), and reloads the same route under the new prefix (for example, `/pt-BR/tasks/12` -> `/es-ES/tasks/12`). After sign-in, a saved profile language different from the current prefix triggers the same switch |
| Unsaved input on a switch | Before the reload, the open form writes its unsaved values to `sessionStorage` under `veredito-unsaved:<route>` (for the item screen, `veredito-unsaved:/tasks/{id}/items/{position}`: impact, operation and comment). `sessionStorage` belongs to the tab and to the origin, so it survives the move from one locale prefix to another. After the reload, the same form restores the values as unsaved changes and deletes the entry; it is also deleted when the values are saved or the person leaves the screen. Nothing is sent to the server until the person saves; a file input cannot be restored |
| Back-end messages | `MessageSource` bundles in `backend/src/main/resources/`: `messages.properties` (en-US texts, the source language, also the base bundle), `messages_pt_BR.properties`, `messages_es_ES.properties`; `spring.messages.fallback-to-system-locale=false`, so that the server's own locale is never used; the completeness test keeps a missing key from ever reaching the base bundle |
| Request locale | An Angular HTTP interceptor sends `Accept-Language` with the build's locale on every API call (the browser's own `Accept-Language` may differ from the chosen language). On the server, `AcceptHeaderLocaleResolver` limited to the three supported locales (a language-only match such as `pt` resolves to `pt-BR`); a missing or unsupported language falls back to pt-BR (default locale `pt_BR`), the same fallback as the language-choice page (spec 003, FR-016) |
| Problem Details | `title` and `detail` are resolved through `MessageSource` in the request locale (the Spring Framework `problemDetail.title.*` and `problemDetail.*` message codes, and the same for the application's own error types); the `type` URI and the validation field names do not change with the language |
| E-mails | Subject and body in the request locale (access code requested before sign-in) or in the participant's `preferred_language` (when there is no request, e.g. a message sent later); one template per e-mail and locale under `backend/src/main/resources/mail/` |
| Consent form | Table `consent_form` (one row per version: `version`, `reference_language` = `pt-BR`, `published_at`) and table `consent_form_text` (one row per version and language: pt-BR reference, en-US, es-ES). The pt-BR text is the version approved by the research ethics committee; the en-US and es-ES texts state that they are translations and link to the pt-BR reference. The texts are data: stored as published and never edited afterwards. The acceptance stores the consent form version, the language in which it was shown and the time of the decision |
| Captured data | Each response stores `language` (the UI locale at save time; updated on each draft save, so the final response keeps the language of its last save); each participant stores `preferred_language`; imported items are stored exactly as imported (English use cases from the pipeline); participant comments stay in the language in which they were written. Exports include `language`, `preferred_language` and the accepted consent form language |
| Codes and labels | `change_impact`, `maintenance_operation`, `professional_skill`, screening answers and study phase are stored and exported as English identifiers; their display labels live in the translation files (front end) and in the `MessageSource` bundles (back end), never in the database. The HAL-FORMS `_templates` carry codes and field names, not display text |
| Missing use case | The import CSV keeps the pipeline text `(the use case does not exist in this version)` unchanged; the item screen recognizes it and shows a localized message instead |

### Rationale

- `@angular/localize` is the official Angular solution, which matches the constitution's "standard, current, open-source stack"; it is released with Angular on the same version line, so it never lags behind an Angular upgrade.
- The translations are compiled into each build: no translation files are loaded at run time, no text flashes in the wrong language, and a missing translation fails the build instead of reaching a volunteer.
- One build per locale under its own prefix keeps a single container and a single URL: Spring Boot already serves the static front end, so the three builds only add three folders and a fallback rule.
- Reloading the page on a language switch is acceptable: the switch is rare (usually once, on the first screen), drafts are already saved on the server, and unsaved input is carried over in `sessionStorage`.
- `MessageSource` is part of Spring Framework and is already used by Spring's own Problem Details support, so localized errors and e-mails need no new dependency.
- Recording the UI language of each response and the language of the accepted consent form lets the analysis check whether the language affected the labels, and documents for the research ethics committee which text each participant accepted.

### Alternatives considered

| Option | Why not |
|---|---|
| Transloco | Runtime translation, single build and instant switch without reload; rejected because it is not the official Angular solution |
| ngx-translate | Same runtime model as Transloco (single build, instant switch); rejected for the same reason |
| A single locale (pt-BR only) | Volunteers may come from Spanish- and English-speaking communities |
| Locale chosen only from `Accept-Language`, without a choice screen | The browser language is often not the one the person prefers to read; it is kept only as the preselection |

### Tests

| Check | How |
|---|---|
| Translation completeness | CI step that reads `messages.pt-BR.xlf` and `messages.es-ES.xlf` and fails the build if any unit of the source file has no non-empty `<target>` in either file (in addition to `i18nMissingTranslation: "error"` in the build) |
| Back-end bundles | Unit test that loads `messages.properties`, `messages_pt_BR.properties` and `messages_es_ES.properties` and fails unless the three have exactly the same keys |
| Localized errors | REST Assured test that triggers a Problem Details response (for example, an unsupported API version) with `Accept-Language` `pt-BR`, `en-US` and `es-ES` and checks that `title` and `detail` come in each language, with the same `type` |
| Locale end to end | One end-to-end smoke test per locale: open `/`, choose the language, sign in with a code read from the test mail server and open a block; checks the prefix, the page language and a translated label. The browser automation tool is chosen in the plan for spec 003 |

## Data model (MySQL)

The initial migration (spec 003) creates all the tables; the following specs only add what is new, in their own migrations.

| Table | One row per | Main columns and constraints |
|---|---|---|
| `import_batch` | confirmed import | `file_name`, `imported_at`, counts |
| `item` | imported edit | `system`, `edit_key`, `uc`, `kind`, `field`, `origin`, `target` (`TEXT`), versions, `origin_usecase`, `target_usecase` (`MEDIUMTEXT`), `batch_id`, `block_id`, `position`; **unique** (`system`, `edit_key`) |
| `block` | block of up to 10 items | `batch_id`, `size` |
| `task` | block-role | `queue_position` (**unique**), `block_id`, `role`, `status`, `participant_id`, `reserved_at`, `expires_at`, `submitted_at`; **unique** (`block_id`, `role`) and **unique** (`block_id`, `participant_id`) |
| `response` | item in a role | `task_id`, `item_id`, `participant_id`, `state` (`draft`, `final`), `change_impact`, `maintenance_operation`, `comment`, `language`, `saved_at`; **unique** (`task_id`, `item_id`) |
| `participant` | person | `pseudonym` (**unique**), `name`, `email` (**unique**), consent form decision, version, language and time, professional skill, screening, `eligible`, `preferred_language`, `anonymized_at` |
| `access_code` | code sent | `email`, `hash`, `created_at`, `expires_at`, `attempts`, `used_at` |
| `session` | sign-in session | `token_hash`, `participant_id` or administrator, `expires_at` |
| `consent_form` | consent form version | `version`, `reference_language`, `published_at` |
| `consent_form_text` | consent form version and language | `consent_form_id`, `language`, `text`; **unique** (`consent_form_id`, `language`) |
| `config` | parameter | administrator e-mail, `admin_preferred_language`, reservation minutes, block size, daily e-mail limit |

The unique constraint (`block_id`, `participant_id`) is the database guarantee that nobody takes both roles of a block (MySQL accepts several `NULL`s in this index, so free tasks do not conflict). A use case of up to 40,000 characters fits comfortably in `MEDIUMTEXT`.

## REST API: Richardson level 3 ("glory of REST")

The API meets all three levels of the Richardson maturity model.

| Level | How Veredito meets it |
|---|---|
| 1. Resources | Every domain thing has its own URI, under the API version: `/api/v1/participants/me`, `/api/v1/administrator/me`, `/api/v1/tasks/{id}`, `/api/v1/tasks/{id}/items/{position}`, `/api/v1/tasks/{id}/responses/{itemId}`, `/api/v1/batches`, `/api/v1/batches/{id}`, `/api/v1/consent-forms/current`, `/api/v1/config`, `/api/v1/exports/{type}` |
| 2. HTTP methods and status codes | `GET` reads with no side effect; `POST` creates (`201 Created` with `Location`); `PUT` writes idempotently; `DELETE` removes or withdraws. Codes: `200`, `201`, `204`, `400`/`422` (validation), `401` (no session), `403` (no permission), `404`, `409` (state conflict, for example a block already reserved by someone else or already submitted), `429` (code or e-mail limit) |
| 3. Hypermedia | Spring HATEOAS with **HAL-FORMS** (`application/prs.hal-forms+json`). Every response carries `_links` (navigation) and `_templates` (actions with method, fields and options) that are valid **in the current state** of the resource |

**Rules**

- **Single entry point per version**: `GET /api/v1` returns only the relations allowed to the caller.
  - No session: `vd:request-code`.
  - Participant: `vd:me`, `vd:consent-form` and, when eligible, `vd:next-task`.
  - Administrator: `vd:me` (own profile, language), `vd:batches`, `vd:exports`, `vd:dashboard`, `vd:participants`.
- **Custom relations** with the CURIE prefix `vd:`, documented at `/api/v1/rels/{rel}`. Standard IANA relations (`self`, `next`, `prev`, `collection`, `item`) where they fit.
- **Reservation in the task representation**: a reserved task carries `reservationExpiresAt` (instant in UTC, ISO 8601) and `reservationMinutes`. Angular computes the on-screen countdown from the difference between `reservationExpiresAt` and the server time (the response's `Date` header), correcting the device clock, and updates the expiry on every saved response (the response `PUT` returns the task with the new `reservationExpiresAt`).
- **State decides the links.** A task reserved by the person themselves offers `vd:items`, `vd:save-response` and `vd:review`, and offers `vd:submit` only when every item has a draft. A completed task has only `self`. An action that does not appear as a link is refused if called (`409` or `403`).
- **The client does not build URLs.** Angular starts from `/api/v1`, follows the relations and fills in the `_templates`. A small, typed service of our own reads HAL-FORMS, because there is no official Angular library for it. Only `/api/v1` is hard-coded in the front end.
- **Errors** as Problem Details (RFC 9457, `application/problem+json`), with the error type and, for validation, the list of fields; `title` and `detail` in the request language (see *Internationalization*). The user-friendly message on screen comes from the error type.
- **Collections** paginated (`PagedModel`, with `next` and `prev`) when they can grow, such as batches, participants and dashboard items.
- **Exports**: `GET /api/v1/exports/{type}` with `Accept: text/csv` returns the file; in HAL, it returns the list of available files.

### Versioning in the path (`/api/v1`)

The version goes in the path, as the segment right after `/api`, using the native API versioning support of Spring Framework 7 (Spring Boot 4).

| Item | Rule |
|---|---|
| Format | `v` followed by an integer: `/api/v1`, `/api/v2`. Major version only; there is no `v1.1` |
| Configuration | `WebMvcConfigurer.configureApiVersioning`: `usePathSegment(1)`, with a predicate that treats only `/api/v{n}/**` paths as versioned; an `ApiVersionParser` that accepts the `v` prefix; supported versions declared explicitly (`addSupportedVersions("1")`) and automatic detection off. Controllers map `/api/{version}/...` with the `version` attribute |
| Nonexistent version | `/api/v9/...` returns `400` as Problem Details (`InvalidApiVersionException`), with the list of supported versions |
| Version index | `GET /api` (outside any version) returns, in HAL, one link per supported version (`vd:v1`, ...), marking the deprecated ones. It is not part of the contract of any version |
| Links | Always in the same version as the request: a response from `/api/v1` only points to `/api/v1/...` |
| What changes the version | Only an incompatible change: removing or renaming a field, relation or action; changing the meaning or the type of a field; making an input field required; changing the status code of an existing case |
| What does not change the version | Adding an optional field, relation, action or resource. The client ignores what it does not know |
| Coexistence | The previous version stays up while there are open tabs with the old front end (Angular and the API ship in the same container): at least 7 days and until there are no calls to it for 24 hours in the log. During this period, its responses carry the `Deprecation` (RFC 9745) and `Sunset` (RFC 8594) headers, through `StandardApiVersionDeprecationHandler`, and Angular, on seeing `Deprecation`, asks to reload the page |
| Code | Controllers, representation models and link assemblers live in one package per version (`api.v1`); the domain layer, the database and the rules have no version. A `v2` reuses the services and only replaces the web layer |
| Expectation | The whole data collection should run on `v1`; the version is a safeguard to fix the API during data collection without breaking whoever has the page open |

### Swagger documentation (OpenAPI 3.1)

The specification is generated from the code itself by springdoc-openapi, so there is no hand-written document that could become outdated.

| Item | Rule |
|---|---|
| Addresses | Swagger UI at `/swagger-ui.html`, with a version selector; one specification per API version (springdoc `GroupedOpenApi`, filtering `/api/v1/**`) at `/v3/api-docs/v1` (JSON) and `/v3/api-docs.yaml/v1`. The `/v3/` in these addresses is the version of the OpenAPI format used by springdoc, not the API version |
| Versions | Each specification carries the API version (`v1`) in `info.version` and, in `info.description`, the system version (the one in the footer); a deprecated version is marked as such, with the `Sunset` date |
| Access | Open in local staging. In production, only with an administrator session, through the same filter as the administration area; participants do not see it |
| General information | Title "Veredito API", study description, researcher contact, license |
| Groups (tags) | Entry point (`/api/v1`), Access (code and session), Participation (consent form, professional skill, screening), Tasks (blocks and responses), Administration (batches, exports, dashboard, participants) |
| Each operation | `summary` and `description` in English; parameters with description and example; body with schema and example; **all** possible responses (2xx, 4xx) with an example; error responses use the Problem Details schema |
| Hypermedia | The HAL schemas (`_links`, `_templates`) appear in the responses; the description of each operation lists the relations and actions the resource offers **in each state** (for example, `vd:submit` only with every item answered); `/api/v1/rels/{rel}` points to the corresponding operation in Swagger |
| Security | Declared security scheme (`HttpOnly` session cookie); administrative operations marked as such |
| Schemas | Every DTO with `@Schema` (description, example, required flag, maximum lengths equal to the validation ones: 5,000 and 40,000 characters, for example) |
| Quality | An integration test reads the specification of each supported version (`/v3/api-docs/v1`, ...) and fails if any operation lacks a `summary`, `description`, example or error response; CI keeps `openapi-v1.json` (one file per version) of each commit as an artifact |

**Critical operations in HTTP** (the transactional semantics are in the next section)

| Operation | Request | Response |
|---|---|---|
| Request a block | `POST /api/v1/tasks/reservations` (following `vd:next-task`) | `201` with the task's `Location`; `200` with the same task if the person already had a valid reservation; `204` if there is no block available to them |
| Save a draft | `PUT /api/v1/tasks/{id}/responses/{itemId}` | `200`; repeating the same response does not create another one (idempotent) |
| Submit a block | `POST /api/v1/tasks/{id}/submission` (`vd:submit`) | `200` with the completed task; resubmitting a block already completed by the person returns `200` without changing anything; an expired reservation or one held by someone else returns `409` |
| Import | `POST /api/v1/batches/previews` (CSV upload) and `POST /api/v1/batches` with the preview id (`vd:confirm`) | `201` for the preview and `201` for the batch |

## Concurrency handling

Each operation is a short transaction; the decision rules live in the domain layer, and the repository only loads and locks the rows.

1. **Request a block**: if the person has a reserved, unexpired task, return it. Otherwise, read in `queue_position` order the free or expired candidates, outside the blocks in which the person already has a role, with `SELECT ... FOR UPDATE SKIP LOCKED`, so that concurrent requests get different tasks without waiting. Reserve the first one, delete the drafts of an expired previous holder and write `expires_at = now + 20 min` (minutes from the configuration).
2. **Save a draft**: lock the task (`FOR UPDATE`), check the holder (or an expired reservation with no new holder), write with `INSERT ... ON DUPLICATE KEY UPDATE` on the key (`task_id`, `item_id`) and renew `expires_at`.
3. **Submit a block**: lock the task, check the holder and that there is a draft for every item, switch the responses to `final` and the task to `completed`, all in the same transaction. Resubmitting a block already completed by the person returns success without changing anything.
4. **Import**: one transaction per confirmation; the next queue position is read with a lock, so that two imports do not collide.
5. **Expiry**: computed on read; there is no scheduled job.

The 30-concurrent-participant test (spec 007, SC-001) runs as an integration test against MySQL in a container, with 30 threads.

## Tests

| Layer | Tool |
|---|---|
| Domain (plain Java) | JUnit, without Spring |
| Repositories, transactions, migrations | Spring Boot Test + Testcontainers (MySQL 9.7) |
| API | REST Assured against the application on a random port (`@SpringBootTest(webEnvironment = RANDOM_PORT)`), with MySQL and a test mail server in containers |
| API documentation | REST Assured test that reads the specification of each version (`/v3/api-docs/v1`, ...) and validates completeness (summary, description, examples, error responses in each operation) |
| Front end | Angular 22 default test runner |
| Internationalization | Translation completeness check in CI, back-end bundle key parity, localized Problem Details, one end-to-end smoke test per locale (see *Internationalization*) |

### API tests with REST Assured

| Item | Rule |
|---|---|
| Why real HTTP | The tests go through the same filters, session cookies, content negotiation and error handling as production; nothing is mocked in the web layer |
| Navigation | Each test starts at `GET /api/v1` and follows the links by relation name (for example, `vd:reserve`), extracting the `href` from the response; the only fixed URL is the version's entry point. This tests level 3: if a link disappears or changes, the test breaks |
| Versions | The tests live in one package per API version (`api.v1`), like the code. Dedicated tests check that: `GET /api` lists the supported versions; `/api/v9/...` returns `400` as Problem Details; all the links in a `v1` response stay under `/api/v1/...`; a version marked as deprecated returns `Deprecation` and `Sunset` |
| Shared specifications | A base class builds the `RequestSpecification` (port, base path `/api/v1`, `Accept: application/prs.hal-forms+json`, logging only on failure) and one per profile: visitor, participant with a session, administrator |
| Organization | One test class per area, following the Swagger groups (Entry point, Access, Participation, Tasks, Administration) |
| What each test checks | HTTP status, `Content-Type`, headers (`Location` on 201), body, and the `_links` and `_templates` present **and absent** in the resource's state |
| Forbidden actions | For each state, the test checks that the action does not appear and that a direct call to the address is refused (403, 404 or 409, as the case may be) |
| Errors | Error responses validated against the Problem Details JSON schema (`matchesJsonSchemaInClasspath`); the localization test sends `Accept-Language` `pt-BR`, `en-US` and `es-ES` |
| Session and e-mail | The access code is read from the test mail server, as the participant would do; the session cookie is reused by the profile's `RequestSpecification` |
| Data | Each test creates its own data by importing, through the administrator API, the Minitest CSV (`veredito_minitest.csv`) or one derived from it (`minitest_duplicates.csv`, `minitest_missing_kind_column.csv`, `minitest_long_text.csv`), with fictitious participants; the database is cleaned between classes |
| Concurrency | The 30-concurrent-participant test (spec 007) uses REST Assured in 30 threads, each with its own session |

## Limits that matter

| Limit | Value | Mitigation |
|---|---|---|
| `db-f1-micro` connections | 25 | Small pool and at most 4 Cloud Run instances; move to `db-g1-small` (about US$ 27 per month) if it runs short |
| `db-f1-micro` without SLA | - | Acceptable for a study; daily backup |
| Gmail e-mails | about 500 per day | 24 h session, resend limits, counter on the dashboard; fallback: a transactional provider |
| Cloud Run cold start | a few seconds on the first call after inactivity | *Startup CPU boost* and the Java AOT cache; minimum of 1 instance during the call for volunteers, if needed (cost to be measured in production, during the pilot) |

## Rejected alternatives

| Option | Why not |
|---|---|
| Apps Script + Google Sheets (2026-11-10 decision) | Low quotas (100 e-mails per day, 30 concurrent executions), a spreadsheet as database, no versioned migrations; the researcher did not feel safe |
| Free `e2-micro` VM with Docker Compose | Backup, updates and security fall on the researcher; 1 GB of RAM for the JVM and MySQL together |
| Firebase / Firestore | Not relational; the decision is to use MySQL |
| External e-mail provider (for example, Brevo) | Outside Google; kept as a fallback if Gmail has delivery problems |
| Runtime i18n libraries (Transloco, ngx-translate) | Not the official Angular solution (see *Internationalization*) |

## Risks and open points

- **Flyway and MySQL 9.7**: Flyway's list of certified versions goes up to 9.4. Newer versions usually only produce a warning. The tests with Testcontainers 9.7 confirm it; if it fails, go back to MySQL 8.4 LTS.
- **Shared-core machine with MySQL 9.7 on Cloud SQL**: confirm in the plan for spec 003. If there is none, use 8.4 on `db-f1-micro` or 9.7 on `db-g1-small`.
- **Public repository**: GitHub *secret scanning* and *push protection* enabled on 2026-11-11.
- **E-mail**: the Gmail app password ("Veredito SMTP") already exists and is stored outside the repository; it goes into Secret Manager in the plan for spec 003. Local staging does not need it (it uses Mailpit).
- **Pilot in production**: only with the Minitest items; the pilot colleagues must know that their responses will be discarded; at the end, the production database is recreated empty and the phase switches to `collection` before the call for volunteers.
- **CSV with the full use cases**: specified in [002-oracle-import-csv](../002-oracle-import-csv/spec.md).
- **Agreement between raters**: see the spec's *Assumptions* section. It affects the analysis, not the technology.
- **LGPD**: the data stays on Google Cloud, in the chosen region. This must be stated in the consent form (TCLE).
- **Consent form translations**: the en-US and es-ES texts must state that the pt-BR text approved by the research ethics committee is the reference and link to it; confirm with the committee whether the translations need their own approval.
