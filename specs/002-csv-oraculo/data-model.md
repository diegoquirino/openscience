# Data Model: Gerar o CSV de importação a partir do pipeline

Não há persistência própria; o comando lê artefatos do pipeline e grava um CSV.

## Entradas

| Artefato | Campos usados | Papel |
|---|---|---|
| `s3_diff/edits.csv` | `edit_key`, `uc`, `kind`, `field`, `origin`, `target` | Uma linha por edição; define a ordem |
| `s5_blind/_private/edits_index.json` | `E-xxx ? {edit_key, k}` | Liga a edição ao pedido T1 e ao passo de versão |
| `s5_blind/public/requests_T1.jsonl` | `request_id` (`E-xxx`), `context` | Casos de uso completos, com `<- edited` |
| Configuração do sistema | `subject.key`, `subject.steps()` ? `(k, versão anterior, versão modificada)` com `label` | `sistema` e rótulos de versão |

## Linha de importação (`ImportRow`)

Um dicionário com as colunas do Apêndice D da spec 001, nesta ordem:

| Campo | Origem | Regra |
|---|---|---|
| `sistema` | `cfg.subject.key` | Obrigatório |
| `edit_key` | `edits.csv` (`oracle._cell`) | Obrigatório; único no arquivo |
| `uc` | `edits.csv` | Obrigatório |
| `kind` | `edits.csv` | Obrigatório; `ADD`, `DEL`, `MOD` ou `META` |
| `field` | `edits.csv` | Obrigatório |
| `origin` | `edits.csv` | Obrigatório se `kind` ? `ADD`; até 5.000 caracteres |
| `target` | `edits.csv` | Obrigatório se `kind` ? `DEL`; até 5.000 caracteres |
| `origin_version` | `"v" + label` da versão anterior do passo `k` | Obrigatório; até 20 caracteres |
| `target_version` | `"v" + label` da versão modificada do passo `k` | Obrigatório; até 20 caracteres |
| `origin_usecase` | Trecho do `context` entre `### origin version` e `### target version` | Obrigatório; até 40.000 caracteres |
| `target_usecase` | Trecho do `context` após `### target version` | Obrigatório; até 40.000 caracteres |

Os dois casos de uso perdem a quebra de linha das pontas (`strip("\n")`). O aviso em inglês do S5
para caso de uso ausente é trocado por `(este caso de uso não existe nesta versão)`.

## Resultado da validação (`Report`)

| Campo | Conteúdo |
|---|---|
| `errors` | Lista de `(linha, edit_key, motivo)`; `linha` conta o cabeçalho como 1, como a prévia do Veredito |
| `warnings` | Linhas sem `<- edited` nos dois casos de uso; arquivo sem nenhuma edição |
| `total` | Número de linhas |
| `by_kind`, `by_field` | Contagens, em ordem alfabética |
| `max_len` | Maior tamanho de `origin`, `target`, `origin_usecase`, `target_usecase` |

Com `errors` não vazio, o arquivo não é gravado.
