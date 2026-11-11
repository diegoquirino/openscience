# Feature Specification: Atender pedido de retirada de dados (LGPD)

**Feature Branch**: `011-retirada-lgpd`

**Created**: 2026-11-10

**Status**: Draft

**Input**: User description: "A pedido de um participante, o administrador remove o nome e o e-mail dele e decide se as respostas ficam, anonimizadas, ou são descartadas, devolvendo os blocos à fila."

**Spec geral**: [001-rotulagem-oraculo](../001-rotulagem-oraculo/spec.md), User Story 10 e FR-059 · **Depende de**: 007 e 009

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Anonimizar mantendo as respostas (Priority: P1)

O administrador localiza o participante pelo e-mail e apaga nome e e-mail, mantendo as respostas ligadas só ao pseudônimo.

**Why this priority**: Atende ao direito de eliminação de dados pessoais sem perder classificações já usadas.

**Independent Test**: Anonimizar um participante com blocos concluídos e conferir telas e exportações.

**Acceptance Scenarios**:

1. **Dado** a tela "Participantes", **Quando** o administrador busca por e-mail, **Então** vê o pseudônimo, a situação e os blocos concluídos e em andamento daquela pessoa.
2. **Dado** um participante, **Quando** o administrador confirma "Anonimizar e manter respostas", **Então** nome e e-mail somem de todas as telas e exportações (a tabela larga mostra "anonimizado") e as respostas continuam ligadas só ao pseudônimo.
3. **Dado** um participante anonimizado, **Quando** alguém tenta entrar com o mesmo e-mail, **Então** é tratado como pessoa nova, com outro pseudônimo.
4. **Dado** um participante anonimizado com bloco reservado, **Quando** a anonimização é confirmada, **Então** a reserva é liberada e os rascunhos são descartados.

---

### User Story 2 - Anonimizar e descartar as respostas (Priority: P2)

O administrador apaga nome, e-mail e todas as respostas da pessoa; os blocos que ela concluiu voltam à fila.

**Why this priority**: Atende a quem retira o consentimento por completo; é raro.

**Independent Test**: Descartar um participante com dois blocos concluídos e conferir a fila e as exportações.

**Acceptance Scenarios**:

1. **Dado** um participante com blocos concluídos, **Quando** o administrador confirma "Anonimizar e descartar respostas", **Então** as respostas dele são apagadas e as tarefas que ele concluiu voltam a ficar livres, na mesma posição da fila.
2. **Dado** uma tarefa devolvida, **Quando** alguém pede um bloco, **Então** ela é entregue antes das tarefas que vêm depois na fila.
3. **Dado** a confirmação, **Quando** pedida, **Então** o sistema mostra quantas respostas e tarefas serão afetadas e exige confirmação explícita.

---

### Edge Cases

- Participante que avaliou o papel 1 de um bloco cujo papel 2 é de outra pessoa: só a tarefa dele volta à fila.
- Retirada depois de uma exportação: os arquivos já baixados não mudam; o sistema avisa que exportações antigas precisam ser refeitas e descartadas.
- Pedido de retirada de alguém sem respostas: só os dados pessoais são apagados.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: O administrador DEVE poder buscar um participante por e-mail ou pseudônimo.
- **FR-002**: O administrador DEVE poder anonimizar um participante mantendo ou descartando as respostas.
- **FR-003**: Anonimizar DEVE apagar nome e e-mail de forma definitiva e liberar qualquer reserva da pessoa.
- **FR-004**: Descartar DEVE apagar as respostas da pessoa e devolver as tarefas concluídas por ela à fila, na posição original.
- **FR-005**: A operação DEVE ser registrada com data, hora, pseudônimo e opção escolhida, sem dados pessoais.
- **FR-006**: A operação DEVE ser atômica em relação a pedidos, salvamentos e envios de blocos.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Depois de uma anonimização, uma busca pelo nome e pelo e-mail em todas as tabelas e exportações não encontra nada.
- **SC-002**: Um pedido de retirada é atendido em menos de 5 minutos.

## Assumptions

- O pedido chega ao pesquisador por e-mail, fora do sistema, conforme o termo (TCLE).
- Cópias de segurança e exportações antigas são tratadas pelo pesquisador, fora do sistema.
