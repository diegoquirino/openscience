# Research: Handle a data withdrawal request (LGPD)

**Feature**: `011-lgpd-withdrawal` | **Date**: 2026-11-16 | **Spec**: [spec.md](spec.md)

This research starts from User Story 10 / FR-059 of spec 001, the participant and task model of
004–007, the wide/long exports of 009 (marker `anonymized`), and the dashboard tallies of 010
(already exclude `anonymized_at IS NOT NULL`).

## 1. Versions (checked on 2026-11-16)

| Component | Version | Note |
|---|---|---|
| Spring Boot | 4.1.2 | Unchanged |
| Angular / Material | 22.2.6 | Unchanged |
| App version | **0.11.0** | Spec number in the minor digit |
| Deploy tags | `deploy/0.11.0-*` | Production only with explicit approval |

No new Maven or npm dependency.

## 2. HAL API shape

- **Decision**:
  - Entry point (admin): `vd:participants` ? `GET /api/v1/participants`.
  - Search: `GET /api/v1/participants?email=` **or** `GET /api/v1/participants?pseudonym=`
    (exactly one of the two query parameters; case-insensitive exact match for e-mail using the
    stored binary collation via normalized lower-case compare in the service, exact match for
    pseudonym). Returns `200` with `_embedded.vd:participant` of size 0 or 1 (never a fuzzy list).
  - Item: `GET /api/v1/participants/{id}` returns the same card; `404` when missing.
  - Confirm: `POST /api/v1/participants/{id}/anonymizations` with JSON
    `{ "option": "keep_responses" | "discard_responses" }` (`vd:anonymize`). `201` with the
    audit summary; `Location` points at the (now anonymized) participant.
  - Already anonymized cards omit `vd:anonymize`. Search by a former e-mail finds nothing.
- **Rationale**: Richardson level 3; one admin screen; matches the disabled Participants entry.
- **Alternatives considered**: separate keep/discard link relations (two POSTs without a body —
  fine, but one relation + option enum matches the confirmation dialog); DELETE of the person
  (would lose the pseudonym link to kept responses).

## 3. Search and card contents (US1)

- **Decision**: the card shows `id`, `pseudonym`, `email` (only while not anonymized),
  `preferredLanguage`, onboarding/eligibility summary suitable for the admin, counts of
  completed tasks and any reserved task (block number + role), impact preview
  (`finalResponseCount`, `draftResponseCount`, `tasksReturnableOnDiscard`), and
  `emailCanBeSent` (rolling 24 h quota still has room). After anonymization: `anonymizedAt`
  set; `email` absent; no anonymize link.
- **Rationale**: FR-001 and US2 scenario 3 (counts + preferred language before confirm).
- **Alternatives considered**: substring search (risk of wrong person); always listing everyone
  (out of scope for a rare LGPD action).

## 4. Transactional withdrawal (FR-003, FR-004, FR-006, FR-007)

- **Decision** (single DB transaction after the confirmation e-mail attempt):
  1. **E-mail first** (outside or before the mutating transaction): if quota allows, send the
     localized confirmation to the current address; on SMTP failure, abort with Problem Details
     and change nothing. If quota is exhausted, skip sending, set `emailSent=false` on the
     result, and continue (edge case in the spec).
  2. Lock the participant row and every task of that participant (`SELECT … FOR UPDATE` in the
     same style as 007) so reserve/save/submit cannot interleave.
  3. Delete draft `response` rows of those tasks; for each `reserved` task, set
     `status='free'`, clear `participant_id` / `reserved_at` / `expires_at` (CHECK of V1).
  4. If `discard_responses`: delete all remaining `response` rows of the participant; for each
     `completed` task of that participant, set `status='free'` and clear
     `participant_id` / `reserved_at` / `expires_at` / `submitted_at`, keeping `queue_position`
     (FR-004 / 001 clarification). Tasks of the other role on the same block are untouched.
  5. If `keep_responses`: leave `final` responses and `completed` tasks linked to the
     participant id / pseudonym.
  6. Delete `access_session` rows for that `participant_id`; delete `access_code` rows for that
     e-mail (while the address is still known).
  7. Set `name=NULL`, `email=NULL`, `anonymized_at=now` (satisfies
     `ck_participant_anonymization`).
  8. Insert `withdrawal_log` (see §5).
