<!--
Sync Impact Report
- Version change: 2.6.1 -> 2.7.0 (MINOR: English everywhere; internationalized UI; data kept in its
  capture language)
- Modified principles (2.7.0):
  - I. Standard, current and open-source stack: `@angular/localize` (official Angular i18n, same
    version as Angular) with one build per locale; Spring MessageSource for backend messages;
    OpenAPI summaries and descriptions in English
  - II. Isolated and tested domain rules: automated i18n completeness checks (Angular trans-units
    with pt-BR and es-ES targets; same keys in the three backend message bundles; Problem Details
    in the requested language)
  - IV. Privacy, LGPD and secrets: identifiers `staging` (former `ensaio`), study phase `pilot`
    (former `piloto`); Minitest fixture `backend/src/test/resources/minitest/veredito_minitest.csv`;
    pipeline files `t1_<system>_rater*.csv`
- Added sections (2.7.0):
  - Language and internationalization: everything in the repository in English; UI in pt-BR,
    en-US and es-ES; language choice before sign-in; consent form and e-mails in three languages
    (pt-BR is the CEP-approved reference); capture language recorded; English codes in the
    database and exports; data never translated; vocabulary and identifiers in `specs/GLOSSARY.md`
- Modified sections (2.7.0):
  - Scope and exceptions: spec 002 folder is `002-oracle-import-csv`
  - Restrictions and workflow: profile `staging`, study phase `collection`; the Portuguese
    vocabulary rule is replaced by a reference to `specs/GLOSSARY.md` (UI vocabulary per locale)
- Templates/docs (2.7.0): all specs (001 to 011) translated into English; spec folders renamed
  with git mv (`001-oracle-labeling`, `002-oracle-import-csv`, `003-deployment-foundation`,
  `004-email-code-access`, `005-block-import`, `006-consent-screening`, `007-block-queue`,
  `008-item-screen`, `009-exports`, `010-progress-dashboard`, `011-lgpd-withdrawal`);
  `specs/GLOSSARY.md` added; specs/ROADMAP.md translated (constitution 2.7.0; i18n infrastructure
  in 003; language-choice screen in 004)
- Follow-up TODOs (2.7.0): none
- Version change 2.6.1 (2.6.0 -> 2.6.1, PATCH: success criterion checked in a later spec)
- Modified sections (2.6.1):
  - Restrictions and workflow: a criterion that can only be checked in a later spec is left to it,
    if the spec says so (case of SC-003 of spec 002, checked in 005)
- Templates/docs (2.6.1): specs/ROADMAP.md (strictly sequential order: 002, 003, 004, 005,
  007, 006, 008, 009, 010, 011; no parallelism columns); specs 002, 003, 005, 006 and 009
  (headers without "May run in parallel"); spec 007 (eligibility left open until 006)
- Version change 2.6.0 (2.5.0 -> 2.6.0, MINOR: data labeling vocabulary)
- Modified sections (2.6.0):
  - Restrictions and workflow: vocabulary (data labeling, former "rotulagem dos dados"; "avaliar"
    in buttons and short messages; participant and volunteer; rater 1 and 2)
  - III. Data collection integrity (rationale): "two classifications" becomes "two labels"
  - Scope and exceptions: "evaluator data" becomes "rater data"
- Version change 2.5.0 (2.4.0 -> 2.5.0, MINOR: API versioning in the path, `/api/v1`)
- Modified principles (2.5.0):
  - I. Standard, current and open-source stack: version in the path with the native support of
    Spring Framework; a new version only for an incompatible change; the previous one deprecated
    with `Deprecation` and `Sunset` until it is no longer used; domain and database without
    version; one entry point per version (`/api/v1`); one OpenAPI specification per version
  - II. Isolated and tested domain rules: tests start from `/api/v1`; each supported version has
    its own tests and its checked specification
- Version change 2.4.0 (2.3.0 -> 2.4.0, MINOR: local staging, tests and pilot only with Minitest
  items)
- Modified principles (2.4.0):
  - IV. Privacy, LGPD and secrets: local staging, automated tests and the pilot in production use
    only Minitest items; the import refuses another system in staging and in phase `pilot`
- Modified sections (2.4.0):
  - Restrictions and workflow: pilot only with Minitest; after the database is recreated, the
    phase switches to `collection` before the real dataset is imported
