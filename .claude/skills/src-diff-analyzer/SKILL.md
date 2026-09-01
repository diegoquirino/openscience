---
name: src-diff-analyzer
description: Computes GitHub diffs of specification files (.claret/.dsl under src/) between adjacent tags or releases in sequential order, and outputs a normalized CSV report with columns (#, file, system, source_version, source_content, target_version, target_content).
---

# src-diff-analyzer — CLARET Specification Diff Analyzer

Analyzes evolutions in `.claret` / `.dsl` specification files in the `src/` directory between consecutive pairs of tags, releases, or local version directories, generating a machine-readable structured CSV report with normalized text representation.

---

## 1. Native Agent-Driven Workflow (Recommended)

When an LLM agent (Antigravity, Claude Code, Cursor) executes `/diff-src` or analyzes specification diffs:

1. **Resolve Ordered Versions/Tags**:
   - From git tags (e.g., `saff-study_v1.0`, `saff-study_v1.1`, ..., `saff-study_v2.9`) or local version directories (`1.0`, `1.1`, ..., `2.9`).
2. **Read & Extract Content**:
   - Retrieve `.claret` / `.dsl` files for each adjacent version pair $(V_i, V_{i+1})$.
   - Extract the `system "..."` name from the specification header.
3. **Normalize Specification Content**:
   - Convert to UTF-8.
   - Convert to lowercase.
   - Collapse consecutive whitespace characters (`[ \t]+`) into a single space per line.
   - Collapse consecutive blank lines into a single newline.
4. **Generate Structured CSV Report**:
   - Produce CSV with header:
     `#,file,system,source_version,source_content,target_version,target_content`
   - Include changed, added (empty `source_content`), and deleted (empty `target_content`) specifications.
   - Save to the requested report path (e.g., `./reports/src_diffs.csv`).

---

## 2. Standalone CLI Invocation (Automated / Headless)

For terminal scripts or CI/CD pipelines:

```bash
# Compute src diffs between consecutive tags and export to CSV
python .claude/skills/src-diff-analyzer/scripts/diff_src.py \
  --tags saff-study_v1.0 saff-study_v1.1 saff-study_v1.2 \
  --output-csv ./reports/src_diffs.csv \
  --repo diegoquirino/openscience
```

Parameters:
- `--tags`: (Required) Ordered list of tags or release names.
- `--output-csv`: (Optional) Output CSV file path. Default: `./reports/src_diffs.csv`.
- `--repo`: (Optional) GitHub repository (`owner/repo`). Defaults to `GITHUB_REPO` from `.env`.
