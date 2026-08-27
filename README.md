# CLARET Standalone Generator (UFCG / SPLab)
### Gerador Executável de Casos de Teste e Modelos MBT a partir de Especificações `.claret` e `.dsl` (Java 26)

Esta ferramenta foi desenvolvida no contexto do **CLARET** (*CentraL Artifact for Requirement Engineering and model-based Testing* - SPLab / UFCG).

O projeto **`claret-generator`** opera de forma **100% Standalone (Fat JAR / CLI)** compilado em **Java 26**, suportando os algoritmos formais de cobertura MBT, a organização com diretórios `src/` e `output/` no mesmo nível (movendo os arquivos processados para `src/`) e suporte às extensões `.claret` e `.dsl`.

---

## 1. Estrutura de Diretórios do Projeto `claret-generator`

```text
claret-generator/                         <-- Raiz do projeto
├── pom.xml                               <-- Configuração Maven (Java 26, POI, Commons CLI, JUnit 5)
├── README.md                             <-- Documentação principal em português
├── CLARET_EXECUTAVEL_GUIA.md             <-- Guia oficial detalhado em português (pt-BR)
├── CLARET_EXECUTABLE_GUIDE.md            <-- Guia técnico completo em inglês (en-US)
├── .gitignore                            <-- Arquivos ignorados pelo Git
│
├── samples/                              <-- Especificações de teste de exemplo
│   ├── login-minitest.claret             <-- Sintaxe padrão Xtext
│   └── login-minitest-alternative-format-dsl.claret <-- Sintaxe alternativa DSL / Groovy
│
├── src/
│   ├── main/java/br/edu/ufcg/splab/claret/
│   │   ├── Main.java                     <-- Ponto de entrada CLI e orquestrador de diretórios
│   │   ├── model/                        <-- Modelos de domínio e enums de cobertura
│   │   │   ├── ClaretSystem.java         <-- Modelo do sistema
│   │   │   ├── UseCase.java              <-- Modelo do caso de uso
│   │   │   ├── Step.java                 <-- Modelo de passo com anotações af/ef/bfs
│   │   │   ├── AlternativeFlow.java      <-- Fluxos alternativos
│   │   │   ├── ExceptionFlow.java        <-- Fluxos de exceção
│   │   │   ├── Actor.java                <-- Atores do caso de uso
│   │   │   ├── TestCase.java             <-- Caso de teste com useCaseVersion
│   │   │   ├── TestStep.java             <-- Passo de teste com ação e resultado esperado
│   │   │   └── CoverageCriteria.java     <-- Enums dos critérios MBT (gt, gtp, art, complete)
│   │   ├── parser/
│   │   │   └── ClaretParser.java         <-- Parser flexível (.claret e .dsl)
│   │   ├── engine/
│   │   │   └── ClaretProcessor.java      <-- Motor de cobertura e redução (Greedy, ART Jaccard)
│   │   └── generator/
│   │       ├── XlsxGenerator.java        <-- Gerador de planilhas de teste (.xlsx)
│   │       ├── TxtGenerator.java         <-- Gerador de especificações em texto tabulado (.txt)
│   │       ├── LtsGenerator.java         <-- Gerador de modelos TGF (.tgf e -annotated.tgf)
│   │       ├── AltsGenerator.java        <-- Gerador de especificações ALTS (.alts)
│   │       ├── DocxGenerator.java        <-- Gerador Microsoft Word (.docx)
│   │       ├── OdtGenerator.java         <-- Gerador OpenDocument Text (.odt)
│   │       └── TestLinkXmlGenerator.java <-- Gerador de suítes XML para TestLink
│   │
│   └── test/
│       ├── java/br/edu/ufcg/splab/claret/
│       │   ├── ClaretParserTest.java     <-- Testes do parser em ambos os formatos
│       │   ├── ClaretProcessorTest.java  <-- Testes dos critérios de cobertura
│       │   ├── XlsxGeneratorTest.java    <-- Testes da planilha XLSX
│       │   ├── TxtGeneratorTest.java     <-- Testes da geração TXT tabulada
│       │   ├── LtsGeneratorTest.java     <-- Testes do modelo TGF
│       │   ├── AltsGeneratorTest.java    <-- Testes de especificação ALTS
│       │   ├── DocxGeneratorTest.java    <-- Testes de geração DOCX
│       │   ├── OdtGeneratorTest.java     <-- Testes de geração ODT
│       │   ├── TestLinkXmlGeneratorTest.java <-- Testes do XML TestLink
│       │   └── ClaretDirectoryTreeAndExtensionsTest.java <-- Validação da árvore de pastas e extensões
│       └── resources/
│           ├── login-minitest.claret
│           └── login-minitest-alternative-format-dsl.claret
│
├── target/                               <-- Diretório de build gerado pelo Maven (excluído no clean)
│   └── claret-generator.jar              <-- Fat JAR executável standalone gerado
│
├── src/                                  <-- Diretório de fontes processados (mesmo nível que output/)
│   └── *.claret / *.dsl                  <-- Arquivos de especificação movidos após processamento
│
└── output/                               <-- Diretório de saída gerado (mesmo nível que src/)
    ├── tgf/                              <-- Modelos de transição (.tgf e -annotated.tgf)
    ├── xlsx/                             <-- Planilhas de teste (--GT-, --GTP-, --ART-, --Complete-)
    ├── txt/                              <-- Especificações em texto tabulado (.txt)
    ├── docx/                             <-- Relatórios executivos Microsoft Word (.docx)
    ├── odt/                              <-- Relatórios executivos OpenDocument Text (.odt)
    ├── alts/                             <-- Especificações formais ALTS
    └── xml/                              <-- Suítes XML para importação no TestLink
```

