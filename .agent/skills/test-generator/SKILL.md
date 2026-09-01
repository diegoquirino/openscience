---
name: test-generator
description: Batch executes claret-generator.jar across a list of version directories (specified in --dirs) to generate the complete MBT test suite and formal model artifacts in output/ from .claret/.dsl specification files in src/ (or root).
---

# test-generator — Batch MBT Test Suite & Output Generator

Executes `claret-generator.jar` locally across an array of version directories to generate complete Model-Based Testing (MBT) test suites, spreadsheets, textual specifications, formal graphs, and documentation into their respective `output/` directories.

---

## 1. Native Agent-Driven Workflow (Recommended)

When an LLM agent (Antigravity, Claude Code, Cursor) executes `/generate-tests` or generates test suites:

1. **Locate Target Directories**:
   - Identify version folders provided by user or found in workspace (e.g., `1.0`, `1.1`, ..., `2.9`).
2. **Execute Generation**:
   - Locate Java 26 / Java runtime and `bin/claret-generator.jar`.
   - Run generator per directory:
     `java -jar bin/claret-generator.jar -i <dir> -o <dir>/output -f all -c gt`
3. **Verify Generated Artifacts**:
   - Ensure `output/` contains test files (`xlsx/`, `txt/`, `docx/`, `odt/`, `tgf/`, `alts/`, `xml/`).
   - Confirm consolidated files exist (`all_usecases--GT-.xlsx`, `all_usecases--GT-.txt`, `all_usecases_testlink.xml`, `all_usecases.docx`, `all_usecases.odt`).

---

## 2. Standalone CLI Invocation (Automated / Headless)

For terminal scripts or CI/CD pipelines:

```bash
# Generate test suites for specific version directories with default GT coverage
python .claude/skills/test-generator/scripts/generate_tests.py \
  --dirs 1.0 1.1 1.2 \
  --coverage gt \
  --clean
```

### Parameters

- `--dirs`: (Required) One or more version directories to process.
- `--coverage` (`-c`): (Optional) MBT coverage criteria: `gt` (default), `gtp`, `art`, `complete`, `basic-only`, `all-branches`.
- `--formats` (`-f`): (Optional) Comma-separated output formats (`all`, `xlsx`, `txt`, `docx`, `odt`, `xml`, `tgf`, `alts`). Default: `all`.
- `--flat`: (Optional) Output all generated files flatly in `output/` without format subdirectories.
- `--clean`: (Optional) Remove existing `output/` directory before generation.
- `--jar-path`: (Optional) Explicit path to `claret-generator.jar`.
