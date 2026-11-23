# Feature Specification: Acknowledgment name choice (positive radios)

**Feature Branch**: `027-acknowledgment-choice`

**Created**: 2026-11-23

**Status**: Complete at **0.27.0** (1.0 still gated; no cloud deploy)

**Input**: On the consent step, the acknowledgment preference is today a single negative checkbox (“I want to remain anonymous…”). The researcher wants a **positive** choice with **two radio buttons**: (1) yes, list first and last names in paper/thesis acknowledgments; (2) no, remain anonymous in those acknowledgments, with a short explanation of the collective mention. **Either choice leaves the participant eligible to label.** Profile and labeling data are still collected; anonymity for acknowledgments is applied **by the system** (flag ? collective “Rotuladores Anonimizados por pedido [N]”), not by the administrator using the manual anonymization screen (specs 011/018).

**Depends on**: 006, 010 (acknowledgment_anonymous column from V10 / commit `6b4a9c0`), 026

## User Scenarios

### US1 - Explicit positive choice on consent (P1)

1. Given the consent screen with the current form text, When the participant has not yet chosen an acknowledgment option, Then **Agree to participate** stays disabled until one radio is selected (Refuse remains available without that choice).
2. Given the radios, When shown, Then the options are mutually exclusive and worded positively as (pt-BR reference):
   - **Sim**: «Sim, desejo ter o meu primeiro e último nomes constando nos agradecimentos do artigo e/ou tese de doutorado do autor.»
   - **Não**: «Não, desejo permanecer anônimo(a) nos agradecimentos do artigo e/ou tese de doutorado do autor. Nesse caso, o sistema inclui a pessoa na menção coletiva «Rotuladores Anonimizados por pedido [N]» (N = quantidade de pedidos), sem exigir anonimização manual pelo administrador.»
3. Given **Sim** selected and Agree, When consent is recorded, Then `acknowledgment_anonymous = false` and onboarding continues as today (eligible to label after later steps).
4. Given **Não** selected and Agree, When consent is recorded, Then `acknowledgment_anonymous = true` and onboarding continues the same way (still eligible to label after later steps).

### US2 - Data still collected; system applies acknowledgment anonymity (P1)

1. Given a participant who chose **Não**, When they later complete profile (name, skill, organization, etc.), Then those data are still requested and stored as today.
2. Given `acknowledgment_anonymous = true`, When acknowledgments are produced from platform data, Then that participant MUST NOT appear by first+last name and MUST count toward the anonymous bucket; the administrator MUST NOT need to open the anonymization UI (011/018) solely for this acknowledgment preference.
3. Given `acknowledgment_anonymous = false`, When acknowledgments are produced, Then first+last name MAY be listed (existing behaviour).

### US3 - TCLE text aligns with the radios (P2)

1. Given the staging consent fixture (and any in-app copy that describes the choice), When the participant reads section VII / related LGPD bullets, Then the text refers to an **explicit choice on the platform** (yes name / anonymous bucket), not to a single “mark to stay anonymous” checkbox wording.

## Requirements

- **FR-001**: Replace the consent acknowledgment **checkbox** with a required **radio group** (two options) before Accept; map Sim ? `acknowledgment_anonymous=false`, Não ? `true`. Refuse does not require a radio selection; store `false` (or leave default) on refuse as today.
- **FR-002**: UI strings in en-US source + pt-BR / es-ES targets; pt-BR wording MUST match the researcher’s positive Sim / Não texts (US1).
- **FR-003**: Choosing Não MUST NOT block labeling eligibility; MUST NOT trigger admin anonymization (`anonymized_at`); MUST NOT clear name/e-mail. Anonymity is acknowledgment-list only, applied by the system via the existing column.
- **FR-004**: Update staging TCLE fixture (`V6__consent_form_fixture.sql` or a new staging migration if Flyway history forbids editing applied V6 in environments that already ran it — prefer a new staging-only version bump of the consent form text when needed) so section VII describes the radio choice.
- **FR-005**: Backend API field `acknowledgmentAnonymous` remains; no schema change required unless tests/docs need wording updates.
- **FR-006**: App version **0.27.0** (`0.<spec>.0`); product **1.0.0** remains gated until **promover a 1.0 e deploy**. No cloud deploy; verify on local staging/dev.
- **FR-007**: Document that specs 011/018 remain for LGPD withdrawal / response discard, distinct from acknowledgment anonymity.

## Success Criteria

- **SC-001**: Consent UI shows two radios; Accept disabled until one is chosen; Sim/Não persist the correct boolean.
- **SC-002**: A participant with Não completes onboarding and can label; `anonymized_at` stays null unless an admin anonymization (011) is performed.
- **SC-003**: ROADMAP lists 027 Complete at 0.27.0; 1.0 still gated; local staging shows the new copy.
