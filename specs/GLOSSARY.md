# Glossary and naming conventions

Single source for the terms and identifiers used by the constitution, the specs and the code of
Veredito. Everything in the repository (folder and file names, identifiers, code, code comments,
documentation) is in English. The user interface is internationalized (pt-BR, en-US, es-ES).
Data is never translated: it stays in the language in which it was captured.

## Language rules

| What | Language |
|---|---|
| Folder and file names, identifiers, database schema, REST paths, link relations, configuration keys and values, code, code comments, commit messages, documentation | English |
| User interface (participant and administrator screens), e-mails, validation messages, Problem Details `title`/`detail`, consent form, question texts and explanations | pt-BR, en-US and es-ES, chosen by the user |
| Data (dataset and database): imported use cases and edits, participant comments, names, the consent form text each person accepted | The language of capture, unchanged; each response and acceptance records the UI language in use |
| Codes stored in the database and in exports (`change_impact`, `maintenance_operation`, `professional_skill`, screening answer, study phase) | English identifiers; only their display labels are translated |

## Concepts

| Term | Meaning | Former Portuguese term |
|---|---|---|
| data labeling | The activity participants do in Veredito (verb: label; result: labels) | rotulagem dos dados |
| participant | Person who labels; formal term (consent form, LGPD, formal documents) | participante |
| volunteer | Same person, in invitations and screens | voluntário |
| administrator | The single researcher who imports items and exports data | administrador |
| rater 1, rater 2 | Position of a person in a block (`role` 1 or 2 in the data model; `rater1`/`rater2` in the pipeline) | rotulador 1 e 2, papel |
| item | One use-case edit to be labeled, identified by (`system`, `edit_key`) | item |
| system | Software system under study, e.g. `minitest` | sistema |
| import batch | One imported CSV file; sets the queue order of its blocks | lote de importação |
| preview | Dry run of an import that lists included, ignored and refused rows | prévia |
| block | 10 consecutive items of a batch (the last one may have fewer) | bloco |
| task (block-role) | Role 1 or 2 of a block: free, reserved or completed | tarefa, bloco-papel |
| queue | Ordered list of tasks handed out to participants | fila |
| reservation | Exclusive hold of a task by one participant until it expires | reserva |
| expiry | Moment a reservation ends (`expires_at`) | vencimento |
| draft | Editable response before the block is submitted | rascunho |
| response | A participant's answer to one item in one role; draft, then final | resposta |
| submission | Sending the whole block; makes all its responses final | envio |
| consent form | Informed consent form (TCLE) and its versions | termo de consentimento, termo |
| current consent form | The version participants must accept now | termo vigente |
| professional skill | Self-declared professional background | habilidade profissional |
| screening | Question that decides eligibility | triagem |
| eligible | Accepted the current consent form and passed screening | elegível |
| access code | 6-digit code sent by e-mail to sign in | código de acesso |
| pseudonym | Identifier shown instead of name and e-mail | pseudônimo |
| progress dashboard | Administrator view of the data collection progress | painel de andamento |
| withdrawal | LGPD request to remove a participant's data | retirada |
| wide export, long export | One row per item with both raters; one row per response | exportação larga, longa |
| staging | Local Docker test environment (there is no cloud staging) | ensaio |
| pilot | Trial run in production with colleagues, Minitest items only | ensaio piloto |
| study phase | `pilot` (default) or `collection`; switched once, in production, with an empty database | fase do estudo (`piloto`, `coleta`) |
| data collection | The real study after the pilot | coleta |
| deployment | Publishing to Google Cloud Run | publicação |
| research ethics committee | Brazilian CEP that approves the consent form | comitê de ética (CEP) |
| use case, edit, change impact, maintenance operation, oracle | As in the ISSTA_2027 pipeline | caso de uso, edição, impacto da mudança, operação de manutenção, oráculo |

## Identifiers

