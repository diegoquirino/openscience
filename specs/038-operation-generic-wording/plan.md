# Plan: 038-operation-generic-wording

**Branch**: `038-operation-generic-wording` | **Date**: 2026-11-25 | **Spec**: [spec.md](./spec.md)

## Summary

Rewrite maintenance-operation explanations without “test case” or “scenario” vocabulary. Version **0.38.0**. Copy-only.

## Technical approach

| Area | Change |
|---|---|
| `question-texts.ts` | New EN strings for KEEP, UPDATE, REMOVE, CREATE, MERGE, SPLIT, FLAG. |
| XLF | Sync `messages.xlf`, `pt-BR`, `es-ES`. |
| e2e | Sync `appendix-texts.ts`. |
| Tests | Update `question-texts.spec.ts`. |
| Version | Bump `0.38.0` / `0.38.0+dev`. |

## Out of scope

- Panel question, screening, impact texts, Appendix B.2 in spec 001.
- Cloud / 1.0.
