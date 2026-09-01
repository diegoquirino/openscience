#!/usr/bin/env python3
"""
download_releases.py
====================
CLI script for the release-downloader skill.
Downloads exclusively the src/ directory from specific GitHub tags or releases into local folders.
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
    logger
)

def download_tags_src(tags: list, output_base_dir: Path, repo: str):
    gh = GitHubManager(repo=repo)
    output_base_dir.mkdir(parents=True, exist_ok=True)

    logger.info(f"Downloading src/ directories for {len(tags)} tags from repository '{repo}' into '{output_base_dir}'")

    for tag in tags:
        clean_tag = tag.strip()
        tag_target_dir = output_base_dir / clean_tag / "src"
        tag_target_dir.mkdir(parents=True, exist_ok=True)

        logger.info(f"Fetching tree for tag: {clean_tag}")
        src_files = gh.list_tree_at_ref(ref=clean_tag, path_prefix="src")

        if not src_files:
            logger.warning(f"No files found under 'src/' for tag '{clean_tag}' via API tree search.")
            # Fallback attempt to fetch standard known files or clone subtree
            continue

        downloaded_count = 0
        for fpath in src_files:
            content = gh.fetch_file_content_at_ref(ref=clean_tag, file_path=fpath)
            if content is not None:
                # Relative file path inside src
                rel_path = Path(fpath).relative_to("src")
                dest_file = tag_target_dir / rel_path
                dest_file.parent.mkdir(parents=True, exist_ok=True)
                dest_file.write_text(content, encoding="utf-8")
                downloaded_count += 1
                logger.debug(f"Saved: {dest_file}")

        logger.info(f"Tag '{clean_tag}': successfully downloaded {downloaded_count} files into '{tag_target_dir}'.")

def main():
    parser = argparse.ArgumentParser(description="Download src/ files for specified GitHub tags/releases.")
    parser.add_argument("--tags", nargs="+", required=True, help="List of tag names or release names")
    parser.add_argument("--output-dir", default="./downloads", help="Destination base directory")
    parser.add_argument("--repo", default=get_default_repo(), help="GitHub repository (owner/repo)")

    args = parser.parse_args()
    download_tags_src(
        tags=args.tags,
        output_base_dir=Path(args.output_dir),
        repo=args.repo
    )

if __name__ == "__main__":
    main()
