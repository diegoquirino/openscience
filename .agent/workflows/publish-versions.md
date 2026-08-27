# /publish-versions — Publish CLARET Specification Versions

Arguments: `$ARGUMENTS` (e.g. `--version-dirs <dir1> <dir2> --branch <branch_name> [--repo <owner/repo>] [--coverage gt]`)

Executes the **version-publisher** skill: read `.agent/skills/version-publisher/SKILL.md` and follow the procedure.

## When to Invoke
- When publishing a sequential sequence of CLARET specification directories into a GitHub branch with automated test suite generation, version tags, and releases.

## Procedure
1. Load repository and credentials from `.env` (`GITHUB_TOKEN`, `GITHUB_REPO`, `CLARET_JAR_PATH`).
2. Execute:
   ```bash
   python .agent/skills/version-publisher/scripts/publish_versions.py $ARGUMENTS
   ```
3. Report published tags and releases to the user.
