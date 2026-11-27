# Plan: 042-one-block-per-system

**Branch**: `042-one-block-per-system` | **Date**: 2026-11-27 | **Spec**: [spec.md](./spec.md)

## Summary

At most one block per `system` per rater (reserved/completed) for kappa; Spec Kit golden rule; version **0.42.0**.

## Technical approach

| Area | Change |
|---|---|
| Domain | `NextTaskChooser` + `heldSystems` + systems on `Candidate`. |
| Persistence | `heldSystems`; enrich candidates with systems from `item`. |
| Service | Pass held systems on reserve and outcome-after-complete. |
| Docs / rule | Spec Kit `.mdc`; amend 007 FR-014; ROADMAP. |
| Version | `0.42.0` / `0.42.0+dev`. |

## Out of scope

- Kappa math in the pipeline; denormalized `block.system`; cloud / 1.0.
