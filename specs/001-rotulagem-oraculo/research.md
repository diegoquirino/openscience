# Seleção de tecnologias: Veredito

**Feature**: `001-rotulagem-oraculo` · **Data**: 2026-11-11 · **Spec**: [spec.md](./spec.md) · **Constitution**: 2.4.0

Esta revisão substitui a decisão de 2026-11-10 (Apps Script com Google Sheets), descartada por insegurança quanto ao ambiente gratuito.

## Decisão

**Angular com Angular Material no frontend, Spring Boot no backend e MySQL Community com Flyway, num único contêiner publicado no Cloud Run, com o banco no Cloud SQL. O código fica no branch órfão `Veredito` do repositório público `diegoquirino/openscience`.**

**Ambientes**: a **produção** é o Google Cloud; o **ensaio** (testes) é **local, em Docker**, na máquina do pesquisador. Não há ensaio na nuvem.

## Versões (conferidas em 2026-11-11)

| Peça | Versão | Observação |
|---|---|---|
| Angular, Angular CDK e Angular Material | 22.2.x | Material Design 3 |
| Node.js (só para compilar o frontend) | 26 LTS | |
| Java | 25 LTS (Eclipse Temurin) | |
| Spring Boot | 4.1.2 | A 4.2.0 está em RC2, com versão final prevista para novembro de 2026; usar a 4.2.x se já tiver saído quando o plano da spec 003 for feito |
| Maven | 3.9.x ou superior | Via Maven Wrapper |
| MySQL Community | 9.7 LTS | Também é a versão do contêiner de desenvolvimento e de teste |
| Spring HATEOAS | gerenciada pelo Spring Boot (`spring-boot-starter-hateoas`) | Hipermídia HAL-FORMS da API |
| springdoc-openapi | 3.2.0 (`springdoc-openapi-starter-webmvc-ui`, Swagger UI 5.34) | OpenAPI 3.1 e Swagger UI. A 3.2.x é feita para o Spring Boot 4.1.x; com o Spring Boot 4.2, usar a 3.x correspondente. Com o parent do Spring Boot no Maven, o Jackson fica na versão gerenciada pelo Boot |
| Flyway (código aberto) | 13.12.1 | Com o módulo `flyway-mysql`; sobrepor a versão gerenciada pelo Spring Boot se ela for mais antiga |
| Testcontainers | gerenciada pelo Spring Boot | MySQL 9.7 em contêiner nos testes de integração |
| REST Assured | 6.3.0 (`rest-assured` e `json-schema-validator`, escopo de teste) | Testes da API. Suporta Spring Boot 4, Jackson 3 e JSON Schema 2020-12; sobrepor a versão gerenciada pelo Spring Boot (`rest-assured.version`) se ela for mais antiga |

## Arquitetura

```text
Navegador (Angular + Angular Material)
        │  HTTPS, cookie de sessão HttpOnly
        ▼
Cloud Run: contêiner único
  Spring Boot 4 (Java 25)
   ├─ /       → arquivos do Angular compilado
   ├─ /api/** → API REST nível 3 (HAL-FORMS, Spring HATEOAS)
   ├─ domínio em Java puro (CSV, blocos, fila, exportações)
   └─ Flyway roda as migrações na subida
        │ conector do Cloud SQL (IAM)           │ SMTP (TLS)
        ▼                                       ▼
Cloud SQL para MySQL 9.7                    smtp.gmail.com
  banco: veredito                           (diegoquirino@gmail.com)
Secret Manager: senha do banco, senha de app do Gmail, chave de sessão
```

### Organização do branch `Veredito`

```text
/                       raiz do branch órfão (conteúdo da pasta Veredito)
├─ backend/             Spring Boot (Maven Wrapper)
│  └─ src/main/resources/db/migration/   migrações Flyway V1__..., V2__...
├─ frontend/            Angular + Angular Material
├─ compose.yaml         MySQL 9.7 (desenvolvimento) e, no perfil `ensaio`, app + MySQL + Mailpit
├─ Dockerfile           build em estágios: Node 26 → Maven/JDK 25 → JRE 25 (o mesmo do ensaio e da produção)
├─ .env.example         variáveis do ensaio local, sem segredos (o `.env` real é ignorado pelo git)
├─ .github/workflows/   testes, imagem e publicação
├─ specs/, .specify/, .cursor/
└─ .gitignore
```

