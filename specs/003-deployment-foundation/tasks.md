---

description: "Tasks of spec 003: deploy Veredito and prepare the database"
---

# Tasks: Deploy Veredito and prepare the database

**Input**: documents in `specs/003-deployment-foundation/` (plan.md, spec.md, research.md, data-model.md, contracts/, quickstart.md)

**Prerequisites**: plan.md and spec.md; Docker Desktop running; JDK 25 and the Google Cloud CLI installed on the researcher's machine (research, section 1)

**Tests**: included. Constitution Principle II and FR-013, FR-017 require automated tests (REST Assured, Testcontainers, ArchUnit, bundle parity, XLIFF completeness, Playwright).

**Organization**: tasks grouped by user story; each story can be tested on its own once the foundation is ready.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: can run in parallel (different files, no dependency on an incomplete task)
- **[Story]**: story of the spec (US1 to US6)
- Paths are relative to the root of the `Veredito` branch (the `Veredito` folder)
- `<pkg>` = `backend/src/main/java/io/github/diegoquirino/veredito`; `<tpkg>` = `backend/src/test/java/io/github/diegoquirino/veredito`
- Code, comments, identifiers and commit messages in English; user-facing texts in pt-BR, en-US and es-ES, following `specs/GLOSSARY.md`
- Every text file in UTF-8; after each phase, run `scripts/_check_utf8.py` (in `TRABALHO DE TESE/scripts/`) over the changed folders
- Commit at the end of each phase on the `Veredito` branch

---

## Phase 1: Setup

**Purpose**: empty projects with the versions of research section 1

- [X] T001 Create the root files: `.gitignore` (add `backend/target/`, `frontend/node_modules/`, `frontend/dist/`, `frontend/.angular/`, `frontend/src/app/build-info.ts`, `frontend/test-results/`, `frontend/playwright-report/`, `backend/src/main/resources/static/pt-BR/`, `backend/src/main/resources/static/en-US/`, `backend/src/main/resources/static/es-ES/`, `.env`), `.editorconfig` (`charset = utf-8`, `end_of_line = lf`, 4 spaces for Java, 2 for the rest) and `.gitattributes` (`* text=auto eol=lf`, `*.cmd text eol=crlf`)
- [X] T002 Create `backend/pom.xml`:
  - **Coordinates:** parent `spring-boot-starter-parent` 4.1.2; `io.github.diegoquirino:veredito:0.3.0`; `java.version` 25.
  - **Properties:** `flyway.version` 13.12.1, `rest-assured.version` 6.3.0, and `veredito.version` (default `${project.version}+dev`).
  - **Runtime dependencies:** the Boot starters for Web MVC, HATEOAS, JDBC, Flyway, Mail and Validation; `org.flywaydb:flyway-mysql`; `com.mysql:mysql-connector-j`; `org.springdoc:springdoc-openapi-starter-webmvc-ui:3.2.0`; `com.google.cloud.sql:mysql-socket-factory-connector-j-8:1.30.2` (runtime scope).
  - **Development:** `spring-boot-docker-compose` (optional).
  - **Test dependencies:** the Boot 4 test starters of each technology (`spring-boot-starter-webmvc-test`, `-hateoas-test`, `-jdbc-test`, `-flyway-test`), `spring-boot-testcontainers`, the Testcontainers MySQL and JUnit Jupiter modules, `io.rest-assured:rest-assured`, `io.rest-assured:json-schema-validator`, `com.tngtech.archunit:archunit-junit5:1.5.1`.
  - **Build:** `spring-boot-maven-plugin` with the `build-info` goal and the additional property `veredito.version`.

  Then add the Maven Wrapper with Maven 3.10.0 (`backend/mvnw`, `backend/mvnw.cmd`, `backend/.mvn/wrapper/`).
- [X] T003 Create `<pkg>/VereditoApplication.java` and one `package-info.java` with a one-line description in each of `<pkg>/domain`, `<pkg>/api`, `<pkg>/api/v1`, `<pkg>/web`, `<pkg>/persistence`, `<pkg>/config`
- [X] T004 [P] Create the Angular application in `frontend/`:
  - **Scaffold:** `npx @angular/cli@22.2.6 new frontend --routing --style=scss --ssr=false --skip-git`, then `ng add @angular/material@22.2.6` (Material 3 theme) and `ng add @angular/localize@22.2.6`.
  - **npm scripts in `frontend/package.json`:**
    - `prebuild`, `pretest` and `prestart` run `node scripts/write-build-info.mjs`;
    - `i18n:extract` = `ng extract-i18n --format=xlf2 --output-path src/locale`;
    - `i18n:check` = `node scripts/check-i18n.mjs`;
    - `e2e` = `playwright test -c e2e/playwright.config.ts`.
  - **Dev dependency:** `@playwright/test@1.65.0`.
- [X] T005 [P] Copy `../ISSTA_2027/src/inputs/oracle/veredito_minitest.csv` to `backend/src/test/resources/minitest/veredito_minitest.csv` byte for byte (same SHA-256), as required by constitution Principle IV
- [X] T006 [P] Create `.env.example` with the variable names of `specs/003-deployment-foundation/contracts/pipeline.md` (section "Environment variables"); harmless local defaults only, no secret values

---

## Phase 2: Foundational

**Purpose**: configuration, errors, locale, versioning, entry point and locale routing used by every story

**CRITICAL**: no story starts before this phase is complete

