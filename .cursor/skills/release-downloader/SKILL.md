---
name: release-downloader
description: Downloads the specification source directory (src/ containing .claret/.dsl files) and the generated test suite directory (output/) from a list of GitHub tags or releases into segregated local folders per tag/release. Supports auto-discovery of all tags for a branch via --branch.
---

# release-downloader — GitHub Release & Tag Source Downloader

Fetches and isolates the `src/` and `output/` directories for a given set of tags or releases from a GitHub repository into clean local folders under `downloads/<tag>/`.

---

## 1. Native Agent-Driven Workflow (Recommended)

When an LLM agent (Antigravity, Claude Code, Cursor) executes `/download-releases` or downloads specification releases:

1. **Resolve Tags**:
   - Explicit list via `--tags`, or auto-discover all `<branch>_v*` tags via `--branch`.
2. **Extract `src/` and `output/` Trees**:
   - Downloads each prefix into `<output_dir>/<tag>/src/` and `<output_dir>/<tag>/output/`.
3. **Verify Integrity**:
   - Confirms all `.claret` / `.dsl` files and test suite files for each tag are present.

---

## 2. Standalone CLI Invocation (Automated / Headless)

```bash
# Auto-discover and download all tags for a branch (src/ + output/)
python .claude/skills/release-downloader/scripts/download_releases.py \
  --branch viceri-study \
  --output-dir ./downloads \
  --repo diegoquirino/openscience

# Explicit tags, only src/
python .claude/skills/release-downloader/scripts/download_releases.py \
  --tags viceri-study_v1.0 viceri-study_v1.1 \
  --output-dir ./downloads \
  --prefixes src \
  --repo diegoquirino/openscience
```

Parameters:
- `--tags`: Explicit ordered list of tag names (mutually exclusive with `--branch`).
- `--branch`: Auto-discover all tags matching `<branch>_v*` from GitHub (mutually exclusive with `--tags`).
- `--output-dir`: (Optional) Base output directory. Default: `./downloads`.
- `--prefixes`: (Optional) Directory prefixes to download per tag. Default: `src output`.
- `--repo`: (Optional) GitHub `owner/repo`. Defaults to `GITHUB_REPO` from `.env`.

