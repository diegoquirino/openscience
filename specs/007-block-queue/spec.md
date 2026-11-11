# Feature Specification: Receive, answer and submit blocks without collision

**Feature Branch**: `007-block-queue`

**Created**: 2026-11-10

**Status**: Draft

**Input**: User description: "The eligible participant receives the next free task (block-role) from the queue (10 items, or fewer in the last block of the batch), answers each item as a draft, reviews and submits the whole block. Two simultaneous requests never receive the same task, and the same person never receives both roles of a block. When finished, they know whether there are more blocks for them."

**Parent spec**: [001-oracle-labeling](../001-oracle-labeling/spec.md), User Stories 3, 5 and 11, FR-013, FR-020 to FR-027, FR-034, FR-040 to FR-042 and FR-075 · **Depends on**: 004 and 005 · **Connects to**: 006 (only eligible participants get in) and 008 (rich item screen)

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Receive the next block from the queue (Priority: P1)

The participant asks for a block and receives the first free task they can label, reserved only for them.

**Why this priority**: It is the core of the study and the part with the highest technical risk (concurrency); that is why it comes before the rich item screen.

**Independent Test**: With 38 imported items (8 tasks), ask for blocks with several people in sequence and in parallel and check the order and the exclusivity.

**Acceptance Scenarios**:

1. **Given** the queue with B1-R1 free, **When** the first participant asks for a block, **Then** they receive B1-R1, with the items in import order.
2. **Given** B1-R1 reserved for A, **When** B asks for a block, **Then** B receives B1-R2.
3. **Given** both roles of B1 reserved or completed, **When** C asks for a block, **Then** C receives B2-R1.
4. **Given** that A completed B1-R1 and B1-R2 is free, **When** A asks for another block, **Then** A never receives B1-R2; A receives the next free task they can label.
5. **Given** A with a reserved block, **When** A asks for another, **Then** A receives the same block, with the drafts already saved.
6. **Given** two people asking at the same time and a single free task, **When** the requests arrive, **Then** only one receives it and the other receives the appropriate message from User Story 4.
7. **Given** a database without any item, **When** a participant asks for a block, **Then** they see "No items to label yet".

---

### User Story 2 - Answer the items as drafts (Priority: P1)

For each item, the participant chooses the impact and the operation, may comment and moves on. Each response is saved as a draft on the server.

**Why this priority**: Without saving responses there is no oracle; saving as a draft protects against a dropped connection.

**Independent Test**: Answer part of a block, reload the page, continue; save the same response twice.

**Acceptance Scenarios**:

1. **Given** an item on the screen, **When** shown in this simple version, **Then** it shows system, use case, `kind`, `field`, `origin` and `target` with their line breaks, the impact question (Low, High), the operation question (Keep, Update, Remove, Create, Merge, Split, Reassign, Flag) and a comment field.
2. **Given** only one of the questions answered, **When** the person tries to move on, **Then** the button stays disabled.
3. **Given** both answered, **When** the person moves on, **Then** the draft is saved with the English codes (`LOW`/`HIGH`, `KEEP`, `UPDATE`, ...) and the comment (up to 1,000 characters), the counter shows "x of 10" (or "x of n") and the next item appears.
4. **Given** a block in progress, **When** the person reloads the page or comes back before the reservation expires, **Then** they resume the same block, at the first item without a response, with the drafts preserved.
5. **Given** an item already answered, **When** the person goes back to it and changes the response, **Then** the draft is replaced.
6. **Given** the same response saved twice (double click or slow network), **When** processed, **Then** there is a single draft for the item.
7. **Given** a block in which another person has the other role, **When** the item is shown, **Then** nothing of that person's response or identity appears.
8. **Given** an item on the screen, **When** it is shown, **Then** a countdown in minutes and seconds (for example, "19:42") shows how much time is left before the reservation expires; when the response is saved and the person moves to the next item, it goes back to 20:00 (the configured time).
9. **Given** the block review, **When** the person changes a response, **Then** the draft is saved and the countdown also goes back to the full value.
10. **Given** a response saved, **When** it is stored, **Then** it records the interface language in use at that save (`pt-BR`, `en-US` or `es-ES`); a later save of the same draft in another language replaces it.

---

### User Story 3 - Review and submit the whole block (Priority: P1)

With all items answered, the participant reviews the choices, may change any of them and submits the block. Only then do the responses count.

**Why this priority**: The study rule is that only complete blocks count.

**Independent Test**: Try to submit with a pending item; submit a complete block; try to change a response after the submission.

**Acceptance Scenarios**:

