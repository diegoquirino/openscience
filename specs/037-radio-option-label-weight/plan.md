# Plan: 037-radio-option-label-weight

**Branch**: `037-radio-option-label-weight` | **Date**: 2026-11-25 | **Spec**: [spec.md](./spec.md)

## Summary

Bold the short radio labels for change impact and maintenance operation; leave explanations regular. Version **0.37.0**. Display-only.

## Technical approach

| Area | Change |
|---|---|
| `item.component.html` | Wrap `impactLabel` / `operationLabel` in `<span class="option-label">`. |
| `item.component.scss` | `.option-label { font-weight: 700; }` |
| Tests | Assert `.option-label` on both panels; explanations without it. |
| Version | Bump `0.37.0` / `0.37.0+dev`. |

## Out of scope

- Changing legend/panel title weight, precedence block, or comment field.
- Cloud / 1.0.
