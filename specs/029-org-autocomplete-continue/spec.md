# Feature Specification: Enable Continue after picking an existing organization suggestion

**Feature Branch**: `029-org-autocomplete-continue`

**Created**: 2026-11-23

**Status**: Complete at **0.29.0** (1.0 still gated; no cloud deploy)

**Input**: On the profile / skill step, with organization = **Outros(as)**, choosing a name already suggested in the autocomplete (an organization previously entered by another participant) leaves **Continue** / **Save** disabled. Typing the same name by hand enables the button. Root cause: `mat-option` selection does not fire the input’s `(input)` handler, so `organizationOther` stays empty and `organizationAnswerValid` fails.

**Depends on**: 025, 028

## User Scenarios

### US1 - Pick existing organization suggestion (P1)

1. Given name and skill are valid and organization = other, When the participant selects a suggestion from the organization autocomplete, Then `organizationOther` becomes that suggestion and **Continue** (onboarding) / **Save** (profile) becomes enabled.
2. Given the same flow, When they submit, Then the API receives `organization=other` and `organizationOther` equal to the selected suggestion (as typed/selected).

### US2 - Typing still works (P1)

1. Given organization = other, When the participant types a new or existing name without using the panel, Then Continue/Save enable rules remain as today (`organizationAnswerValid`).

## Requirements

- **FR-001**: On skill and profile screens, `mat-autocomplete` MUST update `organizationOther` on `optionSelected` (in addition to `(input)`).
- **FR-002**: Unit test covers selecting a suggestion enabling Continue / recording the value.
- **FR-003**: App version **0.29.0**; 1.0 gated; no cloud; verify on clean local staging.

## Success Criteria

- **SC-001**: After selecting an autocomplete option, Continue is enabled when name+skill are valid.
- **SC-002**: ROADMAP 029 Complete at 0.29.0; clean local staging available.
