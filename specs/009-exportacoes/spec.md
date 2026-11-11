# Feature Specification: Exportar as classificações

**Feature Branch**: `009-exportacoes`

**Created**: 2026-11-10

**Status**: Draft

**Input**: User description: "O administrador baixa as classificações finais em três formatos: a tabela larga (uma linha por item, com os dois avaliadores), a tabela longa pseudonimizada (uma linha por resposta) e, por sistema, os arquivos rater1 e rater2 que o pacote issta_tcm lê. Rascunhos nunca são exportados."

**Spec geral**: [001-rotulagem-oraculo](../001-rotulagem-oraculo/spec.md), User Story 8, FR-057, FR-058, FR-060 e Apêndice C · **Depende de**: 005 e 007 (pode começar depois da 005, com respostas de teste)

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Baixar os arquivos do pipeline (Priority: P1)

Para cada sistema, o administrador baixa `t1_<sistema>_rater1.csv` e `t1_<sistema>_rater2.csv`, prontos para o pipeline.

**Why this priority**: É o produto final do estudo e o formato que o pipeline já lê.

**Independent Test**: Exportar o Minitest com blocos concluídos e rodar `agreement` e `oracle-disagreements` do `issta_tcm` sobre os arquivos.

**Acceptance Scenarios**:

1. **Dado** um sistema com blocos concluídos, **Quando** o administrador exporta, **Então** recebe dois CSV com as colunas `edit_key,uc,kind,field,origin,target,change_impact,maintenance_operation,comment`, uma linha por item do sistema, na ordem da fila.
2. **Dado** um item cujo papel ainda não foi concluído, **Quando** exportado, **Então** a linha existe com `change_impact`, `maintenance_operation` e `comment` em branco no arquivo daquele papel.
3. **Dado** os arquivos exportados, **Quando** lidos pelo pipeline, **Então** funcionam sem edição manual.

---

### User Story 2 - Baixar a tabela larga (Priority: P1)

O administrador baixa uma linha por item, com os dados dos dois avaliadores lado a lado (Apêndice C).

**Why this priority**: É a visão consolidada pedida pelo pesquisador, com identificação, para uso interno.

**Independent Test**: Exportar com blocos concluídos, em andamento e livres e conferir as colunas.

**Acceptance Scenarios**:

1. **Dado** blocos concluídos, **Quando** exportada, **Então** a tabela tem o cabeçalho do Apêndice C, uma linha por item de todos os sistemas, na ordem da fila.
2. **Dado** um papel concluído, **Quando** exportado, **Então** traz nome, e-mail, aceite do termo (versão e data/hora), habilidade, códigos escolhidos e comentário do avaliador daquele papel.
3. **Dado** um papel não concluído, **Quando** exportado, **Então** os campos daquele avaliador ficam em branco.

---

### User Story 3 - Baixar a tabela longa pseudonimizada (Priority: P2)

O administrador baixa uma linha por resposta final, sem nome nem e-mail, para análise e para o pacote de replicação.

**Why this priority**: Permite calcular a concordância com avaliadores variáveis (κ de Fleiss ou α de Krippendorff) e publicar os dados sem identificar ninguém.

**Independent Test**: Exportar e conferir que nenhuma coluna traz nome ou e-mail.

**Acceptance Scenarios**:

1. **Dado** respostas finais, **Quando** exportada, **Então** a tabela traz, por resposta: sistema, edit_key, bloco, papel, identificador pseudônimo (`R-0001`), habilidade, códigos, comentário e datas de início e envio do bloco.
2. **Dado** a tabela longa, **Quando** aberta, **Então** não contém nome nem e-mail em nenhuma coluna.

---

### Edge Cases

- Rascunhos de blocos em andamento: não aparecem em nenhuma exportação.
- Texto que começa com `=`, `+`, `-` ou `@`: nas tabelas larga e longa, exportado de forma que não vire fórmula ao abrir numa planilha; nos arquivos do pipeline, mantido como está.
- Textos com vírgulas, aspas ou quebras de linha: ficam entre aspas, preservados.
- Sistema sem nenhum bloco concluído: os arquivos do pipeline saem com todas as respostas em branco.
- Base com centenas de itens: a exportação termina sem estourar o tempo de execução.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: O sistema DEVE exportar só respostas finais.
- **FR-002**: O sistema DEVE exportar a tabela larga do Apêndice C da spec 001.
- **FR-003**: O sistema DEVE exportar a tabela longa com identificador pseudônimo.
- **FR-004**: O sistema DEVE exportar, por sistema, `t1_<sistema>_rater1.csv` e `t1_<sistema>_rater2.csv` no formato do pacote `issta_tcm`, na ordem da fila.
- **FR-005**: A tabela longa e os arquivos do pipeline NÃO DEVEM conter nome nem e-mail.
- **FR-006**: Todos os CSV DEVEM ser UTF-8 e RFC 4180. A tabela larga e a longa DEVEM neutralizar textos que começam com `=`, `+`, `-` ou `@`; os arquivos do pipeline NÃO DEVEM alterar nenhum texto, para continuar idênticos às edições do pipeline.
- **FR-007**: Só o administrador DEVE exportar.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Os arquivos `t1_<sistema>_rater*.csv` são lidos pelos comandos `agreement` e `oracle-disagreements` do `issta_tcm` sem edição manual.
- **SC-002**: Exportar uma base de 500 itens leva menos de 1 minuto.
- **SC-003**: O número de respostas na tabela longa bate com o de papéis concluídos multiplicado pelo tamanho de cada bloco.

## Assumptions

- "Papel 1" vira `rater1` e "papel 2" vira `rater2`, embora as pessoas variem de bloco para bloco (ver nota sobre concordância na spec 001).
- Os arquivos do pipeline não são feitos para abrir numa planilha; quem os abrir deve importá-los como texto.
