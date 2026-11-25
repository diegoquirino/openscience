# Plan: 031-invite-email-settings

**Branch**: `031-invite-email-settings` | **Date**: 2026-11-25 | **Spec**: [spec.md](./spec.md)

## Summary

Ship editable research invite e-mail templates (inicial / lembrete / agradecimento) with `{{variables}}`, admin **date-and-time** settings for collection window and reminder, and automatic **inicial** send after the first complete profile save. Version **0.31.0**.

## Technical approach

| Area | Change |
|---|---|
| DB | `V13__invite_email_settings.sql`: config invite_* columns (`DATETIME(6)` for start/end/reminder); `email_template`; participant invite_*_sent_at. |
| Mail resources | `classpath:mail/inicial.{subject,body}.txt` (and lembrete, agradecimento) seeded into `email_template`. |
| Backend | `InviteEmailSettings` + repository; `InviteMailer` (JavaMailSender + substitution); hook after profile save in `SkillService` (send after successful update when inicial not yet sent). |
| API | Admin GET/PUT `/api/v1/administrator/email-settings`; GET/PUT `/api/v1/administrator/email-templates/{kind}`. |
| Frontend | `/admin/email-settings` with datetime-local for the three moments + CAAE + URL + three template textareas; hub card. |
| Version | Bump `0.31.0` / `0.31.0+dev`. |

## Defaults (staging seed)

- URL: `https://diegoquirino.cloud`
- CAAE: empty or `CAAE [número]`
- Start: `2026-12-21T03:00:00Z` (21/12/2026 00:00 America/Recife ? ?03)
- End: `2027-01-09T02:59:00Z` (08/01/2027 23:59 ?03)
- Reminder: `2026-12-28T15:00:00Z` (mid-window suggestion)

## Out of scope

- Automatic bulk lembrete/agradecimento campaigns (settings + templates only).
- HTML e-mail; keep plain text like access-code mailer.
- Cloud / 1.0.