| Kind | Identifier | Former identifier |
|---|---|---|
| Spec folders | `001-oracle-labeling`, `002-oracle-import-csv`, `003-deployment-foundation`, `004-email-code-access`, `005-block-import`, `006-consent-screening`, `007-block-queue`, `008-item-screen`, `009-exports`, `010-progress-dashboard`, `011-lgpd-withdrawal` | `001-rotulagem-oraculo` … `011-retirada-lgpd` |
| Spring and Docker Compose profile | `staging` | `ensaio` |
| Study phase values | `pilot`, `collection` (config key `study-phase`) | `piloto`, `coleta` |
| CSV and export column | `system` | `sistema` |
| Tables and columns | `import_batch`, `batch_id`, `block`, `block_id`, `task`, `task_id`, `role`, `participant`, `participant_id`, `response`, `consent_form`, `access_code`, `pseudonym`, `eligible`, `reserved_at`, `expires_at`, `preferred_language`, `language` | `lote`, `lote_id`, `bloco`, `bloco_id`, `tarefa`, `tarefa_id`, `papel`, `participante`, `participante_id`, `resposta`, `termo`, `codigo_acesso`, `pseudonimo`, `elegivel`, `reservada_em`, `vence_em` |
| JSON fields | `reservationExpiresAt`, `reservationMinutes` | `reservaVenceEm`, `reservaMinutos` |
| REST resources (under `/api/v1`) | `/batches`, `/batches/previews`, `/participants/me`, `/tasks`, `/tasks/reservations`, `/tasks/{id}`, `/tasks/{id}/items/{position}`, `/tasks/{id}/responses/{itemId}`, `/tasks/{id}/submission`, `/consent-forms/current` | `/lotes`, `/lotes/previas`, `/participantes/eu`, `/tarefas`, `/tarefas/reservas`, `/tarefas/{id}`, `/tarefas/{id}/itens/{posicao}`, `/tarefas/{id}/respostas/{itemId}`, `/tarefas/{id}/envio`, `/termos/vigente` |
| Link relations | `vd:batches`, `vd:dashboard`, `vd:participants`, `vd:request-code`, `vd:next-task`, `vd:reserve`, `vd:save-response`, `vd:consent-form` (others follow the same kebab-case English pattern) | `vd:lotes`, `vd:painel`, `vd:participantes`, `vd:pedir-codigo`, `vd:proxima-tarefa`, `vd:reservar`, `vd:salvar-resposta`, `vd:termo` |
| Minitest fixtures | `veredito_minitest.csv`, `minitest_duplicates.csv`, `minitest_missing_kind_column.csv`, `minitest_long_text.csv` | `minitest_duplicatas.csv`, `minitest_sem_coluna_kind.csv`, `minitest_texto_longo.csv` |
| `professional_skill` codes | `developer`, `software_engineer`, `systems_analyst`, `requirements_analyst`, `test_analyst`, `test_engineer`, `software_architect`, `tech_lead`, `product_project_manager`, `professor_researcher`, `graduate_student`, `undergraduate_student`, `other` | `desenvolvedor`, `engenheiro_software`, `analista_sistemas`, `analista_requisitos`, `analista_testes`, `engenheiro_testes`, `arquiteto_software`, `lider_tecnico`, `gestor_produto_projeto`, `docente_pesquisador`, `estudante_pos`, `estudante_graduacao`, `outro` |
| Screening answer codes | `yes_professional`, `yes_academic`, `no` | — |
| Task labels in examples | `B1-R1`, `B1-R2` (block 1, role 1 or 2) | `B1-P1`, `B1-P2` |
| Consent form texts | table `consent_form_text` (`consent_form_id`, `language`, `text`); `consent_form.reference_language = pt-BR` | — |
| Language columns in exports | wide: `rater_N_response_language`; long: `response_language`, `participant_preferred_language`, `consent_language`; `rater_N_accepted_research_terms_lgpd` = version, language and timestamp (`v1 pt-BR 2026-11-20T14:03:00-03:00`) | — |
| Anonymized participant marker in exports | `anonymized` | `anonimizado` |
| Language cookie | `veredito-lang` | — |
| Administrator profile language | `config.admin_preferred_language`; resource `/api/v1/administrator/me` (`vd:me`) | — |
| Unsaved input kept across a language switch | `sessionStorage` key `veredito-unsaved:<route>` | — |
| Category codes (fixed, English, analyzed after the collection) | `change_impact`: `LOW`, `HIGH`; `maintenance_operation`: `KEEP`, `UPDATE`, `REASSIGN`, `CREATE`, `SPLIT`, `MERGE`, `REMOVE`, `FLAG`; only display labels are translated | `BAIXO`, `ALTO`, `MANTER`, ... |
| Locales | `pt-BR`, `en-US`, `es-ES` (URL prefixes `/pt-BR/`, `/en-US/`, `/es-ES/`); missing or unsupported language falls back to `pt-BR` | — |
| Missing use case in the import CSV | the S5 text `(the use case does not exist in this version)`, kept as captured; the item screen shows a localized message instead | `(este caso de uso não existe nesta versão)` |

Names from the ISSTA_2027 pipeline (`change_impact`, `maintenance_operation`, `t1_<system>_rater1.csv`,
the run folder `runs/minitest-piloto`) do not change.

## User interface vocabulary

| Where | pt-BR | en-US | es-ES |
|---|---|---|---|
| Activity, in titles, consent form, invitations and explanations | rotulagem dos dados | data labeling | etiquetado de datos |
| Buttons and short messages | avaliar ("Avaliar mais um bloco", "Ainda não há itens para avaliar") | label ("Label another block", "No items to label yet") | evaluar ("Evaluar otro bloque", "Aún no hay ítems para evaluar") |
| Person, in formal texts | participante | participant | participante |
| Person, in invitations and screens | voluntário | volunteer | voluntario |
| Position in the block | rotulador 1, rotulador 2 | rater 1, rater 2 | etiquetador 1, etiquetador 2 |
| Language choice | Português (Brasil) | English (US) | Español (España) |
