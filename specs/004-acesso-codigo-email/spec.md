# Feature Specification: Entrar com nome, e-mail e código

**Feature Branch**: `004-acesso-codigo-email`

**Created**: 2026-11-10

**Status**: Draft

**Input**: User description: "Participantes e administrador entram informando nome e e-mail e digitando um código de 6 dígitos enviado por e-mail, sem convite nem senha. O administrador (diegoquirino@gmail.com) vai para a área administrativa; os demais seguem para o fluxo de participação."

**Spec geral**: [001-rotulagem-oraculo](../001-rotulagem-oraculo/spec.md), User Stories 1 e 6, FR-001 a FR-005 e FR-050 · **Depende de**: 003

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Entrar com o código enviado por e-mail (Priority: P1)

Um voluntário informa nome e e-mail, recebe um código de 6 dígitos e o digita para entrar.

**Why this priority**: Toda a participação e a área administrativa dependem de saber quem é a pessoa; é o gargalo de todas as specs seguintes.

**Independent Test**: Entrar com um e-mail novo; tentar código errado, vencido e reutilizado.

**Acceptance Scenarios**:

1. **Dado** a tela "Participar", **Quando** a pessoa informa nome (2 a 120 caracteres) e um e-mail válido, **Então** o sistema envia um código de 6 dígitos, informa que ele vale por 10 minutos e avisa para conferir a pasta de spam.
2. **Dado** um nome ou e-mail inválido, **Quando** a pessoa tenta continuar, **Então** o sistema aponta o campo com problema e não envia código.
3. **Dado** um código enviado há menos de 10 minutos, **Quando** a pessoa digita o código correto, **Então** entra no sistema.
4. **Dado** um código válido, **Quando** a pessoa erra 5 vezes, **Então** o código é invalidado e ela precisa pedir outro.
5. **Dado** um código vencido ou já usado, **Quando** a pessoa o digita, **Então** o sistema recusa e oferece o envio de um novo código.
6. **Dado** um código enviado há menos de 60 segundos, **Quando** a pessoa pede outro, **Então** o sistema informa quantos segundos faltam.
7. **Dado** 5 códigos pedidos para o mesmo e-mail na última hora, **Quando** a pessoa pede o sexto, **Então** o sistema recusa e informa quando poderá pedir de novo.
8. **Dado** `Ana@X.com ` e `ana@x.com`, **Quando** são informados, **Então** o sistema os trata como o mesmo participante.
9. **Dado** o limite diário de envio de e-mails atingido, **Quando** alguém pede um código, **Então** o sistema informa que não pode enviar códigos hoje e sugere tentar no dia seguinte.
10. **Dado** um e-mail que entra pela primeira vez, **Quando** o código é validado, **Então** o participante é registrado com um identificador pseudônimo sequencial (`R-0001`, `R-0002`, …).
11. **Dado** um e-mail já registrado com outro nome, **Quando** o código é validado, **Então** o sistema guarda o nome mais recente.

---

### User Story 2 - Seguir para a área certa depois de entrar (Priority: P1)

O administrador vai para a área administrativa; os demais seguem para o fluxo de participação. Ninguém mais acessa a área administrativa.

**Why this priority**: Separa os dois públicos desde o início e protege as funções que as specs 005, 009, 010 e 011 vão acrescentar.

**Independent Test**: Entrar com `diegoquirino@gmail.com` e com outro e-mail; chamar uma função administrativa sem sessão de administrador.

**Acceptance Scenarios**:

1. **Dado** o e-mail `diegoquirino@gmail.com`, **Quando** o código é validado, **Então** abre a área administrativa, com o nome do administrador e o menu (Importar, Exportar, Painel, Participantes), cujos itens ainda não entregues aparecem desabilitados.
2. **Dado** qualquer outro e-mail, **Quando** o código é validado, **Então** a pessoa segue para a próxima etapa da participação (provisoriamente, uma tela "Obrigado por se identificar; a próxima etapa será liberada em breve").
3. **Dado** uma pessoa que não é o administrador, **Quando** tenta abrir uma tela ou chamar uma função administrativa, **Então** o acesso é negado e a tentativa é registrada.
4. **Dado** o administrador, **Quando** entra, **Então** não recebe identificador pseudônimo nem pode entrar no fluxo de classificação.

