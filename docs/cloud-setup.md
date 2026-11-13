# Google Cloud setup

One-time setup of the production environment of Veredito (research section 19 of
`specs/003-deployment-foundation`). The commands are for PowerShell, run from the repository root,
with the Google Cloud CLI installed. After this setup, every production deployment goes through
`.github/workflows/deploy.yml`.

Who runs each step:

- **Researcher, by hand:** step 0 (project, billing, budget, sign-in) and step 5 (secrets). Payment
  data, second factors and secret values never go through the agent, the repository, logs or
  screenshots.
- **Agent or researcher:** all other steps, with the researcher's authenticated `gcloud`.

## 0. Project, billing and budget (researcher, by hand)

1. Create the project `veredito-issta2027` in the console.
2. Link it to the billing account.
3. Create the budget in **Billing > Budgets & alerts**:
   - name "Veredito mensal";
   - scope: only the project `veredito-issta2027`;
   - amount: R$ 200 per month;
   - alerts at 50 %, 90 % and 100 % of the actual cost, by e-mail;
   - no action on the services when the budget is reached.
4. Sign in and select the project:

```powershell
gcloud auth login
gcloud config set project veredito-issta2027
```

## 1. Session variables

```powershell
$project = "veredito-issta2027"
$region = "southamerica-east1"
$repository = "diegoquirino/openscience"
$projectNumber = gcloud projects describe $project --format="value(projectNumber)"
$runAccount = "veredito-run@$project.iam.gserviceaccount.com"
$ciAccount = "veredito-ci@$project.iam.gserviceaccount.com"
$deployAccount = "veredito-deploy@$project.iam.gserviceaccount.com"
$pool = "projects/$projectNumber/locations/global/workloadIdentityPools/github"
```

## 2. APIs

```powershell
gcloud services enable `
  run.googleapis.com `
  sqladmin.googleapis.com `
  secretmanager.googleapis.com `
  artifactregistry.googleapis.com `
  iam.googleapis.com `
  iamcredentials.googleapis.com `
  sts.googleapis.com `
  cloudresourcemanager.googleapis.com `
  billingbudgets.googleapis.com
```

## 3. Artifact Registry

Docker repository `veredito`, keeping the 5 most recent images (`docs/artifact-registry-cleanup.json`):

```powershell
gcloud artifacts repositories create veredito `
  --repository-format=docker `
  --location=$region `
  --description="Veredito application images"

gcloud artifacts repositories set-cleanup-policies veredito `
  --location=$region `
  --policy=docs/artifact-registry-cleanup.json `
  --no-dry-run
```

## 4. Cloud SQL

MySQL 9.7 in the Enterprise edition (Enterprise Plus, the default for 9.7, has no shared-core
machine). The instance keeps its public IP with no authorized networks, and only clients that use
the Cloud SQL connector with IAM authorization can connect. Daily backups are kept for 7 days.
Creating the instance takes about 10 minutes.

```powershell
gcloud sql instances create veredito-db `
  --database-version=MYSQL_9_7 `
  --edition=ENTERPRISE `
  --tier=db-f1-micro `
  --region=$region `
  --storage-type=SSD `
  --storage-size=10GB `
  --connector-enforcement=REQUIRED `
  --backup-start-time=06:00 `
  --retained-backups-count=7

gcloud sql databases create veredito --instance=veredito-db `
  --charset=utf8mb4 --collation=utf8mb4_0900_ai_ci
```

Check that no network is authorized (the output must be empty):

```powershell
gcloud sql instances describe veredito-db --format="value(settings.ipConfiguration.authorizedNetworks)"
```

## 5. Database users and secrets (researcher, by hand)

Run this block in your own terminal. It generates random passwords for `root` and for the
application user `veredito_app`, stores only the application password in Secret Manager, and asks
for the Gmail app password (from <https://myaccount.google.com/apppasswords>) without echoing it.
No value is printed or written to the repository. The temporary files are deleted at the end.

```powershell
function New-RandomPassword {
  $bytes = New-Object byte[] 24
  [System.Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes)
  [Convert]::ToBase64String($bytes).Replace('+', '-').Replace('/', '_')
}

function New-SecretFromValue($name, $value) {
  $file = New-TemporaryFile
  try {
    [IO.File]::WriteAllText($file.FullName, $value)
    gcloud secrets create $name --replication-policy=automatic --data-file=$($file.FullName)
  } finally {
    Remove-Item $file.FullName -Force
  }
}

$rootPassword = New-RandomPassword
gcloud sql users set-password root --host=% --instance=veredito-db --password=$rootPassword
Remove-Variable rootPassword

$dbPassword = New-RandomPassword
gcloud sql users create veredito_app --host=% --instance=veredito-db --password=$dbPassword
New-SecretFromValue veredito-db-password $dbPassword
Remove-Variable dbPassword