- **Rationale**: FR-007 requires the e-mail before erasure; MySQL unique on `email` allows a
  later sign-in with the same address as a new person; 009 already exports `anonymized`.
- **Alternatives considered**: soft-delete of the whole participant row (breaks kept responses
  and dashboard contributor lists); delaying the e-mail until after erase (forbidden by FR-007).

## 5. Audit log (FR-005)

- **Decision**: Flyway `V7__withdrawal_log.sql` creates table `withdrawal_log`:
  `id`, `occurred_at`, `pseudonym`, `option` (`keep_responses`|`discard_responses`),
  `email_sent` (boolean), `final_responses_discarded`, `tasks_returned`, `participant_id`
  (nullable FK kept for support; never expose name/email — those columns do not exist here).
  No PII columns.
- **Rationale**: durable, queryable audit without personal data; constitution IV.
- **Alternatives considered**: application log only (harder to prove SC-001 / audits); storing
  the e-mail hash (unnecessary once erased).

## 6. Confirmation e-mail (FR-007, FR-008, SC-003)

- **Decision**: MessageSource keys `mail.withdrawal.subject` and `mail.withdrawal.body` in
  `messages.properties` / `messages_pt_BR.properties` / `messages_es_ES.properties`. Locale =
  participant `preferred_language` (fallback `pt-BR` if null). Body states which option was
  applied, without repeating the e-mail as a retained contact. Count the send toward the same
  rolling 24 h quota as access codes (insert a sentinel `access_code` row **or** a dedicated
  counter — prefer recording via the existing `access_code` send path only for codes; for
  withdrawal, increment by inserting into a small `email_send_log` **or** reuse counting by
  adding the withdrawal send to a shared counter table). **Pragmatic choice**: introduce column
  usage via existing `access_code` is wrong semantically; instead count withdrawal sends by
  storing `email_sent` only on `withdrawal_log` and extending the quota query to
  `COUNT(access_code) + COUNT(withdrawal_log WHERE email_sent)` in the rolling window — or
  simpler: when sending, also insert a row into `access_code` is unacceptable. **Chosen**: add
  table `email_outbox_count` is overkill. Extend `AccessCodeRepository.countAllSince` callers to
  a new `EmailQuotaService` that sums `access_code.created_at` plus
  `withdrawal_log.occurred_at WHERE email_sent = TRUE` in the last 24 h. Dashboard 010 keeps
  working through that service.
- **Rationale**: one quota number for the researcher; SC-003 on language.
- **Alternatives considered**: ignoring withdrawal sends in the quota (could exceed Gmail
  limits); requiring quota (spec allows continue without mail).

## 7. Exports and old files

- **Decision**: no export code change required for keep (009 already emits `anonymized`). After
  discard, exports simply omit those responses. UI warns that previously downloaded files must
  be redone and discarded (edge case).
- **Rationale**: SC-001; assumptions leave backups to the researcher.

## 8. Front end

- **Decision**: enable Participants in the admin shell; route `/admin/participants`; search form;
  confirmation dialog with both options, impact counts, preferred language, quota warning, and
  export redo warning; follow `vd:participants` / `vd:anonymize` (never hardcode paths).
- **Rationale**: US1–US2; constitution V (Material).

## 9. Security

- **Decision**: keep `/api/v1/participants/me` and `/api/v1/participants/me/**` as
  `ROLE_PARTICIPANT`. Restrict admin withdrawal paths
  (`GET /api/v1/participants`, `GET /api/v1/participants/{id}`,
  `POST /api/v1/participants/{id}/anonymizations`) to `ROLE_ADMINISTRATOR` (add those exact
  patterns to `ADMINISTRATION` or equivalent matchers **before** any broad participant rule).
  Never grant participants access to another id.
- **Rationale**: FR admin-only; do not break 006 `vd:me` under the same `/participants` prefix.

## 10. Testing

- **Decision**: domain tests for discard vs keep task/response rules; REST Assured
  `ParticipantWithdrawalTest` (search, keep, discard, re-sign-in with same e-mail, mail language,
  quota-exhausted path); update `EntryPointTest` / `AuthorizationTest` /
  `ApiDocumentationCompletenessTest`; Vitest + Playwright for the admin screen; assert wide
  export marker after keep.
- **Rationale**: SC-001–SC-003; constitution II.
