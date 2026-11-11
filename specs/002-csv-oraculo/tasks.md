---

description: "Tarefas da spec 002: gerar o CSV de importação do Veredito a partir do pipeline"
---

# Tasks: Gerar o CSV de importação a partir do pipeline

**Input**: documentos de `specs/002-csv-oraculo/` (plan.md, spec.md, research.md, data-model.md, contracts/import-csv.md, quickstart.md)

**Prerequisites**: plan.md e spec.md; o run `runs/minitest-piloto` processado até o S5

**Tests**: incluídos. O plano define pytest no estilo de `tests/test_oracle.py`, e o quickstart espera um teste para cada regra do contrato.

**Organization**: tarefas agrupadas por user story; cada história pode ser testada sozinha.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: pode rodar em paralelo (arquivos diferentes, sem dependência de tarefa incompleta)
- **[Story]**: história da spec (US1, US2)
- Todos os caminhos são relativos a `TRABALHO DE TESE/`; o código fica em `ISSTA_2027/src/`, fora do branch `Veredito` (exceção da constitution 2.6.0)
- Comandos rodam em `ISSTA_2027/src` com `C:\Users\diego\AppData\Local\Python\bin\python.exe`

---

## Phase 1: Setup

**Purpose**: confirmar a linha de base e criar os arquivos novos vazios

- [ ] T001 Conferir a linha de base em `ISSTA_2027/src`: `python -m pytest -q` passa (73 testes) e existem `runs/minitest-piloto/s3_diff/edits.csv`, `runs/minitest-piloto/s5_blind/public/requests_T1.jsonl` e `runs/minitest-piloto/s5_blind/_private/edits_index.json`
- [ ] T002 Criar `ISSTA_2027/src/issta_tcm/evaluation/veredito_import.py` com docstring do módulo e as constantes: `COLUMNS` (cabeçalho exato `sistema,edit_key,uc,kind,field,origin,target,origin_version,target_version,origin_usecase,target_usecase`), `KINDS = ("ADD", "DEL", "MOD", "META")`, `MAX_TEXT = 5000`, `MAX_USECASE = 40000`, `MAX_VERSION = 20`, `MISSING_EN = "(the use case does not exist in this version)"`, `MISSING_PT = "(este caso de uso não existe nesta versão)"`, `EDITED_MARK = "<- edited"`; e a função `output_path(cfg)` que devolve `oracle.oracle_dir(cfg) / f"veredito_{cfg.subject.key}.csv"`
- [ ] T003 [P] Criar `ISSTA_2027/src/tests/test_veredito_import.py` com uma fixture que monta um run falso em `tmp_path` (`s3_diff/edits.csv`, `s5_blind/_private/edits_index.json` com `E-xxx -> {edit_key, k}`, `s5_blind/public/requests_T1.jsonl` com `request_id` e `context`) e um `Config` apontando para ele via `monkeypatch`, no mesmo estilo de `tests/test_oracle.py`

---

## Phase 2: Foundational

**Purpose**: funções puras usadas pelas duas histórias

**CRITICAL**: as histórias dependem destas funções

- [ ] T004 Implementar `split_context(context: str) -> tuple[str, str]` em `ISSTA_2027/src/issta_tcm/evaluation/veredito_import.py`: exige exatamente um `### origin version\n` no início e um `\n### target version\n`; devolve os dois trechos com `strip("\n")`; troca o texto exato `MISSING_EN` por `MISSING_PT`; com cabeçalho ausente ou repetido, lança `ValueError` com a mensagem "contexto fora do formato do S5"
- [ ] T005 Implementar `version_labels(cfg) -> dict[int, tuple[str, str]]` em `ISSTA_2027/src/issta_tcm/evaluation/veredito_import.py`: para cada `(k, versão anterior, versão modificada)` de `cfg.subject.steps()`, devolve `("v" + label anterior, "v" + label modificada)`
- [ ] T006 [P] Testes de `split_context` e `version_labels` em `ISSTA_2027/src/tests/test_veredito_import.py`: contexto gerado pelo próprio `issta_tcm.stages.s5_blind._context` (protege contra mudança no cabeçalho do S5, risco 7 do research), aviso de caso de uso ausente traduzido nos dois sentidos (criado e removido), cabeçalho ausente lança `ValueError`, rótulos `v1.0`/`v1.1` para `k = 1` com a configuração do Minitest

