# Feature Specification: E-mail-first sign-in and profile before consent

**Feature Branch**: `013-email-first-onboarding`

**Created**: 2026-11-19

**Status**: Complete

**Input**: User description: "The first screen is e-mail only. After the code confirms sign-in, the person fills name and professional skill, then the consent form, then screening."

**Parent spec**: [001-oracle-labeling](../001-oracle-labeling/spec.md) US1/US2 · **Depends on**: 004, 006 · **Amends**: FR-001 (name after code); skill before consent

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Request a code with e-mail only (Priority: P1)

**Acceptance Scenarios**:

1. **Given** the sign-in screen, **When** opened, **Then** only the e-mail field is shown (no name).
2. **Given** a valid e-mail, **When** the person asks for a code, **Then** a code is sent and the code screen opens.
3. **Given** a missing or invalid e-mail, **When** submitted, **Then** the request is refused with a field error.

### User Story 2 - Complete name and skill before the consent form (Priority: P1)

**Acceptance Scenarios**:

1. **Given** a new participant after a valid code, **When** they continue, **Then** they see the profile step (name and professional skill) before the consent form.
2. **Given** the profile step, **When** name or skill is missing, **Then** they cannot proceed.
3. **Given** a completed profile, **When** they proceed, **Then** the next step is the consent form, then screening as today.
4. **Given** a returning participant who already has name and skill, **When** they sign in, **Then** they skip the profile step.

### Edge Cases

- Administrator: e-mail only for the code; if admin name is missing, a name-only profile step applies (no skill).
- Anonymized e-mail signing in again: treated as a new person (unchanged from 011).

## Requirements *(mandatory)*

- **FR-001**: Code request MUST accept only e-mail.
- **FR-002**: After sign-in, participants without name or skill MUST complete both before consent.
- **FR-003**: Onboarding order MUST be profile ? consent ? screening ? participation.
- **FR-004**: Stored skill codes remain Appendix B.4; name rules 2–120 characters.
- **FR-005**: Existing participants with name and skill MUST NOT be forced through profile again.

## Success Criteria *(mandatory)*

- **SC-001**: A new volunteer reaches consent only after name and skill are stored.
- **SC-002**: Automated tests cover e-mail-only code request and profile-before-consent order.
