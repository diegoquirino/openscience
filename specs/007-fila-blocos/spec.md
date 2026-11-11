# Feature Specification: Receber, responder e enviar blocos sem colisão

**Feature Branch**: `007-fila-blocos`

**Created**: 2026-11-10

**Status**: Draft

**Input**: User description: "O participante elegível recebe o próximo bloco-papel livre da fila (10 itens, ou menos no último bloco do lote), responde cada item como rascunho, revisa e envia o bloco inteiro. Dois pedidos simultâneos nunca recebem o mesmo bloco-papel, e a mesma pessoa nunca recebe os dois papéis de um bloco. Ao terminar, ela sabe se há mais blocos para ela."

**Spec geral**: [001-rotulagem-oraculo](../001-rotulagem-oraculo/spec.md), User Stories 3 e 5, FR-013, FR-020 a FR-027, FR-034 e FR-040 a FR-042 · **Depende de**: 004 e 005 · **Liga-se a**: 006 (só elegíveis entram) e 008 (tela rica do item)

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Receber o próximo bloco da fila (Priority: P1)

O participante pede um bloco e recebe o primeiro bloco-papel livre que pode avaliar, reservado só para ele.

**Why this priority**: É o núcleo do estudo e a parte de maior risco técnico (concorrência); por isso vem antes da tela rica do item.

**Independent Test**: Com 38 itens importados (8 tarefas), pedir blocos com várias pessoas em sequência e em paralelo e conferir a ordem e a exclusividade.

**Acceptance Scenarios**:

1. **Dado** a fila com B1-P1 livre, **Quando** o primeiro participante pede um bloco, **Então** recebe B1-P1, com os itens na ordem de importação.
2. **Dado** B1-P1 reservado para A, **Quando** B pede um bloco, **Então** recebe B1-P2.
3. **Dado** os dois papéis de B1 reservados ou concluídos, **Quando** C pede um bloco, **Então** recebe B2-P1.
4. **Dado** que A concluiu B1-P1 e B1-P2 está livre, **Quando** A pede outro bloco, **Então** nunca recebe B1-P2; recebe o próximo bloco-papel livre que pode avaliar.
5. **Dado** A com um bloco reservado, **Quando** pede outro, **Então** recebe o mesmo bloco, com os rascunhos já salvos.
6. **Dado** duas pessoas pedindo ao mesmo tempo e um único bloco-papel livre, **Quando** os pedidos chegam, **Então** só uma o recebe e a outra recebe a mensagem adequada da User Story 4.
7. **Dado** a base sem nenhum item, **Quando** um participante pede um bloco, **Então** vê "Ainda não há itens para classificar".

---

### User Story 2 - Responder os itens como rascunho (Priority: P1)

Para cada item, o participante escolhe o impacto e a operação, pode comentar e avança. Cada resposta fica salva como rascunho no servidor.

**Why this priority**: Sem salvar respostas não há oráculo; salvar como rascunho protege contra queda de conexão.

**Independent Test**: Responder parte de um bloco, recarregar a página, continuar; salvar a mesma resposta duas vezes.

**Acceptance Scenarios**:

1. **Dado** um item na tela, **Quando** exibido nesta versão simples, **Então** mostra sistema, caso de uso, `kind`, `field`, `origin` e `target` com as quebras de linha, a pergunta de impacto (Baixo, Alto), a de operação (Manter, Atualizar, Remover, Criar, Unir, Dividir, Revincular, Sinalizar) e um campo de comentário.
2. **Dado** só uma das perguntas respondida, **Quando** a pessoa tenta avançar, **Então** o botão continua desabilitado.
3. **Dado** as duas respondidas, **Quando** a pessoa avança, **Então** o rascunho é salvo com os códigos em inglês (`LOW`/`HIGH`, `KEEP`, `UPDATE`, …) e o comentário (até 1.000 caracteres), o contador mostra "x de 10" (ou "x de n") e o próximo item aparece.
4. **Dado** um bloco em andamento, **Quando** a pessoa recarrega a página ou volta antes de a reserva vencer, **Então** retoma o mesmo bloco, no primeiro item sem resposta, com os rascunhos preservados.
5. **Dado** um item já respondido, **Quando** a pessoa volta a ele e muda a resposta, **Então** o rascunho é substituído.
6. **Dado** a mesma resposta salva duas vezes (clique duplo ou rede lenta), **Quando** processada, **Então** existe um único rascunho para o item.
7. **Dado** um bloco em que outra pessoa tem o outro papel, **Quando** o item é exibido, **Então** nada da resposta nem da identidade dela aparece.

---

### User Story 3 - Revisar e enviar o bloco inteiro (Priority: P1)

Com todos os itens respondidos, o participante revisa as escolhas, pode alterar qualquer uma e envia o bloco. Só então as respostas passam a valer.

**Why this priority**: A regra do estudo é que só blocos completos contam.

**Independent Test**: Tentar enviar com item pendente; enviar completo; tentar alterar depois do envio.

**Acceptance Scenarios**:

1. **Dado** todos os itens respondidos, **Quando** a pessoa chega ao fim, **Então** vê a revisão com as duas escolhas e o comentário de cada item, e pode abrir qualquer item para alterar.
2. **Dado** a revisão, **Quando** a pessoa envia, **Então** todas as respostas viram finais de uma vez, a tarefa fica concluída com data de início e de envio, e as respostas não podem mais ser alteradas.
3. **Dado** um item sem resposta, **Quando** a pessoa tenta enviar, **Então** o envio é recusado e o sistema leva ao primeiro item pendente.
4. **Dado** um bloco já enviado, **Quando** o mesmo envio chega de novo, **Então** o sistema responde com sucesso sem alterar nada.

---

### User Story 4 - Saber o que fazer depois (Priority: P2)

