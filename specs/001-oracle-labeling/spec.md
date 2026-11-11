# Feature Specification: Veredito, blind and concurrent oracle labeling

**Feature Branch**: `001-oracle-labeling`

**Created**: 2026-11-10

**Status**: Overview; implemented by specs 002 to 011, in the order of the [ROADMAP](../ROADMAP.md)

**Input**: User description: "Web app in which volunteers identify themselves by name and e-mail, validate their participation with a code sent by e-mail, accept the research consent form, state their professional skill, go through a screening question and classify the change impact and the maintenance operation of use-case edits. Each item gets two different raters. Only the administrator (Carlos Diego Quirino Lima, diegoquirino@gmail.com) imports items by CSV (`system, edit_key, uc, kind, field, origin, target`) and accesses the administration area. The system must withstand simultaneous access." (translated from the original Portuguese)

## Clarifications

### Session 2026-11-10

- Q: How does the rater see the context of the use case? -> A: A button next to "Before" opens a modal with the full use case in the previous version (e.g. v1.1); another next to "After" opens the full use case in the modified version (e.g. v1.2).
- Q: How many items per session? -> A: Fixed blocks of 10 consecutive items; the last block may have fewer. The queue is filled top to bottom, alternating roles: block 1 rater 1, block 1 rater 2, block 2 rater 1, and so on. The responses of a block only count when the rater completes all of its items.
- Q: What is the first column of the CSV? -> A: `system`; each item is identified by (`system`, `edit_key`).

### Session 2026-11-11

- Q: How long does the reservation of a block last? -> A: 20 minutes without any saved response (configuration value); each saved response renews the deadline.
- Q: Does the volunteer see how much time is left? -> A: Yes. Each item screen shows a countdown in minutes and seconds until the reservation expires, which goes back to the full value when the person saves the response and moves on to the next item.
- Q: What happens to the drafts when the reservation expires? -> A: If nobody took the block, the person resumes it by saving a response, with the drafts; if another person received it, the first person's drafts are discarded.
- Q: Where does an abandoned block go back in the queue? -> A: To its original position, ahead of the following blocks.
- Q: How many blocks can a person have reserved at the same time? -> A: One.
- Q: Is there a limit of blocks per person in the data collection? -> A: No. The progress dashboard shows how many blocks each pseudonym did, and rater bias is discussed in the paper.
- Q: Can rater 2 start before rater 1 finishes? -> A: Yes, both label the same block in parallel, without seeing anything from each other.
- Q: Where do the blocks of someone who requests withdrawal (LGPD) with discarded responses go back? -> A: To the same position in the queue.
- Q: Which vocabulary should be used? -> A: See the *Vocabulary* section and [GLOSSARY](../GLOSSARY.md).
- Q: Can a `META` item have an empty `origin` or `target`? -> A: Yes, one of the two. The pipeline generates `META` with one empty side when a metadata entry is added or removed (in Minitest, `LoginTest|3|1` goes from no requirement to `REQ-07`). The import refuses only a `META` with both sides empty, and the screen shows a localized "(none)" on the empty side.
- Q: Which languages does the user interface support, and how? -> A: pt-BR, en-US and es-ES. The frontend uses the official Angular i18n (`@angular/localize`): one build per locale, served by the same Spring Boot under `/pt-BR/`, `/en-US/` and `/es-ES/`; switching language reloads the page. Backend messages (Problem Details `title`/`detail`, e-mails) come from Spring `MessageSource` in the user's language.
- Q: How does the person choose the language? -> A: The first screen, before sign-in, offers the three languages, with the browser language preselected when supported (fallback pt-BR). The choice is saved in the browser and, after sign-in, in the profile (`preferred_language` of the participant, `config.admin_preferred_language` of the administrator); the profile wins on later sign-ins; the language can be changed at any time from the header, and what was typed and not saved yet is restored after the switch.
- Q: In which language are the consent form and the e-mails? -> A: In the three languages. The pt-BR consent form is the reference version approved by the research ethics committee (CEP); the en-US and es-ES versions state that they are translations and link to the pt-BR version.
- Q: Is the language of capture recorded? -> A: Yes. Each response records the UI language at save time (`language`); the participant's preferred language and the language of the accepted consent form are recorded too; the exports include them.
- Q: What is stored for codes, and how is translation completeness ensured? -> A: Codes stored in the database and in the exports are English identifiers; only their display labels are translated. Every user-facing string must exist in the three locales, checked automatically in CI.
- Q: How are a missing use case and an empty `META` side shown? -> A: The import CSV keeps the pipeline text `(the use case does not exist in this version)` as captured; the item screen shows a localized message instead. A `META` edit may have `origin` or `target` empty (not both); the empty side is shown as a localized "(none)".
- Q: In which language are the repository and the data? -> A: Everything in the repository (folder and file names, identifiers, code, comments, documentation) is in English. Data (dataset and database contents) is never translated: it stays in the language in which it was captured.

## Vocabulary

Terms, identifiers, REST paths, link relations and user interface vocabulary follow [GLOSSARY](../GLOSSARY.md). Compact version of the user interface vocabulary:

| Where | pt-BR | en-US | es-ES |
|---|---|---|---|
| System name | Veredito | Veredito | Veredito |
| Activity, in titles, consent form, invitations and explanations | rotulagem dos dados | data labeling | etiquetado de datos |
| Buttons and short messages | avaliar ("Avaliar mais um bloco", "Ainda não há itens para avaliar") | label ("Label another block", "No items to label yet") | evaluar ("Evaluar otro bloque", "Aún no hay ítems para evaluar") |
| Person, in formal texts (consent form, LGPD) | participante | participant | participante |
| Person, in invitations and screens | voluntário | volunteer | voluntario |
| Position in the block | rotulador 1, rotulador 2 | rater 1, rater 2 | etiquetador 1, etiquetador 2 |
| Language choice | Português (Brasil) | English (US) | Español (España) |

In the data model the position in the block is `role` 1 or 2; in the pipeline it is `rater1` and `rater2`. The column names, codes and files of the ISSTA_2027 pipeline (`change_impact`, `maintenance_operation`, `t1_<system>_rater1.csv`) do not change.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Identify oneself with name, e-mail and code (Priority: P1)

A volunteer opens Veredito, enters name and e-mail, receives a 6-digit code by e-mail and types it to sign in. There is no invitation and no password.

**Why this priority**: Without reliable identification it is not possible to guarantee that rater 1 and rater 2 of a block are different people.

**Independent Test**: Enter name and e-mail, receive the code and sign in; try a wrong, expired and reused code.

**Acceptance Scenarios**:

