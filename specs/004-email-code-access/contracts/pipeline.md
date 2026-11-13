# Contract: Changes to configuration and deployment

**Feature**: `004-email-code-access` | On top of the
[pipeline of spec 003](../../003-deployment-foundation/contracts/pipeline.md).

## Application variables

| Variable | Production (Cloud Run) | Local staging (Compose) | Tests |
|---|---|---|---|
| `CODE_HASH_KEY` | Secret `veredito-code-key:latest` (32 random bytes, base64url) | Fixed non-secret value in `compose.yaml`, with a comment saying it is for staging only | Random per run |
| `SMTP_HOST`, `SMTP_PORT`, `SMTP_USER` | `smtp.gmail.com`, `587`, `diegoquirino@gmail.com` (spec 003) | `mailpit`, `1025`, none | Mailpit container |
| `SMTP_PASSWORD` | Secret `veredito-smtp-password:latest` (spec 003) | none | none |

The application refuses to start without `CODE_HASH_KEY`, or with a key shorter than 32 bytes, in
the `prod` profile. In development (no profile), a missing key is replaced by a random one at
startup, with a log line saying so.

`.env.example` documents `CODE_HASH_KEY` without a value.

## Google Cloud (one time, researcher)

Added to `docs/cloud-setup.md`, step 5 (secrets) and step 6 (roles):

| Action | Command (summary) |
|---|---|
| Create the secret without showing it | `New-SecretFromValue veredito-code-key (New-RandomPassword)` (helpers of step 5) |
| Let the runtime read it | `gcloud secrets add-iam-policy-binding veredito-code-key --member=serviceAccount:veredito-run@... --role=roles/secretmanager.secretAccessor` |

No new service, API or role type: Secret Manager is already allowed by Principle V.

## `deploy.yml`

`--set-secrets` gains `CODE_HASH_KEY=veredito-code-key:latest`:

```text
--set-secrets "DB_PASSWORD=veredito-db-password:latest,SMTP_PASSWORD=veredito-smtp-password:latest,CODE_HASH_KEY=veredito-code-key:latest"
```

If the secret does not exist when the deployment runs, Cloud Run refuses the new revision and the
previous one keeps 100 % of the traffic. The secret is created before the first deployment of 004.

## `ci.yml`

No change to the jobs. The `backend` job also starts a Mailpit container through Testcontainers.
The `e2e` job already starts Mailpit with the Compose profile `staging`; Playwright reads the codes
from `http://localhost:8025/api/v1/`.

## Rollback

V3 only adds two columns and two indexes, so the revision of spec 003 keeps working on the migrated
database. After a rollback, sessions created by 004 are ignored by the code of 003, which has no
sessions.
