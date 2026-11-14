# Contract: Language choice, screens and header

**Feature**: `004-email-code-access` | Changes on top of the
[web routing of spec 003](../../003-deployment-foundation/contracts/web-routing.md).

## Root page `/`

| Request | Response |
|---|---|
| `GET /` with cookie `veredito-lang` equal to `pt-BR`, `en-US` or `es-ES` | `302` to `/<locale>/`; `Cache-Control: no-cache`, `Vary: Cookie` (FR-011, US4 scenario 4) |
| `GET /` without the cookie, or with any other value | `200` the language-choice page; `Vary: Cookie` (US4 scenario 1, edge case) |

The page (`static/index.html`, `static/language.css`, new `static/language.js`; texts in the three
languages, no Angular):

| Behavior | Rule |
|---|---|
| Options | "Português (Brasil)", "English (US)", "Español (España)", each in its own language (FR-010) |
| Preselection | First entry of `navigator.languages` whose language is `pt`, `en` or `es` (`es-MX` gives "Español (España)", `en-GB` gives "English (US)"); none gives "Português (Brasil)" (US4 scenario 2) |
| Confirm | Sets `veredito-lang=<locale>; Path=/; Max-Age=31536000; SameSite=Lax` and opens `/<locale>/` (US4 scenario 3) |
| Without JavaScript | The three options stay plain links to the prefixes, as in spec 003 |

## Language cookie in the Angular builds

At startup, each build writes its own locale to `veredito-lang` (same attributes), so a direct
link to a prefix saves the language without the choice page (edge case "invitation link").

## Screens (under each prefix)

| Route | Content | Access |
|---|---|---|
| `/` | Home page of 003; "Participate" leads to `/participate` (or to the person's area when signed in) | everyone |
| `/participate` | Name (2 to 120) and e-mail fields; "Send code"; the field with a problem is marked (US1 scenario 2) | visitor |
| `/participate/code` | E-mail shown; 6-digit code field; text "valid for 10 minutes; check the spam folder" (US1 scenario 1); "Resend" disabled with the seconds left; "Correct e-mail" back to `/participate` with the fields filled | visitor with a code requested in this tab |
| `/welcome` | "Thank you for identifying yourself; the next step will be available soon" (US2 scenario 2) | participant |
| `/admin` | "Administration" title, the administrator's name, menu Import, Export, Dashboard, Participants, all disabled in 004 (US2 scenario 1) | administrator |

Guards read `vd:session`:
- a visitor on `/welcome` or `/admin` goes to `/participate`;
- a participant on `/admin` goes to `/welcome`;
- a signed-in person on `/participate` goes to their own area.

A `401` from any call takes the person to `/participate` (US3 scenario 2).

## Header

Shown on every screen, before and after sign-in, for participants and the administrator
(FR-013, FR-015):

| Element | Rule |
|---|---|
| "Veredito" | Link to the home page |
| Language switch | Button with the flag of the current language (accessible name "Language: <name>"); opens a Material menu with the three options, each with its flag and labeled in its own language; the current one is marked |
| Name and "Sign out" | Only when signed in, each with an icon; "Sign out" calls `vd:sign-out` and opens the home page (US3 scenario 3). Below 600 px the name is hidden and "Sign out" shows only its icon |

The footer of 003 keeps the version and loses the language links, replaced by the header switch.

## Language switch

1. When signed in, `PUT` on `vd:me` with `preferredLanguage` (participant or administrator,
   FR-013).
2. The open form writes its unsaved values to `sessionStorage` under
   `veredito-unsaved:<route without prefix>` (for the sign-in form, `veredito-unsaved:/participate`
   with name and e-mail).
3. The cookie `veredito-lang` is set to the new locale.
4. The same route opens under the new prefix (`/pt-BR/participate` to `/es-ES/participate`), with a
   page reload.
5. After the reload, the form reads the entry, fills the fields as unsaved values and deletes the
   entry. The entry is also deleted when the form is submitted or the person leaves the screen.

## After sign-in

1. The client reads the session representation returned by `vd:sign-in`.
2. If `preferredLanguage` differs from the build locale (FR-012, US4 scenario 7), it sets the
   cookie and opens the person's area under the profile prefix, with a reload.
3. Otherwise it opens the area in place: `/welcome` for a participant, `/admin` for the
   administrator.

## Messages on screen

Every message of the spec is a text of the build, chosen by the Problem Details `type` and filled
with its extensions (FR-014, US4 scenario 10):

| Type | Text (en-US source; pt-BR and es-ES in the XLIFF files) |
|---|---|
| `invalid-request` (field) | "Enter your name (2 to 120 characters)", "Enter a valid e-mail" |
| `code-resend-too-soon` | "Wait {seconds} s to request a new code" (countdown) |
| `code-hourly-limit` | "Too many codes for this e-mail; try again in about {minutes} minutes" |
| `daily-email-limit` | "We cannot send more codes today; please try again tomorrow" |
| `code-not-sent` | "The code could not be sent right now; try again" |
| `invalid-code` | "Wrong code; {attemptsLeft} attempts left" |
| `code-expired` | "This code is no longer valid" with the action "Send a new code" |
| `forbidden`, `authentication-required` | Navigation to the right screen, no message |

The final wording follows `specs/GLOSSARY.md`.
