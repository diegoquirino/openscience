# Feature Specification: Data collection progress dashboard

**Feature Branch**: `010-progress-dashboard`

**Created**: 2026-11-10

**Status**: Draft

**Input**: User description: "The administrator follows, per system and per batch, how many tasks are free, in progress and completed, the reservations in progress, the participant totals by status and by skill, and the e-mail quota left for the day." (translated from the original Portuguese)

**Parent spec**: [001-oracle-labeling](../001-oracle-labeling/spec.md), User Story 9 and FR-056 · **Depends on**: 005, 006 and 007

## User Scenarios & Testing *(mandatory)*

### User Story 1 - See the queue progress (Priority: P1)

The administrator sees how much of the database has already been labeled, per system and per batch.

**Why this priority**: It answers the most frequent question during data collection: how much is left.

**Independent Test**: Check the dashboard numbers against the long table of spec 009.

**Acceptance Scenarios**:

1. **Given** blocks in several states, **When** the administrator opens the dashboard, **Then** they see, per system and per batch: items, blocks, and role 1 and role 2 tasks that are free, in progress and completed, with the percentage completed.
2. **Given** the dashboard, **When** opened, **Then** the first row shows the overall total and the position of the first free task in the queue.

---

### User Story 2 - See the reservations in progress (Priority: P2)

The administrator sees who holds which block and when the reservation expires.

**Why this priority**: It helps understand stalled blocks, without exposing identity beyond the pseudonym.

**Independent Test**: Reserve two blocks with different people and check the dashboard.

**Acceptance Scenarios**:

1. **Given** reserved tasks, **When** the dashboard is opened, **Then** each one appears with the participant's pseudonym, the block and the role, how many items already have a draft and when the reservation expires.
2. **Given** an expired reservation that nobody took, **When** the dashboard is opened, **Then** it appears as "expired".

---

### User Story 3 - See the participants and the e-mail quota (Priority: P2)

The administrator sees how many people signed in, what status they are in and how many e-mails can still be sent today.

**Why this priority**: It guides outreach and avoids surprises with the e-mail quota.

**Independent Test**: Register participants in each status and check the totals.

**Acceptance Scenarios**:

1. **Given** registered participants, **When** the dashboard is opened, **Then** it shows totals of eligible, ineligible, who declined the consent form and who have not finished the steps yet, and the distribution by professional skill.
2. **Given** the dashboard, **When** opened, **Then** it shows how many e-mails can still be sent today, highlighted when fewer than 10 remain.
3. **Given** participants with completed blocks, **When** the dashboard is opened, **Then** it shows how many blocks each pseudonym completed and the share of the total of completed tasks that this represents, from largest to smallest. Since there is no limit of blocks per person, this is how the researcher follows the concentration of the data labeling in a few volunteers.
4. **Given** participants whose preferred languages are pt-BR, en-US and es-ES, and final responses saved in each of these UI languages, **When** the dashboard is opened, **Then** it shows, for each of pt-BR, en-US and es-ES (including those with zero), the number of participants with that preferred language and the number of final responses saved in that language.

---

### Edge Cases

- Empty database: the dashboard shows zeros and a shortcut to "Import".
- Dashboard open for a long time: the "Refresh" button reloads the numbers, without reloading the page.
- Many participants: the list by pseudonym is paginated or scrollable.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The dashboard MUST show, per system and per batch, items, blocks and tasks by role and state, with the percentage completed.
- **FR-002**: The dashboard MUST list the reservations in progress with pseudonym, block, role, drafts and expiry.
- **FR-003**: The dashboard MUST show participant totals by status and by skill, and completed blocks per pseudonym, with each one's share of the completed total.
- **FR-004**: The dashboard MUST show how many e-mails were sent in the day and how many still fit within the configured daily limit.
- **FR-005**: The dashboard MUST NOT show participants' names or e-mails.
- **FR-006**: Only the administrator MUST be able to see the dashboard.
- **FR-007**: Opening the dashboard MUST NOT change reservations or responses.
- **FR-008**: The dashboard MUST show, for each UI language (pt-BR, en-US, es-ES), the number of participants whose preferred language (`preferred_language`) is that language and the number of final responses whose `language` is that language.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: The dashboard numbers match 100% with the long table and with the task count.
- **SC-002**: The dashboard opens in less than 5 seconds with 500 items and 100 participants.
- **SC-003**: The response counts per language match 100% with the `response_language` column of the long table of spec 009.

## Assumptions

- The dashboard is read-only; actions on participants belong to spec 011.
- Drafts are not counted in the responses per language; they appear only in the reservations in progress.