1. **Given** an e-mail never used before, **When** the person enters a valid name and e-mail, **Then** the system sends a 6-digit code to that e-mail, states that it is valid for 10 minutes and advises checking the spam folder.
2. **Given** a code sent less than 10 minutes ago, **When** the person types the correct code, **Then** they sign in and go to the next pending step (consent form, profile, screening, block in progress or new block).
3. **Given** a valid code, **When** the person types a wrong code 5 times, **Then** the code is invalidated and they must request another one.
4. **Given** an expired or already used code, **When** the person types it, **Then** the system refuses it and offers to send a new code.
5. **Given** that a code was sent less than 60 seconds ago, **When** the person requests another one, **Then** the system states how many seconds remain before it can be resent.
6. **Given** an e-mail typed with uppercase letters or spaces, **When** the person enters it, **Then** the system treats `Ana@X.com ` and `ana@x.com` as the same person.
7. **Given** that the daily e-mail quota is exhausted, **When** someone requests a code, **Then** the system states that it cannot send codes today and suggests trying again the next day.

---

### User Story 2 - Accept the consent form, state the professional skill and go through screening (Priority: P1)

On the first sign-in, the person reads the consent form (TCLE/LGPD) and accepts it, chooses their professional skill and answers the screening question. Whoever answers "No" to the screening is thanked and leaves the study.

**Why this priority**: Ethical and LGPD requirement to collect data, and the filter that guarantees qualified raters.

**Independent Test**: Sign in with a new e-mail and go through the three steps with each possible answer, checking what is recorded.

**Acceptance Scenarios**:

1. **Given** a participant who never accepted the current consent form, **When** they sign in, **Then** they see the full text of the consent form with the options "I have read it and agree to participate" and "I do not agree".
2. **Given** the consent form on screen, **When** the person chooses "I have read it and agree to participate", **Then** the system records the date, time, version and language of the consent form and proceeds to the professional skill.
3. **Given** the consent form on screen, **When** the person chooses "I do not agree", **Then** the system thanks them, ends the access and records no label from them.
4. **Given** the professional skill screen, **When** the person chooses no option, **Then** they cannot proceed.
5. **Given** the option "Other" chosen, **When** the person leaves the description blank, **Then** they cannot proceed.
6. **Given** the screening question (Appendix B.3), **When** the person gives one of the two "Yes" answers, **Then** they are considered eligible and proceed to the first block.
7. **Given** the screening question, **When** the person answers "No", **Then** the system records the answer, thanks them, explains that the study requires this experience and ends the participation.
8. **Given** a participant already marked as ineligible, **When** they sign in again with the same e-mail, **Then** they see the same closing message and cannot take part in the data labeling.
9. **Given** a participant who already accepted the current consent form and answered professional skill and screening, **When** they sign in again, **Then** they go straight to the block in progress or to a new block.
10. **Given** that the administrator published a new version of the consent form, **When** a returning participant signs in, **Then** they must accept the new version before continuing the data labeling.

---

### User Story 3 - Receive and complete a fixed block of 10 items, without collision with other people (Priority: P1)

The eligible participant receives the next available block from the queue: 10 consecutive items of the dataset in one role (rater 1 or 2); the last block of a batch may have fewer. The queue moves top to bottom, alternating roles: block 1 role 1, block 1 role 2, block 2 role 1, block 2 role 2... The responses stay as drafts until the person completes all the items of the block and submits it; only then do they count.

**Why this priority**: It is the core of the study: producing two independent and complete labels per item.

**Independent Test**: Import 38 items (blocks of 10, 10, 10 and 8), open sessions in parallel with several people and check that each item ends with exactly two final responses from different people.

**Acceptance Scenarios**:

1. **Given** the queue with block 1 role 1 free, **When** the first eligible participant requests a block, **Then** they receive block 1 in role 1, with its 10 items in import order.
2. **Given** block 1 role 1 reserved for A, **When** B requests a block, **Then** B receives block 1 in role 2.
3. **Given** both roles of block 1 reserved or completed, **When** C requests a block, **Then** C receives block 2 in role 1.
4. **Given** that A completed block 1 in role 1 and role 2 of that block is free, **When** A requests another block, **Then** A never receives block 1 in role 2; A receives the next free block they can label.
5. **Given** a dataset of 205 items in a single batch, **When** the queue is formed, **Then** there are 20 blocks of 10 items and a final block of 5, and whoever receives this last block, in any role, labels 5 items.
6. **Given** two people requesting a block at the same instant, **When** a single free task (block-role) remains, **Then** only one of them receives it, and the other receives another block or the end message (User Story 5).
7. **Given** a block in progress, **When** the person answers an item, **Then** the response is saved as a draft on the server, the counter shows "x of 10" (or "x of n" in the final block) and the next item appears.
8. **Given** a block in progress, **When** the person reloads the page or signs in again before the reservation expires, **Then** they resume the same block with the drafts already answered.
9. **Given** all the items of the block answered, **When** the person reaches the end, **Then** they see a review screen with the choices for each item, can change any of them and only then submit the block.
10. **Given** the review, **When** the person submits the block, **Then** all responses become final at once, the task is completed and the responses can no longer be changed.
11. **Given** a block with some item unanswered, **When** the person tries to submit it, **Then** the submission is refused and the system takes them to the first pending item.
12. **Given** a block reserved for A, **When** A goes 20 minutes without saving any response, **Then** the reservation expires and the task goes back to the queue.
13. **Given** that A's reservation expired and B took the same task, **When** A tries to save or submit, **Then** the system refuses, explains that the time for the block is over, discards A's drafts and offers a new block.
14. **Given** that A's reservation expired and nobody took the task, **When** A comes back and saves a response, **Then** the reservation is renewed for A and A's drafts remain valid.
15. **Given** an item on screen, **When** the person saves the same response twice (double click or slow network), **Then** there is a single draft for that item.
16. **Given** a completed block, **When** the person chooses "Label another block", **Then** they receive the next task available to them.
17. **Given** an item on screen, **When** it is displayed, **Then** a countdown in minutes and seconds (for example, "19:42") shows how long until the reservation expires; when the response is saved and the person moves on to the next item, the countdown goes back to 20:00.
18. **Given** the countdown reaching zero, **When** the reservation expires, **Then** the screen warns that the time for the block is over and that the person can still continue if nobody has taken the block; on saving, they resume (scenario 14) or receive the refusal (scenario 13).

---

### User Story 4 - Understand what is being labeled (Priority: P1)

The item screen shows the edit in a way that is comfortable to read (before and after, with the differences highlighted), gives access to the full use case in each version and presents the two questions with an explanation of each option. The participant chooses one option in each question, may leave a comment and moves on.

**Why this priority**: The validity of the oracle depends on the rater understanding the edit, the context in which it occurs and the concepts of impact and operation.

**Independent Test**: Display one item of each `kind` (MOD, ADD, DEL, META), open both modals and check readability, explanations and the recording of the codes.

**Acceptance Scenarios**:

