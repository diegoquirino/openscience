# Implementation Plan: Sign in with name, e-mail and code

**Branch**: `004-email-code-access` | **Date**: 2026-11-13 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/004-email-code-access/spec.md`

## Summary

Spec 004 adds the sign-in on top of the skeleton of 003.

- **Code by e-mail:** a person types name and e-mail, receives a 6-digit code by e-mail (Gmail in
  production, Mailpit in staging) in the language of the interface, and types it to sign in.
- **Session:** a 24-hour session in an `HttpOnly` cookie, stored only as a hash. Spring Security,
  stateless, with a cookie filter and path rules, protects the API.
- **Two areas:** the administrator (`config.admin_email`) goes to an administrative area whose
  menu items are disabled until later specs. Everyone else gets a pseudonym (`R-0001`, ...) and a
  provisional screen.
- **Language:** the root page preselects the browser language and remembers the choice in the
  cookie `veredito-lang`. After sign-in, the profile language wins. A header switch on every
  screen changes the language, saves it in the profile and keeps whatever was typed.

The database needs only `V3__access.sql` (two columns, two indexes). The deployment needs one new
secret, the key of the code hash. Decisions are in [research.md](research.md).

## Technical Context

**Language/Version**: Java 25 LTS with Spring Boot 4.1.2 (4.2.x if its GA, due on 2026-11-19,
comes before the deployment of 004); TypeScript with Angular 22.2.6; Node.js 26 LTS for the build

**Primary Dependencies**:
- **Back end:** as in 003, plus Spring Security 7.1.2 (`spring-boot-starter-security`). Spring
  Mail is already in the build and is used now.
- **Front end:** as in 003 (Angular Material 22.2.6, `@angular/localize`).

**Storage**: MySQL 9.7 with the tables of 003; `V3__access.sql` adds `config.admin_name`, `access_code.name` and the
indexes `access_code (created_at)` and `access_session (expires_at)` ([data-model.md](data-model.md))

**Testing**:
- **Back end:** JUnit 5 domain tests for the access rules; Testcontainers MySQL 9.7 and Mailpit
  v1.33.1.
- **API:** REST Assured 6.3.0, with `AccessTest`, `SessionTest`, `AuthorizationTest` (every
  "Administration" operation of the OpenAPI, SC-003), `LanguageProfileTest`, and the new types in
  `LocalizedProblemDetailsTest`; a movable `Clock` for the expiries.
- **Front end:** Vitest.
- **End to end:** Playwright 1.65.0, reading codes from the Mailpit API.

**Target Platform**: as in 003 (Cloud Run plus Cloud SQL in production; Docker Compose `staging`
locally; GitHub-hosted Ubuntu runners)

**Project Type**: web application (one container: Spring Boot API plus three Angular builds)

**Performance Goals**:
- `POST /api/v1/access-codes` under 2 s with the synchronous SMTP send (Principle V; measured in
  production, research section 6).
- Every other call under 2 s.
- Code in the inbox in under 1 min for 95 % of sends (SC-002).
- Sign-in in under 3 min (SC-001).

**Constraints**:
- **Limits:** the Gmail quota of about 500 recipients per day is guarded by
  `config.daily_email_limit` (450) over a rolling 24-hour window.
- **Stored values:** codes and session tokens only as hashes; no e-mail or token in the logs.
- **Rollback:** V3 is additive, so a rollback to the revision of 003 still works.
- **Language:** the e-mail is in the language of the interface in use, never the profile's,
  before sign-in.

**Scale/Scope**:
- A few hundred participants and one administrator.
- 4 new API resources: access codes, sessions, the participant profile and the administrator
  profile.
- 4 new screens (sign-in, code, provisional participant screen, administrative area), plus the
  header and the root page.
- 7 new Problem Details types.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

Constitution 2.7.1.

| Rule | How the plan meets it | Status |
|---|---|---|
| I. Stack standard and current; versions recorded | Spring Security managed by Spring Boot 4.1.2; versions checked on 2026-11-13 (research section 1); no new third-party library | Pass |
| I. MessageSource for backend messages (Problem Details, e-mails) | New types and the e-mail subject and body in the three bundles (research section 7) | Pass |
| I. REST level 3: resources, verbs and codes, HAL-FORMS, links per state, single entry point | Resources `/access-codes`, `/access-sessions/current`, `/participants/me`, `/administrator/me`; entry point links by caller; `_templates` for the language; `vd:sign-in`, `vd:session`, `vd:sign-out`, `vd:me` documented at `/rels` ([contracts/access.md](contracts/access.md)) | Pass |
| I. OpenAPI complete per operation; Swagger admin-only in production | Every new operation documented; the `AdministratorSessionCheck` of 003 gets its real implementation | Pass |
| II. Domain in plain Java with unit tests | E-mail and name rules, code normalization, the limit decision and the pseudonym format in `domain.access`, without Spring (ArchUnit test of 003) | Pass |
| II. MySQL integration tests; REST Assured from `/api/v1` following links | Testcontainers MySQL and Mailpit; tests start at the entry point and follow `vd:request-code`, `vd:sign-in`, `vd:session`, `vd:me`, `vd:sign-out` | Pass |
| II. i18n completeness (XLIFF targets, bundle parity, localized Problem Details) | New XLIFF units for the screens; new bundle keys; one localized case per new type | Pass |
| II. A new rule comes with its tests in the same commit | Tasks pair each rule with its test | Pass |
| III. Transactions and constraints; schema only through Flyway | Code request: one transaction with a lock on `config`; sign-in: one transaction that consumes the code, upserts the participant and creates the session; `V3` only adds | Pass |
| IV. Codes and session ids only as hashes; cookie `HttpOnly`, `Secure`, `SameSite` | HMAC-SHA256 of the code with a key in Secret Manager; SHA-256 of a 256-bit token; cookie flags of research section 3 | Pass |
| IV. No secrets or participant data in git; staging and tests with fictitious data | Staging key is a labeled non-secret value; tests use `example.com`; the production key is created without being shown | Pass |
| V. Single artifact; only the allowed Google Cloud services | One more secret in Secret Manager; no new service | Pass |
| V. Material only, responsive; API under 2 s | Material form fields, buttons and menu; synchronous SMTP measured, with the asynchronous fallback of research section 6 | Pass, with a measurement in production |
| Language: English in the repository; UI in three locales; language choice before sign-in, saved in browser and profile, header switch keeps unsaved input; e-mails in three languages | Research section 11 and [contracts/web.md](contracts/web.md); e-mail texts with pt-BR as the reference | Pass |
| Workflow: staging before production; deployment by tag | Quickstart sections 2 to 9 in staging before section 10 | Pass |
| All text files UTF-8 | As in 003 | Pass |

No violation. Two points are recorded for the researcher:
- **2-second limit:** the synchronous SMTP send is expected to stay under 2 seconds but depends on
  Gmail. The quickstart measures it in production; if most sends go above, sending becomes
  asynchronous (research section 6).
- **E-mail text location:** the e-mail text moves from per-locale template files (planned in spec
  001 research) to the message bundles, so the parity test covers it. Spec 001 research is
  updated accordingly.

**Post-design re-check** (after data-model and contracts):
- **Services and secrets:** the design adds no service and one secret (`veredito-code-key`).
- **Coverage:** every new operation has its authorization rule, its Problem Details types in the
  three locales and its place in the REST Assured tests. `AuthorizationTest` enumerates
  administrative operations from the OpenAPI, so specs 005, 009, 010 and 011 are covered without
  editing it.

Pass.

## Risks

| Risk | Mitigation |
|---|---|
| Someone exhausts the daily e-mail quota on purpose | The per-e-mail limits slow it down; the researcher can raise `daily_email_limit` up to the Gmail quota. If it happens, a per-IP limit is the next measure (research section 5) |
| Gmail delays or filters the code | Spam warning on screen; SC-002 measured in production; a transactional provider stays the fallback of spec 001 research |
| Gmail SMTP slower than 2 s | Asynchronous sending fallback (research section 6) |
| Missing `veredito-code-key` at deployment | Cloud Run keeps the previous revision; the quickstart creates the secret first |

## Project Structure

### Documentation (this feature)

```text
specs/004-email-code-access/
|-- plan.md               # this file
|-- research.md           # phase 0
|-- data-model.md         # phase 1
|-- quickstart.md         # phase 1
|-- contracts/
|   |-- access.md         # access codes, sessions, profiles, links by caller, authorization
|   |-- problem-details.md
|   |-- web.md            # root page, cookies, screens, header, language switch
|   `-- pipeline.md       # CODE_HASH_KEY, secret, deploy.yml
|-- checklists/requirements.md
`-- tasks.md              # phase 2 (/speckit-tasks; not created here)
```

### Source Code (root of the `Veredito` branch; new or changed in 004)

```text
backend/
|-- pom.xml                              # version 0.4.0; spring-boot-starter-security
`-- src/
    |-- main/
    |   |-- java/io/github/diegoquirino/veredito/
    |   |   |-- domain/access/           # EmailAddress, PersonName, AccessCodeFormat, SendLimits, Pseudonym (plain Java)
    |   |   |-- access/                  # AccessCodeService, SignInService, CodeHasher, TokenHasher, AccessCodeMailer, CleanupJob
    |   |   |-- persistence/             # AccessCodeRepository, AccessSessionRepository, ParticipantRepository, ConfigRepository (JdbcClient)
    |   |   |-- web/                     # SecurityConfig, SessionCookieFilter, ProblemAuthenticationEntryPoint, ProblemAccessDeniedHandler,
    |   |   |                            # SecurityContextAdministratorCheck (replaces NoAdministratorSession), LocaleRoutingFilter (cookie redirect)
    |   |   |-- api/                     # ProblemType and ProblemDetailsAdvice gain the new types
    |   |   |-- api/v1/                  # AccessCodeController, AccessSessionController, ParticipantMeController, AdministratorMeController,
    |   |   |                            # their models; EntryPointController links by caller; LinkRelations gains the new relations
    |   |   `-- config/                  # ClockConfig, AccessProperties (CODE_HASH_KEY, limits), scheduling
    |   `-- resources/
    |       |-- db/migration/V3__access.sql
    |       |-- messages*.properties     # new problem types and mail.access-code.*
    |       `-- static/index.html, language.js   # preselection and cookie
    `-- test/java/io/github/diegoquirino/veredito/
        |-- domain/access/               # unit tests of the rules
        |-- support/                     # Mailpit container and client, movable clock, session specifications
        |-- persistence/                 # V3, participant upsert
        `-- api/v1/                      # AccessTest, SessionTest, AuthorizationTest, LanguageProfileTest

frontend/src/app/
|-- core/                                # session.service, language.service (cookie, switch, unsaved input), hal-forms template reader,
|                                        # problem-messages (type -> localized text), guards
|-- shell/                               # header with language menu, name and "Sign out"; footer without language links
|-- participate/                         # sign-in form, code screen
|-- welcome/                             # provisional participant screen
`-- admin/                               # administrative area with the disabled menu
frontend/src/locale/messages.pt-BR.xlf, messages.es-ES.xlf
frontend/e2e/                            # sign-in per locale, profile language, header switch, administrator

compose.yaml, .env.example               # CODE_HASH_KEY (staging value; documented without value)
.github/workflows/deploy.yml             # --set-secrets gains CODE_HASH_KEY
docs/cloud-setup.md                      # secret veredito-code-key and its binding
```

**Structure Decision**: the two-folder web application of 003, unchanged. The new package
`access` holds the services that coordinate the domain rules, the repositories and the mailer,
so the controllers in `api.v1` stay thin and the rules stay in `domain.access`.

## Complexity Tracking

No violations.