- [X] T007 Create `backend/src/main/resources/application.yaml`:
  - **Server:** `server.port: ${PORT:8080}`.
  - **Datasource:** `spring.datasource.url` from `DB_URL`, with the default `jdbc:mysql://localhost:3306/veredito`; `DB_USER` and `DB_PASSWORD` from the environment, with local defaults. Always append `connectionTimeZone=UTC&preserveInstants=true`.
  - **Pool:** `spring.datasource.hikari.maximum-pool-size: 5`.
  - **Messages:** `spring.messages.basename: messages`, `encoding: UTF-8`, `fallback-to-system-locale: false`.
  - **Errors:** `spring.mvc.problemdetails.enabled: true`.
  - **Flyway:** `spring.flyway.clean-disabled: true`.
  - **Docker Compose:** `spring.docker.compose.skip.in-tests: true`.
  - **Veredito:** `veredito.environment: development`.

  Profile files:
  - **`application-staging.yaml`:** `veredito.environment: staging`; Docker Compose support off; mail to `${SMTP_HOST:mailpit}:${SMTP_PORT:1025}`.
  - **`application-prod.yaml`:** `veredito.environment: production`; Docker Compose support off; mail `smtp.gmail.com:587` with STARTTLS and `SMTP_USER`/`SMTP_PASSWORD` from the environment; structured JSON console logging.
- [X] T008 Create `<pkg>/config/VereditoProperties.java` (`@ConfigurationProperties("veredito")`, field `environment` of enum `Environment { PRODUCTION, STAGING, DEVELOPMENT }` serialized in lowercase) and `<pkg>/config/SystemVersion.java` (reads `veredito.version` from `BuildProperties`, `0.3.0+dev` when absent)
- [X] T009 [P] Create `<pkg>/api/LocaleConfig.java`:
  - **Resolver:** `AcceptHeaderLocaleResolver` with supported locales `pt_BR`, `en_US`, `es_ES` and default `pt_BR`.
  - **Matching:** language-only, so `pt`, `en` and `es` resolve to their supported locale.
  - **Fallback:** `fr-FR`, `*` or a missing header give `pt_BR`.
  - **Response:** a filter that sets `Content-Language` to the resolved locale (`pt-BR`, `en-US` or `es-ES`).
- [X] T010 [P] Create `backend/src/main/resources/messages.properties` (en-US), `messages_pt_BR.properties` and `messages_es_ES.properties`, UTF-8, with `problem.<slug>.title` and `problem.<slug>.detail` for the eight slugs of `specs/003-deployment-foundation/contracts/problem-details.md` (`unsupported-api-version` detail with the placeholders for the requested version and the supported list, as in the contract example)
- [X] T011 Create `<pkg>/api/ProblemType.java` (enum: slug, HTTP status; type URI `urn:veredito:problem:<slug>`) and `<pkg>/api/ProblemDetailsAdvice.java` (`@RestControllerAdvice` extending `ResponseEntityExceptionHandler`).

  **Exception mapping:**
  - `NoResourceFoundException` -> `not-found`;
  - `HttpRequestMethodNotSupportedException` -> `method-not-allowed`;
  - `HttpMediaTypeNotAcceptableException` -> `not-acceptable`;
  - `InvalidApiVersionException` -> `unsupported-api-version`, with the extension `supportedVersions: ["v1"]`;
  - `MethodArgumentNotValidException` and `HttpMessageNotReadableException` -> `invalid-request`, with `errors: [{field, code}]`;
  - `CannotGetJdbcConnectionException` and `DataAccessResourceFailureException` -> `service-unavailable`;
  - any other `Exception` -> `internal-error`, with an `incidentId` UUID, logged at ERROR with the stack trace and the same id.

  **Every response:**
  - `title` and `detail` come from `MessageSource` in the request locale;
  - `instance` is the request path;
  - `Content-Type` is `application/problem+json`.
- [X] T012 Create `<pkg>/api/ApiVersioningConfig.java` (`WebMvcConfigurer.configureApiVersioning`):
  - **Where:** version in path segment 1, applied only to `/api/v{n}/**`.
  - **Parsing:** a parser that accepts the `v` prefix; supported version `1` only; detection of other versions off.
  - **Elsewhere:** version not required outside `/api/v{n}/**`.
  - **Deprecation:** `StandardApiVersionDeprecationHandler` wired, with no deprecated version in 003.
- [X] T013 [P] Create `<pkg>/api/HypermediaConfig.java`: HAL and HAL-FORMS enabled, HAL-FORMS as the default for `*/*`, CURIE `vd` -> `/api/v1/rels/{rel}`
- [X] T014 Create `<pkg>/persistence/DatabaseStatus.java`: `boolean isAvailable()` with `JdbcClient` running `SELECT 1` with a 2-second query timeout; returns `false` on any `DataAccessException`, without logging a stack trace more than once per minute
- [X] T015 Create `<pkg>/api/v1/EntryPointController.java` and `<pkg>/api/v1/EntryPointModel.java`, per `specs/003-deployment-foundation/contracts/api.md`:
  - **Route:** `GET /api/{version}/` mapped with `version = "1"`.
  - **Properties:** `systemVersion`, `environment`, and `available` from `DatabaseStatus`.
  - **Links when available:** `self`, `curies` and `vd:request-code` -> `/api/v1/access-codes`.
  - **Links when unavailable:** only `self` (HAL lists `curies` only next to a curied relation).
- [X] T016 Create `<pkg>/web/LocaleRoutingFilter.java` (servlet filter before Spring MVC) implementing rules 1 to 10 of `specs/003-deployment-foundation/contracts/web-routing.md`:
  - `/api` and the documentation paths pass through;
  - `/` serves `static/index.html`;
  - a prefix without the trailing slash gets `301` to the same prefix with `/`;
  - existing files under a prefix are served;
  - a missing path with an extension gets a plain `404`;
  - any other path under a prefix is forwarded to `/<locale>/index.html`;
  - a prefix in another case gets `301` to the canonical case, keeping path and query;
  - anything else gets `302` to `/`;
  - methods other than GET and HEAD outside `/api` get `405`.

  Also create `<pkg>/web/StaticCacheConfig.java` with the `Cache-Control` values of the contract: `no-cache` for `index.html` and the root page, and `public, max-age=31536000, immutable` for hashed bundles.