- Version change 2.3.0 (2.2.0 -> 2.3.0, MINOR: automated API tests with REST Assured)
- Modified principles (2.3.0):
  - II. Isolated and tested domain rules: API tests with REST Assured, over real HTTP on a random
    port, starting from `/api` and following the links
- Templates: no Spec Kit template mentions a test tool; nothing to adjust
- Version change 2.2.0 (2.1.0 -> 2.2.0, MINOR: REST API at Richardson level 3, "glory of REST", and
  documented in OpenAPI/Swagger)
- Modified principles (2.2.0):
  - I. Standard, current and open-source stack: API rule (resources, HTTP verbs and status codes,
    HAL-FORMS hypermedia with Spring HATEOAS, entry point `/api`, Problem Details) and OpenAPI
    3.1 documentation with Swagger UI (springdoc-openapi)
  - II. Isolated and tested domain rules: links and actions per state covered by API tests; a
    test that blocks an endpoint without complete documentation
- Version change 2.1.0 (2.0.1 -> 2.1.0, MINOR: environments redefined; production on Google Cloud
  and local staging in Docker, no cloud staging)
- Modified sections (2.1.0):
  - Restrictions and workflow: the staging rule becomes local Docker; pilot in production with the
    database recreated empty before the announcement
  - IV. Privacy, LGPD and secrets: local staging only with fictitious data; production data never
    comes down to the machine
  - V. Simplicity and controlled cost: a single service and a single database in the cloud
- Templates: no Spec Kit template mentions environments; nothing to adjust
- Version change 2.0.1: the Principle V budget alert changes from US$ 15 to R$ 200 per month
- Earlier version change: 1.0.0 -> 2.0.0 (MAJOR: technology change redefines Principles I, II and V)
- Modified principles (2.0.0):
  - I. Zero cost on a free Google account -> I. Standard, current and open-source stack
  - II. Rules testable without deploying -> II. Isolated and tested domain rules
  - III. Data collection integrity (NON-NEGOTIABLE): script lock -> database transactions and
    constraints; migrations only through Flyway
  - IV. Privacy and LGPD -> IV. Privacy, LGPD and secrets (public repository)
  - V. Simplicity within the platform limits -> V. Simplicity and controlled cost
- Added sections: none (Scope and exceptions, Restrictions and workflow rewritten)
- Removed sections: none
- Follow-up TODOs: none
-->

# Veredito Constitution

## Core Principles

### I. Standard, current and open-source stack

- Frontend: Angular with Angular Material (Material Design 3), in TypeScript, internationalized
  with `@angular/localize` (official Angular i18n, same version as Angular): one build per locale
  (pt-BR, en-US, es-ES).
- Backend: Spring Boot on Java LTS, with Maven. User-facing backend messages (Problem Details
  `title`/`detail`, e-mails) come from Spring MessageSource, in the user's language.
- API: REST at level 3 of the Richardson maturity model ("glory of REST"):
  - resources identified by URI;
  - HTTP methods and status codes with their standard semantics;
  - hypermedia with Spring HATEOAS in HAL-FORMS: each response carries the links and actions
    allowed in the current state of the resource;
  - version in the path (`/api/v1`, `/api/v2`), with the native support of Spring Framework for
    API versions. Only an incompatible change creates a new version; the previous one stays
    online, marked as deprecated (headers `Deprecation` and `Sunset`), until it is no longer used.
    The domain layer and the database have no version;
  - a single entry point per version, `/api/v1`; the Angular client navigates through the link
    relations and does not build API URLs;
  - errors in Problem Details (RFC 9457).
- API documentation: OpenAPI 3.1 generated from the code (springdoc-openapi), with Swagger UI and
  one specification per API version. Every endpoint MUST have:
  - a summary and a description in English;
  - parameters, body and examples;
  - all possible responses, with the error schema;
  - the link relations and actions it offers.

  Swagger UI is open in local staging and, in production, only to the administrator.
- Database: MySQL Community, with the schema versioned by Flyway (open-source edition).
- Each spec MUST start with the latest stable versions of each piece (the LTS, when there is
  one) and record them in its plan. Minor and patch updates are free; replacing a piece with
  another technology requires amending this constitution.
- Only community or open-source editions; no paid license.

