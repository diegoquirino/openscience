# Feature Specification: Change-impact answer labels and required markers

**Feature Branch**: `012-impact-answer-labels`

**Created**: 2026-11-19

**Status**: In progress

**Input**: User description: "The question 'Does this edit change the behavior that the use case specifies?' should present 'No (low impact)' or 'Yes (high impact)' as the answers, with the existing explanations kept underneath. The impact and maintenance-operation questions should be marked as required (*)."

**Parent spec**: [001-oracle-labeling](../001-oracle-labeling/spec.md), Appendix B.1 / item screen of [008-item-screen](../008-item-screen/spec.md) · **Depends on**: 008

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Answer change impact as No or Yes (Priority: P1)

On each item, the participant sees choice labels that match the yes/no question, while the stored codes stay `LOW` and `HIGH`.

**Why this priority**: The previous "Low"/"High" labels did not read as direct answers to the question.

**Independent Test**: Open an item in each locale and confirm the radio labels and that a save still stores `LOW` or `HIGH`.

**Acceptance Scenarios**:

1. **Given** the item screen in en-US, **When** the impact question is shown, **Then** the two options are labeled `No (low impact)` and `Yes (high impact)`, each with the existing explanation text underneath.
2. **Given** the item screen in pt-BR, **When** the impact question is shown, **Then** the options are `Não (baixo impacto)` and `Sim (alto impacto)` with the existing Portuguese explanations.
3. **Given** the item screen in es-ES, **When** the impact question is shown, **Then** the options are `No (bajo impacto)` and `Sí (alto impacto)` with the existing Spanish explanations.
4. **Given** a chosen option, **When** the draft is saved, **Then** the API still receives `changeImpact` `LOW` for No and `HIGH` for Yes.
5. **Given** the review screen, **When** a draft with `LOW` or `HIGH` is listed, **Then** it shows the same No/Yes labels as the item screen.

---

### User Story 2 - See required markers on the labeling questions (Priority: P1)

The impact and maintenance-operation questions show that an answer is required.

**Why this priority**: Participants need a clear cue that both choices are mandatory before continuing.

**Independent Test**: Open an item and check the legends; try to continue without both answers.

**Acceptance Scenarios**:

1. **Given** the item screen, **When** the impact and operation fieldsets are shown, **Then** each legend includes a required marker (`*`) that is announced for accessibility.
2. **Given** the item screen, **When** either impact or operation is missing, **Then** the participant cannot advance to the next item (same rule as today).
3. **Given** the comment field, **When** shown, **Then** it remains optional and is not marked required.

---

### Edge Cases

- Unknown stored code: still shown as the raw code (no No/Yes mapping).
- Language switch mid-item: unsaved choices kept; labels reload in the new locale.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The short display labels for `change_impact` MUST be No/Yes with the impact level in parentheses in each UI locale, as in the acceptance scenarios; stored codes MUST remain `LOW` and `HIGH`.
- **FR-002**: The explanation paragraphs under each impact option MUST stay the texts already defined for Appendix B.1 (no rewrite of the meaning).
- **FR-003**: The impact and maintenance-operation question legends MUST show a required marker (`*`) in all three locales.
- **FR-004**: The comment MUST remain optional and MUST NOT gain a required marker.
- **FR-005**: Exports, the API contract and Appendix B.1 codes MUST keep `LOW`/`HIGH`; only UI labels and the overview Appendix B.1 display names change.
- **FR-006**: The review screen MUST use the same impact short labels as the item screen.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: In each of pt-BR, en-US and es-ES, a tester identifies the impact options as answers to the yes/no question without training.
- **SC-002**: 100% of saved responses still use only `LOW` or `HIGH` for `change_impact`.
- **SC-003**: Automated UI tests assert the new labels and the required markers on both questions.

## Assumptions

- Spec 008 item and review screens are the only places that show these short labels to participants.
- No backend change is required beyond documentation of the display wording.
