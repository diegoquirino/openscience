<!--
Sync Impact Report
- Version change: 2.6.0 → 2.6.1 (PATCH: critério de sucesso conferido numa spec posterior)
- Modified sections (2.6.1):
  - Restrições e fluxo de trabalho: um critério que só pode ser conferido numa spec posterior fica
    para ela, se a spec disser isso (caso do SC-003 da spec 002, conferido na 005)
- Templates/docs (2.6.1): specs/ROADMAP.md (ordem estritamente sequencial: 002, 003, 004, 005,
  007, 006, 008, 009, 010, 011; sem colunas de paralelismo); specs 002, 003, 005, 006 e 009
  (cabeçalhos sem "Pode correr em paralelo"); spec 007 (elegibilidade aberta até a 006)
- Version change 2.6.0 (2.5.0 → 2.6.0, MINOR: vocabulário da rotulagem dos dados)
- Modified sections (2.6.0):
  - Restrições e fluxo de trabalho: vocabulário (rotulagem dos dados; "avaliar" em botões e
    mensagens curtas; participante e voluntário; rotulador 1 e 2)
  - III. Integridade da coleta (razão): "duas classificações" passa a "dois rótulos"
  - Escopo e exceções: "dados de avaliadores" passa a "dados de rotuladores"
- Version change 2.5.0 (2.4.0 → 2.5.0, MINOR: versionamento da API pelo caminho, `/api/v1`)
- Modified principles (2.5.0):
  - I. Stack padrão, atual e de código aberto: versão no caminho com o suporte nativo do Spring
    Framework; versão nova só para mudança incompatível; anterior obsoleta com `Deprecation` e
    `Sunset` até sair de uso; domínio e banco sem versão; ponto de entrada por versão
    (`/api/v1`); uma especificação OpenAPI por versão
  - II. Regras de domínio isoladas e testadas: testes partem de `/api/v1`; cada versão suportada
    tem seus testes e sua especificação conferida
- Version change 2.4.0 (2.3.0 → 2.4.0, MINOR: ensaio local, testes e piloto só com itens do Minitest)
- Modified principles (2.4.0):
  - IV. Privacidade, LGPD e segredos: ensaio local, testes automatizados e piloto em produção usam
    só itens do Minitest; importação recusa outro sistema no ensaio e na fase `piloto`
- Modified sections (2.4.0):
  - Restrições e fluxo de trabalho: piloto só com o Minitest; depois de recriar a base, a fase
    passa a `coleta` antes de importar a base real
- Version change 2.3.0 (2.2.0 → 2.3.0, MINOR: testes automatizados da API com REST Assured)
- Modified principles (2.3.0):
  - II. Regras de domínio isoladas e testadas: testes da API com REST Assured, por HTTP real numa
    porta aleatória, partindo de `/api` e seguindo os links
- Templates: nenhum template do Spec Kit cita ferramenta de teste; nada a ajustar
- Version change 2.2.0 (2.1.0 → 2.2.0, MINOR: API REST no nível 3 de Richardson, "glory of REST", e
  documentada em OpenAPI/Swagger)
- Modified principles (2.2.0):
  - I. Stack padrão, atual e de código aberto: regra da API (recursos, verbos e status HTTP,
    hipermídia HAL-FORMS com Spring HATEOAS, ponto de entrada `/api`, Problem Details) e
    documentação OpenAPI 3.1 com Swagger UI (springdoc-openapi)
  - II. Regras de domínio isoladas e testadas: links e ações por estado cobertos por testes da API;
    teste que barra endpoint sem documentação completa
- Version change 2.1.0 (2.0.1 → 2.1.0, MINOR: ambientes redefinidos; produção no Google Cloud e ensaio
  local em Docker, sem ensaio na nuvem)
- Modified sections (2.1.0):
  - Restrições e fluxo de trabalho: regra de ensaio passa a ser o Docker local; piloto na produção
    com a base recriada vazia antes da divulgação
  - IV. Privacidade, LGPD e segredos: ensaio local só com dados fictícios; dados de produção não
    descem para a máquina
  - V. Simplicidade e custo controlado: um único serviço e um único banco na nuvem
- Templates: nenhum template do Spec Kit cita ambientes; nada a ajustar
- Version change 2.0.1: alerta de orçamento do Princípio V passa de US$ 15 para R$ 200 por mês
- Version change anterior: 1.0.0 → 2.0.0 (MAJOR: troca de tecnologia redefine os Princípios I, II e V)
- Modified principles (2.0.0):
  - I. Custo zero numa conta Google gratuita → I. Stack padrão, atual e de código aberto
  - II. Regras testáveis sem publicar → II. Regras de domínio isoladas e testadas
  - III. Integridade da coleta (NÃO NEGOCIÁVEL): lock do script → transações e restrições do banco;
    migrações só pelo Flyway
  - IV. Privacidade e LGPD → IV. Privacidade, LGPD e segredos (repositório público)
  - V. Simplicidade dentro dos limites da plataforma → V. Simplicidade e custo controlado
