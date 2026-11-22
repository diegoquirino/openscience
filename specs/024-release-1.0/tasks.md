# Tasks: 024-release-1.0

**Input**: Design documents from `/specs/024-release-1.0/`

**Gate**: Do **not** bump to `1.0.0` or cloud-deploy as 1.0 until the researcher says **promover a 1.0 e deploy** (after TCLE/LGPD read). This slice stays at **0.24.0**.

## Phase 1: Setup

- [x] T001 Set app defaults to `0.24.0` (revert premature `1.0.0` if present: pom, package.json/lock, compose, Dockerfile, SystemVersion, write-build-info)
- [x] T002 Point `.specify/feature.json` at `specs/024-release-1.0`; ROADMAP lists 024 with 1.0 promote gate

## Phase 2: Footer

- [x] T003 MIT-style footer copy + styles
- [x] T004 Vitest + i18n extract/check

## Phase 3: Polish

- [x] T005 Local staging clean on `0.24.0`; mark ROADMAP 024 Complete (footer + gate); push CI; **no** `1.0.0` / cloud deploy without **promover a 1.0 e deploy**

## Deferred (not in this slice)

- [ ] T006 **Only on explicit command**: bump to `1.0.0`, tag `deploy/1.0.0-*`, production deploy
