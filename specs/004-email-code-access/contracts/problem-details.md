# Contract: Problem Details and messages added by spec 004

**Feature**: `004-email-code-access` | Format, locale resolution and bundles as in
[spec 003](../../003-deployment-foundation/contracts/problem-details.md).

## New types

Each type has `problem.<slug>.title` and `problem.<slug>.detail` in the three bundles
(`messages.properties`, `messages_pt_BR.properties`, `messages_es_ES.properties`).

| Slug | Status | When | Extensions |
|---|---|---|---|
| `code-resend-too-soon` | 429 | A code for the e-mail was sent less than 60 s ago | `retryAfterSeconds` (integer); header `Retry-After` |
| `code-hourly-limit` | 429 | 5 codes for the e-mail in the last hour | `retryAt` (ISO 8601 UTC); header `Retry-After` in seconds |
| `daily-email-limit` | 429 | The sends of the last 24 hours reached `config.daily_email_limit` | none |
| `code-not-sent` | 503 | The SMTP server refused or did not answer in time | none |
| `invalid-code` | 422 | Wrong code on a code still alive | `attemptsLeft` (1 to 4) |
| `code-expired` | 422 | Expired, used, invalidated, replaced or missing code | none |
| `forbidden` | 403 | Valid session without permission for the resource | none |

`authentication-required` (401) and `invalid-request` (400) of spec 003 are reused: the first for a
missing or expired session on a protected resource; the second for the name, e-mail, code and
language fields, with these `errors[].code` values:

| Field | Codes |
|---|---|
| `name` | `required`, `size` (2 to 120 after trimming) |
| `email` | `required`, `email` (format or more than 254 characters) |
| `code` | `required` |
| `preferredLanguage` | `required`, `locale` |

A code with the wrong shape (letters, 5 digits) is not a field error: it counts as a wrong attempt
(`invalid-code`), as a typo would.

## Detail texts

`detail` carries the numbers the person needs, as `MessageSource` arguments:
- `code-resend-too-soon`: the seconds left;
- `code-hourly-limit`: the minutes until `retryAt` ("in about N minutes"), because the server does
  not know the person's time zone;
- `invalid-code`: `attemptsLeft`.

The Angular
client does not show `title` or `detail`: it maps each `type` to a text of its own build, filled
with the extension values (spec 003, *User-facing behavior*), so the screens and the API say the
same thing in the same language (FR-014, US4 scenario 10).

## E-mail keys

| Key | Arguments |
|---|---|
| `mail.access-code.subject` | none |
| `mail.access-code.body` | `{0}` name, `{1}` code, `{2}` validity in minutes |

Plain text, in the request locale; the pt-BR text is the reference (spec, *Assumptions*). The body
says that the code is valid for `{2}` minutes, that it must not be shared, and that whoever did not
request it can ignore the message.

## Tests

`LocalizedProblemDetailsTest` gains one case per new type, called with `Accept-Language` `pt-BR`,
`en-US`, `es-ES` and without the header, checking `title`, `detail`, `Content-Language` and the
unchanged `type`. `AccessTest` checks the subject of the e-mail received by Mailpit in each locale.
