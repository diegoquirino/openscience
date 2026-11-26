# Feature Specification: Test suite panel before maintenance operation

**Feature Branch**: `040-test-suite-panel`

**Created**: 2026-11-26

**Status**: Planned

**Input**: On the item labeling screen, Before choosing a maintenance operation, the rater MUST see the Before/After of the **manual test suite TXT for that use case** (evolution step), with `<- edited` markings only on lines affected by the **current edit**. New required CSV columns `origin_tests` / `target_tests` feed the panel; empty oracle CSVs are regenerated.

**Depends on**: 008, 022, 034, 035, 039

## User Scenarios

### US1 - See the UC test suite before the operation (P1)

1. Given an item with `origin_tests` and `target_tests`, When the rater opens the item screen, Then a **Test suite** expandable panel appears after Change impact and before Maintenance operation.
2. Given that panel, When shown, Then two columns show Before/After (or kind-specific headings) of the full suite TXT for that use case, with line highlights for `<- edited` (same colours as the Change panel), without CLARET keyword highlighting.
3. Given a line not marked by the current edit, When shown, Then it has no edit highlight.

### US2 - Import carries the suite texts (P1)

1. Given a CSV with required `origin_tests` and `target_tests`, When preview/confirm runs, Then those columns are stored on each item like use-case texts.
2. Given a missing or empty `origin_tests` / `target_tests`, When validated, Then the row is refused with the same class of errors as empty use cases.

## Requirements

- **FR-001**: Appendix D adds required `origin_tests` and `target_tests` (non-empty; default max 40?000 code points, same family as usecase; raise in this spec if regeneration exceeds).
- **FR-002**: Flyway adds `origin_tests` / `target_tests` `MEDIUMTEXT NOT NULL` on `item`; API and front `ItemBody` expose them.
- **FR-003**: New item panel **Test suite** between Impact and Operation; icon `list_alt`; i18n on text spans.
- **FR-004**: Pipeline `oracle-import` fills suite TXT per UC/step and marks only TCs whose steps intersect the edit SIDs; regenerates all `veredito_*_vazio.csv` and the minitest test fixture.
- **FR-005**: App version **0.40.0**; 1.0 gated; no cloud; clean local staging.

## Success Criteria

- **SC-001**: On an item, Test suite appears before Operation with marked lines only for the current edit’s affected TCs.
- **SC-002**: Import of regenerated empty CSVs succeeds; ROADMAP 040 Complete at 0.40.0; staging clean for manual test.