- [X] T017 Create the test support in `<tpkg>/support/`:
  - **`MySqlContainerConfig.java`:** `@TestConfiguration` with `@ServiceConnection` on `MySQLContainer("mysql:9.7")`, reused across classes.
  - **`ApiTestBase.java`:** `@SpringBootTest(webEnvironment = RANDOM_PORT)`, plus a REST Assured base specification (port, `Accept: application/prs.hal-forms+json`, logging only on failure).
  - **`ApiTestBase` helpers:** `entryPoint()`, and `follow(response, rel)`, which reads the `href` of a relation and expands the `vd` CURIE.

  Also create `backend/src/test/resources/schemas/problem-details.json` (JSON Schema 2020-12 of the contract fields) and stubs `backend/src/test/resources/static/{pt-BR,en-US,es-ES}/index.html`, each with `<html lang="...">` and one asset `main-TEST.js`.
- [X] T018 [P] Create `<tpkg>/domain/DomainIsolationTest.java` (ArchUnit): classes in `..veredito.domain..` must not depend on `org.springframework..`, `java.sql..`, `javax.sql..`, `jakarta.servlet..` or `..veredito.api..`, `..veredito.web..`, `..veredito.persistence..`, `..veredito.config..`; `allowEmptyShould(true)` while the package has no classes
- [X] T019 [P] Create `<tpkg>/i18n/MessageBundleParityTest.java`: loads the three bundles as UTF-8 `Properties` and fails unless they have exactly the same keys and no empty value, listing each missing key and the file where it is missing (FR-017)
- [X] T020 Create `<tpkg>/web/WebRoutingTest.java` (REST Assured, extends `ApiTestBase`), one test per row of the rules table and the headers table of `contracts/web-routing.md`:
  - `/` gives `200`;
  - `/en-US` gives `301` to `/en-US/`;
  - `/es-ES/admin` and `/pt-BR/tasks/12` give `200`, with `lang="es-ES"` and `lang="pt-BR"`;
  - `/en-US/main-TEST.js` gives `200` with the immutable cache header;
  - `/en-US/missing.js` gives `404`;
  - `/en-us/admin?x=1` gives `301` to `/en-US/admin?x=1`;
  - `/fr-FR/`, `/admin` and `/xyz` give `302` to `/`;
  - `POST /pt-BR/` gives `405`;
  - `/api/v1` is not affected by the filter;
  - `index.html` responses carry `Cache-Control: no-cache`.
- [X] T021 Configure i18n in `frontend/angular.json`:
  - **Locales:** `i18n.sourceLocale` `{ "code": "en-US", "subPath": "en-US" }`; `locales` `pt-BR` and `es-ES`, each with `translation: src/locale/messages.<locale>.xlf` and `subPath` equal to the locale.
  - **Build options:** `localize: true`, `i18nMissingTranslation: "error"`.
  - **Serve:** configurations `pt-BR` and `es-ES` for `ng serve`.
  - **Proxy:** `frontend/proxy.conf.json` forwards `/api` to `http://localhost:8080`.
- [X] T022 [P] Create `frontend/scripts/write-build-info.mjs`: writes `frontend/src/app/build-info.ts` with `export const BUILD_VERSION = '<value>';` from the environment variable `VEREDITO_VERSION` (default `0.3.0+dev`)
- [X] T023 Create the front-end core in `frontend/src/app/core/`:
  - **`accept-language.interceptor.ts`:** sends `Accept-Language` equal to `LOCALE_ID` on every `/api` call.
  - **`hal.ts`:** typed link reader for `_links`, with CURIE expansion.
  - **`entry-point.service.ts`:** `GET /api/v1`, returning `{ systemVersion, environment, available, links }`. On an HTTP or network error it returns `available: false` and `environment: 'production'`, which hides the banner.

  Register them in `frontend/src/app/app.config.ts`, together with `MAT_DATE_LOCALE` from `LOCALE_ID`.

**Checkpoint**: `cd backend; .\mvnw.cmd -B verify` passes `DomainIsolationTest`, `MessageBundleParityTest` and `WebRoutingTest`; `cd frontend; npm run build` produces `dist/frontend/browser/{pt-BR,en-US,es-ES}/`

---

## Phase 3: User Story 1 - Open Veredito at a fixed URL (Priority: P1) MVP

**Goal**: the language-choice page at `/` and, under each prefix, the home page with "Veredito", a sentence about the study, the "Participate" button and the version in the footer, responsive

**Independent Test**: with the application running locally (Compose `mysql` + `./mvnw spring-boot:run` with the front-end builds copied into `static/`), open `/` in a private window and each prefix on desktop and phone sizes (quickstart, section 3)

### Tests for User Story 1

- [X] T024 [P] [US1] Component tests (Vitest) in `frontend/src/app/shell/shell.component.spec.ts` (header shows "Veredito"; footer shows `BUILD_VERSION`; footer has links to the other two locales for the current route) and `frontend/src/app/home/home.component.spec.ts` (sentence about the study; "Participate" button navigates to `/participate`)
- [X] T025 [P] [US1] Playwright tests in `frontend/e2e/home.spec.ts`, with `frontend/e2e/playwright.config.ts` (`baseURL` from `E2E_BASE_URL`, default `http://localhost:8080`; Chromium; projects `desktop` and `phone` with a 360-pixel-wide viewport).

  **Root page:** `/` shows "Português (Brasil)", "English (US)" and "Español (España)", each linking to its prefix.

  **Each prefix:**
  - `html[lang]` equals the locale;
  - the header shows "Veredito";
  - the button reads "Participar", "Participate" or "Participar";
  - the footer version equals the `systemVersion` of `GET /api/v1` (US1 scenario 3).

  **Phone project:** `document.documentElement.scrollWidth <= clientWidth` (US1 scenario 2).

### Implementation for User Story 1

