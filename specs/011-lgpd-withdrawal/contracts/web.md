# Contract: Administrator Participants screen (LGPD withdrawal)

**Feature**: `011-lgpd-withdrawal` | Follows [participants.md](participants.md) and the admin
shell of specs 005 / 009 / 010.

## Route

| Path | Who | Source of truth |
|---|---|---|
| `/admin/participants` | Administrator only (`administratorOnly` guard) | Search via `vd:participants` from `/api/v1` |

Participants who open `/admin/participants` are redirected like other admin routes. Visitors are
sent to sign-in.

## Admin home

- **Participants** list item is enabled and navigates to `/admin/participants`.
- Remove the “Participants will be available in the next releases” soon message (or retire that
  unit if unused).
- Import, Export and Dashboard stay as after 010.

## Screen behaviour

1. On init, resolve `vd:participants` from the entry point (never hardcode the collection path).
2. Search form: e-mail **or** pseudonym (exactly one field filled per submit).
3. No match → localized empty state.
4. Match → card with pseudonym, e-mail (if present), preferred language, completed / reserved
   summary, and impact counts for discard.
5. Two confirmation actions: **Anonymize and keep responses** and **Anonymize and discard
   responses**. Each opens a confirm step that shows:
   - preferred language of the confirmation e-mail;
   - counts of responses / tasks affected;
   - warning that previously downloaded exports must be redone and discarded;
   - when `emailCanBeSent` is false, warning that the confirmation e-mail cannot be sent and the
     administrator must inform the person outside the system in that language.
6. On success, show the result (`emailSent`, counts) and clear PII from the card (`anonymizedAt`).
7. Already anonymized card is read-only (no action buttons).

## i18n

New XLIFF units under `participants.*` / `admin.participants.*` in en-US / pt-BR / es-ES.
`i18n:check` must stay green.

## Automated checks

- Vitest: search empty / hit / confirm keep / confirm discard / quota warning.
- Playwright: admin opens Participants, searches, completes one withdrawal path; participant
  cannot open `/admin/participants`.
