# Feature Specification: Organization / employer affiliation

**Feature Branch**: `025-organization-affiliation`

**Created**: 2026-11-21

**Status**: Complete at **0.25.0** (1.0 still gated; no cloud deploy)

**Input**: On the participant profile step (and later profile edit), add a required field **Instituição/Empresa onde estuda/trabalha** as a single choice among fixed options, with **Outros(as)** opening a free-text field that is an autocomplete of organization names previously entered by other participants.

**Depends on**: 013, 014, 024

## User Scenarios

### US1 - Choose organization during profile (P1)

1. Given the profile step (name + skill), When the screen is shown, Then the participant must also choose **one** organization option before Continue is enabled.
2. Given the options, When they choose **TJPB** or **UFCG**, Then no free-text field is shown and `organizationOther` is cleared/absent.
3. Given **Outros(as)**, When chosen, Then a text field appears and is required (not blank; max length aligned with skill-other, 100 code points).
4. Given **Outros(as)** with the text field focused or typed, When suggestions load, Then the field offers autocomplete of distinct `organizationOther` values already stored for **other** participants (not the caller’s own unsaved typing alone); the participant may still type a new name not in the list.
5. Given a valid name, skill and organization, When they Continue, Then the API stores organization code (and other text when applicable) with the profile and onboarding advances as today.

### US2 - Edit organization later (P1)

1. Given a participant who already completed profile, When they open Profile from the account menu, Then they see and can change organization the same way as on the skill screen.
2. Given Save, When successful, Then exports and any admin views that show affiliation use the updated value.

### US3 - Existing participants without organization (P1)

1. Given a participant who completed profile before this feature (organization null), When they sign in, Then onboarding treats the profile step as pending again until organization is set (same gate as missing name/skill).

## Requirements

- **FR-001**: Stored codes (English identifiers, UI translated): `tjpb`, `ufcg`, `other`. Display labels (pt-BR reference):  
  - `tjpb` — TJPB - Tribunal de Justiça do Estado da Paraíba  
  - `ufcg` — UFCG - Universidade Federal de Campina Grande  
  - `other` — Outros(as)  
  (en-US / es-ES equivalents required in i18n.)
- **FR-002**: Field label (pt-BR): **Instituição/Empresa onde estuda/trabalha**; required on profile create and update when the participant is on the profile step or editing profile.
- **FR-003**: With `other`, `organizationOther` is required, not blank, ? 100 code points; with `tjpb`/`ufcg`, `organizationOther` must be absent/null.
- **FR-004**: Autocomplete for `organizationOther` MUST list distinct non-blank values already saved by other participants for `organization = other` (case-preserving storage; suggestion matching may be case-insensitive prefix/contains). New names remain allowed.
- **FR-005**: HAL/OpenAPI: extend skill/profile resource and PUT body with `organization` / `organizationOther`; add a read link (e.g. `vd:organization-suggestions`) or query on the skill resource for suggestion strings.
- **FR-006**: Flyway migration on `participant`; amend onboarding rule so missing organization ? step `profile`.
- **FR-007**: Long/wide exports include organization (code; and other text when `other`) without name/e-mail.
- **FR-008**: App version **0.25.0** (continue `0.<spec>.0` until an explicit researcher command **promover a 1.0 e deploy** on the last auto-tested stable build). Spec 024 prepared the 1.0 *policy and footer*, but product **1.0.0 must not** be treated as released while the CEP/LGPD consent text is still unread/unapproved for production. No cloud deploy without that explicit command; verify on local staging/dev only.
- **FR-009**: Amend overview [001](../001-oracle-labeling/spec.md) appendix / participant entity briefly to document the new codes (cross-link this spec).

## Success Criteria

- **SC-001**: Backend tests: validation matrix (required / option / other required / size / unexpected other); onboarding returns `profile` when organization is null; suggestions exclude caller-only drafts and return distinct others’ values.
- **SC-002**: Frontend: radios + conditional other + autocomplete on skill and profile screens; i18n pt-BR / en-US / es-ES.
- **SC-003**: e2e or component smoke: select Outros(as), type, see suggestion path; select TJPB and submit without other.
- **SC-004**: Staging Docker loads migration; fixture/consent unchanged; production migration path has no CEP TCLE change.