- [X] T026 [P] [US1] Create the root page `backend/src/main/resources/static/index.html` and `backend/src/main/resources/static/language.css` (plain HTML and CSS, title "Veredito", the three options of `contracts/web-routing.md` section "Root page", each in its own language and with its own `lang` attribute, responsive)
- [X] T027 [US1] Create the shell in `frontend/src/app/shell/`:
  - **Header:** `mat-toolbar` with "Veredito".
  - **Main area:** `router-outlet`.
  - **Footer:** the version from `BUILD_VERSION`, and plain `<a href>` links to `/<other locale>/<current route>`, so a click reloads the page under the other prefix. These links are the provisional switch of `contracts/web-routing.md`; spec 004 replaces them.
- [X] T028 [P] [US1] Create `frontend/src/app/home/` (one sentence about the study and the "Participate" `mat-flat-button` to `/participate`), `frontend/src/app/participate/` (provisional page saying that sign-in arrives soon) and `frontend/src/app/not-found/`; routes in `frontend/src/app/app.routes.ts`: `''` home, `'participate'`, `'**'` not found
- [X] T029 [US1] Layout and theme in `frontend/src/styles.scss` and the components: Material 3 theme, fluid container with `max-width`, no fixed widths, readable at 360 px
- [X] T030 [US1] Mark every user-facing text of T027 and T028 with `i18n` / `$localize` and explicit ids (`@@shell.title`, `@@home.intro`, `@@home.participate`, `@@participate.soon`, `@@notFound.title`, ...), run `npm run i18n:extract` and create `frontend/src/locale/messages.pt-BR.xlf` and `frontend/src/locale/messages.es-ES.xlf` with a `<target>` for every unit, using the GLOSSARY vocabulary ("Participar" / "Participate" / "Participar")

**Checkpoint**: US1 works locally; T024 and T025 pass

---

## Phase 4: User Story 3 - Prepare the database (Priority: P1)

**Goal**: Flyway creates every table of the data model with its constraints and the default configuration, safely repeatable; the home page reports a database outage

**Independent Test**: `MigrationTest` and `DatabaseUnavailableTest`, plus quickstart section 5

### Tests for User Story 3

- [X] T031 [P] [US3] Create `<tpkg>/persistence/MigrationTest.java` (Testcontainers `mysql:9.7`).

  **Migration and repetition:**
  1. On an empty database: the 11 tables of `data-model.md` exist (`import_batch`, `block`, `item`, `task`, `response`, `participant`, `access_code`, `access_session`, `consent_form`, `consent_form_text`, `config`), plus `flyway_schema_history` with V1 and V2 successful (US3 scenario 1).
  2. `config` has exactly one row with `admin_email = 'diegoquirino@gmail.com'`, `admin_preferred_language IS NULL`, `reservation_minutes = 20`, `block_size = 10`, `daily_email_limit = 450`, `study_phase = 'pilot'`; `consent_form` is empty (US3 scenario 2).
  3. Migrating again adds no history row and leaves a previously inserted row unchanged (US3 scenario 3).
  4. Two fresh containers give identical `information_schema` columns, indexes and constraints (SC-003).
  5. `participant.preferred_language` and `response.language` exist, and `consent_form_text` accepts one row per language (US3 scenario 6).

  **Constraints refused:**
  - `language = 'fr-FR'` in `response`, `participant`, `consent_form_text` and `config`;
  - `study_phase = 'other'`;
  - a second `config` row (`id = 2`);
  - a duplicate (`system`, `edit_key`);
  - the same participant in both tasks of a block;
  - a `free` task with a `participant_id`;
  - `item` with both `origin` and `target` `NULL`;
  - `participant` with `anonymized_at` set and `email` not `NULL`;
  - `response.change_impact = 'MEDIUM'`;
  - `response.maintenance_operation = 'DELETE'`.
- [X] T032 [P] [US3] Create `<tpkg>/api/v1/DatabaseUnavailableTest.java`: application started with `spring.datasource.url` pointing to a closed port and Flyway off; `GET /api/v1` returns `200` with `available: false` and no `vd:request-code` (edge case "Database deleted or unreachable")
- [X] T033 [P] [US3] Component test in `frontend/src/app/shell/unavailable.spec.ts`: when the entry point service returns `available: false`, the shell shows the localized "System temporarily unavailable" message instead of the page content

### Implementation for User Story 3

