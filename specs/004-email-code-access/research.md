# Research: Sign in with name, e-mail and code

**Feature**: `004-email-code-access` | **Date**: 2026-11-13 | **Spec**: [spec.md](spec.md)

This research starts from the technology selection of spec 001
([research.md](../001-oracle-labeling/research.md)) and from what spec 003 delivered
([research.md](../003-deployment-foundation/research.md)). It only records what spec 004 decides on
top of them.

## 1. Versions (checked on 2026-11-13)

| Component | Version | Note |
|---|---|---|
| Spring Boot | 4.1.2 | 4.2.0 is still at RC2 (GA due on 2026-11-19); the move to 4.2.x stays a minor update, done in this spec if 4.2.0 is out before the deployment of 004 |
| Spring Framework | 7.0.10 | Managed by Spring Boot 4.1.2 |
| Spring Security | 7.1.2 (`spring-boot-starter-security`) | Managed by Spring Boot 4.1.2; new in 004 |
| Jakarta Mail / Angus Mail | 2.1.5 / 2.0.5 (`spring-boot-starter-mail`, already in the build since 003) | Managed by Spring Boot 4.1.2 |
| Testcontainers | 2.0.5 | Managed by Spring Boot 4.1.2; MySQL and, new in 004, a generic container for Mailpit |
| Mailpit | v1.33.1 (latest release) | Local staging, development and, new in 004, the API tests |
| springdoc-openapi | 3.2.0 | Unchanged |
| REST Assured | 6.3.0 | 7.0.0 is at RC1; unchanged |
| Angular, Angular CDK, Angular Material, `@angular/localize` | 22.2.6 | 22.3.0 is at RC; unchanged |
| Playwright | 1.65.0 | Unchanged |

No other new dependency: the access code, the session token and the hashes use the JDK
(`SecureRandom`, `Mac` with HmacSHA256, `MessageDigest` with SHA-256, `MessageDigest.isEqual`).

## 2. Authentication framework

- **Decision**: Spring Security 7, with no HTTP session: `SessionCreationPolicy.STATELESS`, form
  login, HTTP Basic and logout off. A custom `OncePerRequestFilter` reads the cookie
  `veredito-session`, hashes the token, finds a valid row in `access_session` and puts an
  `Authentication` with the role `PARTICIPANT` or `ADMINISTRATOR` in the security context.
  Authorization rules by path and method are listed in [contracts/access.md](contracts/access.md).
  The `AuthenticationEntryPoint` and the `AccessDeniedHandler` write localized Problem Details
  (`authentication-required`, `forbidden`) through the same builder as `ProblemDetailsAdvice`.
  The real `AdministratorSessionCheck` of 003 reads the security context, so Swagger opens in
  production for the administrator (spec 003, US5 scenario 3).
- **Rationale**: Principle I asks for the standard, mainstream piece. Spring Security gives the
  filter chain, the path rules, the security headers (`X-Content-Type-Options`, `X-Frame-Options`,
  `Cache-Control` on API responses) and method security for later specs. The sessions themselves
  live in the database (FR-003 and FR-004), so the four Cloud Run instances share them with no
  sticky routing.
- **Alternatives considered**: a hand-written servlet filter with a `HandlerInterceptor` (fewer
  pieces now, but every later spec would re-implement path rules and headers); Spring Session JDBC
  (stores a serialized session and its own tables, and the token would not be the hashed one of
  `access_session`).

## 3. Session token and cookie

- **Decision**: at sign-in, the server draws 32 random bytes (`SecureRandom`) and sends them,
  base64url without padding, in the cookie `veredito-session`; the database keeps only the
  SHA-256 of the token (`access_session.token_hash`, hexadecimal). Cookie attributes: `HttpOnly`,
  `Secure`, `SameSite=Lax`, `Path=/`, `Max-Age=86400`. The session expires 24 hours after sign-in,
  with no sliding renewal (FR-004, "at most 24 hours"). Sign-out deletes the row and sends the same
  cookie with `Max-Age=0`. Signing in again while a session exists creates a new session; the old
  row expires on its own.
- **Rationale**: Principle IV (hash only; `HttpOnly`, `Secure`, `SameSite`). A 256-bit random token
  does not need a slow or keyed hash: nobody can guess it from the SHA-256. Browsers accept
  `Secure` cookies on `http://localhost`, so local staging and the Playwright tests use the same
  attributes as production; REST Assured ignores the flag.
- **Alternatives considered**: a signed token (JWT) without a database row (sign-out could not
  revoke it before expiry); a sliding 24-hour window (spec says at most 24 hours from sign-in).

## 4. Access code

