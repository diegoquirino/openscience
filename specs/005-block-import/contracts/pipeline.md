# Contract: Pipeline and configuration for spec 005

**Feature**: `005-block-import` | Extends
[spec 003 pipeline](../../003-deployment-foundation/contracts/pipeline.md) and
[spec 004 pipeline](../../004-email-code-access/contracts/pipeline.md).

## Application version

| Place | Value |
|---|---|
| `backend/pom.xml` / `frontend/package.json` | `0.5.0` |
| Deploy tags | `deploy/0.5.0-1`, `deploy/0.5.0-2`, … |
| Footer | `0.5.0+<git sha>` as in 003/004 |

## Multipart (application.yaml)

```yaml
spring:
  servlet:
    multipart:
      max-file-size: 10MB
      max-request-size: 10MB
```

Same values in every profile. No new environment variable and no new Secret Manager secret.

## Profiles

| Profile | Import gate | Study-phase switch |
|---|---|---|
| default / test | Minitest only when `study_phase` is `pilot` (tests set the phase explicitly) | Allowed when emptiness rules hold |
| `staging` | Always Minitest only | Resource absent (`404`) |
| `prod` | Minitest only while `pilot`; any system in `collection` | Allowed when emptiness rules hold |

## Deploy workflow

No change to `deploy.yml` secrets. Image build and Cloud Run deploy stay as in 004; the tag series
moves to `0.5.0-*`.

## Fixtures in the image / tests

| Path | Use |
|---|---|
| `backend/src/test/resources/minitest/veredito_minitest_vazio.csv` | SC-005 / SC-003 of 002 |
| `…/minitest_duplicates.csv` | US1 scenarios 4–5, SC-004 |
| `…/minitest_missing_kind_column.csv` | US1 scenario 2 |
| `…/minitest_long_text.csv` | FR-052 length / quoted newlines |
| `…/other_system_sample.csv` | `minitest-only` and phase-switch tests |

Local staging continues to import through the UI from the researcher's machine (the CSV of
`ISSTA_2027` / the fixture), not from a file baked into the runtime image.
