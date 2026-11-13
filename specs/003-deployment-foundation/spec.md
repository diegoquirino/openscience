# Feature Specification: Deploy Veredito and prepare the database

**Feature Branch**: `003-deployment-foundation`

**Created**: 2026-11-10

**Status**: Complete (2026-11-13; the e-mail part of US4 scenario 2 is checked in spec 004, SC-005)

**Input**: User description: "Put Veredito online at a fixed URL, with a home page, a database with all the planned tables, a separate staging environment and a way to update the system without changing the URL."

**Overall spec**: [001-oracle-labeling](../001-oracle-labeling/spec.md) · **Depends on**: nothing

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Open Veredito at a fixed URL (Priority: P1)

Anyone opens the Veredito address, with no account and no installation, and sees the study home page.

**Why this priority**: Every following spec adds screens to this page; without it there is nothing to test online.

**Independent Test**: Open the URL in a private window, on a computer and on a phone.

**Acceptance Scenarios**:

1. **Given** Veredito deployed, **When** someone opens the URL in a private window, **Then** they see the language-choice page and, after choosing a language, the home page in that language with the name "Veredito", a sentence about the study and the "Participate" button, without having to sign in to any service.
2. **Given** a phone, **When** the home page is opened, **Then** the content fits the screen width, with no horizontal scrolling.
3. **Given** the home page, **When** someone opens it, **Then** the footer shows the deployed version of the system.

---

### User Story 2 - Update the system without changing the URL (Priority: P1)

The researcher deploys a new version and participants keep using the same address.

**Why this priority**: The system will change during the data collection (text corrections, new systems); the announced URL must not break.

**Independent Test**: Deploy three versions in a row and check the URL and the version in the footer.

**Acceptance Scenarios**:

1. **Given** a version online, **When** the researcher deploys another, **Then** the URL does not change and the footer starts showing the new version.
2. **Given** a new version deployed, **When** the database already has records, **Then** no record is deleted or changed by the deployment.
3. **Given** a faulty version, **When** the researcher rolls back to the previous version, **Then** the URL stays the same.

---

### User Story 3 - Prepare the database (Priority: P1)

On the first deployment, the system creates all the tables planned in spec 001, with the integrity constraints and the default configuration. With each new version, the structure is updated automatically, without losing data.

**Why this priority**: It fixes the data structure on which all the following specs depend, so they can be implemented without redoing tables.

**Independent Test**: Deploy on an empty database, deploy again and compare the structures; deploy a version with a structure change over a database with records.

**Acceptance Scenarios**:

1. **Given** an empty database, **When** the system is deployed, **Then** the tables for import batches, items, blocks, tasks (block-role), responses, participants, access codes, access sessions, consent forms and configuration exist.
2. **Given** an empty database, **When** it is prepared, **Then** the configuration receives the default values: administrator e-mail `diegoquirino@gmail.com`, reservation of 20 minutes, block size 10, daily limit of 450 e-mails, no consent form published and study phase (`study_phase`) `pilot`.
3. **Given** a database already prepared and with records, **When** the same version is deployed again, **Then** nothing is duplicated, deleted or changed.
4. **Given** a new version that changes the structure, **When** it is deployed over a database with records, **Then** the structure is updated only once, the records are preserved and it is recorded which change was applied and when.
5. **Given** the database, **When** someone tries to access it over the internet without going through the system, **Then** the access is refused.
6. **Given** an empty database, **When** it is prepared, **Then** the participant table has the column `preferred_language`, the response table has the column `language`, and each consent form version can store one text per language (pt-BR, en-US and es-ES), with pt-BR marked as the reference version approved by the research ethics committee; language values are stored as the locale codes `pt-BR`, `en-US` and `es-ES`.

---

### User Story 4 - Rehearse locally before deploying (Priority: P2)

The researcher tests a change in the staging environment, which runs in Docker on their own machine, with a local database and a local test mailbox, before taking it to production on Google Cloud.

