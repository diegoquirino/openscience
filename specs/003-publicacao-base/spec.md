# Feature Specification: Publicar o Veredito e preparar a base de dados

**Feature Branch**: `003-publicacao-base`

**Created**: 2026-11-10

**Status**: Draft

**Input**: User description: "Deixar o Veredito no ar numa URL fixa, com uma página inicial, uma base de dados com todas as tabelas previstas, um ambiente de ensaio separado e uma forma de atualizar o sistema sem mudar a URL."

**Spec geral**: [001-rotulagem-oraculo](../001-rotulagem-oraculo/spec.md) · **Depende de**: nada · **Pode correr em paralelo com**: 002

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Abrir o Veredito numa URL fixa (Priority: P1)

Qualquer pessoa abre o endereço do Veredito, sem conta nem instalação, e vê a página inicial do estudo.

**Why this priority**: Todas as specs seguintes acrescentam telas a esta página; sem ela não há o que testar no ar.

**Independent Test**: Abrir a URL numa janela anônima, num computador e num celular.

**Acceptance Scenarios**:

1. **Dado** o Veredito publicado, **Quando** alguém abre a URL numa janela anônima, **Então** vê a página inicial com o nome "Veredito", uma frase sobre o estudo e o botão "Participar", sem precisar de login em nenhum serviço.
2. **Dado** um celular, **Quando** a página inicial é aberta, **Então** o conteúdo cabe na largura da tela, sem rolagem horizontal.
3. **Dado** a página inicial, **Quando** alguém a abre, **Então** o rodapé mostra a versão publicada do sistema.

---

### User Story 2 - Atualizar o sistema sem mudar a URL (Priority: P1)

O pesquisador publica uma nova versão e os participantes continuam usando o mesmo endereço.

**Why this priority**: O sistema vai mudar durante a coleta (correções de texto, novos sistemas); a URL divulgada não pode quebrar.

**Independent Test**: Publicar três versões seguidas e conferir a URL e a versão no rodapé.

**Acceptance Scenarios**:

1. **Dado** uma versão no ar, **Quando** o pesquisador publica outra, **Então** a URL não muda e o rodapé passa a mostrar a nova versão.
2. **Dado** uma nova versão publicada, **Quando** a base de dados já tem registros, **Então** nenhum registro é apagado ou alterado pela publicação.
3. **Dado** uma versão com defeito, **Quando** o pesquisador volta à versão anterior, **Então** a URL continua a mesma.

---

### User Story 3 - Preparar a base de dados (Priority: P1)

Na primeira publicação, o sistema cria todas as tabelas previstas na spec 001, com as restrições de integridade e a configuração padrão. A cada nova versão, a estrutura é atualizada automaticamente, sem perder dados.

**Why this priority**: Fixa a estrutura de dados de que dependem todas as specs seguintes, que podem então ser implementadas em paralelo.

**Independent Test**: Publicar numa base vazia, publicar de novo e comparar as estruturas; publicar uma versão com uma mudança de estrutura sobre uma base com registros.

**Acceptance Scenarios**:

1. **Dado** uma base vazia, **Quando** o sistema é publicado, **Então** existem as tabelas de lotes, itens, blocos, tarefas (bloco-papel), respostas, participantes, códigos de acesso, sessões de acesso, termos e configuração.
2. **Dado** uma base vazia, **Quando** é preparada, **Então** a configuração recebe os valores padrão: e-mail do administrador `diegoquirino@gmail.com`, reserva de 30 minutos, tamanho de bloco 10, limite diário de 450 e-mails, nenhum termo publicado.
3. **Dado** uma base já preparada e com registros, **Quando** a mesma versão é publicada de novo, **Então** nada é duplicado, apagado ou alterado.
4. **Dado** uma nova versão que muda a estrutura, **Quando** é publicada sobre uma base com registros, **Então** a estrutura é atualizada uma única vez, os registros são preservados e fica registrado qual mudança foi aplicada e quando.
5. **Dado** a base, **Quando** alguém tenta acessá-la pela internet sem passar pelo sistema, **Então** o acesso é recusado.

