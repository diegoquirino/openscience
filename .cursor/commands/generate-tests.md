# /generate-tests — Batch Generate MBT Test Suites

Arguments: `$ARGUMENTS` (e.g. `--dirs <dir1> <dir2> [--coverage gt] [--formats all] [--clean]`)

Executes the **test-generator** skill: read `.cursor/skills/test-generator/SKILL.md` and follow the procedure.

## When to Invoke
- When generating or updating the `output/` test suites and model artifacts directly inside one or more version directories from their `.claret` / `.dsl` specification files.

## Procedure
1. Verify `claret-generator.jar` location (`bin/claret-generator.jar` or `CLARET_JAR_PATH` in `.env`).
2. Execute:
   ```bash
   python .cursor/skills/test-generator/scripts/generate_tests.py $ARGUMENTS
   ```
3. Report generated output directories and summary to the user.
