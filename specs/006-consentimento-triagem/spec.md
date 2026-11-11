# Feature Specification: Termo de consentimento, habilidade profissional e triagem

**Feature Branch**: `006-consentimento-triagem`

**Created**: 2026-11-10

**Status**: Draft

**Input**: User description: "Depois de entrar, o participante aceita o termo da pesquisa (TCLE/LGPD), escolhe sua habilidade profissional numa lista e responde à pergunta de triagem, que o admite ou o exclui do estudo."

**Spec geral**: [001-rotulagem-oraculo](../001-rotulagem-oraculo/spec.md), User Story 2, FR-006 a FR-010, Apêndices B.3 e B.4 · **Depende de**: 004 · **Pode correr em paralelo com**: 005 e 008

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Aceitar ou recusar o termo (Priority: P1)

Na primeira entrada, o participante lê o termo vigente e escolhe aceitar ou não participar.

**Why this priority**: Exigência ética e da LGPD: nenhuma classificação pode ser registrada sem consentimento.

**Independent Test**: Entrar com um e-mail novo, recusar; entrar com outro, aceitar; publicar nova versão do termo e entrar de novo.

**Acceptance Scenarios**:

1. **Dado** um participante que nunca aceitou o termo vigente, **Quando** entra, **Então** vê o texto completo do termo, com rolagem, e as opções "Li e aceito participar" e "Não aceito".
2. **Dado** o termo, **Quando** a pessoa aceita, **Então** o sistema registra data, hora e versão do aceite e segue para a habilidade profissional.
3. **Dado** o termo, **Quando** a pessoa recusa, **Então** o sistema registra a recusa (data, hora, versão), agradece e encerra o acesso.
4. **Dado** um participante que recusou, **Quando** volta a entrar, **Então** vê o termo de novo e pode mudar de ideia.
5. **Dado** uma nova versão do termo, **Quando** um participante que aceitou a anterior entra, **Então** precisa aceitar a nova antes de continuar; habilidade e triagem não são pedidas de novo.
6. **Dado** nenhum termo definido na configuração, **Quando** um participante entra, **Então** vê "O estudo ainda não está aberto" e não avança.

---

### User Story 2 - Informar a habilidade profissional (Priority: P1)

O participante escolhe uma única opção da lista do Apêndice B.4.

**Why this priority**: Caracteriza a amostra de avaliadores no artigo.

**Independent Test**: Tentar avançar sem escolher; escolher "Outro" sem descrição; escolher uma opção comum.

**Acceptance Scenarios**:

1. **Dado** a tela de habilidade, **Quando** nenhuma opção está marcada, **Então** o botão de avançar fica desabilitado.
2. **Dado** "Outro", **Quando** a descrição está vazia ou passa de 100 caracteres, **Então** não é possível avançar.
3. **Dado** uma opção escolhida, **Quando** a pessoa avança, **Então** o sistema grava o código da opção (por exemplo, `analista_testes`) e, em "Outro", a descrição.

---

### User Story 3 - Responder à triagem (Priority: P1)

O participante vê um exemplo curto de caso de uso e de caso de teste e responde se já trabalhou com algo assim.

**Why this priority**: Garante avaliadores com a experiência que o estudo exige.

**Independent Test**: Responder cada uma das três opções com e-mails diferentes e entrar de novo com cada um.

**Acceptance Scenarios**:

1. **Dado** a triagem, **Quando** é exibida, **Então** mostra o exemplo de caso de uso com fluxo básico e um fluxo de exceção, um caso de teste derivado dele e a pergunta do Apêndice B.3 com três opções.
2. **Dado** a triagem, **Quando** a pessoa responde "Sim, na prática profissional" ou "Sim, em estudos, cursos ou pesquisa", **Então** é marcada como elegível e segue para a participação.
3. **Dado** a triagem, **Quando** a pessoa responde "Não", **Então** o sistema registra a resposta, agradece, explica que o estudo exige essa experiência e encerra a participação.
4. **Dado** um participante inelegível, **Quando** volta a entrar, **Então** vê a mesma mensagem de encerramento e não pode classificar nem responder a triagem de novo.
5. **Dado** um participante elegível com termo vigente aceito, **Quando** volta a entrar, **Então** vai direto para a participação, sem repetir nenhuma etapa.

---

### Edge Cases

- Pessoa que fecha o navegador no meio das etapas: ao voltar, retoma na primeira etapa pendente.
- Voltar com o botão do navegador depois da triagem: não permite mudar a resposta.
- Termo muito longo: rola dentro da página e os botões ficam sempre visíveis.
- Administrador: nunca vê estas telas.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: O sistema DEVE exibir o termo vigente e registrar aceite ou recusa com data, hora e versão; a recusa encerra o acesso.
- **FR-002**: Uma nova versão do termo DEVE exigir novo aceite antes de qualquer classificação.
- **FR-003**: O participante DEVE escolher uma única habilidade da lista do Apêndice B.4; "Outro" exige descrição de até 100 caracteres.
- **FR-004**: O participante DEVE responder à triagem do Apêndice B.3; "Não" o torna inelegível de forma permanente para aquele e-mail.
- **FR-005**: Habilidade e triagem DEVEM ser pedidas uma única vez por participante.
- **FR-006**: Ao entrar, o participante DEVE ser levado à primeira etapa pendente, na ordem termo, habilidade, triagem, participação.
- **FR-007**: Só participantes elegíveis e com o termo vigente aceito DEVEM chegar à participação; esta regra DEVE valer também para chamadas diretas às funções de participação.
- **FR-008**: Texto e versão do termo DEVEM vir da configuração do sistema.

### Key Entities

- **Participante** (acréscimos): versão e data do aceite ou da recusa, habilidade, descrição de "Outro", resposta da triagem, elegibilidade.
- **Termo de consentimento**: texto e versão vigentes.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Um participante novo percorre termo, habilidade e triagem em menos de 3 minutos, sem contar a leitura do termo.
- **SC-002**: Em teste com cada combinação de respostas, nenhum participante sem aceite vigente ou inelegível consegue chegar à participação, nem por chamada direta.
- **SC-003**: Todo participante elegível tem versão e data de aceite registradas.

## Assumptions

- O texto do termo (TCLE) vem do pesquisador, conforme a aprovação do comitê de ética, e é posto na configuração pelo próprio pesquisador; não há tela de edição do termo.
- O exemplo da triagem é fixo e escrito pelo pesquisador.
- Até a spec 007 ser entregue, o participante elegível vê uma tela "Você está apto; os blocos serão liberados em breve".
