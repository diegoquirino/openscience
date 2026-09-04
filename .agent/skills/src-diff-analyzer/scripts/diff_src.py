#!/usr/bin/env python3
"""
diff_src.py
===========
CLI script for src-diff-analyzer skill.
Extracts diffs of src/ .claret files between adjacent tags/releases and outputs normalized CSV.
"""

import sys
import argparse
from pathlib import Path

from typing import Optional

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
    normalize_content,
    extract_system_name,
    extract_granular_diffs,
    generate_diff_csv,
    get_default_repo,
    logger
)

def compute_src_diffs(
    tags: list,
    output_csv: Path,
    repo: str,
    repo_dir: Optional[Path] = None,
    downloads_dir: Optional[Path] = None
):
    gh = GitHubManager(repo=repo, repo_dir=repo_dir, downloads_dir=downloads_dir)
    if len(tags) < 2:
        logger.error("At least 2 tags or releases are required to compute adjacent diffs.")
        return

    records = []
    logger.info(f"Computing granular src diffs across {len(tags) - 1} adjacent tag pairs (Repo: '{repo}', Downloads: '{gh.downloads_dir}').")

    for i in range(len(tags) - 1):
        v_source = tags[i].strip()
        v_target = tags[i + 1].strip()
        logger.info(f"Analyzing diff: {v_source} -> {v_target}")

        files_source = set(gh.list_tree_at_ref(v_source, path_prefix="src"))
        files_target = set(gh.list_tree_at_ref(v_target, path_prefix="src"))
        all_files = sorted(files_source.union(files_target))

        for fpath in all_files:
            if not (fpath.endswith(".claret") or fpath.endswith(".dsl")):
                continue

            raw_src = gh.fetch_file_content_at_ref(v_source, fpath)
            raw_tgt = gh.fetch_file_content_at_ref(v_target, fpath)

            system_name = extract_system_name(raw_tgt or raw_src or "")
            file_name = Path(fpath).name

            diff_chunks = extract_granular_diffs(
                raw_src=raw_src,
                raw_tgt=raw_tgt,
                origin_version=v_source,
                target_version=v_target,
                file_name=file_name,
                system_name=system_name,
                is_dsl=True
            )
            records.extend(diff_chunks)

    generate_diff_csv(records, output_csv)

def main():
    parser = argparse.ArgumentParser(description="Analyze granular diffs of src/ specifications between adjacent tags.")
    parser.add_argument("--tags", nargs="+", required=True, help="Ordered list of tags or releases")
    parser.add_argument("--output-csv", default="./reports/src_diffs.csv", help="Output CSV filepath")
    parser.add_argument("--repo", default=get_default_repo(), help="GitHub repository (owner/repo)")
    parser.add_argument("--repo-dir", default=None, help="Local git repository directory for offline/fast analysis")
    parser.add_argument("--downloads-dir", default=None, help="Local downloads directory containing pre-downloaded versions")

    args = parser.parse_args()
    compute_src_diffs(
        tags=args.tags,
        output_csv=Path(args.output_csv),
        repo=args.repo,
        repo_dir=Path(args.repo_dir) if args.repo_dir else None,
        downloads_dir=Path(args.downloads_dir) if args.downloads_dir else None
    )

if __name__ == "__main__":
    main()
