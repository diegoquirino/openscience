# Feature Specification: Veredito, rotulagem cega e concorrente do oráculo

**Feature Branch**: `001-rotulagem-oraculo`

**Created**: 2026-11-10

**Status**: Visão geral; implementada pelas specs 002 a 011, na ordem do [ROADMAP](../ROADMAP.md)

**Input**: User description: "Web-app em que voluntários se identificam por nome e e-mail, validam a participação com um código enviado por e-mail, aceitam o termo da pesquisa, informam a habilidade profissional, passam por uma pergunta de triagem e classificam o impacto da mudança e a operação de manutenção de edições de casos de uso. Cada item recebe dois avaliadores diferentes. Só o administrador (Carlos Diego Quirino Lima, diegoquirino@gmail.com) importa itens por CSV (`sistema, edit_key, uc, kind, field, origin, target`) e acessa a área administrativa. O sistema precisa resistir a acessos simultâneos."

## Clarifications

### Session 2026-11-10

- Q: Como o rotulador vê o contexto do caso de uso? → A: Um botão junto do "Antes" abre um modal com o caso de uso completo na versão anterior (ex.: v1.1); outro junto do "Depois" abre o caso de uso completo na versão modificada (ex.: v1.2).
- Q: Quantos itens por sessão? → A: Blocos fixos de 10 itens consecutivos; o último bloco pode ter menos. A fila é preenchida de cima para baixo, alternando papéis: bloco 1 rotulador 1, bloco 1 rotulador 2, bloco 2 rotulador 1, e assim por diante. As respostas de um bloco só valem quando o rotulador conclui todos os itens dele.
- Q: Qual a primeira coluna do CSV? → A: `sistema`; cada item é identificado por (`sistema`, `edit_key`).

### Session 2026-11-11

- Q: Quanto tempo dura a reserva de um bloco? → A: 20 minutos sem nenhuma resposta salva (valor da configuração); cada resposta salva renova o prazo.
- Q: O voluntário vê quanto tempo ainda tem? → A: Sim. A tela de cada item mostra um contador regressivo em minutos e segundos até o vencimento da reserva, que volta ao valor cheio quando a pessoa salva a resposta e passa ao próximo item.
- Q: O que acontece com os rascunhos quando a reserva vence? → A: Se ninguém pegou o bloco, a pessoa retoma ao salvar uma resposta, com os rascunhos; se outra pessoa o recebeu, os rascunhos da primeira são descartados.
- Q: Onde o bloco abandonado volta na fila? → A: Na sua posição original, à frente dos blocos seguintes.
- Q: Quantos blocos uma pessoa pode ter reservados ao mesmo tempo? → A: Um.
- Q: Há limite de blocos por pessoa na coleta? → A: Não. O painel mostra quantos blocos cada pseudônimo fez, e o viés de rotulador é discutido no artigo.
- Q: O rotulador 2 pode começar antes de o rotulador 1 terminar? → A: Sim, os dois rotulam o mesmo bloco em paralelo, sem ver nada um do outro.
- Q: Blocos de quem pede retirada (LGPD) com respostas descartadas voltam para onde? → A: Para a mesma posição da fila.
- Q: Que vocabulário usar? → A: Ver a seção *Vocabulário*.

## Vocabulário

| Onde | Termo |
|---|---|
| Nome do sistema | Veredito |
| Atividade, em títulos, no termo, nos convites, nas explicações e nesta documentação | **rotulagem dos dados** (verbo: rotular; resultado: rótulos) |
| Botões e mensagens curtas das telas | "avaliar": "Avaliar mais um bloco", "Ainda não há itens para avaliar" |
| Quem rotula, no termo, na LGPD e nos documentos formais | participante (da pesquisa) |
| Quem rotula, nos convites e nas telas | voluntário |
| Posição no bloco | rotulador 1 e rotulador 2 (papel 1 e papel 2 no modelo de dados; `rater1` e `rater2` no pipeline) |

Os nomes de colunas, códigos e arquivos do pipeline (`change_impact`, `maintenance_operation`, `t1_<sistema>_rater1.csv`) não mudam.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Identificar-se com nome, e-mail e código (Priority: P1)

Um voluntário abre o Veredito, informa nome e e-mail, recebe um código de 6 dígitos no e-mail e o digita para entrar. Não há convite nem senha.

**Why this priority**: Sem identificação confiável não é possível garantir que rotulador 1 e rotulador 2 de um bloco sejam pessoas diferentes.

**Independent Test**: Informar nome e e-mail, receber o código e entrar; tentar código errado, vencido e reutilizado.

**Acceptance Scenarios**:

1. **Dado** um e-mail nunca usado, **Quando** a pessoa informa nome e e-mail válidos, **Então** o sistema envia um código de 6 dígitos para esse e-mail, informa que ele vale por 10 minutos e avisa para conferir a pasta de spam.
2. **Dado** um código enviado há menos de 10 minutos, **Quando** a pessoa digita o código correto, **Então** entra no sistema e segue para a próxima etapa pendente (termo, perfil, triagem, bloco em andamento ou novo bloco).
3. **Dado** um código válido, **Quando** a pessoa erra o código 5 vezes, **Então** o código é invalidado e ela precisa pedir outro.
4. **Dado** um código vencido ou já usado, **Quando** a pessoa o digita, **Então** o sistema recusa e oferece o envio de um novo código.
5. **Dado** que um código foi enviado há menos de 60 segundos, **Quando** a pessoa pede outro, **Então** o sistema informa quantos segundos faltam para poder reenviar.
6. **Dado** um e-mail digitado com maiúsculas ou espaços, **Quando** a pessoa o informa, **Então** o sistema trata `Ana@X.com ` e `ana@x.com` como a mesma pessoa.
7. **Dado** que a cota diária de e-mails acabou, **Quando** alguém pede um código, **Então** o sistema informa que não pode enviar códigos hoje e sugere tentar no dia seguinte.

