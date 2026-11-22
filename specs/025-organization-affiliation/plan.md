# Plan: 025-organization-affiliation

**Branch**: `025-organization-affiliation` | **Date**: 2026-11-21 | **Spec**: [spec.md](./spec.md)

## Summary

Add required **organization / employer** on the participant profile (skill screen + profile menu): radios `tjpb` | `ufcg` | `other`, with free text + autocomplete of other participants’ `organizationOther` values when `other`. Version **0.25.0**. Depends on **024 Complete** (footer at 0.24.0; 1.0 still gated).

## Prerequisites

1. Spec **024** tasks T001–T005 done (app on `0.24.0`, MIT footer, ROADMAP updated).
2. No start of 025 implementation until 024 is marked Complete in ROADMAP.
3. Product **1.0.0** remains deferred until **promover a 1.0 e deploy**.

## Technical approach

| Area | Change |
|---|---|
| DB | Flyway `V9__organization.sql`: `organization VARCHAR(16)`, `organization_other VARCHAR(100)`, CHECKs mirroring skill-other |
| Domain | `Organization` enum + `OrganizationChoice` validate; extend `OnboardingState` / `OnboardingRules` (null org ? `profile`) |
| API | Extend `SkillRequest` / `SkillModel` / `SkillService.updateProfile`; HAL `vd:organization-suggestions` (GET distinct others’ `organization_other`) |
| Export | Long/wide columns for organization (+ other when applicable) |
| UI | Skill + profile: radio group + MatAutocomplete for other; labels i18n |
| Docs | Amend 001 appendix B; GLOSSARY; ROADMAP 025 |

## Contracts

- PUT skill/profile body: `name`, `skill`, `skillOther`, `organization`, `organizationOther`
- GET suggestions: list of strings (no PII beyond the free-text org names participants chose to share as affiliation)

## Target platform

Local Docker staging/dev only. No cloud deploy without approval; no 1.0 promote in this slice.
