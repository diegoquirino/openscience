---
name: version-publisher
description: Orchestrates sequential versioning of CLARET MBT specifications into a dedicated GitHub branch. Renames .claret/.dsl files into PascalCase (preserving technical acronyms in uppercase), executes claret-generator.jar to produce src/ and output/ directories, commits and pushes to the target GitHub branch, creates tag (<branch>_vX.Y), and publishes GitHub Release (<Branch Title> vX.Y).
---

# version-publisher — Sequential CLARET Specification Publisher

Publishes a sequential array of specification version directories into a GitHub branch with automated MBT test case generation, standard tag creation, and release publishing.

---

## 1. Native Agent-Driven Workflow (Recommended)

When an LLM agent (Antigravity, Claude Code, Cursor) executes `/publish-versions` or publishes specifications:

1. **Prepare Integrator Workspace**:
   - Work within the target branch directory `<branch_name>/`.
   - Ensure target branch is checked out (`git checkout -B <branch_name>`).
2. **Sequential Iteration $(V_1, V_2, \dots, V_n)$**:
   For each version folder in the user-specified sequence:
   a. **Merge & Rename**: Copy `.claret` / `.dsl` files into `<branch_name>/`, converting file names to PascalCase with uppercase acronyms (e.g. `CRUD_Cliente.claret`, `ExtracaoLLT.claret`).
   b. **Generate Test Suite**: Run `claret-generator.jar` (via Java runtime) targeting `<branch_name>/output/` (generating `src/` and `output/`).
   c. **Git Stage & Commit**: Stage all changes (`git add -A`), commit (`"feat(<branch>): publish specification and test suite v<X.Y>"`), and push to `origin <branch>`.
   d. **Tag & Release**: Create annotated tag `<branch>_v<X.Y>` and push (`git push origin <branch>_v<X.Y>`).
3. **Branch Isolation**:
   - Never push directly to `main`. Always target the dedicated study branch.

---

## 2. Standalone CLI Invocation (Automated / Headless)

For terminal scripts or CI/CD pipelines:

```bash
# Execute version publisher across multiple sequential version directories
python .claude/skills/version-publisher/scripts/publish_versions.py \
  --version-dirs 1.0 1.1 1.2 \
  --branch saff-study \
  --repo diegoquirino/openscience
```

Parameters:
- `--version-dirs`: (Required) Ordered list of directory paths or version folder names.
- `--branch`: (Required) Target GitHub branch name.
- `--repo`: (Optional) GitHub `owner/repo`. Defaults to `GITHUB_REPO` from `.env`.
- `--coverage`: (Optional) Coverage criteria (`gt`, `gtp`, `art`, `complete`). Default: `gt`.
- `--formats`: (Optional) Output test formats (`all`, `xlsx`, `txt`, `docx`, `odt`, `xml`, `tgf`, `alts`). Default: `all`.
- `--dry-run`: (Optional) Executes naming and generation without pushing to remote GitHub.