1. **Given** a `MOD` item, **When** it is displayed, **Then** "Before" (origin) and "After" (target) appear side by side, with removed words struck through and added words highlighted.
2. **Given** an `ADD` item, **When** it is displayed, **Then** only "Added text" appears, with the target.
3. **Given** a `DEL` item, **When** it is displayed, **Then** only "Removed text" appears, with the origin.
4. **Given** a `META` item of `requirements`, **When** it is displayed, **Then** it appears as "Requirements link: before / after".
5. **Given** any item, **When** it is displayed, **Then** the header shows the system, the use case and the location of the edit in natural language (Appendix A), for example "Exception flow step · Modification".
6. **Given** any item, **When** it is displayed, **Then** next to "Before" there is the button "View full use case (v1.1)" and next to "After" the button "View full use case (v1.2)", with the version labels taken from the item; in an `ADD` the first button appears in place of the "Before" block, and in a `DEL` the second appears in place of the "After" block.
7. **Given** the button of the previous version, **When** the person clicks it, **Then** a modal titled "<use case> · version v1.1 (previous)" opens with the full text of that version, in a monospaced font and with the indentation preserved.
8. **Given** the button of the modified version, **When** the person clicks it, **Then** the modal "<use case> · version v1.2 (modified)" opens with the full text of that version.
9. **Given** a use-case text with lines marked as edited (`<- edited`), **When** the modal opens, **Then** those lines appear highlighted, the marker is not displayed and the modal opens already scrolled to the first of them.
10. **Given** an open modal, **When** the person clicks "Close", presses Esc or clicks outside it, **Then** the modal closes and the choices already made in the item are still there.
11. **Given** a narrow screen (mobile phone), **When** the item is displayed, **Then** "Before" and "After" appear one below the other, without horizontal scrolling, and the modal takes up the whole screen.
12. **Given** a text with several lines, **When** it is displayed in the item or in the modal, **Then** the line breaks are kept.
13. **Given** the impact question, **When** it is displayed, **Then** it shows the definition and examples of each option (Appendix B.1), with a single choice.
14. **Given** the operation question, **When** it is displayed, **Then** it shows the eight options with explanations (Appendix B.2), with a single choice, and the precedence rule for when more than one applies.
15. **Given** only one of the two questions answered, **When** the person tries to proceed, **Then** the button stays disabled.
16. **Given** both questions answered, **When** the person proceeds, **Then** the system stores the English codes (`LOW`/`HIGH` and `KEEP`, `UPDATE`, ...), the optional comment (up to 1,000 characters) and the UI language in use (`language`).
17. **Given** a block whose role 1 is in progress or completed, **When** another person labels the same block in role 2, **Then** they see neither the responses nor the identity of rater 1.
18. **Given** a `META` item with `origin` or `target` empty, **When** it is displayed, **Then** the empty side shows "(none)" in the UI language (pt-BR "(nenhum)", en-US "(none)", es-ES "(ninguno)").
19. **Given** an item whose `origin_usecase` or `target_usecase` holds the pipeline text `(the use case does not exist in this version)`, **When** the person opens the corresponding modal, **Then** it shows the localized message of FR-077 instead of the raw text, without line highlighting.

---

### User Story 5 - End the participation (Priority: P2)

After submitting a block, or when requesting one, the person learns whether there are still blocks for them or whether the dataset has been completely answered.

**Why this priority**: Closes the participation cycle without leaving the person unsure whether there is still work.

**Independent Test**: Exhaust a small dataset and check the three messages.

**Acceptance Scenarios**:

1. **Given** a submitted block and there are still tasks available to the person, **When** the completion screen appears, **Then** the system thanks them and offers "Label another block" or "Sign out".
2. **Given** that all tasks are completed, **When** the person submits the last block or requests a new one, **Then** the system thanks them, states that the data labeling is complete (the whole dataset has been labeled) and ends the participation.
3. **Given** that there are still tasks not completed, but none available to the person (they are role 2 of blocks in which the person was rater 1, or they are reserved by other people), **When** they request a block, **Then** the system thanks them and states that there are no more blocks available to them at the moment.

---

### User Story 6 - Enter the administration area (Priority: P1)

Carlos Diego Quirino Lima signs in with `diegoquirino@gmail.com` and the e-mailed code and goes straight to the administration area. Nobody else has access to it.

**Why this priority**: Without the administration area there are no items to label and no data to export.

**Independent Test**: Sign in with the administrator's e-mail and with another e-mail; call the administrative functions without being authenticated as administrator.

**Acceptance Scenarios**:

1. **Given** the e-mail `diegoquirino@gmail.com`, **When** the code is validated, **Then** the system opens the administration area, without consent form, screening or labeling blocks.
2. **Given** any other e-mail, **When** the person tries to open an administrative screen or function, **Then** access is denied.
3. **Given** the administrator, **When** he tries to enter the labeling flow, **Then** he cannot: the administrator does not label items.

---

### User Story 7 - Import items by CSV (Priority: P1)

The administrator uploads a CSV with the columns of Appendix D, sees a preview with what is included, what is ignored and what has errors, and confirms. On confirmation, the new items are split into blocks of 10 and go to the end of the queue. New systems can be added at any time, including while the data collection is in progress.

**Why this priority**: It is the only way to feed the dataset.

**Independent Test**: Import the Minitest oracle with `system = minitest` and then a file with duplicates and errors.

**Acceptance Scenarios**:

1. **Given** a UTF-8 CSV with all the columns of Appendix D, **When** the administrator uploads it, **Then** he sees the preview: how many items will be included, how many ignored and how many have errors (each with the line number and the reason), and how many blocks will be created.
2. **Given** a CSV missing one of the required columns, **When** it is uploaded, **Then** the import is refused, indicating the missing column.
3. **Given** quoted fields with commas, doubled quotes or line breaks (such as `alt_flow_steps_block` or the full use case), **When** the CSV is read, **Then** the text is preserved exactly.
4. **Given** a row whose pair (`system`, `edit_key`) already exists in the dataset, **When** the preview is generated, **Then** the row appears as ignored and the existing item is not changed.
5. **Given** the same (`system`, `edit_key`) repeated within the file itself, **When** the preview is generated, **Then** only the first occurrence is included and the others appear as errors.
6. **Given** a row that violates some rule of FR-052, **When** the preview is generated, **Then** the row appears as an error with the reason.
7. **Given** the preview with 38 valid items, **When** the administrator confirms, **Then** 4 blocks are created (10, 10, 10 and 8 items) in file order, each with roles 1 and 2 free at the end of the queue, and the import batch is recorded with date, time and file name.
8. **Given** the preview, **When** the administrator cancels, **Then** nothing is stored.
9. **Given** a data collection in progress, **When** a new import batch is imported, **Then** the existing blocks, including an incomplete final block, do not change, and the new blocks go after them in the queue.

---

### User Story 8 - Export the labels (Priority: P1)

The administrator downloads the final labels in three formats: the wide table (one row per item, with both raters), the long table (one row per response) and the per-system files that the `issta_tcm` package reads. Drafts of blocks not completed are never exported.

**Why this priority**: It is the final product of the study.

**Independent Test**: Export with completed, in-progress and free blocks and open the files in the pipeline.

**Acceptance Scenarios**:

