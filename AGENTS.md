# AGENTS.md

> Universal cross-tool agent instructions read natively by Claude Code, Google Antigravity, Cursor, and any LLM agent adhering to the `AGENTS.md` standard.

This project implements the **CLARET Version Control System** agentic skills suite for Model-Based Testing (MBT) specification lifecycle management, translation, versioning, and test suite diff analysis.

---

## 1. Safety & Environment Rules

1. **Security & Secrets**: Never commit `.env` or hardcode tokens. All secrets (`GITHUB_TOKEN`, etc.) are resolved through `.env`. Use `.env.example` as a template.
2. **Branch Isolation**: Never push directly to `main`. All operations target dedicated integration branches (e.g. `claret-version-control-system` or study-specific branches).
3. **DSL Keyword Protection**: Never translate reserved tokens in `.claret` / `.dsl` files (`system`, `usecase`, `version`, `actor`, `step`, `af:`, `ef:`, `bfs:`, `alternative`, `exception`, `preCondition`, `postCondition`).

---

## 2. Available Agentic Skills & Slash Commands

| Skill Name | Claude Skill Path | Antigravity Workflow | Cursor Slash Command |
|---|---|---|---|
| **test-generator** | `.claude/skills/test-generator` | `/generate-tests` | `/generate-tests` |
| **version-publisher** | `.claude/skills/version-publisher` | `/publish-versions` | `/publish-versions` |
| **release-downloader** | `.claude/skills/release-downloader` | `/download-releases` | `/download-releases` |
| **spec-translator** | `.claude/skills/spec-translator` | `/translate-specs` | `/translate-specs` |
| **src-diff-analyzer** | `.claude/skills/src-diff-analyzer` | `/diff-src` | `/diff-src` |
| **output-diff-analyzer** | `.claude/skills/output-diff-analyzer` | `/diff-output` | `/diff-output` |

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