- [X] T034 [US3] Write `backend/src/main/resources/db/migration/V1__create_schema.sql` exactly as `specs/003-deployment-foundation/data-model.md`:
  - **Defaults:** InnoDB, `utf8mb4`, default collation `utf8mb4_0900_ai_ci`.
  - **Binary collation:** `utf8mb4_0900_bin` on `item.system`, `item.edit_key`, `participant.email`, `access_code.email`, `access_code.code_hash`, `access_session.token_hash` and `config.admin_email`.
  - **Instants:** all `DATETIME(6)`.
  - **Foreign keys:** no referential actions.
  - **`item`:** `system VARCHAR(100)`, `edit_key VARCHAR(500)`, `uc VARCHAR(255)`, `field VARCHAR(100)`; `origin` and `target` `TEXT`; the use cases `MEDIUMTEXT`; versions `VARCHAR(20)`. **Unique** (`system`, `edit_key`) and (`block_id`, `position`); `CHECK (origin IS NOT NULL OR target IS NOT NULL)`; `kind` in `ADD`, `DEL`, `MOD`, `META`.
  - **`block`:** **unique** `number`; `size >= 1`.
  - **`task`:**
    - `role` in (1, 2); **unique** `queue_position`, (`block_id`, `role`) and (`block_id`, `participant_id`).
    - `status` in `free`, `reserved`, `completed`, default `free`.
    - `free`: participant, `reserved_at`, `expires_at` and `submitted_at` all `NULL`.
    - `reserved`: participant, `reserved_at` and `expires_at` set; `submitted_at` `NULL`.
    - `completed`: participant, `reserved_at` and `submitted_at` set.
  - **`response`:** `state` in `draft`, `final`; `change_impact` in `LOW`, `HIGH`; `maintenance_operation` in `KEEP`, `UPDATE`, `REMOVE`, `CREATE`, `MERGE`, `SPLIT`, `REASSIGN`, `FLAG`; `comment VARCHAR(1000)`; `language CHAR(5)` locale; **unique** (`task_id`, `item_id`).
  - **`participant`:**
    - `pseudonym VARCHAR(16)` **unique**; `name VARCHAR(120)`; `email VARCHAR(254)` **unique**.
    - The four consent columns are all `NULL` or all set; `consent_decision` in `accepted`, `refused`.
    - `professional_skill` in the 13 codes of Appendix B.4 of spec 001; `professional_skill_other VARCHAR(100)`, only with `other`.
    - `screening_answer` in `yes_professional`, `yes_academic`, `no`.
    - `(anonymized_at IS NULL) = (name IS NOT NULL AND email IS NOT NULL)`.
  - **`access_code`:** index (`email`, `created_at`); `attempts >= 0`, default 0.
  - **`access_session`:** `subject` in `participant`, `administrator`; `participant_id` set if and only if the subject is `participant`.
  - **`consent_form`:** `version VARCHAR(20)` **unique**; `reference_language` must be `pt-BR`.
  - **`consent_form_text`:** **unique** (`consent_form_id`, `language`); `text MEDIUMTEXT`.
  - **`config`:**
    - `id TINYINT` with `CHECK (id = 1)`;
    - `reservation_minutes` 1 to 240; `block_size` 1 to 50; `daily_email_limit` 0 to 2,000;
    - `study_phase` in `pilot`, `collection`;
    - `admin_preferred_language` a locale or `NULL`.
  - **Every locale column:** `CHECK (... IN ('pt-BR', 'en-US', 'es-ES'))`.
- [X] T035 [US3] Write `backend/src/main/resources/db/migration/V2__insert_default_config.sql`: one `config` row with `id = 1`, `admin_email = 'diegoquirino@gmail.com'`, `admin_preferred_language = NULL`, `reservation_minutes = 20`, `block_size = 10`, `daily_email_limit = 450`, `study_phase = 'pilot'`, `study_phase_changed_at = NULL`, `updated_at = UTC_TIMESTAMP(6)`; no consent form
- [X] T036 [US3] Create `frontend/src/app/unavailable/` and wire it in the shell: when `EntryPointService` reports `available: false`, show "System temporarily unavailable" (pt-BR "Sistema temporariamente indisponível", es-ES "Sistema temporalmente no disponible", per GLOSSARY) with a retry button; add the units to both XLIFF files

**Checkpoint**: T031 to T033 pass; V1 and V2 are final and must never be edited after the first production deployment (Principle III)

---

## Phase 5: User Story 6 - Serve the interface in three languages (Priority: P1)

**Goal**: completeness checks for interface texts and backend bundles, localized Problem Details, deep links and redirects per locale, translated display labels of stored codes

**Independent Test**: quickstart section 1 (including the negative checks) and section 3; `LocalizedProblemDetailsTest`; `e2e/locales.spec.ts`

### Tests for User Story 6

- [ ] T037 [P] [US6] Create `<tpkg>/api/LocalizedProblemDetailsTest.java` (REST Assured): `GET /api/v9/tasks` with `Accept-Language` `pt-BR`, `en-US`, `es-ES`, `es`, `fr-FR` and without the header; checks `400`, `application/problem+json`, the same `type` `urn:veredito:problem:unsupported-api-version`, `supportedVersions: ["v1"]`, `title` and `detail` equal to the bundle texts of the expected locale (pt-BR for `fr-FR` and no header), `Content-Language`, and validation against `schemas/problem-details.json` (US6 scenario 5, FR-016)
- [ ] T038 [P] [US6] Create `frontend/e2e/locales.spec.ts`:
  - reload `/es-ES/participate`: still Spanish (`html[lang="es-ES"]`) on the participate page (US6 scenario 2);
  - `/en-us/` ends at `/en-US/`;
  - `/fr-FR/` and `/xyz` end at `/`;
  - clicking the footer link to another locale loads the same route under the other prefix, with a full page load (US6 scenarios 3 and 4).
- [ ] T039 [P] [US6] Unit test `frontend/src/app/core/labels/study-phase-labels.spec.ts`: the label function returns the localized label for `pilot` and `collection` and leaves the code itself unchanged; an unknown code is shown as is (US6 scenario 8)

### Implementation for User Story 6

- [ ] T040 [US6] Create `frontend/scripts/check-i18n.mjs` (Node only, no dependency):
  1. Runs `ng extract-i18n --format=xlf2` to a temporary folder.
  2. Reads every `<unit id>` of the extracted source.
  3. Fails with exit code 1 when `src/locale/messages.pt-BR.xlf` or `messages.es-ES.xlf` lacks a unit or has an empty `<target>`, printing for each problem the unit id, the source text and the locale.
  4. Warns about obsolete units present only in a translation file.

  Covers FR-017 and SC-008.
- [ ] T041 [US6] Create `frontend/src/app/core/labels/study-phase-labels.ts` with `$localize` labels for `pilot` and `collection` (pt-BR "piloto", "coleta"; es-ES "piloto", "recolección"), the pattern later specs follow for `change_impact`, `maintenance_operation`, `professional_skill` and screening codes; add the units to both XLIFF files
- [ ] T042 [US6] Set `<html lang>` per build (Angular sets it from the locale; confirm in `dist/frontend/browser/<locale>/index.html`) and make the chunk-load error handler in `frontend/src/app/core/` (router `withNavigationErrorHandler`) show the localized "A new version is available; reload the page" message with a reload action (edge case "Page open for hours before a deployment"); add the units to both XLIFF files

**Checkpoint**: T037 to T039 pass; `npm run i18n:check` passes; removing one target or one bundle key makes the checks fail (quickstart section 1, negative checks 1 and 2)