---

### User Story 2 - Aceitar o termo, informar a habilidade profissional e passar pela triagem (Priority: P1)

Na primeira entrada, a pessoa lê o termo de consentimento (TCLE/LGPD) e o aceita, escolhe sua habilidade profissional e responde à pergunta de triagem. Quem responde "Não" à triagem é agradecido e sai do estudo.

**Why this priority**: Requisito ético e de LGPD para coletar dados, e o filtro que garante rotuladores qualificados.

**Independent Test**: Entrar com um e-mail novo e percorrer as três etapas com cada resposta possível, conferindo o que fica registrado.

**Acceptance Scenarios**:

1. **Dado** um participante que nunca aceitou o termo vigente, **Quando** entra, **Então** vê o texto completo do termo com as opções "Li e aceito participar" e "Não aceito".
2. **Dado** o termo exibido, **Quando** a pessoa escolhe "Li e aceito participar", **Então** o sistema registra data, hora e versão do termo e segue para a habilidade profissional.
3. **Dado** o termo exibido, **Quando** a pessoa escolhe "Não aceito", **Então** o sistema agradece, encerra o acesso e não registra nenhum rótulo dela.
4. **Dado** a tela de habilidade profissional, **Quando** a pessoa não escolhe nenhuma opção, **Então** não consegue avançar.
5. **Dado** a opção "Outro" escolhida, **Quando** a pessoa deixa a descrição em branco, **Então** não consegue avançar.
6. **Dado** a pergunta de triagem (Apêndice B.3), **Quando** a pessoa responde uma das duas opções "Sim", **Então** é considerada elegível e segue para o primeiro bloco.
7. **Dado** a pergunta de triagem, **Quando** a pessoa responde "Não", **Então** o sistema registra a resposta, agradece, explica que o estudo exige essa experiência e encerra a participação.
8. **Dado** um participante já marcado como inelegível, **Quando** volta a entrar com o mesmo e-mail, **Então** vê a mesma mensagem de encerramento e não pode participar da rotulagem dos dados.
9. **Dado** um participante que já aceitou o termo vigente e respondeu habilidade e triagem, **Quando** volta a entrar, **Então** vai direto para o bloco em andamento ou para um novo bloco.
10. **Dado** que o administrador publicou uma nova versão do termo, **Quando** um participante antigo entra, **Então** precisa aceitar a nova versão antes de continuar a rotulagem dos dados.

---

### User Story 3 - Receber e concluir um bloco fixo de 10 itens, sem colisão com outras pessoas (Priority: P1)

O participante elegível recebe o próximo bloco disponível da fila: 10 itens consecutivos da base num papel (rotulador 1 ou 2); o último bloco de um lote pode ter menos. A fila anda de cima para baixo, alternando os papéis: bloco 1 papel 1, bloco 1 papel 2, bloco 2 papel 1, bloco 2 papel 2… As respostas ficam como rascunho até a pessoa concluir todos os itens do bloco e enviá-lo; só então passam a valer.

**Why this priority**: É o núcleo do estudo: produzir dois rótulos independentes e completos por item.

**Independent Test**: Importar 38 itens (blocos de 10, 10, 10 e 8), abrir sessões em paralelo com várias pessoas e conferir que cada item termina com exatamente duas respostas finais de pessoas diferentes.

**Acceptance Scenarios**:

1. **Dado** a fila com o bloco 1 papel 1 livre, **Quando** o primeiro participante elegível pede um bloco, **Então** recebe o bloco 1 no papel 1, com seus 10 itens na ordem de importação.
2. **Dado** o bloco 1 papel 1 reservado para A, **Quando** B pede um bloco, **Então** B recebe o bloco 1 no papel 2.
3. **Dado** os dois papéis do bloco 1 reservados ou concluídos, **Quando** C pede um bloco, **Então** C recebe o bloco 2 no papel 1.
4. **Dado** que A concluiu o bloco 1 no papel 1 e o papel 2 desse bloco está livre, **Quando** A pede outro bloco, **Então** A nunca recebe o bloco 1 no papel 2; recebe o próximo bloco livre que pode avaliar.
5. **Dado** uma base de 205 itens num único lote, **Quando** a fila é formada, **Então** há 20 blocos de 10 itens e um bloco final de 5, e quem recebe esse último bloco, em qualquer papel, avalia 5 itens.
6. **Dado** duas pessoas pedindo um bloco no mesmo instante, **Quando** resta um único bloco-papel livre, **Então** só uma delas o recebe, e a outra recebe outro bloco ou a mensagem de fim (User Story 5).
7. **Dado** um bloco em andamento, **Quando** a pessoa responde um item, **Então** a resposta é salva como rascunho no servidor, o contador mostra "x de 10" (ou "x de n" no bloco final) e o próximo item aparece.
8. **Dado** um bloco em andamento, **Quando** a pessoa recarrega a página ou volta a entrar antes de a reserva vencer, **Então** retoma o mesmo bloco com os rascunhos já respondidos.
9. **Dado** todos os itens do bloco respondidos, **Quando** a pessoa chega ao fim, **Então** vê uma tela de revisão com as escolhas de cada item, pode alterar qualquer uma e só então envia o bloco.
10. **Dado** a revisão, **Quando** a pessoa envia o bloco, **Então** todas as respostas passam a valer de uma vez, o bloco-papel fica concluído e as respostas não podem mais ser alteradas.
11. **Dado** um bloco com algum item sem resposta, **Quando** a pessoa tenta enviá-lo, **Então** o envio é recusado e o sistema leva ao primeiro item pendente.
12. **Dado** um bloco reservado para A, **Quando** A fica 20 minutos sem salvar nenhuma resposta, **Então** a reserva vence e o bloco-papel volta para a fila.
13. **Dado** que a reserva de A venceu e B pegou o mesmo bloco-papel, **Quando** A tenta salvar ou enviar, **Então** o sistema recusa, explica que o tempo do bloco acabou, descarta os rascunhos de A e oferece um novo bloco.
14. **Dado** que a reserva de A venceu e ninguém pegou o bloco-papel, **Quando** A volta e salva uma resposta, **Então** a reserva é renovada para A e os rascunhos dela continuam valendo.
15. **Dado** um item na tela, **Quando** a pessoa salva a mesma resposta duas vezes (clique duplo ou rede lenta), **Então** existe um único rascunho para aquele item.
16. **Dado** um bloco concluído, **Quando** a pessoa escolhe "Avaliar mais um bloco", **Então** recebe o próximo bloco-papel disponível para ela.
17. **Dado** um item na tela, **Quando** é exibido, **Então** um contador regressivo em minutos e segundos (por exemplo, "19:42") mostra quanto falta para a reserva vencer; ao salvar a resposta e passar ao próximo item, o contador volta a 20:00.
18. **Dado** o contador chegando a zero, **Quando** a reserva vence, **Então** a tela avisa que o tempo do bloco acabou e que a pessoa ainda pode continuar se ninguém tiver pegado o bloco; ao salvar, ela retoma (cenário 14) ou recebe a recusa (cenário 13).

