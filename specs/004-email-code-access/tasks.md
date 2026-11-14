---

description: "Tasks of spec 004: sign in with name, e-mail and code"
---

# Tasks: Sign in with name, e-mail and code

**Input**: documents in `specs/004-email-code-access/` (plan.md, spec.md, research.md, data-model.md, contracts/, quickstart.md)

**Prerequisites**: spec 003 complete; Docker Desktop running; JDK 25 on the researcher's machine

**Tests**: included. Constitution Principle II requires them (domain unit tests, Testcontainers, REST Assured from `/api/v1`, bundle parity, XLIFF completeness, Playwright), and SC-003, SC-005 and SC-006 are checked by automated tests.

**Organization**: tasks grouped by user story; each story can be tested on its own once the foundation is ready.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: can run in parallel (different files, no dependency on an incomplete task)
- **[Story]**: story of the spec (US1 to US4)
- Paths are relative to the root of the `Veredito` branch (the `Veredito` folder)
- `<pkg>` = `backend/src/main/java/io/github/diegoquirino/veredito`; `<tpkg>` = `backend/src/test/java/io/github/diegoquirino/veredito`; `<app>` = `frontend/src/app`
- Code, comments, identifiers and commit messages in English; user-facing texts in pt-BR, en-US and es-ES, following `specs/GLOSSARY.md`; every new Angular text gets its pt-BR and es-ES targets in `frontend/src/locale/messages.pt-BR.xlf` and `messages.es-ES.xlf` in the same task
- No e-mail, code or token in logs, test output, chat or screenshots; test e-mails under `example.com`
- Every text file in UTF-8; after each phase, run `scripts/_check_utf8.py` (in `TRABALHO DE TESE/scripts/`) over the changed folders
- Commit at the end of each phase on the `Veredito` branch

---

## Phase 1: Setup

**Purpose**: dependencies, versions, configuration values and test support

- [X] T001 Update `backend/pom.xml`: version `0.4.0`; add `spring-boot-starter-security` and, in test scope, `spring-boot-starter-security-test` (both managed by Spring Boot 4.1.2, Spring Security 7.1.2, research section 1). Update `frontend/package.json` to version `0.4.0`
  **Record (2026-11-13):** pom `0.4.0` with `spring-boot-starter-security` and `spring-boot-starter-security-test` (Spring Security 7.1.2); `frontend/package.json` and `package-lock.json` `0.4.0`; default `VEREDITO_VERSION` in `compose.yaml` `0.4.0+dev`.
- [X] T002 [P] Add `CODE_HASH_KEY` to the `app` service of `compose.yaml` with a fixed 32-byte base64url value and the comment `# staging only; not a secret (production reads veredito-code-key from Secret Manager)`; add `CODE_HASH_KEY=` without value to `.env.example`, with a comment pointing to `specs/004-email-code-access/contracts/pipeline.md`
  **Record (2026-11-13):** the staging value is a readable phrase with spaces (`staging only - not a secret - code hash key`) instead of a base64url value, because the gitleaks generic-key rule flags any high-entropy value assigned to a `*KEY` name. The key is therefore read as UTF-8 text of at least 32 characters (production: the 32 characters of `New-RandomPassword`); `contracts/pipeline.md` and research section 4 updated. `.env.example` has `CODE_HASH_KEY=` with the comment.
- [X] T003 [P] Create `package-info.java` with a one-line description in `<pkg>/access` (services that coordinate the access rules, repositories and mailer) and `<pkg>/domain/access` (plain-Java access rules)
  **Record (2026-11-13):** both `package-info.java` created.
- [X] T004 [P] Create the Mailpit test support:
  - **`<tpkg>/support/MailpitContainerConfig.java`:** a Testcontainers `GenericContainer` `axllent/mailpit:v1.33.1` exposing 1025 and 8025, started once per test run, with `@DynamicPropertySource` for `spring.mail.host` and `spring.mail.port`.
  - **`<tpkg>/support/MailpitClient.java`:** over the Mailpit HTTP API (`/api/v1/search?query=to:<address>`, `/api/v1/message/{id}`, `DELETE /api/v1/messages`), it can list messages to an address, read the latest subject and text body, extract the 6-digit code, and clear the inbox.
  **Record (2026-11-13):** `MailpitContainerConfig` registers the mail host and port with a `DynamicPropertyRegistrar` bean (the bean form of `@DynamicPropertySource`, so it works through `@Import`) over a static container started once per run; `MailpitClient` waits up to 5 s for a message to arrive.

---

## Phase 2: Foundational

**Purpose**: schema, clock, properties, problem types, hashing, repositories, security and links used by every story

**CRITICAL**: no story starts before this phase is complete

- [X] T005 Create `backend/src/main/resources/db/migration/V3__access.sql` exactly as in `specs/004-email-code-access/data-model.md` ("Migration `V3__access.sql`"): `ALTER TABLE config ADD COLUMN admin_name VARCHAR(120) NULL`; `ALTER TABLE access_code ADD COLUMN name VARCHAR(120) NULL`; index `ix_access_code_created_at` on `access_code (created_at)`; index `ix_access_session_expires_at` on `access_session (expires_at)`. Only additions (rollback to 003 must keep working)
  **Record (2026-11-13):** `V3__access.sql` with the two columns (`AFTER admin_email`, `AFTER email`) and the two indexes; nothing else.
- [X] T006 Extend `<tpkg>/persistence/MigrationTest.java`: V3 is applied once (third row of `flyway_schema_history`), `config.admin_name` and `access_code.name` exist as `VARCHAR(120)` nullable, both indexes exist, and the two-container structure comparison of 003 still passes
  **Record (2026-11-13):** versions `1`, `2`, `3`; new test `v3AddsTheNameColumnsAndTheCleanupIndexes` (types, nullability, index columns); the two-container comparison passes.
- [X] T007 [P] Create `<pkg>/config/ClockConfig.java` (a `java.time.Clock` bean, `Clock.systemUTC()`) and `<tpkg>/support/MovableClock.java` (a `Clock` that tests can move forward, registered as `@Primary` through a `@TestConfiguration`, reset before each test). Every new class reads time only from the `Clock` bean
  **Record (2026-11-13):** `ClockConfig` and `MovableClock` (system clock plus an offset; nested `MovableClock.Config` registers it as `@Primary`); `ApiTestBase` resets it before each test.
- [X] T008 [P] Create `<pkg>/config/AccessProperties.java` (`@ConfigurationProperties("veredito.access")`):
  - **Fields:** `codeHashKey` (base64url); `codeValidity` 10 minutes; `resendInterval` 60 seconds; `hourlyLimit` 5; `maxAttempts` 5; `sessionDuration` 24 hours; `mailFrom`.
  - **Mapping:** in `application.yaml`, `veredito.access.code-hash-key: ${CODE_HASH_KEY:}` and `veredito.access.mail-from: ${SMTP_USER:veredito@localhost}`.
  - **Startup checks:** in the `prod` profile, startup fails when the key is missing or decodes to fewer than 32 bytes. In development (no profile), a missing key is replaced by 32 random bytes with one `WARN` log line saying so, without the value.
  - **Mail timeouts** in `application.yaml`: `spring.mail.properties.mail.smtp.connectiontimeout: 5000`, `timeout: 10000`, `writetimeout: 10000` (research section 6).
  - **Tests:** a random key per run in the test configuration.
  **Record (2026-11-13):** `AccessProperties` (record) and `AccessConfig` with the `CodeHashKey` bean: missing key stops the startup in `prod`; a key under 32 characters stops it in any profile; otherwise a missing key becomes 32 random bytes with one `WARN` line. Tests: `veredito.access.code-hash-key=${random.uuid}${random.uuid}` through `@TestPropertySource` in `ApiTestBase`. SMTP timeouts in `application.yaml`.
