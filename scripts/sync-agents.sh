#!/usr/bin/env bash
# sync-agents.sh — Mirrors .claude/skills/ into .agent/skills/ and .cursor/skills/
# and generates .cursor/commands/ from .agent/workflows/.
#
# Usage:
#   ./scripts/sync-agents.sh          # Synchronize
#   ./scripts/sync-agents.sh --check  # Fail if out of sync (CI)

set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
python3 "$ROOT/scripts/sync_agents.py" "$@" 2>/dev/null || python "$ROOT/scripts/sync_agents.py" "$@"