- **Decision**: 6 digits drawn uniformly from `000000` to `999999` with `SecureRandom`. The database
  keeps `HMAC-SHA256(key, email + ":" + code)` in `access_code.code_hash`; the key (32 random
  bytes) comes from the environment variable `CODE_HASH_KEY`. In production it is the new secret
  `veredito-code-key` in Secret Manager, created by the researcher without seeing its value (same
  helper as the database password in `docs/cloud-setup.md`); in local staging it is a fixed,
  non-secret value in `compose.yaml`; in the tests it is random per run. Before checking, the typed
  code loses spaces and hyphens (edge case). Only the most recent code of an e-mail counts (edge
  case "two devices"): the check reads the latest row of that e-mail and compares in constant time
  (`MessageDigest.isEqual`). A wrong code increments `attempts`; at 5 the code is dead
  (FR-002). A right code sets `used_at` in the same transaction that creates the session.
- **Rationale**: with only one million possible codes, a plain hash can be reversed at once by
  anyone who reads the table (a leaked backup, for example), who could then sign in as the
  administrator during the 10 minutes of a pending code. A keyed hash with the key outside the
  database closes that path. Binding the e-mail to the hash keeps one code from being reused for
  another e-mail.
- **Alternatives considered**: plain SHA-256 or bcrypt of the code (reversible by brute force over
  one million values); storing the code encrypted (needs the same key and keeps a reversible
  value).

## 5. Limits on code requests

- **Decision**: every limit is computed from the rows of `access_code`, which records one row per
  code actually sent:
  - **resend after 60 s** (scenario 6): the latest row of the e-mail is less than 60 s old, so the
    answer is `429 code-resend-too-soon` with `retryAfterSeconds` and the `Retry-After` header;
  - **5 per e-mail per hour** (scenario 7): 5 rows of the e-mail in the last 60 minutes, so the
    answer is `429 code-hourly-limit` with `retryAt` (the oldest of those rows plus 1 hour);
  - **daily e-mail limit** (FR-009, scenario 9): the number of rows of all e-mails in the last
    24 hours reaches `config.daily_email_limit`, so the answer is `429 daily-email-limit`.

  The request locks the `config` row (`SELECT ... FOR UPDATE`) while it counts and inserts the new
  row, so two requests at the limit cannot both pass; the e-mail goes out after the commit
  (section 6). Administrator codes count like any other.