---

### User Story 4 - Entender o que está sendo rotulado (Priority: P1)

A tela do item mostra a edição de um jeito confortável de ler (antes e depois, com as diferenças destacadas), dá acesso ao caso de uso completo em cada versão e traz as duas perguntas com explicação de cada opção. O participante escolhe uma opção em cada pergunta, pode deixar um comentário e segue.

**Why this priority**: A validade do oráculo depende de o rotulador entender a edição, o contexto em que ela ocorre e os conceitos de impacto e de operação.

**Independent Test**: Exibir um item de cada `kind` (MOD, ADD, DEL, META), abrir os dois modais e conferir leitura, explicações e gravação dos códigos.

**Acceptance Scenarios**:

1. **Dado** um item `MOD`, **Quando** é exibido, **Então** aparecem "Antes" (origin) e "Depois" (target) lado a lado, com palavras removidas riscadas e palavras incluídas destacadas.
2. **Dado** um item `ADD`, **Quando** é exibido, **Então** aparece só "Trecho incluído" com o target.
3. **Dado** um item `DEL`, **Quando** é exibido, **Então** aparece só "Trecho removido" com o origin.
4. **Dado** um item `META` de `requirements`, **Quando** é exibido, **Então** aparece como "Vínculo com requisitos: antes / depois".
5. **Dado** qualquer item, **Quando** é exibido, **Então** o cabeçalho mostra sistema, caso de uso e o local da edição em linguagem natural (Apêndice A), por exemplo "Passo de fluxo de exceção · Modificação".
6. **Dado** qualquer item, **Quando** é exibido, **Então** junto do "Antes" há o botão "Ver caso de uso completo (v1.1)" e junto do "Depois" o botão "Ver caso de uso completo (v1.2)", com os rótulos de versão vindos do item; num `ADD` o primeiro botão aparece no lugar do bloco "Antes", e num `DEL` o segundo aparece no lugar do bloco "Depois".
7. **Dado** o botão da versão anterior, **Quando** a pessoa clica nele, **Então** abre um modal intitulado "<caso de uso> · versão v1.1 (anterior)" com o texto completo dessa versão, em fonte monoespaçada e com a indentação preservada.
8. **Dado** o botão da versão modificada, **Quando** a pessoa clica nele, **Então** abre o modal "<caso de uso> · versão v1.2 (modificada)" com o texto completo dessa versão.
9. **Dado** um texto de caso de uso com linhas marcadas como editadas (`<- edited`), **Quando** o modal é aberto, **Então** essas linhas aparecem destacadas, o marcador não é exibido e o modal já abre rolado até a primeira delas.
10. **Dado** um modal aberto, **Quando** a pessoa clica em "Fechar", pressiona Esc ou clica fora dele, **Então** o modal fecha e as escolhas já feitas no item continuam lá.
11. **Dado** uma tela estreita (celular), **Quando** o item é exibido, **Então** "Antes" e "Depois" aparecem um abaixo do outro, sem rolagem horizontal, e o modal ocupa a tela inteira.
12. **Dado** um texto com várias linhas, **Quando** é exibido no item ou no modal, **Então** as quebras de linha são mantidas.
13. **Dado** a pergunta de impacto, **Quando** é exibida, **Então** mostra a definição e exemplos de cada opção (Apêndice B.1), com escolha única.
14. **Dado** a pergunta de operação, **Quando** é exibida, **Então** mostra as oito opções com explicação (Apêndice B.2), com escolha única, e a regra de precedência quando mais de uma se aplica.
15. **Dado** só uma das duas perguntas respondida, **Quando** a pessoa tenta avançar, **Então** o botão continua desabilitado.
16. **Dado** as duas perguntas respondidas, **Quando** a pessoa avança, **Então** o sistema grava os códigos em inglês (`LOW`/`HIGH` e `KEEP`, `UPDATE`, …) e o comentário opcional (até 1.000 caracteres).
17. **Dado** um bloco cujo papel 1 está em andamento ou concluído, **Quando** outra pessoa avalia o mesmo bloco no papel 2, **Então** não vê as respostas nem a identidade do rotulador 1.

---

### User Story 5 - Encerrar a participação (Priority: P2)

Depois de enviar um bloco, ou ao pedir um, a pessoa fica sabendo se ainda há blocos para ela ou se a base foi completamente respondida.

**Why this priority**: Fecha o ciclo da participação sem deixar a pessoa sem saber se ainda há trabalho.

**Independent Test**: Esgotar uma base pequena e conferir as três mensagens.

**Acceptance Scenarios**:

