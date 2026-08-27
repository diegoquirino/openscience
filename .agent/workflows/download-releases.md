# /download-releases — Download Specification Source Trees from GitHub

Arguments: `$ARGUMENTS` (e.g. `--tags <tag1> <tag2> [--output-dir ./downloads] [--repo <owner/repo>]`)

Executes the **release-downloader** skill: read `.agent/skills/release-downloader/SKILL.md` and follow the procedure.

## When to Invoke
- When needing to download the `src/` directory containing `.claret` / `.dsl` specifications from specific tags or releases.

## Procedure
1. Load credentials and repository from `.env`.
2. Execute:
   ```bash
   python .agent/skills/release-downloader/scripts/download_releases.py $ARGUMENTS
   ```
3. Confirm downloaded files in destination folders.
