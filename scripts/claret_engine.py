#!/usr/bin/env python3
"""
claret_engine.py
================
Core shared engine for the claret-versionador suite:
- Environment and configuration loader (.env)
- PascalCase file renaming with acronym preservation
- Execution of standalone claret-generator.jar
- Git and GitHub operations (Commit, Push, Tag, Release)
- Release / Tag artifact and source tree downloader
- CLARET DSL parser & locale translator (preserving language tokens)
- Text normalization & CSV diff generator for src/ and output/
"""

import os
import re
import sys
import csv
import json
import shutil
import logging
import zipfile
import tempfile
import subprocess
from pathlib import Path
from typing import List, Dict, Tuple, Optional, Any
from dotenv import load_dotenv

# Load .env configuration
load_dotenv()

# Logging configuration
LOG_LEVEL = os.getenv("LOG_LEVEL", "INFO").upper()
logging.basicConfig(
    level=getattr(logging, LOG_LEVEL, logging.INFO),
    format="%(asctime)s [%(levelname)s] %(message)s",
    datefmt="%Y-%m-%d %H:%M:%S"
)
logger = logging.getLogger("claret_engine")

# ------------------------------------------------------------------------------
# Known Technical Acronyms (to preserve in uppercase)
# ------------------------------------------------------------------------------
KNOWN_ACRONYMS = {
    "CRUD", "LLT", "DSL", "API", "XML", "JSON", "CSV", "HTML", "HTTP", "HTTPS",
    "SQL", "DB", "UI", "UX", "ID", "UUID", "SAFF", "PJE", "MBT", "TGF", "ALTS",
    "XLSX", "DOCX", "ODT", "TXT", "PDF", "REST", "SOAP", "URL", "URI", "DTO",
    "DAO", "GT", "GTP", "ART", "TC", "UC", "SPLAB", "UFCG", "CLARET"
}

# ------------------------------------------------------------------------------
# Configuration Helpers
# ------------------------------------------------------------------------------
def get_config(key: str, default: Optional[str] = None) -> Optional[str]:
    """Retrieve environment variable or fallback to default."""
    return os.getenv(key, default)

def get_github_token() -> Optional[str]:
    """Retrieve GitHub token from environment."""
    return os.getenv("GITHUB_TOKEN")

def get_default_repo() -> Optional[str]:
    """Retrieve default GitHub repository (owner/repo)."""
    return os.getenv("GITHUB_REPO", "diegoquirino/openscience")

def get_default_branch() -> str:
    """Retrieve default working branch."""
    return os.getenv("GITHUB_BRANCH", "claret-versionador")

def get_claret_jar_path() -> Path:
    """Resolve the path to claret-generator.jar."""
    jar_env = os.getenv("CLARET_JAR_PATH")
    candidates = []
    if jar_env:
        candidates.append(Path(jar_env))
        candidates.append(Path(__file__).resolve().parent.parent / jar_env)
    
    # Common workspace locations
    base_dir = Path(__file__).resolve().parent.parent
    candidates.extend([
        base_dir.parent / "claret-generator" / "target" / "claret-generator.jar",
        base_dir / "claret-generator.jar",
        base_dir / "target" / "claret-generator.jar"
    ])

    for p in candidates:
        if p.exists() and p.is_file():
            return p.resolve()
    
    # Fallback to the first specified path even if not yet compiled
    return candidates[0] if candidates else Path("claret-generator.jar")

