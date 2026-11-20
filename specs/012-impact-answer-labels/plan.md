# Implementation Plan: Change-impact answer labels and required markers

**Branch**: `012-impact-answer-labels` | **Date**: 2026-11-19 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/012-impact-answer-labels/spec.md`

## Summary

Spec 012 only changes how participants *see* the change-impact choices and that impact/operation are marked required. Codes `LOW`/`HIGH` and the explanation copy stay.

- Short labels via `$localize` in `response-labels.ts` + XLIFF.
- `*` on item legends (impact + operation).
- Review screen inherits labels.
- Appendix B.1 display names in 001 updated; codes unchanged.
- App version **0.12.0**.

## Technical Context

**Language/Version**: Java 25 / Spring Boot 4.1.2; Angular 22; Node 26

**App version**: **0.12.0**. Deploy tags: `deploy/0.12.0-*`.

**Primary Dependencies**: existing `@angular/localize`, Material radios on the item screen.

**Storage**: unchanged.

**Testing**: Vitest on labels and item legends; Playwright if an item-flow assertion already covers labels.

**Target Platform**: Cloud Run; Docker Compose staging.

## Constitution Check

| Gate | Plan | Status |
|---|---|---|
| I. Stack standard; versions recorded | 0.12.0; no new dependency | Pass |
| II. Isolated domain rules | No domain rule change | Pass |
| III. HAL / OpenAPI | No new resource | Pass |
| IV. Privacy / LGPD | No PII change | Pass |
| V. Material / i18n three locales | XLIFF updated | Pass |
| Workflow: staging then tag deploy | Quickstart; approval for tag | Pass |
| UTF-8 | As in prior specs | Pass |

## Project Structure

```text
frontend/src/app/core/labels/response-labels.ts
frontend/src/app/label/item/item.component.html
frontend/src/app/label/item/item.component.ts
frontend/src/locale/messages.pt-BR.xlf
frontend/src/locale/messages.es-ES.xlf
specs/001-oracle-labeling/spec.md  (Appendix B.1 display)
specs/012-impact-answer-labels/
```

## Complexity Tracking

None — UI/i18n only.
