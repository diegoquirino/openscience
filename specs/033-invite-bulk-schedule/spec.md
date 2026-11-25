# Feature Specification: Scheduled lembrete and fim-da-coleta e-mails

**Feature Branch**: `033-invite-bulk-schedule`

**Created**: 2026-11-25

**Status**: Complete at **0.33.0** (1.0 still gated; no cloud deploy)

**Input**: On **E-mail send settings**, moment fields MUST appear in chronological order: **collection start**, **collection reminder**, **collection end**. Time fields MUST use a **24-hour** clock (`HH:mm`). At the configured **reminder** date-time the system MUST send the **lembrete** template to every eligible participant; at the configured **collection end** date-time it MUST send the **agradecimento** (fim da coleta) template to every eligible participant. Local staging with Mailpit MUST show those messages when the clock reaches the configured moments.

**Depends on**: 031, 032

## User Scenarios

### US1 - Field order and 24-hour time (P1)

1. Given the administrator opens send settings, When they view **Send moments**, Then the fields appear as: collection start → collection reminder → collection end.
2. Given any time field, When they enter or read a time, Then the UI uses 24-hour format (`HH:mm`), not AM/PM.

### US2 - Lembrete at configured date-time (P1)

1. Given `invite_reminder_at` is set and the clock reaches that instant (or later while still unsent), When the scheduler runs, Then every eligible participant (non-anonymized, with e-mail) who does not yet have `invite_lembrete_sent_at` receives the **lembrete** e-mail once, and the marker is set on success.
2. Given Mailpit/staging, When the reminder moment has passed, Then the messages appear in Mailpit.

### US3 - Fim da coleta (agradecimento) at collection end (P1)

1. Given `invite_collection_end_at` is set and the clock reaches that instant (or later while still unsent), When the scheduler runs, Then every eligible participant without `invite_agradecimento_sent_at` receives the **agradecimento** e-mail once.
2. Given SMTP failure for one recipient, When others succeed, Then failed recipients keep a null marker for a later retry; successes stay marked.

## Requirements

- **FR-001**: Admin UI order: start, reminder, end; time inputs force 24-hour `HH:mm`.
- **FR-002**: Scheduled job (at least once per minute) sends due lembrete / agradecimento using existing templates and `invite_*_sent_at` idempotency.
- **FR-003**: Eligible = `participant.email` present, `anonymized_at` null.
- **FR-004**: App version **0.33.0**; 1.0 gated; no cloud; clean local staging for manual test.
- **FR-005**: Backend tests: clock past reminder/end → Mailpit receives bulk messages; second run does not duplicate.

## Success Criteria

- **SC-001**: UI shows start → reminder → end with 24-hour times.
- **SC-002**: Setting reminder/end to a near-future (or past) moment in staging delivers lembrete/agradecimento to Mailpit for registered participants.
- **SC-003**: ROADMAP 033 Complete at 0.33.0; staging clean for manual test.
