# CLARET Versionador
### Cross-Agent Skills Suite for MBT Specification Versioning, Translation, and Test Suite Diffing

**CLARET Versionador** is an agentic tool suite designed for managing the evolutionary lifecycle of **CLARET** (*CentraL Artifact for Requirement Engineering and model-based Testing*) and DSL specifications. Built natively for **Claude Code** and interchangeable with **Google Antigravity** and **Cursor IDE**, this project automates specification compilation, GitHub release versioning, token-safe translation, and diff extraction for Model-Based Testing (MBT) studies.

---

## 1. Project Directory Structure

```text
claret-versionador/
├── .env.example                          <-- Template for environment variables
├── .gitignore                            <-- Git ignore rules
├── AGENTS.md                             <-- Universal cross-agent manifest
├── README.md                             <-- Project documentation (en-US)
├── requirements.txt                      <-- Python dependencies
├── scripts/
│   ├── claret_engine.py                  <-- Core shared engine (Git, GitHub, JAR, Normalizer)
│   ├── sync_agents.py                    <-- Cross-platform agent skill synchronizer
│   └── sync-agents.sh                    <-- Bash synchronizer wrapper
│
├── .claude/skills/                       <-- SOURCE OF TRUTH FOR SKILLS
│   ├── version-publisher/                <-- Feature 1: Process, generate test suite, commit, tag & release
│   │   ├── SKILL.md
│   │   └── scripts/publish_versions.py
│   ├── release-downloader/               <-- Feature 2: Download src/ from specific tags/releases
│   │   ├── SKILL.md
│   │   └── scripts/download_releases.py
│   ├── spec-translator/                  <-- Feature 3: Translate .claret specs preserving DSL grammar
│   │   ├── SKILL.md
│   │   └── scripts/translate_specs.py
│   ├── src-diff-analyzer/                <-- Feature 4: Adjacent tag diff for src/ .claret files (CSV)
│   │   ├── SKILL.md
│   │   └── scripts/diff_src.py
│   └── output-diff-analyzer/             <-- Feature 5: Adjacent tag diff for output/ test cases (CSV)
│       ├── SKILL.md
│       └── scripts/diff_output.py
│
├── .agent/                               <-- GOOGLE ANTIGRAVITY MIRROR (Auto-generated)
│   ├── skills/                           <-- Mirrored from .claude/skills/
│   └── workflows/                        <-- Antigravity workflows
│
└── .cursor/                              <-- CURSOR IDE MIRROR (Auto-generated)
    ├── skills/                           <-- Mirrored from .claude/skills/
    └── commands/                         <-- Cursor slash menu commands
```

---

## 2. Environment Setup & Configuration

1. Copy `.env.example` to `.env`:
   ```bash
   cp .env.example .env
   ```
2. Fill in your environment variables:
   - `GITHUB_TOKEN`: Personal Access Token with repository and release privileges.
   - `GITHUB_REPO`: Default GitHub repository (`owner/repo`, e.g., `diegoquirino/openscience`).
   - `CLARET_JAR_PATH`: Path to the compiled `claret-generator.jar` (e.g. `../claret-generator/target/claret-generator.jar`).

3. Install Python dependencies:
   ```bash
   pip install -r requirements.txt
   ```

---

## 3. Skills Matrix & Usage

### 1. `version-publisher` (`/publish-versions`)
Processes an array of version directories, renames files to PascalCase (preserving acronyms), executes `claret-generator.jar`, pushes to branch, creates tag (`<branch>_vX.Y`), and creates release (`<Branch Title> vX.Y`).

```bash
python .claude/skills/version-publisher/scripts/publish_versions.py \
  --version-dirs 20150617 20150618 20150619 \
  --branch saff-study
```

### 2. `release-downloader` (`/download-releases`)
Downloads exclusively the `src/` directory from a list of tags or releases into local segregated folders.

```bash
python .claude/skills/release-downloader/scripts/download_releases.py \
  --tags saff-study_v1.0 saff-study_v2.0 \
  --output-dir ./downloads
```

### 3. `spec-translator` (`/translate-specs`)
Translates natural language inside `.claret` specifications across directories to a target locale (e.g. `en-us`, `pt-br`) while strictly preserving DSL tokens and grammar.

```bash
python .claude/skills/spec-translator/scripts/translate_specs.py \
  --dirs 20150617 20150618 \
  --locale en-us
```

### 4. `src-diff-analyzer` (`/diff-src`)
Extracts diffs between adjacent tags/releases for `.claret` files in `src/` and exports a normalized CSV report:
`| # | file | system | source_version | source_content | target_version | target_content |`

```bash
python .claude/skills/src-diff-analyzer/scripts/diff_src.py \
  --tags saff-study_v1.0 saff-study_v2.0 saff-study_v3.0 \
  --output-csv ./reports/src_diffs.csv
```

### 5. `output-diff-analyzer` (`/diff-output`)
Extracts diffs between adjacent tags/releases for generated test cases in `output/` (filtering by format, scope `all_usecases` vs `all`, and coverage criteria like `GT`, `GTP`, `ART`) and exports a normalized CSV report.

```bash
python .claude/skills/output-diff-analyzer/scripts/diff_output.py \
  --tags saff-study_v1.0 saff-study_v2.0 \
  --formats txt xlsx \
  --scope all_usecases \
  --coverage gt \
  --output-csv ./reports/output_diffs.csv
```

---

## 4. Agent Synchronization

Whenever you add or modify skills in `.claude/skills/` or workflows in `.agent/workflows/`, synchronize all mirrors by running:

```bash
python scripts/sync_agents.py
```
