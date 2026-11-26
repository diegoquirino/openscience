# Plan: 040-test-suite-panel

**Branch**: `040-test-suite-panel` | **Date**: 2026-11-26 | **Spec**: [spec.md](./spec.md)

## Summary

Show Before/After of the per-UC manual test suite TXT (edit-linked `<- edited` marks) before the Maintenance operation panel. Version **0.40.0**.

## Technical approach

| Area | Change |
|---|---|
| Import / DB / API | Columns `origin_tests` / `target_tests`; Flyway; repository; HAL item. |
| Frontend | New expansion panel; reuse line-mark layout without CLARET tokens. |
| Pipeline | `veredito_import.py` + regenerate `veredito_*_vazio.csv`. |
| Version | `0.40.0` / `0.40.0+dev`. |

## Out of scope

- Operation question/labels; full-suite diff marks; cloud / 1.0; redesign of Change (UC) panel.