# ------------------------------------------------------------------------------
# 1. PascalCase Naming with Acronym Retention
# ------------------------------------------------------------------------------
def to_pascal_case_with_acronyms(name: str) -> str:
    """
    Converts a filename or identifier to PascalCase while preserving acronyms in uppercase.
    Examples:
      - 'CRUD_Cliente.claret' -> 'CRUD_Cliente.claret' / 'CRUDCliente.claret'
      - 'login-minitest-alternative-format-dsl.claret' -> 'LoginMinitestAlternativeFormatDsl.claret'
      - 'extracao_LLT.claret' -> 'ExtracaoLLT.claret'
      - 'all_usecases--GT-.xlsx' -> 'AllUsecases--GT-.xlsx'
    """
    path_obj = Path(name)
    stem = path_obj.stem
    extension = path_obj.suffix

    # Special handling for composite suffixes like .dsl.claret
    if stem.endswith(".dsl"):
        stem = stem[:-4]
        extension = ".dsl" + extension

    # Tokenize by underscores, hyphens, spaces, and camelCase boundaries
    tokens = re.split(r'[-_\s]+', stem)
    processed_tokens = []

    for token in tokens:
        if not token:
            continue
        # Split further if camelCase / acronyms concatenated (e.g. ExtracaoLLT)
        sub_tokens = re.findall(r'[A-Z]+(?=[A-Z][a-z])|[A-Z]?[a-z]+|[A-Z]+|[0-9]+', token)
        if not sub_tokens:
            sub_tokens = [token]

        for st in sub_tokens:
            st_upper = st.upper()
            if st_upper in KNOWN_ACRONYMS:
                processed_tokens.append(st_upper)
            elif st.isupper() and len(st) <= 4:
                # Keep short all-caps as acronyms
                processed_tokens.append(st)
            else:
                # Capitalize first letter, keep rest lowercase
                processed_tokens.append(st.capitalize())

    new_stem = "".join(processed_tokens)
    return new_stem + extension

def format_version(version_raw: Any) -> str:
    """
    Format version number into standard X.Y format.
    E.g. 1 -> '1.0', 2 -> '2.0', '1' -> '1.0', '1.1' -> '1.1', 'v1' -> '1.0'
    """
    v_str = str(version_raw).strip().lstrip("vV")
    if not v_str:
        return "1.0"
    if "." not in v_str and v_str.isdigit():
        return f"{v_str}.0"
    return v_str

def format_branch_title(branch_name: str) -> str:
    """
    Formats a branch name into Title Case while preserving uppercase acronyms.
    E.g. 'saff-study' -> 'Saff Study', 'abc-da-net' -> 'ABC Da Net'
    """
    tokens = re.split(r'[-_\s]+', branch_name)
    formatted = []
    for t in tokens:
        if not t:
            continue
        t_upper = t.upper()
        if t_upper in KNOWN_ACRONYMS:
            formatted.append(t_upper)
        elif t.isupper() and len(t) <= 4:
            formatted.append(t)
        else:
            formatted.append(t.capitalize())
    return " ".join(formatted)

# ------------------------------------------------------------------------------
# 2. Standalone claret-generator.jar Runner
# ------------------------------------------------------------------------------
def run_claret_generator(
    input_dir: Path,
    output_dir: Optional[Path] = None,
    formats: str = "all",
    coverage: str = "gt",
    java_cmd: str = "java",
    jar_path: Optional[Path] = None
) -> Tuple[bool, str]:
    """
    Executes claret-generator.jar CLI over a given directory.
    Moves specification files to src/ and generates output/ tree.
    """
    resolved_jar = jar_path or get_claret_jar_path()
    if not resolved_jar.exists():
        msg = f"claret-generator.jar not found at {resolved_jar}. Please compile it with 'mvn package'."
        logger.error(msg)
        return False, msg

    if output_dir is None:
        output_dir = input_dir / "output"

    cmd = [
        java_cmd,
        "-jar",
        str(resolved_jar),
        "-i",
        str(input_dir),
        "-o",
        str(output_dir),
        "-f",
        formats,
        "-c",
        coverage
    ]

    logger.info(f"Executing: {' '.join(cmd)}")
    try:
        res = subprocess.run(cmd, capture_output=True, text=True, check=True)
        logger.info(f"claret-generator output:\n{res.stdout}")
        return True, res.stdout
    except subprocess.CalledProcessError as e:
        logger.error(f"claret-generator failed with exit code {e.returncode}:\n{e.stderr}")
        return False, e.stderr
    except Exception as e:
        logger.error(f"Error running claret-generator: {e}")
        return False, str(e)

