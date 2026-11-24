# Plan: 029-org-autocomplete-continue

**Branch**: `029-org-autocomplete-continue` | **Date**: 2026-11-23 | **Spec**: [spec.md](./spec.md)

## Summary

Wire `mat-autocomplete` `(optionSelected)` to `onOrganizationOtherInput` on skill and profile so picking an existing suggestion updates the signal and enables Continue/Save. Version **0.29.0**.

## Technical approach

| Area | Change |
|---|---|
| Frontend | `skill.component.ts` and `profile.component.ts`: `(optionSelected)="onOrganizationOtherInput($event.option.value)"` on `mat-autocomplete`. |
| Tests | Extend `skill.component.spec.ts` (and profile if present) for optionSelected ? valid Continue. |
| Version | Bump `0.29.0` / `0.29.0+dev`. |
| Docs | ROADMAP 029; `.specify/feature.json`. |

## Out of scope

- Changing organization codes or suggestion API.
- Cloud / 1.0.
