# Plan: 035-before-after-highlight-colors

**Branch**: `035-before-after-highlight-colors` | **Date**: 2026-11-25 | **Spec**: [spec.md](./spec.md)

## Summary

Use distinct highlight colors for edited lines on the **before** (origin) vs **after** (target) sides of the item screen and enlarge dialog. Version **0.35.0**. Display-only.

## Technical approach

| Area | Change |
|---|---|
| `item.component.html` / scss | Keep `.edited`; add `.highlight-before` / `.highlight-after` from `column.side`; style before with error-tint, after with tertiary-tint (Material sys tokens). |
| `usecase-dialog` | Host `data-side`; same highlight classes from `data.side`; matching scss. |
| Tests | Assert side-specific highlight classes on edited lines (item + dialog). |
| Version | Bump `0.35.0` / `0.35.0+dev` (pom, package, compose, Dockerfile, SystemVersion, write-build-info). |

## Out of scope

- Changing which lines are marked edited, pipeline markers, or DB.
- Cloud / 1.0.