1. **Dado** um bloco enviado e ainda há blocos-papel disponíveis para a pessoa, **Quando** a tela de conclusão aparece, **Então** o sistema agradece e oferece "Avaliar mais um bloco" ou "Sair".
2. **Dado** que todos os blocos-papel estão concluídos, **Quando** a pessoa envia o último bloco ou pede um novo, **Então** o sistema agradece, informa que a rotulagem dos dados foi concluída (toda a base foi avaliada) e encerra a participação.
3. **Dado** que ainda há blocos-papel não concluídos, mas nenhum disponível para a pessoa (são papéis 2 de blocos em que ela foi rotuladora 1, ou estão reservados por outras pessoas), **Quando** ela pede um bloco, **Então** o sistema agradece e informa que não há mais blocos disponíveis para ela no momento.

---

### User Story 6 - Entrar na área administrativa (Priority: P1)

Carlos Diego Quirino Lima entra com `diegoquirino@gmail.com` e o código por e-mail e vai direto para a área administrativa. Ninguém mais tem acesso a ela.

**Why this priority**: Sem a área administrativa não há itens para rotular nem dados para exportar.

**Independent Test**: Entrar com o e-mail do administrador e com outro e-mail; chamar as funções administrativas sem estar autenticado como administrador.

**Acceptance Scenarios**:

1. **Dado** o e-mail `diegoquirino@gmail.com`, **Quando** o código é validado, **Então** o sistema abre a área administrativa, sem termo, triagem nem blocos de rotulagem.
2. **Dado** qualquer outro e-mail, **Quando** a pessoa tenta abrir uma tela ou função administrativa, **Então** o acesso é negado.
3. **Dado** o administrador, **Quando** tenta entrar no fluxo de rotulagem, **Então** não consegue: o administrador não rotula itens.

---

### User Story 7 - Importar itens por CSV (Priority: P1)

O administrador envia um CSV com as colunas do Apêndice D, vê uma prévia com o que entra, o que é ignorado e o que tem erro, e confirma. Na confirmação, os itens novos são divididos em blocos de 10 e entram no fim da fila. Novos sistemas podem ser adicionados a qualquer momento, inclusive com a coleta em andamento.

**Why this priority**: É a única forma de alimentar a base.

**Independent Test**: Importar o oráculo do Minitest com `sistema = minitest` e depois um arquivo com duplicatas e erros.

**Acceptance Scenarios**:

1. **Dado** um CSV UTF-8 com todas as colunas do Apêndice D, **Quando** o administrador o envia, **Então** vê a prévia: quantos itens serão incluídos, quantos ignorados e quantos com erro (cada um com o número da linha e o motivo), e quantos blocos serão criados.
2. **Dado** um CSV sem uma das colunas obrigatórias, **Quando** é enviado, **Então** a importação é recusada indicando a coluna ausente.
3. **Dado** campos entre aspas com vírgulas, aspas duplicadas ou quebras de linha (como `alt_flow_steps_block` ou o caso de uso completo), **Quando** o CSV é lido, **Então** o texto é preservado exatamente.
4. **Dado** uma linha cujo par (`sistema`, `edit_key`) já existe na base, **Quando** a prévia é gerada, **Então** a linha aparece como ignorada e o item existente não é alterado.
5. **Dado** o mesmo (`sistema`, `edit_key`) repetido no próprio arquivo, **Quando** a prévia é gerada, **Então** só a primeira ocorrência entra e as demais aparecem como erro.
6. **Dado** uma linha que viola alguma regra de FR-052, **Quando** a prévia é gerada, **Então** a linha aparece como erro com o motivo.
7. **Dado** a prévia com 38 itens válidos, **Quando** o administrador confirma, **Então** são criados 4 blocos (10, 10, 10 e 8 itens) na ordem do arquivo, cada um com os papéis 1 e 2 livres no fim da fila, e o lote fica registrado com data, hora e nome do arquivo.
8. **Dado** a prévia, **Quando** o administrador cancela, **Então** nada é gravado.
9. **Dado** uma coleta em andamento, **Quando** um novo lote é importado, **Então** os blocos existentes, inclusive um bloco final incompleto, não mudam, e os novos blocos entram depois deles na fila.

---

### User Story 8 - Exportar os rótulos (Priority: P1)

O administrador baixa os rótulos finais em três formatos: a tabela larga (uma linha por item, com os dois rotuladores), a tabela longa (uma linha por resposta) e os arquivos por sistema que o pacote `issta_tcm` lê. Rascunhos de blocos não concluídos nunca são exportados.

**Why this priority**: É o produto final do estudo.

**Independent Test**: Exportar com blocos concluídos, em andamento e livres e abrir os arquivos no pipeline.

**Acceptance Scenarios**:

1. **Dado** blocos concluídos, **Quando** o administrador exporta a tabela larga, **Então** recebe um CSV UTF-8 com o cabeçalho do Apêndice C, uma linha por item, na ordem da fila, com os campos de um papel ainda não concluído em branco.
2. **Dado** blocos concluídos, **Quando** exporta a tabela longa, **Então** recebe uma linha por resposta final com sistema, edit_key, bloco, papel, identificador pseudônimo do participante (`R-0001`), habilidade, códigos escolhidos, comentário e datas de início e envio do bloco, sem nome nem e-mail.
3. **Dado** um sistema, **Quando** o administrador exporta os arquivos do pipeline, **Então** recebe `t1_<sistema>_rater1.csv` e `t1_<sistema>_rater2.csv` com as colunas `edit_key,uc,kind,field,origin,target,change_impact,maintenance_operation,comment`, na ordem da fila.
4. **Dado** um bloco em andamento com rascunhos, **Quando** qualquer exportação é gerada, **Então** os rascunhos não aparecem.
5. **Dado** textos com vírgulas, aspas ou quebras de linha, **Quando** são exportados, **Então** o CSV os mantém corretamente entre aspas.
6. **Dado** um texto que começa com `=`, `+`, `-` ou `@`, **Quando** é exportado nas tabelas larga ou longa, **Então** não vira fórmula ao abrir no Excel ou no Sheets; nos arquivos do pipeline, o texto é mantido sem alteração.

