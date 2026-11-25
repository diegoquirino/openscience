# Feature Specification: Localized date-time calendar in admin e-mail settings

**Feature Branch**: `032-datetime-picker-locale`

**Created**: 2026-11-25

**Status**: Complete at **0.32.0** (1.0 still gated; no cloud deploy)

**Input**: On the administrator **E-mail send settings** page, the calendar / date-time control is presented in English even when the UI language is pt-BR or es-ES. Date and time pickers MUST follow the same locale as the rest of the app (pt-BR, en-US, es-ES), which is the common expectation for the signed-in user’s language.

**Depends on**: 031

## User Scenarios

### US1 - Calendar follows UI language (P1)

1. Given the administrator uses the **pt-BR** build, When they open a collection or reminder date control, Then month names, weekday headers and calendar chrome follow Portuguese (Brazil), not English browser defaults.
2. Given the **es-ES** build, When they open the same control, Then the calendar follows Spanish (Spain).
3. Given the **en-US** build, When they open the same control, Then the calendar stays in English (US).

### US2 - Date and time still save correctly (P1)

1. Given localized date + time fields, When the administrator saves send settings, Then values still round-trip as ISO-8601 UTC instants (same API as 031).
2. Given an empty date, When they save, Then that moment is sent as null (unchanged semantics).

## Requirements

- **FR-001**: Replace native `datetime-local` (OS/browser locale) with Angular Material datepicker driven by `MAT_DATE_LOCALE` / `LOCALE_ID`, plus an explicit time control, on `/admin/email-settings`.
- **FR-002**: Register / provide the native date adapter so Material calendar labels use the active locale; keep API payloads as ISO-8601 UTC.
- **FR-003**: App version **0.32.0**; 1.0 gated; no cloud; clean local staging for manual test.
- **FR-004**: i18n for any new visible strings (date / time field labels if split).

## Success Criteria

- **SC-001**: In pt-BR staging, opening the collection-start calendar shows Portuguese month/weekday labels.
- **SC-002**: Saving start/end/reminder still persists correct UTC instants.
- **SC-003**: ROADMAP 032 Complete at 0.32.0; staging clean for manual test.