Ao enviar um bloco ou pedir um novo, o participante sabe se há mais blocos para ele ou se a base acabou.

**Why this priority**: Fecha o ciclo sem deixar a pessoa sem saber se ainda há trabalho.

**Independent Test**: Esgotar uma base de 2 blocos com 3 pessoas e conferir as três mensagens.

**Acceptance Scenarios**:

1. **Dado** um bloco enviado e outro bloco-papel disponível para a pessoa, **Quando** a conclusão aparece, **Então** o sistema agradece e oferece "Avaliar mais um bloco" ou "Sair".
2. **Dado** todas as tarefas concluídas, **Quando** a pessoa envia o último bloco ou pede um novo, **Então** o sistema agradece e informa que a base foi completamente respondida.
3. **Dado** tarefas não concluídas, mas nenhuma disponível para a pessoa, **Quando** ela pede um bloco, **Então** o sistema agradece e informa que não há mais blocos para ela no momento.

---

### User Story 5 - Liberar blocos abandonados (Priority: P1)

Um bloco sem atividade por 30 minutos volta à fila na sua posição original.

**Why this priority**: Sem isso, quem abandona um bloco trava a coleta.

**Independent Test**: Reservar um bloco, deixar a reserva vencer (com tempo de reserva reduzido no ensaio) e pedir com outra pessoa.

**Acceptance Scenarios**:

1. **Dado** um bloco reservado para A, **Quando** A fica 30 minutos sem salvar nenhuma resposta, **Então** a reserva vence e o bloco-papel volta a ser o primeiro livre na sua posição da fila.
2. **Dado** que a reserva de A venceu e B recebeu o bloco-papel, **Quando** B o recebe, **Então** os rascunhos de A são descartados e B começa do primeiro item.
3. **Dado** o caso anterior, **Quando** A tenta salvar ou enviar, **Então** o sistema recusa, explica que o tempo do bloco acabou e oferece um novo bloco.
4. **Dado** que a reserva de A venceu e ninguém pegou o bloco-papel, **Quando** A salva uma resposta, **Então** a reserva é renovada para A e os rascunhos continuam valendo.
5. **Dado** uma resposta salva, **Quando** gravada, **Então** a reserva é renovada por mais 30 minutos.

---

### Edge Cases

- Duas abas da mesma pessoa: mostram o mesmo bloco e os mesmos rascunhos; ela nunca segura dois blocos.
- Último bloco-papel disputado por várias pessoas: só uma recebe.
- Bloco final com menos de 10 itens: contador e revisão usam o tamanho real.
- Participante que deixa de ser elegível (termo novo não aceito) com bloco reservado: precisa aceitar o termo antes de continuar; a reserva segue as regras normais.
- Rede cai ao enviar: ao reconectar, a pessoa vê o bloco como enviado ou ainda em revisão, nunca meio enviado.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: O participante DEVE receber o primeiro bloco-papel livre da fila que possa avaliar, um de cada vez; com um bloco reservado, recebe o mesmo.
- **FR-002**: O sistema NUNCA DEVE entregar à mesma pessoa os dois papéis de um bloco.
- **FR-003**: A reserva DEVE ser exclusiva, vencer após o tempo configurado (30 minutos) sem resposta salva e ser renovada a cada resposta salva.
- **FR-004**: Pedir bloco, salvar rascunho e enviar bloco DEVEM ser atômicos; rascunho e envio só são aceitos de quem detém a reserva ou de reserva vencida que ninguém pegou, o que a renova.
- **FR-005**: Ao entregar um bloco-papel de reserva vencida a outra pessoa, os rascunhos da anterior DEVEM ser descartados.
- **FR-006**: As respostas DEVEM ser rascunhos editáveis até o envio; o envio exige todos os itens respondidos e torna todas finais de uma vez; respostas finais não podem ser alteradas pelo participante.
- **FR-007**: Salvar ou enviar mais de uma vez NÃO DEVE duplicar registros.
- **FR-008**: Cada resposta DEVE guardar `change_impact`, `maintenance_operation`, comentário opcional (até 1.000 caracteres), participante, bloco, papel e data/hora; a tarefa concluída guarda início e envio.
- **FR-009**: A tela DEVE mostrar o progresso do bloco ("x de 10" ou "x de n").
- **FR-010**: O sistema NUNCA DEVE mostrar a um avaliador as respostas ou a identidade do outro avaliador do bloco.
- **FR-011**: Ao concluir ou pedir um bloco, o sistema DEVE mostrar a mensagem de fim adequada: há mais blocos, base completa ou nada disponível para a pessoa.
- **FR-012**: Só participantes elegíveis, com o termo vigente aceito, DEVEM pedir, salvar ou enviar blocos.

### Key Entities

- **Tarefa (bloco-papel)**: livre, reservada (dono, início, vencimento) ou concluída (dono, início, envio).
- **Resposta**: escolha de um participante para um item num papel; rascunho ou final.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Em teste com 30 participantes pedindo, salvando e enviando ao mesmo tempo, nenhuma tarefa é entregue a duas pessoas e ninguém ocupa os dois papéis de um bloco.
- **SC-002**: Ao esgotar uma base de teste, todo item tem exatamente duas respostas finais, de pessoas diferentes.
- **SC-003**: O próximo item aparece em menos de 3 segundos em 95 % das vezes.
- **SC-004**: Nenhum rascunho é perdido ao recarregar a página dentro do prazo da reserva.

## Assumptions

- A regra de escolha do próximo bloco-papel é testada isoladamente com cenários de fila (spec 003, FR-008) antes de ir ao ar.
- A tela do item desta spec é propositalmente simples; a spec 008 a substitui sem mudar o que é gravado.
- No ensaio, o tempo de reserva pode ser reduzido para testar o vencimento.
