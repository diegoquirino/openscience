# Plan: 028-lgpd-controller

**Branch**: `028-lgpd-controller` | **Date**: 2026-11-23 | **Spec**: [spec.md](./spec.md)

## Summary

Update TCLE LGPD **Controlador** from UFCG (CNPJ) to **Carlos Diego Quirino Lima** (PF) in the staging consent fixture (three languages) and the CEP draft mirror. Adjust Encarregado wording. Version **0.28.0**. Depends on **027 Complete**.

## Prerequisites

1. Spec **027** Complete (app on `0.27.0`).
2. Product **1.0.0** remains deferred until **promover a 1.0 e deploy**.

## Technical approach

| Area | Change |
|---|---|
| Fixture | Edit `backend/src/main/resources/db/staging/V6__consent_form_fixture.sql` section VIII (pt-BR) and Controller/Responsable lines (en-US/es-ES). |
| CEP | Same Controlador + Encarregado in `ISSTA_2027/CEP-TCLE/03_tcle_virtual/TCLE_ambiente_virtual_pt-BR.md`. |
| Version | Bump to `0.28.0` / `0.28.0+dev`. |
| Docs | ROADMAP 028; `.specify/feature.json` ? this feature. |
| Verify | `docker compose --profile staging down -v` then `up --build` with `APP_PORT=8200`; query `consent_form_text`. |

## Out of scope

- Cloud deploy / 1.0 promote.
- Changing CEP committee contact (XIII).
- Production Flyway migrations for consent text.

## Target platform

Local Docker staging/dev only.