- **Rationale**: the table already records each send, so no counter table is needed (the "counter
  of e-mails sent per day" that spec 003 left to 004 is this query). A rolling 24-hour window
  matches the Gmail quota, which is itself rolling, better than a calendar day; the message still
  says to try again the next day. The lock on one row serializes only code requests, which are
  rare.
- **Alternatives considered**: a `daily_email_count` table per date (a calendar day can let 900
  sends through in 24 hours, above the Gmail quota); in-memory counters (lost on restart and not
  shared by the Cloud Run instances).
- **Not in this spec**: a limit per client IP address. The spec does not ask for it; if someone
  exhausts the daily quota on purpose during the data collection, a per-IP limit is the first
  measure (risk recorded in [plan.md](plan.md)).

## 6. Sending the e-mail

- **Decision**: Spring Mail (`JavaMailSender`), synchronous, inside the request, after the
  transaction of section 5 commits; connection, read and write timeouts of 5, 10 and 10 seconds.
  If sending fails, the row just inserted is deleted and the answer is `503 code-not-sent`
  (edge case "error when sending": no code becomes valid). The sender is
  `Veredito <SMTP_USER>`; the recipient is the normalized e-mail. Production uses Gmail SMTP
  (`smtp.gmail.com:587`, STARTTLS, app password from `veredito-smtp-password`); local staging and
  development use Mailpit (`mailpit:1025`, no authentication).
- **Rationale**: the person must know at once whether the code left (scenario 1 and the error edge
  case), and the volume is low. Committing before sending keeps the `config` lock away from the
  network call. A Gmail SMTP exchange from `southamerica-east1` normally takes about one second,
  within the 2-second limit of Principle V; the quickstart measures it in production.
- **Alternatives considered**: asynchronous sending with a status resource that the client polls
  (meets the 2-second limit with margin but adds a resource, a state and a poll loop); kept as the
  fallback if the production measurement goes above 2 seconds for most sends.

## 7. Text of the e-mail

- **Decision**: subject and body (plain text) come from `MessageSource`, keys
  `mail.access-code.subject` and `mail.access-code.body`, with the name, the code and the validity
  in minutes as arguments, in the request locale (the `Accept-Language` that the Angular build
  sends). The bundle parity test then covers the e-mail too.
- **Rationale**: FR-014 and the edge case "the e-mail uses the language of the interface in use,
  not the profile". One short plain-text message per locale fits a bundle; spec 001 research
  planned one template file per locale under `mail/`, which would need its own parity check. Spec
  001 research is updated to point here.
- **Alternatives considered**: template files per locale (`mail/access-code_pt_BR.txt`, ...)
  or a template engine (one more dependency for a three-line message); HTML e-mail (more likely to
  be filtered as spam, and nothing to format).

## 8. Participant registration, pseudonym and the administrator

- **Decision**: when a code is validated:
  - **Administrator** (the e-mail equals `config.admin_email`): no participant row (FR-007).
    `config.admin_name` keeps the latest name typed (new column, V3), and
    `config.admin_preferred_language` gets the language in use if it is still `NULL` (FR-012).
  - **Participant**: `INSERT ... ON DUPLICATE KEY UPDATE` on the unique `participant.email`.
    - A new e-mail gets `pseudonym = 'R-' + id`, zero-padded to 4 digits (`R-0001`; after 9999,
      `R-10000`), set in the same transaction (scenario 10).
    - An existing e-mail keeps its pseudonym and receives the latest name (scenario 11, FR-005).
    - `preferred_language` gets the language in use only if it is `NULL`.
- **Rationale**: the auto-increment id is sequential and unique, and is generated by the database
  without an extra lock. The only gaps come from a rolled-back insert, which keeps the order and
  the uniqueness of FR-006. Keeping the administrator in `config` follows spec 001 (a single
  administrator, not a participant).
- **Alternatives considered**: `MAX(pseudonym) + 1` under a table lock (no gaps, but it serializes
  every first sign-in); a random pseudonym (not sequential, against FR-006).

## 9. Authorization and denied attempts

- **Decision**: three callers (visitor, participant, administrator) and the rules of
  [contracts/access.md](contracts/access.md):
  - a missing or expired session on a protected resource answers `401 authentication-required`;
  - a participant on an administrative resource answers `403 forbidden`;
  - the administrator on a participant-only resource answers `403 forbidden` (US2 scenario 4: the
    administrator cannot enter the data labeling flow).

  Every `401` and `403` on an administrative resource is logged at `WARN` with the caller kind,
  the pseudonym when there is one, the method and the path; never the e-mail or the token (US2
  scenario 3). The test `AuthorizationTest` reads the OpenAPI group `v1`, takes every operation
  tagged "Administration", and calls each one without a session and with a participant session,
  expecting 100 % denials (SC-003). Later specs add operations to the tag and are covered with no
  change to the test.
- **Rationale**: FR-007, FR-008 and SC-003, with the same enumeration idea as the documentation
  completeness test of 003.
- **Alternatives considered**: a hand-written list of administrative endpoints in the test (each
  spec would have to remember to extend it).

## 10. Cross-site request forgery

- **Decision**: Spring Security's CSRF token is off. The defenses are:
  - the session cookie is `SameSite=Lax`, so browsers do not send it on cross-site `POST`, `PUT`
    or `DELETE`;
  - every state-changing operation takes `application/json` (or HAL-FORMS) or has no body, and
    `DELETE`/`PUT` are not simple methods, so a cross-site page cannot send them without a CORS
    preflight;
  - the application has no CORS configuration, so every preflight fails.
- **Rationale**: these three layers cover the attack. A CSRF token would add a cookie, a header
  and an extra request to every REST Assured test and to the Angular client, for no additional
  protection.
- **Alternatives considered**: `CookieCsrfTokenRepository` with Angular's built-in XSRF support
  (more moving parts for the same protection).

## 11. Language choice before and after sign-in

- **Decision**:
  - **Root `/`**: the routing filter of 003 answers `302` to `/<locale>/` when the cookie
    `veredito-lang` holds one of the three locales, with `Vary: Cookie` (FR-011, scenario 4).
    Otherwise it serves the static page, whose new `language.js` preselects the first supported
    language of `navigator.languages` by language (`es-MX` gives es-ES; anything else gives pt-BR)
    and sets the cookie when the person confirms (FR-010, scenarios 1 to 3). An invalid cookie
    value is ignored (edge case).
  - **Cookie**: `veredito-lang`, `Path=/`, `Max-Age` one year, `SameSite=Lax`, not `HttpOnly`
    (set by JavaScript). At startup, each Angular build writes its own locale to the cookie, which
    covers a direct link to a prefix with nothing saved (edge case) and keeps the cookie in step
    with the page.
  - **Profile wins** (FR-012, scenario 7): the session representation carries the profile
    language. After sign-in, if it differs from the build locale, the client sets the cookie and
    reloads the next screen under the profile prefix.
  - **Header switch** (FR-013, scenario 8): a Material menu in the header, on every screen. When
    signed in, it first sends `PUT` to the `vd:me` resource with the new language. Then the open
    form writes its unsaved values to `sessionStorage` under `veredito-unsaved:<route>`, the
    client sets the cookie and loads the same route under the new prefix, and after the reload the
    form restores the values and deletes the entry. It replaces the footer links of 003.
- **Rationale**: the server-side redirect avoids flashing the choice page for whoever already
  chose; the rest follows spec 001 research (*Switching language*, *Unsaved input on a switch*).
- **Alternatives considered**: redirecting `/` only in JavaScript (the choice page would flash);
  storing the language only in the profile (does not work before sign-in).

## 12. Screens

- **Decision**: Angular Material screens, all under each locale prefix:

  | Route | Screen | Who |
  |---|---|---|
  | `/participate` | Name and e-mail form; validation per field; warning about spam | visitor |
  | `/participate/code` | 6-digit code field; "Resend" (counts down the seconds left) and "Correct e-mail" | visitor with a code requested |
  | `/welcome` | Provisional participant screen "Thank you for identifying yourself; the next step will be available soon" (US2 scenario 2) | participant |
  | `/admin` | Administrative area: the administrator's name and the menu Import, Export, Dashboard, Participants, all disabled in 004 (US2 scenario 1) | administrator |

  The header shows the language switch on every screen and, when signed in, the name and
  "Sign out". Route guards read the session representation (`GET /api/v1/access-sessions/current`,
  reached through the `vd:session` link of the entry point): a visitor who opens `/welcome` or
  `/admin` goes to `/participate`, a participant who opens `/admin` goes to `/welcome` (and the API
  refuses anyway), and a signed-in person who opens `/participate` goes to their area. Every
  message of the spec comes from the Problem Details `type` mapped to a localized text of the
  build (FR-014), never from the raw `title` or `detail`.
- **Rationale**: US1 to US4 with the provisional screens that bound the scope until 005 to 011.
- **Alternatives considered**: one page with the two steps (the "Correct e-mail" path and a page
  reload during the code step are simpler with two routes).

## 13. Tests

- **Decision**:

  | Level | What |
  |---|---|
  | Domain (JUnit, no Spring) | E-mail normalization and validation, name length (2 to 120 after trimming), code normalization (spaces and hyphens), the limit decision (resend, hourly, daily) as a pure function of the recent send times, `now` and the configuration, the pseudonym format |
  | Persistence (Testcontainers MySQL) | V3 migration; upsert of a participant with a concurrent first sign-in; only the latest code counts |
  | API (REST Assured, MySQL and Mailpit in containers) | `AccessTest` (scenarios of US1, codes read from the Mailpit API), `SessionTest` (US3, with a test `Clock` moved forward 24 hours), `AuthorizationTest` (US2 and SC-003), `LanguageProfileTest` (FR-012, FR-013), localized Problem Details for every new type in the three locales |
  | Front end (Vitest) | Guards, the Problem Details type mapping, the language service (cookie, `sessionStorage` restore), the resend countdown |
  | End to end (Playwright, local staging) | Per locale: `/`, preselection by browser language (`pt-BR`, `es-MX`, `en-GB`, `fr-FR`), sign in with the code read from Mailpit, the e-mail language, the screen after sign-in; profile wins; header switch on the sign-in form keeps the typed values; administrator sign-in |

  The application reads time from a `java.time.Clock` bean, which the tests replace with a clock
  they can move, so expiry (10 minutes, 24 hours) is tested without waiting.
- **Rationale**: Principle II and SC-003, SC-005, SC-006.

## 14. Cleanup of expired rows

- **Decision**: a `@Scheduled` job, every hour, deletes the rows of `access_code` older than
  24 hours and the rows of `access_session` already expired. With several Cloud Run instances,
  the job may run more than once; the `DELETE` is idempotent.
- **Rationale**: access codes hold an e-mail, which is personal data; keeping them only for the
  24-hour window that the daily limit needs follows the LGPD principle of necessity, and keeps the
  tables small.
- **Alternatives considered**: keeping the rows for statistics (the dashboard of 010 counts sends
  per day from the same window; older numbers are not needed by the study).

## 15. Changes to the deployment

- **Decision**:
  - **New secret:** `veredito-code-key`, with `secretAccessor` for `veredito-run`.
  - **`deploy.yml`:** the new secret is added to `--set-secrets`
    (`CODE_HASH_KEY=veredito-code-key:latest`).
  - **`docs/cloud-setup.md`:** gains the one-time commands, using the same helper that generates a
    password without showing it.
  - **Order of operations:** the researcher creates the secret before the first deployment of 004.
    The deployment of 004 fails at startup if the key is missing, and Cloud Run keeps the previous
    revision.
- **Rationale**: section 4; Principle IV (secrets only in Secret Manager).
- **Alternatives considered**: deriving the key from the database password (couples two secrets
  and breaks if the password is rotated).