---

### User Story 9 - Acompanhar o andamento (Priority: P2)

O painel mostra, por sistema e por lote, quantos itens e blocos existem e quantos blocos-papel estão livres, em andamento e concluídos, além de quantos participantes entraram, foram excluídos pela triagem ou recusaram o termo.

**Why this priority**: Ajuda a decidir quando divulgar mais o estudo ou encerrar a coleta, mas o estudo funciona sem ele.

**Independent Test**: Conferir os números do painel contra a exportação longa.

**Acceptance Scenarios**:

1. **Dado** blocos e respostas, **Quando** o administrador abre o painel, **Então** vê por sistema: itens, blocos e blocos-papel (papel 1 e papel 2) livres, em andamento e concluídos.
2. **Dado** blocos em andamento, **Quando** o administrador abre o painel, **Então** vê, para cada um, o pseudônimo de quem o reservou, quantos itens já têm rascunho e quando a reserva vence.
3. **Dado** participantes registrados, **Quando** abre o painel, **Então** vê totais de elegíveis, inelegíveis, que recusaram o termo e distribuição por habilidade profissional.
4. **Dado** o painel aberto, **Quando** o administrador o consulta, **Então** vê quantos e-mails ainda podem ser enviados hoje.

---

### User Story 10 - Atender um pedido de retirada de dados (Priority: P3)

A pedido de um participante (direito previsto na LGPD), o administrador remove o nome e o e-mail dele e decide se as respostas dele ficam (anonimizadas) ou são descartadas, devolvendo os blocos à fila.

**Why this priority**: Obrigação legal, mas rara; pode ser feita depois do lançamento.

**Independent Test**: Retirar um participante com blocos concluídos e conferir a exportação e a fila.

**Acceptance Scenarios**:

1. **Dado** um participante, **Quando** o administrador o anonimiza mantendo as respostas, **Então** nome e e-mail somem de todas as telas e exportações e as respostas continuam ligadas só ao identificador pseudônimo.
2. **Dado** um participante, **Quando** o administrador descarta as respostas dele, **Então** os blocos-papel que ele concluiu voltam a ficar livres, na mesma posição da fila.

---

### Edge Cases

- Base vazia: o participante elegível vê "Ainda não há itens para avaliar" em vez de um bloco vazio.
- Duas abas da mesma pessoa: as duas mostram o mesmo bloco e os mesmos rascunhos; a pessoa nunca segura dois blocos ao mesmo tempo.
- Pedido de código para o mesmo e-mail em dois dispositivos: vale só o código mais recente.
- Importação feita durante a coleta: blocos já reservados ou concluídos não mudam, e o bloco final incompleto de um lote anterior não é completado com itens do novo lote.
- Bloco com papel 1 abandonado: a reserva vence, os rascunhos são descartados quando outra pessoa o pega, e o bloco volta à sua posição original na fila, à frente dos blocos seguintes.
- Último bloco-papel disputado por várias pessoas: só uma recebe; as outras recebem a mensagem adequada da User Story 5.
- Participante inelegível que tenta outro e-mail: o sistema não tem como impedir; a exportação longa permite auditar.
- Texto muito longo (`origin`/`target` acima de 400 caracteres): exibido inteiro, com quebra de linha, sem cortar.
- `field` desconhecido num sistema novo: exibido com o nome original em vez da tradução.
- Caso de uso sem linha marcada como editada: o modal abre no topo, sem destaque.

## Requirements *(mandatory)*

### Functional Requirements

**Identificação e acesso**

- **FR-001**: O sistema DEVE identificar o participante por nome (2 a 120 caracteres) e e-mail válido, normalizado em minúsculas e sem espaços.
- **FR-002**: O sistema DEVE validar a entrada com um código de 6 dígitos enviado por e-mail, válido por 10 minutos, de uso único, invalidado após 5 tentativas erradas, com reenvio liberado após 60 segundos e no máximo 5 códigos por e-mail por hora.
- **FR-003**: O sistema DEVE guardar os códigos apenas de forma não reversível (hash).
- **FR-004**: A sessão de acesso DEVE durar no máximo 24 horas ou até a pessoa sair.
- **FR-005**: Se a pessoa informar um nome diferente para um e-mail já registrado, o sistema DEVE guardar o nome mais recente.

**Termo, perfil e triagem**

- **FR-006**: O sistema DEVE exibir o termo de consentimento vigente e registrar aceite com data, hora e versão; a recusa DEVE encerrar o acesso sem registrar rótulos.
- **FR-007**: Quando o termo mudar de versão, o sistema DEVE pedir novo aceite antes de continuar a rotulagem dos dados.
- **FR-008**: O participante DEVE escolher uma única habilidade profissional da lista do Apêndice B.4; "Outro" DEVE exigir descrição de até 100 caracteres.
- **FR-009**: O participante DEVE responder à pergunta de triagem do Apêndice B.3; a resposta "Não" DEVE torná-lo inelegível, de forma permanente para aquele e-mail.
- **FR-010**: Termo, habilidade e triagem DEVEM ser pedidos uma única vez por participante, salvo nova versão do termo.

**Blocos e fila**

- **FR-011**: Na confirmação de cada lote importado, o sistema DEVE dividir os itens novos, na ordem do arquivo, em blocos de 10 itens consecutivos; o último bloco do lote fica com o restante (1 a 9 itens). Blocos não mudam depois de criados.
- **FR-012**: Cada bloco DEVE ter dois blocos-papel, papel 1 e papel 2. A fila é ordenada por lote, bloco e papel: B1-P1, B1-P2, B2-P1, B2-P2…
- **FR-013**: A tela DEVE mostrar o progresso do bloco ("x de 10", ou "x de n" no bloco final).

