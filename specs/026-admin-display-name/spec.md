# Feature Specification: Administrator default display name

**Feature Branch**: `026-admin-display-name`

**Created**: 2026-11-23

**Status**: Complete at **0.26.0** (1.0 still gated; no cloud deploy)

**Input**: The administrator row in `config` is created with `admin_email = diegoquirino@gmail.com` but without `admin_name`. Sign-in is e-mail-only (spec 013), so the name typed on the old code form is no longer stored at access-code time. The researcher identity in overview FR-050 / US6 is **Carlos Diego Quirino Lima**; that name MUST appear as the default administrator display name (header / `vd:me`) from a clean install and after migration of existing databases where `admin_name` is still null.

**Depends on**: 004, 013, 014, 025

## User Scenarios

### US1 - Default name on fresh install (P1)

1. Given a new database after Flyway migrations, When the administrator signs in with `diegoquirino@gmail.com`, Then the session and `GET /api/v1/administrator/me` show name **Carlos Diego Quirino Lima** without requiring a prior profile edit.
2. Given the same install, When the administrator opens the profile menu and changes the name, Then the new name is stored and shown (existing profile PUT behaviour).

### US2 - Existing databases with null admin_name (P1)

1. Given a database where `config.admin_name` is null, When migrations apply this slice, Then `admin_name` becomes **Carlos Diego Quirino Lima**.
2. Given `admin_name` already set to a non-blank value, When migrations apply, Then that value is left unchanged.

## Requirements

- **FR-001**: After this slice’s migration(s), `config.admin_name` for the single config row MUST be **Carlos Diego Quirino Lima** whenever it was null or blank; non-blank existing values MUST NOT be overwritten.
- **FR-002**: E-mail-only administrator sign-in MUST continue not to require a name on the access code; the seeded/default display name is sufficient until the administrator edits profile.
- **FR-003**: App version **0.26.0** (`0.<spec>.0`); product **1.0.0** remains gated until explicit **promover a 1.0 e deploy**. No cloud deploy in this slice; verify on local staging/dev.
- **FR-004**: Migration and repository/API tests that previously asserted `admin_name` null after migrate MUST expect the default name instead.

## Success Criteria

- **SC-001**: `MigrationTest` (or equivalent) asserts `admin_name = Carlos Diego Quirino Lima` after full migrate.
- **SC-002**: Local staging after clean volume shows administrator name on session/`vd:me` without manual profile save.
- **SC-003**: ROADMAP lists 026 Complete at 0.26.0; 1.0 still gated.