$smtpInput = Read-Host "Gmail app password" -AsSecureString
$bstr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($smtpInput)
try {
  $smtpPassword = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($bstr) -replace '\s', ''
  New-SecretFromValue veredito-smtp-password $smtpPassword
} finally {
  [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($bstr)
  Remove-Variable smtpPassword, smtpInput -ErrorAction SilentlyContinue
}
```

Nobody needs the `root` password: if access as `root` is ever needed, set a new one with
`gcloud sql users set-password root --host=% --instance=veredito-db --prompt-for-password`. To
inspect the database, the researcher creates a separate personal user in the same way and connects
with the Cloud SQL Auth Proxy.

Check that both secrets exist (this lists versions, not values):

```powershell
gcloud secrets versions list veredito-db-password
gcloud secrets versions list veredito-smtp-password
```

## 6. Service accounts and roles

| Account | Use | Roles |
|---|---|---|
| `veredito-run` | identity of the running service | Cloud SQL client (project); secret accessor on the two secrets |
| `veredito-ci` | `ci.yml` pushes images | Artifact Registry writer on `veredito` |
| `veredito-deploy` | `deploy.yml` deploys revisions | Cloud Run developer (project); service account user on `veredito-run`; Artifact Registry reader on `veredito` |

```powershell
gcloud iam service-accounts create veredito-run --display-name="Veredito runtime"
gcloud iam service-accounts create veredito-ci --display-name="Veredito CI"
gcloud iam service-accounts create veredito-deploy --display-name="Veredito deployment"

gcloud projects add-iam-policy-binding $project `
  --member="serviceAccount:$runAccount" --role=roles/cloudsql.client --condition=None
foreach ($secret in "veredito-db-password", "veredito-smtp-password") {
  gcloud secrets add-iam-policy-binding $secret `
    --member="serviceAccount:$runAccount" --role=roles/secretmanager.secretAccessor
}

gcloud artifacts repositories add-iam-policy-binding veredito --location=$region `
  --member="serviceAccount:$ciAccount" --role=roles/artifactregistry.writer

gcloud projects add-iam-policy-binding $project `
  --member="serviceAccount:$deployAccount" --role=roles/run.developer --condition=None
gcloud iam service-accounts add-iam-policy-binding $runAccount `
  --member="serviceAccount:$deployAccount" --role=roles/iam.serviceAccountUser
gcloud artifacts repositories add-iam-policy-binding veredito --location=$region `
  --member="serviceAccount:$deployAccount" --role=roles/artifactregistry.reader
```

## 7. Cloud Run service (public access)

The service is created once with Google's sample image, only to make it public: `deploy.yml` runs as
`veredito-deploy`, whose role (Cloud Run developer) cannot change the IAM policy of the service. The
first `deploy/*` tag replaces the sample image with Veredito.

```powershell
gcloud run deploy veredito `
  --image=us-docker.pkg.dev/cloudrun/container/hello `
  --region=$region `
  --service-account=$runAccount `
  --allow-unauthenticated `
  --min-instances=0 `
  --max-instances=4
```

## 8. Workload Identity Federation for GitHub Actions

The provider accepts only tokens of the repository `diegoquirino/openscience` for the branch
`Veredito` and the tags `deploy/*`. Within those, the branch (`ref_type` `branch`) can act only as
`veredito-ci` and the tags (`ref_type` `tag`) only as `veredito-deploy`.

```powershell
gcloud iam workload-identity-pools create github `
  --location=global `
  --display-name="GitHub Actions"

gcloud iam workload-identity-pools providers create-oidc github `
  --location=global `
  --workload-identity-pool=github `
  --display-name="openscience Veredito" `
  --issuer-uri="https://token.actions.githubusercontent.com" `
  --attribute-mapping="google.subject=assertion.sub,attribute.repository=assertion.repository,attribute.ref=assertion.ref,attribute.ref_type=assertion.ref_type" `
  --attribute-condition="assertion.repository == 'diegoquirino/openscience' && (assertion.ref == 'refs/heads/Veredito' || assertion.ref.startsWith('refs/tags/deploy/'))"

gcloud iam service-accounts add-iam-policy-binding $ciAccount `
  --role=roles/iam.workloadIdentityUser `
  --member="principalSet://iam.googleapis.com/$pool/attribute.ref_type/branch"

gcloud iam service-accounts add-iam-policy-binding $deployAccount `
  --role=roles/iam.workloadIdentityUser `
  --member="principalSet://iam.googleapis.com/$pool/attribute.ref_type/tag"
```

## 9. GitHub repository variables

The workflows read only these non-secret identifiers. Set them with the GitHub CLI (or in
**Settings > Secrets and variables > Actions > Variables**):

```powershell
gh variable set GCP_WORKLOAD_IDENTITY_PROVIDER --repo $repository --body "$pool/providers/github"
gh variable set GCP_CI_SERVICE_ACCOUNT --repo $repository --body $ciAccount
gh variable set GCP_DEPLOY_SERVICE_ACCOUNT --repo $repository --body $deployAccount
gh variable set GCP_RUN_SERVICE_ACCOUNT --repo $repository --body $runAccount
gh variable set CLOUD_SQL_INSTANCE --repo $repository --body "${project}:${region}:veredito-db"
```

## 10. First run

1. Push to `Veredito` and wait for all jobs of `ci.yml`; the image
   `southamerica-east1-docker.pkg.dev/veredito-issta2027/veredito/app:<sha>` must appear in
   Artifact Registry.
2. Rehearse that commit in local staging (quickstart section 4).
3. Deploy it: `git push origin <commit>:refs/tags/deploy/<version>`, for example
   `deploy/0.3.0-1`. The workflow prints the service URL and the new revision.

Rollback to an earlier revision (listed by `gcloud run revisions list --service=veredito --region=$region`):

```powershell
gcloud run services update-traffic veredito --region=$region --to-revisions=<revision>=100
```
