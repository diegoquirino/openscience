# Feature Specification: Importar itens por CSV e formar os blocos da fila

**Feature Branch**: `005-importacao-blocos`

**Created**: 2026-11-10

**Status**: Draft

**Input**: User description: "O administrador envia um CSV com as colunas do Apêndice D da spec 001, confere uma prévia e confirma. Na confirmação, os itens novos são divididos em blocos fixos de 10 e cada bloco entra no fim da fila com os papéis de avaliador 1 e 2."

**Spec geral**: [001-rotulagem-oraculo](../001-rotulagem-oraculo/spec.md), User Story 7, FR-011, FR-012 e FR-051 a FR-055 · **Depende de**: 004 · **Pode correr em paralelo com**: 006 e 008 · **Usa como teste**: o arquivo da spec 002

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Ver a prévia de um CSV (Priority: P1)

O administrador envia um arquivo e vê, antes de gravar qualquer coisa, o que entra, o que é ignorado e o que tem erro.

**Why this priority**: Erros na base de itens contaminam o oráculo; a prévia é a barreira.

**Independent Test**: Enviar o arquivo do Minitest e um arquivo com duplicatas e erros, e conferir as contagens e os motivos.

**Acceptance Scenarios**:

1. **Dado** um CSV UTF-8 com todas as colunas do Apêndice D, **Quando** o administrador o envia, **Então** vê quantos itens serão incluídos, quantos ignorados e quantos com erro (cada um com número da linha e motivo) e quantos blocos serão criados, com o tamanho de cada um.
2. **Dado** um CSV sem uma coluna obrigatória, **Quando** é enviado, **Então** é recusado inteiro, indicando a coluna ausente.
3. **Dado** campos entre aspas com vírgulas, aspas duplicadas ou quebras de linha, **Quando** o arquivo é lido, **Então** os textos são preservados exatamente.
4. **Dado** uma linha cujo (`sistema`, `edit_key`) já existe na base, **Quando** a prévia é gerada, **Então** a linha aparece como ignorada.
5. **Dado** o mesmo (`sistema`, `edit_key`) repetido no arquivo, **Quando** a prévia é gerada, **Então** só a primeira ocorrência entra e as demais aparecem como erro.
6. **Dado** uma linha com campo obrigatório vazio, `kind` inválido, `origin` vazio fora de `ADD`, `target` vazio fora de `DEL`, `origin`/`target` acima de 5.000 caracteres, caso de uso acima de 40.000 caracteres ou rótulo de versão acima de 20 caracteres, **Quando** a prévia é gerada, **Então** a linha aparece como erro com o motivo.
7. **Dado** a prévia, **Quando** o administrador cancela, **Então** nada é gravado.
8. **Dado** colunas extras no arquivo, **Quando** é lido, **Então** elas são ignoradas e a prévia as lista.
9. **Dado** o ensaio local, ou a produção na fase `piloto`, **Quando** o administrador envia um arquivo com alguma linha de `sistema` diferente de `minitest`, **Então** o arquivo é recusado inteiro, com a mensagem "Nesta fase só são aceitos itens do Minitest" e as linhas de outro sistema.

---

### User Story 2 - Confirmar e formar os blocos (Priority: P1)

Ao confirmar, os itens novos são gravados, divididos em blocos de 10 na ordem do arquivo, e cada bloco entra no fim da fila com os papéis 1 e 2 livres.

**Why this priority**: Os blocos e a fila são a entrada da alocação (spec 007).

**Independent Test**: Confirmar 38 itens e conferir 4 blocos (10, 10, 10 e 8) e 8 tarefas na fila; depois importar outro lote e conferir que os anteriores não mudaram.

**Acceptance Scenarios**:

1. **Dado** a prévia com 38 itens válidos, **Quando** o administrador confirma, **Então** são criados 4 blocos de 10, 10, 10 e 8 itens, na ordem do arquivo, e 8 tarefas na fila: B1-P1, B1-P2, B2-P1, B2-P2, …, todas livres.
2. **Dado** 205 itens válidos num lote, **Quando** confirmados, **Então** há 20 blocos de 10 e um de 5, e 42 tarefas.
3. **Dado** a confirmação, **Quando** termina, **Então** o lote fica registrado com data, hora, nome do arquivo e contagens, e o administrador vê o resumo: blocos criados e posições ocupadas na fila.
4. **Dado** uma coleta em andamento com um bloco final de 8 itens, **Quando** um novo lote é importado, **Então** esse bloco continua com 8 itens e os novos blocos entram depois de todas as tarefas existentes.
5. **Dado** duas confirmações do mesmo arquivo (clique duplo ou duas abas), **Quando** processadas, **Então** os itens são gravados uma única vez e a segunda confirmação mostra todas as linhas como ignoradas.
6. **Dado** a confirmação, **Quando** os itens são gravados, **Então** itens, blocos, tarefas e respostas existentes não mudam.

---

### User Story 3 - Consultar os lotes importados (Priority: P3)

O administrador vê a lista de lotes com data, arquivo, sistemas e número de itens e blocos.

**Why this priority**: Ajuda a auditar a base, mas o painel completo vem na spec 010.

**Independent Test**: Importar dois lotes e conferir a lista.

**Acceptance Scenarios**:

