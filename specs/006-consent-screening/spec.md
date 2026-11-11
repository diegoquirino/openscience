# Feature Specification: Consent form, professional skill and screening

**Feature Branch**: `006-consent-screening`

**Created**: 2026-11-10

**Status**: Draft

**Input**: User description: "After signing in, the participant accepts the research consent form (TCLE/LGPD), chooses their professional skill from a list and answers the screening question, which admits them to or excludes them from the study."

**Parent spec**: [001-oracle-labeling](../001-oracle-labeling/spec.md), User Stories 2 and 11, FR-006 to FR-010, FR-074 to FR-076, Appendices B.3 and B.4 · **Depends on**: 004
## User Scenarios & Testing *(mandatory)*

### User Story 1 - Accept or refuse the consent form (Priority: P1)

On the first sign-in, the participant reads the current consent form and chooses to accept or not to participate.

**Why this priority**: Ethical and LGPD requirement: no label can be recorded without consent.

**Independent Test**: Sign in with a new e-mail and refuse; sign in with another and accept; publish a new version of the consent form and sign in again.

**Acceptance Scenarios**:

1. **Given** a participant who never accepted the current consent form, **When** they sign in, **Then** they see the full text of the consent form, with scrolling, and the options "I have read it and agree to participate" and "I do not agree".
2. **Given** the consent form, **When** the person accepts, **Then** the system records the date, time and version of the acceptance and moves on to the professional skill.
3. **Given** the consent form, **When** the person refuses, **Then** the system records the refusal (date, time, version), thanks them and ends the access.
4. **Given** a participant who refused, **When** they sign in again, **Then** they see the consent form again and can change their mind.
5. **Given** a new version of the consent form, **When** a participant who accepted the previous one signs in, **Then** they must accept the new one before continuing; professional skill and screening are not asked again.
6. **Given** no consent form defined in the configuration, **When** a participant signs in, **Then** they see "The study is not open yet" and cannot proceed.

---

### User Story 2 - State the professional skill (Priority: P1)

The participant chooses a single option from the list in Appendix B.4.

**Why this priority**: It characterizes the sample of raters in the paper.

**Independent Test**: Try to proceed without choosing; choose "Other" without a description; choose a common option; repeat in each of the three languages.

**Acceptance Scenarios**:

1. **Given** the skill screen, **When** no option is selected, **Then** the button to proceed stays disabled.
2. **Given** "Other", **When** the description is empty or longer than 100 characters, **Then** it is not possible to proceed.
3. **Given** a chosen option, **When** the person proceeds, **Then** the system stores the option code (for example, `test_analyst`) and, for "Other", the description.
4. **Given** the interface in pt-BR, en-US or es-ES, **When** the skill screen is shown, **Then** the options appear with the display labels of Appendix B.4 in that language, and the stored code is the same English code whatever the language.
5. **Given** "Other", **When** the person types the description, **Then** it is stored exactly as typed, without translation.

---

### User Story 3 - Answer the screening (Priority: P1)

The participant sees a short example of a use case and of a test case and answers whether they have ever worked with something like it.

**Why this priority**: It ensures raters with the experience the study requires.

**Independent Test**: Answer each of the three options with different e-mails and sign in again with each one; repeat the screen in each of the three languages.

**Acceptance Scenarios**:

1. **Given** the screening, **When** it is shown, **Then** it shows the example use case with a basic flow and an exception flow, a test case derived from it and the question of Appendix B.3 with three options.
2. **Given** the screening, **When** the person answers "Yes, in professional practice" or "Yes, in studies, courses or research", **Then** they are marked as eligible and move on to participation.
3. **Given** the screening, **When** the person answers "No", **Then** the system records the answer, thanks them, explains that the study requires this experience and ends their participation.
4. **Given** an ineligible participant, **When** they sign in again, **Then** they see the same closing message and can neither take part in the data labeling nor answer the screening again.
5. **Given** an eligible participant with the current consent form accepted, **When** they sign in again, **Then** they go straight to participation, without repeating any step.
6. **Given** the interface in pt-BR, en-US or es-ES, **When** the screening is shown, **Then** the question, the three options and the surrounding instructions appear in that language (Appendix B.3), the example use case and test case appear in English, as the study items do, and the answer is stored as the English code (`yes_professional`, `yes_academic` or `no`) whatever the language.

---

### User Story 4 - Read the consent form in the chosen language (Priority: P1)

The consent form is shown in the interface language chosen by the participant. The pt-BR text is the reference approved by the research ethics committee (CEP); the en-US and es-ES texts are translations of the same version and say so.

**Why this priority**: The participant must understand what they agree to, and the study must be able to prove which text was approved and which text each person read.

**Independent Test**: Accept the current consent form with three new e-mails, one in each language, open the link to the reference text in the en-US and es-ES versions, and check the recorded acceptances; configure a new version missing one language and sign in again.

**Acceptance Scenarios**:

1. **Given** the interface in pt-BR, en-US or es-ES, **When** the consent form is shown, **Then** its text is the one of the current version in that language, and the buttons and messages of the screen are in that language.
2. **Given** the interface in pt-BR, **When** the consent form is shown, **Then** it is the reference text, with no translation note.
3. **Given** the interface in en-US or es-ES, **When** the consent form is shown, **Then** a note at the top says that the text is a translation and that the reference version, approved by the research ethics committee, is the Portuguese (Brazil) one, with a link that opens the pt-BR text of the same version without changing the interface language.
4. **Given** a participant who accepts or refuses, **When** the decision is recorded, **Then** it stores the version, the language of the text shown and the date and time.
5. **Given** a participant on the consent form screen, **When** they switch the language in the header, **Then** the page reloads in the new language, shows the same version of the consent form in that language, and nothing is recorded until they choose an option.
6. **Given** a new version of the consent form configured without the text in one of the three languages, **When** the system starts, **Then** that version does not become current, the missing languages are reported in the application log, and participants keep seeing the previous current version (or "The study is not open yet" if there is none).
7. **Given** a participant who accepted the consent form, **When** their data is exported (spec 009), **Then** the export includes their preferred language and the language of the accepted consent form.

---

### Edge Cases

- A person who closes the browser in the middle of the steps: on return, resumes at the first pending step.
- Going back with the browser button after the screening: does not allow changing the answer.
- Very long consent form: scrolls within the page and the buttons are always visible.
- Administrator: never sees these screens.
- A participant who accepted the consent form in one language and later switches the interface to another: no new acceptance is required; acceptance is per version, not per language.
- A participant who switches the language between steps: the steps already completed are kept; the next step appears in the new language.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST display the current consent form and record acceptance or refusal with date, time and version; refusal ends the access.
- **FR-002**: A new version of the consent form MUST require a new acceptance before the data labeling continues.
- **FR-003**: The participant MUST choose a single skill from the list in Appendix B.4; "Other" requires a description of up to 100 characters.
- **FR-004**: The participant MUST answer the screening of Appendix B.3; "No" makes them permanently ineligible for that e-mail.
- **FR-005**: Skill and screening MUST be asked only once per participant.
- **FR-006**: On sign-in, the participant MUST be taken to the first pending step, in the order consent form, skill, screening, participation.
- **FR-007**: Only eligible participants with the current consent form accepted MUST reach participation; this rule MUST also apply to direct calls to the participation functions.
- **FR-008**: The text and version of the consent form MUST come from the system configuration.
- **FR-009**: Each version of the consent form MUST have its text in pt-BR, en-US and es-ES; the system MUST show the text in the interface language in use.
- **FR-010**: The pt-BR text MUST be treated as the reference approved by the research ethics committee; the en-US and es-ES texts MUST state that they are translations and link to the pt-BR text of the same version.
- **FR-011**: The record of acceptance or refusal MUST store the version, the language of the text shown and the date and time; the participant's preferred language and the language of the accepted consent form MUST be available for the exports.
- **FR-012**: A version of the consent form MUST NOT become current until it has texts in the three languages; while it is incomplete, the previous current version stays in force and the missing languages are reported in the application log.
- **FR-013**: The professional skill options, the screening question, its answers and instructions, and all messages of these steps MUST be shown in the interface language; the screening example (use case and test case) MUST be shown in English in every language, like the study items; the stored values MUST be the English codes (`professional_skill` codes of Appendix B.4; screening answers `yes_professional`, `yes_academic`, `no`), the same in every language.
- **FR-014**: The "Other" description MUST be stored as typed, without translation.

### Key Entities

- **Participant** (additions): version, language, date and time of the acceptance or refusal, skill, "Other" description, screening answer, eligibility, preferred language.
- **Consent form**: current text and version, with one text per language (pt-BR as the reference, en-US and es-ES as translations).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A new participant goes through consent form, skill and screening in less than 3 minutes, not counting the reading of the consent form.
- **SC-002**: In a test with every combination of answers, no participant without a current acceptance or who is ineligible can reach participation, not even by a direct call.
- **SC-003**: Every eligible participant has the version and date of acceptance recorded.
- **SC-004**: In a test with one participant per language, every acceptance records the version and the language of the text shown, and the en-US and es-ES screens show the translation note and the link to the pt-BR text.
- **SC-005**: In a test in each of the three languages, the stored skill and screening answer are the same English codes for the same choices.

## Assumptions

- The text of the consent form (TCLE) comes from the researcher, as approved by the research ethics committee, and is put in the configuration by the researcher; there is no screen to edit the consent form.
- The en-US and es-ES texts of the consent form are translations provided by the researcher; only the pt-BR text is the one approved by the research ethics committee.
- The screening example is fixed and written by the researcher, in English, the language of the use cases participants will label (Appendix B.3 of spec 001); only the question, the options and the instructions around it are translated.
- The display labels of the skills and of the screening in the three languages are those of Appendices B.3 and B.4 of spec 001.
- The language is chosen on the first screen, before sign-in (spec 004); this spec only uses it.
- Until spec 007 is delivered, the eligible participant sees a screen "You are eligible; blocks will be available soon".