- [X] T009 Add the seven types of `specs/004-email-code-access/contracts/problem-details.md` to `<pkg>/api/ProblemType.java`:
  - **Types:** `code-resend-too-soon` 429, `code-hourly-limit` 429, `daily-email-limit` 429, `code-not-sent` 503, `invalid-code` 422, `code-expired` 422, `forbidden` 403.
  - **Exception:** a `<pkg>/api/ProblemException.java` (type, `detail` arguments, extensions, extra headers), mapped by `ProblemDetailsAdvice`.
  - **Shared builder:** the advice exposes its builder as a component (`<pkg>/api/ProblemResponses.java`), so the security handlers write the same localized Problem Details.
  - **Bundles:** `problem.<slug>.title` and `problem.<slug>.detail` in `messages.properties`, `messages_pt_BR.properties` and `messages_es_ES.properties`, with the `detail` arguments of the contract ("Detail texts").
  - **E-mail keys:** `mail.access-code.subject` and `mail.access-code.body` (`{0}` name, `{1}` code, `{2}` validity in minutes; plain text; valid for `{2}` minutes, do not share, ignore if not requested). pt-BR is the reference text.
  **Record (2026-11-13):** seven types in `ProblemType`; `ProblemException` (detail arguments, extensions, headers) handled by `ProblemDetailsAdvice`; `ProblemResponses` builds and writes the localized problems (also used by `ApiDocsAccessFilter` and the security handlers). Bundles: the 14 problem keys and the two mail keys in the three files; the `detail` of `authentication-required` is now "Sign in to access this resource." (no longer only the administrator).
- [X] T010 [P] Write the domain tests first, then the classes, in `<pkg>/domain/access/` and `<tpkg>/domain/access/` (plain Java, no Spring; the ArchUnit test of 003 guards it):
  - `EmailAddress`: "trimmed and lowercased; at most 254 characters; one `@`, a non-empty local part and a domain with a dot, no spaces" (`Ana@X.com ` equals `ana@x.com`).
  - `PersonName`: "trimmed, 2 to 120 characters".
  - `AccessCodeFormat`: normalization removes spaces and hyphens; valid only as exactly 6 digits; generation of `000000` to `999999` from an injected `RandomGenerator` (production: `SecureRandom`).
  - `SendLimits`: a pure decision from the e-mail's send times in the last hour, the latest send, the count of sends in the last 24 hours, `now`, the resend interval, the hourly limit and `daily_email_limit`. Results: `Allowed`, `TooSoon(secondsLeft)`, `HourlyLimit(retryAt = oldest of the 5 + 1 hour)` or `DailyLimit`, checked in that order.
  - `Pseudonym`: `R-%04d` of the id (`R-0001`, `R-10000`).
  **Record (2026-11-13):** `AccessRulesTest` (37 cases) and `EmailAddress`, `PersonName`, `AccessCodeFormat`, `SendLimits` (`Rules`, sealed `Decision`), `Pseudonym`, plus the `Subject` enum; numbers formatted with `Locale.ROOT`. `DomainIsolationTest` passes.
- [X] T011 [P] Create the hashing classes in `<pkg>/access/`, with unit tests in `<tpkg>/access/`:
  - `CodeHasher`: `HMAC-SHA256(key, email + ":" + code)` as 64 lowercase hexadecimal characters, plus a constant-time comparison with `MessageDigest.isEqual`.
  - `TokenGenerator`: 32 `SecureRandom` bytes, base64url without padding.
  - `TokenHasher`: SHA-256 of the token, as 64 hexadecimal characters.

  The tests cover the known vectors, that the same code gives different hashes for different e-mails, and that a hash never contains the code or the token.
  **Record (2026-11-13):** `CodeHasher`, `TokenGenerator`, `TokenHasher` and `HashingTest` (8 cases; the HMAC vector was computed with the Python `hmac` module, the SHA-256 one is the FIPS `abc` vector).
- [X] T012 Create the `JdbcClient` repositories in `<pkg>/persistence/`:
  - **`AccessCodeRepository`:** `insert` (with `name`), `latestFor(email)`, `sendTimesSince(email, since)`, `countAllSince(since)`, `incrementAttempts(id)`, `markUsed(id, now)`, `delete(id)` and `deleteCreatedBefore(instant)`.
  - **`AccessSessionRepository`:** `insert`, `findValid(tokenHash, now)`, `delete(tokenHash)` and `deleteExpired(now)`.
  - **`ParticipantRepository`:**
    - `upsertOnSignIn(email, name, language, now)`: `INSERT ... ON DUPLICATE KEY UPDATE name`; a new row gets `pseudonym = Pseudonym.of(id)` and `preferred_language = language`; an existing row keeps its pseudonym and receives the latest name.
    - `findById` and `updatePreferredLanguage`.
  - **`ConfigRepository`:** `lockForUpdate()` (`SELECT ... FOR UPDATE` on `id = 1`), `adminEmail`, `dailyEmailLimit`, `updateAdminSignIn(name, languageIfNull)`, `adminProfile` and `updateAdminPreferredLanguage`.

  Integration test `<tpkg>/persistence/AccessRepositoriesTest.java` (Testcontainers MySQL):
  - only the latest code of an e-mail is returned;
  - pseudonyms `R-0001`, `R-0002` in order;
  - two concurrent first sign-ins of the same e-mail produce one row and one pseudonym;
  - `preferred_language` is set only when `NULL`.
  **Record (2026-11-13):** the upsert is `INSERT ... AS typed ON DUPLICATE KEY UPDATE` with a placeholder pseudonym (`~` and 14 hexadecimal characters) replaced by `R-%04d` of the id in the same transaction; `latestFor` locks the row (`FOR UPDATE`) and `markUsed` answers `false` for a code already used. `AccessRepositoriesTest` (6 cases) passes, including two concurrent first sign-ins.
- [X] T013 Configure Spring Security in `<pkg>/web/SecurityConfig.java`:
  - `SecurityFilterChain` with `SessionCreationPolicy.STATELESS`.
  - CSRF, form login, HTTP Basic, logout and the request cache off. The CSRF line carries a comment citing research section 10.
  - Default security headers.
  - The rules of `specs/004-email-code-access/contracts/access.md` ("Authorization rules"), with every non-API path (root page, locale prefixes, static files) permitted so that `LocaleRoutingFilter` and `ApiDocsAccessFilter` keep their behavior of 003.
  - Roles `PARTICIPANT` and `ADMINISTRATOR`.
  **Record (2026-11-13):** `SecurityConfig` with `@ConditionalOnWebApplication` (contexts without a web server, such as `MigrationTest`, have no `HttpSecurity`); administrative paths in `SecurityConfig.ADMINISTRATION`; `UserDetailsServiceAutoConfiguration` excluded in `VereditoApplication`, so no generated password is logged. New `SecurityRulesTest` (5 cases: `401` per protected path, `403` both ways, expired session, public paths).
- [X] T014 Create `<pkg>/web/SessionCookieFilter.java`:
  - reads the cookie `veredito-session`, hashes it with `TokenHasher` and calls `AccessSessionRepository.findValid`;
  - sets a `VereditoPrincipal` (subject, participant id, pseudonym) with its role;
  - a missing, unknown or expired cookie leaves the request anonymous.

  Also create `<pkg>/web/ProblemAuthenticationEntryPoint.java` (`401 authentication-required`) and `<pkg>/web/ProblemAccessDeniedHandler.java` (`403 forbidden`), both through `ProblemResponses`. Each refusal under `/api/v1/administrator/**`, or on an operation tagged "Administration", is logged at `WARN` with the caller kind, the pseudonym (if any), the method and the path, never the e-mail or the token (research section 9).
  **Record (2026-11-13):** `SessionCookieFilter` (not a bean; added before `AnonymousAuthenticationFilter`) takes the session from `CurrentSession`, which reads the cookie once per request, ignores a malformed token and answers `503 service-unavailable` if the database fails. `VereditoPrincipal` in `access`. `DeniedAccessLog` writes, for example, `Denied 403 on administrative resource: caller=participant pseudonym=R-0003 GET /api/v1/administrator/me`.