**Checkpoint**: funções de base prontas e testadas

---

## Phase 3: User Story 1 - Gerar o arquivo de importação de um sistema (Priority: P1) MVP

**Goal**: `python -m issta_tcm oracle-import` grava `inputs/oracle/veredito_<sistema>.csv`, uma linha por edição, nas colunas do Apêndice D

**Independent Test**: gerar o arquivo do Minitest e conferir, linha a linha, contra `edits.csv` e `labeling_sheet_minitest.md` (quickstart, seções 2 a 4)

### Tests for User Story 1

> Escrever primeiro e ver falhar antes da implementação

- [ ] T007 [P] [US1] Teste de `build_rows` em `ISSTA_2027/src/tests/test_veredito_import.py`: as chaves de cada linha são `COLUMNS`, nessa ordem; `edit_key`, `uc`, `kind`, `field`, `origin` e `target` iguais aos de `edits.csv` lidos com `oracle._cell` (cenário 2); `sistema == cfg.subject.key`; ordem das linhas igual à de `edits.csv` (cenário 1); `origin_version`/`target_version` vêm do passo `k` da edição, e não da versão declarada no caso de uso (cenário 4); caso de uso criado ou removido traz `MISSING_PT` (cenário 5); `edit_key` sem pedido T1 lança erro com a lista das chaves e a orientação "rode o S5"
- [ ] T008 [P] [US1] Teste de `write` em `ISSTA_2027/src/tests/test_veredito_import.py`: arquivo em UTF-8 sem BOM; registros separados por `\r\n`; campos com vírgula, aspas ou quebra de linha entre aspas, com aspas internas duplicadas e `\n` preservado dentro do campo; lido de volta com `csv.DictReader`, devolve exatamente as linhas escritas; gerar duas vezes produz bytes idênticos (cenário 6); sistema sem edições grava só o cabeçalho

### Implementation for User Story 1

- [ ] T009 [US1] Implementar `build_rows(cfg) -> list[dict]` em `ISSTA_2027/src/issta_tcm/evaluation/veredito_import.py`: lê `oracle.edits_csv(cfg)` com `utils.read_csv`; liga `edit_key -> E-xxx` com `oracle._key_to_eid(cfg)` e `E-xxx -> k` com `s5_blind/_private/edits_index.json`; pega o `context` de cada `E-xxx` em `stages.common.requests(cfg, "T1")`; monta cada linha com `oracle._cell` para os campos de `edits.csv`, `version_labels(cfg)[k]` e `split_context(context)`; não lê nenhum `t1_*_rater*.csv` nem grava `E-xxx` no resultado (FR-008)
- [ ] T010 [US1] Implementar `write(cfg, rows) -> Path` em `ISSTA_2027/src/issta_tcm/evaluation/veredito_import.py` com `utils.write_csv(output_path(cfg), rows, COLUMNS)`, sobrescrevendo o arquivo existente
- [ ] T011 [US1] Acrescentar `cmd_oracle_import` e o subcomando `oracle-import` (`parents=[common]`, ajuda "write the Veredito import CSV of a subject") em `ISSTA_2027/src/issta_tcm/cli.py`, seguindo `cmd_oracle_template`; incluir a linha do comando na docstring do topo do arquivo; nesta fase, chama `build_rows` e `write`, imprime o caminho gravado e devolve 0
- [ ] T012 [US1] Rodar `python -m issta_tcm oracle-import --config config/experiment-minitest.yaml` em `ISSTA_2027/src` e conferir: `inputs/oracle/veredito_minitest.csv` com 38 linhas; colunas `edit_key` a `target` iguais, na mesma ordem, às de `inputs/oracle/t1_minitest_rater1.csv`; edições `|1|` com `v1.0`/`v1.1` e `|12|` com `v2.1`/`v2.2`; alguns casos de uso iguais aos blocos de `inputs/oracle/labeling_sheet_minitest.md`

