---
name: cia-analyzer
description: Performs Change Impact Analysis (CIA) by mapping granular specification diffs in src/ (.claret/.dsl) to precisely which Test Cases (CTs) of which Use Cases in output/txt/ are impacted by addition, deletion, or modification, outputting an enriched CSV report (src-output_cia.csv).
---

# cia-analyzer — Change Impact Analysis (CIA) Engine: src/ -> output/ (.txt)

Connects Model-Based Testing (MBT) specification evolution directly to test suite impact. Instead of performing disconnected output comparisons, it traces each granular diff chunk in `.claret` / `.dsl` files under `src/` to determine exactly which generated Test Cases (`TC1`, `TC2`, ...) in `output/txt/` were affected, their nature of change, and impact classification.

---

## 1. Native Agent-Driven Workflow (Recommended)

When an LLM agent (Antigravity, Claude Code, Cursor) executes `/cia` or analyzes change impact:

1. **Resolve Versions / Tags**:
   - Auto-discovers all downloaded versions in `downloads/` (e.g. `saff-study_v1.0` to `saff-study_v2.6`) or accepts an explicit ordered `--tags` list.
2. **Compute Granular Specification Diffs**:
   - Extracts semantic clause-level diff chunks between each adjacent version pair $(V_i, V_{i+1})$ in `src/`.
3. **Trace Impact on Generated Test Suites (.txt)**:
   - Reads `output/txt/<usecase>--GT-.txt` for both versions.
   - Classifies the model change (`MODEL_METADATA`, `PRE_POST_CONDITION`, `STEP_MODIFICATION`, `FLOW_ADDITION`, `FLOW_DELETION`, `FLOW_MODIFICATION`, `USECASE_LIFECYCLE`).
   - Maps to affected test case IDs (`TC1`, `TC2`, ...), impact type (`UPDATED`, `ADDED`, `REMOVED`, `NO_IMPACT`), and flow names.
4. **Generate Enriched CIA CSV**:
   - Exports `reports/src-output_cia.csv` preserving original diff columns (`#`, `file`, `system`, `origin_version`, `origin_content`, `target_version`, `target_content`) and appending:
     `usecase`, `change_nature`, `impact_type`, `affected_cts_count`, `affected_cts_origin`, `affected_cts_target`, `affected_flows`, `impact_summary`.

---

## 2. Standalone CLI Invocation (Automated / Headless)

For terminal scripts or CI/CD pipelines:

```bash
# Run CIA across all downloaded versions automatically
python scripts/cia_analyzer.py

# Run CIA for specific version pairs with custom output path
python scripts/cia_analyzer.py \
  --tags saff-study_v1.0 saff-study_v1.1 \
  --output-csv ./reports/src-output_cia.csv
```

### Parameters

- `--tags`: (Optional) Ordered list of tags/releases. If omitted, automatically discovers all versions in `downloads/` in natural numerical order.
- `--output-csv`: (Optional) Output CSV file path. Default: `./reports/src-output_cia.csv`.
- `--downloads-dir`: (Optional) Directory containing pre-downloaded versions with `src/` and `output/txt/`. Default: `./downloads`.
- `--repo`: (Optional) GitHub repository (`owner/repo`). Defaults to `GITHUB_REPO` from `.env`.
- `--repo-dir`: (Optional) Local git repository path for fallback inspections.