1. **Given** completed blocks, **When** the administrator exports the wide table, **Then** he receives a UTF-8 CSV with the header of Appendix C, one row per item, in queue order, with the fields of a role not yet completed left blank.
2. **Given** completed blocks, **When** he exports the long table, **Then** he receives one row per final response with system, edit_key, block, role, participant pseudonym identifier (`R-0001`), professional skill, chosen codes, comment, response language (`language`), participant's preferred language, version and language of the accepted consent form, and block start and submission dates, without name or e-mail.
3. **Given** a system, **When** the administrator exports the pipeline files, **Then** he receives `t1_<system>_rater1.csv` and `t1_<system>_rater2.csv` with the columns `edit_key,uc,kind,field,origin,target,change_impact,maintenance_operation,comment`, in queue order.
4. **Given** a block in progress with drafts, **When** any export is generated, **Then** the drafts do not appear.
5. **Given** texts with commas, quotes or line breaks, **When** they are exported, **Then** the CSV keeps them correctly quoted.
6. **Given** a text that starts with `=`, `+`, `-` or `@`, **When** it is exported in the wide or long tables, **Then** it does not become a formula when opened in Excel or Sheets; in the pipeline files, the text is kept unchanged.

---

### User Story 9 - Follow the progress (Priority: P2)

The progress dashboard shows, per system and per import batch, how many items and blocks exist and how many tasks are free, in progress and completed, as well as how many participants signed in, were excluded by the screening or declined the consent form.

**Why this priority**: Helps decide when to promote the study further or close the data collection, but the study works without it.

**Independent Test**: Check the dashboard numbers against the long export.

**Acceptance Scenarios**:

1. **Given** blocks and responses, **When** the administrator opens the progress dashboard, **Then** he sees per system: items, blocks and tasks (role 1 and role 2) free, in progress and completed.
2. **Given** blocks in progress, **When** the administrator opens the progress dashboard, **Then** he sees, for each one, the pseudonym of whoever reserved it, how many items already have a draft and when the reservation expires.
3. **Given** registered participants, **When** he opens the progress dashboard, **Then** he sees the totals of eligible, ineligible and those who declined the consent form, and the distribution by professional skill.
4. **Given** the progress dashboard open, **When** the administrator looks at it, **Then** he sees how many e-mails can still be sent today.

---

### User Story 10 - Handle a data withdrawal request (Priority: P3)

At the request of a participant (a right under the LGPD), the administrator removes their name and e-mail and decides whether their responses stay (anonymized) or are discarded, returning the blocks to the queue.

**Why this priority**: Legal obligation, but rare; it can be done after launch.

**Independent Test**: Withdraw a participant with completed blocks and check the export and the queue.

**Acceptance Scenarios**:

1. **Given** a participant, **When** the administrator anonymizes them keeping the responses, **Then** name and e-mail disappear from all screens and exports and the responses stay linked only to the pseudonym identifier.
2. **Given** a participant, **When** the administrator discards their responses, **Then** the tasks they completed become free again, in the same position in the queue.

---

### User Story 11 - Choose the language of the screens (Priority: P1)

Before signing in, the volunteer chooses the language of the screens: Português (Brasil), English (US) or Español (España). Every screen, message, e-mail, question, explanation and the consent form follow that choice, for participants and for the administrator. The choice is remembered and can be changed at any time from the header. The use cases and the comments stay in the language in which they were captured.

**Why this priority**: The study recruits volunteers who are not all Portuguese speakers; the questions and explanations must be understood in the person's own language for the labels to be valid, and the language in use must be recorded for the analysis.

**Independent Test**: Open Veredito with browsers set to pt-BR, en-US, es-ES and fr-FR; choose each language, sign in, accept the consent form, label part of a block, switch language in the middle of the block, finish and submit it; check the screens, the e-mail, the stored `preferred_language`, the consent acceptance and the `language` of each response.

**Acceptance Scenarios**:

1. **Given** a first visit with no saved choice and a browser whose language is supported (e.g. `es-ES`), **When** Veredito opens, **Then** the first screen offers the three languages with that language preselected.
2. **Given** a first visit with a browser whose language is not supported (e.g. `fr-FR`), **When** Veredito opens, **Then** Português (Brasil) is preselected.
3. **Given** the language screen, **When** the person confirms a language, **Then** the sign-in screen and every following screen appear in that language, under the corresponding URL prefix (`/pt-BR/`, `/en-US/` or `/es-ES/`), and the choice is saved in the browser.
4. **Given** a saved choice in the browser, **When** the person comes back to Veredito, **Then** the language screen is skipped and the saved language is used.
5. **Given** the language chosen on screen, **When** the person requests an access code, **Then** the e-mail with the code is written in that language.
6. **Given** a person signing in for the first time, **When** the code is validated, **Then** the participant profile stores the current language as `preferred_language`.
7. **Given** a profile with `preferred_language = es-ES` and a browser with en-US saved, **When** the person signs in, **Then** the profile wins: the page reloads in es-ES and the browser choice is updated to es-ES.
8. **Given** any screen, **When** the person picks another language in the header, **Then** the page reloads in the new language at the same place, whatever the person had typed or chosen and not saved yet is restored, and the browser choice and, if signed in, the profile language are updated.
9. **Given** the en-US or es-ES consent form on screen, **When** it is displayed, **Then** it states that it is a translation, links to the pt-BR version approved by the research ethics committee (CEP) as the reference, and on acceptance the system records version, language and date/time.
10. **Given** a block in progress with drafts saved in pt-BR, **When** the person switches to en-US, **Then** the page reloads at the same item, the drafts and the reservation are kept, and each response saved from then on records `language = en-US`; a draft saved earlier keeps `pt-BR` unless it is saved again.
11. **Given** a block whose drafts were saved in more than one language, **When** the person submits it, **Then** each final response keeps the language of its last save.
12. **Given** the administrator, **When** he chooses a language, **Then** the administration area (import, preview, progress dashboard, exports, withdrawal) and its messages appear in that language, the choice is saved in the administrator profile (`config.admin_preferred_language`) and wins on later sign-ins, while the exported data, column names and codes stay unchanged.
13. **Given** an operation refused by the server (e.g. expired reservation), **When** the message is shown, **Then** the Problem Details `title` and `detail` are in the language in use.
14. **Given** any UI language, **When** an item is displayed, **Then** the use-case texts and edits appear exactly as captured, only the labels of Appendix A and the questions of Appendix B are localized, and the comment is stored as typed.

---

### Edge Cases

- Empty dataset: the eligible participant sees "No items to label yet" instead of an empty block.
- Two tabs of the same person: both show the same block and the same drafts; the person never holds two blocks at the same time.
- Code requested for the same e-mail on two devices: only the most recent code is valid.
- Import done during the data collection: blocks already reserved or completed do not change, and the incomplete final block of a previous batch is not completed with items from the new batch.
- Block with role 1 abandoned: the reservation expires, the drafts are discarded when another person takes it, and the block goes back to its original position in the queue, ahead of the following blocks.
- Last task disputed by several people: only one receives it; the others receive the appropriate message from User Story 5.
- Ineligible participant who tries another e-mail: the system has no way to prevent it; the long export allows auditing.
- Very long text (`origin`/`target` above 400 characters): displayed in full, with line wrapping, without cutting.
- Unknown `field` in a new system: displayed with the original name instead of the translation.
- Use case with no line marked as edited: the modal opens at the top, without highlighting.
- Language switched in the middle of a block: allowed; the page reloads at the same item, drafts and reservation are kept (the switch neither renews nor shortens the reservation), the choices and comment of the current item not saved yet are restored (still unsaved), and each response records the language in use when it was saved.
- Browser language not supported (e.g. `fr-FR`, `de-DE`): pt-BR is preselected on the first screen; the person can pick any of the three languages.
- Two tabs of the same person in different languages: each tab shows its own language; each response records the language of the tab that saved it.

