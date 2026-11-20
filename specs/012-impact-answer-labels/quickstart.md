# Quickstart: Impact answer labels

**Feature**: `012-impact-answer-labels` | Contract: [web](contracts/web.md)

## 1. Prerequisites

- Staging: `docker compose --profile staging up --build` with `VEREDITO_VERSION=0.12.0+…`.
- Minitest imported; an eligible participant with a reserved block.

## 2. Labels

1. Open an item in pt-BR: options `Não (baixo impacto)` / `Sim (alto impacto)` with explanations under each.
2. Switch to en-US / es-ES and confirm the localized No/Yes labels.
3. Save a draft; export or API still has `LOW` or `HIGH`.

## 3. Required markers

1. Impact and operation legends show `*`.
2. Comment stays optional.

## 4. Production (only with explicit approval)

1. CI green.
2. `git push origin <commit>:refs/tags/deploy/0.12.0-1`
3. Footer / `/api/v1` shows `0.12.0+…`.
