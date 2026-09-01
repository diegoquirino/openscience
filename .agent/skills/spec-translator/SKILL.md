---
name: spec-translator
description: Translates natural language requirements within .claret specifications across a set of directories into a target locale (e.g. en-us, pt-br) using native agent reasoning or helper scripts while strictly preserving CLARET DSL tokens, reserved keywords (system, usecase, version, actor, step, af:, bfs:, alternative, etc.), and file grammar.
---

# spec-translator — Token-Safe CLARET Specification Translator

Translates human-readable requirement steps, preconditions, postconditions, and alternative flow titles inside `.claret` specifications into the desired target locale without altering any reserved DSL keywords or syntax.

---

## 1. Native Agent-Driven Workflow (Recommended)

When an LLM agent (Antigravity, Claude Code, Cursor) executes `/translate-specs` or translates `.claret` specifications:

1. **Locate Specifications**: Find all `.claret` / `.dsl` files in the directories specified by the user (e.g., `--dirs 1.0 1.1 ... 2.9`).
2. **Contextual Translation via LLM**:
   - The agent reads each file and uses its native multilingual reasoning to translate all natural language text enclosed in double quotes (`"..."`) and actor labels.
   - Preserves dates (`"20/03/2015"`), semantic versions (`"1.0"`), and metadata identifiers (`"Creation"`, `"Modification"`, `"SAFF"`, `"SAFF Server"`, `"SAFF Extractor"`).
   - Produces idiomatic, grammatically sound sentences in the target locale (e.g., `en-us`).
3. **Strict DSL Keyword & Token Protection**:
   Never alter or translate DSL reserved keywords:
   - Structure: `system`, `usecase`, `version`, `type:`, `user:`, `date:`, `actor`, `preCondition`, `basicFlow`, `step`, `postCondition`
   - Flow branches: `alternative`, `exception`, `af:`, `ef:`, `bfs:`
   - Identifiers: `superAdmin`, `operador`, `system`, etc.
4. **Write Translated Files**: Write the translated content back to the target file (in-place or to the output directory).
5. **Verify Syntax**: Run `claret-generator.jar` (or `/generate-tests`) to ensure 100% grammar compliance.

---

## 2. Standalone CLI Invocation (Automated / Headless)

For headless terminal scripts or CI/CD pipelines:

```bash
# Translate all .claret files across directories to English (en-US)
python .claude/skills/spec-translator/scripts/translate_specs.py \
  --dirs 1.0 1.1 1.2 \
  --locale en-us \
  --in-place
```

Parameters:
- `--dirs`: (Required) List of directories containing `.claret` / `.dsl` files.
- `--locale`: (Optional) Target locale (e.g., `en-us`, `pt-br`, `es-es`). Default: `en-us`.
- `--output-dir`: (Optional) Target destination directory.
- `--in-place`: (Optional) Overwrite files in their original location.