## Requirements *(mandatory)*

### Functional Requirements

**Identification and access**

- **FR-001**: The system MUST identify the participant by name (2 to 120 characters) and a valid e-mail, normalized to lowercase and without spaces.
- **FR-002**: The system MUST validate the sign-in with a 6-digit code sent by e-mail, valid for 10 minutes, single-use, invalidated after 5 wrong attempts, with resending allowed after 60 seconds and at most 5 codes per e-mail per hour.
- **FR-003**: The system MUST store the codes only in a non-reversible form (hash).
- **FR-004**: The access session MUST last at most 24 hours or until the person signs out.
- **FR-005**: If the person enters a different name for an e-mail already registered, the system MUST keep the most recent name.

**Consent form, profile and screening**

- **FR-006**: The system MUST display the current consent form and record the acceptance with date, time, version and language (FR-074); declining MUST end the access without recording labels.
- **FR-007**: When the consent form changes version, the system MUST ask for a new acceptance before continuing the data labeling.
- **FR-008**: The participant MUST choose a single professional skill from the list in Appendix B.4; "Other" MUST require a description of up to 100 characters.
- **FR-009**: The participant MUST answer the screening question of Appendix B.3; the answer "No" (`no`) MUST make them ineligible, permanently for that e-mail.
- **FR-010**: Consent form, professional skill and screening MUST be requested only once per participant, except for a new version of the consent form.

**Blocks and queue**

- **FR-011**: On confirmation of each import batch, the system MUST split the new items, in file order, into blocks of 10 consecutive items; the last block of the batch gets the remainder (1 to 9 items). Blocks do not change after they are created.
- **FR-012**: Each block MUST have two tasks, role 1 and role 2. The queue is ordered by import batch, block and role: B1-R1, B1-R2, B2-R1, B2-R2...
- **FR-013**: The screen MUST show the progress of the block ("x of 10", or "x of n" in the final block).

**Allocation and concurrency**

- **FR-020**: The participant MUST receive the first free task in the queue that they can label, one at a time; while they hold a reserved block, they do not receive another one.
- **FR-021**: The system MUST NEVER hand the same person both roles of the same block, whether the other role is reserved for them or completed by them.
- **FR-022**: When handing out a task, the system MUST reserve it exclusively for the person; the reservation expires after 20 minutes (configurable value) without any saved response and is renewed with each saved response. A task whose reservation expired goes back to its original position in the queue. Each person holds at most one reserved block, with no limit of blocks during the data collection, and rater 2 may receive the block while rater 1 is still labeling it.
- **FR-022a**: Each item screen MUST show a countdown, in minutes and seconds, until the reservation expires, computed from the expiry time sent by the server (`reservationExpiresAt`); the countdown goes back to the full value with each saved response and, when it reaches zero, warns the person as described in User Story 3.
- **FR-023**: Handing out, saving a draft and submitting the block MUST be atomic operations: two simultaneous requests never receive the same task, and drafts and submission are only accepted from the person who holds the reservation, or from an expired reservation that nobody took (which renews it).
- **FR-024**: When a task whose reservation expired is handed to another person, the previous person's drafts MUST be discarded.
- **FR-025**: The responses of a block MUST stay as drafts, editable by the person, until the block is submitted; the submission is only accepted with all items answered and makes all responses final at once. Final responses cannot be changed by the participant.
- **FR-026**: Saving the same response more than once MUST NOT create duplicate records.
- **FR-027**: Each response MUST store: `change_impact` (`LOW` or `HIGH`), `maintenance_operation` (one of the eight codes of Appendix B.2), optional comment (up to 1,000 characters), participant, block, role, date/time and the UI language at save time (`language`, FR-075); the completed task stores the start and submission date/time.

**Item presentation**

- **FR-030**: The item header MUST show the system, the use case (`uc`) and the kind and location of the edit in natural language (Appendix A).
- **FR-031**: The system MUST show `origin` and `target` side by side, highlighting word by word what was removed and what was added (only `target` for `ADD`, only `origin` for `DEL`), preserving line breaks and stacking the two blocks on narrow screens.
- **FR-032**: The impact and operation questions MUST present the explanation of each option (Appendix B.1 and B.2), with a single choice in each; proceeding is only available with both answered.
- **FR-033**: Next to "Before", the system MUST offer a button that opens, in a modal, the full use case in the previous version (`origin_usecase`, labeled with `origin_version`); next to "After", another button that opens the full use case in the modified version (`target_usecase`, labeled with `target_version`). The modal preserves indentation, highlights the lines marked with `<- edited` (without displaying the marker), opens scrolled to the first of them, closes by button, Esc or click outside, and does not erase the choices made in the item.
- **FR-034**: The system MUST NEVER show a rater the responses or the identity of the other rater of the same block.

**Closing**

- **FR-040**: After a block is submitted, the system MUST offer to label another block when there is a task available to the person.
- **FR-041**: When all tasks are completed, the system MUST thank the person, state that the data labeling is complete (the whole dataset has been labeled) and end the participation.
- **FR-042**: When there are tasks not completed, but none available to the person, the system MUST thank them and state that there are no more blocks available to them at the moment.

**Administration**

- **FR-050**: Only `diegoquirino@gmail.com` (Carlos Diego Quirino Lima) MUST access the administration area, signing in with the same e-mailed code; the administrator does not label items.
- **FR-051**: The import MUST accept UTF-8 CSV (RFC 4180, including fields with line breaks) with the columns of Appendix D; extra columns are ignored.
- **FR-052**: The import MUST refuse rows with empty `system`, `edit_key`, `uc`, `kind`, `field`, `origin_version`, `target_version`, `origin_usecase` or `target_usecase`; `kind` outside `ADD`, `DEL`, `MOD`, `META`; empty `origin` in `MOD` or `DEL`; empty `target` in `MOD` or `ADD`; both `origin` and `target` empty in `META` (a `META` adds or removes a metadata entry, such as the first requirements link, and may therefore have one empty side); `origin`/`target` longer than 5,000 characters; `origin_usecase`/`target_usecase` longer than 40,000 characters; version label longer than 20 characters.
- **FR-053**: The pair (`system`, `edit_key`) MUST be unique; rows that repeat an existing item MUST be ignored without changing it; repetitions within the file MUST be refused from the second occurrence on.
- **FR-054**: The import MUST show a preview (included, ignored, with errors with line and reason, and blocks to create) and only store after confirmation; the import batch MUST be recorded with date, time and file name.
- **FR-055**: The import MUST NOT change existing items, blocks or responses.
- **FR-056**: The progress dashboard MUST show the progress per system, import batch, block and role; the reservations in progress with pseudonym, drafts and expiry; the totals of participants per status and per professional skill; and the remaining e-mail quota for the day.
- **FR-057**: The system MUST export only final responses: (a) the wide table of Appendix C; (b) the long table with pseudonym identifier; (c) per system, `t1_<system>_rater1.csv` and `t1_<system>_rater2.csv` in the format of the `issta_tcm` package.
- **FR-058**: Exports (b) and (c) MUST NOT contain name or e-mail.
- **FR-059**: The administrator MUST be able to anonymize a participant, keeping or discarding their responses; discarding returns their tasks to the queue, in the original position.
- **FR-060**: Exports (a) and (b) MUST neutralize texts that start with `=`, `+`, `-` or `@`; export (c) MUST keep the texts identical to those of the pipeline.

