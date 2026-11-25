# Feature Specification: Research invite e-mails and admin date-time settings

**Feature Branch**: `031-invite-email-settings`

**Created**: 2026-11-25

**Status**: Complete at **0.31.0** (1.0 still gated; no cloud deploy)

**Input**: CEP invite texts live in `ISSTA_2027/CEP-TCLE/08_convites/` (inicial, lembrete, agradecimento). Operationally, the **initial invite** MUST be sent **right after** the participant completes the profile cadastro (name + skill + organization/employer). Templates MUST be easy to edit and to fill variables (including pending values such as CAAE). **Collection and reminder moments MUST be configured as date and time** in the administrator panel (“configurações de envio de e-mail”), not as date-only and not hard-coded in the template files alone.

**Depends on**: 013, 025, 030

## User Scenarios

### US1 - Admin configures e-mail send settings with date and time (P1)

1. Given an administrator session, When they open **E-mail send settings**, Then they can view and save:
   - CAAE (free text; may be placeholder until CEP parecer),
   - platform URL,
   - **collection start** as **date and time**,
   - **collection end** as **date and time**,
   - **reminder** as **date and time**.
2. Given invalid range (end before start), When they save, Then the API rejects with a validation problem and the UI shows an error.
3. Given saved values, When templates are rendered, Then `{{caae}}`, `{{url}}`, `{{coleta_inicio}}`, `{{coleta_fim}}` reflect those settings (formatted for display).

### US2 - Editable templates with variables (P1)

1. Given the three kinds `inicial`, `lembrete`, `agradecimento`, When the administrator opens template editors, Then subject and body are editable plain text with placeholders such as `{{nome}}`, `{{caae}}`, `{{url}}`, `{{coleta_inicio}}`, `{{coleta_fim}}`.
2. Given a pending CAAE, When the body still contains a placeholder value from settings (empty or literal), Then the researcher can update CAAE in settings without redeploying the app.
3. Given seed content derived from `08_convites`, When staging is fresh, Then the three templates are pre-loaded with the CEP drafts (variables substituted for hard-coded dates/URL/CAAE slots).

### US3 - Initial invite after profile cadastro (P1)

1. Given a participant who just saved a complete profile (name + skill + organization) for the first time, When the save succeeds, Then the system sends the **inicial** e-mail once to that participant’s e-mail, with `{{nome}}` replaced, and records `invite_inicial_sent_at`.
2. Given the same participant saves the profile again, When `invite_inicial_sent_at` is already set, Then no second inicial e-mail is sent.
3. Given SMTP failure, When send fails, Then the profile save still succeeds; the failure is logged (same resilience style as non-blocking where appropriate) **or** marked for retry without rolling back the profile — prefer: attempt send after commit; on failure leave `invite_inicial_sent_at` null so a later profile touch or resume can retry once.

### US4 - Lembrete / agradecimento (P2 in this slice)

1. Given templates and reminder **date and time** are configured, When this slice ships, Then lembrete and agradecimento are stored and editable; automatic bulk send of lembrete/agradecimento MAY be deferred to a later spec, but the admin settings MUST already hold `invite_reminder_at` as date-time.

## Requirements

- **FR-001**: Flyway migration adds invite settings on `config` (`invite_caae`, `invite_platform_url`, `invite_collection_start_at`, `invite_collection_end_at`, `invite_reminder_at` as `DATETIME(6)`) and `email_template(kind, subject, body, updated_at)`; participant columns `invite_inicial_sent_at` (and nullable stubs for lembrete/agradecimento timestamps).
- **FR-002**: Admin HAL resource for settings GET/PUT and templates GET/PUT by kind; date-times exchanged as ISO-8601 instants (UTC). Admin UI uses **date and time** controls (`datetime-local` or equivalent).
- **FR-003**: After first successful profile completion (name+skill+organization), send `inicial` with variable substitution; idempotent via `invite_inicial_sent_at`.
- **FR-004**: Seed templates from CEP `08_convites` wording (pt-BR body); keep files also under `backend/src/main/resources/mail/` as the migration seed source of truth for easy repo edits.
- **FR-005**: App version **0.31.0**; 1.0 gated; no cloud; clean local staging for manual test (Mailpit).
- **FR-006**: Admin hub card linking to e-mail send settings.

## Success Criteria

- **SC-001**: Admin can save collection start/end and reminder as date **and** time; values round-trip via API.
- **SC-002**: Completing profile once delivers an inicial message to Mailpit with name and settings variables filled.
- **SC-003**: ROADMAP 031 Complete at 0.31.0; staging clean for manual test.
