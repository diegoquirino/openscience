# Research: Change-impact answer labels and required markers

**Feature**: `012-impact-answer-labels` | **Date**: 2026-11-19 | **Spec**: [spec.md](spec.md)

## Decisions

| Topic | Decision | Rationale |
|---|---|---|
| App version | **0.12.0** | Spec number in the minor digit |
| Deploy tags | `deploy/0.12.0-*` | Production only with explicit approval |
| Stored codes | Keep `LOW` / `HIGH` | Pipeline and exports already fixed |
| Short labels | No/Yes + impact level in parentheses | Matches the yes/no question wording |
| Explanations | Unchanged `item.impact.LOW` / `HIGH` | Researcher-approved Appendix B.1 body |
| Required marker | Asterisk on impact and operation legends only | Comment stays optional |
| Review screen | Reuse `changeImpactLabel` | Single source of short labels |

## Alternatives considered

- Renaming codes to `NO`/`YES`: rejected — would break exports and ISSTA_2027 prompts.
- Rewriting explanations to start with "No:" / "Yes:": rejected — user asked to keep current explanations.