- [X] T015 Replace `<pkg>/web/NoAdministratorSession.java` with `<pkg>/web/SecurityContextAdministratorCheck.java` (implements `AdministratorSessionCheck`; `true` only for the role `ADMINISTRATOR`); delete `NoAdministratorSession.java`
  **Record (2026-11-13):** named `SessionAdministratorCheck` instead of `SecurityContextAdministratorCheck`: `ApiDocsAccessFilter` (order `HIGHEST_PRECEDENCE + 5`) runs before the Spring Security chain (order -100), so the security context is still empty there; the check reads the cookie through `CurrentSession`, shared with the filter (research section 2 and plan updated). `NoAdministratorSession` deleted. `ApiDocsAccessTest` gains: the administrator opens Swagger in production; a participant still gets `401`.
- [X] T016 Register the relations `sign-in`, `session`, `sign-out` and `me` in `<pkg>/api/v1/LinkRelations.java`, with method and description as in `contracts/access.md` ("Relations added"). Change `<pkg>/api/v1/EntryPointController.java` to emit the links by caller of `contracts/access.md` ("Entry point"):
  - visitor: unchanged;
  - participant and administrator: `vd:session`, `vd:me` (participant or administrator address), `vd:sign-out`;
  - an invalid cookie: treated as a visitor, never `401`.

  Update the OpenAPI description of the entry point.
  **Record (2026-11-13):** four relations registered; entry point links by caller, description and a participant example updated. `EntryPointTest` gains: malformed, unknown and expired cookie as visitor (never `401`); participant and administrator links; documentation of the four relations.
- [X] T017 Extend `<tpkg>/support/ApiTestBase.java`:
  - Mailpit and `MovableClock` wiring.
  - A `signIn(name, email, locale)` helper that starts at `/api/v1`, follows `vd:request-code`, reads the code with `MailpitClient`, follows `vd:sign-in` and returns the `veredito-session` cookie.
  - `RequestSpecification`s for a visitor, a participant and the administrator (`diegoquirino@gmail.com`).
  **Record (2026-11-13):** `signIn(name, email, locale)` follows the links and reads the code from Mailpit (used from US1 on). `participantSession()` and `administratorSession()` write the session row directly, because repeated code requests for `diegoquirino@gmail.com` across test classes would hit the 60-second and hourly limits; `as(token)` adds the cookie.
- [X] T018 Run `.\mvnw.cmd -B verify` in `backend/`: every test of 003 still passes with Spring Security on (`WebRoutingTest`, `ApiDocsAccessTest`, `EntryPointTest`, `LocalizedProblemDetailsTest`, `ApiDocumentationCompletenessTest`, `MigrationTest`, `DatabaseUnavailableTest`)
  **Record (2026-11-13):** `mvnw -B verify` BUILD SUCCESS, 131 tests, none failed: every test of 003 (`WebRoutingTest` 12, `ApiDocsAccessTest` 12 with the two new production cases, `EntryPointTest` 22 with the new caller cases, `LocalizedProblemDetailsTest` 6, `ApiDocumentationCompletenessTest`, `MigrationTest` 15, `DatabaseUnavailableTest`, `ApiIndexTest` 3, `MessageBundleParityTest`, `DomainIsolationTest` 2) plus `AccessRulesTest` 37, `HashingTest` 8, `AccessRepositoriesTest` 6 and `SecurityRulesTest` 5. Front end: `npm test` (31 tests), `i18n:check` and `npm run build` pass.
- [X] T019 [P] Create the front-end core, with Vitest specs:
  - **`<app>/core/hal-forms.ts`:** reads `_templates.default` (method, properties, inline options).
  - **`<app>/core/problem-messages.ts`:** maps each Problem Details `type` of `contracts/web.md` ("Messages on screen") to a `$localize` text filled with the extension values (`retryAfterSeconds`, minutes until `retryAt`, `attemptsLeft`).
  - **`<app>/core/session.service.ts`:**
    - signals `session` and `subject` (`visitor`, `participant`, `administrator`);
    - `refresh()` follows `vd:session` from the entry point when present;
    - `signIn` and `signOut` follow the links;
    - no URL is built except `/api/v1`.

  Add the XLIFF units for the problem messages.
  **Record (2026-11-13):** `hal-forms.ts`, `problem-messages.ts` (also `fieldMessage` for `invalid-request`) and `session.service.ts`, with 3 spec files; Vitest 31 tests in 10 files pass. 9 units in `messages.pt-BR.xlf` and `messages.es-ES.xlf`; `i18n:check` passes (16 units), the 9 new ones reported as "not in the built application" until the screens of US1 use them.

**Checkpoint**: foundation ready; the API of 003 behaves as before, now behind Spring Security

---

## Phase 3: User Story 1 - Sign in with the code sent by e-mail (Priority: P1) MVP

**Goal**: a person types name and e-mail, receives the code in Mailpit and signs in; first sign-in creates the participant with a pseudonym

**Independent Test**: in local staging, sign in with a new e-mail; try a wrong, expired and reused code (quickstart sections 4 and 5)

### Tests for User Story 1

> Write these tests first and see them fail

- [X] T020 [P] [US1] Create `<tpkg>/api/v1/AccessTest.java` (REST Assured from `/api/v1`, Mailpit, `MovableClock`):
  - **Request (scenario 1):** a valid request answers `200` with `email`, `validityMinutes` 10, `resendAfterSeconds` 60, `vd:sign-in`, and exactly one e-mail.
  - **Invalid fields (scenario 2):** name "A" or e-mail "ana@" answer `400 invalid-request` with `errors` (`name`/`size`, `email`/`email`), and no e-mail is sent.
  - **Right code (scenario 3):** typed with spaces and hyphens, it answers `201` with `Location: /api/v1/access-sessions/current` and `Set-Cookie veredito-session` with `HttpOnly`, `Secure`, `SameSite=Lax`, `Path=/`, `Max-Age=86400`.
  - **Wrong code (scenario 4):** four wrong codes give `invalid-code` with `attemptsLeft` 4, 3, 2, 1; the fifth gives `code-expired`; the right code then fails.
  - **Dead code (scenario 5):** after the clock moves 10 minutes, `code-expired`; a reused code, `code-expired`.
  - **Too soon (scenario 6):** a second request within 60 s gives `429 code-resend-too-soon` with `retryAfterSeconds` and `Retry-After`.
  - **Hourly limit (scenario 7):** the sixth request in an hour (clock moved 60 s between requests) gives `429 code-hourly-limit` with `retryAt`.
  - **Same participant (scenario 8):** `Ana@Example.com ` and `ana@example.com` are one participant.
  - **Daily limit (scenario 9):** with `config.daily_email_limit = 1`, a second e-mail gets `429 daily-email-limit`.
  - **Pseudonyms (scenario 10):** first sign-ins get `R-0001` and `R-0002`.
  - **Latest name (scenario 11):** the latest name is kept.
  - **Two devices:** only the latest of two codes works.
  - **Send failure:** a nested class with `spring.mail.port` pointing to a closed port answers `503 code-not-sent`, with no new `access_code` row.
  - **No registration leak:** the wrong-code and dead-code answers are identical for a registered and an unknown e-mail.
  **Record (2026-11-13):** `AccessTest` has 22 cases: the scenarios above, the e-mail language in pt-BR, en-US and es-ES, a code of the wrong shape, and a missing e-mail or code. The tests share one database, so they use unique `example.com` e-mails, compute the daily limit from the rows of the last 24 hours (restored in `finally`) and check consecutive pseudonyms instead of literal `R-0001`/`R-0002`. The send failure uses `support/SwitchableMailSender` (a `BeanPostProcessor` wraps the `JavaMailSender`, `failing(true)` throws `MailSendException`) instead of a nested class with a closed port, which would start a second context and a second MySQL container. `MovableClock` follows the system clock, so the `Retry-After` bounds come from clock readings around the request, not from fixed numbers (a slow SMTP run once gave 3249 s against a fixed 3290-3301).
