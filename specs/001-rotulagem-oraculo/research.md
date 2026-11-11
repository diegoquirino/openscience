# Seleção de tecnologias: Veredito

**Feature**: `001-rotulagem-oraculo` · **Data**: 2026-11-11 · **Spec**: [spec.md](./spec.md) · **Constitution**: 2.0.0

Esta revisão substitui a decisão de 2026-11-10 (Apps Script com Google Sheets), descartada por insegurança quanto ao ambiente gratuito.

## Decisão

**Angular com Angular Material no frontend, Spring Boot no backend e MySQL Community com Flyway, num único contêiner publicado no Cloud Run, com o banco no Cloud SQL. O código fica no branch órfão `Veredito` do repositório público `diegoquirino/openscience`.**

## Versões (conferidas em 2026-11-11)

| Peça | Versão | Observação |
|---|---|---|
| Angular, Angular CDK e Angular Material | 22.2.x | Material Design 3 |
| Node.js (só para compilar o frontend) | 26 LTS | |
| Java | 25 LTS (Eclipse Temurin) | |
| Spring Boot | 4.1.2 | A 4.2.0 está em RC2, com versão final prevista para novembro de 2026; usar a 4.2.x se já tiver saído quando o plano da spec 003 for feito |
| Maven | 3.9.x ou superior | Via Maven Wrapper |
| MySQL Community | 9.7 LTS | Também é a versão do contêiner de desenvolvimento e de teste |
| Flyway (código aberto) | 13.12.1 | Com o módulo `flyway-mysql`; sobrepor a versão gerenciada pelo Spring Boot se ela for mais antiga |
| Testcontainers | gerenciada pelo Spring Boot | MySQL 9.7 em contêiner nos testes de integração |

## Arquitetura

```text
Navegador (Angular + Angular Material)
        │  HTTPS, cookie de sessão HttpOnly
        ▼
Cloud Run: contêiner único
  Spring Boot 4 (Java 25)
   ├─ /       → arquivos do Angular compilado
   ├─ /api/** → API REST (JSON)
   ├─ domínio em Java puro (CSV, blocos, fila, exportações)
   └─ Flyway roda as migrações na subida
        │ conector do Cloud SQL (IAM)           │ SMTP (TLS)
        ▼                                       ▼
Cloud SQL para MySQL 9.7                    smtp.gmail.com
  bancos: veredito, veredito_ensaio         (diegoquirino@gmail.com)
Secret Manager: senha do banco, senha de app do Gmail, chave de sessão
```

### Organização do branch `Veredito`

```text
/                       raiz do branch órfão (conteúdo da pasta Veredito)
├─ backend/             Spring Boot (Maven Wrapper)
│  └─ src/main/resources/db/migration/   migrações Flyway V1__..., V2__...
├─ frontend/            Angular + Angular Material
├─ compose.yaml         MySQL 9.7 para desenvolvimento local
├─ Dockerfile           build em estágios: Node 26 → Maven/JDK 25 → JRE 25
├─ .github/workflows/   testes, imagem e publicação
├─ specs/, .specify/, .cursor/
└─ .gitignore
```

- No desenvolvimento, `ng serve` encaminha `/api` para o Spring Boot em `localhost:8080`, e o suporte do Spring Boot ao Docker Compose sobe o MySQL sozinho.
- Na imagem, o Angular compilado vai para os recursos estáticos do Spring Boot, que serve tudo pela mesma URL (sem CORS).

## Produção no Google Cloud

| Peça | Configuração |
|---|---|
| Região | `southamerica-east1` (São Paulo), perto dos avaliadores; `us-central1` é mais barata se o custo apertar |
| Cloud Run | Serviço `veredito` (produção) e `veredito-ensaio`; mínimo de 0 instâncias, máximo de 4; *startup CPU boost* ligado |
| Cloud SQL | Uma instância MySQL 9.7, edição Enterprise, a menor máquina compartilhada (`db-f1-micro`, 0,6 GB, 25 conexões); dois bancos com usuários próprios, `veredito` e `veredito_ensaio`; backup diário automático com retenção de 7 dias |
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

1. **A cada push**: testes do backend (`./mvnw verify`, com Testcontainers) e do frontend; build da imagem; envio ao Artifact Registry; publicação no `veredito-ensaio`.
2. **Produção**: workflow disparado manualmente, que publica no `veredito` a **mesma imagem** (mesmo *digest*) já aprovada no ensaio. A URL não muda.
3. **Autenticação**: Workload Identity Federation entre GitHub e Google Cloud, sem chave de conta de serviço guardada no GitHub.
4. **Volta atrás**: o Cloud Run mantém as revisões anteriores; basta mandar o tráfego para a anterior. Por isso, toda migração Flyway precisa ser compatível com a versão anterior do código (acrescentar antes, remover depois).

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
| API | Spring Boot Test com MockMvc |
| Frontend | Executor de testes padrão do Angular 22 |

## Limites que importam

| Limite | Valor | Mitigação |
|---|---|---|
| Conexões do `db-f1-micro` | 25 | Pool pequeno e no máximo 4 instâncias do Cloud Run; subir para `db-g1-small` (cerca de US$ 27 por mês) se faltar |
| `db-f1-micro` sem SLA | — | Aceitável para um estudo; backup diário |
| E-mails do Gmail | cerca de 500 por dia | Sessão de 24 h, limites de reenvio, contador no painel; reserva: provedor transacional |
| Partida a frio do Cloud Run | alguns segundos na primeira chamada após inatividade | *Startup CPU boost* e cache AOT do Java; mínimo de 1 instância durante a divulgação, se necessário (custo a medir no ensaio) |

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
- **E-mail**: a senha de app do Gmail ("Veredito SMTP") já existe e está guardada fora do repositório; entra no Secret Manager no plano da spec 003.
- **CSV com os casos de uso completos**: especificado em [002-csv-oraculo](../002-csv-oraculo/spec.md).
- **Concordância entre avaliadores**: ver a seção *Assumptions* da spec. Afeta a análise, não a tecnologia.
- **LGPD**: os dados ficam no Google Cloud, na região escolhida. Isso precisa constar no termo (TCLE).
