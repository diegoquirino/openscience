# Research: Gerar o CSV de importação a partir do pipeline

Levantamento feito em 2026-11-11 sobre o run `runs/minitest-piloto` (38 edições, 12 passos de
versão, de `minitest_v1.0` a `minitest_v2.2`).

## 1. De onde vêm os casos de uso completos

- **Decision**: usar o campo `context` dos pedidos T1 (`s5_blind/public/requests_T1.jsonl`), que tem
  a forma `### origin version\n<caso de uso>\n### target version\n<caso de uso>`, e dividi-lo no
  cabeçalho `### target version`. O pedido de cada edição é achado por
  `edit_key → E-xxx` em `s5_blind/_private/edits_index.json` (o mesmo caminho de
  `oracle._key_to_eid`).
- **Rationale**: é exatamente o texto que a planilha `labeling_sheet_<sistema>.md` mostra e que as
  técnicas recebem: `claret.render(uc, include_metadata=False, mark=...)`, com `<- edited` nos
  elementos tocados pela edição, em cada versão (`s5_blind._context`). Reaproveitar o texto
  garante o FR-004 sem duplicar a lógica de marcação. No Minitest, os 38 contextos têm exatamente
  um cabeçalho `### target version`, todos têm ao menos uma linha marcada e o maior lado tem 2.186
  caracteres.
- **Alternatives considered**: renderizar de novo com `claret.render` a partir de
  `s1_version/<tag>/src` (duplicaria `_context` e poderia divergir da planilha); ler a planilha
  `.md` (formato de apresentação, frágil para parsing).

## 2. Caso de uso que não existe numa das versões

- **Decision**: o S5 já escreve `(the use case does not exist in this version)` no lado ausente. O
  comando troca esse texto exato por `(este caso de uso não existe nesta versão)`.
- **Rationale**: atende ao FR-005 (aviso curto, nunca vazio) e a interface do Veredito é em
  português. Os casos de uso em si continuam em inglês, como no pipeline.
- **Alternatives considered**: manter o texto em inglês (mistura idiomas no modal do Veredito);
  deixar vazio (recusado pela importação, FR-052).

## 3. Rótulos de versão

- **Decision**: `origin_version = "v" + label` da versão anterior e `target_version = "v" + label`
  da versão modificada do passo `k` da edição (`cfg.subject.steps()`; `k` vem de
  `edits_index.json`). No Minitest: `v1.0`…`v2.2`.
- **Rationale**: a versão declarada dentro do caso de uso fica atrás da release. Por exemplo, as
  releases `v1.1` e `v1.2` ainda declaram `1.0`, e uma edição do passo 2 apareceria como
  `1.0 → 1.0`. O rótulo da release é sempre distinto entre as duas versões e é o mesmo usado nos
  nomes de passo do pipeline (`step_label`). Além disso, o S5 remove o metadado `version` do texto
  mostrado.
- **Alternatives considered**: versão declarada no caso de uso, com a release só como reserva
  (cenário 4 original da spec). Foi descartada pelo problema acima. A spec foi ajustada: o cenário 4
  e o caso de borda agora usam o rótulo da release.

## 4. Colunas copiadas de `edits.csv`

- **Decision**: `edit_key`, `uc`, `kind`, `field`, `origin` e `target` são lidos de
  `s3_diff/edits.csv` com `oracle._cell`, como em `oracle._items`, que gera os
  `t1_<sistema>_rater*.csv`. A ordem das linhas é a de `edits.csv`. `sistema` é
  `cfg.subject.key`, o mesmo de `t1_<sistema>_*.csv`.
- **Rationale**: as respostas exportadas pelo Veredito (`t1_<sistema>_raterN.csv`, FR-057c da 001)
  precisam casar com as linhas dos arquivos do avaliador. Copiar do mesmo jeito que o
  `oracle-template` garante isso. `_cell` só tira espaços nas pontas e troca `nan` por vazio.
- **Alternatives considered**: ler `edits.json` (tem os mesmos campos, mas o `.csv` é a referência
  do oráculo).

## 5. Validação antes de gravar

- **Decision**: espelhar em Python as regras de recusa da importação (FR-052 e FR-053 da 001):
  - campos obrigatórios;
  - `kind` dentro de `ADD`, `DEL`, `MOD`, `META`;
  - `origin` obrigatório quando `kind` ≠ `ADD`, e `target` quando `kind` ≠ `DEL`;
  - `origin` e `target` com até 5.000 caracteres;
  - casos de uso completos com até 40.000 caracteres;
  - rótulos de versão com até 20 caracteres;
  - (`sistema`, `edit_key`) único no arquivo.

  Com erro, o comando não grava o arquivo, lista linha e motivo e sai com código 1. Uma linha sem
  `<- edited` nos dois lados gera só um aviso. A contagem de caracteres usa `len()` do Python (pontos
  de código), e o Veredito deve contar igual (`codePointCount` em Java).
- **Rationale**: atende à US2 e ao FR-006 e evita descobrir o erro só na prévia da 005. Não gravar
  impede importar por engano um arquivo velho junto com um novo inválido.
- **Alternatives considered**: gravar mesmo com erro e só avisar (o arquivo inválido ficaria na
  pasta).

## 6. Formato do arquivo

- **Decision**: `utils.write_csv` (`csv.DictWriter`, UTF-8 sem BOM, `newline=""`): registros
  separados por `\r\n`, aspas só quando preciso, aspas internas duplicadas, `\n` preservado dentro
  dos campos. Nome do arquivo: `veredito_<sistema>.csv`, ao lado dos `t1_*`.
- **Rationale**: é RFC 4180 (FR-002), é o que o pipeline já usa e é determinístico para a mesma
  entrada (FR-007).
- **Alternatives considered**: `import_<sistema>.csv` (nome genérico demais na pasta do oráculo).

## 7. Riscos

- **Pedidos T1 desatualizados**: se `edits.csv` mudar sem rodar o S5 de novo, alguma `edit_key` fica
  sem pedido. O comando falha com a mensagem "rode o S5" (via `MissingStageOutput` ou com a lista de
  chaves sem contexto).
- **Mudança no cabeçalho do contexto**: se o S5 mudar `### origin version` ou `### target version`,
  a divisão quebra. Há um teste que gera o contexto com o próprio `_context` do S5 e confere a divisão.
