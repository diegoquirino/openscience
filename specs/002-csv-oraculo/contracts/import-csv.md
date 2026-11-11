# Contrato: arquivo de importação do Veredito

Contrato entre o pipeline (produtor, spec 002) e a importação do Veredito (consumidor, spec 005).
Fonte das regras: Apêndice D, FR-051, FR-052 e FR-053 da [spec 001](../../001-rotulagem-oraculo/spec.md).

## Comando

```text
python -m issta_tcm oracle-import [--config <yaml>] [--set chave=valor ...]
```

- Executado em `ISSTA_2027/src`, depois do S5 do run configurado.
- Saída: `<oracle_dir>/veredito_<sistema>.csv`; `oracle_dir` é `stages.s8_evaluate.oracle_dir`
  (padrão `../inputs/oracle`).
- Sobrescreve o arquivo existente, porque a saída é determinística.
- Saída padrão: caminho gravado e resumo (total, por `kind`, por `field`, maiores tamanhos), depois
  os avisos.
- Código de saída: `0` gravou; `1` alguma linha seria recusada (nada é gravado; erros listados como
  `linha N (edit_key): motivo`) ou faltam artefatos do pipeline.

## Arquivo

- UTF-8 sem BOM; RFC 4180; registros separados por `\r\n`; campos com vírgula, aspas ou quebra de
  linha ficam entre aspas, com aspas internas duplicadas.
- Cabeçalho exato:

```text
sistema,edit_key,uc,kind,field,origin,target,origin_version,target_version,origin_usecase,target_usecase
```

- Uma linha por edição, na ordem de `s3_diff/edits.csv`. Sem edições, sai só o cabeçalho, com
  aviso.

## Regras que o produtor garante e o consumidor confere

| Regra | Produtor (002) | Consumidor (005) |
|---|---|---|
| Colunas obrigatórias não vazias: `sistema`, `edit_key`, `uc`, `kind`, `field`, `origin_version`, `target_version`, `origin_usecase`, `target_usecase` | Falha | Recusa a linha |
| `kind` ? {`ADD`, `DEL`, `MOD`, `META`} | Falha | Recusa |
| `origin` não vazio se `kind` ? `ADD`; `target` não vazio se `kind` ? `DEL` | Falha | Recusa |
| `origin`, `target` ? 5.000 caracteres (pontos de código) | Falha | Recusa |
| `origin_usecase`, `target_usecase` ? 40.000 caracteres | Falha | Recusa |
| `origin_version`, `target_version` ? 20 caracteres | Falha | Recusa |
| (`sistema`, `edit_key`) único no arquivo | Falha na 2ª ocorrência | Recusa a partir da 2ª |
| (`sistema`, `edit_key`) já importado | — | Ignora, sem alterar o item |
| Nenhuma linha `<- edited` nos dois casos de uso | Aviso | Aceita |
| Colunas extras | Não gera | Ignora |

## Conteúdo dos casos de uso

- Sintaxe CLARET canônica sem metadados (`claret.render(uc, include_metadata=False, mark=...)`),
  igual à planilha `labeling_sheet_<sistema>.md`.
- Linhas tocadas pela edição terminam em `   <- edited`.
- Caso de uso ausente na versão: `(este caso de uso não existe nesta versão)`.

## Exemplo (abreviado)

```text
sistema,edit_key,uc,kind,field,origin,target,origin_version,target_version,origin_usecase,target_usecase
minitest,LoginTest|1|1,LoginTest,MOD,step,"…","…",v1.0,v1.1,"systemName ""Email""
usecase ""Login User"" {
  …   <- edited
}","systemName ""Email""
…"
```
