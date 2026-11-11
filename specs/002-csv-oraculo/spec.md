# Feature Specification: Gerar o CSV de importação a partir do pipeline

**Feature Branch**: `002-csv-oraculo`

**Created**: 2026-11-10

**Status**: Draft

**Input**: User description: "Gerar, a partir dos dados do pipeline do ISSTA 2027, o arquivo CSV que o Veredito importa (Apêndice D da spec 001), um arquivo por sistema, com o caso de uso completo de cada versão e as linhas editadas marcadas."

**Spec geral**: [001-rotulagem-oraculo](../001-rotulagem-oraculo/spec.md) · **Depende de**: nada · **Pode correr em paralelo com**: 003

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Gerar o arquivo de importação de um sistema (Priority: P1)

O pesquisador roda um comando para um sistema já processado pelo pipeline (por exemplo, `minitest`) e recebe um CSV com uma linha por edição, pronto para ser importado no Veredito.

**Why this priority**: É o contrato entre o pipeline e o Veredito. Com um arquivo real em mãos desde o início, a importação (005) e a tela do item (008) são testadas com textos longos, aspas e quebras de linha de verdade.

**Independent Test**: Gerar o arquivo do Minitest e conferir, linha a linha, contra `edits.csv` e a planilha de rotulagem do sistema.

**Acceptance Scenarios**:

1. **Dado** um sistema com as edições já calculadas pelo pipeline, **Quando** o pesquisador gera o arquivo, **Então** recebe um CSV com uma linha por edição, na mesma ordem das edições do pipeline (38 linhas no Minitest), com as colunas do Apêndice D da spec 001, nessa ordem.
2. **Dado** o arquivo gerado, **Quando** o pesquisador compara uma linha com a edição original, **Então** `edit_key`, `uc`, `kind`, `field`, `origin` e `target` são idênticos aos do pipeline, caractere por caractere, e `sistema` traz o identificador do sistema.
3. **Dado** uma edição, **Quando** o arquivo é gerado, **Então** `origin_usecase` traz o caso de uso inteiro na versão anterior e `target_usecase` na versão modificada, no mesmo formato da planilha de rotulagem, com `<- edited` só nas linhas tocadas por aquela edição.
4. **Dado** uma edição entre duas versões do sistema, **Quando** o arquivo é gerado, **Então** `origin_version` e `target_version` trazem os rótulos dessas versões na configuração do pipeline (por exemplo, `v1.1` e `v1.2`), e não a versão declarada dentro do caso de uso, que pode ficar atrás da versão do sistema.
5. **Dado** um caso de uso criado na versão modificada (não existia na anterior), **Quando** o arquivo é gerado, **Então** `origin_usecase` traz um aviso curto de que o caso de uso não existia naquela versão; o inverso vale para caso de uso removido.
6. **Dado** o mesmo sistema, **Quando** o arquivo é gerado duas vezes, **Então** os dois arquivos são idênticos.

---

### User Story 2 - Conferir o arquivo antes de importar (Priority: P2)

Ao gerar o arquivo, o pesquisador vê um resumo e é avisado de qualquer linha que o Veredito recusaria.

**Why this priority**: Evita descobrir problemas só na prévia da importação, já com a coleta em andamento.

**Independent Test**: Gerar o arquivo de um sistema com uma edição artificialmente longa e conferir o aviso.

**Acceptance Scenarios**:

1. **Dado** o arquivo gerado, **Quando** o comando termina, **Então** mostra o total de linhas, a contagem por `kind` e por `field` e o maior tamanho de `origin`, `target` e caso de uso completo.
2. **Dado** uma linha que viola alguma regra de validação da importação (FR-052 da spec 001), **Quando** o arquivo é gerado, **Então** o comando lista a linha e o motivo e termina indicando falha.
3. **Dado** uma linha em que nenhum dos dois casos de uso tem linha marcada como editada, **Quando** o arquivo é gerado, **Então** o comando avisa, sem falhar.

---

### Edge Cases

- Edição `META` de `requirements`: a linha marcada é a de vínculo com requisitos.
- Caso de uso que declara uma versão diferente da versão do sistema (no Minitest, as versões 1.1 e 1.2 ainda declaram 1.0): vale o rótulo da versão do sistema.
- Texto com vírgulas, aspas ou quebras de linha: fica entre aspas, e as aspas internas são duplicadas.
- Sistema sem nenhuma edição: o arquivo sai só com o cabeçalho e o comando avisa.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: O comando DEVE gerar um CSV por sistema, com uma linha por edição, na ordem das edições do pipeline.
- **FR-002**: O CSV DEVE ter exatamente as colunas do Apêndice D da spec 001, nessa ordem, em UTF-8 e no padrão RFC 4180.
- **FR-003**: `edit_key`, `uc`, `kind`, `field`, `origin` e `target` DEVEM ser copiados sem alteração.
- **FR-004**: Os casos de uso completos DEVEM seguir o formato da planilha de rotulagem, marcando com `<- edited` apenas as linhas tocadas pela edição da linha.
- **FR-005**: Quando o caso de uso não existir numa das versões, o texto daquela versão DEVE ser um aviso curto, nunca vazio.
- **FR-006**: O comando DEVE validar cada linha com as regras de importação do Veredito e falhar se alguma for violada, listando linha e motivo.
- **FR-007**: A geração DEVE ser determinística.
- **FR-008**: O arquivo NÃO DEVE conter rótulos de avaliadores, saídas das técnicas nem identificadores cegos.

### Key Entities

- **Edição**: unidade do oráculo produzida pelo pipeline; vira um item no Veredito.
- **Caso de uso versionado**: texto completo de um caso de uso numa versão do sistema.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: O arquivo do Minitest é gerado em menos de 1 minuto.
- **SC-002**: 100 % das linhas do Minitest têm ao menos uma linha marcada como editada num dos dois casos de uso.
- **SC-003**: O arquivo do Minitest passa pela prévia da importação do Veredito (spec 005) sem nenhuma linha com erro.

## Assumptions

- O comando faz parte do pipeline do ISSTA 2027 e usa as mesmas configurações por sistema; o arquivo sai na pasta do oráculo, ao lado dos arquivos `t1_*`.
- O identificador do sistema é o mesmo usado nos nomes `t1_<sistema>_*.csv`.
- Repetir o caso de uso completo em cada linha é aceitável para algumas centenas de edições por sistema.
