# Plan: 036-claret-keyword-highlight

**Branch**: `036-claret-keyword-highlight` | **Date**: 2026-11-25 | **Spec**: [spec.md](./spec.md)

## Summary

IDE-style CLARET highlighting: language keywords vs quoted developer text on the item screen and enlarge dialog. Version **0.36.0**. Display-only.

## Technical approach

| Area | Change |
|---|---|
| `usecase-text.ts` | `tokenizeClaret(content)` + attach `tokens` on each `UseCaseLine`; keywords = thesis CLARET list (case-sensitive); strings = `"..."`; remainder = `text`. |
| Templates / scss | Render token spans (`.claret-keyword`, `.claret-string`); keep 035 edit classes. |
| Dialog | Same token rendering. |
| Tests | Tokenizer unit tests; update item/dialog specs for DOM classes. |
| Version | Bump `0.36.0` / `0.36.0+dev`. |

## Out of scope

- Full CLARET parser, comments, or syntax validation.
- Cloud / 1.0.