**Why this priority**: It allows fixing the system during the data collection without risking the real data and with no cloud cost for tests.

**Independent Test**: Start local staging from a clean copy of the repository, create records and request an access code; check that the production database and mailbox do not change.

**Acceptance Scenarios**:

1. **Given** a copy of the repository and Docker installed, **When** the researcher starts staging (profile `staging`) with a single command, **Then** the system opens at a local address, with the database prepared by the same structure changes as production.
2. **Given** local staging, **When** someone uses it, **Then** the page shows a "STAGING ENVIRONMENT" banner (in the UI language), the records go only to the local database and the e-mails stay in a local test mailbox, without reaching anyone.
3. **Given** a change approved in local staging, **When** the researcher deploys it to production, **Then** production runs exactly the same version of the code.
4. **Given** local staging, **When** the researcher deletes it and starts it again, **Then** the database comes back empty and prepared.

---

### User Story 5 - Consult the documented API (Priority: P2)

The researcher, or whoever reproduces the study, opens the interactive API documentation (Swagger) and understands each operation: what it is for, what it receives, what it returns, which errors may occur and which actions the resource offers in each state.

**Why this priority**: The following specs add operations to the API; with the entry point, the documentation and the completeness test ready since 003, each new operation is born documented.

**Independent Test**: Open Swagger in local staging, call the entry point without a session and check the links returned; try to open Swagger in production without being the administrator.

**Acceptance Scenarios**:

1. **Given** local staging, **When** the researcher opens the API documentation, **Then** they see all operations grouped by area, each one with summary, description, examples and all possible responses, including the error ones.
2. **Given** the API entry point, **When** it is called without a session, **Then** it returns only the links allowed to a visitor (request an access code), and no administrative link.
3. **Given** production, **When** someone without an administrator session tries to open the documentation, **Then** the access is refused.
4. **Given** a new operation without a documented summary, description, example or error response, **When** the tests run, **Then** they fail, pointing out the operation.
5. **Given** the API, **When** someone calls a version that does not exist (for example, `/api/v9`), **Then** they receive a standardized error with the available versions; and the index `/api` lists the supported versions, with the entry link of each one.

---

### User Story 6 - Serve the interface in three languages (Priority: P1)

The same deployed image serves the interface in Portuguese (Brazil), English (US) and Spanish (Spain), each under its own address prefix, and the backend answers its messages in the language the user asked for. A check in the automated pipeline prevents any user-facing text from being left untranslated.

**Why this priority**: Every following spec adds screens, messages and e-mails; with the infrastructure and the completeness checks ready since 003, each new text is born in the three languages, instead of being translated in a hurry before the launch.

**Independent Test**: In local staging, open `/`, `/pt-BR/`, `/en-US/` and `/es-ES/`, reload a deep link under each prefix, call an API operation that fails with each `Accept-Language`, and remove one translation on purpose to see the pipeline fail.

**Acceptance Scenarios**:

1. **Given** Veredito deployed, **When** someone opens `/pt-BR/`, `/en-US/` or `/es-ES/`, **Then** the home page (header, footer with the version and the "Participate" button) appears entirely in that language, served by the same image and the same application as the other two.
2. **Given** a locale prefix, **When** someone opens or reloads an address inside the application under that prefix (for example, `/es-ES/admin`), **Then** that locale's interface is served and opens on the requested page, never the interface of another locale.
3. **Given** the root `/`, **When** someone opens it, **Then** they see the language-choice page with the three options "Português (Brasil)", "English (US)" and "Español (España)", each one leading to its prefix; the preselection, the cookie `veredito-lang`, the participant profile and the header language switch are delivered in spec 004.
4. **Given** an interface in one locale, **When** the person switches to another language, **Then** the page is reloaded under the other prefix.
5. **Given** an API call that ends in an error with `Accept-Language: es-ES` (or `en-US`, or `pt-BR`), **When** the error is returned, **Then** the Problem Details `title` and `detail` are in that language; without `Accept-Language`, or with an unsupported language, they are in pt-BR.
6. **Given** a new user-facing text added to the interface without its pt-BR or es-ES translation, **When** the automated pipeline runs, **Then** it fails, pointing out the text and the missing locale.
7. **Given** a new backend message key present in only one or two of the three message bundles, **When** the automated pipeline runs, **Then** it fails, pointing out the key and the bundle where it is missing.
8. **Given** a code stored in the database (for example, the study phase `pilot`), **When** it is shown on screen, **Then** only its display label is translated; the stored value stays the English identifier.