- [X] T021 [P] [US1] Extend `<tpkg>/api/LocalizedProblemDetailsTest.java` with `code-resend-too-soon`, `code-hourly-limit`, `daily-email-limit`, `code-not-sent`, `invalid-code` and `code-expired`, each with `Accept-Language` `pt-BR`, `en-US`, `es-ES` and without the header (title, detail, `Content-Language`, unchanged `type`)
  **Record (2026-11-13):** `accessProblemsAreLocalized` covers the 6 types in the 4 language cases (24 cases); titles and details are compared with the bundles through `support/Bundles`, with the extension values filled in.

### Implementation for User Story 1

- [X] T022 [US1] Create `<pkg>/access/AccessCodeMailer.java`: a `SimpleMailMessage` from `mail.access-code.subject` and `mail.access-code.body` in the request locale, from `Veredito <veredito.access.mail-from>` to the normalized e-mail; a `MailException` is rethrown as `code-not-sent`
  **Record (2026-11-13):** body arguments are the name, the code and the validity in minutes. On a `MailException` only the exception class is logged, since the message can carry the address.
- [X] T023 [US1] Create `<pkg>/access/AccessCodeService.java` with `request(name, email, locale)`:
  1. Validate with `PersonName` and `EmailAddress`; a failure answers `invalid-request` with the field codes of `contracts/problem-details.md`.
  2. In one transaction: `ConfigRepository.lockForUpdate()`, decide with `SendLimits` (any refusal becomes its `ProblemException`), generate the code, and insert the row with the normalized name, `CodeHasher` and `expires_at = now + 10 minutes`.
  3. After the commit, send with `AccessCodeMailer`. On failure, delete the row and answer `code-not-sent` (research sections 5 and 6).
  **Record (2026-11-13):** the code comes from `SecureRandom`; the transaction uses `TransactionTemplate`. Refusals: `code-resend-too-soon` with `retryAfterSeconds`, `code-hourly-limit` with `retryAt` (rounded up to the second), both with `Retry-After`; `daily-email-limit`. `ProblemException.invalidFields` builds the `errors` list.
- [X] T024 [US1] Create `<pkg>/access/SignInService.java` with `signIn(email, code, locale)`:
  1. Normalize the e-mail and the code; read `latestFor(email)`.
  2. A dead or missing code answers `code-expired`.
  3. A mismatch, or a code of the wrong shape, increments `attempts` in a transaction that commits even though the call fails: `invalid-code` with `attemptsLeft`, or `code-expired` at the fifth.
  4. A match, in one transaction:
     - `markUsed`;
     - the administrator (`config.admin_email`) gets `updateAdminSignIn(name, language)` with `access_code.name`, and no participant row;
     - a participant gets `upsertOnSignIn(email, name, language, now)`, where `name` is `access_code.name` of the code just used;
     - then a session with `TokenGenerator` and `TokenHasher`, `expires_at = now + 24 hours`.
  5. Return the session view and the raw token.
  **Record (2026-11-13):** the outcome (success, wrong code, dead code) is decided in a transaction that commits; the failure is thrown afterwards. `SessionView` carries the subject, name, pseudonym, language and expiry; `current(principal)` serves `GET current`. `ParticipantRepository.upsertOnSignIn` now updates an existing participant by id before trying the insert: InnoDB spends an auto-increment value on every `INSERT ... ON DUPLICATE KEY UPDATE`, which left gaps in the pseudonyms of returning people (`AccessRepositoriesTest.aReturningPersonLeavesNoGapInThePseudonyms`).
- [X] T025 [US1] Create `<pkg>/api/v1/AccessCodeController.java` (`POST /api/{version}/access-codes`) and `<pkg>/api/v1/AccessSessionController.java`:
  - **`POST /api/{version}/access-sessions`:** `201`, with the `ResponseCookie` of research section 3 and `Location`.
  - **`GET /api/{version}/access-sessions/current`:** the session model of `contracts/access.md`.

  Request and response models go in `<pkg>/api/v1/`. Every operation gets `summary`, `description`, examples and every error response of the contract, so `ApiDocumentationCompletenessTest` passes.
  **Record (2026-11-13):** the controllers map `/api/v1/...` with `version = "v1"`, as the existing controllers do. Models: `AccessCodeRequest`, `SignInRequest`, `AccessCodeModel`, `SessionModel`. Links: `self`, `vd:sign-in` and `vd:request-code` on the code; `self`, `vd:me` and `vd:sign-out` on the session. `RelationController` describes the new relations. The version default moved to `0.4.0+dev` in `SystemVersion`, `Dockerfile` and `write-build-info.mjs` (missed in Phase 1).
- [X] T026 [US1] Extend `<tpkg>/api/v1/EntryPointTest.java`: with a participant session, the entry point offers `vd:session`, `vd:me` (`/api/v1/participants/me`) and `vd:sign-out`, and no administrative link; with an unknown cookie, the visitor links
  **Record (2026-11-13):** mostly covered in Phase 2 (T016). New cases: an empty `request-code` answers the field errors, and a participant signed in with a real code gets only the participant links.
- [X] T027 [P] [US1] Replace `<app>/participate/participate.component.ts` with the sign-in form `<app>/participate/sign-in.component.ts` (and `.html`, `.scss`, `.spec.ts`):
  - **Fields:** Material name and e-mail fields with the rules of the data model ("Name: trimmed, 2 to 120 characters"; e-mail as `EmailAddress`). The field with a problem is marked.
  - **Submit:** "Send code" follows `vd:request-code` and maps Problem Details through `problem-messages`.
  - **On `200`:** keeps name and e-mail in `sessionStorage` (`veredito-pending-code`) and opens `/participate/code`.

  Add the XLIFF units.
  **Record (2026-11-13):** `participate/pending-code.ts` keeps name, typed and normalized e-mail, validity, `resendAt` and the `vd:sign-in` href, so the code screen survives a reload without rebuilding URLs (`SessionService.requestCode` now returns `signInHref`). Validators in `participate/access-validators.ts`. `problemOf` accepts only bodies with a Veredito `type`; other failures show "Something went wrong; please try again". 5 Vitest cases; the old placeholder was removed.
- [X] T028 [P] [US1] Create `<app>/participate/code.component.ts` (and `.html`, `.scss`, `.spec.ts`):
  - **Screen:** shows the e-mail, "valid for 10 minutes", the spam warning, and a 6-digit code field.
  - **Sign in:** follows `vd:sign-in`. `invalid-code` shows the attempts left; `code-expired` shows "Send a new code".
  - **"Resend":** disabled with a countdown from `resendAfterSeconds` or `retryAfterSeconds`.
  - **"Correct e-mail":** goes back to `/participate` with the fields filled.
  - **On `201`:** `session.refresh()`, then `/welcome`.

  Add the XLIFF units.
  **Record (2026-11-13):** the code field also checks the 6-digit shape on the client (spaces and hyphens ignored), so a mistyped code does not spend an attempt. The countdown ticks every second from `resendAt`; "Resend" sends the same name and typed e-mail again. 8 Vitest cases.
- [X] T029 [US1] Create `<app>/welcome/welcome.component.ts` with "Thank you for identifying yourself; the next step will be available soon" (US2 scenario 2); add the routes `participate`, `participate/code` and `welcome` to `<app>/app.routes.ts`; add the XLIFF units; `npm run i18n:check` passes
  **Record (2026-11-13):** the three routes are lazy (`loadComponent`): with them eager the initial bundle reached 531 kB, over the 500 kB budget; now 416 kB. `i18n:check` passes with 41 units in pt-BR and es-ES (es-ES in "usted"); Vitest 46 tests in 13 files pass.
