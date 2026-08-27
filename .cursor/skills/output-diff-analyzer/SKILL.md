---
name: output-diff-analyzer
description: Computes GitHub diffs of generated test cases in output/ (supporting formats xlsx, txt, docx, odt, xml, tgf, alts; scope all vs all_usecases; coverage filter GT, GTP, ART, Complete) between adjacent tags or releases in sequential order, and outputs a normalized CSV report (#, file, system, source_version, source_content, target_version, target_content).
---

# output-diff-analyzer — Generated Test Suite Diff Analyzer

Analyzes evolutions in generated Model-Based Testing test suites and artifacts in the `output/` directory between consecutive pairs of tags or releases, generating a structured CSV report with normalized text representation.

## Key Capabilities

1. **Format Filtering**: Filter by target formats (`xlsx`, `txt`, `docx`, `odt`, `xml`, `tgf`, `alts`).
2. **Scope Selection**:
   - `all_usecases`: Focuses strictly on consolidated test suite files (e.g. `all_usecases--GT-.xlsx`, `all_usecases--GT-.txt`, `all_usecases_testlink.xml`).
   - `all`: Analyzes all generated test case files.
3. **Coverage Approach Filtering**: Optionally filter by coverage suffix:
   - `GT`: Reduced (Greedy Heuristic - Transition Coverage)
   - `GTP`: Reduced (Greedy Heuristic - Transition Pair Coverage)
   - `ART`: Reduced (Adaptive Random Testing by Jaccard Distance)
   - `Complete`: Complete Path Coverage
   - `Basic`: Happy Path / Basic Flow Only
   - `Branches`: Reduced Decision Branches
4. **Text Extraction & Normalization**:
   - Parses text content from structured text/XML/XLSX.
   - Converts to UTF-8 lowercase.
   - Collapses consecutive whitespace characters into a single space and consecutive empty lines into a single newline.
5. **Structured CSV Output**:
   Columns:
   `# | file | system | source_version | source_content | target_version | target_content`

## CLI Invocation

```bash
# Analyze diffs of consolidated Excel and TXT test suites under GT coverage
python .claude/skills/output-diff-analyzer/scripts/diff_output.py \
  --tags saff-study_v1.0 saff-study_v2.0 \
  --formats txt xlsx \
  --scope all_usecases \
  --coverage gt \
  --output-csv ./reports/output_diffs.csv \
  --repo diegoquirino/openscience
```

Parameters:
- `--tags`: (Required) Ordered list of tags or releases.
- `--formats`: (Optional) Formats to inspect (`txt`, `xlsx`, `docx`, `odt`, `xml`, `tgf`, `alts`, `all`). Default: `txt`.
- `--scope`: (Optional) `all_usecases` (consolidated suites only) or `all` (all test case files). Default: `all_usecases`.
- `--coverage`: (Optional) Coverage criteria suffix filter (`gt`, `gtp`, `art`, `complete`, `basic`, `branches`, `all`). Default: `all`.
- `--output-csv`: (Optional) Output CSV file path. Default: `./reports/output_diffs.csv`.
- `--repo`: (Optional) GitHub repository (`owner/repo`). Defaults to `GITHUB_REPO` from `.env`.
