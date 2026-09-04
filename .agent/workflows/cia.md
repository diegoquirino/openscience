# /cia — Change Impact Analysis: src/ -> output/ (.txt)

Arguments: `$ARGUMENTS` (e.g. `[--tags <tag1> <tag2> ...] [--output-csv ./reports/src-output_cia.csv]`)

Executes the **cia-analyzer** skill: read `.agent/skills/cia-analyzer/SKILL.md` and follow the procedure.

## When to Invoke
- When mapping granular `.claret` specification diffs in `src/` directly to impacted test cases (CTs) in `output/txt/`.

## Procedure
1. Execute:
   ```bash
   python .agent/skills/cia-analyzer/scripts/cia_analyzer.py $ARGUMENTS
   ```
2. Inspect the generated CSV report in `reports/src-output_cia.csv`.