1. **Dado** lotes importados, **Quando** o administrador abre "Importar", **Então** vê os lotes anteriores, do mais recente ao mais antigo.

---

### User Story 4 - Encerrar o piloto e liberar a coleta real (Priority: P2)

Depois do piloto, com a base de produção recriada vazia, o administrador passa a fase do estudo de `piloto` para `coleta` e passa a poder importar os sistemas reais.

**Why this priority**: Garante que o piloto, na produção, use só o Minitest e que os dados do piloto não se misturem com os da coleta real.

**Independent Test**: Num teste automático com a configuração de produção (MySQL em contêiner): importar o Minitest na fase `piloto`, tentar passar para `coleta` (recusado), limpar a base, passar para `coleta` (aceito) e importar um arquivo de outro sistema (aceito).

**Acceptance Scenarios**:

1. **Dado** a produção na fase `piloto`, **Quando** o administrador abre "Importar", **Então** vê a fase atual e o aviso de que só itens do Minitest são aceitos.
2. **Dado** a fase `piloto` com qualquer item, participante ou resposta na base, **Quando** o administrador tenta passar para `coleta`, **Então** a mudança é recusada, explicando que a base precisa ser recriada antes.
3. **Dado** a fase `piloto` com a base vazia, **Quando** o administrador confirma a passagem para `coleta`, **Então** a fase muda, a mudança fica registrada com data e hora e a importação passa a aceitar qualquer sistema.
4. **Dado** a fase `coleta`, **Quando** alguém tenta voltar para `piloto`, **Então** não há essa ação: a única forma é recriar a base.
5. **Dado** o ensaio local, **Quando** o administrador abre "Importar", **Então** a ação de passar para `coleta` não existe e só o Minitest é aceito.

---

### Edge Cases

- Arquivo vazio ou só com cabeçalho: prévia com zero itens e confirmação desabilitada.
- Arquivo com BOM, fim de linha Windows ou Unix: lido igualmente.
- Arquivo que não é CSV ou não está em UTF-8: recusado com mensagem clara.
- Arquivo com 500 linhas: prévia e confirmação terminam sem estourar o tempo de execução.
- Importação enquanto participantes pedem blocos: ninguém recebe um bloco pela metade.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: A importação DEVE aceitar CSV UTF-8 no padrão RFC 4180, inclusive campos com quebra de linha, com as colunas do Apêndice D da spec 001; colunas extras são ignoradas.
- **FR-002**: A importação DEVE aplicar as regras de validação de linha da FR-052 da spec 001.
- **FR-003**: O par (`sistema`, `edit_key`) DEVE ser único na base; linhas que repetem item existente são ignoradas sem alterá-lo; repetições dentro do arquivo são erro a partir da segunda.
- **FR-004**: A importação DEVE mostrar a prévia e só gravar após confirmação.
- **FR-005**: Na confirmação, o sistema DEVE dividir os itens novos, na ordem do arquivo, em blocos consecutivos do tamanho configurado (10); o último bloco do lote fica com o restante.
- **FR-006**: Cada bloco DEVE gerar duas tarefas, papel 1 e papel 2, acrescentadas ao fim da fila nessa ordem.
- **FR-007**: Blocos, itens e posições na fila NÃO DEVEM mudar depois de criados.
- **FR-008**: A confirmação DEVE ser atômica em relação às demais operações: ou grava o lote inteiro, ou nada.
- **FR-009**: O lote DEVE ser registrado com data, hora, nome do arquivo e contagens de incluídas, ignoradas e com erro.
- **FR-010**: Só o administrador DEVE poder importar.
- **FR-011**: No ensaio local, sempre, e na produção, enquanto a fase do estudo for `piloto`, a importação DEVE recusar inteiro um arquivo que tenha alguma linha com `sistema` diferente de `minitest`.
- **FR-012**: O administrador DEVE poder passar a fase de `piloto` para `coleta` só na produção e só com a base sem itens, participantes e respostas; a mudança é registrada e não tem volta pela interface.

### Key Entities

- **Lote de importação**: arquivo, data/hora, contagens.
- **Item**: (`sistema`, `edit_key`), `uc`, `kind`, `field`, `origin`, `target`, versões e casos de uso completos, lote, bloco e posição no bloco.
- **Bloco**: itens consecutivos de um lote; tamanho de 1 a 10.
- **Tarefa (bloco-papel)**: papel 1 ou 2 de um bloco; posição na fila; começa livre.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Importar 500 linhas (as do Minitest repetidas com sufixo no `edit_key`), com prévia e confirmação, leva menos de 2 minutos.
- **SC-002**: Em todos os testes de divisão (1, 9, 10, 11, 38 e 205 itens), o número e o tamanho dos blocos batem com a regra.
- **SC-003**: Um texto importado e lido de volta é idêntico ao do arquivo em 100 % das linhas do Minitest.

## Assumptions

- Os testes e o ensaio usam só o CSV do Minitest gerado pela spec 002 e arquivos derivados dele (duplicatas, coluna ausente, texto longo, repetição para volume), conforme a constitution.
- Os blocos são formados dentro de cada lote; um lote pode terminar num bloco menor que 10 mesmo que outros lotes venham depois.
- Itens não são editados nem apagados pela interface. Corrigir um item antes do início da coleta exige limpar a base e importar de novo; depois do início, a correção fica registrada como decisão do pesquisador, fora do sistema.
