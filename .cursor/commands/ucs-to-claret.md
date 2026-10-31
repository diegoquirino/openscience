# /ucs-to-claret — Translate Natural Language UCS to CLARET Specifications

Arguments: `$ARGUMENTS` (e.g. `--dirs v1/docs [--output-dir claret_src] [--system-name iTrust2] [--clean]`)

Executes the **ucs-to-claret** skill: read `.cursor/skills/ucs-to-claret/SKILL.md` and follow the procedure.

## When to Invoke
- When translating Use Case Specifications (UCS) written in natural language (Markdown/Text) into formal `.claret` files.
- When generating specification files into `claret_src/` for MBT test suite generation with `claret-generator`.

## Procedure
1. Identify source directories or UCS markdown files (e.g. `v1/docs`).
2. Run the translator:
   ```bash
   python .cursor/skills/ucs-to-claret/scripts/ucs_to_claret.py $ARGUMENTS
   ```
3. Verify that translated `.claret` files are placed flatly inside `claret_src/` with zero subdirectories.
4. Report the generated specifications, extracted actors, and validation results to the user.