---

### Edge Cases

- Database deleted or unreachable: the home page shows "System temporarily unavailable" (in the UI language) instead of a technical error.
- Two accesses at the exact moment of a deployment: each one is served entirely by the old version or by the new one.
- Page open for hours before a deployment: the next action keeps working or asks to reload the page.
- Address with an unsupported locale prefix (for example, `/fr-FR/`), or any other address outside the locale prefixes, the API (`/api`) and the API documentation: the person is redirected to the language-choice page `/`.
- Locale prefix written with different case (for example, `/en-us/`): the person is redirected to the canonical prefix (`/en-US/`).

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST be reachable at a fixed URL, without requiring an account or an installation from the visitor.
- **FR-002**: Deploying a new version MUST NOT change the URL nor alter existing data.
- **FR-003**: The footer MUST show the deployed version.
- **FR-004**: The database preparation MUST create all the tables of the data model of spec 001, with the integrity constraints, and be safe to repeat; structure changes MUST be versioned, applied automatically on deployment and recorded.
- **FR-005**: The configuration MUST store, at least: administrator e-mail, administrator preferred language (`admin_preferred_language`, empty until the first administrator sign-in in spec 004), reservation minutes (default 20), block size (default 10), daily e-mail limit (default 450), version and texts (one per language) of the consent form, and study phase (`study_phase`: `pilot`, the default, or `collection`).
- **FR-006**: The database MUST NOT be reachable over the internet; only the system and the researcher, with their own credentials, access it.
- **FR-007**: There MUST be a local staging environment, in Docker (profile `staging`), that starts with a single command, uses the same image and the same structure changes as production, has its own database and test mailbox and a visible identification on screen. There is no cloud staging.
- **FR-008**: The study rules (CSV validation, block formation, choice of the next block, export format) MUST be verifiable by automated tests without deploying the system.
- **FR-009**: Unexpected errors MUST appear to the user as a friendly message and be logged for the researcher.
- **FR-010**: On every change pushed to the repository, the tests and the image build MUST run automatically; the deployment to production MUST be triggered by the researcher, with a single command, for a version already rehearsed locally, and production receives exactly that version.
- **FR-011**: Passwords and keys MUST NOT be in the code repository.
- **FR-012**: The API MUST be REST at Richardson level 3 ("glory of REST"). This includes:
  - a single entry point that lists only the actions allowed to the caller;
  - responses that carry the links and the actions valid in the current state of the resource;
  - errors in a standardized format;
  - version in the address (`/api/v1`): a nonexistent version is refused with a standardized error, and an obsolete version keeps responding, warning the client of the date on which it will be taken offline.
