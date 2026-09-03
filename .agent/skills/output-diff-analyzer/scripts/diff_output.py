#!/usr/bin/env python3
"""
diff_output.py
==============
CLI script for output-diff-analyzer skill.
Extracts diffs of generated test cases in output/ between adjacent tags/releases and outputs normalized CSV.
"""

import os
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

def matches_filter(fpath: str, formats: list, scope: str, coverage: str) -> bool:
    """Check if an output file matches format, scope, and coverage filters."""
    p = Path(fpath)
    ext = p.suffix.lower().lstrip(".")
    filename = p.name.lower()

    # Format filter
    if "all" not in formats and ext not in formats:
        return False

    # Scope filter
    if scope == "all_usecases" and not filename.startswith("all_usecases"):
        return False

    # Coverage filter
    if coverage and coverage.lower() != "all":
        cov_marker = f"--{coverage.lower()}-"
        if cov_marker not in filename:
            # Check for standard suffix without hyphens e.g. _testlink
            if coverage.lower() not in filename:
                return False

    return True

def compute_output_diffs(
    tags: list,
    output_csv: Path,
    repo: str,
    formats: list,
    scope: str,
    coverage: str,
    repo_dir: Optional[Path] = None
):
    gh = GitHubManager(repo=repo, repo_dir=repo_dir)
    if len(tags) < 2:
        logger.error("At least 2 tags or releases are required to compute adjacent diffs.")
        return

    records = []
    logger.info(f"Computing granular output diffs across {len(tags) - 1} adjacent pairs from '{repo}' (Formats: {formats}, Scope: {scope}, Coverage: {coverage}).")

    for i in range(len(tags) - 1):
        v_source = tags[i].strip()
        v_target = tags[i + 1].strip()
        logger.info(f"Analyzing output diff: {v_source} -> {v_target}")

        files_source = set(gh.list_tree_at_ref(v_source, path_prefix="output"))
        files_target = set(gh.list_tree_at_ref(v_target, path_prefix="output"))
        all_files = sorted(files_source.union(files_target))

        for fpath in all_files:
            if not matches_filter(fpath, formats, scope, coverage):
                continue

            raw_src = gh.fetch_file_content_at_ref(v_source, fpath)
            raw_tgt = gh.fetch_file_content_at_ref(v_target, fpath)

            file_name = Path(fpath).name
            system_name = extract_system_name(raw_tgt or raw_src or "")

            diff_chunks = extract_granular_diffs(
                raw_src=raw_src,
                raw_tgt=raw_tgt,
                origin_version=v_source,
                target_version=v_target,
                file_name=file_name,
                system_name=system_name,
                is_dsl=False
            )
            records.extend(diff_chunks)

    generate_diff_csv(records, output_csv)

def main():
    parser = argparse.ArgumentParser(description="Analyze granular diffs of generated test cases in output/ between adjacent tags.")
    parser.add_argument("--tags", nargs="+", required=True, help="Ordered list of tags or releases")
    parser.add_argument("--formats", nargs="+", default=["txt", "xlsx"], help="Formats to inspect (e.g. txt, xlsx, docx, xml, all)")
    parser.add_argument("--scope", default="all_usecases", choices=["all_usecases", "all"], help="Scope of test cases")
    parser.add_argument("--coverage", default="all", help="Coverage suffix filter (gt, gtp, art, complete, basic, branches, all)")
    parser.add_argument("--output-csv", default="./reports/output_diffs.csv", help="Output CSV filepath")
    parser.add_argument("--repo", default=get_default_repo(), help="GitHub repository (owner/repo)")
    parser.add_argument("--repo-dir", default=None, help="Local git repository directory for offline/fast analysis")

    args = parser.parse_args()
    compute_output_diffs(
        tags=args.tags,
        output_csv=Path(args.output_csv),
        repo=args.repo,
        formats=[f.lower() for f in args.formats],
        scope=args.scope,
        coverage=args.coverage,
        repo_dir=Path(args.repo_dir) if args.repo_dir else None
    )

if __name__ == "__main__":
    main()