**Alocação e concorrência**

- **FR-020**: O participante DEVE receber o primeiro bloco-papel livre da fila que ele possa avaliar, um de cada vez; enquanto tiver um bloco reservado, não recebe outro.
- **FR-021**: O sistema NUNCA DEVE entregar à mesma pessoa os dois papéis de um mesmo bloco, esteja o outro papel reservado para ela ou concluído por ela.
- **FR-022**: Ao entregar um bloco-papel, o sistema DEVE reservá-lo com exclusividade para a pessoa; a reserva vence após 20 minutos (valor configurável) sem nenhuma resposta salva e é renovada a cada resposta salva. Um bloco-papel de reserva vencida volta à sua posição original na fila. Cada pessoa tem no máximo um bloco reservado, sem limite de blocos na coleta, e o rotulador 2 pode receber o bloco enquanto o rotulador 1 ainda o rotula.
- **FR-022a**: A tela de cada item DEVE mostrar um contador regressivo, em minutos e segundos, até o vencimento da reserva, calculado a partir do horário de vencimento informado pelo servidor; o contador volta ao valor cheio a cada resposta salva e, ao chegar a zero, avisa a pessoa conforme a User Story 3.
- **FR-023**: Entregar, salvar rascunho e enviar o bloco DEVEM ser operações atômicas: dois pedidos simultâneos nunca recebem o mesmo bloco-papel, e rascunhos e envio só são aceitos da pessoa que detém a reserva, ou de reserva vencida que ninguém pegou (o que a renova).
- **FR-024**: Quando um bloco-papel de reserva vencida for entregue a outra pessoa, os rascunhos da pessoa anterior DEVEM ser descartados.
- **FR-025**: As respostas de um bloco DEVEM ficar como rascunho, editáveis pela pessoa, até o envio do bloco; o envio só é aceito com todos os itens respondidos e torna todas as respostas finais de uma vez. Respostas finais não podem ser alteradas pelo participante.
- **FR-026**: Salvar a mesma resposta mais de uma vez NÃO DEVE criar registros duplicados.
- **FR-027**: Cada resposta DEVE guardar: `change_impact` (`LOW` ou `HIGH`), `maintenance_operation` (um dos oito códigos do Apêndice B.2), comentário opcional (até 1.000 caracteres), participante, bloco, papel e data/hora; o bloco-papel concluído guarda data/hora de início e de envio.

**Apresentação do item**

- **FR-030**: O cabeçalho do item DEVE mostrar sistema, caso de uso (`uc`) e o tipo e local da edição em linguagem natural (Apêndice A).
- **FR-031**: O sistema DEVE mostrar `origin` e `target` lado a lado, destacando palavra a palavra o que saiu e o que entrou (só `target` para `ADD`, só `origin` para `DEL`), preservando quebras de linha e empilhando os dois blocos em telas estreitas.
- **FR-032**: As perguntas de impacto e de operação DEVEM trazer a explicação de cada opção (Apêndice B.1 e B.2), com escolha única em cada; avançar só fica disponível com as duas respondidas.
- **FR-033**: Junto do "Antes", o sistema DEVE oferecer um botão que abre, num modal, o caso de uso completo na versão anterior (`origin_usecase`, rotulado com `origin_version`); junto do "Depois", outro botão que abre o caso de uso completo na versão modificada (`target_usecase`, rotulado com `target_version`). O modal preserva indentação, destaca as linhas marcadas com `<- edited` (sem exibir o marcador), abre rolado até a primeira delas, fecha por botão, Esc ou clique fora e não apaga as escolhas do item.
- **FR-034**: O sistema NUNCA DEVE mostrar a um rotulador as respostas ou a identidade do outro rotulador do mesmo bloco.

**Encerramento**

- **FR-040**: Após o envio de um bloco, o sistema DEVE oferecer avaliar mais um bloco quando houver bloco-papel disponível para a pessoa.
- **FR-041**: Quando todos os blocos-papel estiverem concluídos, o sistema DEVE agradecer, informar que a rotulagem dos dados foi concluída (toda a base foi avaliada) e encerrar a participação.
- **FR-042**: Quando houver blocos-papel não concluídos, mas nenhum disponível para a pessoa, o sistema DEVE agradecer e informar que não há mais blocos disponíveis para ela no momento.

**Administração**

- **FR-050**: Somente `diegoquirino@gmail.com` (Carlos Diego Quirino Lima) DEVE acessar a área administrativa, entrando pelo mesmo código por e-mail; o administrador não rotula itens.
- **FR-051**: A importação DEVE aceitar CSV UTF-8 (RFC 4180, inclusive campos com quebra de linha) com as colunas do Apêndice D; colunas extras são ignoradas.
- **FR-052**: A importação DEVE recusar linhas com `sistema`, `edit_key`, `uc`, `kind`, `field`, `origin_version`, `target_version`, `origin_usecase` ou `target_usecase` vazios; `kind` fora de `ADD`, `DEL`, `MOD`, `META`; `origin` vazio quando `kind` ≠ `ADD`; `target` vazio quando `kind` ≠ `DEL`; `origin`/`target` com mais de 5.000 caracteres; `origin_usecase`/`target_usecase` com mais de 40.000 caracteres; rótulo de versão com mais de 20 caracteres.
- **FR-053**: O par (`sistema`, `edit_key`) DEVE ser único; linhas que repetem um item existente DEVEM ser ignoradas sem alterá-lo; repetições dentro do arquivo DEVEM ser recusadas a partir da segunda ocorrência.
- **FR-054**: A importação DEVE mostrar uma prévia (incluídas, ignoradas, com erro com linha e motivo, e blocos a criar) e só gravar após confirmação; o lote DEVE ser registrado com data, hora e nome do arquivo.
- **FR-055**: A importação NÃO DEVE alterar itens, blocos nem respostas existentes.
- **FR-056**: O painel DEVE mostrar o andamento por sistema, lote, bloco e papel; as reservas em andamento com pseudônimo, rascunhos e vencimento; os totais de participantes por situação e por habilidade; e a cota de e-mails restante do dia.
- **FR-057**: O sistema DEVE exportar só respostas finais: (a) a tabela larga do Apêndice C; (b) a tabela longa com identificador pseudônimo; (c) por sistema, `t1_<sistema>_rater1.csv` e `t1_<sistema>_rater2.csv` no formato do pacote `issta_tcm`.
- **FR-058**: As exportações (b) e (c) NÃO DEVEM conter nome nem e-mail.
- **FR-059**: O administrador DEVE poder anonimizar um participante, mantendo ou descartando as respostas dele; descartar devolve os blocos-papel dele à fila, na posição original.
- **FR-060**: As exportações (a) e (b) DEVEM neutralizar textos que começam com `=`, `+`, `-` ou `@`; a exportação (c) DEVE manter os textos idênticos aos do pipeline.