- [X] T030 [US1] Create `frontend/e2e/support/mailpit.ts` (latest message to an address through `http://localhost:8025/api/v1/search`, subject, code) and `frontend/e2e/sign-in.spec.ts`: in `/pt-BR/`, sign in as a new `example.com` e-mail with the code from Mailpit; lands on `/pt-BR/welcome`; the e-mail subject is the pt-BR one
  **Record (2026-11-13):** `sign-in.spec.ts` also checks the cookie flags and a wrong code ("Código incorreto; restam 4 tentativas"). `locales.spec.ts` now checks the "Nombre" field instead of the removed placeholder link. Against local staging (port 8200, Mailpit 8025): 32 passed, 3 skipped, in desktop and phone; one `page.goto` abort in `staging.spec.ts` did not repeat in 3 further runs of `staging` and `home` (57 passed). Back end: `mvnw -B verify` BUILD SUCCESS, 179 tests, none failed (`AccessTest` 22, `LocalizedProblemDetailsTest` 30, `EntryPointTest` 23, `AccessRepositoriesTest` 7).

**Checkpoint**: sign-in works end to end in pt-BR; codes, limits and pseudonyms behave as in the spec

---

## Phase 4: User Story 2 - Go to the right area after signing in (Priority: P1)

**Goal**: the administrator reaches the administrative area; participants reach the provisional screen; nobody else reaches administrative functions

**Independent Test**: sign in with `diegoquirino@gmail.com` and with another e-mail; call an administrative function without a session and with a participant session (quickstart sections 4 and 8)

### Tests for User Story 2

- [X] T031 [P] [US2] Create `<tpkg>/api/v1/AuthorizationTest.java`:
  - **Enumeration:** reads `/v3/api-docs/v1` and takes every operation tagged "Administration".
  - **Denials:** for each one, no session answers `401 authentication-required`, and a participant session answers `403 forbidden`.
  - **Administrator:** gets neither `401` nor `403`.
  - **Participant-only operations:** with the administrator session, they answer `403` (US2 scenario 4).
  - **Report and logs:** prints how many operations were checked (SC-003). With `OutputCaptureExtension`, each refusal logs one `WARN` line with the caller kind, the pseudonym, the method and the path, and no e-mail or cookie value (US2 scenario 3).
  **Record (2026-11-13):** the operations come from `/v3/api-docs/v1` by tag; path variables become `1`, and `PUT`/`POST`/`PATCH` send `{}`. Today it checks 1 "Administration" operation (`GET /api/v1/administrator/me`) and 1 "Participation" operation (`GET /api/v1/participants/me`); later specs are checked as soon as they tag their operations. The participant's pseudonym for the log line is read from `vd:session`.
- [X] T032 [P] [US2] Create `<tpkg>/api/v1/AdministratorAccessTest.java`:
  - **Sign-in:** signing in with `diegoquirino@gmail.com` gives a session with `subject` `administrator` and no `pseudonym`.
  - **Data:** no new `participant` row; `config.admin_name` is the typed name; `config.admin_preferred_language` is set on the first sign-in only.
  - **Profile:** `GET /api/v1/administrator/me` gives the contract body.
  - **Entry point:** offers `vd:me` to the administrator address.

  Extend `<tpkg>/web/ApiDocsAccessTest.java` (`Production`): with the administrator session, `/swagger-ui.html` and `/v3/api-docs/v1` open; with a participant session, `401`.
  **Record (2026-11-13):** `AdministratorAccessTest` signs the administrator in twice with real codes (` DiegoQuirino@Gmail.com ` in es-ES, then the plain address in en-US 61 s later): no `participant` row, the latest name kept, the language of the first sign-in kept, and `vd:me` from the entry point gives the contract body. The `config` columns are cleared before and restored after. A second case reads the participant's own profile through `vd:me`. The `ApiDocsAccessTest` cases were already added in Phase 2.

### Implementation for User Story 2

- [X] T033 [US2] Create `<pkg>/api/v1/ParticipantMeController.java` (`GET /api/{version}/participants/me`, tag "Participation") and `<pkg>/api/v1/AdministratorMeController.java` (`GET /api/{version}/administrator/me`, tag "Administration"), with the representation models of `contracts/access.md` and complete documentation (examples, `401`, `403`)
  **Record (2026-11-13):** models `ParticipantProfileModel` (pseudonym, name, language, `registeredAt`) and `AdministratorProfileModel` (name, e-mail, language), each with `self`; `access/ProfileService` reads them. The `_templates` for `PUT` come with T042. The authorization rules, the `403` handler and the `WARN` log were already in place from Phase 2. `mvnw -B verify` BUILD SUCCESS, 183 tests, none failed.
- [X] T034 [P] [US2] Create `<app>/core/guards.ts` (`visitorOnly`, `participantOnly`, `administratorOnly`, reading `session.service`; the redirects of `contracts/web.md` "Guards") and an HTTP interceptor that sends a `401` from any call to `/participate`; Vitest specs
  **Record (2026-11-13):** `areaOf(subject)` maps visitor, participant and administrator to `/participate`, `/welcome` and `/admin`. `SessionService.current()` reads the session once per page load (later navigations use the signal); `clear()` forgets it. `core/unauthorized.interceptor.ts` clears the session and opens `/participate` on a `401` from `/api/**`. Specs: `guards.spec.ts` (7), `unauthorized.interceptor.spec.ts` (2), one more case in `session.service.spec.ts`.
- [X] T035 [P] [US2] Create `<app>/admin/admin.component.ts` (and `.html`, `.scss`, `.spec.ts`): "Administration" title, the administrator's name, and a Material list with Import, Export, Dashboard and Participants, all disabled (US2 scenario 1); add the XLIFF units
  **Record (2026-11-13):** a `mat-action-list` (role `group`, labeled "Administrative functions") with four disabled buttons, "Signed in as {name}" and a note that the functions come in the next releases. 8 XLIFF units in pt-BR and es-ES; "Dashboard" follows the glossary ("Painel de andamento", "Panel de progreso").
- [X] T036 [US2] Wire the areas:
  - **Routes:** `/admin` (`administratorOnly`), `/welcome` (`participantOnly`), `/participate` and `/participate/code` (`visitorOnly`).
  - **After sign-in:** the code screen opens `/admin` for the administrator and `/welcome` for a participant.
  - **Home page:** the "Participate" button goes to the person's area when signed in.
  **Record (2026-11-13):** `/admin` is lazy like the other areas (initial bundle 419.6 kB). The code screen now uses the session returned by `vd:sign-in` instead of reading it again with `refresh()`; the answer carries the same links, and a failed reread no longer sends the person back to the form. The interceptor joins `acceptLanguageInterceptor` in `app.config.ts`. Vitest: 59 tests in 16 files; `i18n:check` 49 units; `npm run build` passes.
- [X] T037 [US2] Add `frontend/e2e/admin.spec.ts`:
  - signing in as `diegoquirino@gmail.com` (code from Mailpit) opens `/pt-BR/admin` with the four menu items disabled;
  - a participant who opens `/pt-BR/admin` lands on `/pt-BR/welcome`.
  **Record (2026-11-13):** a third case sends a visitor from `/pt-BR/admin` and `/pt-BR/welcome` to `/pt-BR/participate`; the administrator also goes back to `/pt-BR/admin` from `/pt-BR/participate`. The administrator case runs only in the desktop project: both projects run in parallel and the single address accepts one code per 60 s. `latestMessageTo` takes the id of the newest message seen before the request, so an older code to the same address is never used. Against local staging, the full run gave 37 passed and 4 skipped; the administrator case failed only on the menu locator (a `mat-action-list` has role `group`, not `list`), and after the fix `admin.spec.ts` gave 5 passed, 1 skipped.

**Checkpoint**: the two areas are separated in the API and in the screens; Swagger opens in production for the administrator

---

## Phase 5: User Story 4 - Choose the language before signing in (Priority: P1)

**Goal**: language choice on `/`, remembered by the cookie and the profile, switchable from the header without losing typed values; e-mails and messages in the language in use

**Independent Test**: quickstart sections 3 and 7, and the sign-in of section 4 in the three locales

