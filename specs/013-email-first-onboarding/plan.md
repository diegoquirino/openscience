# Implementation Plan: E-mail-first onboarding

**Branch**: `013-email-first-onboarding` | **Date**: 2026-11-19 | **Spec**: [spec.md](./spec.md)

## Summary

Sign-in asks only for e-mail. After the code, the participant completes name and professional skill (profile), then consent, then screening. App version **0.13.0**.

## Technical Context

**App version**: 0.13.0. Flyway V8 relaxes participant name for active rows. OnboardingStep adds `profile`. Skill PUT carries name + skill.

## Constitution Check

Pass: stack unchanged; HAL `vd:skill` extended; i18n updated; deploy only with approval.