---

## 2. Abordagens de Cobertura (`-c` / `--coverage`)

| Parâmetro CLI (`-c`) | Nome da Suíte (`Suite Type`) | Algoritmo e Descrição | Sufixo no `.xlsx` e `.txt` |
| :--- | :--- | :--- | :--- |
| **`gt`** *(Padrão)* | **Reduced (Greedy Heuristic - Transition Coverage)** | **Greedy Transition Coverage:** Aplica heurística gulosa para selecionar o menor conjunto possível de casos de teste que cubra todas as transições do grafo. | `--GT-.xlsx` / `--GT-.txt` |
| **`gtp`** | **Reduced (Greedy Heuristic - Transition Pair Coverage)** | **Greedy Transition Pair Coverage:** Seleciona o conjunto de testes que cobre todas as sequências de pares de transições consecutivas ($T_i \to T_j$). | `--GTP-.xlsx` / `--GTP-.txt` |
| **`art`** | **Reduced (Adaptive Random Testing by Jaccard Distance)** | **Adaptive Random Testing (ART):** Algoritmo adaptativo baseado na distância de Jaccard entre caminhos para maximizar diversidade. | `--ART-.xlsx` / `--ART-.txt` |
| **`complete`** | **Complete Test Suite** | **Complete Path Coverage:** Gera a suíte completa de todos os caminhos explorados no grafo sem redução. | `--Complete-.xlsx` / `--Complete-.txt` |
| **`basic-only`** | **Basic Flow Only (Happy Path)** | **Happy Path:** Gera exclusivamente o caso de teste do fluxo básico. | `--Basic-.xlsx` / `--Basic-.txt` |
| **`all-branches`** | **Reduced (Decision Branches)** | **Branch Coverage:** Foca nos desvios alternativos e de exceção. | `--Branches-.xlsx` / `--Branches-.txt` |

---

## 3. Padrão de Formatação dos Artefatos Gerados

### XLSX (Planilhas Excel) e TXT (Texto Tabulado)
- **Arquivo Individual (`<UseCase>--GT-.xlsx` e `<UseCase>--GT-.txt`)**:
  - Cabeçalho: Contém `System: <Nome>`, `Use Case: <Nome>`, `Version: <v>`, `Suite Type: <Tipo>`, `Size: N`, `Creation Date: <data>`.
  - Casos de Teste: Iniciam diretamente com `Test Case ID: TC...` (não repete o nome do caso de uso antes de cada caso de teste).
- **Arquivo Consolidado (`all_usecases--GT-.xlsx` e `all_usecases--GT-.txt`)**:
  - Cabeçalho: Identifica `Use Case: All Use Cases` agrupado por abas de `System Name` (sem versão global no cabeçalho).
  - Casos de Teste: Cada caso de teste possui uma linha dedicada acima do `Test Case ID` contendo `Use Case: <Nome>` e `Version: <v>`.

### DOCX (Word) e ODT (OpenDocument)
- **Hierarquia Tipográfica e de Estilos**:
  - **Título (Title)**: 20pt Negrito (Azul Marinho `#1A365D`)
  - **Subtítulo (Subtitle)**: 10pt Itálico (Cinza `#555555`)
  - **Título 1 (Heading 1)**: 15pt Negrito (`Use Case: <Nome> (v<Versão>)`)
  - **Título 2 (Heading 2)**: 12pt Negrito (Azul `#2B6CB0` - `[<TC_ID>] <Descrição>`)
  - **Metadados**: Parágrafos formatados com rótulos em negrito (`System:`, `Preconditions:`, `Postconditions:`).
  - **Tabela de Passos**: Cabeçalho azul com texto em branco e bordas finas.

### XML (TestLink)
- **Suíte Consolidada (`all_usecases_testlink.xml`)**: Cria sub-suítes `<testsuite name="<CasoDeUso>">` agrupadas sob a raiz `<testsuite name="Consolidated Test Suite">`.

---

## 4. Exemplos de Execução Standalone (Java 26)

Após executar `mvn package`, o fat JAR executável estará disponível em `target/claret-generator.jar`:

```bash
# 1. Processar pasta com arquivos .claret e .dsl (Move para src/ e gera output/tgf/, output/xlsx/, output/txt/, output/docx/, etc.)
java -jar target/claret-generator.jar -i 20150617 -o 20150617/output -f all -c gt

# 2. Gerar relatório TXT tabulado
java -jar target/claret-generator.jar -i 20150617 -o 20150617/output -f txt -c gt

# 3. Gerar relatório DOCX (Word)
java -jar target/claret-generator.jar -i 20150617 -o 20150617/output -f docx -c gt

# 4. Gerar planilha com cobertura por Pares de Transições (GTP)
java -jar target/claret-generator.jar -i 20150617 -o 20150617/output -f xlsx -c gtp

# 5. Gerar com Adaptive Random Testing (ART por Distância de Jaccard)
java -jar target/claret-generator.jar -i 20150617 -o 20150617/output -f xlsx -c art

# 6. Gerar arquivos diretamente na pasta de saída (formato plano, sem subpastas)
java -jar target/claret-generator.jar -i 20150617 -o 20150617/output --flat -f all -c gt
```

---

## 5. Como Importar no TestLink

1. Acesse o **TestLink** e selecione o **Test Project**.
2. Clique no menu superior **Test Specification** e selecione a suíte na árvore lateral.
3. No painel direito, clique no ícone de engrenagem (**Actions**) $\rightarrow$ **Import**.
4. Selecione o arquivo gerado em `<data>/output/xml/*_testlink.xml`.
5. Confirme o tipo **XML** e clique em **Upload file**.
