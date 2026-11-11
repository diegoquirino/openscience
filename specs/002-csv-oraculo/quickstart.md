# Quickstart: validar o CSV de importação

## Pré-requisitos

- Repositório `ISSTA_2027` com o run do Minitest já processado até o S5
  (`src/runs/minitest-piloto/s5_blind/public/requests_T1.jsonl` existe).
- Python do pesquisador: `C:\Users\diego\AppData\Local\Python\bin\python.exe` com o pacote
  instalado em modo editável (`pip install -e .[dev]`).

Todos os comandos rodam em `ISSTA_2027/src`.

## 1. Testes automáticos

```powershell
python -m pytest tests/test_veredito_import.py -q
```

Esperado: todos passam, cobrindo a divisão do contexto, o aviso de caso de uso ausente, os rótulos
de versão, a validação (cada regra do [contrato](contracts/import-csv.md)), aspas e quebras de linha
e a geração duas vezes igual.

## 2. Gerar o arquivo do Minitest (US1, SC-001)

```powershell
python -m issta_tcm oracle-import --config config/experiment-minitest.yaml
```

Esperado, em poucos segundos:

- `inputs/oracle/veredito_minitest.csv` gravado;
- resumo com 38 linhas, contagens por `kind` e `field` e maiores tamanhos;
- código de saída 0, sem avisos de linha sem `<- edited` (SC-002).

## 3. Conferir contra o pipeline (US1, cenários 1 a 3)

- As colunas `edit_key` … `target` batem, linha a linha e na mesma ordem, com
  `inputs/oracle/t1_minitest_rater1.csv`.
- Para algumas linhas, `origin_usecase` e `target_usecase` são iguais aos dois blocos do item
  correspondente em `inputs/oracle/labeling_sheet_minitest.md` (fora o aviso traduzido).
- `origin_version` e `target_version` seguem os passos da configuração: as edições `|1|` têm `v1.0` e
  `v1.1`, e as `|12|` têm `v2.1` e `v2.2`.

## 4. Determinismo (cenário 6)

Gerar de novo e comparar o hash:

```powershell
(Get-FileHash inputs/oracle/veredito_minitest.csv).Hash
python -m issta_tcm oracle-import --config config/experiment-minitest.yaml
(Get-FileHash inputs/oracle/veredito_minitest.csv).Hash
```

Esperado: os dois hashes são iguais.

## 5. Falha de validação (US2)

O teste automático com uma edição de 5.001 caracteres cobre este caso. Esperado: código 1,
`linha N (edit_key): origin com mais de 5000 caracteres` e nenhum arquivo gravado.

## 6. Prévia no Veredito (SC-003)

Feito na spec 005: importar `veredito_minitest.csv` no ensaio local (Docker) e conferir que a prévia mostra 38
incluídos, 0 ignorados e 0 com erro.
