# AGENTS.md

> Universal cross-tool agent instructions read natively by Claude Code, Google Antigravity, Cursor, and any LLM agent adhering to the `AGENTS.md` standard.

This project implements the **CLARET Version Control System** agentic skills suite for Model-Based Testing (MBT) specification lifecycle management, versioning, and test suite diff analysis.

---

## 1. Safety & Environment Rules

1. **Security & Secrets**: Never commit `.env` or hardcode tokens. All secrets (`GITHUB_TOKEN`, etc.) are resolved through `.env`. Use `.env.example` as a template.
2. **Branch Isolation**: Never push directly to `main`. All operations target dedicated integration branches (e.g. `claret-version-control-system` or study-specific branches).
3. **DSL Keyword Protection**: Never alter reserved tokens in `.claret` / `.dsl` files (`system`/`systemName`, `usecase`, `version`, `requirements`, `actor`, `basic`/`basicFlow`, `step`, `af[]`/`af:[]`, `ef[]`/`ef:[]`, `bs`/`bfs:`, `alternative`, `exception`, `preCondition`, `postCondition`).
4. **Both CLARET Syntaxes**: The engine accepts the two syntaxes of `claret-generator` as equivalent: `step 3 user "..." af[1]` / `bs 4` / `alternative 1 "..." {` and `step 3, user, "...", af:[1]` / `bfs:4` / `alternative 1, "...", {`. Use the helpers in `scripts/claret_engine.py` (`branch_annotations`, `step_actor`, `CLARET_*_RE`) instead of new syntax-specific regexes; a change of syntax alone is not a behavioral change.

---

## 2. Available Agentic Skills & Slash Commands

| Skill Name | Claude Skill Path | Antigravity Workflow | Cursor Slash Command |
|---|---|---|---|
| **test-generator** | `.claude/skills/test-generator` | `/generate-tests` | `/generate-tests` |
| **version-publisher** | `.claude/skills/version-publisher` | `/publish-versions` | `/publish-versions` |
| **release-downloader** | `.claude/skills/release-downloader` | `/download-releases` | `/download-releases` |
| **src-diff-analyzer** | `.claude/skills/src-diff-analyzer` | `/diff-src` | `/diff-src` |
| **output-diff-analyzer** | `.claude/skills/output-diff-analyzer` | `/diff-output` | `/diff-output` |
| **cia-analyzer** | `.claude/skills/cia-analyzer` | `/cia` | `/cia` |
| **ucs-to-claret** | `.claude/skills/ucs-to-claret` | `/ucs-to-claret` | `/ucs-to-claret` |

---

## 3. How Agents Discover Skills

| Agent | Skills Discovery Path | Slash Command Discovery |
|---|---|---|
| **Claude Code** | `.claude/skills/<name>/SKILL.md` (Source of Truth) | Automatic via skill frontmatter |
| **Google Antigravity** | `.agent/skills/<name>/SKILL.md` (Mirrored) | `.agent/workflows/<name>.md` |
| **Cursor** | `.cursor/skills/<name>/SKILL.md` (Mirrored) | `.cursor/commands/<name>.md` |

To synchronize all skills and commands across tools, run:

```bash
python scripts/sync_agents.py
```
