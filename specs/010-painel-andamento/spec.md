# Feature Specification: Painel de andamento da coleta

**Feature Branch**: `010-painel-andamento`

**Created**: 2026-11-10

**Status**: Draft

**Input**: User description: "O administrador acompanha, por sistema e por lote, quantos blocos-papel estão livres, em andamento e concluídos, as reservas em andamento, os totais de participantes por situação e por habilidade e a cota de e-mails restante do dia."

**Spec geral**: [001-rotulagem-oraculo](../001-rotulagem-oraculo/spec.md), User Story 9 e FR-056 · **Depende de**: 005, 006 e 007

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Ver o andamento da fila (Priority: P1)

O administrador vê quanto da base já foi rotulado, por sistema e por lote.

**Why this priority**: Responde à pergunta mais frequente durante a coleta: quanto falta.

**Independent Test**: Conferir os números do painel contra a tabela longa da spec 009.

**Acceptance Scenarios**:

1. **Dado** blocos em vários estados, **Quando** o administrador abre o painel, **Então** vê, por sistema e por lote: itens, blocos e tarefas do papel 1 e do papel 2 livres, em andamento e concluídas, com o percentual concluído.
2. **Dado** o painel, **Quando** aberto, **Então** a primeira linha mostra o total geral e a posição da primeira tarefa livre na fila.

---

### User Story 2 - Ver as reservas em andamento (Priority: P2)

O administrador vê quem está com qual bloco e quando a reserva vence.

**Why this priority**: Ajuda a entender blocos parados, sem expor identidade além do pseudônimo.

**Independent Test**: Reservar dois blocos com pessoas diferentes e conferir o painel.

**Acceptance Scenarios**:

1. **Dado** tarefas reservadas, **Quando** o painel é aberto, **Então** cada uma aparece com o pseudônimo do participante, o bloco e o papel, quantos itens já têm rascunho e quando a reserva vence.
2. **Dado** uma reserva vencida que ninguém pegou, **Quando** o painel é aberto, **Então** aparece como "vencida".

---

### User Story 3 - Ver os participantes e a cota de e-mails (Priority: P2)

O administrador vê quantas pessoas entraram, em que situação estão e quantos e-mails ainda podem ser enviados hoje.

**Why this priority**: Orienta a divulgação e evita surpresas com a cota de e-mails.

**Independent Test**: Registrar participantes em cada situação e conferir os totais.

**Acceptance Scenarios**:

1. **Dado** participantes registrados, **Quando** o painel é aberto, **Então** mostra totais de elegíveis, inelegíveis, que recusaram o termo e que ainda não concluíram as etapas, e a distribuição por habilidade profissional.
2. **Dado** o painel, **Quando** aberto, **Então** mostra quantos e-mails ainda podem ser enviados hoje, com destaque quando restarem menos de 10.
3. **Dado** participantes com blocos concluídos, **Quando** o painel é aberto, **Então** mostra quantos blocos cada pseudônimo concluiu e a parcela do total de blocos-papel concluídos que isso representa, do maior para o menor. Como não há limite de blocos por pessoa, é assim que o pesquisador acompanha a concentração da rotulagem dos dados em poucos voluntários.

---

### Edge Cases

- Base vazia: o painel mostra zeros e um atalho para "Importar".
- Painel aberto por muito tempo: o botão "Atualizar" recarrega os números, sem recarregar a página.
- Muitos participantes: a lista por pseudônimo é paginada ou rolável.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: O painel DEVE mostrar, por sistema e por lote, itens, blocos e tarefas por papel e estado, com percentual concluído.
- **FR-002**: O painel DEVE listar as reservas em andamento com pseudônimo, bloco, papel, rascunhos e vencimento.
- **FR-003**: O painel DEVE mostrar totais de participantes por situação e por habilidade, e blocos concluídos por pseudônimo, com a parcela de cada um no total concluído.
- **FR-004**: O painel DEVE mostrar quantos e-mails foram enviados no dia e quantos ainda cabem no limite diário da configuração.
- **FR-005**: O painel NÃO DEVE mostrar nome nem e-mail de participantes.
- **FR-006**: Só o administrador DEVE ver o painel.
- **FR-007**: Abrir o painel NÃO DEVE alterar reservas nem respostas.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Os números do painel batem 100 % com a tabela longa e com a contagem de tarefas.
- **SC-002**: O painel abre em menos de 5 segundos com 500 itens e 100 participantes.

## Assumptions

- O painel é só de leitura; ações sobre participantes ficam na spec 011.
