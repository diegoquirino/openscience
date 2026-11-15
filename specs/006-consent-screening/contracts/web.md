# Contract: Participant onboarding screens

**Feature**: `006-consent-screening` | Builds on the shell, language switch and session of
[spec 004](../../004-email-code-access/contracts/web.md) and on the labeling routes of
[spec 007](../../007-block-queue/contracts/web.md). API in [onboarding.md](onboarding.md).

## Routes (under each locale prefix)

| Route | Content | Access |
|---|---|---|
| `/welcome` | **Resolver**, no content of its own beyond a spinner: follows `vd:onboarding` and navigates (`replaceUrl`) to the route of the first pending step | participant |
| `/consent` | Current consent form in the interface language; "I have read it and agree to participate" and "I do not agree" | participant, step `consent` |
| `/consent/declined` | Thanks after a refusal; "Read the consent form again" (back to `/consent`) and "Sign out" | participant who just refused |
| `/consent/reference/:version` | pt-BR text of that version (the reference), opened in a new tab, same interface language | participant |
| `/skill` | Professional skill list and the "Other" description | participant, step `skill` |
| `/screening` | English example use case and test case, the question and the three answers | participant, step `screening` |
| `/screening/closed` | Closing message for "No": thanks, the study requires this experience; "Sign out" | participant, step `ineligible` |
| `/not-open` | "The study is not open yet"; "Sign out" | participant, step `not-open` |
| `/label`, `/label/**` | Labeling of spec 007, now behind `participationOnly` | participant, step `participation` |

Step to route map (the single table the resolver and the guards share):

| `step` | Route |
|---|---|
| `consent` | `/consent` |
| `skill` | `/skill` |
| `screening` | `/screening` |
| `participation` | `/label` |
| `ineligible` | `/screening/closed` |
| `not-open` | `/not-open` |

The post-sign-in destination of a participant stays `areaOf('participant') = /welcome` (004); the
code screen's language handling (`language.open(preferred, area)`) therefore keeps working: the
resolver runs **under the preferred language's prefix** after the switch.

## Guards

| Guard | Used on | Rule |
|---|---|---|
| `participantOnly` (004) | every route above | Visitor goes to `/participate`; the **administrator goes to `/admin`**, so they never see an onboarding screen |
| `onboardingStep(step)` | `/consent`, `/skill`, `/screening`, `/screening/closed`, `/not-open` | Reads `vd:onboarding`; when the real step differs, redirects to the route of the real step. Makes the browser Back button and typed URLs harmless (US3 edge cases) |
| `participationOnly` | `/label`, `/label/**` | Same, requires `step = participation`; a participant who is not eligible never reaches the labeling screens |

`/consent/declined` and `/consent/reference/:version` only need `participantOnly`. The guards read
the resource on every navigation (no cache), so a new consent version takes effect on the next
navigation. A `403 not-eligible` or `409 step-not-pending` from any call navigates to `/welcome`
(the resolver); `409 study-not-open` goes to `/not-open`; `409 consent-form-outdated` reloads
`/consent`.

## Consent screen (`/consent`)

| Element | Rule |
|---|---|
| Text | `text` of `vd:consent-form`, plain text rendered with `white-space: pre-line`, in a scrollable region (`role="region"`, labelled by the heading); the page never grows beyond the viewport because of it |
| Translation note | Shown **above the text** when `translation` is true (en-US and es-ES): "This text is a translation. The reference version, approved by the research ethics committee, is the Portuguese (Brazil) one." with a link "Read the reference text (pt-BR)" (`target="_blank"`, `rel="noopener"`) to `/consent/reference/<version>`; not shown in pt-BR (US4 scenarios 2 and 3) |
| Buttons | "I have read it and agree to participate" (primary) and "I do not agree", in a **sticky footer**, always visible with a very long text (edge case). They are never disabled by scrolling; both disabled only while a request is in flight (no double submit) |
| Accept | `PUT vd:consent` (`accepted`, version of the text on screen) then navigates to the next step (the resolver) |
| Refuse | `PUT vd:consent` (`refused`) then `/consent/declined` (US1 scenario 3). Back on `/consent` the buttons work again (US1 scenario 4) |
| Language switch | Header control reloads the same URL under the other prefix; the screen fetches the **same version** in the new language and records nothing (US4 scenario 5) |
| Outdated version | `consent-form-outdated`: the screen reloads the current text and shows a short notice that the form changed |
| Missing form | `404` on `vd:consent-form` or step `not-open`: goes to `/not-open` |