- No desenvolvimento, `ng serve` encaminha `/api` para o Spring Boot em `localhost:8080`, e o suporte do Spring Boot ao Docker Compose sobe o MySQL sozinho.
- Na imagem, o Angular compilado vai para os recursos estáticos do Spring Boot, que serve tudo pela mesma URL (sem CORS).

## Ensaio local (Docker)

O ambiente de testes roda na máquina do pesquisador com Docker Compose (`docker compose --profile ensaio up --build`):

| Peça | Configuração |
|---|---|
| `app` | Imagem construída pelo mesmo `Dockerfile` da produção, perfil Spring `ensaio`, em `http://localhost:8080`; faixa "AMBIENTE DE ENSAIO" na tela |
| `mysql` | MySQL Community 9.7 (mesma versão do Cloud SQL), volume próprio; o Flyway roda as mesmas migrações na subida |
| `mailpit` | Servidor de e-mail de teste ([Mailpit](https://mailpit.axllent.org/), código aberto): recebe os códigos de acesso sem enviá-los de verdade; caixa em `http://localhost:8025` |
| Dados | Só os itens do Minitest (ver "Dados de ensaio e do piloto"), com participantes fictícios; nada vindo da produção. A importação recusa outro sistema neste perfil |
| Segredos | Nenhum segredo real é necessário; para testar o envio pelo Gmail, a senha de app vai num `.env` local, ignorado pelo git |
| Recomeçar do zero | `docker compose --profile ensaio down -v` apaga o volume do banco |

- O teste de 30 participantes simultâneos e os demais testes de integração rodam com Testcontainers, sem depender do Compose.

### Dados de ensaio e do piloto: só o Minitest

O ensaio local, os testes automatizados e o ensaio piloto em produção usam só itens do sistema `minitest` (38 edições, versões 1.0 a 2.2, tags `minitest_v*` deste mesmo repositório público).

| Uso | Dados |
|---|---|
| Arquivo de referência | `veredito_minitest.csv` gerado pela spec 002, copiado para `backend/src/test/resources/minitest/` e versionado. Quando o pipeline gerar o arquivo de novo, a cópia é trocada no mesmo commit |
| Casos de erro | Arquivos derivados do de referência, na mesma pasta, com o nome dizendo o defeito (por exemplo, `minitest_duplicatas.csv`, `minitest_sem_coluna_kind.csv`, `minitest_texto_longo.csv`) |
| Casos de volume | Gerados em memória pelos testes, repetindo as linhas do Minitest com um sufixo no `edit_key` (`LoginTest\|1\|1#r07`), até 205 ou 500 linhas; não são gravados como arquivo |
| Participantes | Fictícios, com e-mails do domínio reservado `example.com`; no ensaio local os códigos chegam ao Mailpit |
| Ensaio local | O pesquisador importa o arquivo de referência pela tela do administrador; o perfil `ensaio` recusa qualquer arquivo com `sistema` diferente de `minitest` |
| Piloto em produção | A configuração "fase do estudo" começa em `piloto` numa base nova; nessa fase, a importação também recusa outro sistema. Depois do piloto, a base é recriada vazia (de novo em `piloto`), o administrador passa a fase para `coleta`, o que só é aceito sem itens, participantes e respostas na base, e então importa a base real |
- O ensaio usa a mesma imagem e as mesmas migrações da produção; o que muda são só as variáveis (banco, SMTP, faixa de ensaio).

## Produção no Google Cloud

| Peça | Configuração |
|---|---|
| Região | `southamerica-east1` (São Paulo), perto dos avaliadores; `us-central1` é mais barata se o custo apertar |
| Cloud Run | Um único serviço, `veredito`; mínimo de 0 instâncias, máximo de 4; *startup CPU boost* ligado |
| Cloud SQL | Uma instância MySQL 9.7, edição Enterprise, a menor máquina compartilhada (`db-f1-micro`, 0,6 GB, 25 conexões); um único banco, `veredito`, com usuário próprio; backup diário automático com retenção de 7 dias |
| Acesso ao banco | Sem redes autorizadas; só o Cloud Run entra, pelo conector do Cloud SQL com IAM |
| Pool de conexões | No máximo 5 por instância do Cloud Run, para caber nas 25 conexões |
| Segredos | Secret Manager, injetados como variáveis de ambiente do Cloud Run |
| Imagens | Artifact Registry, mantendo só as 5 imagens mais recentes |
| Projeto | `veredito-issta2027`, vinculado à conta de faturamento existente (em reais) |
| Orçamento | "Veredito mensal": R$ 200 por mês só neste projeto, só alertas, em 50%, 90% e 100% |
| URL | A padrão do Cloud Run (`https://veredito-….run.app`); domínio próprio é opcional |

**Custo estimado**: cerca de US$ 10 a 13 por mês, quase tudo do Cloud SQL (`db-f1-micro` custa cerca de US$ 7,70 por mês em `us-central1`, mais disco e backup; São Paulo é um pouco mais cara). Cloud Run, Artifact Registry, Secret Manager e GitHub Actions (repositório público) ficam no nível gratuito. Contas novas recebem US$ 300 de crédito por 90 dias.

## Publicação

GitHub Actions no branch `Veredito`:

1. **A cada push**: testes do backend (`./mvnw verify`, com Testcontainers, incluindo o teste da documentação da API) e do frontend; `openapi.json` guardado como artefato do workflow; build da imagem pelo `Dockerfile`; envio ao Artifact Registry com a etiqueta do commit. Nada é publicado.
2. **Ensaio**: o pesquisador roda o mesmo commit no Docker local (seção *Ensaio local*) e confere os critérios de sucesso da spec.
3. **Produção**: workflow disparado manualmente, informando o commit ensaiado; publica no `veredito` a imagem desse commit já enviada ao Artifact Registry. A URL não muda.
4. **Autenticação**: Workload Identity Federation entre GitHub e Google Cloud, sem chave de conta de serviço guardada no GitHub.
5. **Volta atrás**: o Cloud Run mantém as revisões anteriores; basta mandar o tráfego para a anterior. Por isso, toda migração Flyway precisa ser compatível com a versão anterior do código (acrescentar antes, remover depois).
6. **Fim do piloto**: o piloto com colegas roda na produção, só com os itens do Minitest (fase `piloto`). Antes da divulgação, o banco `veredito` é apagado e criado de novo no Cloud SQL, e a publicação seguinte o prepara vazio pelo Flyway. O administrador passa a fase para `coleta` e importa a base real. O `flyway clean` fica desligado em produção.

## Código por e-mail

- Spring Mail pelo SMTP do Gmail (`smtp.gmail.com:587`, STARTTLS), com senha de app de `diegoquirino@gmail.com`; a conta precisa de verificação em duas etapas.
- Limite de cerca de 500 destinatários por dia numa conta Gmail comum. O sistema conta os envios do dia e para em um limite configurável (padrão 450), mostrando a mensagem da spec 004.
- O e-mail pode cair no spam; a tela avisa.

## Modelo de dados (MySQL)

A migração inicial (spec 003) cria todas as tabelas; as specs seguintes só acrescentam o que for novo, em migrações próprias.

| Tabela | Uma linha por | Colunas e restrições principais |
|---|---|---|
| `lote` | importação confirmada | `arquivo`, `importado_em`, contagens |
| `item` | edição importada | `sistema`, `edit_key`, `uc`, `kind`, `field`, `origin`, `target` (`TEXT`), versões, `origin_usecase`, `target_usecase` (`MEDIUMTEXT`), `lote_id`, `bloco_id`, `posicao`; **único** (`sistema`, `edit_key`) |
| `bloco` | bloco de até 10 itens | `lote_id`, `tamanho` |
| `tarefa` | bloco-papel | `fila` (**única**), `bloco_id`, `papel`, `status`, `participante_id`, `reservada_em`, `vence_em`, `enviada_em`; **único** (`bloco_id`, `papel`) e **único** (`bloco_id`, `participante_id`) |
| `resposta` | item num papel | `tarefa_id`, `item_id`, `participante_id`, `estado` (rascunho, final), `change_impact`, `maintenance_operation`, `comment`, `salva_em`; **único** (`tarefa_id`, `item_id`) |
| `participante` | pessoa | `pseudonimo` (**único**), `nome`, `email` (**único**), decisão e versão do termo, habilidade, triagem, `elegivel`, `anonimizado_em` |
| `codigo_acesso` | código enviado | `email`, `hash`, `criado_em`, `expira_em`, `tentativas`, `usado_em` |
| `sessao` | sessão de acesso | `token_hash`, `participante_id` ou administrador, `expira_em` |
| `termo` | versão do termo | `versao`, `texto`, `publicado_em` |
| `config` | parâmetro | e-mail do administrador, minutos de reserva, tamanho do bloco, limite diário de e-mails |

A restrição única (`bloco_id`, `participante_id`) é a garantia de banco de que ninguém ocupa os dois papéis de um bloco (o MySQL aceita vários `NULL` nesse índice, então tarefas livres não conflitam). Um caso de uso de até 40.000 caracteres cabe com folga em `MEDIUMTEXT`.

## API REST: nível 3 de Richardson ("glory of REST")

A API atende aos três níveis do modelo de maturidade de Richardson.

| Nível | Como o Veredito atende |
|---|---|
| 1. Recursos | Cada coisa do domínio tem URI própria: `/api/participantes/eu`, `/api/tarefas/{id}`, `/api/tarefas/{id}/itens/{posicao}`, `/api/tarefas/{id}/respostas/{itemId}`, `/api/lotes`, `/api/lotes/{id}`, `/api/termos/vigente`, `/api/config`, `/api/exportacoes/{tipo}` |
| 2. Métodos e status HTTP | `GET` lê sem efeito; `POST` cria (`201 Created` com `Location`); `PUT` grava de forma idempotente; `DELETE` remove ou retira. Códigos: `200`, `201`, `204`, `400`/`422` (validação), `401` (sem sessão), `403` (sem permissão), `404`, `409` (conflito de estado, por exemplo bloco já reservado por outra pessoa ou já enviado), `429` (limite de códigos ou de e-mails) |
| 3. Hipermídia | Spring HATEOAS com **HAL-FORMS** (`application/prs.hal-forms+json`). Cada resposta traz `_links` (navegação) e `_templates` (ações com método, campos e opções) que valem **no estado atual** do recurso |

**Regras**

- **Ponto de entrada único**: `GET /api` devolve só as relações permitidas a quem chama.
  - Sem sessão: `vd:pedir-codigo`.
  - Participante: `vd:eu`, `vd:termo` e, quando elegível, `vd:proxima-tarefa`.
  - Administrador: `vd:lotes`, `vd:exportacoes`, `vd:painel`, `vd:participantes`.
- **Relações próprias** com o prefixo CURIE `vd:`, documentadas em `/api/rels/{rel}`. Relações padrão da IANA (`self`, `next`, `prev`, `collection`, `item`) quando cabem.
- **O estado decide os links.** Uma tarefa reservada pela própria pessoa oferece `vd:itens`, `vd:salvar-resposta` e `vd:revisar`, e só oferece `vd:enviar` quando todos os itens têm rascunho. Uma tarefa concluída só tem `self`. Uma ação que não aparece como link é recusada se chamada (`409` ou `403`).
- **O cliente não monta URLs.** O Angular parte de `/api`, segue as relações e preenche os `_templates`. Um serviço próprio, pequeno e tipado, lê HAL-FORMS, porque não há biblioteca oficial do Angular para isso. Só `/api` fica fixo no código do frontend.
- **Erros** em Problem Details (RFC 9457, `application/problem+json`), com o tipo do erro e, nas validações, a lista de campos. A mensagem amigável da tela sai do tipo do erro.
- **Coleções** paginadas (`PagedModel`, com `next` e `prev`) quando puderem crescer, como lotes, participantes e itens do painel.
- **Exportações**: `GET /api/exportacoes/{tipo}` com `Accept: text/csv` devolve o arquivo; em HAL, devolve a lista de arquivos disponíveis.
- **Sem versão na URL.** A API evolui acrescentando campos e relações; o cliente ignora o que não conhece.

### Documentação no Swagger (OpenAPI 3.1)

A especificação é gerada do próprio código pelo springdoc-openapi, então não há documento escrito à mão que possa ficar desatualizado.

| Item | Regra |
|---|---|
| Endereços | Swagger UI em `/swagger-ui.html`; especificação em `/v3/api-docs` (JSON) e `/v3/api-docs.yaml` |
| Acesso | Aberto no ensaio local. Na produção, só com sessão de administrador, pelo mesmo filtro da área administrativa; participantes não veem |
| Informações gerais | Título "Veredito API", versão do sistema (a mesma do rodapé), descrição do estudo, contato do pesquisador, licença |
| Grupos (tags) | Entrada (`/api`), Acesso (código e sessão), Participação (termo, habilidade, triagem), Tarefas (blocos e respostas), Administração (lotes, exportações, painel, participantes) |
| Cada operação | `summary` e `description` em português; parâmetros com descrição e exemplo; corpo com esquema e exemplo; **todas** as respostas possíveis (2xx, 4xx) com exemplo; as respostas de erro usam o esquema Problem Details |
| Hipermídia | Os esquemas HAL (`_links`, `_templates`) aparecem nas respostas; a descrição de cada operação lista as relações e ações que o recurso oferece **em cada estado** (por exemplo, `vd:enviar` só com todos os itens respondidos); `/api/rels/{rel}` aponta para a operação correspondente no Swagger |
| Segurança | Esquema de segurança declarado (cookie de sessão `HttpOnly`); operações administrativas marcadas como tal |
| Esquemas | Cada DTO com `@Schema` (descrição, exemplo, obrigatoriedade, tamanhos máximos iguais aos da validação: 5.000 e 40.000 caracteres, por exemplo) |
| Qualidade | Um teste de integração lê `/v3/api-docs` e falha se alguma operação estiver sem `summary`, `description`, exemplo ou resposta de erro; o CI guarda o `openapi.json` de cada commit como artefato |

**Operações críticas em HTTP** (a semântica transacional está na seção seguinte)

| Operação | Requisição | Resposta |
|---|---|---|
| Pedir bloco | `POST /api/tarefas/reservas` (seguido de `vd:proxima-tarefa`) | `201` com `Location` da tarefa; `200` com a mesma tarefa se a pessoa já tinha uma reserva válida; `204` se não há bloco disponível para ela |
| Salvar rascunho | `PUT /api/tarefas/{id}/respostas/{itemId}` | `200`; repetir a mesma resposta não cria outra (idempotente) |
| Enviar bloco | `POST /api/tarefas/{id}/envio` (`vd:enviar`) | `200` com a tarefa concluída; reenviar um bloco já concluído pela pessoa devolve `200` sem alterar nada; reserva vencida ou de outra pessoa devolve `409` |
| Importar | `POST /api/lotes/previas` (envio do CSV) e `POST /api/lotes` com o id da prévia (`vd:confirmar`) | `201` para a prévia e `201` para o lote |

## Tratamento de concorrência

Cada operação é uma transação curta; as regras de decisão ficam na camada de domínio, e o repositório só carrega e bloqueia as linhas.

1. **Pedir bloco**: se a pessoa tem tarefa reservada e não vencida, devolver essa. Senão, ler pela ordem da `fila` as candidatas livres ou vencidas, fora dos blocos em que ela já tem papel, com `SELECT … FOR UPDATE SKIP LOCKED`, para que pedidos simultâneos peguem tarefas diferentes sem esperar. Reservar a primeira, apagar rascunhos de um dono anterior vencido e gravar `vence_em = agora + 30 min`.
2. **Salvar rascunho**: bloquear a tarefa (`FOR UPDATE`), conferir o dono (ou reserva vencida sem novo dono), gravar com `INSERT … ON DUPLICATE KEY UPDATE` na chave (`tarefa_id`, `item_id`) e renovar `vence_em`.
3. **Enviar bloco**: bloquear a tarefa, conferir o dono e que há rascunho para todos os itens, trocar as respostas para `final` e a tarefa para `concluida`, tudo na mesma transação. Reenviar um bloco já concluído pela pessoa devolve sucesso sem alterar nada.
4. **Importar**: uma transação por confirmação; a próxima posição da fila é lida com bloqueio, para que duas importações não colidam.
5. **Vencimento**: calculado na leitura; não há tarefa agendada.

O teste de 30 participantes simultâneos (spec 007, SC-001) roda como teste de integração contra o MySQL em contêiner, com 30 threads.

## Testes

| Camada | Ferramenta |
|---|---|
| Domínio (Java puro) | JUnit, sem Spring |
| Repositórios, transações, migrações | Spring Boot Test + Testcontainers (MySQL 9.7) |
| API | REST Assured contra a aplicação numa porta aleatória (`@SpringBootTest(webEnvironment = RANDOM_PORT)`), com MySQL e servidor de e-mail de teste em contêiner |
| Documentação da API | Teste com REST Assured que lê `/v3/api-docs` e valida a completude (resumo, descrição, exemplos, respostas de erro em cada operação) |
| Frontend | Executor de testes padrão do Angular 22 |

### Testes da API com REST Assured

| Item | Regra |
|---|---|
| Por que HTTP real | Os testes passam pelos mesmos filtros, cookies de sessão, negociação de conteúdo e tratamento de erros da produção; nada é simulado na camada web |
| Navegação | Cada teste começa em `GET /api` e segue os links pelo nome da relação (por exemplo, `vd:reservar`), extraindo o `href` da resposta; a única URL fixa é a do ponto de entrada. Isso testa o nível 3: se um link sumir ou mudar, o teste quebra |
| Especificações comuns | Uma classe base monta o `RequestSpecification` (porta, `Accept: application/prs.hal-forms+json`, log só em falha) e um por perfil: visitante, participante com sessão, administrador |
| Organização | Uma classe de teste por área, na mesma divisão dos grupos do Swagger (Entrada, Acesso, Participação, Tarefas, Administração) |
| O que cada teste confere | Status HTTP, `Content-Type`, cabeçalhos (`Location` no 201), corpo, e os `_links` e `_templates` presentes **e ausentes** no estado do recurso |
| Ações proibidas | Para cada estado, o teste confere que a ação não aparece e que a chamada direta ao endereço é recusada (403, 404 ou 409, conforme o caso) |
| Erros | Respostas de erro validadas contra o esquema JSON de Problem Details (`matchesJsonSchemaInClasspath`) |
| Sessão e e-mail | O código de acesso é lido do servidor de e-mail de teste, como faria o participante; o cookie de sessão é reaproveitado pelo `RequestSpecification` do perfil |
| Dados | Cada teste cria os próprios dados importando, pela API do administrador, o CSV do Minitest ou um derivado dele, com participantes fictícios; a base é limpa entre classes |
| Concorrência | O teste de 30 participantes simultâneos (spec 007) usa REST Assured em 30 threads, cada uma com sua sessão |

## Limites que importam

| Limite | Valor | Mitigação |
|---|---|---|
| Conexões do `db-f1-micro` | 25 | Pool pequeno e no máximo 4 instâncias do Cloud Run; subir para `db-g1-small` (cerca de US$ 27 por mês) se faltar |
| `db-f1-micro` sem SLA | — | Aceitável para um estudo; backup diário |
| E-mails do Gmail | cerca de 500 por dia | Sessão de 24 h, limites de reenvio, contador no painel; reserva: provedor transacional |
| Partida a frio do Cloud Run | alguns segundos na primeira chamada após inatividade | *Startup CPU boost* e cache AOT do Java; mínimo de 1 instância durante a divulgação, se necessário (custo a medir na produção, durante o piloto) |

## Alternativas descartadas

| Opção | Por que não |
|---|---|
| Apps Script + Google Sheets (decisão de 2026-11-10) | Cotas baixas (100 e-mails por dia, 30 execuções simultâneas), planilha como banco, sem migrações versionadas; o pesquisador não se sentiu seguro |
| VM `e2-micro` gratuita com Docker Compose | Backup, atualizações e segurança ficam por conta do pesquisador; 1 GB de RAM para JVM e MySQL juntos |
| Firebase / Firestore | Não relacional; a decisão é usar MySQL |
| Provedor de e-mail externo (por exemplo, Brevo) | Fica fora do Google; mantido como reserva se o Gmail tiver problemas de entrega |

## Riscos e pontos em aberto

- **Flyway e MySQL 9.7**: a lista de versões certificadas do Flyway vai até a 9.4. Versões mais novas costumam só gerar aviso. Os testes com Testcontainers 9.7 confirmam; se falhar, voltar para MySQL 8.4 LTS.
- **Máquina compartilhada com MySQL 9.7 no Cloud SQL**: confirmar no plano da spec 003. Se não houver, usar 8.4 no `db-f1-micro` ou 9.7 no `db-g1-small`.
- **Repositório público**: *secret scanning* e *push protection* do GitHub ativados em 2026-11-11.
- **E-mail**: a senha de app do Gmail ("Veredito SMTP") já existe e está guardada fora do repositório; entra no Secret Manager no plano da spec 003. O ensaio local não precisa dela (usa o Mailpit).
- **Piloto na produção**: só com os itens do Minitest; os colegas do piloto devem saber que as respostas serão descartadas; ao fim, o banco de produção é recriado vazio e a fase passa a `coleta` antes da divulgação.
- **CSV com os casos de uso completos**: especificado em [002-csv-oraculo](../002-csv-oraculo/spec.md).
- **Concordância entre avaliadores**: ver a seção *Assumptions* da spec. Afeta a análise, não a tecnologia.
- **LGPD**: os dados ficam no Google Cloud, na região escolhida. Isso precisa constar no termo (TCLE).
