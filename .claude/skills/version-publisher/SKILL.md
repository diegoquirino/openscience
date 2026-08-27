---
name: version-publisher
description: Orchestrates sequential versioning of CLARET MBT specifications into a dedicated GitHub branch. Renames .claret/.dsl files into PascalCase (preserving technical acronyms in uppercase), executes claret-generator.jar to produce src/ and output/ directories, commits and pushes to the target GitHub branch, creates tag (<branch>_vX.Y), and publishes GitHub Release (<Branch Title> vX.Y).
---

# version-publisher — Sequential CLARET Specification Publisher

Publishes a sequential array of specification version directories into a GitHub branch with automated MBT test case generation, standard tag creation, and release publishing.

## Key Capabilities

1. **PascalCase Normalization**: Recursively collects `.claret` and `.dsl` files from the given version directory, renaming them to PascalCase while strictly retaining uppercase acronyms (e.g., `CRUD_Users.claret`, `ExtracaoLLT.claret`).
2. **MBT Test Suite Generation**: Runs `claret-generator.jar` (standalone fat JAR) to parse specifications and generate test cases in `output/` while moving specification files to `src/`.
3. **Branch Publishing**: Commits and pushes the version artifacts to the target branch (never `main`).
4. **Git Tagging**: Creates an annotated Git tag formatted as `<branch_name>_v<X.Y>` (e.g., integers like `1` or `2` are automatically normalized to `1.0`, `2.0`).
5. **GitHub Release**: Publishes a formal GitHub Release titled `<Branch Title> v<X.Y>` (e.g., `Saff Study v1.0`).

## Prerequisites & Environment

Defined in `.env`:
- `GITHUB_TOKEN`: GitHub Personal Access Token with repo/release permissions.
- `GITHUB_REPO`: Target GitHub repository (`owner/repo`). Defaults to `diegoquirino/openscience`.
- `CLARET_JAR_PATH`: Path to `claret-generator.jar` (e.g., `../claret-generator/target/claret-generator.jar`).

## CLI Invocation

```bash
# Execute version publisher across multiple sequential version directories
python .claude/skills/version-publisher/scripts/publish_versions.py \
  --version-dirs 20150617 20150618 20150619 \
  --branch saff-study \
  --repo diegoquirino/openscience
```

Parameters:
- `--version-dirs`: (Required) Ordered list of directory paths or version folder names.
- `--branch`: (Required) Target GitHub branch name (also used as local integrator directory).
- `--repo`: (Optional) GitHub `owner/repo`. Defaults to `GITHUB_REPO` from `.env`.
- `--coverage`: (Optional) Coverage criteria (`gt`, `gtp`, `art`, `complete`). Default: `gt`.
- `--formats`: (Optional) Output test formats (`all`, `xlsx`, `txt`, `docx`, `odt`, `xml`, `tgf`, `alts`). Default: `all`.
- `--dry-run`: (Optional) Executes naming and generation without pushing to remote GitHub.

## Procedural Workflow

1. For each version directory in `version_dirs` (ordered from first to last):
   a. Create or clean the working directory `<branch_name>/`.
   b. Recursively scan the input version directory for `*.claret` and `*.dsl` files.
   c. Copy each file to `<branch_name>/` with PascalCase naming (preserving acronyms).
   d. Invoke `claret-generator.jar` on `<branch_name>/` with specified coverage and formats.
   e. Verify that `src/` contains specifications and `output/` contains test cases.
   f. Execute Git stage, commit (`"feat: publish version <X.Y> for <branch>"`), and push to `origin <branch>`.
   g. Create tag `<branch>_v<X.Y>` and push to remote.
   h. Create GitHub Release `<Branch Title> v<X.Y>` via REST API.
