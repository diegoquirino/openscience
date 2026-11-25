# Plan: 032-datetime-picker-locale

**Branch**: `032-datetime-picker-locale` | **Date**: 2026-11-25 | **Spec**: [spec.md](./spec.md)

## Summary

Make admin e-mail send **date and time** controls follow the UI locale (pt-BR / en-US / es-ES) by replacing native `datetime-local` with Material datepicker + time input wired to `MAT_DATE_LOCALE`. Version **0.32.0**.

## Technical approach

| Area | Change |
|---|---|
| App config | `provideNativeDateAdapter()` alongside existing `MAT_DATE_LOCALE` ? `LOCALE_ID`. |
| Optional intl | Locale-aware `MatDatepickerIntl` (open/prev/next labels) from `LOCALE_ID`. |
| Email settings UI | Three moments: `mat-datepicker` + `type="time"`; combine to ISO on save. |
| Service helpers | Split/join ISO ? `{ date: Date \| null, time: string }` (browser local zone). |
| Version | Bump `0.32.0` / `0.32.0+dev` (pom, package.json/lock, compose, Dockerfile, SystemVersion, write-build-info). |
| Docs | ROADMAP 032; `.specify/feature.json`. |

## Out of scope

- Changing invite send API or templates.
- Bulk lembrete/agradecimento jobs.
- Cloud / 1.0.