**Checkpoint**: US1 funciona sozinha; o arquivo do Minitest existe

---

## Phase 4: User Story 2 - Conferir o arquivo antes de importar (Priority: P2)

**Goal**: o comando mostra um resumo, avisa linhas sem `<- edited` e falha, sem gravar, se alguma linha seria recusada pelo Veredito

**Independent Test**: teste automático com uma edição de 5.001 caracteres termina com código 1, lista `linha N (edit_key): motivo` e não grava o arquivo (quickstart, seção 5)

### Tests for User Story 2

- [ ] T013 [P] [US2] Testes de `validate` em `ISSTA_2027/src/tests/test_veredito_import.py`, um por regra de `specs/002-csv-oraculo/contracts/import-csv.md`: campo obrigatório vazio (`sistema`, `edit_key`, `uc`, `kind`, `field`, `origin_version`, `target_version`, `origin_usecase`, `target_usecase`); `kind` fora de `ADD`, `DEL`, `MOD`, `META`; `origin` vazio com `kind` diferente de `ADD`; `target` vazio com `kind` diferente de `DEL`; `origin` e `target` com 5.001 caracteres; caso de uso com 40.001 caracteres; rótulo de versão com 21 caracteres; (`sistema`, `edit_key`) repetido gera erro só a partir da 2ª ocorrência; a contagem usa `len()` (pontos de código, com um texto que tenha caracteres fora do BMP); a numeração conta o cabeçalho como linha 1; linha sem `<- edited` nos dois casos de uso gera aviso, não erro; zero linhas gera o aviso "sistema sem edições"
- [ ] T014 [P] [US2] Teste do comando em `ISSTA_2027/src/tests/test_veredito_import.py` chamando `cli.main([...])` com o run falso: com uma edição de 5.001 caracteres, devolve 1, imprime `linha N (edit_key): origin com mais de 5000 caracteres` e não cria nem altera `veredito_<sistema>.csv` (um arquivo anterior continua idêntico); sem erros, devolve 0 e imprime o caminho, o total, as contagens por `kind` e por `field`, os maiores tamanhos e depois os avisos

### Implementation for User Story 2

- [ ] T015 [US2] Implementar a dataclass `Report` (`errors: list[tuple[int, str, str]]`, `warnings: list[str]`, `total`, `by_kind`, `by_field` em ordem alfabética, `max_len` por `origin`, `target`, `origin_usecase`, `target_usecase`) e `validate(rows) -> Report` em `ISSTA_2027/src/issta_tcm/evaluation/veredito_import.py`, com exatamente as regras do contrato e as mensagens em português no formato `<campo> com mais de <limite> caracteres`, `<campo> obrigatório`, `kind inválido: <valor>`, `edit_key repetida no arquivo`
- [ ] T016 [US2] Implementar `format_report(report) -> str` em `ISSTA_2027/src/issta_tcm/evaluation/veredito_import.py`: total; contagens por `kind` e por `field`; maiores tamanhos; avisos; e, havendo erros, uma linha `linha N (edit_key): motivo` por erro
- [ ] T017 [US2] Completar `cmd_oracle_import` em `ISSTA_2027/src/issta_tcm/cli.py`: `build_rows` -> `validate` -> com erros, imprime `format_report` e devolve 1 sem chamar `write`; sem erros, chama `write` e imprime o caminho e o `format_report`; artefato do pipeline ausente (`MissingStageOutput` ou chaves sem pedido T1) devolve 1 com a orientação "rode o S5"

