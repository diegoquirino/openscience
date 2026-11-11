# Feature Specification: Tela do item com diferenças, contexto e perguntas explicadas

**Feature Branch**: `008-tela-item`

**Created**: 2026-11-10

**Status**: Draft

**Input**: User description: "A tela do item mostra a edição de forma confortável (antes e depois lado a lado, com as diferenças destacadas), botões que abrem num modal o caso de uso completo na versão anterior e na modificada, e as duas perguntas com a explicação de cada opção."

**Spec geral**: [001-rotulagem-oraculo](../001-rotulagem-oraculo/spec.md), User Story 4, FR-030 a FR-033, Apêndices A, B.1 e B.2 · **Depende de**: 003 (pode ser feita com itens de exemplo do arquivo da spec 002) · **Integra-se a**: 007

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Ler a edição com as diferenças destacadas (Priority: P1)

O avaliador vê o que mudou sem precisar comparar os textos de cabeça.

**Why this priority**: A validade do oráculo depende de o avaliador entender a edição.

**Independent Test**: Exibir um item de cada `kind` (MOD, ADD, DEL, META) do Minitest num computador e num celular.

**Acceptance Scenarios**:

1. **Dado** um item `MOD`, **Quando** é exibido, **Então** "Antes" (origin) e "Depois" (target) aparecem lado a lado, com as palavras removidas riscadas e as incluídas destacadas.
2. **Dado** um item `ADD`, **Quando** é exibido, **Então** aparece só "Trecho incluído" com o target.
3. **Dado** um item `DEL`, **Quando** é exibido, **Então** aparece só "Trecho removido" com o origin.
4. **Dado** um item `META` de `requirements`, **Quando** é exibido, **Então** aparece como "Vínculo com requisitos: antes / depois".
5. **Dado** qualquer item, **Quando** é exibido, **Então** o cabeçalho mostra sistema, caso de uso e o local da edição em linguagem natural (Apêndice A), por exemplo "Passo de fluxo de exceção · Modificação".
6. **Dado** um texto com várias linhas ou com mais de 400 caracteres, **Quando** é exibido, **Então** aparece inteiro, com as quebras de linha mantidas.
7. **Dado** uma tela estreita, **Quando** o item é exibido, **Então** "Antes" e "Depois" ficam um abaixo do outro, sem rolagem horizontal.
8. **Dado** um `field` sem tradução, **Quando** o item é exibido, **Então** aparece o nome original.

---

### User Story 2 - Consultar o caso de uso completo em cada versão (Priority: P1)

Junto do "Antes" e do "Depois", um botão abre num modal o caso de uso completo daquela versão, já na linha editada.

**Why this priority**: Muitas edições só fazem sentido no contexto do fluxo inteiro.

**Independent Test**: Abrir os dois modais num item de cada `kind`, fechar de cada forma e conferir que as escolhas do item continuam lá.

**Acceptance Scenarios**:

1. **Dado** qualquer item, **Quando** é exibido, **Então** junto do "Antes" há o botão "Ver caso de uso completo (v1.1)" e junto do "Depois", "Ver caso de uso completo (v1.2)", com os rótulos de versão do item; num `ADD` o primeiro botão fica no lugar do bloco "Antes", e num `DEL` o segundo fica no lugar do bloco "Depois".
2. **Dado** o botão da versão anterior, **Quando** clicado, **Então** abre o modal "<caso de uso> · versão v1.1 (anterior)" com o texto completo, em fonte monoespaçada e com a indentação preservada.
3. **Dado** o botão da versão modificada, **Quando** clicado, **Então** abre o modal "<caso de uso> · versão v1.2 (modificada)".
4. **Dado** linhas marcadas com `<- edited`, **Quando** o modal abre, **Então** elas aparecem destacadas, o marcador não é exibido e o modal já abre rolado até a primeira delas.
5. **Dado** um caso de uso sem linha marcada, **Quando** o modal abre, **Então** abre no topo, sem destaque.
6. **Dado** um modal aberto, **Quando** a pessoa clica em "Fechar", pressiona Esc ou clica fora, **Então** o modal fecha e as escolhas do item continuam lá.
7. **Dado** uma tela estreita, **Quando** o modal abre, **Então** ocupa a tela inteira e as linhas longas quebram sem perder a indentação inicial.

---

### User Story 3 - Responder com as opções explicadas (Priority: P1)

Cada pergunta traz a definição e exemplos de cada opção, e a regra de precedência da operação.

**Why this priority**: Reduz discordâncias causadas por entendimentos diferentes dos conceitos.

**Independent Test**: Conferir os textos contra os Apêndices B.1 e B.2 e responder um item.

**Acceptance Scenarios**:

1. **Dado** a pergunta de impacto, **Quando** exibida, **Então** mostra o enunciado e a explicação de Baixo e Alto do Apêndice B.1, com escolha única.
2. **Dado** a pergunta de operação, **Quando** exibida, **Então** mostra as oito opções com a explicação do Apêndice B.2, com escolha única, e a regra de precedência.
3. **Dado** as duas perguntas respondidas, **Quando** a pessoa avança, **Então** grava exatamente os mesmos códigos e comentário que a tela simples da spec 007.
4. **Dado** um teclado, **Quando** a pessoa navega pela tela, **Então** alcança botões, opções e modais pelo teclado, e o foco volta ao botão de origem quando o modal fecha.

---

### Edge Cases

- Edição que só muda espaços ou pontuação: o destaque mostra a diferença, mesmo que pequena.
- Texto idêntico antes e depois (por exemplo, metadados): a tela avisa "Sem diferença no texto deste trecho".
- Caso de uso com dezenas de passos: o modal rola sem travar a tela.
- Aviso de caso de uso inexistente numa versão (spec 002): o modal mostra o aviso.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: O cabeçalho do item DEVE mostrar sistema, caso de uso e o tipo e local da edição em linguagem natural (Apêndice A).
- **FR-002**: O sistema DEVE mostrar `origin` e `target` lado a lado com destaque palavra a palavra (só `target` em `ADD`, só `origin` em `DEL`), mantendo quebras de linha e empilhando os blocos em telas estreitas.
- **FR-003**: O sistema DEVE oferecer os dois botões de caso de uso completo, rotulados com as versões do item, abrindo modais conforme a User Story 2.
- **FR-004**: As perguntas DEVEM trazer a explicação de cada opção (Apêndices B.1 e B.2), com escolha única; avançar só fica disponível com as duas respondidas.
- **FR-005**: A tela NÃO DEVE mudar o que é gravado em relação à spec 007.
- **FR-006**: A tela DEVE ser utilizável por teclado e legível em celular.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: O modal do caso de uso abre em menos de 1 segundo.
- **SC-002**: Um bloco de 10 itens é concluído, em média, em menos de 20 minutos no piloto.
- **SC-003**: No piloto, ao menos 80 % dos avaliadores dizem que entenderam a edição sem ajuda externa.

## Assumptions

- Os textos dos Apêndices B.1 e B.2 são fixos no sistema; mudá-los exige nova versão publicada.
- O destaque de diferenças é calculado na exibição, a partir de `origin` e `target`; nada novo é gravado.
- O piloto citado nos critérios de sucesso é o ensaio com poucos avaliadores previsto no [ROADMAP](../ROADMAP.md), antes da divulgação.
