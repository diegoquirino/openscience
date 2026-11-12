# Contract: Web routing of the root page and the locale prefixes

**Feature**: `003-deployment-foundation` | FR-014, FR-015, US6, edge cases

Supported prefixes: `/pt-BR/`, `/en-US/`, `/es-ES/`. Each one holds the Angular build of that
locale (`baseHref` equal to the prefix).

## Rules, in order

| # | Request (GET or HEAD) | Response |
|---|---|---|
| 1 | `/api`, `/api/**` | API; never touched by the rules below |
| 2 | `/swagger-ui.html`, `/swagger-ui/**`, `/v3/api-docs/**` | API documentation (access rules in [api.md](api.md)) |
| 3 | `/` | `200` static language-choice page (`static/index.html`) |
| 4 | Static file of the root page (`/language.css`, `/language.js`, `/favicon.ico`) | `200` the file |
| 5 | `/pt-BR`, `/en-US`, `/es-ES` (no trailing slash) | `301` to the same prefix with `/` |
| 6 | Path under a supported prefix that names an existing file (`/en-US/main-ABC123.js`) | `200` the file |
| 7 | Path under a supported prefix with a file extension that does not exist (`/en-US/missing.js`) | `404` (plain, no SPA fallback) |
| 8 | Any other path under a supported prefix (`/es-ES/`, `/es-ES/admin`, `/pt-BR/tasks/12`) | `200` that locale's `index.html` (SPA fallback); Angular opens the requested route |
| 9 | Supported prefix in another case (`/en-us/admin`, `/PT-br/`) | `301` to the canonical prefix, keeping the rest of the path and the query (`/en-US/admin`) |
| 10 | Anything else (`/fr-FR/`, `/admin`, `/xyz`) | `302` to `/` |

Methods other than GET and HEAD outside `/api` answer `405`.

## Headers

| Resource | `Cache-Control` |
|---|---|
| `/`, every `index.html` (including the SPA fallback) | `no-cache` |
| Hashed bundles (`*-<hash>.js`, `*-<hash>.css`), fonts, images of a build | `public, max-age=31536000, immutable` |
| Root page assets | `no-cache` |

Each locale's `index.html` has `<html lang="pt-BR">`, `lang="en-US"` or `lang="es-ES"`.

## Root page (`/`) in spec 003

Plain HTML, CSS and JavaScript (no Angular), title "Veredito", three options, each labeled in its
own language and linking to its prefix:

| Label | Link |
|---|---|
| Português (Brasil) | `/pt-BR/` |
| English (US) | `/en-US/` |
| Español (España) | `/es-ES/` |

Preselection by browser language, the cookie `veredito-lang`, the profile language and the header
switch are delivered by spec 004 on top of this page.

## Language switch in the shell

The Angular shell of 003 has a provisional switch only in the footer links (to the same route
under each prefix), enough for US6 scenario 4: the switch loads the other prefix, reloading the
page. Spec 004 replaces it with the header switch and the `sessionStorage` restore (FR-078 of
spec 001).

## Shell content per locale (US1, US6 scenario 1)

| Element | Content |
|---|---|
| Header | "Veredito" |
| Home page | One sentence about the study and the "Participate" button (pt-BR "Participar", en-US "Participate", es-ES "Participar"), leading to a provisional page until spec 004 |
| Footer | Deployed version (`systemVersion`) and the links to the other two languages |
| Banner | "STAGING ENVIRONMENT" (pt-BR "AMBIENTE DE ENSAIO", es-ES "ENTORNO DE PRUEBAS") when `environment` is not `production` |
| Unavailable | "System temporarily unavailable" (pt-BR "Sistema temporariamente indisponível", es-ES "Sistema temporalmente no disponible") when `available` is `false` or the entry point fails |
| New version | "A new version is available; reload the page" when a lazy chunk fails to load |

The final wording of these texts lives in the XLIFF files and must follow `specs/GLOSSARY.md`.
