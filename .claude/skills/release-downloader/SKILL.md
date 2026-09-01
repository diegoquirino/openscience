---
name: release-downloader
description: Downloads the specification source directory (src/ containing .claret/.dsl files) from a list of GitHub tags or releases into segregated local folders per tag/release.
---

# release-downloader — GitHub Release & Tag Source Downloader

Fetches and isolates the `src/` directory containing `.claret` / `.dsl` specification files for a given set of tags or releases from a GitHub repository into clean local folders.

---

## 1. Native Agent-Driven Workflow (Recommended)

When an LLM agent (Antigravity, Claude Code, Cursor) executes `/download-releases` or downloads specification releases:

1. **Inspect Tags / Releases**:
   - Resolve target tags (e.g. `saff-study_v1.0` ... `saff-study_v2.9`) using `git tag -l` or GitHub CLI / API.
2. **Extract `src/` Specifications**:
   - Extract specifications directly from local/remote git history (`git archive <tag> src/` or `git checkout <tag> -- src/`) or download from GitHub release tarball.
   - Isolate into segregated folders: `<output_dir>/<version_or_tag>/src/`.
3. **Verify Integrity**:
   - Confirm that all `.claret` / `.dsl` files for each tag are present and uncorrupted.

---

## 2. Standalone CLI Invocation (Automated / Headless)

For terminal scripts or CI/CD pipelines:

```bash
# Download src/ for specific tags or releases
python .claude/skills/release-downloader/scripts/download_releases.py \
  --tags saff-study_v1.0 saff-study_v1.1 \
  --output-dir ./downloads \
  --repo diegoquirino/openscience
```

Parameters:
- `--tags`: (Required) List of tag names or release names to download.
- `--output-dir`: (Optional) Base output directory. Default: `./downloads`.
- `--repo`: (Optional) GitHub `owner/repo`. Defaults to `GITHUB_REPO` from `.env`.
