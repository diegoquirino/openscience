# /diff-src — Analyze Specification Diffs Between Consecutive Tags

Arguments: `$ARGUMENTS` (e.g. `--tags <tag1> <tag2> <tag3> [--output-csv ./reports/src_diffs.csv] [--repo <owner/repo>]`)

Executes the **src-diff-analyzer** skill: read `.cursor/skills/src-diff-analyzer/SKILL.md` and follow the procedure.

## When to Invoke
- When generating a structured CSV comparison of `.claret` files in `src/` between adjacent tags or releases.

## Procedure
1. Execute:
   ```bash
   python .cursor/skills/src-diff-analyzer/scripts/diff_src.py $ARGUMENTS
   ```
2. Inspect the generated CSV report.
