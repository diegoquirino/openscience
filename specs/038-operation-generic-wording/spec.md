# Feature Specification: Generic maintenance-operation descriptions

**Feature Branch**: `038-operation-generic-wording`

**Created**: 2026-11-25

**Status**: Complete

**Input**: The eight maintenance-operation **explanations** under each radio MUST avoid both “test case” / “caso de teste” / “caso de prueba” and “scenario” / “cenário” / “escenario”, using slightly more generic wording focused on behavior and verification. Short labels and stored codes stay unchanged. The panel question (`item.operation.question`) stays unchanged.

**Depends on**: 008, 012, 037

## User Scenarios

### US1 - Generic explanations (P1)

1. Given the Maintenance operation panel, When the rater reads each option’s explanation, Then none of those texts contain “test case”, “caso de teste”, “caso de prueba”, “scenario”, “cenário”, or “escenario”.
2. Given KEEP through FLAG, When displayed in pt-BR, en-US or es-ES, Then the wording matches the approved generic copy (behavior / verification).
3. Given short radio labels (Keep, Update, …), When shown, Then they are unchanged.

## Requirements

- **FR-001**: Update EN sources in `question-texts.ts` and XLF `source`/`target` for KEEP, UPDATE, REMOVE, CREATE, MERGE, SPLIT, FLAG (REASSIGN unchanged).
- **FR-002**: Sync `e2e/support/appendix-texts.ts` with the same strings.
- **FR-003**: No change to codes, API, short labels, or `item.operation.question`.
- **FR-004**: App version **0.38.0**; 1.0 gated; no cloud; clean local staging.
- **FR-005**: Unit tests assert the new EN MERGE (and related) strings.

## Success Criteria

- **SC-001**: Operation explanations in the three locales use the generic copy without test-case/scenario vocabulary.
- **SC-002**: ROADMAP 038 Complete at 0.38.0; staging clean for manual test.