- Added sections: nenhuma (Escopo e exceções, Restrições e fluxo de trabalho reescritas)
- Removed sections: nenhuma
- Follow-up TODOs: nenhum
-->

# Veredito Constitution

## Core Principles

### I. Stack padrão, atual e de código aberto

- Frontend: Angular com Angular Material (Material Design 3), em TypeScript.
- Backend: Spring Boot sobre Java LTS, em Maven.
- API: REST no nível 3 do modelo de maturidade de Richardson ("glory of REST"):
  - recursos identificados por URI;
  - métodos e códigos de status HTTP com a semântica padrão;
  - hipermídia com Spring HATEOAS em HAL-FORMS: cada resposta traz os links e as ações
    permitidas no estado atual do recurso;
  - versão no caminho (`/api/v1`, `/api/v2`), com o suporte nativo do Spring Framework a
    versões de API. Só mudança incompatível cria versão nova; a anterior continua no ar,
    marcada como obsoleta (cabeçalhos `Deprecation` e `Sunset`), até não ter mais uso. A camada
    de domínio e o banco não têm versão;
  - um único ponto de entrada por versão, `/api/v1`; o cliente Angular navega pelas relações de
    link e não monta URLs da API;
  - erros em Problem Details (RFC 9457).
- Documentação da API: OpenAPI 3.1 gerada a partir do código (springdoc-openapi), com Swagger UI e
  uma especificação por versão da API. Todo endpoint DEVE ter:
  - resumo e descrição em português;
  - parâmetros, corpo e exemplos;
  - todas as respostas possíveis, com o esquema de erro;
  - as relações de link e ações que oferece.

  O Swagger UI fica aberto no ensaio local e, na produção, só para o administrador.
- Banco: MySQL Community, com o esquema versionado pelo Flyway (edição de código aberto).
- Cada spec DEVE começar com as versões estáveis mais recentes de cada peça (a LTS, quando
  houver) e registrá-las no seu plano. Atualizações menores e de correção são livres; trocar uma
  peça por outra tecnologia exige emendar esta constitution.
- Só edições comunitárias ou de código aberto; nenhuma licença paga.

Razão: tecnologias de mercado, conhecidas e bem documentadas dão segurança para manter o sistema
durante toda a coleta e para quem for reproduzir o estudo.

### II. Regras de domínio isoladas e testadas

- A validação do CSV, a divisão em blocos, a escolha do próximo bloco e o formato das exportações
  DEVEM ficar numa camada de domínio em Java puro, sem dependência de Spring, banco ou HTTP, com
  testes unitários.
- O acesso ao banco DEVE ter testes de integração contra um MySQL da mesma versão da produção,
  em contêiner.
- Os testes automatizados da API DEVEM ser escritos com REST Assured, por HTTP real contra a
  aplicação numa porta aleatória (Spring Boot Test com MySQL em contêiner). Cada teste parte do
  ponto de entrada da versão (`/api/v1`) e segue as relações de link, como o cliente Angular; a
  única URL fixa nos testes é a do ponto de entrada. Cada versão suportada tem os seus testes.
- Os links e ações que a API oferece em cada estado de um recurso DEVEM ser cobertos por esses
  testes; uma ação proibida naquele estado não aparece como link e, se chamada, é recusada.
- Um teste DEVE falhar se algum endpoint da especificação OpenAPI gerada, em qualquer versão
  suportada, estiver sem resumo, descrição, respostas de erro ou exemplos; o CI guarda a
  especificação de cada versão (`openapi-v1.json`, ...) de cada commit.
- Uma regra nova ou alterada DEVE vir acompanhada dos seus testes, no mesmo commit.

Razão: as regras de alocação são o ponto de maior risco e precisam ser verificadas sem depender do
ambiente publicado.

### III. Integridade da coleta (NÃO NEGOCIÁVEL)

- Pedir bloco, salvar rascunho, enviar bloco e importar DEVEM rodar, cada um, numa única transação
  do banco, com bloqueio das linhas disputadas.
- As invariantes DEVEM estar também no banco, como restrições: um participante por bloco (nunca os
  dois papéis), uma resposta por item e papel, um item por (`sistema`, `edit_key`).
- Blocos e suas posições na fila NÃO DEVEM mudar depois de criados; respostas finais são imutáveis
  para o participante.
- O esquema só muda por migrações versionadas do Flyway; migração já aplicada NUNCA é editada.

Razão: o oráculo só tem valor se cada item tiver dois rótulos independentes e completos, de
pessoas diferentes, e isso não pode depender só do código da aplicação.

### IV. Privacidade, LGPD e segredos

- O código fica num repositório público: senhas, chaves, URLs de banco com credenciais, dados de
  participantes, dumps e exportações NUNCA vão para o git.
- Segredos de produção ficam no Secret Manager do Google Cloud e, para a publicação, em segredos
  do GitHub; o acesso do GitHub ao Google Cloud é feito sem chave de longa duração.
- Códigos de acesso e identificadores de sessão DEVEM ser guardados só em hash; a sessão vai em
  cookie `HttpOnly`, `Secure` e `SameSite`.