### Tests for User Story 4

- [X] T038 [P] [US4] Extend `<tpkg>/web/WebRoutingTest.java`:
  - `GET /` with `veredito-lang=es-ES` answers `302` to `/es-ES/`, with `Cache-Control: no-cache` and `Vary: Cookie`;
  - with `veredito-lang=xx-XX` or no cookie, it answers `200` with the choice page and `Vary: Cookie`.

  **Record (2026-11-13):** three tests: `rootWithASavedLanguageGoesStraightToItsPrefix`, `rootIgnoresAnUnsupportedSavedLanguage` (`xx-XX`, `es-es` and `pt` are not exact supported tags) and `rootWithoutASavedLanguageVariesByCookie`.
- [X] T039 [P] [US4] Create `<tpkg>/api/v1/LanguageProfileTest.java`:
  - **First sign-in:** with `Accept-Language: es-ES`, it stores `preferred_language = es-ES`.
  - **Profile wins:** a later sign-in with `en-US` keeps `es-ES`, and the session representation answers `preferredLanguage` `es-ES` (FR-012).
  - **E-mail language:** the access-code e-mail uses the request language even when the profile differs (edge case).
  - **`PUT /api/v1/participants/me`:** changes the language; a value outside the three gives `400 invalid-request` (`preferredLanguage`/`locale`).
  - **`PUT /api/v1/administrator/me`:** stores `config.admin_preferred_language`.
  - **Templates:** both `GET`s carry `_templates.default` with method `PUT` and the three inline options.

  **Record (2026-11-13):** six tests over Testcontainers MySQL and Mailpit with `MovableClock`. Invalid values `fr-FR`, `es-es` and `en` give `locale`; `{}` gives `required`. The administrator's language is restored in `finally`. The template also checks `required`, `minItems` and `maxItems` = 1. Spring HATEOAS 3 names a template after its input type (`putPreferredLanguageRequest`), so both affordances use `withName("default")`.
- [X] T040 [P] [US4] Add to `<tpkg>/api/v1/AccessTest.java`: the access-code e-mail subject and body arrive in pt-BR, en-US and es-ES according to `Accept-Language` (FR-014, SC-005 back-end part)

  **Record (2026-11-13):** already covered by `AccessTest.theEmailIsWrittenInTheLanguageOfTheRequest` (Phase 3, parameterized over the three locales: subject and body from each bundle); nothing added.

### Implementation for User Story 4

- [X] T041 [US4] Change `<pkg>/web/LocaleRoutingFilter.java`: for `GET /`, a cookie `veredito-lang` with a supported value answers `302` to `/<locale>/`; every answer on `/` carries `Vary: Cookie` (contracts/web.md "Root page")

  **Record (2026-11-13):** the supported prefixes come from `LocaleConfig.SUPPORTED_TAGS` (new); only an exact tag redirects, with `Cache-Control: no-cache`; otherwise `/` forwards to the choice page as before.
- [X] T042 [US4] Add `PUT` to `ParticipantMeController` and `AdministratorMeController` (body `{ "preferredLanguage": ... }`, values `pt-BR`, `en-US`, `es-ES`), and the HAL-FORMS `_templates.default` affordance on both `GET` representations; complete documentation

  **Record (2026-11-13):** body `PreferredLanguageRequest` (`@NotNull`, so the HAL-FORMS property is `required`); validation in `ProfileService` (`required`/`locale`). The inline options come from a `HalFormsConfiguration` bean in `HypermediaConfig`. Each operation documents 200 (example with `_templates`), 400, 401 and 403; `ApiDocumentationCompletenessTest` and `AuthorizationTest` pass.
- [X] T043 [P] [US4] Create `backend/src/main/resources/static/language.js` and reference it from `static/index.html`; adjust `static/language.css` for the selected option:
  - **Preselection:** the first entry of `navigator.languages` whose language is `pt`, `en` or `es`; otherwise pt-BR.
  - **Confirm:** sets `veredito-lang=<locale>; Path=/; Max-Age=31536000; SameSite=Lax` and opens the prefix.
  - **Without JavaScript:** the three options stay plain links.

  **Record (2026-11-13):** the preselected link gets `aria-current="true"` and the focus; `language.css` marks it with a darker background, bold text and an outline (no symbol, so the accessible name stays the language name). The cookie is set on the click of any `a[hreflang]`, and the link then opens the prefix.
- [X] T044 [P] [US4] Create `<app>/core/language.service.ts`, with a Vitest spec, implementing `contracts/web.md` ("Language cookie in the Angular builds", "Language switch"):
  - **Cookie:** `setCookie(locale)` writes `veredito-lang`, and it runs at startup with the build `LOCALE_ID` (`provideAppInitializer` in `<app>/app.config.ts`).
  - **Switch:** `switchTo(locale)` does `PUT` on `vd:me` when signed in, then sets the cookie and loads the same route under the new prefix.
  - **Unsaved input:** `saveUnsaved(route, values)` and `takeUnsaved(route)` over `sessionStorage` key `veredito-unsaved:<route>`.

  **Record (2026-11-13):** `LOCALES` moved here from the shell. The page load goes through the `LOAD_PAGE` token, so specs replace it. A failed `PUT` does not stop the switch. A form registers its values with `keepOnSwitch(route, values)` in its constructor; the entry is dropped when the form is destroyed, and `takeUnsaved` removes the entry it reads. Vitest: 7 tests.
- [X] T045 [US4] Change the shell (`<app>/shell/shell.component.html`, `.ts`, `.scss`, `.spec.ts`):
  - a Material menu in the header with the three languages, each labeled in its own language, the current one marked, calling `language.switchTo`;
  - the footer keeps the version and loses the language links.

  Update the XLIFF units.

  **Record (2026-11-13):** the header button shows the current language and is named "Language: <language>" (`shell.language`, built with `$localize`). Each menu option has `lang`; the current one has `aria-current="true"` and a check mark hidden from screen readers. Choosing the current language does nothing. The unit `shell.otherLanguages` was replaced by `shell.language` in pt-BR and es-ES. The footer test of `e2e/locales.spec.ts` moved to `e2e/language.spec.ts` (header switch).
- [X] T046 [US4] Use the language service in the screens:
  - **Sign-in form:** saves name and e-mail before a switch and restores them after the reload (`veredito-unsaved:/participate`).
  - **Code screen, after sign-in:** if `preferredLanguage` differs from `LOCALE_ID`, sets the cookie and opens `/<profile locale>/welcome` or `/<profile locale>/admin` with a reload (FR-012, US4 scenario 7).

  **Record (2026-11-13):** the restored values override the pending code; the code screen keeps its state across a switch through the pending code in `sessionStorage`. A profile language outside the three is ignored. Vitest: three new tests (sign-in restore; code screen redirect to `/es-ES/welcome` and no redirect when the profile has no language).
- [X] T047 [US4] Add `frontend/e2e/language.spec.ts`:
  - **Preselection:** Playwright contexts with `locale` `pt-BR`, `es-MX`, `en-GB` and `fr-FR` (expect "Português (Brasil)", "Español (España)", "English (US)", "Português (Brasil)").
  - **Cookie:** confirming sets the cookie and `/` redirects afterwards; an invalid cookie shows the choice page; a direct `/es-ES/` saves `es-ES`.
  - **Sign-in in each locale:** e-mail subject in that locale and no text from another locale on the screens (SC-005).
  - **Profile wins (SC-006):** a participant with profile `es-ES` signs in from `/en-US/` and ends on `/es-ES/welcome`.
  - **Header switch:** on the sign-in form it keeps the typed values; when signed in it updates the profile.

  **Record (2026-11-13):** 12 tests per project (desktop and phone), all passing against local staging. Other-locale text is checked on the welcome screen. The profile test signs in on `/pt-BR/`, switches to es-ES from the header, clears the cookies, waits until 61 s after the first code request was accepted (one code per minute) and signs in from `/en-US/`: the e-mail comes in en-US and the screen opens on `/es-ES/welcome`. Full e2e suite: 60 passed, 4 skipped by design (no-scroll checks on phone only, administrator on desktop only).