---

## Phase 6: User Story 4 - Rehearse locally before deploying (Priority: P2)

**Goal**: one command starts local staging with the production `Dockerfile`, MySQL 9.7 and Mailpit, with the staging banner

**Independent Test**: quickstart section 2, plus the staging rows of sections 3 to 5

**Order note**: this P2 story comes before US2 because the constitution requires every change to pass local staging before production

### Tests for User Story 4

- [ ] T043 [P] [US4] Component test in `frontend/src/app/shell/staging-banner.spec.ts`: the banner is shown for `environment` `staging` and `development` and hidden for `production`
- [ ] T044 [P] [US4] Playwright test in `frontend/e2e/staging.spec.ts`: under each prefix, the banner reads "AMBIENTE DE ENSAIO", "STAGING ENVIRONMENT" or "ENTORNO DE PRUEBAS" (US4 scenario 2), and `GET /api/v1` reports `environment: "staging"`

### Implementation for User Story 4

- [ ] T045 [US4] Create the staging banner in `frontend/src/app/shell/staging-banner.component.ts` (shown when `environment !== 'production'`), with the GLOSSARY texts in both XLIFF files
- [ ] T046 [US4] Write the `Dockerfile` per `contracts/pipeline.md`:
  1. **Front-end stage** (`node:26-bookworm-slim`): `npm ci`, then `npm run build` with `VEREDITO_VERSION`.
  2. **Back-end stage** (`eclipse-temurin:25-jdk`): copy `dist/frontend/browser/{pt-BR,en-US,es-ES}` into `backend/src/main/resources/static/`, then `./mvnw -B -DskipTests -Dveredito.version=$VEREDITO_VERSION package`.
  3. **Runtime stage** (`eclipse-temurin:25-jre`): non-root user, `-XX:MaxRAMPercentage=75`, exposes 8080, runs the jar.

  The build argument `VEREDITO_VERSION` defaults to `0.3.0+dev`. Add a `.dockerignore` excluding `node_modules`, `target`, `dist`, `.angular`, `.git`, `.env` and `specs`.
- [ ] T047 [US4] Write `compose.yaml`:
  - **`mysql`:** `mysql:9.7`; database `veredito`, user `veredito`, password `${DB_PASSWORD:-veredito-local}`, root password random; volume `mysql-data`; healthcheck; port 3306.
  - **`mailpit`:** `axllent/mailpit:v1.33.1`; ports 8025 and 1025.
  - **`app`:** profile `staging`; `image: ${VEREDITO_IMAGE:-veredito-app:local}`; `build` from `.` with argument `VEREDITO_VERSION`; `SPRING_PROFILES_ACTIVE=staging`; `DB_URL=jdbc:mysql://mysql:3306/veredito`; depends on a healthy `mysql`; port 8080.
- [ ] T048 [US4] Run quickstart section 2 on the researcher's machine: `docker compose --profile staging down -v`, then `up --build` timed (less than 10 minutes, SC-001), app at `http://localhost:8080`, inbox at `http://localhost:8025`; then `down -v` and `up` again give an empty, prepared database (US4 scenario 4); note the times in this task

**Checkpoint**: local staging runs with one command; T043 and T044 pass against it

---

## Phase 7: User Story 5 - Consult the documented API (Priority: P2)

**Goal**: version index, relation documentation, Swagger with complete operations, open in staging and refused in production

**Independent Test**: quickstart section 4; the REST Assured tests below; negative check 3 of quickstart section 1

### Tests for User Story 5

- [ ] T049 [P] [US5] Create `<tpkg>/api/ApiIndexTest.java`:
  - `GET /api` lists `v1` (`deprecated: false`) with the link `vd:v1` to `/api/v1`;
  - `GET /api/v9` returns `400` `unsupported-api-version` (US5 scenario 5);
  - every `href` in a `/api/v1` response starts with `/api/v1`.
- [ ] T050 [P] [US5] Create `<tpkg>/api/v1/EntryPointTest.java`:
  - **Visitor links:** the entry point has exactly `self`, `curies` and `vd:request-code`, and none of `vd:me`, `vd:batches`, `vd:exports`, `vd:dashboard` or `vd:participants` (US5 scenario 2).
  - **Properties:** `systemVersion` and `environment` are present.
  - **Relation documentation:** following the `vd` CURIE for `request-code` returns `200` with `rel`, `description`, `method` and `documentation`; an unknown relation returns `404` `not-found`.
  - **Operation not yet delivered:** following `vd:request-code` with `POST` returns `404` Problem Details until spec 004.
  - **Errors:** `GET /api/v1/unknown` returns `404`; `DELETE /api/v1` returns `405` with `Allow`; `Accept: text/csv` returns `406`.
- [ ] T051 [P] [US5] Create `<tpkg>/api/ApiDocumentationCompletenessTest.java`:
  - **Coverage:** reads `/v3/api-docs/v1` and `/v3/api-docs/index`.
  - **Each operation must have:** a non-empty `summary` and `description`; an example in every success response; at least one 4xx response whose content `application/problem+json` references the Problem Details schema.
  - **On failure:** names `METHOD path` and what is missing (US5 scenario 4, SC-007).
  - **Output:** writes `backend/target/openapi/openapi-v1.json` and `openapi-index.json`.
- [ ] T052 [P] [US5] Create `<tpkg>/web/ApiDocsAccessTest.java`: with profile `prod` (datasource from the test container), `/swagger-ui.html`, `/v3/api-docs/v1` and `/v3/api-docs/index` return `401` Problem Details `authentication-required` (US5 scenario 3); with profile `staging`, all three return `200`

### Implementation for User Story 5

