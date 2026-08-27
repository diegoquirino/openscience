# /translate-specs — Translate CLARET Specifications Preserving DSL Grammar

Arguments: `$ARGUMENTS` (e.g. `--dirs <dir1> <dir2> [--locale en-us] [--output-dir ./translated]`)

Executes the **spec-translator** skill: read `.agent/skills/spec-translator/SKILL.md` and follow the procedure.

## When to Invoke
- When translating requirements within `.claret` specifications across directories without breaking DSL syntax or translating reserved keywords (`system`, `step`, etc.).

## Procedure
1. Execute:
   ```bash
   python .agent/skills/spec-translator/scripts/translate_specs.py $ARGUMENTS
   ```
2. Verify translated output files.