**Checkpoint**: the three locales work from the first screen to the area after sign-in, with e-mails in the language in use

---

## Phase 6: User Story 3 - Stay signed in and sign out (Priority: P2)

**Goal**: the session lasts up to 24 hours on the same browser and ends on "Sign out"

**Independent Test**: sign in, close and reopen the browser; sign out and try to come back (quickstart section 6)

### Tests for User Story 3

- [X] T048 [P] [US3] Create `<tpkg>/api/v1/SessionTest.java`:
  - **Within 24 hours:** the cookie gives `200` on `vd:session`, and no e-mail is sent (SC-004).
  - **After 24 hours:** with the clock moved 24 hours, `401`, and the entry point shows the visitor links.
  - **Sign-out:** `DELETE` on `vd:sign-out` answers `204` with `Set-Cookie veredito-session=; Max-Age=0`; the row is gone; the same cookie then gives `401`.
  - **Hash only:** `access_session.token_hash` never equals the cookie value.

  **Record (2026-11-13):** five tests. The 24-hour test signs in with a real code and moves the clock 23 h 59 min. Sign-out is checked for a participant and for the administrator, including all cookie attributes and a second `DELETE` giving `401`. A visitor's `DELETE` gives `401`.
- [X] T049 [P] [US3] Create `<tpkg>/access/CleanupJobTest.java` (Testcontainers, `MovableClock`): rows of `access_code` older than 24 hours and expired rows of `access_session` are deleted; newer rows stay; running twice is harmless

  **Record (2026-11-13):** two tests. The first checks the limits around "now"; a second run removes nothing. The second moves the clock 23 h (rows stay), then 1 h 1 s more (rows go).

### Implementation for User Story 3

- [X] T050 [US3] Add `DELETE /api/{version}/access-sessions/current` to `AccessSessionController` (deletes the row, clears the cookie with the attributes of research section 3), with complete documentation

  **Record (2026-11-13):** `SignInService.signOut` deletes the row by the principal's token hash. The sign-in and the sign-out build the cookie through the same `sessionCookie(token, maxAge)` method. The `204` response documents the `Set-Cookie` header and no body; `ApiDocumentationCompletenessTest` now requires a `204` to document no content instead of an example.
- [X] T051 [US3] Create `<pkg>/access/CleanupJob.java` (`@Scheduled(cron = "0 0 * * * *")`, deletes as in T049) and enable scheduling in `<pkg>/config/SchedulingConfig.java`

  **Record (2026-11-13):** `run()` returns the counts and logs them (no personal data). The scheduled entry point logs a WARN and skips the run when the database is unavailable. The 24-hour window reuses `SendLimits.DAY`.
- [X] T052 [US3] Show the name and "Sign out" in the header when signed in (`<app>/shell/`); "Sign out" follows `vd:sign-out`, clears the session signal and opens the home page (US3 scenario 3); update the spec and the XLIFF units

  **Record (2026-11-13):** the header reads the session signal, which the home page and the guards load. Below 600 px the name is hidden and the language button shows the code (`pt-BR`), so the phone layout does not scroll sideways. New unit `shell.signOut` ("Sair", "Cerrar sesión"). Vitest: three new shell tests.
- [X] T053 [US3] Add `frontend/e2e/session.spec.ts`:
  - a new page with the same storage state stays signed in, and the Mailpit message count is unchanged;
  - "Sign out" goes to the home page, after which `/pt-BR/welcome` leads to `/pt-BR/participate`.

  **Record (2026-11-13):** the reopened browser is a new context with the storage state of the first one and the same device options. The test also checks that there is no horizontal scrolling when signed in and that the session cookie is gone after "Sign out". Full e2e suite against local staging: 62 passed, 4 skipped by design. Backend `mvnw verify`: 201 tests.

**Checkpoint**: all stories work in local staging

---

## Phase 7: Staging rehearsal and deployment

**Purpose**: rehearse in local staging, prepare the secret and deploy (constitution: staging before production)

- [X] T054 If Spring Boot 4.2.0 is GA by now (due on 2026-11-19), move `backend/pom.xml` to the latest 4.2.x, run `.\mvnw.cmd -B verify` and record the versions in `specs/004-email-code-access/research.md` section 1; otherwise record that 4.1.2 stays

  **Record (2026-11-13):** Maven Central lists 4.2.0-RC2 as the latest release; 4.1.2 is the latest 4.1.x and stays. Recorded in research section 1.
- [X] T055 Run quickstart sections 1 to 9 in local staging and record the results in this task, including each failure case of section 5 and the `WARN` line of section 8 (without e-mail or token)

  **Record (2026-11-13):** local staging on port 8200 (`APP_PORT`), rebuilt with `up --build -d`. The `down -v` of section 2 was not run: deleting the volumes is blocked, so the database keeps earlier test rows and the pseudonyms are not `R-0001`/`R-0002`.
  - **Sections 1 and 9:** `mvnw verify`, `i18n:check`, `npm test`, `npm run build` and the full Playwright suite pass (Phase 6 record).
  - **Sections 3 to 8:** run through the API by `scripts/_veredito_quickstart_004.py` (prints pass/fail and statuses only). 28 checks:
    - **Section 3:** `/` gives the choice page with `Vary: Cookie`; `/` with cookie `en-US` gives `302` to `/en-US/`; cookie `xx-XX` gives the choice page. Preselection by browser language is covered by `e2e/language.spec.ts`.
    - **Section 4:**
      - name "A" and e-mail "ana@" give `400` with `name/size` and `email/email`;
      - `  Qs-Ana-...@Example.com  ` gives `validityMinutes` 10, the normalized e-mail and one e-mail in Portuguese;
      - the code typed as `123 456` signs in, with no new e-mail, and the row has the pseudonym, `Ana Souza` and `pt-BR`;
      - a sign-in in es-ES gives a Spanish e-mail and profile `es-ES`; after sign-out and 61 s, a sign-in from en-US gets the e-mail in en-US and `preferredLanguage` `es-ES` (profile wins);
      - the administrator gets an administrator session with no new participant row, and `config.admin_name` is set.
    - **Section 5:**
      - five wrong codes give `attemptsLeft` 4, 3, 2, 1, then `code-expired`;
      - an immediate resend gives `429 code-resend-too-soon` (60 s);
      - five codes 61 s apart, then a sixth, give `429 code-hourly-limit`;
      - with `daily_email_limit = 1`, `429 daily-email-limit` (the value 450 was restored);
      - with Mailpit stopped, `503 code-not-sent` and no `access_code` row (Mailpit restarted).
      - **Two tabs:** this check failed at first: the replaced code got `422 invalid-code` and used up an attempt, against `contracts/access.md` ("Dead code ... replaced by a newer code"). `SignInService` now answers `code-expired` for a code that a newer one replaced, without counting an attempt (`AccessCodeRepository.replacedHashes`). `AccessTest.onlyTheLatestOfTwoCodesWorks` now expects `code-expired` and zero attempts. The check passes after the rebuild.
    - **Section 6:** the cookie has `Path=/`, `Max-Age=86400`, `Secure`, `HttpOnly`, `SameSite=Lax`; `access_session` holds the SHA-256 of the token; sign-out gives `204`, `Max-Age=0`, then `401`.
    - **Section 7:** the participant's `PUT` stores `es-ES`; the administrator's `PUT` stores `en-US` (restored); `400` problems come with `Content-Language` and title in each locale ("Requisição inválida", "Invalid request", "Solicitud no válida").
    - **Section 8:** the visitor gets `401 authentication-required` and the participant `403 forbidden`. The log line is `DeniedAccessLog : Denied 403 on administrative resource: caller=participant pseudonym=R-0083 GET /api/v1/administrator/me`, with no e-mail or token.