# ------------------------------------------------------------------------------
# 3. Git and GitHub Operations
# ------------------------------------------------------------------------------
class GitHubManager:
    """Manages Git operations and GitHub REST API interactions."""

    def __init__(self, repo: Optional[str] = None, token: Optional[str] = None):
        self.token = token or get_github_token()
        self.repo = repo or get_default_repo()

    def run_git(self, args: List[str], cwd: Optional[Path] = None) -> Tuple[int, str, str]:
        """Run a git command in the specified working directory."""
        cmd = ["git"] + args
        logger.debug(f"Git command: {' '.join(cmd)} (cwd={cwd})")
        res = subprocess.run(cmd, capture_output=True, text=True, cwd=cwd)
        return res.returncode, res.stdout.strip(), res.stderr.strip()

    def add_commit_push(self, repo_dir: Path, branch: str, commit_message: str) -> bool:
        """Stage all changes, commit, and push to the specified branch."""
        # Ensure branch exists and is checked out
        self.run_git(["checkout", "-B", branch], cwd=repo_dir)
        self.run_git(["add", "-A"], cwd=repo_dir)

        # Check if there are changes to commit
        code, out, _ = self.run_git(["status", "--porcelain"], cwd=repo_dir)
        if not out:
            logger.info("No changes to commit.")
        else:
            code, out, err = self.run_git(["commit", "-m", commit_message], cwd=repo_dir)
            if code != 0:
                logger.error(f"Git commit failed: {err}")
                return False

        # Push to remote
        code, out, err = self.run_git(["push", "-u", "origin", branch], cwd=repo_dir)
        if code != 0:
            logger.warning(f"Git push standard attempt: {err}. Attempting with upstream.")
            code, out, err = self.run_git(["push", "--set-upstream", "origin", branch], cwd=repo_dir)
            if code != 0:
                logger.error(f"Git push failed: {err}")
                return False
        return True

    def create_tag(self, repo_dir: Path, tag_name: str, message: str) -> bool:
        """Create and push a git tag."""
        code, _, err = self.run_git(["tag", "-a", tag_name, "-m", message], cwd=repo_dir)
        if code != 0 and "already exists" not in err:
            logger.error(f"Failed to create tag {tag_name}: {err}")
            return False
        
        code, _, err = self.run_git(["push", "origin", tag_name], cwd=repo_dir)
        if code != 0:
            logger.warning(f"Failed to push tag {tag_name}: {err}")
        return True

    def create_release(self, tag_name: str, release_title: str, body: str = "") -> bool:
        """Create a GitHub Release via GitHub REST API."""
        if not self.token:
            logger.warning("No GITHUB_TOKEN provided. Skipping GitHub Release creation via API.")
            return False
        
        import requests
        url = f"https://api.github.com/repos/{self.repo}/releases"
        headers = {
            "Authorization": f"token {self.token}",
            "Accept": "application/vnd.github.v3+json"
        }
        payload = {
            "tag_name": tag_name,
            "name": release_title,
            "body": body or f"Automated release for {release_title}",
            "draft": False,
            "prerelease": False
        }
        resp = requests.post(url, headers=headers, json=payload)
        if resp.status_code in [200, 201]:
            logger.info(f"Created GitHub Release: {release_title} ({tag_name})")
            return True
        elif resp.status_code == 422 and "already_exists" in resp.text:
            logger.info(f"Release {release_title} already exists.")
            return True
        else:
            logger.error(f"Failed to create GitHub release: {resp.status_code} - {resp.text}")
            return False

    def fetch_file_content_at_ref(self, ref: str, file_path: str) -> Optional[str]:
        """Fetch file content from GitHub repository at a specific tag or ref."""
        if not self.token:
            # Fallback to public raw content
            import requests
            url = f"https://raw.githubusercontent.com/{self.repo}/{ref}/{file_path}"
            resp = requests.get(url)
            return resp.text if resp.status_code == 200 else None

        import requests
        url = f"https://api.github.com/repos/{self.repo}/contents/{file_path}?ref={ref}"
        headers = {
            "Authorization": f"token {self.token}",
            "Accept": "application/vnd.github.v3.raw"
        }
        resp = requests.get(url, headers=headers)
        if resp.status_code == 200:
            return resp.text
        return None

    def list_tree_at_ref(self, ref: str, path_prefix: str = "src") -> List[str]:
        """List all file paths under path_prefix at a given ref/tag."""
        if not self.token:
            logger.warning("GITHUB_TOKEN missing; cannot inspect remote git tree via API.")
            return []

        import requests
        url = f"https://api.github.com/repos/{self.repo}/git/trees/{ref}?recursive=1"
        headers = {
            "Authorization": f"token {self.token}",
            "Accept": "application/vnd.github.v3+json"
        }
        resp = requests.get(url, headers=headers)
        if resp.status_code != 200:
            logger.error(f"Error fetching tree for ref {ref}: {resp.status_code}")
            return []

        data = resp.json()
        tree = data.get("tree", [])
        files = []
        for item in tree:
            if item.get("type") == "blob":
                p = item.get("path", "")
                if p.startswith(path_prefix):
                    files.append(p)
        return files

