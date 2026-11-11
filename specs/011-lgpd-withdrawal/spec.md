# Feature Specification: Handle a data withdrawal request (LGPD)

**Feature Branch**: `011-lgpd-withdrawal`

**Created**: 2026-11-10

**Status**: Draft

**Input**: User description: "At a participant's request, the administrator removes their name and e-mail and decides whether the responses stay, anonymized, or are discarded, returning the blocks to the queue." (translated from the original Portuguese)

**Parent spec**: [001-oracle-labeling](../001-oracle-labeling/spec.md), User Story 10 and FR-059 · **Depends on**: 007 and 009

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Anonymize keeping the responses (Priority: P1)

The administrator finds the participant by e-mail and erases name and e-mail, keeping the responses linked only to the pseudonym.

**Why this priority**: It fulfills the right to erasure of personal data without losing labels already used.

**Independent Test**: Anonymize a participant with completed blocks and check screens and exports.

**Acceptance Scenarios**:

1. **Given** the "Participants" screen, **When** the administrator searches by e-mail, **Then** they see that person's pseudonym, status, and completed and in-progress blocks.
2. **Given** a participant, **When** the administrator confirms "Anonymize and keep responses", **Then** name and e-mail disappear from all screens and exports (the wide table shows `anonymized`) and the responses remain linked only to the pseudonym.
3. **Given** an anonymized participant, **When** someone tries to sign in with the same e-mail, **Then** they are treated as a new person, with another pseudonym.
4. **Given** an anonymized participant with a reserved block, **When** the anonymization is confirmed, **Then** the reservation is released and the drafts are discarded.
5. **Given** a participant whose preferred language is es-ES and an administrator using the en-US interface, **When** the administrator confirms the withdrawal (with either option), **Then** the confirmation e-mail sent to the participant is written in es-ES, and it is sent before the e-mail address is erased.

---

### User Story 2 - Anonymize and discard the responses (Priority: P2)

The administrator erases name, e-mail and all of the person's responses; the blocks they completed go back to the queue.

**Why this priority**: It serves those who withdraw consent completely; it is rare.

**Independent Test**: Discard a participant with two completed blocks and check the queue and the exports.

**Acceptance Scenarios**:

1. **Given** a participant with completed blocks, **When** the administrator confirms "Anonymize and discard responses", **Then** their responses are erased and the tasks they completed become free again, in the same queue position.
2. **Given** a returned task, **When** someone requests a block, **Then** it is handed out before the tasks that come after it in the queue.
3. **Given** the confirmation, **When** requested, **Then** the system shows how many responses and tasks will be affected, and the participant's preferred language, and requires explicit confirmation.

---

### Edge Cases

- Participant who labeled role 1 of a block whose role 2 belongs to another person: only their task goes back to the queue.
- Withdrawal after an export: files already downloaded do not change; the system warns that old exports need to be redone and discarded.
- Withdrawal request from someone with no responses: only the personal data is erased.
- Daily e-mail quota exhausted: the confirmation step warns that the confirmation e-mail cannot be sent; the withdrawal is still carried out and the administrator informs the participant outside the system, in the participant's preferred language.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The administrator MUST be able to search for a participant by e-mail or pseudonym.
- **FR-002**: The administrator MUST be able to anonymize a participant, keeping or discarding the responses.
- **FR-003**: Anonymizing MUST erase name and e-mail permanently and release any reservation of the person.
- **FR-004**: Discarding MUST erase the person's responses and return the tasks they completed to the queue, in the original position.
- **FR-005**: The operation MUST be logged with date, time, pseudonym and chosen option, without personal data.
- **FR-006**: The operation MUST be atomic with respect to block requests, saves and submissions.
- **FR-007**: When the withdrawal is confirmed, the system MUST send the participant a confirmation e-mail before erasing the address; the address MUST NOT be kept for this or any other purpose afterwards.
- **FR-008**: Every message the system sends to the participant about the withdrawal MUST be written in the participant's preferred language (`preferred_language`: pt-BR, en-US or es-ES), regardless of the administrator's UI language.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: After an anonymization, a search for the name and the e-mail in all tables and exports finds nothing.
- **SC-002**: A withdrawal request is handled in less than 5 minutes.
- **SC-003**: 100% of the withdrawal messages sent by the system are in the participant's preferred language.

## Assumptions

- The request reaches the researcher by e-mail, outside the system, as stated in the consent form (TCLE).
- Backups and old exports are handled by the researcher, outside the system.
- The text of the confirmation e-mail exists in pt-BR, en-US and es-ES, like the other e-mails of the system.
