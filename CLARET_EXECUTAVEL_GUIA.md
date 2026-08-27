# Guia Oficial: Construção e Uso do Executável Standalone CLARET (UFCG / SPLab) - Java 26
### Geração Automatizada de Casos de Teste e Modelos MBT a partir de Especificações `.claret` e `.dsl` em LTS, ALTS, XLSX, TXT, DOCX, ODT e XML (TestLink)

---

## 1. Visão Geral e Contexto

O **CLARET** (*CentraL Artifact for Requirement Engineering and model-based Testing*) foi criado no **SPLab** (*Software Practices Laboratory*) da **Universidade Federal de Campina Grande (UFCG)**.

O projeto **`claret-generator`** é compilado e executado em **Java 26** como uma ferramenta de linha de comando (*Standalone CLI / Fat JAR*) que elimina qualquer dependência do ambiente Eclipse, oferecendo:
- Compilação moderna com **Java 26** (`javac [release 26]`).
- Processamento nativo de especificações com extensões **`.claret`** e **`.dsl`**.
- Organização padronizada com pastas **`src/`** e **`output/`** no mesmo nível hierárquico, **movendo** os arquivos de entrada processados para dentro de `src/`.
- Suporte a exportação em texto tabulado (**TXT**) no mesmo padrão estruturado do Excel (**XLSX**).
- Formatação refinada de documentos Word (**DOCX**) e OpenDocument (**ODT**) com hierarquia de estilos `Title`, `Subtitle`, `Heading 1` e `Heading 2`.
- Planilhas **XLSX** individuais com cabeçalho limpo e planilhas consolidadas (`all_usecases`) com versionamento e caso de uso identificados por caso de teste.
- Algoritmos formais de cobertura e redução: `gt` (Transition Coverage), `gtp` (Transition Pair Coverage), `art` (Adaptive Random Testing por Jaccard) e `complete` (Complete Path Coverage).

---

## 2. Estrutura de Diretórios Gerada

```text
<diretório_alvo>/                         <-- Pasta do caso de uso (ex: 20150617)
│
├── src/                                  <-- Diretório com fontes movidos (mesmo nível que output/)
│   └── *.claret / *.dsl                  <-- Arquivos de especificação processados com sucesso
│
└── output/                               <-- Diretório com artefatos gerados (mesmo nível que src/)
    ├── tgf/                              <-- Modelos TGF (.tgf e -annotated.tgf)
    ├── xlsx/                             <-- Planilhas XLSX (--GT-, --GTP-, --ART-, --Complete-)
    ├── txt/                              <-- Especificações em texto tabulado (.txt)
    ├── docx/                             <-- Relatórios Microsoft Word (.docx)
    ├── odt/                              <-- Relatórios ODT formatados (.odt)
    ├── alts/                             <-- Especificações formais ALTS
    └── xml/                              <-- Suítes XML para TestLink
```

---

## 3. Padrão de Formatação dos Artefatos Gerados

### XLSX (Planilhas Excel) e TXT (Texto Tabulado)
- **Arquivo Individual (`<UseCase>--GT-.xlsx` e `<UseCase>--GT-.txt`)**:
  - Cabeçalho: Contém `System: <Nome>`, `Use Case: <Nome>`, `Version: <v>`, `Suite Type: <Tipo>`, `Size: N`, `Creation Date: <data>`.
  - Casos de Teste: Iniciam diretamente com `Test Case ID: TC...` (não repete o nome do caso de uso antes de cada caso de teste).
- **Arquivo Consolidado (`all_usecases--GT-.xlsx` e `all_usecases--GT-.txt`)**:
  - Cabeçalho: Identifica `Use Case: All Use Cases` agrupado por abas/seções de `System Name` (sem versão global no cabeçalho).
  - Casos de Teste: Cada caso de teste possui uma linha dedicada acima do `Test Case ID` contendo `Use Case: <Nome>` e `Version: <v>`.