**Checkpoint**: US1 e US2 funcionam; o comando nunca grava um arquivo que o Veredito recusaria

---

## Phase 5: Polish & Cross-Cutting Concerns

- [ ] T018 [P] Acrescentar `oracle-import` à lista de comandos do oráculo em `ISSTA_2027/src/README.md`, com uma frase sobre a saída `inputs/oracle/veredito_<sistema>.csv`
- [ ] T019 Rodar `python -m pytest -q` em `ISSTA_2027/src`: os 73 testes anteriores e os novos passam
- [ ] T020 Rodar o quickstart (`specs/002-csv-oraculo/quickstart.md`, seções 2 a 4): 38 linhas, código 0, nenhum aviso de linha sem `<- edited` (SC-002), tempo bem abaixo de 1 minuto (SC-001) e o mesmo hash em duas gerações seguidas
- [ ] T021 Conferir o FR-008 em `ISSTA_2027/src/inputs/oracle/veredito_minitest.csv`: nenhum identificador `E-` seguido de dígitos, nenhuma coluna `change_impact`, `maintenance_operation` ou `comment`, e nenhuma saída de técnica

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: sem dependências
- **Foundational (Phase 2)**: depende de T002 e T003; bloqueia as histórias
- **US1 (Phase 3)**: depende da fase 2
- **US2 (Phase 4)**: depende da fase 2; usa `build_rows` (T009) e o comando (T011) da US1, mas `validate` e seus testes (T013, T015, T016) podem ser feitos em paralelo com a US1
- **Polish (Phase 5)**: depende das duas histórias

### Within Each User Story

- Testes primeiro, falhando
- `build_rows` antes de `write`, e os dois antes do comando
- `validate` antes de `format_report`, e os dois antes de completar o comando (T017)

### Parallel Opportunities

- T003 em paralelo com T002 (arquivos diferentes)
- T006 em paralelo com T004 e T005, depois que as assinaturas existirem
- T007 e T008 juntos; T013 e T014 juntos
- T013, T015 e T016 (US2) em paralelo com T009 a T012 (US1): `validate` só depende do formato das linhas
- T018 em paralelo com T019 a T021

---

## Parallel Example: User Story 1

```text
Tarefa: "Teste de build_rows em ISSTA_2027/src/tests/test_veredito_import.py" (T007)
Tarefa: "Teste de write em ISSTA_2027/src/tests/test_veredito_import.py" (T008)
```

## Parallel Example: User Story 2

```text
Tarefa: "Testes de validate em ISSTA_2027/src/tests/test_veredito_import.py" (T013)
Tarefa: "Teste do comando em ISSTA_2027/src/tests/test_veredito_import.py" (T014)
```

T007/T008 e T013/T014 ficam no mesmo arquivo de teste: em paralelo só se cada um acrescentar funções sem mexer nas dos outros.

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Fases 1 e 2
2. Fase 3 (US1): o arquivo do Minitest é gerado
3. **Parar e validar**: quickstart, seções 2 a 4
4. O arquivo já serve para a tela do item (spec 008) e para os testes da importação (spec 005)

### Incremental Delivery

1. Setup e base
2. US1: arquivo gerado e conferido
3. US2: resumo, avisos e recusa antes de gravar
4. Polish: README, suíte completa, quickstart

---

## Notes

- O SC-003 (o arquivo passa pela prévia da importação sem erro) só é conferido na spec 005, no ensaio local.
- Pela constitution 2.6.0 (Princípio IV), o `veredito_minitest.csv` gerado aqui é o único dado de itens do ensaio local, dos testes e do piloto. A cópia para `Veredito/backend/src/test/resources/minitest/` é feita quando o backend existir (specs 003 e 005), e é trocada no mesmo commit sempre que o pipeline gerar o arquivo de novo.
- Nenhuma tarefa chama modelos; a regra de execução sequencial do `ISSTA_2027` não se aplica.
- Fazer commit no repositório do `ISSTA_2027` ao fim de cada fase.
