---
name: output-diff-analyzer
description: Computes GitHub diffs of generated test cases in output/ (supporting formats xlsx, txt, docx, odt, xml, tgf, alts; scope all vs all_usecases; coverage filter GT, GTP, ART, Complete) between adjacent tags or releases in sequential order, and outputs a normalized CSV report (#, file, system, source_version, source_content, target_version, target_content).
---

# output-diff-analyzer — Generated Test Suite Diff Analyzer

Analyzes evolutions in generated Model-Based Testing test suites and artifacts in the `output/` directory between consecutive pairs of tags, releases, or local version directories, generating a structured CSV report with normalized text representation.

---

## 1. Native Agent-Driven Workflow (Recommended)

When an LLM agent (Antigravity, Claude Code, Cursor) executes `/diff-output` or analyzes test suite diffs:

1. **Resolve Scope, Formats & Coverage Filter**:
   - Target formats: `txt`, `xlsx`, `docx`, `odt`, `xml`, `tgf`, `alts` (default: `txt`).
   - Scope: `all_usecases` (consolidated test suite files only) or `all` (all test case files).
   - Coverage: `GT`, `GTP`, `ART`, `Complete`, `Basic`, `Branches` (default: `all` or `GT`).
2. **Extract & Inspect Test Artifacts**:
   - Retrieve generated test files in `output/` for each adjacent pair $(V_i, V_{i+1})$.
   - Extract raw text or structured XML/TXT/TGF contents.
3. **Normalize Content**:
   - Convert to UTF-8 lowercase.
   - Collapse consecutive intra-line whitespace into a single space.
   - Collapse consecutive blank lines into a single newline.
4. **Generate Structured CSV Report**:
   - Output CSV with header:
     `#,file,system,source_version,source_content,target_version,target_content`
   - Save to requested path (e.g., `./reports/output_diffs.csv`).

---

## 2. Standalone CLI Invocation (Automated / Headless)

For terminal scripts or CI/CD pipelines:

```bash
# Analyze diffs of consolidated TXT test suites under GT coverage
python .claude/skills/output-diff-analyzer/scripts/diff_output.py \
  --tags saff-study_v1.0 saff-study_v1.1 \
  --formats txt \
  --scope all_usecases \
  --coverage gt \
  --output-csv ./reports/output_diffs.csv \
  --repo diegoquirino/openscience
```

Parameters:
- `--tags`: (Required) Ordered list of tags or releases.
- `--formats`: (Optional) Formats to inspect (`txt`, `xlsx`, `docx`, `odt`, `xml`, `tgf`, `alts`, `all`). Default: `txt`.
- `--scope`: (Optional) `all_usecases` or `all`. Default: `all_usecases`.
- `--coverage`: (Optional) Coverage criteria suffix filter (`gt`, `gtp`, `art`, `complete`, `basic`, `branches`, `all`). Default: `all`.
- `--output-csv`: (Optional) Output CSV file path. Default: `./reports/output_diffs.csv`.
- `--repo`: (Optional) GitHub repository (`owner/repo`). Defaults to `GITHUB_REPO` from `.env`.