- [X] T056 [P] Update `.github/workflows/deploy.yml` (`--set-secrets` gains `CODE_HASH_KEY=veredito-code-key:latest`, as in `contracts/pipeline.md`) and `docs/cloud-setup.md`:
  - **Step 5:** create `veredito-code-key` with `New-SecretFromValue` and `New-RandomPassword`.
  - **Step 6:** `secretAccessor` for `veredito-run`.
  - **New note:** the secret must exist before the first deployment of 004.

  **Record (2026-11-13):** step 5 gains "Access-code key (spec 004)", with the note and the check of the three secrets. Step 6 binds the three secrets and adds the single command for a project set up before 004.
- [X] T057 The researcher creates the secret `veredito-code-key` and its binding with the commands of `docs/cloud-setup.md`. The agent never sees, prints or logs the value; it only checks that version 1 exists and that `veredito-run` has `secretAccessor`
  **Record (2026-11-13):** created by the researcher from the terminal (value generated by `New-RandomPassword` and sent through a temporary file, never displayed). Checked by metadata only: version 1 `enabled`; the only binding is `roles/secretmanager.secretAccessor` for `veredito-run`.
- [X] T058 Push to `Veredito`; wait for `ci.yml` to pass (all five jobs); deploy with `git push origin <commit>:refs/tags/deploy/0.4.0-1`; record the deploy run, the revision, the time and the footer version
  **Record (2026-11-13):** `ci.yml` run 43543220559 green on `31fdc63` (backend, frontend, secrets, image, e2e). Tag `deploy/0.4.0-1` on `31fdc63`; deploy run [43543908176](https://github.com/diegoquirino/openscience/actions/runs/43543908176) succeeded (started 23:01:03 UTC-3, about 1 minute). Revision `veredito-00005-ds2` (created 23:01:26 UTC-3) is ready and takes 100% of the traffic, which also removes the earlier pin to `veredito-00003-dgv`. The production bundle carries the footer version `0.4.0+31fdc63`.
- [X] T059 Run quickstart section 10 in production and record:
  - the time for the administrator's code to arrive in Gmail (SC-002);
  - Swagger with the administrator session, and `401` in a private window;
  - the request time of `POST /api/v1/access-codes` over five sends (Principle V; research section 6);
  - a colleague's sign-in time (SC-001), if available.

  If most sends take more than 2 seconds, stop and ask the researcher about the asynchronous fallback.

  **Record (2026-11-13), request time:** `scripts/_veredito_send_timing.py` sent five codes for the administrator, 61 s apart (23:05:18 to 23:09:19 UTC-3), all `200`: 6.84, 4.06, 4.29, 4.88 and 4.22 s (median 4.29 s; none below 2 s). Asked the researcher, who accepted up to 7 seconds; the synchronous send stays (research section 6 updated).
  **Record (2026-11-13), arrival and Swagger without a session:** the researcher received all five codes in Gmail, each in less than 1 minute (SC-002). Without a session, `/swagger-ui.html` and `/v3/api-docs` answer `401` with `application/problem+json`. The researcher signed in as a participant with two other addresses of his own, in two languages, and the checks passed (sign-in, name and "Sign out" in the bar, language switch while signed in, sign-out). An attempt with the administrator address answered `429 code-hourly-limit` ("about 48 minutes"), as expected after the five timing sends. Pending: the administrator's sign-in with `/admin`, Swagger with the administrator session, and a colleague's sign-in (SC-001).
  **Record (2026-11-14), closing:** the researcher signed in as administrator and `/admin` opened; `/swagger-ui.html` opened with that session; a colleague signed in from the "Participate" screen in less than 3 minutes (SC-001). Feedback from the session: the full language name in the toolbar feels heavy (prefer a flag that opens the same menu, still saving the profile language); add icons next to the participant's name and "Sign out"; later, look for ways to shorten the wait for the code (research section 6; roadmap, pilot milestone).

---

## Phase 8: Polish & Cross-Cutting Concerns

- [ ] T060 [P] Run `scripts/_check_utf8.py` over `Veredito/backend/src`, `Veredito/frontend/src`, `Veredito/frontend/e2e`, `Veredito/specs`, `Veredito/docs` and the root files; no file fails
- [ ] T061 [P] Confirm SC-006 of spec 003 still holds: the `secrets` job is green, GitHub secret scanning has no alert, and `git grep -n -i -E "password|secret|api[_-]?key|code_hash_key"` shows only variable names, documentation and the labeled staging value of `CODE_HASH_KEY`
- [ ] T062 Mark spec 004 as complete:
  - set `Status` in `specs/004-email-code-access/spec.md`;
  - update the line of 004 and milestone 2 ("Access") in `specs/ROADMAP.md`;
  - add a note in `specs/003-deployment-foundation/spec.md` that the e-mail part of US4 scenario 2 was checked by SC-005 of 004;
  - list any criterion left to a later spec;
  - commit and push to `Veredito`.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: no dependencies
- **Foundational (Phase 2)**: depends on Setup; blocks every story
- **US1 (Phase 3)**: depends on Phase 2
- **US2 (Phase 4)**: depends on US1 (sign-in creates the sessions it separates)
- **US4 (Phase 5)**: depends on US1 (e-mail language, profile language at sign-in) and on US2 (the `vd:me` resources that `PUT` extends)
- **US3 (Phase 6)**: depends on US1; T052 uses the header of US4 (T045)
- **Staging rehearsal and deployment (Phase 7)**: depends on all stories; T057 depends on the researcher
- **Polish (Phase 8)**: depends on Phase 7

### Story order versus priority

The spec gives P1 to US1, US2 and US4 and P2 to US3. US1 comes first because every other story needs a
session. US2 comes before US4 because the language `PUT` extends the profile resources of US2. US3 (P2)
comes last.

### Within Each User Story

- Tests first, failing
- Domain rules before services; services before controllers; back end before the screens that call it
- XLIFF units in the same task that adds the text

### Parallel Opportunities

- T002, T003 and T004 in parallel with T001
- T007, T008, T010 and T011 in parallel inside Phase 2; T019 in parallel with T012 to T017
- T020 and T021; T027 and T028; T031 and T032; T034 and T035; T038, T039 and T040; T043 and T044; T048 and T049 (different files)
- T056 in parallel with T055

---

## Parallel Example: Phase 2

```text
Task: "ClockConfig and MovableClock in <pkg>/config/ClockConfig.java, <tpkg>/support/MovableClock.java" (T007)
Task: "AccessProperties in <pkg>/config/AccessProperties.java" (T008)
Task: "Domain rules and tests in <pkg>/domain/access/, <tpkg>/domain/access/" (T010)
Task: "Hashers and tests in <pkg>/access/, <tpkg>/access/" (T011)
```

## Parallel Example: User Story 1

```text
Task: "AccessTest in <tpkg>/api/v1/AccessTest.java" (T020)
Task: "New types in <tpkg>/api/LocalizedProblemDetailsTest.java" (T021)
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Phases 1 and 2
2. Phase 3 (US1): codes, sessions and participants, with the sign-in screens in pt-BR
3. **Stop and validate**: quickstart sections 4 and 5 against local staging

### Incremental Delivery

1. Setup and foundation (Spring Security on, the API of 003 unchanged)
2. US1: sign in with the code
3. US2: administrative area and participant screen, authorization test
4. US4: language choice, profile language, header switch
5. US3: session lifetime, sign-out, cleanup
6. Staging rehearsal, secret, production deployment
7. Polish: UTF-8, secrets, status

---

## Notes

- V1 to V3 are immutable once deployed; any later schema change is a new migration (V4 on), compatible with the previous code version.
- The production key `veredito-code-key` and the Gmail app password are created only by the researcher (T057, spec 003 T062); they never appear in files, logs, chat or screenshots.
- `daily_email_limit` counts every code actually sent in the last 24 hours, the administrator's included (research section 5).
- No task calls models; the sequential execution rule of `ISSTA_2027` does not apply.
