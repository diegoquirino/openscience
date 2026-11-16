# Implementation Plan: Data collection progress dashboard

**Branch**: `010-progress-dashboard` | **Date**: 2026-11-16 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/010-progress-dashboard/spec.md`

## Summary

Spec 010 turns the administrator's **Dashboard** menu into a read-only progress view on top of
the blocks/tasks of 005–007, the onboarding fields of 006, and the e-mail quota of 004.

- **Queue progress:** per system and per import batch: items, blocks, and role-1 / role-2 tasks
  free / in progress / completed, with percentage completed; overall total row and the queue
  position of the first free (or reclaimable) task.
- **Reservations:** active and expired reservations with pseudonym, block, role, draft count and
  expiry (never name or e-mail).
- **Participants & quota:** status and skill totals; completed tasks per pseudonym with share;
  preferred-language and final-response language counts for pt-BR / en-US / es-ES (zeros included);
  e-mails sent in the rolling 24 h window and remaining against `config.daily_email_limit`
  (highlight when fewer than 10 remain).
- **HAL:** `vd:dashboard` on the entry point; single `GET /api/v1/dashboard` returns the snapshot.
- **UI:** `/admin/dashboard`; Dashboard menu item enabled; Refresh reloads the snapshot without a
  full page reload; empty study shows zeros and a shortcut to Import.

No Flyway migration. Decisions are in [research.md](research.md).

## Technical Context

**Language/Version**: Java 25 LTS with Spring Boot 4.1.2; TypeScript with Angular 22.2.6; Node.js
26 LTS for the build

**App version**: **0.10.0**. Deploy tags: `deploy/0.10.0-*`.

**Primary Dependencies**:
- **Back end:** as in 009 (Spring HATEOAS, JDBC, springdoc-openapi).
- **Front end:** as in 009 (Angular Material, `@angular/localize`).

**Storage**: MySQL 9.7; read-only over existing tables ([data-model.md](data-model.md))

**Testing**:
- **Domain:** JUnit 5 for status buckets, percentages, language tallies and quota remaining.
- **API:** REST Assured `DashboardTest`; `AuthorizationTest` / `EntryPointTest` updated.
- **Front end:** Vitest for the dashboard screen; Playwright admin smoke (numbers + refresh;
  participant cannot open it).

**Target Platform**: Cloud Run + Cloud SQL; Docker Compose `staging`; GitHub-hosted Ubuntu runners

**Project Type**: web application

**Performance Goals**: dashboard opens under 5 s with 500 items and 100 participants (SC-002);
interactive API under 2 s

**Constraints**:
- Admin only (FR-006).
- Pseudonym only — no name/e-mail (FR-005).
- Read-only: opening / refreshing MUST NOT change reservations or responses (FR-007).
- No DDL; rollback to 0.9.0 still starts.
- Deploy by tag only with explicit researcher approval.

**Scale/Scope**:
- One administrator; hundreds of items; ~100 participants in SC-002.
- One new screen; one HAL resource (index = snapshot).
- No new Problem Details type.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

Constitution 2.7.1.

| Rule | How the plan meets it | Status |
|---|---|---|
| I. Stack standard; versions recorded | 0.10.0; no new dependency | Pass |
| I. MessageSource | UI via XLIFF; no new Problem type | Pass |
| I. REST level 3 | `vd:dashboard`; client follows the link ([contracts/dashboard.md](contracts/dashboard.md)) | Pass |
| I. OpenAPI complete | Dashboard under Administration | Pass |
| II. Domain plain Java | `domain.dashboard` for tallies / percentages / status buckets | Pass |
| II. MySQL + REST Assured from `/api/v1` | `DashboardTest` follows `vd:dashboard` | Pass |
| II. i18n completeness | New XLIFF units in three locales | Pass |
| III. Transactions; Flyway only | Read-only; no migration | Pass |
| IV. Staging/tests/pilot Minitest | Fixtures reuse Minitest import | Pass |
| V. Single artifact; Material | `/admin/dashboard` | Pass |
| Language / data untranslated | Skill codes English; languages as stored codes | Pass |
| Workflow: staging then tag deploy | Quickstart; `deploy/0.10.0-*` with approval | Pass |
| UTF-8 | As in 003–009 | Pass |

No violation.

**Post-design re-check**: no new secrets; Administration matcher includes `/api/v1/dashboard/**`;
FR-005 enforced in the JSON model (no name/email fields).

## Risks

| Risk | Mitigation |
|---|---|
| Expired `reserved` counted as in progress | Research §3: progress counts treat expired reserved as free |
| SC-001 / SC-003 drift vs long export | Assert against long-table counts in `DashboardTest` |
| Heavy GROUP BY at SC-002 scale | Single read-only snapshot query set; measure in staging |
| Participant status ambiguity | Research §5 locked buckets from consent / screening / eligibility |

## Project Structure

### Documentation (this feature)

```text
specs/010-progress-dashboard/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   ├── dashboard.md
│   └── web.md
└── tasks.md
```

### Source Code (`Veredito` branch)

```text
backend/src/main/java/.../
├── domain/dashboard/          # tallies, percentages, status buckets
├── dashboard/                 # DashboardQuery, DashboardService
└── api/v1/DashboardController.java

frontend/src/app/admin/dashboard/  # dashboard.component + service
```

**Structure Decision**: mirror 009 (`domain.*` + application package + HAL controller + one admin
screen). No Option 2/3 layout.

## Complexity Tracking

> None — no constitution violations.
