# Feature Specification: Bold radio option labels (impact & operation)

**Feature Branch**: `037-radio-option-label-weight`

**Created**: 2026-11-25

**Status**: Complete

**Input**: On the item labeling screen, each **radio option label** under Change impact and Maintenance operation MUST be more visually prominent (bold), so the short choice name stands out from the supporting explanation text underneath. Display-only; no API or wording change.

**Depends on**: 008, 012

## User Scenarios

### US1 - Bold labels, normal explanations (P1)

1. Given the Change impact panel, When the rater views LOW/HIGH options, Then each radio’s short label is bold and the explanation under it is not bold.
2. Given the Maintenance operation panel, When the rater views the eight operations, Then each radio’s short label is bold and each explanation stays regular weight.
3. Given either panel, When the accessible name is read, Then it remains the short label text (no extra words).

## Requirements

- **FR-001**: Radio labels for impact and operation options use bold weight (e.g. `font-weight: 700` via a dedicated class on the label text).
- **FR-002**: Explanation paragraphs under options keep the existing (non-bold) body style.
- **FR-003**: No change to stored codes, i18n strings content, or API.
- **FR-004**: App version **0.37.0**; 1.0 gated; no cloud; clean local staging.
- **FR-005**: Component test asserts the option-label class (or equivalent) on impact and operation radios.

## Success Criteria

- **SC-001**: Every impact and operation `mat-radio-button` shows its label through a bold-styled element; explanations lack that class.
- **SC-002**: ROADMAP 037 Complete at 0.37.0; staging clean for manual test.
