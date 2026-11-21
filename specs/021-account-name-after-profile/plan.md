# Plan: 021-account-name-after-profile

## Summary

Root cause: `SkillComponent.submit` navigates to `/welcome` after `recordSkill` without refreshing the session; the shell still has `name: null` from sign-in and shows `@@shell.account`. Profile already calls `session.refresh()` in `afterSave`. Align onboarding skill (and any other name writes that skip refresh) with the same pattern. Version **0.21.0**.

## Technical approach

- In `skill.component.ts`, after successful `recordSkill`, call `session.refresh()` then navigate (or navigate after refresh completes / fails softly like profile).
- Inject `SessionService` into the skill component if not already present.
- Update `skill.component.spec.ts` to assert refresh is invoked and/or session name is set.
- Optional: e2e assert account menu text after completing profile in an existing onboarding path (lightweight).

## Contracts

No API/HAL changes.

## Target platform

Local Docker staging; production Cloud Run (deploy only with approval).