Rationale: mainstream, well-known and well-documented technologies give confidence to maintain the
system throughout the data collection and to whoever reproduces the study.

### II. Isolated and tested domain rules

- CSV validation, splitting into blocks, choosing the next block and the export format MUST live
  in a domain layer in plain Java, without dependencies on Spring, the database or HTTP, with unit
  tests.
- Database access MUST have integration tests against a MySQL of the same version as production,
  in a container.
- Automated API tests MUST be written with REST Assured, over real HTTP against the application on
  a random port (Spring Boot Test with MySQL in a container). Each test starts from the entry
  point of the version (`/api/v1`) and follows the link relations, like the Angular client; the
  only fixed URL in the tests is the entry point. Each supported version has its own tests.
- The links and actions the API offers in each state of a resource MUST be covered by these
  tests; an action forbidden in that state does not appear as a link and, if called, is refused.
- A test MUST fail if any endpoint of the generated OpenAPI specification, in any supported
  version, lacks a summary, description, error responses or examples; CI keeps the specification
  of each version (`openapi-v1.json`, ...) for each commit.
- Internationalization completeness MUST be checked automatically in CI:
  - every Angular trans-unit has a pt-BR and an es-ES target (en-US is the source locale);
  - the three backend message bundles (pt-BR, en-US, es-ES) have the same keys;
  - API tests check that Problem Details come in the requested language.
- A new or changed rule MUST come with its tests, in the same commit.

Rationale: the allocation rules are the point of highest risk and must be verified without
depending on the deployed environment.

### III. Data collection integrity (NON-NEGOTIABLE)

- Requesting a block, saving a draft, submitting a block and importing MUST each run in a single
  database transaction, locking the contended rows.
- The invariants MUST also be in the database, as constraints: one participant per block (never
  both roles), one response per item and role, one item per (`system`, `edit_key`).
- Blocks and their positions in the queue MUST NOT change after they are created; final responses
  are immutable for the participant.
- The schema only changes through versioned Flyway migrations; an already applied migration is
  NEVER edited.

Rationale: the oracle only has value if each item has two independent and complete labels, from
different people, and this cannot depend only on the application code.

### IV. Privacy, LGPD and secrets

- The code lives in a public repository: passwords, keys, database URLs with credentials,
  participant data, dumps and exports NEVER go into git.
- Production secrets live in Google Cloud Secret Manager and, for deployment, in GitHub secrets;
  GitHub accesses Google Cloud without a long-lived key.
- Access codes and session identifiers MUST be stored only as hashes; the session goes in an
  `HttpOnly`, `Secure` and `SameSite` cookie.
- The long table and the pipeline files (`t1_<system>_rater*.csv`) MUST NOT contain name or
  e-mail.
- The database is not exposed to the internet; participants only access data through the
  application.
- Local staging, the automated tests and the pilot in production use only items of the Minitest
  system: the CSV generated by spec 002, versioned as the fixture
  `backend/src/test/resources/minitest/veredito_minitest.csv`, and files derived from it for the
  error and volume cases. The CSV is versioned in the repository because Minitest is already
  public. The participants of local staging and of the tests are fictitious.
- The import MUST refuse a file of another system in local staging and, in production, while the
  study phase is `pilot`.
- Production dumps and exports MUST NOT be loaded on the development machine.

Rationale: the study collects personal data under a consent form, the replication package will be
public and the repository itself is already public.

### V. Simplicity and controlled cost

- A single deployable artifact: the same container serves the compiled Angular app (one build per
  locale) and the API.
- In production, only managed Google Cloud services: Cloud Run, Cloud SQL for MySQL, Secret
  Manager and Artifact Registry, with a single Cloud Run service and a single database. Any
  additional service requires a justification in the plan.
- The Google Cloud project (`veredito-issta2027`) MUST have a monthly budget of R$ 200, with
  alerts only (no limit that suspends the service), warning at 50%, 90% and 100%.
- Interface only with Angular Material components, responsive from phone to desktop.
- Each API call MUST finish in less than 2 seconds under normal use.

Rationale: fewer pieces mean less to maintain, and a predictable cost fits a student's budget.

## Scope and exceptions

- This constitution governs the Veredito app, deployed from the `Veredito` branch of the
  `diegoquirino/openscience` repository.