- **FR-013**: The API MUST have interactive documentation (Swagger), generated from the code, with summary, description, examples and all possible responses of each operation. It is open in local staging and only for the administrator in production. An automated test MUST fail when any operation is documented incompletely.
- **FR-014**: The interface MUST be built once per locale (pt-BR, en-US and es-ES) with the official Angular i18n (`@angular/localize`), and the three builds MUST be served by the same image and the same Spring Boot application under `/pt-BR/`, `/en-US/` and `/es-ES/`; any address under a prefix that is not a static file MUST return that locale's application (SPA fallback per prefix). Switching language MUST load the other prefix, reloading the page.
- **FR-015**: The root `/` MUST be served by the same image and show the language-choice page, with each option labeled in its own language and leading to its prefix; addresses with an unsupported prefix MUST redirect to `/`. 003 delivers the routing, the serving and an empty application shell (header, footer with the version, home page) in the three locales; the language-choice screen and its behaviour (preselection by the browser language, cookie `veredito-lang`, participant profile, header switch) are specified in spec 004.
- **FR-016**: Every backend message shown to users (Problem Details `title` and `detail`, validation messages and e-mails) MUST come from message bundles (Spring MessageSource) for pt-BR, en-US and es-ES with the same keys, chosen by the request's `Accept-Language`; when it is missing or names an unsupported language, pt-BR MUST be used.
- **FR-017**: The automated pipeline MUST fail when: a pt-BR or es-ES translation file (XLIFF) of the interface has a missing or empty target for any source text; the three backend message bundles do not have exactly the same keys; or a test shows that a Problem Details response does not honour `Accept-Language` in any of the three locales.
- **FR-018**: The database preparation MUST create the column `preferred_language` in `participant`, the column `language` in `response` (the UI language at the moment the response is saved) and storage for the consent form text per version and per language, with pt-BR as the reference version; the acceptance of a consent form MUST be able to record the language of the text accepted. Language values MUST be one of `pt-BR`, `en-US` and `es-ES`.
- **FR-019**: Codes stored in the database and in exports (`change_impact`, `maintenance_operation`, `professional_skill`, screening answer, study phase) MUST be English identifiers; only their display labels are translated. Data (imported items, participant comments, names, the consent form text each person accepted) MUST be stored as captured, never translated.

### Key Entities

- **Configuration**: study parameters that the researcher adjusts without deploying code.
- **Environment**: production (Google Cloud) or staging (local Docker), each one with its own database.
- **Locale**: `pt-BR`, `en-US` or `es-ES`; address prefix of the interface, language of the backend messages and value recorded in `preferred_language` and `language`.
- **Message bundle**: set of backend texts of one locale; the three bundles have the same keys.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Local staging starts with a single command in less than 10 minutes the first time (including the image build), and a rehearsed version reaches production in less than 15 minutes after the researcher triggers the deployment.
- **SC-002**: In three deployments in a row, the URL does not change and no database record is altered.
- **SC-003**: Preparing the database twice results in the same structure.
- **SC-004**: The home page opens in less than 3 seconds, once the system is already in use.
- **SC-005**: The monthly cost of the system stays below R$ 200, with an automatic alert if it goes over that.
- **SC-006**: A search for passwords and keys in the public repository finds nothing.
- **SC-007**: 100 % of the API operations appear in the documentation with summary, description, example and error responses, checked by an automated test.
- **SC-008**: In the automated pipeline, the completeness checks pass on every change: 100 % of the interface texts have pt-BR and es-ES targets, the three backend message bundles have the same set of keys, and Problem Details come back in the requested language for each of the three locales; removing a single translation on purpose makes the pipeline fail.

## Assumptions

- The technology follows the selection in [research.md](../001-oracle-labeling/research.md) of spec 001 and constitution 2.7.0.
- The production database lives in the researcher's cloud project (`veredito-issta2027`); no participant has direct access to it.
- Local staging, the automated tests and the pilot in production use only the items of the Minitest system (the CSV of spec 002 and files derived from it); the participants of local staging and of the tests are fictitious; nothing from production is copied to the machine. The refusal of other systems in the import is delivered in spec 005.
- The first opening after a period without use may take a few seconds more; this can only be measured in production.
- The "Participate" button leads to a provisional screen until spec 004 is delivered.
- The source language of the interface texts in the code is en-US (everything in the repository is in English); pt-BR and es-ES are translations, and pt-BR is the reference version of the consent form approved by the research ethics committee.
- Local staging details are in [research.md](../001-oracle-labeling/research.md#local-staging-docker).
