# /diff-output — Analyze Generated Test Suite Diffs Between Consecutive Tags

Arguments: `$ARGUMENTS` (e.g. `--tags <tag1> <tag2> [--formats txt xlsx] [--scope all_usecases] [--coverage gt] [--output-csv ./reports/output_diffs.csv] [--repo <owner/repo>]`)

Executes the **output-diff-analyzer** skill: read `.cursor/skills/output-diff-analyzer/SKILL.md` and follow the procedure.

## When to Invoke
- When generating a structured CSV comparison of generated test suites in `output/` between adjacent tags or releases.

## Procedure
1. Execute:
   ```bash
   python .cursor/skills/output-diff-analyzer/scripts/diff_output.py $ARGUMENTS
   ```
2. Inspect the generated CSV report.
