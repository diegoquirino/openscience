# Plan: 030-org-autocomplete-min3

**Branch**: `030-org-autocomplete-min3` | **Date**: 2026-11-24 | **Spec**: [spec.md](./spec.md)

## Summary

Require **3 typed characters** before showing organization autocomplete options on skill/profile; seed **50** fictional `organization_other` values in staging for manual verification. Version **0.30.0**.

## Technical approach

| Area | Change |
|---|---|
| Labels helper | `ORGANIZATION_SUGGESTION_MIN_CHARS = 3` + `filterOrganizationSuggestions(query, all)`. |
| Frontend | `skill.component.ts` / `profile.component.ts` use the helper in `filteredSuggestions`. |
| Tests | Unit tests on helper + skill component behaviour for min length. |
| Staging DB | `db/staging/V12__organization_other_seed.sql`: 50 participants with `organization='other'` and distinct names (include UFPB/IFPB/UEPB/UNIFACISA/Ingenico/Viceri). |
| Fixture test | `FixtureLocationTest` expects V6 + V12. |
| Version | Bump `0.30.0` / `0.30.0+dev`. |

## Out of scope

- Changing suggestion API pagination.
- Cloud / 1.0.
- Production seed of organizations.
