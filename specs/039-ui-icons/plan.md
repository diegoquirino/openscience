# Plan: 039-ui-icons

**Branch**: `039-ui-icons` | **Date**: 2026-11-26 | **Spec**: [spec.md](./spec.md)

## Summary

Add Material Symbols Outlined icons to titles and action buttons across participant and admin UI. Version **0.39.0**.

## Technical approach

| Area | Change |
|---|---|
| `styles.scss` | `.page-title`, `.page-title-icon`, `.section-title`, button `mat-icon` spacing. |
| Templates | Glyph map from feature plan; `MatIconModule` imports. |
| i18n | Text on inner spans; `i18n:check`. |
| Tests | Query title/button text via span or normalized trim. |
| Version | `0.39.0` / `0.39.0+dev`. |

## Out of scope

- Radios, form fields, toasts; shell menu (already iconed); cloud / 1.0.
