# Implementation Plan: Gerar o CSV de importação a partir do pipeline

**Branch**: `002-csv-oraculo` | **Date**: 2026-11-11 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/002-csv-oraculo/spec.md`

## Summary

Um novo comando do pipeline do ISSTA 2027, `python -m issta_tcm oracle-import`, gera
`inputs/oracle/veredito_<sistema>.csv` com uma linha por edição de `s3_diff/edits.csv`, nas colunas
do Apêndice D da spec 001. Os textos completos do caso de uso saem do mesmo contexto que a planilha
de rotulagem e as técnicas já usam (campo `context` de `s5_blind/public/requests_T1.jsonl`,
gerado por `claret.render(..., mark=...)`), dividido em versão anterior e modificada. Os rótulos de
versão vêm da configuração do sistema (`v1.1`, `v1.2`). Antes de gravar, o comando valida cada linha
com as regras de importação do Veredito (FR-052 e FR-053 da 001), mostra um resumo e falha se
alguma linha seria recusada.

## Technical Context

**Language/Version**: Python ≥ 3.10 (o do pacote `issta-tcm`; na máquina do pesquisador,
`C:\Users\diego\AppData\Local\Python\bin\python.exe`)

**Primary Dependencies**: só a biblioteca padrão (`csv`, `collections`) e módulos do próprio
pacote (`config`, `stages.common`, `evaluation.oracle`, `utils`); nenhuma dependência nova

**Storage**: arquivos; lê `runs/<run>/s3_diff/edits.csv`, `s5_blind/public/requests_T1.jsonl` e
`s5_blind/_private/edits_index.json`; grava `inputs/oracle/veredito_<sistema>.csv`

**Testing**: pytest (`src/tests/`), no mesmo estilo de `test_oracle.py` (`tmp_path` e
`monkeypatch`)

**Target Platform**: máquina do pesquisador (Windows), linha de comando

**Project Type**: subcomando de CLI de um pacote Python existente

**Performance Goals**: Minitest (38 edições) em menos de 1 minuto (SC-001); na prática, segundos

**Constraints**: saída determinística (FR-007); UTF-8 sem BOM, RFC 4180 (`csv` com `\r\n` entre
registros e `\n` dentro dos campos entre aspas); nada de rótulos, saídas de técnicas ou ids cegos
(FR-008)

**Scale/Scope**: algumas centenas de edições por sistema; cada caso de uso completo tem hoje até
cerca de 2.200 caracteres (limite do Veredito: 40.000)

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

A constitution 2.1.0 trata a spec 002 como exceção (seção *Escopo e exceções*): o comando segue as
convenções do `ISSTA_2027`, e os Princípios I, II, III e V e as regras de publicação não se aplicam.
Continuam valendo:

| Regra | Como o plano atende | Situação |
|---|---|---|
| Princípio IV: sem dados de avaliadores nem ids cegos no arquivo | Só as colunas de `edits.csv` (`edit_key` … `target`) e o contexto; os `E-xxx` são usados só em memória para achar o contexto e não vão para o arquivo; nenhum `t1_*_rater*.csv` é lido | Passa |
| Princípio IV: repositório público | O arquivo fica em `ISSTA_2027/src/inputs/oracle/`, fora do branch `Veredito`; não tem dados pessoais | Passa |
| UTF-8 | `utils.write_csv` já grava em UTF-8; teste confere o arquivo | Passa |
| Compatibilidade com a importação do Veredito | Colunas do Apêndice D na ordem; validação espelha FR-052/FR-053; contrato em [contracts/import-csv.md](contracts/import-csv.md) | Passa |

Nenhuma violação; *Complexity Tracking* fica vazio.

**Re-check pós-design**: o desenho da fase 1 não acrescenta dependências, não grava fora da pasta do
oráculo e não lê arquivos de avaliadores. Passa.

## Project Structure

### Documentation (this feature)

```text
specs/002-csv-oraculo/
├── plan.md              # este arquivo
├── research.md          # fase 0
├── data-model.md        # fase 1
├── quickstart.md        # fase 1
├── contracts/
│   └── import-csv.md    # fase 1: formato do arquivo e regras de validação
└── tasks.md             # fase 2 (/speckit-tasks; não criado aqui)
```

### Source Code (repositório `ISSTA_2027`, fora do branch `Veredito`)

```text
ISSTA_2027/src/
├── issta_tcm/
│   ├── cli.py                     # + subcomando oracle-import (cmd_oracle_import)
│   └── evaluation/
│       ├── oracle.py              # sem mudança de comportamento; reutiliza ITEM_FIELDS, _cell, edits_csv
│       └── veredito_import.py     # novo: montar linhas, validar, resumir, gravar
├── tests/
│   └── test_veredito_import.py    # novo
├── inputs/oracle/
│   └── veredito_<sistema>.csv     # saída
└── README.md                      # + uma linha na lista de comandos do oráculo
```

**Structure Decision**: módulo novo em `issta_tcm/evaluation/`, ao lado de `oracle.py`, porque o
arquivo é derivado dos mesmos insumos da planilha de rotulagem; o subcomando segue o padrão dos
`oracle-*` já existentes em `cli.py`.

## Complexity Tracking

Sem violações.
