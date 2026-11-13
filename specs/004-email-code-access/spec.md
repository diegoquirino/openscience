# Feature Specification: Sign in with name, e-mail and code

**Feature Branch**: `004-email-code-access`

**Created**: 2026-11-10

**Status**: Draft

**Input**: User description: "Participants and the administrator sign in by entering name and e-mail and typing a 6-digit code sent by e-mail, with no invitation and no password. The administrator (diegoquirino@gmail.com) goes to the administrative area; everyone else goes on to the participation flow."

**Overall spec**: [001-oracle-labeling](../001-oracle-labeling/spec.md), User Stories 1 and 6, FR-001 to FR-005 and FR-050 · **Depends on**: 003

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Sign in with the code sent by e-mail (Priority: P1)

A volunteer enters name and e-mail, receives a 6-digit code and types it to sign in.

**Why this priority**: All the participation and the administrative area depend on knowing who the person is; it is the bottleneck of all the following specs.

**Independent Test**: Sign in with a new e-mail; try a wrong, expired and reused code.

**Acceptance Scenarios**:

1. **Given** the "Participate" screen, **When** the person enters a name (2 to 120 characters) and a valid e-mail, **Then** the system sends a 6-digit code, says that it is valid for 10 minutes and warns to check the spam folder.
2. **Given** an invalid name or e-mail, **When** the person tries to continue, **Then** the system points out the field with the problem and does not send a code.
3. **Given** a code sent less than 10 minutes ago, **When** the person types the correct code, **Then** they sign in to the system.
4. **Given** a valid code, **When** the person gets it wrong 5 times, **Then** the code is invalidated and they need to request another one.
5. **Given** an expired or already used code, **When** the person types it, **Then** the system refuses it and offers to send a new code.
6. **Given** a code sent less than 60 seconds ago, **When** the person requests another one, **Then** the system says how many seconds are left.
7. **Given** 5 codes requested for the same e-mail in the last hour, **When** the person requests the sixth, **Then** the system refuses and says when they can request again.
8. **Given** `Ana@X.com ` and `ana@x.com`, **When** they are entered, **Then** the system treats them as the same participant.
9. **Given** the daily e-mail sending limit reached, **When** someone requests a code, **Then** the system says that it cannot send codes today and suggests trying the next day.
10. **Given** an e-mail signing in for the first time, **When** the code is validated, **Then** the participant is registered with a sequential pseudonym (`R-0001`, `R-0002`, …).
11. **Given** an e-mail already registered with another name, **When** the code is validated, **Then** the system keeps the most recent name.

---

### User Story 2 - Go to the right area after signing in (Priority: P1)

The administrator goes to the administrative area; everyone else goes on to the participation flow. Nobody else accesses the administrative area.

**Why this priority**: It separates the two audiences from the start and protects the functions that specs 005, 009, 010 and 011 will add.

**Independent Test**: Sign in with `diegoquirino@gmail.com` and with another e-mail; call an administrative function without an administrator session.

**Acceptance Scenarios**:

1. **Given** the e-mail `diegoquirino@gmail.com`, **When** the code is validated, **Then** the administrative area opens, with the administrator's name and the menu (Import, Export, Dashboard, Participants), whose items not yet delivered appear disabled.
2. **Given** any other e-mail, **When** the code is validated, **Then** the person goes on to the next step of the participation (provisionally, a screen "Thank you for identifying yourself; the next step will be available soon").
3. **Given** a person who is not the administrator, **When** they try to open an administrative screen or call an administrative function, **Then** the access is denied and the attempt is logged.
4. **Given** the administrator, **When** they sign in, **Then** they do not receive a pseudonym and cannot enter the data labeling flow.

---

### User Story 3 - Stay signed in and sign out (Priority: P2)

Whoever signed in stays signed in on the same browser for up to 24 hours and can sign out at any time.

**Why this priority**: It saves e-mail sends, which have a daily limit, without requiring a code on every visit.

**Independent Test**: Sign in, close and reopen the browser; sign out and try to come back.

**Acceptance Scenarios**:

1. **Given** a person who signed in less than 24 hours ago, **When** they reopen Veredito on the same browser, **Then** they stay signed in, with no new code.
2. **Given** a session older than 24 hours, **When** the person takes any action, **Then** they go back to the identification screen.
3. **Given** a signed-in person, **When** they click "Sign out", **Then** the session ends and they go back to the home page.

---

### User Story 4 - Choose the language before signing in (Priority: P1)

