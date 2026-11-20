---
description: "Tasks of spec 012: impact answer labels and required markers"
---

# Tasks: Change-impact answer labels and required markers

**Input**: documents in `specs/012-impact-answer-labels/`

**Prerequisites**: 008 complete; Docker for staging verify

**Tests**: Vitest (labels, item legends); CI e2e as applicable

**Organization**: version; labels; required markers; docs; staging/CI; polish. Deploy only with explicit approval.

## Format: `[ID] [P?] [Story] Description`

---

## Phase 1: Setup

- [ ] T001 Bump app to `0.12.0` (pom, package.json/lock, compose, Dockerfile, SystemVersion.FALLBACK, write-build-info.mjs, OpenAPI example if needed)
- [ ] T002 Set `spec.md` Status to In progress; ROADMAP 012 to In progress (0.12.0)

---

## Phase 2: User Story 1 — No/Yes labels

- [ ] T003 [US1] Update `changeImpact.LOW` / `HIGH` short labels in `response-labels.ts` and XLIFF pt-BR/es-ES
- [ ] T004 [US1] Update Appendix B.1 display option names in `specs/001-oracle-labeling/spec.md` (codes unchanged)
- [ ] T005 [US1] Vitest: assert No/Yes labels; review still uses `changeImpactLabel`

---

## Phase 3: User Story 2 — Required markers

- [ ] T006 [US2] Add required marker to impact and operation legends on the item screen (i18n three locales)
- [ ] T007 [US2] Vitest: legends include required marker; comment remains optional

---

## Phase 4: Staging and deployment

- [ ] T008 Frontend `i18n:check`, `test`, `build`
- [ ] T009 Push; wait for `ci.yml`; deploy `deploy/0.12.0-1` **only after explicit researcher approval**; verify production

---

## Phase 5: Polish

- [ ] T010 [P] UTF-8 check on changed folders
- [ ] T011 [P] Secret scan deferred to `ci.yml`
- [ ] T012 Mark Complete in spec/ROADMAP/001; point `.specify/feature.json` at `specs/013-email-first-onboarding`

## Dependencies

- T003 before T005; T006 before T007; T009 last for Complete