- **Exception: spec 002 (`002-oracle-import-csv`).** The import CSV generator is a Python command
  of the pipeline in `ISSTA_2027` and follows that project's conventions. Principles I, II, III and
  V and the deployment rules do not apply to it. Still in force: no rater data and no blind
  identifiers in the generated file (Principle IV), UTF-8 and compatibility with the Veredito
  import format.

## Language and internationalization

- Everything in the repository MUST be in English: folder and file names, identifiers (database
  schema, REST paths, link relations, configuration keys and values), code, code comments,
  documentation and commit messages.
- The vocabulary and identifiers MUST follow `specs/GLOSSARY.md`, which also holds the user
  interface vocabulary per locale.
- The user interface MUST be available in pt-BR, en-US and es-ES, with the official Angular i18n
  (`@angular/localize`): one build per locale, served by the same Spring Boot application under
  `/pt-BR/`, `/en-US/` and `/es-ES/`; switching language reloads the page. Backend messages
  (Problem Details `title`/`detail`, e-mails) MUST come from Spring MessageSource in the user's
  language.
- The first screen, before sign-in, MUST let the user choose the language among the three
  options, with the browser language preselected when supported (fallback pt-BR). The choice is
  saved in the browser and, after sign-in, in the participant profile (`preferred_language`); it
  MUST be possible to change it at any time from the header.
- The consent form and the e-mails MUST exist in the three languages. The pt-BR version is the
  reference approved by the research ethics committee (CEP); the en-US and es-ES versions MUST
  state that they are translations and link to the pt-BR version.
- The capture language MUST be recorded: each response records the UI language at save time
  (`language`); the participant's preferred language and the language of the accepted consent form
  are recorded too; exports MUST include them.
- Codes stored in the database and in exports (`change_impact`, `maintenance_operation`,
  `professional_skill`, screening answer, study phase) MUST be English identifiers; only their
  display labels are translated. Every user-facing string MUST exist in the three locales,
  checked automatically in CI (Principle II).
- Data MUST NEVER be translated: imported use cases and edits, participant comments, names and
  the consent form text each person accepted stay in the language in which they were captured.
- A missing use case in the import CSV keeps the pipeline text
  `(the use case does not exist in this version)` as captured; the item screen MUST show a
  localized message instead. A META edit may have the origin OR the target empty (not both); the
  empty side MUST be shown as a localized "(none)".

## Restrictions and workflow

- The `Veredito` branch is orphan: it shares no history with the other studies in the repository.
- **Environments**: production is Google Cloud (project `veredito-issta2027`); staging (tests) is
  local, in Docker: Docker Compose with the image built by the same `Dockerfile` as production
  (profile `staging`), a MySQL of the same version and a test e-mail server. There is no cloud
  staging.
- Every change MUST pass the automated tests and local staging before production.
- The pilot with colleagues happens in production, before the announcement, only with Minitest
  items (study phase `pilot`). At its end, the production database is recreated empty, the
  administrator switches the study phase to `collection` and only then imports the real dataset.
- Deployment is done by GitHub Actions from the `Veredito` branch, always to the same Cloud Run
  service, to keep the URL.
- Specs are implemented one at a time, in the order of `specs/ROADMAP.md`. The next one only
  starts when the success criteria of the previous one are met in local staging (or in
  production, when the criterion concerns the deployment itself). A criterion that can only be
  checked in a later spec is left to it, if the spec says so.
- Exported files MUST remain compatible with the `issta_tcm` package.
- All text files of the project MUST be in UTF-8.
- Vocabulary: terms, identifiers and the user interface vocabulary per locale are defined in
  `specs/GLOSSARY.md` (see *Language and internationalization*). Pipeline column and file names
  (`rater1`, `rater2`) do not change.

## Governance

- This constitution prevails over other project practices. Every plan (`/speckit-plan`) MUST
  check compliance with it.
- A plan that violates a principle MUST justify the exception in its constitution check section;
  without an accepted justification, the plan does not proceed.
- Amendments are made with `/speckit-constitution`, record the date and follow
  major.minor.patch versioning: major to remove or redefine a principle, minor to add a principle
  or section, patch for wording adjustments.

**Version**: 2.7.0 | **Ratified**: 2026-11-10 | **Last Amended**: 2026-11-11