# ------------------------------------------------------------------------------
# 4. CLARET DSL Token-Safe Translator
# ------------------------------------------------------------------------------
CLARET_RESERVED_KEYWORDS = [
    "system", "usecase", "version", "type", "user", "date", "Creation", "Modification",
    "actor", "preCondition", "basicFlow", "step", "af", "ef", "bfs", "alternative",
    "exception", "postCondition"
]

class ClaretTranslator:
    """
    Translates textual requirements inside .claret files while preserving DSL grammar & keywords.
    """

    def __init__(self, target_locale: str = "en-us"):
        self.target_locale = target_locale.lower()
        self.pt_to_en_lexicon = {
            "Estar logado como super administrador": "Be logged in as super administrator",
            "Estar logado como administrador": "Be logged in as administrator",
            "Estar logado no sistema": "Be logged in the system",
            "Super administrador": "Super administrator",
            "Administrador": "Administrator",
            "Usuário": "User",
            "exibe lista de clientes": "displays customer list",
            "exibe página com campos de cadastro de novo cliente": "displays page with new customer registration fields",
            "exibe página com campos de cadastro de notvo cliente": "displays page with new customer registration fields",
            "exibe página com dados do novo cliente e mensagem que um novo cliente foi adicionado": "displays page with new customer data and confirmation message",
            "exibe página com dados do notvo cliente e mensagem que um notvo cliente foi adicionado": "displays page with new customer data and confirmation message",
            "clica no nome de um cliente": "clicks on a customer name",
            "clica not notme de um cliente": "clicks on a customer name",
            "exibe dados do cliente": "displays customer data",
            "retorna a lista de clientes": "returns to customer list",
            "Cancela criação": "Cancel creation",
            "Edita dados": "Edit data",
            "exibe campos do cliente com valores atuais que podem ser editados": "displays customer fields with current values that can be edited",
            "preenche os novos valores e clicks the button 'Update": "fills in new values and clicks the button 'Update'",
            "preenche os notvos valores e clicks the button 'Update": "fills in new values and clicks the button 'Update'",
            "Deleta cliente": "Delete customer",
            "exibe mensagem solicitando confirmação": "displays confirmation message",
            "preenche corretamente os campos": "correctly fills in fields",
        }

    def translate_text(self, text: str) -> str:
        """Translate a single natural language string chunk."""
        cleaned = text.strip()
        if cleaned in self.pt_to_en_lexicon:
            return self.pt_to_en_lexicon[cleaned]

        # Simple substitution dictionary pass for common phrases
        result = text
        for pt, en in self.pt_to_en_lexicon.items():
            result = result.replace(pt, en)
        
        # Replace remaining typical PT phrases if translating to EN
        if "en" in self.target_locale:
            replacements = [
                ("exibe ", "displays "),
                ("solicitando confirmação", "requesting confirmation"),
                ("retorna a ", "returns to "),
                ("preenche ", "fills "),
                ("Cancela ", "Cancels "),
                ("Edita ", "Edits "),
                ("Deleta ", "Deletes "),
                ("Cria ", "Creates "),
                ("Salva ", "Saves "),
            ]
            for pt, en in replacements:
                result = result.replace(pt, en)

        return result

    def translate_claret_content(self, content: str) -> str:
        """
        Translates .claret content line by line, ensuring reserved keywords and structure are preserved.
        """
        lines = content.splitlines()
        translated_lines = []

        for line in lines:
            # 1. Quoted string translation: "..."
            def replace_quote(match):
                inner_text = match.group(1)
                # Check if it's a metadata keyword that shouldn't be touched
                if inner_text in ["Creation", "Modification"]:
                    return f'"{inner_text}"'
                # If date or version format, leave untouched
                if re.match(r'^\d+(\.\d+)*$', inner_text) or re.match(r'^\d{2}/\d{2}/\d{4}$', inner_text):
                    return f'"{inner_text}"'
                translated = self.translate_text(inner_text)
                return f'"{translated}"'

            # Replace quoted strings
            new_line = re.sub(r'"([^"]*)"', replace_quote, line)
            translated_lines.append(new_line)

        return "\n".join(translated_lines)