### Key Entities *(include if feature involves data)*

- **Lote de importação**: arquivo, data/hora e contagem de linhas incluídas, ignoradas e com erro; define a ordem dos seus blocos na fila.
- **Item**: uma edição de caso de uso a rotular; identificado por (`sistema`, `edit_key`); guarda `uc`, `kind`, `field`, `origin`, `target`, as versões e os textos completos do caso de uso antes e depois, o bloco e a posição no bloco.
- **Bloco**: 10 itens consecutivos de um lote (o último pode ter menos); posição fixa na fila.
- **Bloco-papel (tarefa)**: papel 1 ou 2 de um bloco; está livre, reservado (dono, início, vencimento) ou concluído (dono, início, envio).
- **Resposta**: escolha de um participante para um item num papel; é rascunho até o envio do bloco e final depois dele.
- **Participante**: pessoa identificada pelo e-mail; guarda nome, identificador pseudônimo, aceite do termo (versão e data), habilidade profissional, resposta da triagem e elegibilidade.
- **Código de acesso**: código de 6 dígitos ligado a um e-mail, com validade, tentativas e uso.
- **Termo de consentimento**: texto e versão vigentes.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Em teste com 30 participantes pedindo blocos, salvando e enviando ao mesmo tempo, nenhum bloco-papel é entregue a duas pessoas e ninguém ocupa os dois papéis de um mesmo bloco (zero violações).
- **SC-002**: Ao fim da coleta, todo item tem exatamente duas respostas finais, de duas pessoas diferentes, e nenhum bloco tem resposta final faltando.
- **SC-003**: Um participante novo vai do pedido de código ao primeiro item do bloco em menos de 5 minutos.
- **SC-004**: O próximo item aparece em menos de 3 segundos em 95 % das vezes; o modal do caso de uso abre em menos de 1 segundo.
- **SC-005**: Um bloco de 10 itens é concluído, em média, em menos de 20 minutos.
- **SC-006**: Importar 500 linhas, com prévia e confirmação, leva menos de 2 minutos.
- **SC-007**: Os arquivos `t1_<sistema>_rater1.csv` e `t1_<sistema>_rater2.csv` são lidos pelos comandos `agreement` e `oracle-disagreements` do `issta_tcm` sem edição manual.

## Assumptions

- A interface é em português; os códigos gravados ficam em inglês (`LOW`, `HIGH`, `KEEP`, …), como no pipeline do ISSTA 2027.
- A participação é aberta: qualquer pessoa com e-mail pode se identificar, sem convite. Um e-mail corresponde a uma pessoa.
- O texto do termo (TCLE) é fornecido pelo pesquisador, conforme a aprovação do comitê de ética; o sistema só exibe e registra o aceite.
- Os blocos são formados dentro de cada lote; por isso cada lote pode terminar num bloco menor que 10, mesmo que lotes posteriores sejam importados.
- Dentro de um bloco, os itens aparecem na ordem do arquivo importado; não há sorteio.
- Os textos completos do caso de uso vêm prontos no CSV, um por versão, com as linhas editadas marcadas com `<- edited`, no mesmo formato da planilha de rotulagem atual.
- **Análise de concordância**: como o rotulador 1 e o rotulador 2 variam de bloco para bloco, o κ de Cohen, que supõe os mesmos dois rotuladores em todos os itens e hoje é calculado pelo comando `agreement`, deixa de ser adequado. O artigo deve usar κ de Fleiss ou α de Krippendorff. Isso afeta a análise, não este sistema.

## Apêndice A: Tradução de `kind` e `field` para exibição

| `kind` | Exibição |
|---|---|
| `MOD` | Modificação |
| `ADD` | Inclusão |
| `DEL` | Remoção |
| `META` | Metadados |

| `field` | Exibição |
|---|---|
| `step` | Passo do fluxo básico |
| `steps_block` | Bloco de passos do fluxo básico |
| `alt_flow_step` | Passo de fluxo alternativo |
| `alt_flow_steps_block` | Fluxo alternativo inteiro |
| `alt_flow_title` | Título de fluxo alternativo |
| `exc_flow_step` | Passo de fluxo de exceção |
| `exc_flow_steps_block` | Fluxo de exceção inteiro |
| `exc_flow_title` | Título de fluxo de exceção |
| `pre` | Pré-condição |
| `post` | Pós-condição |
| `requirements` | Vínculo com requisitos |
| outro valor | O próprio valor, sem tradução |

## Apêndice B: Textos das perguntas

### B.1 Impacto da mudança (`change_impact`)

**Pergunta**: "Esta edição muda o comportamento que o caso de uso especifica?"

