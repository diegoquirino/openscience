# Plan: 027-acknowledgment-choice

**Branch**: `027-acknowledgment-choice` | **Date**: 2026-11-23 | **Spec**: [spec.md](./spec.md)

## Summary

Replace the negative acknowledgment checkbox on the consent step with a **required positive radio pair** (list first+last name vs remain anonymous in acknowledgments). Keep storing `participant.acknowledgment_anonymous`. System applies the anonymous bucket for paper/thesis acknowledgments; admin anonymization screens stay for LGPD only. Version **0.27.0**. Depends on **026 Complete**.

## Prerequisites

1. Spec **026** Complete (app on `0.26.0`).
2. Column `acknowledgment_anonymous` already present (V10).
3. Product **1.0.0** remains deferred until **promover a 1.0 e deploy**.

## Technical approach

| Area | Change |
|---|---|
| Frontend | `consent.component.ts`: `MatRadioModule` group; signal `acknowledgmentChoice: 'named' \| 'anonymous' \| null`; disable Accept until non-null; map to boolean on `recordConsent`. Remove checkbox. |
| i18n | New units for legend + Sim + Não (en-US source in template; pt-BR / es-ES in locale XLF). Remove or stop using `consent.acknowledgmentAnonymous` checkbox unit. |
| TCLE fixture | Update `backend/src/main/resources/db/staging/V6__consent_form_fixture.sql` section VII (and en/es equivalents) to describe the radio choice. Local staging with `down -v` reloads V6; no production Flyway rewrite of applied V6. |
| Backend | Docs/OpenAPI wording only if needed; no new migration expected. |
| Version | Bump to `0.27.0` / `0.27.0+dev` (pom, package.json/lock, compose, Dockerfile, SystemVersion, write-build-info). |
| Docs | ROADMAP row 027; `.specify/feature.json` ? this feature; note 011/018 vs acknowledgment anonymity. |

## Out of scope

- Auto-running participant anonymization (`anonymized_at`) when Não is chosen.
- Changing admin anonymization UI (011/018).
- Cloud deploy / 1.0 promote.
- Building a full “acknowledgments export” API in this slice (flag remains the system contract).

## Target platform

Local Docker staging/dev only.
