#!/usr/bin/env python3
"""
download_releases.py
====================
CLI script for the release-downloader skill.
Downloads src/ and output/ directories from specific GitHub tags or releases into
local segregated folders (<output_dir>/<tag>/src/ and <output_dir>/<tag>/output/).

Supports:
  --tags       : explicit list of tag names
  --branch     : auto-discover all tags matching <branch>_v* from GitHub
  --prefixes   : one or more path prefixes to download (default: src output)
"""

import sys
import argparse
from pathlib import Path

# Add project root scripts to sys.path
SCRIPT_DIR = Path(__file__).resolve().parent
PROJECT_ROOT = SCRIPT_DIR
for parent in [SCRIPT_DIR] + list(SCRIPT_DIR.parents):
    if (parent / "scripts" / "claret_engine.py").exists():
        sys.path.insert(0, str(parent / "scripts"))
        PROJECT_ROOT = parent
        break
    elif (parent / "claret-version-control-system" / "scripts" / "claret_engine.py").exists():
        sys.path.insert(0, str(parent / "claret-version-control-system" / "scripts"))
        PROJECT_ROOT = parent / "claret-version-control-system"
        break

from claret_engine import (
    GitHubManager,
    get_default_repo,
    get_github_token,
    logger
)


def discover_tags_for_branch(branch: str, repo: str) -> list:
    """Discover all tags matching <branch>_v* from the GitHub API, sorted numerically."""
    import requests, re
    token = get_github_token()
    headers = {"Accept": "application/vnd.github.v3+json"}
    if token:
        headers["Authorization"] = f"token {token}"

    prefix = f"{branch}_v"
    tags = []
    url = f"https://api.github.com/repos/{repo}/tags?per_page=100"
    while url:
        resp = requests.get(url, headers=headers, timeout=30)
        if resp.status_code != 200:
            logger.error(f"Failed to list tags: {resp.status_code} {resp.text[:100]}")
            break
        for t in resp.json():
            name = t["name"]
            if name.startswith(prefix):
                tags.append(name)
        url = None
        if "next" in resp.links:
            url = resp.links["next"]["url"]

    def version_key(tag):
        m = re.search(r'v(\d+)\.(\d+)', tag)
        return (int(m.group(1)), int(m.group(2))) if m else (0, 0)

    return sorted(tags, key=version_key)


def download_tags(tags: list, output_base_dir: Path, repo: str, prefixes: list):
    gh = GitHubManager(repo=repo)
    output_base_dir.mkdir(parents=True, exist_ok=True)

    logger.info(f"Downloading {prefixes} for {len(tags)} tags from '{repo}' into '{output_base_dir}'")

    for tag in tags:
        clean_tag = tag.strip()
        tag_root_dir = output_base_dir / clean_tag
        tag_root_dir.mkdir(parents=True, exist_ok=True)

        for prefix in prefixes:
            prefix_target_dir = tag_root_dir / prefix
            prefix_target_dir.mkdir(parents=True, exist_ok=True)

            logger.info(f"[{clean_tag}] Fetching tree for prefix: {prefix}/")
            files = gh.list_tree_at_ref(ref=clean_tag, path_prefix=prefix)

            if not files:
                logger.warning(f"[{clean_tag}] No files found under '{prefix}/' — skipping.")
                continue

            downloaded_count = 0
            for fpath in files:
                content = gh.fetch_file_content_at_ref(ref=clean_tag, file_path=fpath)
                if content is not None:
                    rel_path = Path(fpath).relative_to(prefix)
                    dest_file = prefix_target_dir / rel_path
                    dest_file.parent.mkdir(parents=True, exist_ok=True)
                    dest_file.write_text(content, encoding="utf-8")
                    downloaded_count += 1
                    logger.debug(f"  Saved: {dest_file}")

            logger.info(f"[{clean_tag}] {prefix}/: {downloaded_count} files -> '{prefix_target_dir}'")


def main():
    parser = argparse.ArgumentParser(
        description="Download src/ and output/ from GitHub tags/releases into local downloads/ folders."
    )
    group = parser.add_mutually_exclusive_group(required=True)
    group.add_argument("--tags", nargs="+", help="Explicit list of tag names to download.")
    group.add_argument("--branch", help="Auto-discover all tags matching <branch>_v* from GitHub.")
    parser.add_argument("--output-dir", default="./downloads", help="Destination base directory (default: ./downloads)")
    parser.add_argument("--repo", default=get_default_repo(), help="GitHub repository (owner/repo)")
    parser.add_argument(
        "--prefixes", nargs="+", default=["src", "output"],
        help="Directory prefixes to download from each tag (default: src output)"
    )

    args = parser.parse_args()

    if args.branch:
        tags = discover_tags_for_branch(args.branch, args.repo)
        if not tags:
            logger.error(f"No tags found matching '{args.branch}_v*' in repo '{args.repo}'.")
            return
        logger.info(f"Auto-discovered {len(tags)} tags for branch '{args.branch}': {tags}")
    else:
        tags = args.tags

    download_tags(
        tags=tags,
        output_base_dir=Path(args.output_dir),
        repo=args.repo,
        prefixes=args.prefixes,
    )


if __name__ == "__main__":
    main()