**Language and internationalization**

- **FR-070**: The system MUST offer the user interface in pt-BR, en-US and es-ES: participant and administrator screens, validation messages, Problem Details `title`/`detail`, e-mails, consent form, question texts, explanations and display labels. The frontend MUST use one build per locale (`@angular/localize`), served by the same Spring Boot under `/pt-BR/`, `/en-US/` and `/es-ES/`; switching language reloads the page. Backend messages MUST come from Spring `MessageSource` in the user's language.
- **FR-071**: The first screen, before sign-in, MUST offer the three languages (labels "Português (Brasil)", "English (US)", "Español (España)"), with the browser language preselected when supported, matched by primary language (`pt-*` -> pt-BR, `en-*` -> en-US, `es-*` -> es-ES), and pt-BR otherwise.
- **FR-072**: The chosen language MUST be saved in the browser and, after sign-in, in the profile (`preferred_language` of the participant; `config.admin_preferred_language` of the administrator); on later sign-ins the profile wins over the browser. The language MUST be changeable at any time from the header, on every screen, updating the browser and, when signed in, the profile.
- **FR-073**: E-mails (including the access code e-mail) MUST be sent in the UI language in use when they are requested.
- **FR-074**: Each consent form version MUST have texts in pt-BR, en-US and es-ES; the pt-BR text is the reference approved by the research ethics committee (CEP); the en-US and es-ES texts MUST state that they are translations and link to the pt-BR text. The acceptance MUST record the version, the language of the text shown and the date/time.
- **FR-075**: Data MUST NOT be translated: imported use cases and edits, comments and names stay as captured. Each response MUST record the UI language at save time (`language`); the participant's preferred language and the language of the accepted consent form MUST be recorded too. Exports (a) and (b) MUST include them (Appendix C and User Story 8); export (c) keeps the `issta_tcm` format.
- **FR-076**: Codes stored in the database and in the exports (`change_impact`, `maintenance_operation`, `professional_skill`, screening answer, `language`) MUST be fixed English identifiers or locale tags, stored the same whatever the UI language, because they are analyzed as categories after the collection; only their display labels are translated. Every user-facing string MUST exist in the three locales, checked automatically in CI (a missing translation fails the build).
- **FR-077**: When `origin_usecase` or `target_usecase` holds the pipeline text `(the use case does not exist in this version)`, the item screen MUST show a localized message instead (pt-BR "(este caso de uso não existe nesta versão)", en-US "(this use case does not exist in this version)", es-ES "(este caso de uso no existe en esta versión)"); the stored text stays as captured. The empty side of a `META` edit MUST be shown as a localized "(none)" (pt-BR "(nenhum)", en-US "(none)", es-ES "(ninguno)").
- **FR-078**: A language switch MUST NOT lose what the person typed or chose on the screen and has not saved yet (for example, the impact, the operation and the comment of the current item, or the name and e-mail on the sign-in screen): it is kept in the browser tab only, restored after the reload under the new prefix, and removed once saved or when the person leaves that screen. A selected file (batch import) cannot be restored by the browser and must be chosen again.

### Key Entities *(include if feature involves data)*

- **Import batch**: file, date/time and count of included, ignored and error rows; sets the queue order of its blocks.
- **Item**: one use-case edit to be labeled; identified by (`system`, `edit_key`); stores `uc`, `kind`, `field`, `origin`, `target`, the versions and the full texts of the use case before and after, the block and the position in the block.
- **Block**: 10 consecutive items of an import batch (the last one may have fewer); fixed position in the queue.
- **Task (block-role)**: role 1 or 2 of a block; it is free, reserved (owner, start, expiry) or completed (owner, start, submission).
- **Response**: a participant's choice for one item in one role, with the UI language at save time (`language`); it is a draft until the block is submitted and final after that.
- **Participant**: person identified by e-mail; stores name, pseudonym identifier, preferred language (`preferred_language`), consent form acceptance (version, language and date/time), professional skill, screening answer and eligibility.
- **Access code**: 6-digit code linked to an e-mail, with validity, attempts and use.
- **Consent form**: versions, one of them current; each version has texts in pt-BR (reference approved by the CEP), en-US and es-ES. An acceptance records the version, the language of the text shown and the date/time.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: In a test with 30 participants requesting blocks, saving and submitting at the same time, no task is handed to two people and nobody holds both roles of the same block (zero violations).
- **SC-002**: At the end of the data collection, every item has exactly two final responses, from two different people, and no block has a final response missing.
- **SC-003**: A new participant goes from requesting the code to the first item of the block in less than 5 minutes.
- **SC-004**: The next item appears in less than 3 seconds 95% of the time; the use-case modal opens in less than 1 second.
- **SC-005**: A block of 10 items is completed, on average, in less than 20 minutes.
- **SC-006**: Importing 500 rows, with preview and confirmation, takes less than 2 minutes.
- **SC-007**: The files `t1_<system>_rater1.csv` and `t1_<system>_rater2.csv` are read by the `agreement` and `oracle-disagreements` commands of `issta_tcm` without manual editing.
- **SC-008**: 100% of the user-facing strings (screens, messages, Problem Details, e-mails, consent form, questions, explanations and display labels) exist in pt-BR, en-US and es-ES, checked automatically in CI on every build.
- **SC-009**: A participant completes a block in each of the three languages, from the language choice to the submission, with no untranslated string on screen, and every final response records the language in which it was saved.

## Assumptions

- The user interface is in pt-BR, en-US and es-ES; the stored codes are in English (`LOW`, `HIGH`, `KEEP`, ...), as in the ISSTA 2027 pipeline.
- The use cases and edits are English data from the ISSTA_2027 dataset and are never translated, so participants need to read English regardless of the UI language; the UI language changes only the screens, questions, explanations and labels around them.
- Participation is open: anyone with an e-mail can identify themselves, without an invitation. One e-mail corresponds to one person.
- The text of the consent form (TCLE) is provided by the researcher, according to the approval of the research ethics committee; the en-US and es-ES texts are translations of the approved pt-BR text; the system only displays it and records the acceptance.
- Blocks are formed within each import batch; therefore each batch may end with a block smaller than 10, even if later batches are imported.
- Within a block, items appear in the order of the imported file; there is no randomization.
- The full use-case texts come ready in the CSV, one per version, with the edited lines marked with `<- edited`, in the same format as the current labeling spreadsheet.
- **Agreement analysis**: since rater 1 and rater 2 vary from block to block, Cohen's kappa, which assumes the same two raters for all items and is currently computed by the `agreement` command, is no longer adequate. The paper should use Fleiss' kappa or Krippendorff's alpha. This affects the analysis, not this system.