- [ ] T053 [US5] Create `<pkg>/api/ApiIndexController.java` (`GET /api`, outside versioning, per `contracts/api.md`)
- [ ] T054 [US5] Create `<pkg>/api/v1/LinkRelations.java` (registry of `vd:` relations: name, English description, method, Swagger anchor; `request-code` in 003) and `<pkg>/api/v1/RelationController.java` (`GET /api/v1/rels/{rel}`)
- [ ] T055 [US5] Create `<pkg>/config/OpenApiConfig.java`:
  - **Groups:** `GroupedOpenApi` `v1` (`/api/v1/**`) and `index` (`/api`).
  - **`info`:** title "Veredito API"; `info.version` equal to the API version; `info.description` with the study and the `systemVersion`; contact and license.
  - **Tags:** Entry point, Access, Participation, Tasks, Administration.
  - **Components:** a reusable `ProblemDetails` schema, and the declared session cookie security scheme (used from 004).
- [ ] T056 [US5] Document every operation of `EntryPointController`, `ApiIndexController` and `RelationController` with `@Operation` (summary, description listing the relations offered in each state), `@ApiResponse` for each success and error status with examples, and `@Schema` on the models, until T051 passes
- [ ] T057 [US5] Create `<pkg>/web/ApiDocsAccessFilter.java`, active only in profile `prod`. It refuses `/swagger-ui.html`, `/swagger-ui/**` and `/v3/api-docs/**` with a `401` `authentication-required` Problem Details unless `AdministratorSessionCheck.isAdministrator(request)` is true. Also create the interface `<pkg>/web/AdministratorSessionCheck.java` and a 003 implementation that always returns `false`; spec 004 replaces it.

**Checkpoint**: T049 to T052 pass; Swagger UI in local staging lists `v1` and `index` with every operation complete

---

## Phase 8: User Story 2 - Update the system without changing the URL (Priority: P1)

**Goal**: CI on every push, production deployment by tag of a rehearsed commit, same URL, rollback, no data change

**Independent Test**: quickstart sections 6 to 8

**Order note**: depends on US4 (rehearsal in local staging) and on the researcher's one-time cloud setup

- [ ] T058 [US2] If Spring Boot 4.2.0 GA is out (due 2026-11-19), raise the parent in `backend/pom.xml` to the latest 4.2.x and springdoc to the matching 3.x, run `.\mvnw.cmd -B verify`, and record the versions in `specs/003-deployment-foundation/research.md` section 1; otherwise keep 4.1.2 and note the date of the check here
- [ ] T059 [US2] Write `.github/workflows/ci.yml` per `contracts/pipeline.md`:
  - **Trigger:** push to `Veredito`.
  - **Jobs:**
    - `backend`: Temurin 25, `./mvnw -B verify`, upload `backend/target/openapi/*.json`.
    - `frontend`: Node 26, `npm ci`, `npm run i18n:check`, `npm test`, `npm run build`.
    - `secrets`: gitleaks, image pinned, with `fetch-depth: 0`.
    - `image`: needs the three above; `VEREDITO_VERSION=<pom version>+<short sha>`; authenticates with `google-github-actions/auth` through Workload Identity Federation; builds and pushes `southamerica-east1-docker.pkg.dev/veredito-issta2027/veredito/app:<sha>`.
    - `e2e`: needs `image`; runs `VEREDITO_IMAGE=<that image> docker compose --profile staging up -d --no-build`, then the Playwright tests (Chromium only).
  - **Variables:** only non-secret repository variables.
  - **Permissions:** `id-token: write` only where needed.
- [ ] T060 [US2] Write `.github/workflows/deploy.yml`:
  - **Trigger:** push of tags `deploy/*`.
  - **Steps:**
    1. Resolve the tagged commit.
    2. Fail if the `ci.yml` run for that commit did not succeed (`gh run list --commit`), or if the image `app:<sha>` is missing.
    3. Authenticate as `veredito-deploy`.
    4. Run `gcloud run deploy veredito` with these flags: `--image <image>@<digest>`, `--region southamerica-east1`, `--service-account veredito-run`, `--allow-unauthenticated`, `--cpu 1`, `--memory 1Gi`, `--min-instances 0`, `--max-instances 4`, `--cpu-boost`.
    5. Pass the environment variables of `contracts/pipeline.md`, with `SPRING_PROFILES_ACTIVE=prod`, and the secrets through `--set-secrets` (`DB_PASSWORD=veredito-db-password:latest`, `SMTP_PASSWORD=veredito-smtp-password:latest`).
    6. Print the service URL and the new revision.
- [ ] T061 [P] [US2] Write `docs/cloud-setup.md`: the `gcloud` commands of research section 19 in order.

  **Resources:**
  - enable the APIs;
  - Artifact Registry repository `veredito` with a cleanup policy keeping 5 images;
  - Cloud SQL `MYSQL_9_7` with `--edition=ENTERPRISE --tier=db-f1-micro --region=southamerica-east1`, no authorized networks, `--connector-enforcement=REQUIRED`, daily backups with 7-day retention;
  - database `veredito` and user `veredito_app`;
  - the secrets `veredito-db-password` and `veredito-smtp-password`, with values entered interactively, never in the file;
  - the three service accounts and their roles;
  - the Workload Identity pool `github` and its provider, with the attribute condition limited to `diegoquirino/openscience` and the refs `refs/heads/Veredito` and `refs/tags/deploy/*`;
  - the GitHub repository variables.

  **Done by hand (billing):** the project, the billing link and the budget "Veredito mensal" (R$ 200, alerts at 50/90/100 %, no action).