Before signing in, the person chooses the language of the interface among Portuguese (Brazil), English (US) and Spanish (Spain). The choice is remembered by the browser and, after sign-in, by the participant profile, and can be changed at any time from the header. The access-code e-mail and the administrative area follow the chosen language.

**Why this priority**: Volunteers from Brazil, other Spanish-speaking countries and English-speaking countries must understand the screens, the consent form and the questions from the first contact; the language must be settled before the first e-mail is sent.

**Independent Test**: In local staging, with browsers set to `pt-BR`, `es-MX`, `en-GB` and `fr-FR`, open `/`, check the preselection, choose a language, request a code and check the e-mail in the test mailbox; sign in again from a browser with another saved language; switch language from the header on a screen with a saved draft.

**Acceptance Scenarios**:

1. **Given** a person opening `/` with no saved language, **When** the page loads, **Then** before any sign-in they see the language-choice screen with the three options "Português (Brasil)", "English (US)" and "Español (España)", each one labeled in its own language.
2. **Given** a browser whose preferred language is one of the three supported languages (matched by language: Portuguese, English or Spanish, for example `es-MX` preselects "Español (España)"), **When** the language-choice screen opens, **Then** that option is preselected; with any other browser language (for example `fr-FR`), "Português (Brasil)" is preselected.
3. **Given** the language-choice screen, **When** the person confirms a language, **Then** it is saved in the browser (cookie `veredito-lang`) and the home page opens in that language, under its prefix (`/pt-BR/`, `/en-US/` or `/es-ES/`).
4. **Given** a language already saved in the browser, **When** the person opens `/` again, **Then** they go straight to the home page in that language, without the language-choice screen.
5. **Given** a person who chose a language, **When** they request an access code, **Then** the e-mail (subject and body) arrives in the language of the interface in use at that moment.
6. **Given** a participant signing in for the first time, **When** the code is validated, **Then** the language in use is saved in the profile as the preferred language (`preferred_language`).
7. **Given** a participant whose profile has `es-ES` as preferred language, signing in on a browser that has `en-US` saved (or nothing saved), **When** the code is validated, **Then** the profile wins: the interface reloads in Spanish (Spain) and the browser now keeps `es-ES`.
8. **Given** any screen, before or after sign-in, **When** the person chooses another language from the header, **Then** the page reloads in the new language on the same screen, responses already saved as drafts are kept, whatever was typed or chosen and not saved yet (for example, name and e-mail on the sign-in screen) is restored, the browser keeps the new language and, if someone is signed in, the profile language is updated (`preferred_language` of the participant, `config.admin_preferred_language` of the administrator).
9. **Given** the administrator, **When** they sign in, **Then** the administrative area (menu, screens, messages and the provisional screens) is shown in the chosen language, with the same header language switch; the first sign-in saves that language in the administrator profile, and on later sign-ins, from any browser, the profile language wins.
10. **Given** any message of this spec (invalid field, wrong or expired code, seconds left before resending, hourly limit, daily e-mail limit, sending error), **When** it is shown, **Then** it appears in the language of the interface in use.

---

### Edge Cases