1. **Given** all items answered, **When** the person reaches the end, **Then** they see the review with the two choices and the comment of each item, and can open any item to change it.
2. **Given** the review, **When** the person submits, **Then** all responses become final at once, the task is completed with start and submission date and time, and the responses can no longer be changed.
3. **Given** an item without a response, **When** the person tries to submit, **Then** the submission is refused and the system takes them to the first pending item.
4. **Given** a block already submitted, **When** the same submission arrives again, **Then** the system responds with success without changing anything.

---

### User Story 4 - Know what to do next (Priority: P2)

On submitting a block or asking for a new one, the participant knows whether there are more blocks for them or whether the database is finished.

**Why this priority**: It closes the cycle without leaving the person unsure whether there is still work.

**Independent Test**: Exhaust a database of 2 blocks with 3 people and check the three messages.

**Acceptance Scenarios**:

1. **Given** a submitted block and another task available for the person, **When** the completion screen appears, **Then** the system thanks them and offers "Label another block" or "Sign out".
2. **Given** all tasks completed, **When** the person submits the last block or asks for a new one, **Then** the system thanks them and informs that the database has been completely answered.
3. **Given** tasks not completed, but none available for the person, **When** they ask for a block, **Then** the system thanks them and informs that there are no more blocks for them at the moment.

---

### User Story 5 - Release abandoned blocks (Priority: P1)

A block without activity for 20 minutes goes back to the queue at its original position.

**Why this priority**: Without it, whoever abandons a block stalls the data collection.

**Independent Test**: Reserve a block, let the reservation expire (with a reduced reservation time in staging) and ask with another person.

**Acceptance Scenarios**:

1. **Given** a block reserved for A, **When** A spends 20 minutes without saving any response, **Then** the reservation expires and the task becomes again the first free one at its position in the queue.
2. **Given** that A's reservation expired and B received the task, **When** B receives it, **Then** A's drafts are discarded and B starts from the first item.
3. **Given** the previous case, **When** A tries to save or submit, **Then** the system refuses, explains that the time for the block is up and offers a new block.
4. **Given** that A's reservation expired and nobody took the task, **When** A saves a response, **Then** the reservation is renewed for A and the drafts remain valid.
5. **Given** a saved response, **When** it is stored, **Then** the reservation is renewed for another 20 minutes.
6. **Given** the screen countdown reaching zero, **When** the reservation expires, **Then** the screen warns "Time for this block is up. If nobody has taken it, you can continue." and keeps the choices of the current item; on saving, the person resumes (scenario 4) or receives the refusal (scenario 3).
7. **Given** the clock of the person's device running fast or slow, **When** the countdown is shown, **Then** it uses the expiry time and the server time, not the local clock, so as not to show a wrong time.

---

### User Story 6 - Label a block in the chosen language (Priority: P2)

The participant labels in pt-BR, en-US or es-ES and can switch language in the middle of a block without losing work.

**Why this priority**: Volunteers come from different countries; switching language must not cost them their drafts or their reservation.

**Independent Test**: Answer part of a block in pt-BR, switch to en-US in the header, continue and switch to es-ES in the review; submit and check the `language` of each response.

**Acceptance Scenarios**:

1. **Given** the interface in pt-BR, en-US or es-ES, **When** a block is shown, **Then** the questions, option labels, buttons, counter ("x de 10", "x of 10", "x de 10"), countdown warnings, end messages and refusals appear in that language; the countdown keeps the format minutes:seconds.
2. **Given** a block in progress, **When** the person switches the language in the header, **Then** the page reloads in the new language on the same item (or on the review), with the same reservation and all saved drafts preserved; choices not yet saved on that item are not part of a draft and are not kept by the switch.
3. **Given** a language switch, **When** the page reloads, **Then** the reservation is not renewed by the switch itself, and the countdown shows the same remaining time computed from the expiry time.
4. **Given** an item on the screen, **When** shown in any language, **Then** the system, use case, `origin` and `target` appear as captured, without translation, and the comment is stored as typed.
5. **Given** responses saved in different languages within the same block, **When** the block is submitted, **Then** each final response keeps the language of its last save.

---

### Edge Cases

