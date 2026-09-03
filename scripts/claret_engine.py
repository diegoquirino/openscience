#!/usr/bin/env python3
"""
claret_engine.py
================
Core shared engine for the claret-version-control-system suite:
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
    return os.getenv("GITHUB_BRANCH", "claret-version-control-system")

def get_java_cmd() -> str:
    """Resolve Java 26+ runtime command, prioritizing explicitly configured modern JDKs."""
    java_env = os.getenv("JAVA_CMD")
    if java_env and java_env.strip() != "java" and Path(java_env).exists():
        return java_env

    # Search for modern JDKs (Java 26 / 21) before falling back to system PATH
    common_jdk_paths = [
        Path(r"C:\Program Files\Java\jdk-26.0.1\bin\java.exe"),
        Path(r"C:\Program Files\Java\latest\bin\java.exe"),
        Path(r"C:\Program Files\Java\jdk-26\bin\java.exe"),
        Path(r"C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot\bin\java.exe"),
        Path("/usr/lib/jvm/java-26-openjdk/bin/java"),
        Path("/usr/lib/jvm/java-21-openjdk/bin/java"),
    ]
    for p in common_jdk_paths:
        if p.exists() and p.is_file():
            return str(p)

    java_home = os.getenv("JAVA_HOME")
    if java_home:
        candidate = Path(java_home) / "bin" / ("java.exe" if sys.platform == "win32" else "java")
        if candidate.exists():
            return str(candidate)

    return java_env if java_env else "java"

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
        base_dir / "bin" / "claret-generator.jar",
        base_dir / "lib" / "claret-generator.jar",
        base_dir / "claret-generator.jar",
        base_dir / "target" / "claret-generator.jar",
        base_dir.parent / "claret-generator" / "target" / "claret-generator.jar"
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
    flat: bool = False,
    java_cmd: Optional[str] = None,
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

    resolved_java = java_cmd or get_java_cmd()
    if output_dir is None:
        output_dir = input_dir / "output"

    cmd = [
        resolved_java,
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
    if flat:
        cmd.append("--flat")

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

    def __init__(self, repo: Optional[str] = None, token: Optional[str] = None, repo_dir: Optional[Path] = None):
        self.token = token or get_github_token()
        self.repo = repo or get_default_repo()
        self.repo_dir = Path(repo_dir).resolve() if repo_dir else self._discover_local_repo()
        self._cache: Dict[Tuple[str, str], Optional[str]] = {}
        self._tree_cache: Dict[Tuple[str, str], List[str]] = {}
        self._session = None

    def _discover_local_repo(self) -> Optional[Path]:
        branch = os.getenv("GITHUB_BRANCH", "saff-study")
        candidates = [
            Path.cwd() / branch,
            Path.cwd(),
            Path(__file__).resolve().parents[2] / branch,
            Path(__file__).resolve().parents[1] / branch
        ]
        for c in candidates:
            if (c / ".git").is_dir():
                return c
        return None

    def _local_repo_has_ref(self, ref: str) -> bool:
        if not self.repo_dir or not (self.repo_dir / ".git").is_dir():
            return False
        code, out, _ = self.run_git(["tag", "-l", ref], cwd=self.repo_dir)
        if code == 0 and out.strip() == ref:
            return True
        code, out, _ = self.run_git(["rev-parse", "--verify", f"refs/tags/{ref}"], cwd=self.repo_dir)
        if code == 0:
            return True
        code, out, _ = self.run_git(["rev-parse", "--verify", ref], cwd=self.repo_dir)
        return code == 0

    def _get_session(self):
        if self._session is None:
            import requests
            from requests.adapters import HTTPAdapter
            from urllib3.util import Retry
            s = requests.Session()
            retries = Retry(total=5, backoff_factor=1, status_forcelist=[429, 500, 502, 503, 504], raise_on_status=False)
            adapter = HTTPAdapter(max_retries=retries, pool_connections=10, pool_maxsize=20)
            s.mount("https://", adapter)
            s.mount("http://", adapter)
            self._session = s
        return self._session

    def run_git(self, args: List[str], cwd: Optional[Path] = None) -> Tuple[int, str, str]:
        """Run a git command in the specified working directory."""
        cmd = ["git"] + args
        logger.debug(f"Git command: {' '.join(cmd)} (cwd={cwd})")
        res = subprocess.run(cmd, capture_output=True, text=True, encoding="utf-8", errors="replace", cwd=cwd)
        stdout = res.stdout.strip() if res.stdout else ""
        stderr = res.stderr.strip() if res.stderr else ""
        return res.returncode, stdout, stderr

    def add_commit_push(self, repo_dir: Path, branch: str, commit_message: str) -> bool:
        """Stage all changes, commit, and push to the specified branch."""
        # Ensure directory is a valid git repository
        rc, _, _ = self.run_git(["rev-parse", "--is-inside-work-tree"], cwd=repo_dir)
        if rc != 0:
            self.run_git(["init"], cwd=repo_dir)
            if self.token:
                remote_url = f"https://x-access-token:{self.token}@github.com/{self.repo}.git"
            else:
                remote_url = f"https://github.com/{self.repo}.git"
            self.run_git(["remote", "remove", "origin"], cwd=repo_dir)
            self.run_git(["remote", "add", "origin", remote_url], cwd=repo_dir)

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
        """Fetch file content from GitHub repository or local git tree at a specific tag or ref."""
        cache_key = (ref, file_path)
        if cache_key in self._cache:
            return self._cache[cache_key]

        # 1. Try local git repository if available
        if self._local_repo_has_ref(ref):
            code, out, _ = self.run_git(["show", f"{ref}:{file_path}"], cwd=self.repo_dir)
            if code == 0:
                self._cache[cache_key] = out
                return out

        # 2. Remote GitHub fetch with session & retries
        session = self._get_session()
        content = None
        if self.token:
            url = f"https://api.github.com/repos/{self.repo}/contents/{file_path}?ref={ref}"
            headers = {
                "Authorization": f"token {self.token}",
                "Accept": "application/vnd.github.v3.raw"
            }
            try:
                resp = session.get(url, headers=headers, timeout=30)
                if resp.status_code == 200:
                    content = resp.text
            except Exception as e:
                logger.warning(f"Error fetching {file_path} at {ref} via API: {e}")

        if content is None:
            # Fallback to public raw content
            url = f"https://raw.githubusercontent.com/{self.repo}/{ref}/{file_path}"
            try:
                resp = session.get(url, timeout=30)
                if resp.status_code == 200:
                    content = resp.text
            except Exception as e:
                logger.warning(f"Error fetching {file_path} at {ref} via raw URL: {e}")

        self._cache[cache_key] = content
        return content

    def list_tree_at_ref(self, ref: str, path_prefix: str = "src") -> List[str]:
        """List all file paths under path_prefix at a given ref/tag."""
        cache_key = (ref, path_prefix)
        if cache_key in self._tree_cache:
            return self._tree_cache[cache_key]

        # 1. Try local git repository if available
        if self._local_repo_has_ref(ref):
            code, out, _ = self.run_git(["ls-tree", "-r", "--name-only", ref, path_prefix], cwd=self.repo_dir)
            if code == 0 and out:
                files = [line.strip().replace("\\", "/") for line in out.splitlines() if line.strip()]
                self._tree_cache[cache_key] = files
                return files

        # 2. Remote GitHub REST API with session & retries
        if not self.token:
            logger.warning("GITHUB_TOKEN missing and ref not in local git; cannot inspect remote git tree via API.")
            return []

        session = self._get_session()
        url = f"https://api.github.com/repos/{self.repo}/git/trees/{ref}?recursive=1"
        headers = {
            "Authorization": f"token {self.token}",
            "Accept": "application/vnd.github.v3+json"
        }
        try:
            resp = session.get(url, headers=headers, timeout=30)
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
            self._tree_cache[cache_key] = files
            return files
        except Exception as e:
            logger.error(f"Network error fetching tree for ref {ref}: {e}")
            return []

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
        self.dictionary: Dict[str, str] = {}
        
        # Load external translation mapping if available
        dict_candidates = [
            Path(__file__).resolve().parent / "full_saff_translations.json",
            Path(__file__).resolve().parent.parent / "full_saff_translations.json",
            Path("full_saff_translations.json")
        ]
        for candidate in dict_candidates:
            if candidate.exists():
                try:
                    self.dictionary = json.loads(candidate.read_text(encoding="utf-8"))
                    logger.debug(f"Loaded {len(self.dictionary)} translations from {candidate}")
                    break
                except Exception as e:
                    logger.warning(f"Failed to load translation dictionary from {candidate}: {e}")

    def clean_garbled(self, text: str) -> str:
        """Clean garbled fragments caused by previous partial replacements."""
        t = text
        subs = [
            ("notme", "nome"), ("notvo", "novo"), ("notva", "nova"), ("notvamente", "novamente"),
            ("diagnotstico", "diagnostico"), ("Diagnotstic", "Diagnostic"), ("not ", "no "),
            ("Not ", "No "), ("menots", "menos"), ("Espanotl", "Espanhol"), ("Españotl", "Español"),
            ("Nao ", "Não "), ("nao ", "não "), ("ja ", "já "), ("usuario", "usuário"),
            ("Usuario", "Usuário"), ("extracao", "extração"), ("extracoes", "extrações"),
            ("Extracao", "Extração"), ("Extracoes", "Extrações"), ("secao", "seção"),
            ("secoes", "seções"), ("Secao", "Seção"), ("Secoes", "Seções"),
            ("criacao", "criação"), ("Criacao", "Criação"), ("edicao", "edição"),
            ("Edicao", "Edição"), ("delecao", "deleção"), ("Delecao", "Deleção")
        ]
        for pt, en in subs:
            t = t.replace(pt, en)
        return t

    def translate_text(self, text: str) -> str:
        """Translate a single natural language string chunk."""
        cleaned = text.strip()
        if not cleaned:
            return text

        # 1. Exact match in dictionary
        if cleaned in self.dictionary:
            return self.dictionary[cleaned]

        cleaned_fixed = self.clean_garbled(cleaned)
        if cleaned_fixed in self.dictionary:
            return self.dictionary[cleaned_fixed]

        # 2. Heuristic word/phrase substitutions
        result = cleaned_fixed
        word_subs = [
            ("exibe ", "displays "), ("Exibe ", "Displays "), ("apresenta ", "displays "),
            ("Apresenta ", "Displays "), ("solicitando confirmação", "requesting confirmation"),
            ("retorna à ", "returns to "), ("retorna a ", "returns to "), ("preenche ", "fills "),
            ("Preenche ", "Fills "), ("clica ", "clicks "), ("Clica ", "Clicks "),
            ("seleciona ", "selects "), ("Seleciona ", "Selects "), ("cancela ", "cancels "),
            ("Cancela ", "Cancels "), ("edita ", "edits "), ("Edita ", "Edits "),
            ("deleta ", "deletes "), ("Deleta ", "Deletes "), ("cria ", "creates "),
            ("Cria ", "Creates "), ("salva ", "saves "), ("Salva ", "Saves "),
            ("mensagem ", "message "), ("Mensagem ", "Message "), ("botão ", "button "),
            ("botões ", "buttons "), ("usuário", "user"), ("usuários", "users"),
            ("Usuário", "User"), ("Usuários", "Users"), ("cliente", "customer"),
            ("clientes", "customers"), ("Cliente", "Customer"), ("Clientes", "Customers"),
            ("documento", "document"), ("documentos", "documents"), ("Documento", "Document"),
            ("Documentos", "Documents"), ("laboratório", "laboratory"), ("laboratórios", "laboratories"),
            ("Laboratório", "Laboratory"), ("Laboratórios", "Laboratories"), ("diagnóstico", "diagnostic"),
            ("diagnósticos", "diagnostics"), ("Diagnóstico", "Diagnostic"), ("Diagnósticos", "Diagnostics"),
            ("seção", "section"), ("seções", "sections"), ("Seção", "Section"),
            ("Seções", "Sections"), ("extração", "extraction"), ("extrações", "extractions"),
            ("Extração", "Extraction"), ("Extrações", "Extractions"), ("servidor", "server"),
            ("Servidor", "Server"), ("extrator", "extractor"), ("Extrator", "Extractor"),
            ("relatório", "report"), ("relatórios", "reports"), ("Relatório", "Report"),
            ("Relatórios", "Reports"), ("gráfico", "graph"), ("gráficos", "graphs"),
            ("Gráfico", "Graph"), ("Gráficos", "Graphs"), ("estatística", "statistic"),
            ("estatísticas", "statistics"), ("Estatística", "Statistic"), ("Estatísticas", "Statistics"),
            ("filtro", "filter"), ("filtros", "filters"), ("Filtro", "Filter"),
            ("Filtros", "Filters"), ("configuração", "setting"), ("configurações", "settings"),
            ("Configuração", "Setting"), ("Configurações", "Settings"), ("não existe ", "there is no "),
            ("Não existe ", "There is no "), ("inválido", "invalid"), ("inválidos", "invalid"),
            ("inválida", "invalid"), ("inválidas", "invalid"), ("válido", "valid"),
            ("válidos", "valid"), ("válida", "valid"), ("válidas", "valid"),
            (" e ", " and "), (" ou ", " or "), (" com ", " with "), (" para ", " for "),
            (" de ", " of "), (" da ", " of the "), (" do ", " of the "), (" das ", " of the "),
            (" dos ", " of the "), (" no ", " in the "), (" na ", " in the "),
            (" nos ", " in the "), (" nas ", " in the "), (" por ", " by "),
            (" pelo ", " by the "), (" pela ", " by the "), (" após ", " after "),
            (" antes ", " before "), (" sem ", " without "), (" todos ", " all "),
            (" todas ", " all "), (" tela ", " page "), (" página ", " page "),
            (" campo ", " field "), (" campos ", " fields "), (" pasta ", " folder "),
            (" pastas ", " folders "), (" arquivo ", " file "), (" arquivos ", " files "),
            (" lista ", " list "), (" dados ", " data "), (" erro ", " error "),
            (" sucesso ", " success "), (" realizado ", " completed "), (" realizada ", " completed "),
            (" realizadas ", " completed "), (" realizados ", " completed ")
        ]
        for pt, en in word_subs:
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

def get_clause_type(line: str) -> str:
    """Classifies a .claret line by its primary semantic clause keyword."""
    l = line.strip().lower()
    for kw in ["actor ", "precondition ", "postcondition ", "step ", "alternative ", "exception ", "version ", "usecase ", "system ", "basicflow"]:
        if l.startswith(kw):
            return kw.strip()
    return "other"

def split_heterogeneous_chunks(src_lines: List[str], tgt_lines: List[str]) -> List[Tuple[List[str], List[str]]]:
    """
    Partitions a replace hunk containing multiple distinct DSL clause types
    (e.g., actor series followed by preCondition) into separate clause-specific chunks.
    """
    src_types = [get_clause_type(l) for l in src_lines]
    tgt_types = [get_clause_type(l) for l in tgt_lines]

    common_types = set(src_types).intersection(set(tgt_types)) - {"other"}
    if len(common_types) > 1:
        ordered_common = []
        for t in src_types:
            if t in common_types and (not ordered_common or ordered_common[-1] != t):
                ordered_common.append(t)

        tgt_order = []
        for t in tgt_types:
            if t in common_types and (not tgt_order or tgt_order[-1] != t):
                tgt_order.append(t)

        if ordered_common == tgt_order and len(ordered_common) > 1:
            sub_chunks = []
            s_idx = 0
            t_idx = 0
            for ct in ordered_common:
                s_part = []
                while s_idx < len(src_lines) and src_types[s_idx] == ct:
                    s_part.append(src_lines[s_idx])
                    s_idx += 1
                t_part = []
                while t_idx < len(tgt_lines) and tgt_types[t_idx] == ct:
                    t_part.append(tgt_lines[t_idx])
                    t_idx += 1
                if s_part or t_part:
                    sub_chunks.append((s_part, t_part))
            if s_idx < len(src_lines) or t_idx < len(tgt_lines):
                sub_chunks.append((src_lines[s_idx:], tgt_lines[t_idx:]))
            return sub_chunks

    return [(src_lines, tgt_lines)]

def extract_granular_diffs(
    raw_src: Optional[str],
    raw_tgt: Optional[str],
    origin_version: str,
    target_version: str,
    file_name: str,
    system_name: str,
    is_dsl: bool = True
) -> List[Dict[str, Any]]:
    """
    Extracts fine-grained diff chunks between two versions of a specification or test case file.
    Instead of serializing the entire file, each added, deleted, or modified block forms a record.
    """
    import difflib
    norm_src = normalize_content(raw_src)
    norm_tgt = normalize_content(raw_tgt)

    if norm_src == norm_tgt:
        return []

    records = []

    # Case 1: Added file
    if not norm_src and norm_tgt:
        records.append({
            "file": file_name,
            "system": system_name,
            "origin_version": origin_version,
            "origin_content": "",
            "target_version": target_version,
            "target_content": norm_tgt
        })
        return records

    # Case 2: Deleted file
    if norm_src and not norm_tgt:
        records.append({
            "file": file_name,
            "system": system_name,
            "origin_version": origin_version,
            "origin_content": norm_src,
            "target_version": target_version,
            "target_content": ""
        })
        return records

    src_lines = norm_src.splitlines()
    tgt_lines = norm_tgt.splitlines()

    sm = difflib.SequenceMatcher(None, src_lines, tgt_lines)

    for tag, i1, i2, j1, j2 in sm.get_opcodes():
        if tag == "equal":
            continue

        s_chunk = src_lines[i1:i2]
        t_chunk = tgt_lines[j1:j2]

        if tag == "replace" and is_dsl:
            sub_chunks = split_heterogeneous_chunks(s_chunk, t_chunk)
            for s_sub, t_sub in sub_chunks:
                records.append({
                    "file": file_name,
                    "system": system_name,
                    "origin_version": origin_version,
                    "origin_content": "\n".join(s_sub) if s_sub else "",
                    "target_version": target_version,
                    "target_content": "\n".join(t_sub) if t_sub else ""
                })
        else:
            records.append({
                "file": file_name,
                "system": system_name,
                "origin_version": origin_version,
                "origin_content": "\n".join(s_chunk) if s_chunk else "",
                "target_version": target_version,
                "target_content": "\n".join(t_chunk) if t_chunk else ""
            })

    return records

def generate_diff_csv(
    diff_records: List[Dict[str, Any]],
    output_csv_path: Path
) -> Path:
    """
    Writes diff records to CSV matching the format:
    | # | file | system | origin_version | origin_content | target_version | target_content |
    """
    output_csv_path.parent.mkdir(parents=True, exist_ok=True)
    fieldnames = ["#", "file", "system", "origin_version", "origin_content", "target_version", "target_content"]

    with open(output_csv_path, mode="w", newline="", encoding="utf-8") as f:
        writer = csv.DictWriter(f, fieldnames=fieldnames)
        writer.writeheader()
        for idx, rec in enumerate(diff_records, start=1):
            writer.writerow({
                "#": idx,
                "file": rec.get("file", ""),
                "system": rec.get("system", ""),
                "origin_version": rec.get("origin_version") or rec.get("source_version", ""),
                "origin_content": rec.get("origin_content") or rec.get("source_content", ""),
                "target_version": rec.get("target_version", ""),
                "target_content": rec.get("target_content", "")
            })
    logger.info(f"Diff CSV generated at: {output_csv_path} with {len(diff_records)} records.")
    return output_csv_path