# ------------------------------------------------------------------------------
# 5. Diff Normalization and CSV Generation
# ------------------------------------------------------------------------------
def normalize_content(content: Optional[str]) -> str:
    """
    Normalizes content according to the specification:
    - Converted to UTF-8
    - Lowercase
    - Collapses adjacent whitespace characters into a single space
    - Collapses adjacent newlines into a single newline
    """
    if content is None:
        return ""
    # Ensure utf-8 text
    if isinstance(content, bytes):
        text = content.decode("utf-8", errors="replace")
    else:
        text = str(content)

    text = text.lower()
    # Normalize lines: split, collapse internal spaces, remove redundant blank lines
    lines = text.splitlines()
    normalized_lines = []
    for l in lines:
        cleaned_line = re.sub(r'[ \t]+', ' ', l).strip()
        if cleaned_line:
            normalized_lines.append(cleaned_line)

    return "\n".join(normalized_lines)

def extract_system_name(content: str) -> str:
    """Extract system name from .claret file content."""
    m = re.search(r'system\s+"([^"]+)"', content, re.IGNORECASE)
    if m:
        return m.group(1)
    return "UnknownSystem"

def generate_diff_csv(
    diff_records: List[Dict[str, Any]],
    output_csv_path: Path
) -> Path:
    """
    Writes diff records to CSV matching the format:
    | # | file | system | source_version | source_content | target_version | target_content |
    """
    output_csv_path.parent.mkdir(parents=True, exist_ok=True)
    fieldnames = ["#", "file", "system", "source_version", "source_content", "target_version", "target_content"]

    with open(output_csv_path, mode="w", newline="", encoding="utf-8") as f:
        writer = csv.DictWriter(f, fieldnames=fieldnames)
        writer.writeheader()
        for idx, rec in enumerate(diff_records, start=1):
            writer.writerow({
                "#": idx,
                "file": rec.get("file", ""),
                "system": rec.get("system", ""),
                "source_version": rec.get("source_version", ""),
                "source_content": rec.get("source_content", ""),
                "target_version": rec.get("target_version", ""),
                "target_content": rec.get("target_content", "")
            })
    logger.info(f"Diff CSV generated at: {output_csv_path} with {len(diff_records)} records.")
    return output_csv_path
