# Plan: 041-suite-step-marks

**Branch**: `041-suite-step-marks` | **Date**: 2026-11-26 | **Spec**: [spec.md](./spec.md)

## Summary

Fine-grained `<- edited` marks in the per-UC suite TXT (steps / PRE / POST only) and **Enlarge test suite** per column. Version **0.41.0**.

## Technical approach

| Area | Change |
|---|---|
| Pipeline | `render_txt(..., mark_sids=...)`; `veredito_import` passes edit SIDs; regenerate vazios + fixture. |
| Frontend | Suite Enlarge buttons; dialog `kind: 'usecase' \| 'suite'`; i18n. |
| Version | `0.41.0` / `0.41.0+dev`. |

## Out of scope

- Redesign of Change panel; CLARET on suite TXT; cloud / 1.0.
