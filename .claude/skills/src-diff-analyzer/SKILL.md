---
name: src-diff-analyzer
description: Computes GitHub diffs of specification files (.claret/.dsl under src/) between adjacent tags or releases in sequential order, and outputs a normalized CSV report with columns (#, file, system, source_version, source_content, target_version, target_content).
---

# src-diff-analyzer — CLARET Specification Diff Analyzer

Analyzes evolutions in `.claret` / `.dsl` specification files in the `src/` directory between consecutive pairs of tags or releases, generating a machine-readable structured CSV report with normalized text representation.

## Key Capabilities

1. **Adjacent Version Pair Extraction**: Iterates over ordered tags/releases `(T[0] -> T[1], T[1] -> T[2], ...)`.
2. **Text Normalization Protocol**:
   - Converted to UTF-8.
   - Converted to lowercase.
   - Consecutive intra-line whitespace (`[ \t]+`) collapsed into a single space.
   - Consecutive empty lines/newlines collapsed into a single newline.
3. **Structured CSV Output**:
   Columns:
   `# | file | system | source_version | source_content | target_version | target_content`

## CLI Invocation

```bash
# Compute src diffs between consecutive tags and export to CSV
python .claude/skills/src-diff-analyzer/scripts/diff_src.py \
  --tags saff-study_v1.0 saff-study_v2.0 saff-study_v3.0 \
  --output-csv ./reports/src_diffs.csv \
  --repo diegoquirino/openscience
```

Parameters:
- `--tags`: (Required) Ordered list of tags or release names.
- `--output-csv`: (Optional) Output CSV file path. Default: `./reports/src_diffs.csv`.
- `--repo`: (Optional) GitHub repository (`owner/repo`). Defaults to `GITHUB_REPO` from `.env`.
