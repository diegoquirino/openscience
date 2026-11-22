# Feature Specification: MIT footer and 1.0 promote gate

**Feature Branch**: `024-release-1.0`

**Created**: 2026-11-21

**Status**: Complete at **0.24.0** (footer + gate; product **1.0.0** not released)

**Input**: Keep the shell footer (spec 003 FR-003) and present the deployed version in a discreet MIT/copyleft style together with the license name. Prepare the path to product **1.0.0**, but **do not** treat 1.0 as released until the researcher explicitly commands **promover a 1.0 e deploy** on the last automatically tested stable build — after the CEP/LGPD consent text has been read and approved for production.

**Depends on**: 003, 023

## User Scenarios

### US1 - Visitor sees version and license in the footer (P1)

1. Given any locale page with the shell, When the footer renders, Then it shows the build version and the MIT License mention in muted typography.
2. Given a new deployment with a new `VEREDITO_VERSION`, When the image is served, Then the footer reflects that version (build-time constant).

### US2 - 1.0 promote is gated (P1)

1. Given the CEP/LGPD consent text is still unread or not approved for production, When any feature ships, Then the app version remains on the `0.<spec>.0` line (this slice: **0.24.0**).
2. Given the last auto-tested stable build and an explicit researcher command **promover a 1.0 e deploy**, When that command is given, Then defaults become **1.0.0**, CI/tag/`deploy/1.0.0-*` may run, and only then is product 1.0 considered released.

## Requirements

- FR-001: App version defaults for this slice MUST be **0.24.0** (`0.24.0+dev` local/staging). If the tree was prematurely set to `1.0.0`, revert to `0.24.0` before closing this slice.
- FR-002: The shell footer MUST remain; it MUST show a short copyleft-style line with the deployed version and **MIT License** (name kept as proper noun in locales unless a locale already uses “Licença MIT”).
- FR-003: Versioning policy: continue **`0.<spec>.0`** until the explicit **promover a 1.0 e deploy** command. After that promote, later features use **`1.x.y`**.
- FR-004: No cloud deploy of 1.0 without that explicit command; local staging must verify the footer on `0.24.0`.

## Success Criteria

- SC-001: Defaults and footer show `0.24.0` (or `0.24.0+…` with build metadata) until the promote command.
- SC-002: Unit test asserts footer contains the build version and `MIT`.
- SC-003: i18n complete for the footer string in pt-BR, en-US, es-ES.
- SC-004: ROADMAP and plan state that **1.0.0** is blocked on TCLE/LGPD read + explicit promote+deploy.