## Reference screen (`/consent/reference/:version`)

Follows `vd:consent-text` for `pt-BR`, shows the heading "Reference text (Portuguese, Brazil)" and
the text with `lang="pt-BR"`; the rest of the page (header, buttons of the shell) stays in the
current interface language, and the header switch still works. No decision buttons: it only reads.

## Skill screen (`/skill`)

| Element | Rule |
|---|---|
| Options | Material radio group with the 13 codes of the `vd:skill` template, in the order of Appendix B.4, labeled with the display labels of the interface language ([Appendix B.4](../../001-oracle-labeling/spec.md)); the value sent is always the English code |
| "Other" | Selecting `other` shows a text field (counter "n / 100"); required, non-blank, at most 100 characters (counted as code points), stored as typed; selecting another option hides it and clears it |
| Proceed | Disabled until an option is chosen, and for `other` until the description is valid (US2 scenarios 1 and 2); sends `PUT vd:skill`, then the resolver |
| Language switch | Choice and description survive the reload through `sessionStorage` (`veredito-unsaved:/skill`); the switch calls nothing |
| Server errors | `invalid-request` field codes shown at the field (`skillOther` `size`, `required`); `step-not-pending` goes to the resolver |

## Screening screen (`/screening`)

| Element | Rule |
|---|---|
| Example | Card "Use case" (basic flow as a numbered list, the exception flow with its condition) and card "Test case" (steps, expected result), from `vd:screening-example`; the cards carry `lang="en"` and `translate="no"` and are **English in every locale** (US3 scenario 6); the instructions around them ("Read the example...") are XLIFF text |
| Question | The Appendix B.3 question in the interface language (pt-BR, en-US, es-ES), as a radio group legend |
| Answers | Three radios with the Appendix B.3 labels in the interface language; values `yes_professional`, `yes_academic`, `no` |
| Submit | Disabled until an answer is chosen; sends `PUT vd:screening`, then the resolver: eligible goes to `/label` (US3 scenario 2), `no` to `/screening/closed` (US3 scenario 3) |
| Back button | After answering, the guard redirects; the answer can not be changed (edge case) |
| Language switch | Choice survives the reload (`veredito-unsaved:/screening`); the example is the same English text |

## Closing screens

| Route | Message (keys; the three languages in XLIFF) |
|---|---|
| `/consent/declined` | Thanks for considering the study; no data of the labeling is recorded; "You can read the consent form again if you change your mind." |
| `/screening/closed` | Thanks; the study requires experience with use cases and test cases; participation ends here; the same message on every later sign-in (US3 scenario 4) |
| `/not-open` | "The study is not open yet. Please come back later." (US1 scenario 6) |

Closing screens show only "Sign out" (and, for a refusal, the way back to `/consent`); there is no
link to `/label`.

## Header / menu

No new header controls. The participant shell keeps the language switch and "Sign out"; the
onboarding screens reuse it. The administrator's menu is untouched.

## Internationalization

- Every string above is an `i18n` unit with a stable `@@` id (`consent.*`, `skill.*`,
  `screening.*`, `onboarding.*`); `npm run i18n:check` stays green and the pt-BR and es-ES `.xlf`
  files are complete. The 007 units `welcome.next` and `welcome.start` are removed with the old
  welcome page.
- Skill labels (13) and screening answers (3) come from `core/labels/skill-labels.ts` and
  `screening-labels.ts`: one exhaustive `switch` each (the compiler flags a missing code), same
  pattern as `response-labels.ts`. The labels are exactly those of Appendices B.3 and B.4 of spec 001.
- Unchanged by language: codes sent and stored, the screening example, the "Other" description and
  the consent text of the chosen version (each language of that version is its own stored text).

## Tests (front end)

- **Vitest**: step-to-route map (every step, including `ineligible` before `consent`); resolver
  redirects; guards (wrong step redirects, administrator goes to `/admin`); skill form (empty
  option, `other` empty, 100 and 101 characters); translation note only when `translation`; unsaved
  restore after a locale switch; labels exhaustive for the 13 skills and 3 answers.
- **Playwright** (`onboarding.spec.ts`, local staging with the fixture form): in each of pt-BR, en-US
  and es-ES a new participant accepts, chooses a skill and answers "Yes"; the stored
  `consent_language` matches; the reference link opens pt-BR text without changing the locale; a
  second participant refuses, signs in again and accepts; a third answers "No" and sees the same
  closing message after signing in again; typing `/pt-BR/label` before finishing returns to the
  pending step.
