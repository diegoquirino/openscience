#!/usr/bin/env python3
"""
sync_agents.py
==============
Cross-platform agent skill and workflow synchronizer.
- Mirrors .claude/skills/ into .agent/skills/ (Google Antigravity) and .cursor/skills/ (Cursor)
- Transforms .agent/workflows/ into .cursor/commands/ (Cursor slash menu)

Usage:
  python scripts/sync_agents.py          # Synchronize
  python scripts/sync_agents.py --check  # Fail if out of sync (CI validation)
"""

import sys
import shutil
import filecmp
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SRC_SKILLS = ROOT / ".claude" / "skills"
DST_AGENT_SKILLS = ROOT / ".agent" / "skills"
DST_CURSOR_SKILLS = ROOT / ".cursor" / "skills"
SRC_WORKFLOWS = ROOT / ".agent" / "workflows"
DST_CURSOR_COMMANDS = ROOT / ".cursor" / "commands"

def sync_directory_mirror(src: Path, dst: Path, platform_name: str, tool_name: str):
    """Mirror source skills directory to destination, generating README header."""
    if not src.exists():
        print(f"ERROR: Source directory {src} does not exist.")
        sys.exit(1)

    dst.mkdir(parents=True, exist_ok=True)

    # Clean existing skills in destination except README.md
    for item in dst.iterdir():
        if item.name == "README.md":
            continue
        if item.is_dir():
            shutil.rmtree(item)
        else:
            item.unlink()

    # Copy tree
    for item in src.iterdir():
        if item.name == "README.md":
            continue
        dst_item = dst / item.name
        if item.is_dir():
            shutil.copytree(item, dst_item)
        else:
            shutil.copy2(item, dst_item)

    # Create README.md
    readme_content = f"""# {platform_name} Skills Mirror — Auto-generated

This directory is an automated mirror of `.claude/skills/` enabling **{tool_name}** to discover
and execute the CLARET Version Control System agentic skills.

**Do not edit files here directly.** Edit source files in `.claude/skills/` and run:

```bash
python scripts/sync_agents.py
```

### Conventions:
- **Claude Code**: reads `.claude/skills/<name>/SKILL.md` (source of truth).
- **Google Antigravity**: reads `.agent/skills/<name>/SKILL.md` (mirror) and `.agent/workflows/<name>.md`.
- **Cursor**: reads `.cursor/skills/<name>/SKILL.md` (mirror) and slash commands in `.cursor/commands/<name>.md`.
"""
    (dst / "README.md").write_text(readme_content, encoding="utf-8")
    print(f"Synchronized skills: {src} -> {dst} ({tool_name})")

def sync_commands():
    """Convert .agent/workflows/ into .cursor/commands/ with adjusted skill path references."""
    if not SRC_WORKFLOWS.exists():
        print(f"INFO: {SRC_WORKFLOWS} does not exist — skipping Cursor commands sync.")
        return

    DST_CURSOR_COMMANDS.mkdir(parents=True, exist_ok=True)

    # Remove orphan commands
    for item in DST_CURSOR_COMMANDS.iterdir():
        if item.name == "README.md":
            continue
        expected_src = SRC_WORKFLOWS / item.name
        if not expected_src.exists():
            item.unlink()
            print(f"Removed orphan: {item.name}")

    # Generate Cursor commands
    for wf in SRC_WORKFLOWS.glob("*.md"):
        content = wf.read_text(encoding="utf-8")
        # Replace .agent/skills/ with .cursor/skills/
        adjusted = content.replace(".agent/skills/", ".cursor/skills/")
        (DST_CURSOR_COMMANDS / wf.name).write_text(adjusted, encoding="utf-8")

    readme = """# .cursor/commands/ — Auto-generated

Cursor slash commands (menu `/`), generated from `.agent/workflows/`.
Each command wrapper references the corresponding skill in `.cursor/skills/`.

**Do not edit files here directly.** Edit workflows in `.agent/workflows/` (or skills in
`.claude/skills/`) and run:

```bash
python scripts/sync_agents.py
```
"""
    (DST_CURSOR_COMMANDS / "README.md").write_text(readme, encoding="utf-8")
    print(f"Synchronized workflows: {SRC_WORKFLOWS} -> {DST_CURSOR_COMMANDS}")

def check_sync() -> bool:
    """Validate if mirrors are in sync (for CI / automated testing)."""
    # Simple check comparing file lists and contents
    all_good = True
    for dst in [DST_AGENT_SKILLS, DST_CURSOR_SKILLS]:
        if not dst.exists():
            print(f"CHECK ERROR: Destination {dst} missing.")
            all_good = False
            continue
        # Compare files
        for src_file in SRC_SKILLS.rglob("*"):
            if src_file.name == "README.md" or src_file.is_dir():
                continue
            rel = src_file.relative_to(SRC_SKILLS)
            dst_file = dst / rel
            if not dst_file.exists():
                print(f"CHECK ERROR: Missing mirrored file {dst_file}")
                all_good = False
            elif not filecmp.cmp(src_file, dst_file, shallow=False):
                print(f"CHECK ERROR: Content divergence at {dst_file}")
                all_good = False

    return all_good

def main():
    if "--check" in sys.argv:
        is_synced = check_sync()
        if not is_synced:
            print("FAILED: Skills mirrors are out of sync. Run 'python scripts/sync_agents.py'")
            sys.exit(1)
        print("OK: All skill mirrors and workflows are in sync.")
        sys.exit(0)

    sync_directory_mirror(SRC_SKILLS, DST_AGENT_SKILLS, "Google Antigravity", "Google Antigravity")
    sync_directory_mirror(SRC_SKILLS, DST_CURSOR_SKILLS, "Cursor", "Cursor IDE")
    sync_commands()
    print("Agent skills synchronization complete!")

if __name__ == "__main__":
    main()
