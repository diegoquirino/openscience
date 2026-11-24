# Feature Specification: Organization autocomplete after 3 characters

**Feature Branch**: `030-org-autocomplete-min3`

**Created**: 2026-11-24

**Status**: Complete at **0.30.0** (1.0 still gated; no cloud deploy)

**Input**: With many `organization_other` values in the suggestions list, focusing the Outros(as) field opens the full autocomplete panel immediately and hurts readability. Suggestions MUST appear only after the participant has typed **at least 3 characters**, then as a filtered list. Local staging needs a seed of **50** fictional organizations (including named Paraíba institutions and two companies) so the behaviour can be tested manually.

**Depends on**: 025, 029

## User Scenarios

### US1 - No panel dump on focus (P1)

1. Given organization = other and the suggestions API returned many names, When the participant focuses or clicks the organization name field with fewer than 3 characters typed, Then the autocomplete list is empty (no full dump of all seeded names).
2. Given at least 3 characters typed, When the text matches existing suggestions, Then the panel shows the filtered matches (cap as today, e.g. 20).
3. Given fewer than 3 characters again (delete), When the query shrinks, Then the list empties again.

### US2 - Staging seed for manual test (P1)

1. Given a clean local staging database, When Flyway staging migrations apply, Then at least 50 distinct `organization_other` values exist from seed participants, including:
   - UFPB - Universidade Federal da Paraíba  
   - IFPB - Instituto Federal da Paraíba  
   - UEPB - Universidade Estadual da Paraíba  
   - UNIFACISA - Centro Universitário Unifacisa  
   - Ingenico do Brasil Ltda  
   - Viceri Solution Ltda  
2. Given a new real participant on Outros(as), When they type three letters that match a seed (e.g. `UFP` / `Vic` / `Ing`), Then those names appear in the filtered list.

### US3 - Typing a new name still allowed (P1)

1. Given organization = other, When the participant types a new name (with or without matching suggestions), Then Continue/Save rules remain `organizationAnswerValid` (non-blank, ? 100 code points); suggestions never become mandatory.

## Requirements

- **FR-001**: Skill and profile screens: `filteredSuggestions` returns no options while trimmed query length &lt; 3; for length ? 3, filter case-insensitive contains as today.
- **FR-002**: Shared constant (e.g. `ORGANIZATION_SUGGESTION_MIN_CHARS = 3`) in organization labels helper; unit tests cover &lt;3 empty / ?3 filtered.
- **FR-003**: Staging-only Flyway seed (not production `db/migration`) inserting participants with `organization=other` and 50 distinct `organization_other` values including the six named above.
- **FR-004**: Update `FixtureLocationTest` (or equivalent) that currently expects only `V6` in `db/staging`.
- **FR-005**: App version **0.30.0**; 1.0 gated; no cloud; clean local staging at the end for manual test.

## Success Criteria

- **SC-001**: Unit tests: filtered list empty with 0–2 chars; non-empty after 3+ when matches exist.
- **SC-002**: Clean staging shows ?50 suggestion values via API; typing 3 letters filters without dumping the full list on focus.
- **SC-003**: ROADMAP 030 Complete at 0.30.0.