## Appendix A: Display labels of `kind` and `field`

| `kind` | pt-BR | en-US | es-ES |
|---|---|---|---|
| `MOD` | Modificação | Modification | Modificación |
| `ADD` | Inclusão | Addition | Adición |
| `DEL` | Remoção | Removal | Eliminación |
| `META` | Metadados | Metadata | Metadatos |

| `field` | pt-BR | en-US | es-ES |
|---|---|---|---|
| `step` | Passo do fluxo básico | Basic flow step | Paso del flujo básico |
| `steps_block` | Bloco de passos do fluxo básico | Block of basic flow steps | Bloque de pasos del flujo básico |
| `alt_flow_step` | Passo de fluxo alternativo | Alternative flow step | Paso de flujo alternativo |
| `alt_flow_steps_block` | Fluxo alternativo inteiro | Entire alternative flow | Flujo alternativo completo |
| `alt_flow_title` | Título de fluxo alternativo | Alternative flow title | Título de flujo alternativo |
| `exc_flow_step` | Passo de fluxo de exceção | Exception flow step | Paso de flujo de excepción |
| `exc_flow_steps_block` | Fluxo de exceção inteiro | Entire exception flow | Flujo de excepción completo |
| `exc_flow_title` | Título de fluxo de exceção | Exception flow title | Título de flujo de excepción |
| `pre` | Pré-condição | Precondition | Precondición |
| `post` | Pós-condição | Postcondition | Postcondición |
| `requirements` | Vínculo com requisitos | Requirements link | Vínculo con requisitos |
| any other value | O próprio valor, sem tradução | The value itself, untranslated | El propio valor, sin traducir |

## Appendix B: Question texts

### B.1 Change impact (`change_impact`)

**pt-BR**

**Pergunta**: "Esta edição muda o comportamento que o caso de uso especifica?"

- **Baixo** (`LOW`): a edição não muda o comportamento observável. Exemplos: reescrita, detalhamento, sinônimo, correção de digitação, pontuação, formatação, redução, reordenação, renomeação, ou mudança apenas no vínculo com requisitos. Um passo que só mudou de lugar, ou foi unido a outro ou separado dele, sem perder comportamento, também é baixo.
- **Alto** (`HIGH`): a edição muda comportamento, entradas, saídas, condições ou o fluxo. Exemplos: incluir ou remover um requisito, funcionalidade, entidade, passo, fluxo alternativo ou de exceção. Também é alto quando o texto fica vago a ponto de o testador não conseguir decidir se o teste passou ou falhou.

**en-US**

**Question**: "Does this edit change the behavior that the use case specifies?"

- **Low** (`LOW`): the edit does not change the observable behavior. Examples: rewording, added detail, synonym, typo fix, punctuation, formatting, shortening, reordering, renaming, or a change only in the requirements link. A step that was only moved, or merged with another or split from it, without losing behavior, is also low.
- **High** (`HIGH`): the edit changes behavior, inputs, outputs, conditions or the flow. Examples: adding or removing a requirement, feature, entity, step, alternative or exception flow. It is also high when the text becomes so vague that the tester cannot decide whether the test passed or failed.

**es-ES**

**Pregunta**: "¿Esta edición cambia el comportamiento que especifica el caso de uso?"

- **Bajo** (`LOW`): la edición no cambia el comportamiento observable. Ejemplos: reescritura, mayor detalle, sinónimo, corrección de erratas, puntuación, formato, reducción, reordenación, cambio de nombre, o un cambio solo en el vínculo con requisitos. Un paso que solo cambió de lugar, o que se unió a otro o se separó de él, sin perder comportamiento, también es bajo.
- **Alto** (`HIGH`): la edición cambia el comportamiento, las entradas, las salidas, las condiciones o el flujo. Ejemplos: incluir o eliminar un requisito, una funcionalidad, una entidad, un paso, un flujo alternativo o de excepción. También es alto cuando el texto queda tan vago que quien prueba no puede decidir si la prueba pasó o falló.

### B.2 Maintenance operation (`maintenance_operation`)

**Question**:

- pt-BR: "O que deve ser feito com os casos de teste manuais que exercitam este trecho?"
- en-US: "What should be done with the manual test cases that exercise this part?"
- es-ES: "¿Qué se debe hacer con los casos de prueba manuales que ejercitan este fragmento?"

**Option labels**:

| Code | pt-BR | en-US | es-ES |
|---|---|---|---|
| `KEEP` | Manter | Keep | Mantener |
| `UPDATE` | Atualizar | Update | Actualizar |
| `REMOVE` | Remover | Remove | Eliminar |
| `CREATE` | Criar | Create | Crear |
| `MERGE` | Unir | Merge | Unir |
| `SPLIT` | Dividir | Split | Dividir |
| `REASSIGN` | Revincular | Reassign | Reasignar |
| `FLAG` | Sinalizar | Flag | Señalar |

**When to choose (pt-BR)**:

| Opção | Código | Quando escolher |
|---|---|---|
| Manter | `KEEP` | O caso de teste não exercita comportamento alterado, ou a edição só reescreve o texto; ele continua executável e verificável como está. |
| Atualizar | `UPDATE` | O cenário continua existindo, mas um passo, dado, condição ou resultado esperado mudou, ou o resultado esperado precisa citar um detalhe novo. |
| Remover | `REMOVE` | O comportamento que o caso de teste verifica deixou de existir (foi apagado, não movido nem unido a outro). |
| Criar | `CREATE` | Há comportamento novo que nenhum caso de teste existente cobre. |
| Unir | `MERGE` | O comportamento foi unido ao de outro caso de teste num único cenário. |
| Dividir | `SPLIT` | Um único cenário virou vários cenários verificáveis de forma independente. |
| Revincular | `REASSIGN` | O comportamento é o mesmo, mas o vínculo com requisitos mudou. |
| Sinalizar | `FLAG` | A edição é ambígua ou deixa um passo vago; não há decisão segura e o caso precisa de análise humana. |

**Se mais de uma se aplicar**, escolha a de maior consequência: `FLAG` > `REMOVE` > `MERGE` > `SPLIT` > `CREATE` > `UPDATE` > `REASSIGN` > `KEEP`.

**When to choose (en-US)**:

| Option | Code | When to choose |
|---|---|---|
| Keep | `KEEP` | The test case does not exercise changed behavior, or the edit only rewords the text; it remains executable and verifiable as it is. |
| Update | `UPDATE` | The scenario still exists, but a step, data item, condition or expected result changed, or the expected result needs to mention a new detail. |
| Remove | `REMOVE` | The behavior the test case verifies no longer exists (it was deleted, not moved or merged with another). |
| Create | `CREATE` | There is new behavior that no existing test case covers. |
| Merge | `MERGE` | The behavior was merged with that of another test case into a single scenario. |
| Split | `SPLIT` | A single scenario became several independently verifiable scenarios. |
| Reassign | `REASSIGN` | The behavior is the same, but the requirements link changed. |
| Flag | `FLAG` | The edit is ambiguous or leaves a step vague; there is no safe decision and the case needs human analysis. |