- Code requested on two devices for the same e-mail: only the most recent one is valid.
- E-mail that does not arrive: the code screen offers "Resend" (respecting the limits) and "Correct e-mail".
- Code typed with spaces or hyphens: the spaces and hyphens are ignored.
- Error when sending the e-mail: the person sees "The code could not be sent right now" and no code becomes valid.
- Wrong-code messages do not reveal whether the e-mail is already registered.
- The access-code e-mail uses the language of the interface in use, not the profile's preferred language: the profile is not looked up before sign-in, so the language of the e-mail does not reveal whether the e-mail is registered.
- Cookie `veredito-lang` with a value that is not one of the three locales: it is ignored and the person sees the language-choice screen.
- Person who opens an address with a locale prefix directly (for example, an invitation link to `/en-US/`) with no saved language: the interface opens in that language and the browser saves it, without going through the language-choice screen.
- Language switched from the header while typing something not yet saved: no warning; the typed values are kept in the browser tab (`sessionStorage`) and restored, still unsaved, after the reload under the new prefix. A file already selected for import cannot be restored and must be chosen again.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST identify the person by name (2 to 120 characters) and valid e-mail, normalized to lowercase and without spaces.
- **FR-002**: The system MUST validate the sign-in with a 6-digit code sent by e-mail, valid for 10 minutes, single use, invalidated after 5 wrong attempts, with resending after 60 seconds and at most 5 codes per e-mail per hour.
- **FR-003**: Codes and session identifiers MUST be stored only in a non-reversible form.
- **FR-004**: The session MUST last at most 24 hours or until the person signs out.
- **FR-005**: The system MUST keep the most recent name entered for an e-mail.
- **FR-006**: Each new participant MUST receive a sequential and unique pseudonym.
- **FR-007**: Only the administrator e-mail of the configuration MUST access the administrative area; the administrator does not take part in the data labeling.
- **FR-008**: Every function, except requesting and validating a code, MUST require a valid session, and the administrative ones, an administrator session.
- **FR-009**: The system MUST count the e-mails sent in the day, stop at the daily limit of the configuration and say when it is reached.
- **FR-010**: Before sign-in, the first screen (`/`) MUST be the language choice among pt-BR, en-US and es-ES, each option labeled in its own language, with the browser's preferred language preselected when it is supported (matched by language: Portuguese, English or Spanish) and pt-BR otherwise.
- **FR-011**: The chosen language MUST be saved in the browser (cookie `veredito-lang`); with a valid saved language, `/` MUST go straight to the home page under that locale's prefix.
- **FR-012**: When the code is validated, the system MUST save the language in use as the profile language of whoever does not have one yet (`preferred_language` of the participant, `config.admin_preferred_language` of the administrator); when the profile already has a language, the profile MUST win: the interface switches to that language and the browser cookie is updated.
- **FR-013**: The header MUST offer the language switch on every screen, before and after sign-in, for participants and the administrator; switching MUST reload the same screen under the new prefix, keep the responses already saved as drafts, restore what was typed and not saved yet (spec 001, FR-078), update the cookie `veredito-lang` and, when someone is signed in, the profile language (`preferred_language` of the participant, `config.admin_preferred_language` of the administrator).
- **FR-014**: The access-code e-mail (subject and body) and every message of this spec (screens, validation messages, Problem Details `title` and `detail`) MUST be in the language of the interface in use at the time of the request, in each of the three locales.
- **FR-015**: The administrative area MUST be fully available in pt-BR, en-US and es-ES, following the same language choice and the same header switch as the participant screens.

### Key Entities

- **Participant**: person identified by e-mail; name, pseudonym, registration date, preferred language (`preferred_language`).
- **Access code**: linked to an e-mail; validity, attempts, use.
- **Access session**: linked to a participant or to the administrator; validity.
- **Language choice in the browser**: cookie `veredito-lang` with `pt-BR`, `en-US` or `es-ES`; used before sign-in.
- **Administrator profile language**: `config.admin_preferred_language` (`pt-BR`, `en-US` or `es-ES`), read and changed through `/api/v1/administrator/me` (`vd:me`).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A new person goes from the "Participate" screen to the screen after sign-in in less than 3 minutes.
- **SC-002**: In 95 % of the sends, the code arrives in less than 1 minute.
- **SC-003**: In a test with all administrative functions called by a regular session and without a session, 100 % of the calls are denied.
- **SC-004**: A person who comes back on the same day does not use a new e-mail send.
- **SC-005**: In local staging, a person completes the sign-in in each of the three languages (from the language-choice screen to the screen after sign-in), with no text in another language on any screen, and the access-code e-mail arrives in the chosen language in 100 % of the tests, only in the local test mailbox (closes the e-mail part of US4 scenario 2 of spec 003).
- **SC-006**: In 100 % of the tests where a participant with a preferred language signs in on a browser with another saved language, the interface switches to the profile language right after sign-in.

## Assumptions

- The code e-mail is sent from the researcher's account and may land in spam.
- In production, the e-mail is sent from the researcher's Gmail account, which accepts about 500 recipients per day; the system default limit is 450, leaving a margin (see [research.md](../001-oracle-labeling/research.md) of spec 001).
- In local staging (Docker, profile `staging`), the codes go to a local test mailbox and do not reach anyone; the daily limit can be reduced to test the exhausted-quota message.
- The consent form, the profile and the screening (spec 006) come between the sign-in and the participation; until then, the provisional screen takes that place.
- The routing of the locale prefixes, the serving of the three builds, the message bundles and the CI completeness checks come from spec 003; this spec delivers the language-choice screen and its behaviour.
- The e-mail texts exist in the three languages; pt-BR is the reference version, as for the consent form approved by the research ethics committee.
- The administrator is not a participant (no row in `participant`); their profile language is kept in the configuration (`config.admin_preferred_language`), as there is a single administrator.