---

### User Story 4 - Ensaiar antes de publicar (Priority: P2)

O pesquisador testa uma mudança num ambiente de ensaio, com endereço e base de dados próprios, antes de levá-la à produção.

**Why this priority**: Permite corrigir o sistema durante a coleta sem arriscar os dados reais.

**Independent Test**: Criar registros no ensaio e conferir que a base de produção não muda.

**Acceptance Scenarios**:

1. **Dado** os dois ambientes, **Quando** alguém usa o ensaio, **Então** a página mostra uma faixa "AMBIENTE DE ENSAIO" e todos os registros vão para a base de ensaio.
2. **Dado** uma mudança aprovada no ensaio, **Quando** o pesquisador a publica em produção, **Então** a produção passa a ter exatamente a mesma versão do código.

---

### Edge Cases

- Base de dados apagada ou inacessível: a página inicial mostra "Sistema temporariamente indisponível" em vez de um erro técnico.
- Dois acessos no exato momento de uma publicação: cada um é atendido inteiro pela versão antiga ou pela nova.
- Página aberta por horas antes de uma publicação: a próxima ação continua funcionando ou pede para recarregar a página.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: O sistema DEVE ficar acessível numa URL fixa, sem exigir conta ou instalação do visitante.
- **FR-002**: Publicar uma nova versão NÃO DEVE mudar a URL nem alterar dados existentes.
- **FR-003**: O rodapé DEVE mostrar a versão publicada.
- **FR-004**: A preparação da base DEVE criar todas as tabelas do modelo de dados da spec 001, com as restrições de integridade, e ser segura para repetir; mudanças de estrutura DEVEM ser versionadas, aplicadas automaticamente na publicação e registradas.
- **FR-005**: A configuração DEVE guardar, no mínimo: e-mail do administrador, minutos de reserva, tamanho do bloco, limite diário de e-mails, versão e texto do termo de consentimento.
- **FR-006**: A base NÃO DEVE ser acessível pela internet; só o sistema e o pesquisador, com credenciais próprias, a acessam.
- **FR-007**: DEVE existir um ambiente de ensaio, com URL e base de dados próprias e identificação visível na tela.
- **FR-008**: As regras do estudo (validação do CSV, formação de blocos, escolha do próximo bloco, formato das exportações) DEVEM poder ser verificadas por testes automáticos sem publicar o sistema.
- **FR-009**: Erros inesperados DEVEM aparecer ao usuário como mensagem amigável e ficar registrados para o pesquisador.
- **FR-010**: A publicação DEVE ser automática a partir do repositório: cada mudança aprovada vai primeiro ao ensaio, e a produção recebe exatamente a mesma versão já ensaiada.
- **FR-011**: Senhas e chaves NÃO DEVEM ficar no repositório de código.

### Key Entities

- **Configuração**: parâmetros do estudo que o pesquisador ajusta sem publicar código.
- **Ambiente**: produção ou ensaio, cada um com sua base.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Uma mudança enviada ao repositório chega ao ensaio em menos de 15 minutos, sem passos manuais.
- **SC-002**: Em três publicações seguidas, a URL não muda e nenhum registro da base é alterado.
- **SC-003**: Preparar a base duas vezes resulta na mesma estrutura.
- **SC-004**: A página inicial abre em menos de 3 segundos, depois que o sistema já está em uso.
- **SC-005**: O custo mensal do sistema fica abaixo de R$ 200, com alerta automático se passar disso.
- **SC-006**: Uma busca por senhas e chaves no repositório público não encontra nada.

## Assumptions

- A tecnologia segue a seleção em [research.md](../001-rotulagem-oraculo/research.md) da spec 001 e a constitution 2.0.0.
- A base de dados fica no projeto de nuvem do pesquisador; nenhum participante tem acesso direto a ela.
- A primeira abertura depois de um período sem uso pode levar alguns segundos a mais; isso é medido no ensaio.
- O botão "Participar" leva a uma tela provisória até a spec 004 ser entregue.
