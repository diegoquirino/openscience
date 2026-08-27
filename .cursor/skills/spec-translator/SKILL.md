---
name: spec-translator
description: Translates natural language requirements within .claret specifications across a set of directories into a target locale (e.g. en-us, pt-br) while strictly preserving CLARET DSL tokens, reserved keywords (system, usecase, version, actor, step, af:, bfs:, alternative, etc.), and file grammar.
---

# spec-translator — Token-Safe CLARET Specification Translator

Translates human-readable requirement steps, preconditions, postconditions, and alternative flow titles inside `.claret` specifications into the desired target locale without altering any reserved DSL keywords or syntax.

## Key Capabilities

1. **Strict Token Isolation**: Ensures keywords (`system`, `usecase`, `version`, `actor`, `step`, `af:`, `ef:`, `bfs:`, `alternative`, `exception`, `preCondition`, `postCondition`) remain completely untouched.
2. **String-Targeted Translation**: Translates text within double quotes (`"..."`) and actor descriptive labels while preserving variable identifiers (e.g. `actor superAdmin, "Super administrador"` becomes `actor superAdmin, "Super administrator"`).
3. **Multi-Directory Batching**: Operates across an array of directories in-place or into a specified output directory.

## CLI Invocation

```bash
# Translate all .claret files across directories to English (en-US)
python .claude/skills/spec-translator/scripts/translate_specs.py \
  --dirs 20150617 20150618 \
  --locale en-us \
  --output-dir ./translated_specs
```

Parameters:
- `--dirs`: (Required) List of directories containing `.claret` / `.dsl` files.
- `--locale`: (Optional) Target locale (e.g., `en-us`, `pt-br`, `es-es`). Default: `en-us`.
- `--output-dir`: (Optional) Target destination directory (if omitted, modifies files in-place or inside directory).
- `--in-place`: (Optional) Overwrite files in their original location.