- Two tabs of the same person: they show the same block and the same drafts; the person never holds two blocks.
- Last task disputed by several people: only one receives it.
- Final block with fewer than 10 items: counter and review use the real size.
- A participant who stops being eligible (new consent form not accepted) with a reserved block: they must accept the consent form before continuing; the reservation follows the normal rules.
- Network drops while submitting: on reconnecting, the person sees the block as submitted or still under review, never half submitted.
- Two tabs of the same person in different languages: both show the same block and drafts; each save records the language of the tab that saved it.
- Language switch exactly when the reservation expires: after the reload, the person sees the expiry warning in the new language and follows scenarios 3 or 4 of User Story 5.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The participant MUST receive the first free task in the queue that they can label, one at a time; with a reserved block, they receive the same one.
- **FR-002**: The system MUST NEVER give the same person both roles of a block.
- **FR-003**: The reservation MUST be exclusive, expire after the configured time (20 minutes) without a saved response and be renewed at every saved response.
- **FR-004**: Asking for a block, saving a draft and submitting a block MUST be atomic; drafts and submissions are accepted only from whoever holds the reservation, or from an expired reservation nobody took, which renews it.
- **FR-005**: When a task with an expired reservation is given to another person, the drafts of the previous person MUST be discarded.
- **FR-006**: Responses MUST be editable drafts until the submission; the submission requires all items answered and makes all of them final at once; final responses cannot be changed by the participant.
- **FR-007**: Saving or submitting more than once MUST NOT duplicate records.
- **FR-008**: Each response MUST store `change_impact`, `maintenance_operation`, an optional comment (up to 1,000 characters), participant, block, role and date/time; the completed task stores start and submission.
- **FR-009**: The screen MUST show the progress of the block ("x of 10" or "x of n").
- **FR-010**: The system MUST NEVER show a rater the responses or the identity of the other rater of the block.
- **FR-011**: On completing or asking for a block, the system MUST show the appropriate end message: more blocks available, database complete or nothing available for the person.
- **FR-012**: Only eligible participants, with the current consent form accepted, MUST ask for, save or submit blocks.
- **FR-013**: The screen of each item MUST show a countdown, in minutes and seconds, until the reservation expires, computed from the expiry time and the server time; it goes back to the full value at every saved response and warns when it reaches zero.
- **FR-014**: A task with an expired reservation MUST go back to its original position in the queue; a person holds at most one reserved block, there is no limit of blocks per person in the data collection, and rater 2 may receive the block while rater 1 is still labeling it.
- **FR-015**: Each saved response MUST record in `language` the interface language in use at the save (`pt-BR`, `en-US` or `es-ES`); a new save of the same draft replaces it, and the submission does not change it.
- **FR-016**: Switching the interface language in the middle of a block MUST be allowed; it MUST keep the reservation and the saved drafts, return to the same item or to the review, and MUST NOT renew or shorten the reservation by itself.
- **FR-017**: The countdown, its warnings, the progress counter, the end messages and the refusals MUST be shown in the interface language; the use case data (`system`, use case, `origin`, `target`) MUST be shown as captured, and the comment stored as typed.
- **FR-018**: The block flow MUST be exposed under `/api/v1` with the resources `/tasks`, `/tasks/reservations`, `/tasks/{id}`, `/tasks/{id}/items/{position}`, `/tasks/{id}/responses/{itemId}` and `/tasks/{id}/submission`, the link relations `vd:next-task`, `vd:reserve` and `vd:save-response` (others following the same kebab-case English pattern), and the JSON fields `reservationExpiresAt` (expiry time) and `reservationMinutes` (configured reservation time).

### Key Entities

- **Task (block-role)**: free, reserved (owner, start, expiry) or completed (owner, start, submission).
- **Response**: one participant's choice for one item in one role; draft or final; records the interface language of its last save (`language`).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: In a test with 30 participants asking, saving and submitting at the same time, no task is given to two people and nobody holds both roles of a block.
- **SC-002**: When a test database is exhausted, every item has exactly two final responses, from different people.
- **SC-003**: The next item appears in less than 3 seconds 95% of the time.
- **SC-004**: No draft is lost when the page is reloaded within the reservation time.
- **SC-005**: The screen countdown differs from the expiry recorded on the server by at most 2 seconds, even with the device clock 5 minutes off.
- **SC-006**: In a test that switches language in the middle of a block through the three locales, no saved draft is lost, the reservation stays the same, and every response records the language in use at its last save.

## Assumptions

- The rule for choosing the next task is tested in isolation with queue scenarios (spec 003, FR-008) before going live.
- The item screen of this spec is deliberately simple; spec 008 replaces it without changing what is stored.
- This spec comes before 006 in the roadmap. Eligibility (FR-012) lives in a single point of the backend that, until 006 exists, considers every participant with a session eligible; 006 then requires the current consent form and the screening at that point, and the refusal-by-eligibility scenarios are tested together with it.
- In staging, the reservation time can be reduced to test the expiry.
- The interface language is chosen before sign-in and changed in the header (spec 004); switching language reloads the page, as there is one build per locale.
- The display labels of the impact and operation options in the three languages are those of Appendices B.1 and B.2 of spec 001.
