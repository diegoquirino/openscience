---
name: release-downloader
description: Downloads the specification source directory (src/ containing .claret/.dsl files) from a list of GitHub tags or releases into segregated local folders per tag/release.
---

# release-downloader — GitHub Release & Tag Source Downloader

Fetches and isolates the `src/` directory containing `.claret` / `.dsl` specification files for a given set of tags or releases from a GitHub repository.

## Key Capabilities

1. **Tag & Release Inspection**: Resolves tags or releases via GitHub REST API or Git tree objects.
2. **Selective `src/` Extraction**: Downloads exclusively the `src/` directory, avoiding unnecessary downloads of heavy test suite outputs or temporary files.
3. **Structured Directory Organization**: Places downloaded files into isolated folders named by tag/release (`<output_dir>/<tag_or_release>/src/`).

## Prerequisites & Environment

Defined in `.env`:
- `GITHUB_TOKEN`: GitHub Personal Access Token.
- `GITHUB_REPO`: Target GitHub repository (`owner/repo`). Defaults to `diegoquirino/openscience`.

## CLI Invocation

```bash
# Download src/ for specific tags or releases
python .claude/skills/release-downloader/scripts/download_releases.py \
  --tags saff-study_v1.0 saff-study_v2.0 \
  --output-dir ./downloads \
  --repo diegoquirino/openscience
```

Parameters:
- `--tags`: (Required) List of tag names or release names to download.
- `--output-dir`: (Optional) Base output directory. Default: `./downloads`.
- `--repo`: (Optional) GitHub `owner/repo`. Defaults to `GITHUB_REPO` from `.env`.