**If more than one applies**, choose the one with the greatest consequence: `FLAG` > `REMOVE` > `MERGE` > `SPLIT` > `CREATE` > `UPDATE` > `REASSIGN` > `KEEP`.

**When to choose (es-ES)**:

| Opción | Código | Cuándo elegirla |
|---|---|---|
| Mantener | `KEEP` | El caso de prueba no ejercita comportamiento modificado, o la edición solo reescribe el texto; sigue siendo ejecutable y verificable tal como está. |
| Actualizar | `UPDATE` | El escenario sigue existiendo, pero cambió un paso, un dato, una condición o el resultado esperado, o el resultado esperado debe mencionar un detalle nuevo. |
| Eliminar | `REMOVE` | El comportamiento que verifica el caso de prueba dejó de existir (se borró; no se movió ni se unió a otro). |
| Crear | `CREATE` | Hay comportamiento nuevo que ningún caso de prueba existente cubre. |
| Unir | `MERGE` | El comportamiento se unió al de otro caso de prueba en un único escenario. |
| Dividir | `SPLIT` | Un único escenario se convirtió en varios escenarios verificables de forma independiente. |
| Reasignar | `REASSIGN` | El comportamiento es el mismo, pero cambió el vínculo con requisitos. |
| Señalar | `FLAG` | La edición es ambigua o deja un paso vago; no hay una decisión segura y el caso necesita análisis humano. |

**Si se aplica más de una**, elige la de mayor consecuencia: `FLAG` > `REMOVE` > `MERGE` > `SPLIT` > `CREATE` > `UPDATE` > `REASSIGN` > `KEEP`.

### B.3 Screening question

Before the question, the screen shows a short use-case excerpt (basic flow with one exception flow) and a test case derived from it. Like the study items, the excerpt and the test case are shown in English in every locale; the question, the options and the surrounding instructions are localized.

**Question**:

- pt-BR: "Você já escreveu, executou ou revisou casos de teste a partir de casos de uso como este, com fluxo básico, fluxos alternativos e de exceção?"
- en-US: "Have you ever written, executed or reviewed test cases based on use cases like this one, with a basic flow and alternative and exception flows?"
- es-ES: "¿Alguna vez has escrito, ejecutado o revisado casos de prueba a partir de casos de uso como este, con flujo básico, flujos alternativos y de excepción?"

| Code | pt-BR | en-US | es-ES | Result |
|---|---|---|---|---|
| `yes_professional` | Sim, na prática profissional | Yes, in professional practice | Sí, en la práctica profesional | eligible |
| `yes_academic` | Sim, em estudos, cursos ou pesquisa | Yes, in studies, courses or research | Sí, en estudios, cursos o investigación | eligible |
| `no` | Não | No | No | ineligible; ends with a thank-you message |

### B.4 Professional skill (`professional_skill`)

| Code | pt-BR | en-US | es-ES |
|---|---|---|---|
| `developer` | Desenvolvedor(a) de software | Software developer | Desarrollador(a) de software |
| `software_engineer` | Engenheiro(a) de software | Software engineer | Ingeniero(a) de software |
| `systems_analyst` | Analista de sistemas | Systems analyst | Analista de sistemas |
| `requirements_analyst` | Analista de requisitos ou de negócios | Requirements or business analyst | Analista de requisitos o de negocio |
| `test_analyst` | Analista de testes / QA | Test analyst / QA | Analista de pruebas / QA |
| `test_engineer` | Engenheiro(a) de testes ou de automação de testes | Test or test automation engineer | Ingeniero(a) de pruebas o de automatización de pruebas |
| `software_architect` | Arquiteto(a) de software | Software architect | Arquitecto(a) de software |
| `tech_lead` | Líder técnico(a) | Tech lead | Líder técnico(a) |
| `product_project_manager` | Product owner, gerente de produto ou de projetos | Product owner, product manager or project manager | Product owner, responsable de producto o jefe(a) de proyectos |
| `professor_researcher` | Docente ou pesquisador(a) em computação | Computing professor or researcher | Docente o investigador(a) en informática |
| `graduate_student` | Estudante de pós-graduação em computação | Graduate student in computing | Estudiante de posgrado en informática |
| `undergraduate_student` | Estudante de graduação em computação | Undergraduate student in computing | Estudiante de grado en informática |
| `other` | Outro (descrever) | Other (describe) | Otro (describir) |

## Appendix C: Wide export header

```text
system,edit_key,uc,kind,field,origin,target,rater_1_name,rater_1_email,rater_1_accepted_research_terms_lgpd,rater_1_professional_skill,rater_1_change_impact_choice,rater_1_maintenance_operation_choice,rater_1_comment,rater_1_response_language,rater_2_name,rater_2_email,rater_2_accepted_research_terms_lgpd,rater_2_professional_skill,rater_2_change_impact_choice,rater_2_maintenance_operation_choice,rater_2_comment,rater_2_response_language
```

`rater_N_accepted_research_terms_lgpd` holds the version, the language and the date/time of the consent form acceptance (for example, `v1 pt-BR 2026-11-20T14:03:00-03:00`). `rater_N_response_language` holds the `language` of that rater's final response to the item (`pt-BR`, `en-US` or `es-ES`). `rater_N_professional_skill` holds the code of Appendix B.4. All values are exported as captured, without translation.

## Appendix D: Import CSV columns

| Column | Required | Content |
|---|---|---|
| `system` | yes | Name of the system under study, for example `minitest` |
| `edit_key` | yes | Identifier of the edit within the system, for example `LoginTest\|1\|1` |
| `uc` | yes | Use case, for example `LoginTest` |
| `kind` | yes | `ADD`, `DEL`, `MOD` or `META` |
| `field` | yes | Location of the edit (Appendix A) |
| `origin` | in `MOD` and `DEL`; in `META`, `origin` or `target` | Text before the edit |
| `target` | in `MOD` and `ADD`; in `META`, `origin` or `target` | Text after the edit |
| `origin_version` | yes | Label of the previous version, for example `v1.1` |
| `target_version` | yes | Label of the modified version, for example `v1.2` |
| `origin_usecase` | yes | Full use case in the previous version, edited lines marked with `<- edited`; when the use case does not exist in that version, the pipeline text `(the use case does not exist in this version)`, kept as captured (FR-077) |
| `target_usecase` | yes | Full use case in the modified version, likewise |

## Appendix E: Queue example

Dataset of 205 items imported in a single batch: 20 blocks of 10 items and block 21 with 5 items, or 42 tasks.

| Order | Task | Items | Who can receive it |
|---|---|---|---|
| 1 | B1-R1 | 1-10 | any eligible participant |
| 2 | B1-R2 | 1-10 | anyone except whoever has B1-R1 |
| 3 | B2-R1 | 11-20 | any eligible participant |
| 4 | B2-R2 | 11-20 | anyone except whoever has B2-R1 |
| ... | ... | ... | ... |
| 41 | B21-R1 | 201-205 | any eligible participant |
| 42 | B21-R2 | 201-205 | anyone except whoever has B21-R1 |
