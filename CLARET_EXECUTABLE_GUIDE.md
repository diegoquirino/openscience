# Official Guide: CLARET (SPLab / UFCG) Standalone Executable - Java 26
### Model-Based Testing and Test Case Generation from `.claret` and `.dsl` Specifications in LTS, ALTS, XLSX, TXT, DOCX, ODT, and XML (TestLink)

---

## 1. Overview and Context (CLARET / SPLab / UFCG)

**CLARET** (*CentraL Artifact for Requirement Engineering and model-based Testing*) was developed at the **SPLab** (*Software Practices Laboratory*) of the **Federal University of Campina Grande (UFCG)**.

The **`claret-generator`** standalone project is compiled and executed in **Java 26**, providing an autonomous CLI environment without Eclipse dependencies:
- Built and compiled with **Java 26** (`javac [release 26]`).
- Native processing of both **`.claret`** and **`.dsl`** file extensions.
- Standardized directory layout with **`src/`** and **`output/`** at the same hierarchy level, moving successfully processed files into `src/`.
- Support for tabulated text (**TXT**) export mirroring the structured layout of Excel (**XLSX**).
- Refined formatting for Microsoft Word (**DOCX**) and OpenDocument (**ODT**) documents with a hierarchical typography structure: `Title`, `Subtitle`, `Heading 1`, and `Heading 2`.
- Formatted **XLSX** spreadsheets: individual use case sheets with clean headers and consolidated sheets (`all_usecases`) with use case names and versions identified per test case.
- Formal MBT coverage and reduction algorithms: `gt` (Transition Coverage), `gtp` (Transition Pair Coverage), `art` (Adaptive Random Testing by Jaccard Distance), and `complete` (Complete Path Coverage).

---

## 2. Generated Directory Structure

```text
<target_directory>/                       <-- Use case folder (e.g., 20150617)
│
├── src/                                  <-- Processed source specifications (sibling to output/)
│   └── *.claret / *.dsl                  <-- Source files moved after successful processing
│
└── output/                               <-- Generated test artifacts (sibling to src/)
    ├── tgf/                              <-- Graph models (.tgf and -annotated.tgf)
    ├── xlsx/                             <-- Excel suites (--GT-, --GTP-, --ART-, --Complete-)
    ├── txt/                              <-- Tabulated text specifications (.txt)
    ├── docx/                             <-- Microsoft Word reports (.docx)
    ├── odt/                              <-- Formatted OpenDocument Text reports (.odt)
    ├── alts/                             <-- ALTS formal state specifications
    └── xml/                              <-- TestLink importable XML suites
```

---

## 3. Artifact Formatting Standards

### XLSX (Excel Spreadsheets) & TXT (Tabulated Text)
- **Single Use Case File (`<UseCase>--GT-.xlsx` / `<UseCase>--GT-.txt`)**:
  - Header: Contains `System: <Name>`, `Use Case: <Name>`, `Version: <v>`, `Suite Type: <Type>`, `Size: N`, `Creation Date: <date>`.
  - Test Cases: Start directly with `Test Case ID: TC...` (use case name is not redundantly repeated before each test case).
- **Consolidated File (`all_usecases--GT-.xlsx` / `all_usecases--GT-.txt`)**:
  - Header: Displays `Use Case: All Use Cases` grouped into sheets/sections by `System Name` (no global version in header).
  - Test Cases: Each test case has a line immediately above `Test Case ID` displaying `Use Case: <Name>` and `Version: <v>`.

### DOCX (Word) & ODT (OpenDocument)
- **Typographic & Style Hierarchy**:
  - **Title**: 20pt Bold (Navy Blue `#1A365D`)
  - **Subtitle**: 10pt Italic (Gray `#555555`)
  - **Heading 1**: 15pt Bold (`Use Case: <Name> (v<Version>)`)
  - **Heading 2**: 12pt Bold (Accent Blue `#2B6CB0` - `[<TC_ID>] <Summary>`)
  - **Metadata**: Paragraphs with bold labels (`System:`, `Preconditions:`, `Postconditions:`).
  - **Step Tables**: Accent header background with bold white text and thin borders.

---

## 4. Supported Coverage Approaches (`-c` / `--coverage`)

| CLI Option (`-c`) | Suite Name in `.xlsx` (`Suite Type`) | Algorithm and Description | Generated Suffix |
| :--- | :--- | :--- | :--- |
| **`gt`** *(Default)* | **Reduced (Greedy Heuristic - Transition Coverage)** | **Greedy Transition Coverage:** Applies a greedy heuristic algorithm to select the minimal test suite that covers every transition (edge) in the use case graph. | `--GT-.xlsx` / `--GT-.txt` |
| **`gtp`** | **Reduced (Greedy Heuristic - Transition Pair Coverage)** | **Greedy Transition Pair Coverage:** Selects test paths that cover every consecutive pair of transitions ($T_i \to T_j$). | `--GTP-.xlsx` / `--GTP-.txt` |
| **`art`** | **Reduced (Adaptive Random Testing by Jaccard Distance)** | **Adaptive Random Testing (ART):** Uses Jaccard distance/dissimilarity between test paths to maximize functional diversity and early fault detection. | `--ART-.xlsx` / `--ART-.txt` |
| **`complete`** | **Complete Test Suite** | **Complete Path Coverage:** Generates all explored graph paths from initial state to terminal states without applying reduction. | `--Complete-.xlsx` / `--Complete-.txt` |
| **`basic-only`** | **Basic Flow Only (Happy Path)** | **Happy Path:** Generates only the test case for the basic flow (*Smoke Testing*). | `--Basic-.xlsx` / `--Basic-.txt` |
| **`all-branches`** | **Reduced (Decision Branches)** | **Branch Coverage:** Focuses on conditional branches (alternative flows and exception flows). | `--Branches-.xlsx` / `--Branches-.txt` |

---

## 5. Standalone CLI Usage Guide (Java 26)

```bash
# 1. Process directory containing .claret and .dsl files (Moves to src/ and generates output/tgf/, output/xlsx/, output/txt/, output/docx/, etc.)
java -jar target/claret-generator.jar -i 20150617 -o 20150617/output -f all -c gt

# 2. Generate tabulated text format (TXT)
java -jar target/claret-generator.jar -i 20150617 -o 20150617/output -f txt -c gt

# 3. Generate Microsoft Word format (DOCX)
java -jar target/claret-generator.jar -i 20150617 -o 20150617/output -f docx -c gt

# 4. Generate with Transition Pair Coverage (GTP)
java -jar target/claret-generator.jar -i 20150617 -o 20150617/output -f xlsx -c gtp

# 5. Generate with Adaptive Random Testing (ART)
java -jar target/claret-generator.jar -i 20150617 -o 20150617/output -f xlsx -c art

# 6. Output files flatly in root output directory (without format subdirectories)
java -jar target/claret-generator.jar -i 20150617 -o 20150617/output --flat -f all -c gt
```

---

## 6. TestLink Import Step-by-Step

1. Log into your TestLink server and select the **Test Project**.
2. Navigate to **Test Specification** in the top navigation bar.
3. Select the target test suite node in the left tree.
4. Click the gear icon (**Actions**) on the right pane and choose **Import**.
5. Select the generated `<date>/output/xml/*_testlink.xml` file.
6. Verify **File Type: XML** and click **Upload file**.