- [ ] T062 [US2] Researcher, by hand: create the project `veredito-issta2027`, link billing, create the budget "Veredito mensal" (SC-005), run `gcloud auth login`, and enter the secret values when prompted by `docs/cloud-setup.md`; the agent never sees or prints them
- [ ] T063 [US2] Run the remaining commands of `docs/cloud-setup.md` with the researcher's authenticated `gcloud`, set the GitHub repository variables, push to `Veredito` and confirm that all `ci.yml` jobs pass and the image `app:<sha>` exists in Artifact Registry
- [ ] T064 [US2] First production deployment with `git push origin <commit>:refs/tags/deploy/0.3.0-1`, for a commit rehearsed in local staging.

  **Check (quickstart section 7):**
  - the workflow finishes in less than 15 minutes (SC-001);
  - the footer shows the same `systemVersion` as staging (US4 scenario 3);
  - `/api/v1` reports `environment: "production"` and `available: true`, with no banner;
  - Swagger returns `401`;
  - a direct `mysql -h <public IP>` is refused (US3 scenario 5);
  - the warm home page loads in less than 3 seconds (SC-004).

  Record the URL and the results in this task.
- [ ] T065 [US2] Three deployments in a row and one rollback.

  **Deployments:** with the first deployment's `config.updated_at` noted, deploy two more commits (`deploy/0.3.0-2`, `deploy/0.3.0-3`). The URL does not change, the footer follows the commit, `flyway_schema_history` has no new rows, and the `config` row is unchanged (US2 scenarios 1 and 2, SC-002).

  **Rollback:** `gcloud run services update-traffic veredito --region=southamerica-east1 --to-revisions=<previous>=100` keeps the URL and shows the previous version (US2 scenario 3). Record the results in this task.

**Checkpoint**: production at a fixed URL; deployment and rollback proven

---

## Phase 9: Polish & Cross-Cutting Concerns

- [ ] T066 Run quickstart section 1 completely, including the three negative checks (one translation target, one bundle key, one operation description removed and restored), and record that each check failed as expected (SC-008, SC-007)
- [ ] T067 [P] Run `scripts/_check_utf8.py` over `Veredito/backend/src`, `Veredito/frontend/src`, `Veredito/specs`, `Veredito/docs` and the root files; no file fails
- [ ] T068 [P] Confirm SC-006: the `secrets` job is green and GitHub secret scanning has no alert; `git grep -n -i -E "password|secret|api[_-]?key"` shows only variable names and documentation, no values
- [ ] T069 Mark spec 003 as complete: `Status` in `specs/003-deployment-foundation/spec.md` and the line of 003 in `specs/ROADMAP.md`, listing any criterion left to a later spec (US4 scenario 2 for e-mails is checked in spec 004, SC-005 there); commit and push to `Veredito`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: no dependencies
- **Foundational (Phase 2)**: depends on Setup; blocks every story
- **US1 (Phase 3)**: depends on Phase 2
- **US3 (Phase 4)**: depends on Phase 2; T036 uses the shell of US1 (T027)
- **US6 (Phase 5)**: depends on Phase 2; T038 and T042 use the shell and routes of US1
- **US4 (Phase 6)**: depends on US1, US3 and US6 (the image must contain the shell, the migrations and the translations)
- **US5 (Phase 7)**: depends on Phase 2 only; can run in parallel with Phases 3 to 6
- **US2 (Phase 8)**: depends on US4 (rehearsal before production) and on US5 (production must refuse Swagger); T062 depends on the researcher
- **Polish (Phase 9)**: depends on all stories

### Story order versus priority

The spec gives P1 to US1, US2, US3 and US6 and P2 to US4 and US5. US2 (production) runs last because
the constitution requires local staging (US4) before any production deployment and the production
image must already refuse Swagger (US5).

### Within Each User Story

- Tests first, failing
- Migrations before the code that reads them; back end before the front-end parts that call it
- XLIFF units added in the same task that adds the text

### Parallel Opportunities

- T004, T005, T006 in parallel with T002 and T003
- T009, T010, T013, T018, T019, T022 in parallel inside Phase 2
- T024 and T025; T031, T032 and T033; T037, T038 and T039; T043 and T044; T049 to T052 (different files)
- Phase 7 (US5) in parallel with Phases 3 to 6
- T061 in parallel with T058 to T060; T067 and T068 in parallel

---

## Parallel Example: User Story 5

```text
Task: "ApiIndexTest in backend/src/test/java/io/github/diegoquirino/veredito/api/ApiIndexTest.java" (T049)
Task: "EntryPointTest in backend/src/test/java/io/github/diegoquirino/veredito/api/v1/EntryPointTest.java" (T050)
Task: "ApiDocumentationCompletenessTest in backend/src/test/java/io/github/diegoquirino/veredito/api/ApiDocumentationCompletenessTest.java" (T051)
Task: "ApiDocsAccessTest in backend/src/test/java/io/github/diegoquirino/veredito/web/ApiDocsAccessTest.java" (T052)
```

## Parallel Example: User Story 3

```text
Task: "MigrationTest in backend/src/test/java/io/github/diegoquirino/veredito/persistence/MigrationTest.java" (T031)
Task: "DatabaseUnavailableTest in backend/src/test/java/io/github/diegoquirino/veredito/api/v1/DatabaseUnavailableTest.java" (T032)
Task: "Unavailable component test in frontend/src/app/shell/unavailable.spec.ts" (T033)
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Phases 1 and 2
2. Phase 3 (US1): root page and home page in three locales, running locally
3. **Stop and validate**: quickstart section 3 against the local application

### Incremental Delivery

1. Setup and foundation
2. US1: home page and locales
3. US3: database schema and default configuration
4. US6: translation checks, localized errors, redirects
5. US4: local staging with one command
6. US5: API index, relation docs, Swagger completeness and access
7. US2: CI, cloud setup, production deployment, rollback
8. Polish: quickstart, UTF-8, secrets, status

---

## Notes

- V1 and V2 become immutable after the first production deployment (T064); any later schema change is a new migration (V3 on), compatible with the previous code version.
- Secrets (database password, Gmail app password) are typed only by the researcher (T062); they never appear in files, logs, chat or screenshots.
- No task calls models; the sequential execution rule of `ISSTA_2027` does not apply.
- US4 scenario 2 (e-mails stay in the local mailbox) only becomes observable when spec 004 sends the first e-mail; 003 delivers the Mailpit wiring and spec 004 checks it.
