# Plan: 034-full-usecase-inline

**Branch**: `034-full-usecase-inline` | **Date**: 2026-11-25 | **Spec**: [spec.md](./spec.md)

## Summary

Show the **full use case** (with `<- edited` highlights) in each Before/After column on the item screen; add a **Today** shortcut on admin date pickers. Version **0.34.0**. Display-only.

## Technical approach

| Area | Change |
|---|---|
| `item-layout.ts` | Attach `UseCaseView` from `originUsecase`/`targetUsecase` to each column; headings stay before/after (or kind-specific); primary body = use case lines. |
| `item.component.html` / scss | Render use case lines (reuse dialog styles); keep open-dialog button for enlarge. |
| Tests | Update `item-layout.spec` / `item.component.spec` for full UC display. |
| Email settings | Today button next to each datepicker. |
| Version | Bump `0.34.0` / `0.34.0+dev`. |

## Out of scope

- Changing import pipeline, DB, or item API.
- Cloud / 1.0.