### DOCX (Word) e ODT (OpenDocument)
- **Hierarquia Tipográfica e de Estilos**:
  - **Título (Title)**: 20pt Negrito (Azul Marinho `#1A365D`)
  - **Subtítulo (Subtitle)**: 10pt Itálico (Cinza `#555555`)
  - **Título 1 (Heading 1)**: 15pt Negrito (`Use Case: <Nome> (v<Versão>)`)
  - **Título 2 (Heading 2)**: 12pt Negrito (Azul `#2B6CB0` - `[<TC_ID>] <Descrição>`)
  - **Metadados**: Parágrafos formatados com rótulos em negrito (`System:`, `Preconditions:`, `Postconditions:`).
  - **Tabela de Passos**: Cabeçalho azul com texto em branco e bordas finas.

---

## 4. Critérios de Cobertura (`-c` / `--coverage`)

| Parâmetro CLI (`-c`) | Nome da Suíte no `.xlsx` (`Suite Type`) | Algoritmo e Funcionamento | Sufixo Gerado no `.xlsx` e `.txt` |
| :--- | :--- | :--- | :--- |
| **`gt`** *(Padrão)* | **Reduced (Greedy Heuristic - Transition Coverage)** | **Greedy Transition Coverage:** Aplica heurística gulosa para selecionar o menor conjunto possível de casos de teste que cubra todas as transições do grafo do caso de uso. | `--GT-.xlsx` / `--GT-.txt` |
| **`gtp`** | **Reduced (Greedy Heuristic - Transition Pair Coverage)** | **Greedy Transition Pair Coverage:** Aplica heurística gulosa para selecionar a suíte que cobre todas as sequências de pares de transições consecutivas ($T_i \to T_j$). | `--GTP-.xlsx` / `--GTP-.txt` |
| **`art`** | **Reduced (Adaptive Random Testing by Jaccard Distance)** | **Adaptive Random Testing (ART):** Algoritmo de Teste Aleatório Adaptativo que utiliza a distância/dissimilaridade de Jaccard entre caminhos de teste para maximizar a diversidade funcional da suíte. | `--ART-.xlsx` / `--ART-.txt` |
| **`complete`** | **Complete Test Suite** | **Complete Path Coverage:** Exploração de todos os caminhos no grafo de testes sem aplicar algoritmos de redução. | `--Complete-.xlsx` / `--Complete-.txt` |
| **`basic-only`** | **Basic Flow Only (Happy Path)** | **Happy Path:** Gera exclusivamente o caso de teste do fluxo básico (*Smoke Testing*). | `--Basic-.xlsx` / `--Basic-.txt` |
| **`all-branches`** | **Reduced (Decision Branches)** | **Branch Coverage:** Foca nos caminhos de desvios alternativos e exceções. | `--Branches-.xlsx` / `--Branches-.txt` |

---

## 5. Guia de Execução Standalone (Java 26)

```bash
# 1. Processar pasta com arquivos .claret e .dsl (Move para src/ e gera output/tgf/, output/xlsx/, output/txt/, output/docx/, etc.)
java -jar target/claret-generator.jar -i 20150617 -o 20150617/output -f all -c gt

# 2. Gerar relatório TXT tabulado
java -jar target/claret-generator.jar -i 20150617 -o 20150617/output -f txt -c gt

# 3. Gerar relatório DOCX (Word)
java -jar target/claret-generator.jar -i 20150617 -o 20150617/output -f docx -c gt

# 4. Gerar com Adaptive Random Testing (art)
java -jar target/claret-generator.jar -i 20150617 -o 20150617/output -f xlsx -c art

# 5. Executar gerando arquivos na raiz da pasta de destino (modo plano)
java -jar target/claret-generator.jar -i 20150617 -o 20150617/output --flat -f all -c gt
```

---

## 6. Como Importar os Testes no TestLink

1. Faça login no TestLink e selecione o **Test Project**.
2. Clique em **Test Specification** e selecione a suíte de testes de destino.
3. No painel direito, clique no menu de engrenagem (**Actions**) $\rightarrow$ **Import**.
4. Selecione o arquivo gerado em `<data>/output/xml/*_testlink.xml`.
5. Confirme o tipo **XML** e clique em **Upload file**.
