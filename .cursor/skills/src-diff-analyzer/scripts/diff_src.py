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
    generate_diff_csv,
    get_default_repo,
    logger
)

def compute_src_diffs(tags: list, output_csv: Path, repo: str):
    gh = GitHubManager(repo=repo)
    if len(tags) < 2:
        logger.error("At least 2 tags or releases are required to compute adjacent diffs.")
        return

    records = []
    logger.info(f"Computing src diffs across {len(tags) - 1} adjacent tag pairs from '{repo}'.")

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

            norm_src = normalize_content(raw_src)
            norm_tgt = normalize_content(raw_tgt)

            # Skip if identical
            if norm_src == norm_tgt:
                continue

            # Extract system name
            system_name = extract_system_name(raw_tgt or raw_src or "")
            file_name = Path(fpath).name

            records.append({
                "file": file_name,
                "system": system_name,
                "source_version": v_source,
                "source_content": norm_src,
                "target_version": v_target,
                "target_content": norm_tgt
            })

    generate_diff_csv(records, output_csv)

def main():
    parser = argparse.ArgumentParser(description="Analyze diffs of src/ specifications between adjacent tags.")
    parser.add_argument("--tags", nargs="+", required=True, help="Ordered list of tags or releases")
    parser.add_argument("--output-csv", default="./reports/src_diffs.csv", help="Output CSV filepath")
    parser.add_argument("--repo", default=get_default_repo(), help="GitHub repository (owner/repo)")

    args = parser.parse_args()
    compute_src_diffs(
        tags=args.tags,
        output_csv=Path(args.output_csv),
        repo=args.repo
    )

if __name__ == "__main__":
    main()