---

### User Story 3 - Continuar conectado e sair (Priority: P2)

Quem entrou continua conectado no mesmo navegador por até 24 horas e pode sair a qualquer momento.

**Why this priority**: Economiza envios de e-mail, que têm limite diário, sem obrigar um código a cada visita.

**Independent Test**: Entrar, fechar e reabrir o navegador; sair e tentar voltar.

**Acceptance Scenarios**:

1. **Dado** uma pessoa que entrou há menos de 24 horas, **Quando** reabre o Veredito no mesmo navegador, **Então** continua conectada, sem novo código.
2. **Dado** uma sessão com mais de 24 horas, **Quando** a pessoa faz qualquer ação, **Então** volta para a tela de identificação.
3. **Dado** uma pessoa conectada, **Quando** clica em "Sair", **Então** a sessão é encerrada e ela volta à página inicial.

---

### Edge Cases

- Código pedido em dois dispositivos para o mesmo e-mail: vale só o mais recente.
- E-mail que não chega: a tela de código oferece "Reenviar" (respeitando os limites) e "Corrigir e-mail".
- Código digitado com espaços ou hífen: os espaços e hífens são ignorados.
- Erro no envio do e-mail: a pessoa vê "Não foi possível enviar o código agora" e nenhum código fica válido.
- Mensagens de código errado não revelam se o e-mail já está registrado.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: O sistema DEVE identificar a pessoa por nome (2 a 120 caracteres) e e-mail válido, normalizado em minúsculas e sem espaços.
- **FR-002**: O sistema DEVE validar a entrada com um código de 6 dígitos enviado por e-mail, válido por 10 minutos, de uso único, invalidado após 5 tentativas erradas, com reenvio após 60 segundos e no máximo 5 códigos por e-mail por hora.
- **FR-003**: Códigos e identificadores de sessão DEVEM ser guardados só de forma não reversível.
- **FR-004**: A sessão DEVE durar no máximo 24 horas ou até a pessoa sair.
- **FR-005**: O sistema DEVE guardar o nome mais recente informado para um e-mail.
- **FR-006**: Cada participante novo DEVE receber um identificador pseudônimo sequencial e único.
- **FR-007**: Somente o e-mail de administrador da configuração DEVE acessar a área administrativa; o administrador não participa da classificação.
- **FR-008**: Toda função, exceto pedir e validar código, DEVE exigir sessão válida, e as administrativas, sessão de administrador.
- **FR-009**: O sistema DEVE contar os e-mails enviados no dia, parar no limite diário da configuração e informar quando ele for atingido.

### Key Entities

- **Participante**: pessoa identificada pelo e-mail; nome, identificador pseudônimo, data do registro.
- **Código de acesso**: ligado a um e-mail; validade, tentativas, uso.
- **Sessão de acesso**: ligada a um participante ou ao administrador; validade.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Uma pessoa nova vai da tela "Participar" à tela seguinte ao login em menos de 3 minutos.
- **SC-002**: Em 95 % dos envios, o código chega em menos de 1 minuto.
- **SC-003**: Em teste com todas as funções administrativas chamadas por uma sessão comum e sem sessão, 100 % das chamadas são negadas.
- **SC-004**: Uma pessoa que volta no mesmo dia não gasta novo envio de e-mail.

## Assumptions

- O e-mail do código sai da conta do pesquisador e pode cair no spam.
- Na produção, o e-mail sai da conta Gmail do pesquisador, que aceita cerca de 500 destinatários por dia; o limite padrão do sistema é 450, deixando folga (ver research.md da spec 001).
- No ensaio local (Docker), os códigos vão para uma caixa de e-mail de teste local e não chegam a ninguém; o limite diário pode ser reduzido para testar a mensagem de cota esgotada.
- O termo, o perfil e a triagem (spec 006) entram entre o login e a participação; até lá, a tela provisória ocupa esse lugar.
