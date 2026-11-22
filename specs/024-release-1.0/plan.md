# Implementation Plan: MIT footer and 1.0 promote gate

**Branch**: `024-release-1.0` | **Date**: 2026-11-21 | **Spec**: [spec.md](spec.md)

## Summary

Deliver the discreet MIT-style footer (`v{{ version }} · MIT License`) at app version **0.24.0**. Document that product **1.0.0** is released only after CEP/LGPD consent is read/approved and the researcher says **promover a 1.0 e deploy** on the last auto-tested stable build. Revert any premature `1.0.0` defaults to `0.24.0`.

## Technical approach

| Area | Change |
|---|---|
| Version defaults | pom, package.json/lock, compose, Dockerfile, SystemVersion.FALLBACK, write-build-info.mjs ? **0.24.0** (not 1.0.0) |
| UI | `shell.component.html` / scss / spec; i18n units |
| Docs | ROADMAP 024 = footer + gate; feature.json |

## Environments

Local Docker staging; production Cloud Run only after explicit **promover a 1.0 e deploy**.