- A tabela longa e os arquivos do pipeline (`t1_<sistema>_rater*.csv`) NÃO DEVEM conter nome nem
  e-mail.
- O banco não é exposto à internet; participantes só acessam dados pela aplicação.
- O ensaio local, os testes automatizados e o ensaio piloto em produção usam só itens do sistema
  Minitest: o CSV gerado pela spec 002 e arquivos derivados dele para os casos de erro e de
  volume. O CSV fica versionado no repositório, porque o Minitest já é público. Os participantes
  do ensaio local e dos testes são fictícios.
- A importação DEVE recusar arquivo de outro sistema no ensaio local e, na produção, enquanto a
  fase do estudo for `piloto`.
- Dumps e exportações da produção NÃO DEVEM ser carregados na máquina de desenvolvimento.

Razão: o estudo coleta dados pessoais sob termo de consentimento, o pacote de replicação será
público e o próprio repositório já é público.

### V. Simplicidade e custo controlado

- Um único artefato publicável: o mesmo contêiner serve o Angular compilado e a API.
- Na produção, só serviços gerenciados do Google Cloud: Cloud Run, Cloud SQL para MySQL, Secret
  Manager e Artifact Registry, com um único serviço do Cloud Run e um único banco. Qualquer
  serviço a mais exige justificativa no plano.
- O projeto no Google Cloud (`veredito-issta2027`) DEVE ter orçamento mensal de R$ 200, só com
  alertas (sem limite que suspenda o serviço), avisando em 50%, 90% e 100%.
- Interface só com componentes do Angular Material, responsiva do celular ao computador.
- Cada chamada à API DEVE terminar em menos de 2 segundos no uso normal.

Razão: menos peças significam menos a manter, e um custo previsível cabe no orçamento de um
estudante.

## Escopo e exceções

- Esta constitution governa o app Veredito, publicado a partir do branch `Veredito` do repositório
  `diegoquirino/openscience`.
- **Exceção: spec 002 (`002-csv-oraculo`).** O gerador do CSV de importação é um comando Python do
  pipeline em `ISSTA_2027` e segue as convenções daquele projeto. Os Princípios I, II, III e V e as
  regras de publicação não se aplicam a ele. Continuam valendo: a ausência de dados de rotuladores
  e de identificadores cegos no arquivo gerado (Princípio IV), UTF-8 e a compatibilidade com o
  formato de importação do Veredito.

## Restrições e fluxo de trabalho

- O branch `Veredito` é órfão: não compartilha histórico com os demais estudos do repositório.
- **Ambientes**: a produção é o Google Cloud (projeto `veredito-issta2027`); o ensaio (testes) é
  local, em Docker: Docker Compose com a imagem construída pelo mesmo `Dockerfile` da produção, um
  MySQL na mesma versão e um servidor de e-mail de teste. Não existe ensaio na nuvem.
- Toda mudança DEVE passar pelos testes automáticos e pelo ensaio local antes da produção.
- O ensaio piloto com colegas acontece na produção, antes da divulgação, só com os itens do
  Minitest. Ao fim dele, a base de produção é recriada vazia, o administrador passa a fase do
  estudo para `coleta` e só então importa a base real.
- A publicação é feita pelo GitHub Actions a partir do branch `Veredito`, sempre no mesmo serviço
  do Cloud Run, para manter a URL.
- As specs são implementadas uma por vez, na ordem de `specs/ROADMAP.md`. A seguinte só começa
  quando os critérios de sucesso da anterior forem atendidos no ensaio local (ou na produção, quando
  o critério for da própria publicação). Um critério que só pode ser conferido numa spec posterior
  fica para ela, se a spec disser isso.
- Os arquivos exportados DEVEM continuar compatíveis com o pacote `issta_tcm`.
- Todos os arquivos de texto do projeto DEVEM estar em UTF-8.
- Vocabulário: a atividade é a **rotulagem dos dados** (títulos, termo, convites, explicações e
  documentação); botões e mensagens curtas usam "avaliar". Quem rotula é "participante" no termo,
  na LGPD e nos documentos formais, e "voluntário" nos convites e telas; a posição no bloco é
  rotulador 1 ou 2. Nomes de colunas e arquivos do pipeline (`rater1`, `rater2`) não mudam. A
  tabela completa fica na seção *Vocabulário* da spec 001.

## Governance

- Esta constitution prevalece sobre outras práticas do projeto. Todo plano (`/speckit-plan`) DEVE
  conferir a conformidade com ela.
- Um plano que viole um princípio DEVE justificar a exceção na sua seção de verificação da
  constitution; sem justificativa aceita, o plano não segue.
- Emendas são feitas com `/speckit-constitution`, registram a data e seguem versão
  maior.menor.correção: maior para remover ou redefinir um princípio, menor para acrescentar
  princípio ou seção, correção para ajustes de redação.

**Version**: 2.6.1 | **Ratified**: 2026-11-10 | **Last Amended**: 2026-11-11
