#!/usr/bin/env python3
"""
cia_analyzer.py
===============
Change Impact Analysis (CIA) Engine:
Maps granular specification diffs (.claret under src/) between adjacent versions
directly to the affected Test Cases (CTs) in generated test suites (output/txt/).

Produces reports/src-output_cia.csv with original diff columns complemented by:
- usecase
- change_nature
- impact_type (UPDATED, ADDED, REMOVED, NO_IMPACT)
- affected_cts_count
- affected_cts_origin
- affected_cts_target
- affected_flows
- impact_summary
"""

import re
import sys
import argparse
from pathlib import Path
from typing import List, Optional

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
    extract_system_name,
    extract_granular_diffs,
    analyze_diff_impact,
    generate_cia_csv,
    get_default_repo,
    logger
)

def sort_version_key(tag_or_folder: str):
    """Extract numeric version tuple for natural version sorting (e.g. 1.0 < 1.10 < 2.0)."""
    m = re.findall(r'\d+', tag_or_folder)
    return [int(x) for x in m] if m else [0]

def discover_tags_from_downloads(downloads_dir: Path) -> List[str]:
    """Auto-discovers and naturally sorts version directories from downloads/."""
    if not downloads_dir or not downloads_dir.is_dir():
        return []
    dirs = [d.name for d in downloads_dir.iterdir() if d.is_dir()]
    return sorted(dirs, key=sort_version_key)

def compute_cia(
    tags: Optional[List[str]],
    output_csv: Path,
    repo: str,
    repo_dir: Optional[Path] = None,
    downloads_dir: Optional[Path] = None
):
    gh = GitHubManager(repo=repo, repo_dir=repo_dir, downloads_dir=downloads_dir)

    resolved_tags = tags
    if not resolved_tags:
        resolved_tags = discover_tags_from_downloads(gh.downloads_dir)
        logger.info(f"Auto-discovered {len(resolved_tags)} versions in downloads: {resolved_tags}")

    if not resolved_tags or len(resolved_tags) < 2:
        logger.error("At least 2 versions/tags are required to perform Change Impact Analysis.")
        return False

    cia_records = []
    total_pairs = len(resolved_tags) - 1
    logger.info(f"Starting Change Impact Analysis (CIA) across {total_pairs} adjacent pairs.")
    logger.info(f"Using downloads directory: {gh.downloads_dir}")

    total_diff_count = 0
    impacted_count = 0

    for i in range(total_pairs):
        v_source = resolved_tags[i].strip()
        v_target = resolved_tags[i + 1].strip()
        logger.info(f"\n[{i + 1}/{total_pairs}] Analyzing CIA: {v_source} -> {v_target}")

        files_source = set(gh.list_tree_at_ref(v_source, path_prefix="src"))
        files_target = set(gh.list_tree_at_ref(v_target, path_prefix="src"))
        all_files = sorted(files_source.union(files_target))

        for fpath in all_files:
            if not (fpath.endswith(".claret") or fpath.endswith(".dsl")):
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
                is_dsl=True
            )

            for chunk in diff_chunks:
                total_diff_count += 1
                # Run CIA impact mapping
                cia_info = analyze_diff_impact(chunk, gh)
                
                # Combine base diff fields with CIA fields
                enriched_record = {**chunk, **cia_info}
                cia_records.append(enriched_record)

                if cia_info.get("impact_type") in ["UPDATED", "ADDED", "REMOVED"]:
                    impacted_count += 1

    # Save output CSV
    generate_cia_csv(cia_records, output_csv)

    # Summary statistics for truth table
    low_impact_count = sum(1 for r in cia_records if r.get("actual_change_impact") == "Low")
    high_impact_count = sum(1 for r in cia_records if r.get("actual_change_impact") == "High")

    tcm_counts = {}
    prim_counts = {}
    total_affected_cts = 0
    total_not_affected_cts = 0

    for r in cia_records:
        op = r.get("tcm_operation", "UNKNOWN")
        pr = r.get("primitive_operation", "UNKNOWN")
        tcm_counts[op] = tcm_counts.get(op, 0) + 1
        prim_counts[pr] = prim_counts.get(pr, 0) + 1
        total_affected_cts += r.get("affected_cts_count", 0)
        total_not_affected_cts += r.get("not_affected_cts_count", 0)

    print("\n" + "=" * 80)
    print(" GROUND TRUTH CHANGE IMPACT ANALYSIS (CIA) — SUMMARY")
    print("=" * 80)
    print(f"Total Spec Diffs Analyzed      : {total_diff_count}")
    print(f"  - Actual Low Impact (Syntactic)  : {low_impact_count:>4} (Keep / Retain)")
    print(f"  - Actual High Impact (Semantic)  : {high_impact_count:>4} (Update, Create, Remove, etc.)")
    print(f"Total Affected CT Occurrences  : {total_affected_cts:>4}")
    print(f"Total Not Affected CT Occurrences: {total_not_affected_cts:>4}")
    print("-" * 80)
    print(" TCM OPERATIONS (8-Operation Taxonomy):")
    for op in ["Keep", "Update", "Reassign", "Remove", "Create", "Merge", "Split", "Flag"]:
        cnt = tcm_counts.get(op, 0)
        print(f"  - {op:<10}: {cnt:>4} diffs")
    print("-" * 80)
    print(" PRIMITIVE OPERATIONS (4-Primitive Taxonomy):")
    for pr in ["Retain", "Modify", "Create", "Discard"]:
        cnt = prim_counts.get(pr, 0)
        print(f"  - {pr:<10}: {cnt:>4} diffs")
    print("=" * 80)
    print(f"Truth table successfully generated at: {output_csv}\n")

    return True

def main():
    parser = argparse.ArgumentParser(
        description="Change Impact Analysis (CIA): Connects src specification diffs to affected test cases in output/txt/."
    )
    parser.add_argument(
        "--tags",
        nargs="*",
        default=None,
        help="Ordered list of versions/tags. If omitted, auto-discovers all versions in downloads/ in natural order."
    )
    parser.add_argument(
        "--output-csv",
        default="./reports/src-output_cia.csv",
        help="Path for generated CIA CSV (default: ./reports/src-output_cia.csv)"
    )
    parser.add_argument(
        "--downloads-dir",
        default=None,
        help="Path to downloads/ directory (defaults to auto-discovery)"
    )
    parser.add_argument(
        "--repo",
        default=get_default_repo(),
        help="GitHub repository (owner/repo)"
    )
    parser.add_argument(
        "--repo-dir",
        default=None,
        help="Local git repository directory"
    )

    args = parser.parse_args()
    success = compute_cia(
        tags=args.tags,
        output_csv=Path(args.output_csv),
        repo=args.repo,
        repo_dir=Path(args.repo_dir) if args.repo_dir else None,
        downloads_dir=Path(args.downloads_dir) if args.downloads_dir else None
    )
    sys.exit(0 if success else 1)

if __name__ == "__main__":
    main()
