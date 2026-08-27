#!/usr/bin/env python3
"""
translate_specs.py
==================
CLI script for the spec-translator skill.
Translates human language inside .claret files while strictly preserving DSL keywords and structure.
"""

import os
import sys
import argparse
from pathlib import Path

# Add project root scripts to sys.path
SCRIPT_DIR = Path(__file__).resolve().parent
PROJECT_ROOT = SCRIPT_DIR.parents[2]
sys.path.insert(0, str(PROJECT_ROOT / "scripts"))

from claret_engine import (
    ClaretTranslator,
    logger
)

def translate_directories(
    dirs: list,
    locale: str = "en-us",
    output_dir: str = None,
    in_place: bool = False
):
    translator = ClaretTranslator(target_locale=locale)
    out_base = Path(output_dir) if output_dir else None

    logger.info(f"Starting CLARET translation to locale '{locale}' across {len(dirs)} directories.")

    total_translated = 0
    for d_entry in dirs:
        d_path = Path(d_entry)
        if not d_path.is_absolute():
            if (PROJECT_ROOT.parent / d_entry).exists():
                d_path = PROJECT_ROOT.parent / d_entry
            elif (PROJECT_ROOT / d_entry).exists():
                d_path = PROJECT_ROOT / d_entry

        if not d_path.exists():
            logger.warning(f"Directory not found: {d_entry}")
            continue

        for root_p, _, files in os.walk(d_path):
            for file in files:
                if file.endswith(".claret") or file.endswith(".dsl"):
                    f_path = Path(root_p) / file
                    original_content = f_path.read_text(encoding="utf-8", errors="replace")
                    translated_content = translator.translate_claret_content(original_content)

                    if in_place or out_base is None:
                        target_file = f_path
                    else:
                        rel_p = f_path.relative_to(d_path)
                        target_file = out_base / d_path.name / rel_p

                    target_file.parent.mkdir(parents=True, exist_ok=True)
                    target_file.write_text(translated_content, encoding="utf-8")
                    logger.debug(f"Translated: {f_path} -> {target_file}")
                    total_translated += 1

    logger.info(f"Translation completed: {total_translated} files translated to '{locale}'.")

def main():
    parser = argparse.ArgumentParser(description="Translate .claret files while preserving DSL keywords.")
    parser.add_argument("--dirs", nargs="+", required=True, help="Directories containing .claret files")
    parser.add_argument("--locale", default="en-us", help="Target locale (e.g. en-us, pt-br)")
    parser.add_argument("--output-dir", default=None, help="Output directory (optional)")
    parser.add_argument("--in-place", action="store_true", help="Overwrite files in-place")

    args = parser.parse_args()
    translate_directories(
        dirs=args.dirs,
        locale=args.locale,
        output_dir=args.output_dir,
        in_place=args.in_place
    )

if __name__ == "__main__":
    main()
