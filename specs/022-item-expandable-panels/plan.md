# Plan: 022-item-expandable-panels

## Summary

Restructure `item.component` template into five `mat-expansion-panel`s (or same pattern used elsewhere): change, impact, operation, comment, actions. All expanded by default. Keep progress bar / expiry warning outside the panels. Version **0.22.0**.

## Technical approach

- Wrap existing sections in `MatExpansionModule` panels; titles via `$localize`.
- `[expanded]="true"` initially (or leave default expanded); do not gate answers on panel order.
- Actions panel holds Continue (and later Cancel from 023 — leave a clear slot).
- Update SCSS for panel spacing; preserve phone layout (stacked columns).
- Adjust Vitest queries (legend still in DOM when expanded); add `data-testid` on each panel.
- e2e: expand if needed before interacting; prefer always-open default so existing flows keep working.

## Contracts

No API/HAL changes.

## Target platform

Local Docker staging; production Cloud Run (deploy only with approval).
