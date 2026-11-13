# Quickstart: validate spec 004

**Feature**: `004-email-code-access` | Contracts: [access](contracts/access.md),
[problem-details](contracts/problem-details.md), [web](contracts/web.md),
[pipeline](contracts/pipeline.md) | Data: [data-model.md](data-model.md)

All commands run from the root of the `Veredito` branch (the `Veredito` folder), in PowerShell.
Prerequisites and the port note are those of the
[quickstart of spec 003](../003-deployment-foundation/quickstart.md#prerequisites).

## 1. Automated checks (SC-003, Principle II)

```powershell
cd backend; .\mvnw.cmd -B verify; cd ..
cd frontend; npm ci; npm run i18n:check; npm test; npm run build; cd ..
```

Expected: everything passes. Among the back-end tests: the domain tests of the access rules,
`AccessTest`, `SessionTest`, `AuthorizationTest`, `LanguageProfileTest`, and
`LocalizedProblemDetailsTest` with the new types. `AuthorizationTest` reports how many
administrative operations it called; every call without a session and with a participant session
is denied (SC-003).

## 2. Local staging

```powershell
docker compose --profile staging down -v
docker compose --profile staging up --build -d
```

Mailpit inbox: `http://localhost:8025`. Use e-mails under `example.com`.

## 3. Language choice (US4)

| Step | Expected |
|---|---|
| Private window, browser language `es-MX`, open `http://localhost:8080/` | Choice page with "Español (España)" preselected |
| Same with `en-GB`, `pt-BR` and `fr-FR` | "English (US)", "Português (Brasil)", "Português (Brasil)" preselected |
| Confirm "English (US)" | `/en-US/`; cookie `veredito-lang=en-US` |
| Open `http://localhost:8080/` again | Straight to `/en-US/`, no choice page |
| Set the cookie to `xx-XX` in the developer tools and open `/` | Choice page |
| New private window, open `http://localhost:8080/es-ES/` directly | Spanish home page; cookie `veredito-lang=es-ES` |

## 4. Sign in (US1, US2, US4 scenarios 5 to 7)

| Step | Expected |
|---|---|
| In `/pt-BR/`, "Participar", name "A", e-mail "ana@" | Both fields marked; no e-mail in Mailpit |
| Name "Ana Souza", e-mail ` Ana@Example.com ` | Code screen; "valid for 10 minutes", spam warning; Mailpit shows one e-mail to `ana@example.com` in Portuguese |
| Type the code with spaces (`123 456`) | `/pt-BR/welcome` with the provisional text; Mailpit has no new e-mail |
| `docker compose exec mysql mysql -uveredito -p veredito -e "SELECT pseudonym, name, email, preferred_language FROM participant"` | `R-0001`, `Ana Souza`, `ana@example.com`, `pt-BR` |
| Repeat in `/es-ES/` with `bia@example.com` | E-mail in Spanish; `R-0002`, `es-ES` |
| Sign out, switch the browser to `/en-US/`, sign in again as `bia@example.com` | After the code, the interface reloads in Spanish (profile wins); cookie `es-ES` |
| Sign in with `diegoquirino@gmail.com` (arrives in Mailpit) | `/admin`: name, menu Import, Export, Dashboard, Participants, all disabled; no new row in `participant`; `config.admin_name` and `admin_preferred_language` set |

## 5. Codes and limits (US1 scenarios 4 to 9, edge cases)

| Step | Expected |
|---|---|
| Request a code and type a wrong one 5 times | "Wrong code; N attempts left" for the first 4; the 5th says the code is no longer valid and offers a new one |
| Request a code, then "Resend" at once | "Resend" disabled with the seconds left; the API answers `429 code-resend-too-soon` |
| Request 5 codes for one e-mail (waiting 60 s between them), then a 6th | Refused, "try again in about N minutes" |
| Request on two tabs for the same e-mail; type the first code | Refused; the second code works |
| Lower the limit: `docker compose exec mysql mysql -uveredito -p veredito -e "UPDATE config SET daily_email_limit = 1"`, request two codes for different e-mails | The second says no more codes today |
| `docker compose stop mailpit`, request a code | "The code could not be sent right now"; `access_code` has no new row; `docker compose start mailpit` |

## 6. Session (US3, SC-004)

| Step | Expected |
|---|---|
| Sign in, close the browser, open `http://localhost:8080/` again | Still signed in; no e-mail in Mailpit |
| Developer tools: the cookie `veredito-session` | `HttpOnly`, `Secure`, `SameSite=Lax`, expires in 24 hours; `access_session.token_hash` does not contain the cookie value |
| "Sign out" | Home page; the cookie is gone; `GET /api/v1/access-sessions/current` answers `401` |

`SessionTest` covers the 24-hour expiry with a moved clock.

## 7. Language switch (US4 scenarios 8 to 10)

| Step | Expected |
|---|---|
| On `/pt-BR/participate`, type name and e-mail, switch to English in the header | `/en-US/participate` with the typed values restored |
| Signed in as a participant, switch to Spanish | `/es-ES/welcome`; `participant.preferred_language = es-ES` |
| Signed in as the administrator, switch to English | `/en-US/admin` in English; `config.admin_preferred_language = en-US` |
| Trigger each error of section 5 in each locale | Every message in the language of the interface |

## 8. Administrative access (US2 scenario 3, SC-003)

```powershell
curl.exe -s -i http://localhost:8080/api/v1/administrator/me
```

Expected: `401` Problem Details. With a participant's cookie (copied from the developer tools):
`403` Problem Details, and `docker compose logs app` shows a `WARN` line with the pseudonym, the
method and the path, without e-mail or token.

## 9. End to end

```powershell
cd frontend; npm run e2e; cd ..
```

Expected: the Playwright tests of 003 plus the sign-in per locale, the e-mail language, the profile
language winning and the header switch keeping the sign-in form (SC-005, SC-006).

## 10. Production (researcher)

1. Create the secret `veredito-code-key` and its binding ([contracts/pipeline.md](contracts/pipeline.md)).
2. Deploy: `git push origin <commit>:refs/tags/deploy/0.4.0-1`.

| Check | Expected |
|---|---|
| Sign in as `diegoquirino@gmail.com` at the production URL | The code arrives in Gmail (check spam once) in less than 1 minute (SC-002); `/admin` opens |
| `<url>/swagger-ui.html` with the administrator session | Opens (spec 003, US5 scenario 3) |
| Same address in a private window | `401` Problem Details |
| Request time of `POST /api/v1/access-codes` (browser network panel), five sends | Below 2 s for most sends (Principle V; research, section 6) |
| A colleague signs in from the "Participate" screen | Done in less than 3 minutes (SC-001) |
