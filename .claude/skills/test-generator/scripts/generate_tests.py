#!/usr/bin/env python3
"""
generate_tests.py
=================
CLI script for the test-generator skill.
Batch executes claret-generator.jar across a list of version directories (specified in --dirs)
to generate output/ artifacts from .claret / .dsl specification files in src/ (or root).
"""

import os
import sys
import shutil
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
    run_claret_generator,
    get_claret_jar_path,
    logger
)

def resolve_directory_path(dir_str: str) -> Optional[Path]:
    """Resolve directory path against absolute or relative project paths."""
    p = Path(dir_str)
    if p.is_absolute() and p.exists() and p.is_dir():
        return p

    # Check relative to current working directory
    if Path.cwd().joinpath(dir_str).is_dir():
        return Path.cwd().joinpath(dir_str).resolve()

    # Check relative to PROJECT_ROOT
    if (PROJECT_ROOT / dir_str).is_dir():
        return (PROJECT_ROOT / dir_str).resolve()

    # Check relative to parent workspace
    if (PROJECT_ROOT.parent / dir_str).is_dir():
        return (PROJECT_ROOT.parent / dir_str).resolve()

    return None

def count_spec_files(dir_path: Path) -> int:
    """Count .claret and .dsl files in directory tree."""
    count = 0
    for root, _, files in os.walk(dir_path):
        # Ignore output directory when searching for source specs
        if "output" in Path(root).parts:
            continue
        for f in files:
            if f.endswith(".claret") or f.endswith(".dsl"):
                count += 1
    return count

def generate_tests_batch(
    dirs: List[str],
    coverage: str = "gt",
    formats: str = "all",
    flat: bool = False,
    clean: bool = False,
    jar_path: Optional[str] = None
) -> bool:
    """
    Processes each directory in dirs, generating output/ using claret-generator.jar.
    """
    resolved_jar = Path(jar_path).resolve() if jar_path else get_claret_jar_path()
    if not resolved_jar.exists():
        logger.error(f"claret-generator.jar not found at {resolved_jar}. Please compile it with 'mvn package'.")
        return False

    logger.info(f"Using claret-generator: {resolved_jar}")
    logger.info(f"Target directories count: {len(dirs)}")
    logger.info(f"Coverage criteria: {coverage} | Formats: {formats} | Flat: {flat} | Clean: {clean}")

    results = []
    all_success = True

    for idx, d_str in enumerate(dirs, start=1):
        d_path = resolve_directory_path(d_str)
        if not d_path:
            logger.error(f"[{idx}/{len(dirs)}] Directory not found: {d_str}")
            results.append((d_str, "NOT FOUND", 0, "Directory does not exist"))
            all_success = False
            continue

        specs_count = count_spec_files(d_path)
        if specs_count == 0:
            logger.warning(f"[{idx}/{len(dirs)}] No .claret or .dsl files found in {d_path}")
            results.append((d_path.name, "NO SPECS", 0, "No .claret or .dsl files found"))
            continue

        output_dir = d_path / "output"

        if clean and output_dir.exists():
            logger.info(f"Cleaning existing output directory: {output_dir}")
            shutil.rmtree(output_dir, ignore_errors=True)

        logger.info(f"\n=======================================================")
        logger.info(f"[{idx}/{len(dirs)}] Processing directory: {d_path.name} ({specs_count} spec files)")
        logger.info(f"Output path: {output_dir}")
        logger.info(f"=======================================================")

        success, log_output = run_claret_generator(
            input_dir=d_path,
            output_dir=output_dir,
            formats=formats,
            coverage=coverage,
            flat=flat,
            jar_path=resolved_jar
        )

        if success:
            logger.info(f"-> Successfully generated test suite in: {output_dir}")
            results.append((d_path.name, "SUCCESS", specs_count, f"Generated in {output_dir.name}/"))
        else:
            logger.error(f"-> Failed to generate test suite for {d_path.name}")
            results.append((d_path.name, "FAILED", specs_count, log_output.strip().splitlines()[-1] if log_output else "Unknown error"))
            all_success = False

    # Print Summary Table
    print("\n" + "=" * 70)
    print(" BATCH GENERATION SUMMARY")
    print("=" * 70)
    print(f"{'Directory':<20} | {'Status':<10} | {'Specs':<6} | {'Details'}")
    print("-" * 70)
    for d_name, status, count, detail in results:
        print(f"{d_name:<20} | {status:<10} | {count:<6} | {detail}")
    print("=" * 70 + "\n")

    return all_success

def main():
    parser = argparse.ArgumentParser(
        description="Batch generate MBT test cases and models in output/ for specified version directories."
    )
    parser.add_argument(
        "--dirs",
        nargs="+",
        required=True,
        help="List of version directories to process (e.g., --dirs 1.0 1.1 1.2 or 20150617 20150618)"
    )
    parser.add_argument(
        "-c", "--coverage",
        default="gt",
        choices=["gt", "gtp", "art", "complete", "basic-only", "all-branches"],
        help="MBT coverage criteria (default: gt)"
    )
    parser.add_argument(
        "-f", "--formats",
        default="all",
        help="Comma-separated output formats (all, xlsx, txt, docx, odt, xml, tgf, alts) [default: all]"
    )
    parser.add_argument(
        "--flat",
        action="store_true",
        help="Output all files flatly in output/ without format subdirectories"
    )
    parser.add_argument(
        "--clean",
        action="store_true",
        help="Remove existing output/ directory before generating"
    )
    parser.add_argument(
        "--jar-path",
        default=None,
        help="Explicit path to claret-generator.jar"
    )

    args = parser.parse_args()
    success = generate_tests_batch(
        dirs=args.dirs,
        coverage=args.coverage,
        formats=args.formats,
        flat=args.flat,
        clean=args.clean,
        jar_path=args.jar_path
    )
    sys.exit(0 if success else 1)

if __name__ == "__main__":
    main()
