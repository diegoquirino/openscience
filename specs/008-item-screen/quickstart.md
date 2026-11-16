# Quickstart: validate spec 008

**Feature**: `008-item-screen` | Contract: [web](contracts/web.md) | Data: [data-model.md](data-model.md)

All commands run from the root of the `Veredito` branch (the `Veredito` folder), in PowerShell.
Prerequisites are those of the
[quickstart of spec 003](../003-deployment-foundation/quickstart.md#prerequisites). Participant and
administrator sign-in follow the [quickstart of spec 004](../004-email-code-access/quickstart.md);
the onboarding of [spec 006](../006-consent-screening/quickstart.md) is required before labeling.
A confirmed Minitest batch (spec 005) must exist.

## 1. Automated checks (SC-001, SC-004, Principle II)

```powershell
cd backend; .\mvnw.cmd -B verify; cd ..
cd frontend; npm ci; npm run i18n:check; npm test; npm run build; cd ..
```

Expected: everything passes. Among the front-end tests: `word-diff`, `usecase-text`,
`item-layout`, `item-clock`, the use case dialog and the item component (all four kinds, "(none)",
"No text difference", the `PUT` body unchanged). `i18n:check` fails if any new unit lacks its pt-BR
or es-ES target.

## 2. Local staging

```powershell
docker compose --profile staging down -v
docker compose --profile staging up --build -d
```

1. Sign in as administrator; import and confirm `veredito_minitest_vazio.csv` (005).
2. Sign out; sign in as a participant (code in Mailpit at `http://localhost:8025`) and finish the
   onboarding of 006 (consent form, skill, screening).

## 3. The four kinds (US1)

Items of the Minitest batch (positions are inside the block you receive):

| Kind | Where | What to check |
|---|---|---|
| `MOD` | Block 1, position 1 (`step`) | "Before" / "After" side by side; "launches" struck through, "displays" highlighted; both buttons with `v1.0` and `v1.1`; header "Basic flow step · Modification" |
| `ADD` | Block 1, position 3 (`alt_flow_steps_block`) | Only "Added text" with the new flow (line breaks kept); the first button stands where "Before" would be |
| `META` | Block 1, position 8 (`requirements`) | "Requirements link: before" shows "(none)"; "after" shows `REQ-07` |
| `DEL` | Block 3, position 1 (`exc_flow_steps_block`) | Only "Removed text"; the second button stands where "After" would be |

Block 3 is reached by labeling and submitting the earlier blocks you may receive; to look at it
faster, answer the earlier blocks over the API as the e2e does. Check on a computer and in a
360 px wide window: blocks stack, no horizontal scrolling.

## 4. Use case modals (US2)

On any item, click "View full use case (v1.1)": the title reads "<use case> · version v1.1
(previous)", the text is monospaced with indentation, the lines that had `<- edited` are
highlighted without the marker and the dialog opens scrolled to the first one. Close with the
"Close" button, with `Esc` and with a click outside: the impact, operation and comment you had
chosen are still there and the focus is back on the button. In a narrow window the dialog is full
screen, long lines wrap keeping their indentation, and the counter and countdown stay visible in
its header.

## 5. Questions (US3)

Both questions show every option with its explanation (Appendices B.1 and B.2 of
[spec 001](../001-oracle-labeling/spec.md)) and the operation question ends with the precedence
rule. Answer both, Continue: the draft stores the same codes as on the simple screen of 007:

```powershell
docker compose exec mysql mysql -uveredito -pveredito-local veredito -e "SELECT change_impact, maintenance_operation, language FROM response ORDER BY id DESC LIMIT 3"
```

## 6. Languages (US4, SC-004)

Repeat sections 3 to 5 under `/en-US/` and `/es-ES/` (or switch in the header): every label,
statement, explanation and message is in that language and the codes in the database are the same.
Use cases, `origin`, `target`, system and `uc` stay as captured. Type an impact, an operation and
a comment without saving, switch the language in the header: they come back after the reload.
The "(this use case does not exist in this version)" message and "(none)" are covered by the
Playwright run (the Minitest batch has no row without a version).

## 7. Pilot checks (SC-002, SC-003)

Recorded during the pilot, not by this run: the average time of a block of 10 items (target under
20 minutes) and the share of volunteers who understood the edit without outside help (target at
least 80%).

## 8. Production

1. Push to `Veredito`; wait for `ci.yml`.
2. Deploy: `git push origin <commit>:refs/tags/deploy/0.8.0-1` (**only with the researcher's explicit
   approval**).
3. A participant labels one block in each locale; the footer shows `0.8.0+…`.
