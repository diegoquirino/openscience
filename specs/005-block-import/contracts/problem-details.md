# Contract: Problem Details and messages added by spec 005

**Feature**: `005-block-import` | Format, locale resolution and bundles as in
[spec 003](../../003-deployment-foundation/contracts/problem-details.md).

## New types

| Slug | Status | When | Extensions |
|---|---|---|---|
| `csv-not-utf8` | 400 | The upload is not valid UTF-8 | none |
| `csv-invalid` | 400 | The file is not a readable CSV (RFC 4180) | none |
| `csv-missing-column` | 400 | A required Appendix D column is missing | `column` (name) |
| `minitest-only` | 422 | Staging, or production in `pilot`, and the file has a non-`minitest` system | `systems` (array of distinct foreign values) |
| `preview-not-found` | 404 | Unknown or expired preview id | none |
| `phase-switch-not-allowed` | 409 | Switch to `collection` refused (not empty, already `collection`, or staging) | none |

`authentication-required`, `forbidden` and `invalid-request` of 003/004 are reused.

## Row reason codes (preview body, not Problem Details)

Keys `import.reason.<code>` in the three bundles; the API also returns the localized string as
`reason`.

| Code | When |
|---|---|
| `already-imported` | (`system`, `edit_key`) already in the database |
| `duplicate-in-file` | Second or later occurrence in the file |
| `empty-system` / `empty-edit-key` / `empty-uc` / `empty-kind` / `empty-field` / `empty-origin-version` / `empty-target-version` / `empty-origin-usecase` / `empty-target-usecase` / `empty-origin-tests` / `empty-target-tests` | Required field empty |
| `invalid-kind` | `kind` not in `ADD`,`DEL`,`MOD`,`META` |
| `empty-origin` | Empty `origin` in `MOD` or `DEL` |
| `empty-target` | Empty `target` in `MOD` or `ADD` |
| `empty-meta-sides` | Both `origin` and `target` empty in `META` |
| `origin-too-long` / `target-too-long` | More than 5,000 characters |
| `usecase-too-long` | `origin_usecase` or `target_usecase` more than 40,000 characters |
| `tests-too-long` | `origin_tests` or `target_tests` more than 40,000 characters |
| `version-too-long` | Version label more than 20 characters |
| `system-too-long` / `edit-key-too-long` / `uc-too-long` / `field-too-long` | Over the column lengths of the 003 model |

## Tests

`LocalizedProblemDetailsTest` gains one case per new type in each of the three locales.
Domain tests cover every row reason code at least once.
