---
name: test-generator
description: Batch executes claret-generator.jar across a list of version directories (specified in --dirs) to generate the complete MBT test suite and formal model artifacts in output/ from .claret/.dsl specification files in src/ (or root).
---

# test-generator — Batch MBT Test Suite & Output Generator

Executes `claret-generator.jar` locally across an array of version directories to generate complete Model-Based Testing (MBT) test suites, spreadsheets, textual specifications, formal graphs, and documentation into their respective `output/` directories.

## Key Capabilities

1. **Batch Processing**: Accepts multiple version directory paths (`--dirs 1.0 1.1 1.2` or `--dirs 20150617 20150618`).
2. **Sibling Directory Layout**: Automatically processes `.claret` / `.dsl` files (from either `src/` or root) and produces structured `output/` (with subdirectories `xlsx/`, `txt/`, `docx/`, `odt/`, `tgf/`, `alts/`, `xml/`).
3. **Flexible Coverage Criteria**: Supports `gt` (default), `gtp`, `art`, `complete`, `basic-only`, and `all-branches`.
4. **Multi-Format Generation**: Selectively generates `all` or specific formats (`xlsx`, `txt`, `docx`, `odt`, `xml`, `tgf`, `alts`).
5. **Output Cleaning**: Optional `--clean` flag to remove stale artifacts in `output/` before generation.

## CLI Invocation

```bash
# Generate test suites for specific version directories with default GT coverage
python .claude/skills/test-generator/scripts/generate_tests.py \
  --dirs 1.0 1.1 1.2 \
  --coverage gt \
  --formats all

# Generate with Adaptive Random Testing (ART) and clean previous outputs
python .claude/skills/test-generator/scripts/generate_tests.py \
  --dirs 20150617 20150618 \
  --coverage art \
  --clean
```

### Parameters

- `--dirs`: (Required) One or more version directories to process.
- `--coverage` (`-c`): (Optional) MBT coverage criteria: `gt` (default), `gtp`, `art`, `complete`, `basic-only`, `all-branches`.
- `--formats` (`-f`): (Optional) Comma-separated output formats (`all`, `xlsx`, `txt`, `docx`, `odt`, `xml`, `tgf`, `alts`). Default: `all`.
- `--flat`: (Optional) Output all generated files flatly in `output/` without format subdirectories.
- `--clean`: (Optional) Remove existing `output/` directory before generation.
- `--jar-path`: (Optional) Explicit path to `claret-generator.jar` (defaults to auto-detection in `bin/`, `lib/`, etc.).

## Execution Flow

```mermaid
sequenceDiagram
    autonumber
    actor User as User / Agent
    participant Script as generate_tests.py
    participant Engine as claret_engine.py
    participant JAR as claret-generator.jar
    participant FS as Version Directory (dir/)

    User->>Script: Run generate_tests.py (--dirs 1.0 1.1 ...)
    loop For each directory in --dirs
        Script->>FS: Validate directory and check .claret/.dsl files
        alt --clean enabled
            Script->>FS: Remove existing output/ folder
        end
        Script->>Engine: Call run_claret_generator(input_dir=dir)
        Engine->>JAR: Execute Java 26 CLI (-i dir -o dir/output -c ... -f ...)
        JAR->>FS: Parse specs in src/ and generate output/ artifacts
        JAR-->>Engine: Return execution log and exit code
        Engine-->>Script: Report status
    end
    Script-->>User: Display summary table of generated directories
```
