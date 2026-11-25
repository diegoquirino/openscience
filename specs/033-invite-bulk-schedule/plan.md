# Plan: 033-invite-bulk-schedule

**Branch**: `033-invite-bulk-schedule` | **Date**: 2026-11-25 | **Spec**: [spec.md](./spec.md)

## Summary

Reorder admin send moments (start ? reminder ? end), force **24-hour** time entry, and run a minute scheduler that bulk-sends **lembrete** at `invite_reminder_at` and **agradecimento** at `invite_collection_end_at` to every eligible participant. Version **0.33.0**.

## Technical approach

| Area | Change |
|---|---|
| Frontend | Reorder moment blocks; replace `type="time"` with text `HH:mm` (24h) + validation. |
| Repository | List eligible recipients pending lembrete / agradecimento; claim/clear sent_at like inicial. |
| Job | `InviteBulkSendJob` `@Scheduled(cron = "0 * * * * *")` using `Clock`; due when `now >= configured`. |
| Mailer | Reuse `InviteMailer` + templates from 031. |
| Tests | `InviteBulkSendTest` with MovableClock + Mailpit. |
| Version | Bump `0.33.0` / `0.33.0+dev`. |
| Docs | ROADMAP 033; `.specify/feature.json`. |

## Out of scope

- Changing inicial-after-profile behaviour.
- Cloud / 1.0.
- Throttling by `daily_email_limit` (access-code quota stays separate).
