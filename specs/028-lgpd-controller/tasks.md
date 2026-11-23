# Tasks: 028-lgpd-controller

**Input**: Design documents from `/specs/028-lgpd-controller/`

**Prerequisites**: [027-acknowledgment-choice](../027-acknowledgment-choice/tasks.md) Complete (app at `0.27.0`; 1.0 still gated). Execute **strictly in task order**.

## Phase 1: Setup

- [x] T001 Bump app to `0.28.0` (pom, package.json/lock, compose, Dockerfile, SystemVersion, write-build-info)
- [x] T002 Point `.specify/feature.json` at `specs/028-lgpd-controller`; add 028 to ROADMAP (after 027 Complete)

## Phase 2: TCLE copy

- [x] T003 Staging fixture `V6__consent_form_fixture.sql`: Controlador PF + Encarregado (pt-BR, en-US, es-ES) (FR-001, FR-002)
- [x] T004 Mirror Controlador + Encarregado in CEP `TCLE_ambiente_virtual_pt-BR.md` (FR-003)

## Phase 3: Polish

- [x] T005 Local staging clean reload; verify section VIII (SC-001); mark ROADMAP 028 Complete; push CI; **no** cloud deploy; **no** 1.0 until **promover a 1.0 e deploy**
