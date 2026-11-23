# Plan: 026-admin-display-name

**Branch**: `026-admin-display-name` | **Date**: 2026-11-23 | **Spec**: [spec.md](./spec.md)

## Summary

Seed / backfill `config.admin_name` to **Carlos Diego Quirino Lima** (FR-050 identity) so the administrator is never created with a null display name under e-mail-only sign-in. Version **0.26.0**. Depends on **025 Complete**.

## Prerequisites

1. Spec **025** Complete (app on `0.25.0`).
2. Product **1.0.0** remains deferred until **promover a 1.0 e deploy**.

## Technical approach

| Area | Change |
|---|---|
| DB | New Flyway `V11__admin_display_name.sql`: `UPDATE config SET admin_name = 'Carlos Diego Quirino Lima' WHERE id = 1 AND (admin_name IS NULL OR TRIM(admin_name) = '')`. Do **not** rewrite V2/V3 (already applied in deployed/staging histories). |
| Tests | Update `MigrationTest` (and any assert of null default `admin_name` after migrate). Keep AdministratorAccessTest behaviour that e-mail-only sign-in does not invent a name from the code when name was cleared. |
| Version | Bump to `0.26.0` / `0.26.0+dev` (pom, package.json/lock, compose, Dockerfile, SystemVersion, write-build-info). |
| Docs | ROADMAP row 026; `.specify/feature.json` ? this feature. |

## Out of scope

- Changing access-code UX to collect administrator name again.
- Cloud deploy / 1.0 promote.
- Retroactive Spec Kit packaging of the TCLE acknowledgment-anonymity UI already shipped in `6b4a9c0` (document only if a later slice needs it).

## Target platform

Local Docker staging/dev only.
