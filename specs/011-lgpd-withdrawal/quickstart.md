# Quickstart: LGPD withdrawal

**Feature**: `011-lgpd-withdrawal` | Contracts: [participants](contracts/participants.md),
[web](contracts/web.md)

## 1. Prerequisites

- Docker Desktop; JDK 25; Node 26.
- Staging: `docker compose --profile staging up --build`.
- Minitest imported; at least one eligible participant with a completed block (and ideally one
  reserved) created through the normal access / queue flows (or test fixtures).

## 2. Version

Footer / `/api/v1` show `0.11.0+…`. Sign in as administrator (`diegoquirino@gmail.com`; code in
Mailpit in staging).

## 3. Search

1. Open **Participants**.
2. Search by the participant e-mail ? card with pseudonym, language, counts.
3. Search by a wrong e-mail ? empty state.
4. After anonymization, the same e-mail search finds nothing; pseudonym search shows a read-only
   anonymized card.

## 4. Keep responses

1. Confirm **Anonymize and keep responses**.
2. Mailpit (or production inbox) receives the confirmation in the participant's preferred
   language **before** the address disappears from the card.
3. Wide export shows `anonymized` in name/e-mail columns for that rater; finals remain.
4. Any reservation is gone; drafts are gone.

## 5. Discard responses

1. On another participant with completed tasks, confirm **Anonymize and discard responses**.
2. Confirmation shows how many responses and tasks will be affected.
3. After confirm: responses gone; those tasks are free again at the same queue positions and are
   handed out before later positions.
4. The other role on a shared block is unchanged.

## 6. Quota exhausted

With the daily quota forced to zero in staging (or after exhausting it), confirm a withdrawal:
operation succeeds; UI warns that mail was not sent; administrator informs the person offline.

## 7. Production (only with explicit approval)

1. CI green on the commit.
2. Deploy: `git push origin <commit>:refs/tags/deploy/0.11.0-1` (**only with the researcher's
   approval**).
3. Footer shows `0.11.0+?`; admin Participants works; participant cannot open it.

**Done (2026-11-16):** `ci.yml` #74 green on `060bd1e`; tag `deploy/0.11.0-1`; deploy #14;
revision `veredito-00014-7tc` at 100%; production `/api/v1` `systemVersion` `0.11.0+060bd1e`.
