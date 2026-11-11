<!--
Sync Impact Report
- Version change: 1.0.0 → 2.0.0 (MAJOR: troca de tecnologia redefine os Princípios I, II e V)
- Modified principles:
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

Razão: o oráculo só tem valor se cada item tiver duas classificações independentes e completas, de
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

Razão: o estudo coleta dados pessoais sob termo de consentimento, o pacote de replicação será
público e o próprio repositório já é público.

### V. Simplicidade e custo controlado

- Um único artefato publicável: o mesmo contêiner serve o Angular compilado e a API.
- Na produção, só serviços gerenciados do Google Cloud: Cloud Run, Cloud SQL para MySQL, Secret
  Manager e Artifact Registry. Qualquer serviço a mais exige justificativa no plano.
- O projeto no Google Cloud DEVE ter alerta de orçamento de US$ 15 por mês.
- Interface só com componentes do Angular Material, responsiva do celular ao computador.
- Cada chamada à API DEVE terminar em menos de 2 segundos no uso normal.

Razão: menos peças significam menos a manter, e um custo previsível cabe no orçamento de um
estudante.

## Escopo e exceções

- Esta constitution governa o app Veredito, publicado a partir do branch `Veredito` do repositório
  `diegoquirino/openscience`.
- **Exceção: spec 002 (`002-csv-oraculo`).** O gerador do CSV de importação é um comando Python do
  pipeline em `ISSTA_2027` e segue as convenções daquele projeto. Os Princípios I, II, III e V e as
  regras de publicação não se aplicam a ele. Continuam valendo: a ausência de dados de avaliadores
  e de identificadores cegos no arquivo gerado (Princípio IV), UTF-8 e a compatibilidade com o
  formato de importação do Veredito.

## Restrições e fluxo de trabalho

- O branch `Veredito` é órfão: não compartilha histórico com os demais estudos do repositório.
- Toda mudança DEVE passar pelo ambiente de ensaio (serviço e banco próprios) antes da produção.
- A publicação é feita pelo GitHub Actions a partir do branch `Veredito`, sempre no mesmo serviço
  do Cloud Run, para manter a URL.
- As specs são implementadas uma por vez, na ordem de `specs/ROADMAP.md`. A seguinte só começa
  quando os critérios de sucesso da anterior forem atendidos no ensaio.
- Os arquivos exportados DEVEM continuar compatíveis com o pacote `issta_tcm`.
- Todos os arquivos de texto do projeto DEVEM estar em UTF-8.

## Governance

- Esta constitution prevalece sobre outras práticas do projeto. Todo plano (`/speckit-plan`) DEVE
  conferir a conformidade com ela.
- Um plano que viole um princípio DEVE justificar a exceção na sua seção de verificação da
  constitution; sem justificativa aceita, o plano não segue.
- Emendas são feitas com `/speckit-constitution`, registram a data e seguem versão
  maior.menor.correção: maior para remover ou redefinir um princípio, menor para acrescentar
  princípio ou seção, correção para ajustes de redação.

**Version**: 2.0.0 | **Ratified**: 2026-11-10 | **Last Amended**: 2026-11-11
