# Quickstart: validate spec 006

**Feature**: `006-consent-screening` | Contracts: [onboarding](contracts/onboarding.md),
[problem-details](contracts/problem-details.md), [web](contracts/web.md) |
Data: [data-model.md](data-model.md)

All commands run from the root of the `Veredito` branch (the `Veredito` folder), in PowerShell.
Prerequisites are those of the
[quickstart of spec 003](../003-deployment-foundation/quickstart.md#prerequisites). Participant and
administrator sign-in follow the [quickstart of spec 004](../004-email-code-access/quickstart.md).
A confirmed Minitest batch (spec 005) must exist before the last check of section 3.

## 1. Automated checks (SC-001 to SC-005, Principle II)

```powershell
cd backend; .\mvnw.cmd -B verify; cd ..
cd frontend; npm ci; npm run i18n:check; npm test; npm run build; cd ..
```

Expected: everything passes. Among the back-end tests: `OnboardingRulesTest` (every combination of
consent / skill / screening / form, SC-002); `ConsentFormRulesTest` (current form, incomplete
version, future publication); REST Assured `ConsentFormTest`, `ConsentDecisionTest`, `SkillTest`,
`ScreeningTest`, `OnboardingFlowTest` and `EligibilityGateTest` (a direct `POST
/tasks/reservations` is `403 not-eligible` until the three steps are done); the same codes stored in
the three locales (SC-005); the language of the text recorded in each acceptance (SC-004);
`FixtureLocationTest` (the fixture exists only in the staging location); `AuthorizationTest` denies
every Participation operation to visitors and to the administrator; the 007 task tests keep passing
with `eligibleParticipantSession()`.

## 2. Local staging

```powershell
docker compose --profile staging down -v
docker compose --profile staging up --build -d
```

The staging profile loads the fixture consent form `fixture-1` (placeholder text, not approved by
the CEP). Check it and the version of the footer (`0.6.0+...`):

```powershell
docker compose exec mysql mysql -uveredito -pveredito-local veredito -e "SELECT cf.version, cf.published_at, GROUP_CONCAT(t.language ORDER BY t.language) AS languages FROM consent_form cf JOIN consent_form_text t ON t.consent_form_id = cf.id GROUP BY cf.id"
```

Sign in as a participant (any non-admin e-mail; the code is in Mailpit at `http://localhost:8025`).
The administrator never sees the screens below: signing in as `diegoquirino@gmail.com` goes to
`/admin` and `GET /api/v1/participants/me/onboarding` answers `403`.

## 3. Happy path in each language (US1 to US4, SC-001, SC-004, SC-005)

Repeat with three new e-mails, choosing `Português (Brasil)`, `English (US)` and `Español (España)`
before signing in.

| Step | Expected |
|---|---|
| After the code | `/welcome` redirects to `/consent` (first pending step) |
| Consent text | The fixture text in the interface language; in en-US and es-ES the translation note and the link to the pt-BR reference at the top; buttons stay visible while scrolling |
| Click the reference link | A new tab shows the pt-BR text under the same interface language (header unchanged) |
| Switch language in the header | The page reloads with the same version in the new language; nothing recorded yet |
| "I have read it and agree to participate" | `/skill` |
| Skill: no option | Proceed disabled; choose "Other" with an empty or 101-character description: disabled |
| Choose `test_analyst` (or "Other" + a description) | `/screening` |
| Screening | English use case (basic + exception flow) and test case in every language; question and answers in the interface language |
| "Yes, in professional practice" | Goes straight to `/label` (007) |
| Sign out and sign in again | Straight to `/label`, no step repeated (US3 scenario 5) |

Check what was stored (the codes are identical in the three languages; `consent_language` differs):

```powershell
docker compose exec mysql mysql -uveredito -pveredito-local veredito -e "SELECT pseudonym, consent_decision, consent_language, consent_decided_at, professional_skill, professional_skill_other, screening_answer, eligible, preferred_language FROM participant ORDER BY id DESC LIMIT 3; SELECT cf.version FROM consent_form cf JOIN participant p ON p.consent_form_id = cf.id ORDER BY p.id DESC LIMIT 1"
```

## 4. Refusal and "No" (US1 scenarios 3 and 4, US3 scenarios 3 and 4)

| Step | Expected |
|---|---|
| New participant: "I do not agree" | `/consent/declined` with thanks; the row has `consent_decision = refused`, version, language, time |
| Sign out, sign in again | `/consent` again; this time accept and continue (changed mind) |
| Another participant answers "No" at the screening | `/screening/closed`; `screening_answer = no`, `eligible = 0` |
| Same participant signs in again; types `/pt-BR/label` and `/pt-BR/screening` in the address bar | Always the same closing message; never the labeling or the screening |
| Press the browser Back button after any answer | The guard returns to the real step; the answer can not be changed |

## 5. New version of the consent form (US1 scenario 5, US4 scenario 6)

Use an eligible participant of section 3. Publish a **complete** second version in MySQL:

```powershell
docker compose exec mysql mysql -uveredito -pveredito-local veredito -e "INSERT INTO consent_form (version, reference_language, created_at, published_at) VALUES ('fixture-2', 'pt-BR', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)); INSERT INTO consent_form_text (consent_form_id, language, text) SELECT id, l.language, CONCAT('FIXTURE 2 (', l.language, '): not approved by the CEP.') FROM consent_form, (SELECT 'pt-BR' AS language UNION SELECT 'en-US' UNION SELECT 'es-ES') l WHERE version = 'fixture-2'"
```

Reload the participant's page or sign in again: only the consent form is asked (the new text), then
`/label`; skill and screening are not repeated. The entry point had no `vd:next-task` in between.

Now an **incomplete** third version:

```powershell
docker compose exec mysql mysql -uveredito -pveredito-local veredito -e "INSERT INTO consent_form (version, reference_language, created_at, published_at) VALUES ('fixture-3', 'pt-BR', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)); INSERT INTO consent_form_text (consent_form_id, language, text) SELECT id, 'pt-BR', 'only Portuguese' FROM consent_form WHERE version = 'fixture-3'"
docker compose restart app
docker compose logs app --tail 30
```

Expected: participants keep seeing `fixture-2`; the log has a `WARN` naming `fixture-3` and the
missing languages `en-US, es-ES`. Add the two texts and it becomes current without a restart.

"The study is not open yet" (US1 scenario 6):

```powershell
docker compose exec mysql mysql -uveredito -pveredito-local veredito -e "UPDATE consent_form SET published_at = NULL"
```

Any participant (even a previously eligible one) lands on `/not-open`; `GET /api/v1/consent-forms/current`
is `404`. Restore with `UPDATE consent_form SET published_at = created_at WHERE version = 'fixture-1'`
(or restart staging with `down -v`). A participant who answered "No" still sees the closing message.

## 6. Direct calls and exclusion (US1 to US3, SC-002)

With a **new, not onboarded** participant and the cookie of that session (browser developer tools,
or the REST Assured tests):

| Call | Expected |
|---|---|
| `GET /api/v1` | Links `vd:onboarding` and `vd:consent-form`; **no** `vd:next-task` |
| `POST /api/v1/tasks/reservations` | `403` `not-eligible` with `step: "consent"` |
| `PUT /api/v1/participants/me/skill` before consent | `409` `step-not-pending`, `step: "consent"` |
| `PUT /api/v1/participants/me/consent` with `consentFormVersion: "v0"` | `409` `consent-form-outdated` with `currentVersion` |

After the three steps, `GET /api/v1` has `vd:next-task` and the reservation works. Administrator
cookie on any `/participants/me/*`, `/consent-forms/*` or `/screening-example`: `403` `forbidden`.

## 7. Labeling still works (spec 007)

With an eligible participant and a confirmed Minitest batch (spec 005, `veredito_minitest_vazio.csv`):
`/label` reserves block B1-R1 and the 007 flow is unchanged. Reserve a block, then publish
`fixture-2` as in section 5: the next save or submit answers `not-eligible`, the screens send the
participant to `/consent`, and after accepting the normal reservation rules of 007 apply (the block
may have expired).

## 8. Production

1. The researcher supplies the CEP-approved pt-BR text and the en-US / es-ES translations as one
   migration in `db/migration` (next free number, for example `V7__consent_form_v1.sql`, version
   `v1`; one form, three texts, one file). Review it like code; a typo is fixed with a new version,
   never an `UPDATE`.
2. Push to `Veredito`; wait for `ci.yml`. The staging fixture is not in the production image's active
   Flyway locations (`FixtureLocationTest`).
3. Run sections 2 to 7 once more in staging **with the real migration added to `db/migration`**
   (staging then shows `v1` as current, because it is published after the fixture date).
4. Deploy only with explicit approval of the researcher:
   `git push origin <commit>:refs/tags/deploy/0.6.0-1`.
5. Sign in with a participant of the pilot: they pass consent, skill and screening, then label one
   block; the footer shows `0.6.0+...`. In the pilot database, check that the accepted row stores
   `v1` and the language of the text shown.
6. Rollback if needed: redeploy the previous tag (`deploy/0.7.0-1`); there is no schema change and
   the old gate ignores the new data.