- **Baixo** (`LOW`): a edição não muda o comportamento observável. Exemplos: reescrita, detalhamento, sinônimo, correção de digitação, pontuação, formatação, redução, reordenação, renomeação, ou mudança apenas no vínculo com requisitos. Um passo que só mudou de lugar, ou foi unido a outro ou separado dele, sem perder comportamento, também é baixo.
- **Alto** (`HIGH`): a edição muda comportamento, entradas, saídas, condições ou o fluxo. Exemplos: incluir ou remover um requisito, funcionalidade, entidade, passo, fluxo alternativo ou de exceção. Também é alto quando o texto fica vago a ponto de o testador não conseguir decidir se o teste passou ou falhou.

### B.2 Operação de manutenção (`maintenance_operation`)

**Pergunta**: "O que deve ser feito com os casos de teste manuais que exercitam este trecho?"

| Opção | Código | Quando escolher |
|---|---|---|
| Manter | `KEEP` | O caso de teste não exercita comportamento alterado, ou a edição só reescreve o texto; ele continua executável e verificável como está. |
| Atualizar | `UPDATE` | O cenário continua existindo, mas um passo, dado, condição ou resultado esperado mudou, ou o resultado esperado precisa citar um detalhe novo. |
| Remover | `REMOVE` | O comportamento que o caso de teste verifica deixou de existir (foi apagado, não movido nem unido a outro). |
| Criar | `CREATE` | Há comportamento novo que nenhum caso de teste existente cobre. |
| Unir | `MERGE` | O comportamento foi unido ao de outro caso de teste num único cenário. |
| Dividir | `SPLIT` | Um único cenário virou vários cenários verificáveis de forma independente. |
| Revincular | `REASSIGN` | O comportamento é o mesmo, mas o vínculo com requisitos mudou. |
| Sinalizar | `FLAG` | A edição é ambígua ou deixa um passo vago; não há decisão segura e o caso precisa de análise humana. |

**Se mais de uma se aplicar**, escolha a de maior consequência: `FLAG` > `REMOVE` > `MERGE` > `SPLIT` > `CREATE` > `UPDATE` > `REASSIGN` > `KEEP`.

### B.3 Pergunta de triagem

Antes da pergunta, a tela mostra um trecho curto de caso de uso (fluxo básico com um fluxo de exceção) e um caso de teste derivado dele.

**Pergunta**: "Você já escreveu, executou ou revisou casos de teste a partir de casos de uso como este, com fluxo básico, fluxos alternativos e de exceção?"

- Sim, na prática profissional → elegível
- Sim, em estudos, cursos ou pesquisa → elegível
- Não → inelegível; encerra com agradecimento

### B.4 Habilidade profissional (`professional_skill`)

| Código | Exibição |
|---|---|
| `desenvolvedor` | Desenvolvedor(a) de software |
| `engenheiro_software` | Engenheiro(a) de software |
| `analista_sistemas` | Analista de sistemas |
| `analista_requisitos` | Analista de requisitos ou de negócios |
| `analista_testes` | Analista de testes / QA |
| `engenheiro_testes` | Engenheiro(a) de testes ou de automação de testes |
| `arquiteto_software` | Arquiteto(a) de software |
| `lider_tecnico` | Líder técnico(a) |
| `gestor_produto_projeto` | Product owner, gerente de produto ou de projetos |
| `docente_pesquisador` | Docente ou pesquisador(a) em computação |
| `estudante_pos` | Estudante de pós-graduação em computação |
| `estudante_graduacao` | Estudante de graduação em computação |
| `outro` | Outro (descrever) |

## Apêndice C: Cabeçalho da exportação larga

```text
sistema,edit_key,uc,kind,field,origin,target,rater_1_name,rater_1_email,rater_1_accepted_research_terms_lgpd,rater_1_professional_skill,rater_1_change_impact_choice,rater_1_maintenance_operation_choice,rater_1_comment,rater_2_name,rater_2_email,rater_2_accepted_research_terms_lgpd,rater_2_professional_skill,rater_2_change_impact_choice,rater_2_maintenance_operation_choice,rater_2_comment
```

`rater_N_accepted_research_terms_lgpd` traz a versão e a data/hora do aceite (por exemplo, `v1 2026-11-20T14:03:00-03:00`).

## Apêndice D: Colunas do CSV de importação

| Coluna | Obrigatória | Conteúdo |
|---|---|---|
| `sistema` | sim | Nome do sistema estudado, por exemplo `minitest` |
| `edit_key` | sim | Identificador da edição no sistema, por exemplo `LoginTest\|1\|1` |
| `uc` | sim | Caso de uso, por exemplo `LoginTest` |
| `kind` | sim | `ADD`, `DEL`, `MOD` ou `META` |
| `field` | sim | Local da edição (Apêndice A) |
| `origin` | exceto em `ADD` | Trecho antes da edição |
| `target` | exceto em `DEL` | Trecho depois da edição |
| `origin_version` | sim | Rótulo da versão anterior, por exemplo `v1.1` |
| `target_version` | sim | Rótulo da versão modificada, por exemplo `v1.2` |
| `origin_usecase` | sim | Caso de uso completo na versão anterior, linhas editadas marcadas com `<- edited` |
| `target_usecase` | sim | Caso de uso completo na versão modificada, idem |

## Apêndice E: Exemplo de fila

Base de 205 itens importados num único lote: 20 blocos de 10 itens e o bloco 21 com 5 itens, ou 42 blocos-papel.

| Ordem | Bloco-papel | Itens | Quem pode receber |
|---|---|---|---|
| 1 | B1-P1 | 1–10 | qualquer participante elegível |
| 2 | B1-P2 | 1–10 | qualquer um, menos quem tem B1-P1 |
| 3 | B2-P1 | 11–20 | qualquer participante elegível |
| 4 | B2-P2 | 11–20 | qualquer um, menos quem tem B2-P1 |
| … | … | … | … |
| 41 | B21-P1 | 201–205 | qualquer participante elegível |
| 42 | B21-P2 | 201–205 | qualquer um, menos quem tem B21-P1 |
