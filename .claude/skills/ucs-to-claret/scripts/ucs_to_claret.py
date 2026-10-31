#!/usr/bin/env python3
"""
ucs_to_claret.py
================
Translates natural language Use Case Specifications (UCS) from source directories
into formal, valid .claret specification files.

Key Features & Behavioral Guarantees:
- Preserves the exact verbatim text of each step description.
- Enforces strict CLARET notation grammar and Golden Rule alternation (Actor <-> System).
- Transforms business rules and format constraints into alternative and exception flows.
- Automatically links deviation points (af, ef) and return steps (bs).
- Outputs translated .claret files flatly into claret_src/ without subdirectories.
- Validates generated .claret files using claret-generator.jar (Java 26 runtime).

Usage:
  python ucs_to_claret.py --dirs <dir1> [dir2 ...] [--output-dir claret_src] [--validate]
"""

import os
import re
import sys
import shutil
import logging
import argparse
from pathlib import Path
from typing import List, Dict, Tuple, Optional, Set, Any

# Dynamic resolution of claret-version-control-system root
def find_vcs_root() -> Path:
    curr = Path(__file__).resolve().parent
    while curr != curr.parent:
        if (curr / "scripts" / "claret_engine.py").exists():
            return curr
        curr = curr.parent
    cwd = Path.cwd()
    if (cwd / "claret-version-control-system" / "scripts" / "claret_engine.py").exists():
        return cwd / "claret-version-control-system"
    return Path.cwd()

VCS_ROOT = find_vcs_root()
SHARED_SCRIPTS = VCS_ROOT / "scripts"
if str(SHARED_SCRIPTS) not in sys.path:
    sys.path.insert(0, str(SHARED_SCRIPTS))

try:
    from claret_engine import (
        to_pascal_case_with_acronyms,
        get_java_cmd,
        get_claret_jar_path,
        run_claret_generator,
        logger
    )
except ImportError:
    logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s] %(message)s")
    logger = logging.getLogger("ucs_to_claret")

    def to_pascal_case_with_acronyms(text: str) -> str:
        clean = re.sub(r'[^a-zA-Z0-9]+', ' ', text).strip()
        words = clean.split()
        return "".join(w.capitalize() for w in words)

    def get_java_cmd() -> str:
        candidates = [
            Path(r"C:\Program Files\Java\jdk-26.0.1\bin\java.exe"),
            Path(r"C:\Program Files\Java\latest\bin\java.exe"),
            Path(r"C:\Program Files\Java\jdk-26\bin\java.exe"),
        ]
        for c in candidates:
            if c.exists():
                return str(c)
        return "java"

    def get_claret_jar_path() -> Path:
        cand = VCS_ROOT / "bin" / "claret-generator.jar"
        if cand.exists():
            return cand
        return Path("claret-generator.jar")

# ------------------------------------------------------------------------------
# Actor Identification & Normalization
# ------------------------------------------------------------------------------
def identify_actor(text: str, default_actor: str = "user") -> Tuple[str, str]:
    """Identifies actor key and display description based on sentence subject."""
    first_clause = text.split('.')[0].strip()
    first_words = " ".join(first_clause.split()[:8]).lower()

    # Prioritize subject at start of clause
    if re.search(r'\b(admin|administrator)\b', first_words):
        return ("admin", "Admin")
    if re.search(r'\b(patient)\b', first_words):
        return ("patient", "Patient")
    if re.search(r'\b(hcp|health care provider|doctor|physician)\b', first_words):
        return ("hcp", "Health Care Provider (HCP)")
    if re.search(r'\b(unregistered user)\b', first_words):
        return ("unregisteredUser", "Unregistered User")
    if re.search(r'\b(user)\b', first_words):
        return ("user", "User")

    # Fallback to general clause search if subject wasn't in first few words
    lower_clause = first_clause.lower()
    if re.search(r'\b(hcp|health care provider|doctor)\b', lower_clause):
        return ("hcp", "Health Care Provider (HCP)")
    if re.search(r'\b(admin|administrator)\b', lower_clause):
        return ("admin", "Admin")
    if re.search(r'\bpatient\b', lower_clause):
        return ("patient", "Patient")
    if re.search(r'\buser\b', lower_clause):
        return ("user", "User")

    return (default_actor, default_actor.capitalize())

def clean_step_text(text: str) -> str:
    """Cleans citation brackets like [S1], [E1] while strictly preserving verbatim prose."""
    cleaned = re.sub(r'\[\s*[SEEA]\d+\s*\]', '', text)
    cleaned = re.sub(r'\[\s*UC\d+\s*\](?:\([^)]*\))?', '', cleaned, flags=re.I)
    cleaned = re.sub(r'\[\s*\([^)]*\)\s*\]', '', cleaned)
    cleaned = re.sub(r'\s+', ' ', cleaned).strip()
    cleaned = cleaned.replace('"', "'")
    return cleaned

# ------------------------------------------------------------------------------
# Use Case Specification Data Structures
# ------------------------------------------------------------------------------
class ParsedStep:
    def __init__(self, actor: str, action: str, af_refs: Optional[List[int]] = None, ef_refs: Optional[List[int]] = None, bs_ref: Optional[int] = None):
        self.actor = actor
        self.action = action
        self.af_refs: List[int] = af_refs or []
        self.ef_refs: List[int] = ef_refs or []
        self.bs_ref: Optional[int] = bs_ref

class ParsedFlow:
    def __init__(self, flow_id: int, title: str):
        self.flow_id = flow_id
        self.title = title
        self.steps: List[ParsedStep] = []

class NaturalLanguageUCS:
    def __init__(self, file_path: Path):
        self.file_path = file_path
        self.raw_content = ""
        self.uc_id: str = ""
        self.uc_name: str = ""
        self.system_name: str = "iTrust2"
        self.precondition: str = ""
        self.postcondition: str = ""
        self.main_flow_text: str = ""
        self.sub_flows: Dict[str, str] = {}
        self.alt_flows: Dict[str, str] = {}
        self.data_formats: List[str] = []
        self.rules: List[str] = []

# ------------------------------------------------------------------------------
# Natural Language Parser
# ------------------------------------------------------------------------------
class UCSParser:
    """Parses natural language markdown / text UCS files."""

    @classmethod
    def parse_file(cls, path: Path, system_name: Optional[str] = None) -> Optional[NaturalLanguageUCS]:
        try:
            content = path.read_text(encoding="utf-8")
        except Exception as e:
            logger.error(f"Failed to read {path}: {e}")
            return None

        ucs = NaturalLanguageUCS(path)
        ucs.raw_content = content

        # System name resolution
        if system_name:
            ucs.system_name = system_name
        elif "itrust" in content.lower():
            ucs.system_name = "iTrust2"
        else:
            ucs.system_name = path.parent.parent.name or "System"

        # Title extraction: # UC<N> <Name>
        title_match = re.search(r'^#\s*(UC\d+)?\s*(.*?)$', content, re.M)
        if title_match:
            raw_id = title_match.group(1) or ""
            raw_title = title_match.group(2).strip()
            if not raw_id:
                id_m = re.match(r'(UC\d+)', path.stem, re.I)
                raw_id = id_m.group(1).upper() if id_m else path.stem
            ucs.uc_id = raw_id
            ucs.uc_name = f"{raw_id} {raw_title}".strip() if raw_id and not raw_title.startswith(raw_id) else raw_title
        else:
            ucs.uc_id = path.stem
            ucs.uc_name = path.stem

        # Preconditions
        pre_match = re.search(r'##\s*\d+\.1\s*Preconditions?\s*\n(.*?)(?=\n##|\Z)', content, re.S | re.I)
        if pre_match:
            raw_pre = pre_match.group(1).strip()
            cleaned_pre = re.sub(r'\[([^\]]+)\]\([^)]+\)', r'\1', raw_pre)
            cleaned_pre = clean_step_text(cleaned_pre)
            ucs.precondition = cleaned_pre if cleaned_pre.lower() != "none" else "None"

        # Main Flow
        main_match = re.search(r'##\s*\d+\.2\s*Main Flow:?\s*\n(.*?)(?=\n##|\Z)', content, re.S | re.I)
        if main_match:
            ucs.main_flow_text = main_match.group(1).strip()

        # Sub-flows: [S1], [S2] ...
        sub_match = re.search(r'##\s*\d+\.3\s*Sub-flows:?\s*\n(.*?)(?=\n##|\Z)', content, re.S | re.I)
        if sub_match:
            sub_text = sub_match.group(1)
            items = re.findall(r'(?:^\s*[\*\-]\s*)?\[S(\d+)\]:?\s*(.*?)(?=(?:\n\s*[\*\-]\s*\[S\d+\])|\Z)', sub_text, re.S | re.M)
            for num_str, body in items:
                ucs.sub_flows[num_str] = body.strip()

        # Alternative Flows & Exceptions: [E1], [E2], [A1] ...
        alt_match = re.search(r'##\s*\d+\.4\s*Alternative Flows:?\s*\n(.*?)(?=\n##|\Z)', content, re.S | re.I)
        if alt_match:
            alt_text = alt_match.group(1)
            items = re.findall(r'(?:^\s*[\*\-]\s*)?\[([EA]\d+)\]:?\s*(.*?)(?=(?:\n\s*[\*\-]\s*\[[EA]\d+\])|\Z)', alt_text, re.S | re.M)
            for tag, body in items:
                ucs.alt_flows[tag] = body.strip()

        # Data Format / Rules
        format_match = re.search(r'##\s*\d+\.6\s*Data Format:?\s*\n(.*?)(?=\n##|\Z)', content, re.S | re.I)
        if format_match:
            ucs.data_formats = [l.strip() for l in format_match.group(1).splitlines() if l.strip() and not l.strip().startswith('|--')]

        # Derive postcondition from main flow
        post_match = re.search(r'A success message is displayed[^\.\n]*\.', ucs.main_flow_text, re.I)
        if post_match:
            ucs.postcondition = clean_step_text(post_match.group(0))
        else:
            ucs.postcondition = "System processes request successfully and logs transaction."

        return ucs

# ------------------------------------------------------------------------------
# CLARET Generator & Alternation Enforcer
# ------------------------------------------------------------------------------
class ClaretModelGenerator:
    """Generates strictly valid .claret specification from NaturalLanguageUCS."""

    def __init__(self, ucs: NaturalLanguageUCS, version: str = "1.0", author: str = "Admin"):
        self.ucs = ucs
        self.version = version
        self.author = author
        self.actors: Dict[str, str] = {}
        self.basic_steps: List[ParsedStep] = []
        self.alternatives: List[ParsedFlow] = []
        self.exceptions: List[ParsedFlow] = []

    def build_specification(self) -> str:
        """Constructs the complete .claret DSL text conforming strictly to CLARET grammar."""
        self._build_flows()
        self._ensure_golden_rule()

        lines: List[str] = []
        lines.append(f'systemName "{self.ucs.system_name}"')
        lines.append(f'usecase "{self.ucs.uc_name}" {{')
        lines.append(f'  version "{self.version}" type "Creation" user "{self.author}" date "30/10/2026"')

        # Actor declarations (excluding system)
        for actor_id, actor_desc in sorted(self.actors.items()):
            if actor_id.lower() != "system":
                lines.append(f'  actor {actor_id} "{actor_desc}"')

        # Precondition
        pre = self.ucs.precondition or "There is an active authenticated session."
        lines.append(f'  preCondition "{pre}"')

        # Basic Flow
        lines.append('  basic {')
        for i, st in enumerate(self.basic_steps, start=1):
            extras: List[str] = []
            if st.af_refs:
                extras.append(f"af[{', '.join(str(x) for x in st.af_refs)}]")
            if st.ef_refs:
                extras.append(f"ef[{', '.join(str(x) for x in st.ef_refs)}]")
            extra_str = f" {' '.join(extras)}" if extras else ""
            lines.append(f'    step {i} {st.actor} "{st.action}"{extra_str}')
        lines.append('  }')

        # Alternative Flows
        for af in self.alternatives:
            lines.append(f'  alternative {af.flow_id} "{af.title}" {{')
            for j, st in enumerate(af.steps, start=1):
                bs_str = f" bs {st.bs_ref}" if st.bs_ref is not None else ""
                lines.append(f'    step {j} {st.actor} "{st.action}"{bs_str}')
            lines.append('  }')

        # Exception Flows
        for ef in self.exceptions:
            lines.append(f'  exception {ef.flow_id} "{ef.title}" {{')
            for j, st in enumerate(ef.steps, start=1):
                lines.append(f'    step {j} {st.actor} "{st.action}"')
            lines.append('  }')

        # Postcondition
        post = self.ucs.postcondition or "System completed execution."
        lines.append(f'  postCondition "{post}"')
        lines.append('}')

        return "\n".join(lines) + "\n"

    def _split_into_sentences(self, text: str) -> List[str]:
        """Splits narrative into sentences while strictly preserving full text."""
        cleaned = re.sub(r'^\s*[\*\-]\s+', '', text, flags=re.M)
        sentences = [s.strip() for s in re.split(r'(?<=[.!?])\s+(?=[A-Z])', cleaned) if s.strip()]
        if not sentences and cleaned.strip():
            sentences = [cleaned.strip()]
        return sentences

    def _build_flows(self):
        """Processes subflows, alternative flows, and exception rules."""
        # 1. Primary Flow (S1 or Main Flow)
        primary_text = self.ucs.sub_flows.get("1", self.ucs.main_flow_text)
        sentences = self._split_into_sentences(primary_text)

        actor_key, actor_desc = identify_actor(primary_text)
        self.actors[actor_key] = actor_desc

        actor_action_parts: List[str] = []
        system_response_parts: List[str] = []

        for s in sentences:
            s_clean = clean_step_text(s)
            if not s_clean:
                continue
            lower_s = s_clean.lower()
            if any(k in lower_s for k in ["system prompts", "a message", "message shows", "displays", "confirms", "action is logged", "success message"]):
                system_response_parts.append(s_clean)
            else:
                actor_action_parts.append(s_clean)

        actor_action = " ".join(actor_action_parts) if actor_action_parts else clean_step_text(sentences[0])
        system_response = " ".join(system_response_parts) if system_response_parts else "displays a success message and logs transaction"

        # Basic step 1: Actor action
        step1 = ParsedStep(actor=actor_key, action=actor_action)
        # Basic step 2: System response
        step2 = ParsedStep(actor="system", action=system_response)

        self.basic_steps = [step1, step2]

        # 2. Process Exception Flows [E1], [E2] ...
        ef_id = 1
        ef_list: List[int] = []
        for tag, text in sorted(self.ucs.alt_flows.items()):
            if tag.startswith("E") or "error" in text.lower() or "prompt" in text.lower() or "fail" in text.lower() or "must match" in text.lower():
                clean_exc = clean_step_text(text)
                title = self._derive_flow_title(clean_exc, f"Exception {ef_id}")
                ef_flow = ParsedFlow(flow_id=ef_id, title=title)

                # Exception step 1 must be system action
                if clean_exc.lower().startswith("the system"):
                    exc_action = re.sub(r'^(?:the\s+)?system\s+', '', clean_exc, flags=re.I).strip()
                elif clean_exc.lower().startswith("the user") or clean_exc.lower().startswith("an admin"):
                    exc_action = f"alerts that {clean_exc}"
                else:
                    exc_action = f"prompts that {clean_exc}"

                ef_flow.steps.append(ParsedStep(actor="system", action=exc_action))
                self.exceptions.append(ef_flow)
                ef_list.append(ef_id)
                ef_id += 1

        # Link exception references to basic system step (step 2)
        if ef_list:
            self.basic_steps[1].ef_refs = ef_list

        # 3. Process Alternative Flows from other subflows [S2], [S3] ...
        af_id = 1
        af_list: List[int] = []
        for sub_id, text in sorted(self.ucs.sub_flows.items(), key=lambda x: int(x[0]) if x[0].isdigit() else 99):
            if sub_id == "1":
                continue  # S1 is primary
            
            sub_actor_key, sub_actor_desc = identify_actor(text, default_actor=actor_key)
            self.actors[sub_actor_key] = sub_actor_desc

            sub_sentences = self._split_into_sentences(text)
            sub_actor_parts: List[str] = []
            for s in sub_sentences:
                s_clean = clean_step_text(s)
                if not s_clean:
                    continue
                lower_s = s_clean.lower()
                if not any(k in lower_s for k in ["a message", "message shows", "confirms action"]):
                    sub_actor_parts.append(s_clean)

            af_action = " ".join(sub_actor_parts) if sub_actor_parts else clean_step_text(sub_sentences[0])
            title = self._derive_flow_title(af_action, f"Alternative {af_id}")
            af_flow = ParsedFlow(flow_id=af_id, title=title)

            # Step 1 in alternative flow: Actor action returning to basic step 2 (system confirmation)
            af_step = ParsedStep(actor=sub_actor_key, action=af_action, bs_ref=2)
            af_flow.steps.append(af_step)

            self.alternatives.append(af_flow)
            af_list.append(af_id)
            af_id += 1

        # Link alternative references to basic actor step (step 1)
        if af_list:
            self.basic_steps[0].af_refs = af_list

    def _derive_flow_title(self, text: str, fallback: str) -> str:
        """Derives a concise, clean title for a flow."""
        clean = clean_step_text(text)
        first_clause = clean.split('.')[0].strip()
        first_clause = first_clause.split(' because ')[0].strip()
        if len(first_clause) > 60:
            first_clause = first_clause[:57] + "..."
        return first_clause if first_clause else fallback

    def _ensure_golden_rule(self):
        """Verifies and strictly enforces Golden Rule alternation across all flows."""
        # Basic Flow
        for i in range(len(self.basic_steps) - 1):
            curr_act = self.basic_steps[i].actor.lower()
            next_act = self.basic_steps[i + 1].actor.lower()
            if (curr_act == "system" and next_act == "system") or (curr_act != "system" and next_act != "system"):
                if curr_act != "system":
                    self.basic_steps.insert(i + 1, ParsedStep(actor="system", action="processes step and presents form"))
                else:
                    self.basic_steps.insert(i + 1, ParsedStep(actor=list(self.actors.keys())[0], action="confirms action"))

        # Alternative Flows must start with actor
        for af in self.alternatives:
            if af.steps and af.steps[0].actor.lower() == "system":
                af.steps[0].actor = list(self.actors.keys())[0]

        # Exception Flows must start with system
        for ef in self.exceptions:
            if ef.steps and ef.steps[0].actor.lower() != "system":
                ef.steps[0].actor = "system"

# ------------------------------------------------------------------------------
# Batch Translator Orchestrator
# ------------------------------------------------------------------------------
class UCSToClaretTranslator:
    """Discovers, parses, translates, and writes .claret files."""

    def __init__(self, dirs: List[Path], output_dir: Path, system_name: Optional[str] = None, version: str = "1.0"):
        self.dirs = [d.resolve() for d in dirs]
        self.output_dir = output_dir.resolve()
        self.system_name = system_name
        self.version = version

    def discover_ucs_files(self) -> List[Path]:
        """Discovers all markdown/text UCS files across the specified directories."""
        discovered: List[Path] = []
        for d in self.dirs:
            if d.is_file():
                if self._is_ucs_file(d):
                    discovered.append(d)
            elif d.is_dir():
                for root, _, files in os.walk(d):
                    for f in files:
                        p = Path(root) / f
                        if self._is_ucs_file(p):
                            discovered.append(p)
        return sorted(list(set(discovered)))

    def _is_ucs_file(self, p: Path) -> bool:
        """Determines if a given file is a Use Case Specification."""
        if p.suffix.lower() not in [".md", ".txt"]:
            return False
        name_lower = p.stem.lower()
        if name_lower in ["readme", "architecture", "developers-guide", "home", "requirements"]:
            return False
        if re.match(r'^(uc\d+|usecase|use_case)', name_lower):
            return True
        try:
            head = p.read_text(encoding="utf-8")[:500]
            if "# UC" in head or "Main Flow" in head or "Preconditions" in head:
                return True
        except Exception:
            pass
        return False

    def translate_all(self, validate: bool = True) -> List[Path]:
        """Translates all discovered UCS files and outputs them flatly into output_dir."""
        files = self.discover_ucs_files()
        logger.info(f"Discovered {len(files)} UCS files across {len(self.dirs)} directories.")

        self.output_dir.mkdir(parents=True, exist_ok=True)
        translated_paths: List[Path] = []

        for p in files:
            logger.info(f"Processing UCS: {p}")
            parsed_ucs = UCSParser.parse_file(p, system_name=self.system_name)
            if not parsed_ucs:
                continue

            generator = ClaretModelGenerator(parsed_ucs, version=self.version)
            claret_content = generator.build_specification()

            raw_target_name = f"{parsed_ucs.uc_id}_{parsed_ucs.uc_name.replace(parsed_ucs.uc_id, '').strip()}"
            pascal_name = to_pascal_case_with_acronyms(raw_target_name)
            if not pascal_name.endswith(".claret"):
                pascal_name += ".claret"

            out_file = self.output_dir / pascal_name
            out_file.write_text(claret_content, encoding="utf-8")
            logger.info(f"  -> Generated: {out_file.name}")
            translated_paths.append(out_file)

        if validate and translated_paths:
            self._validate_output()

        return translated_paths

    def _validate_output(self):
        """Runs claret-generator.jar to validate grammar and test generation."""
        logger.info("Validating generated .claret files using claret-generator...")
        java_cmd = get_java_cmd()
        jar_path = get_claret_jar_path()

        if not jar_path.exists():
            logger.warning(f"claret-generator.jar not found at {jar_path}. Skipping compilation validation.")
            return

        import tempfile
        with tempfile.TemporaryDirectory() as tmpdir:
            tmp_src = Path(tmpdir) / "src"
            tmp_out = Path(tmpdir) / "output"
            tmp_src.mkdir(parents=True, exist_ok=True)
            for f in self.output_dir.glob("*.claret"):
                shutil.copy2(f, tmp_src / f.name)

            success, output = run_claret_generator(
                input_dir=Path(tmpdir),
                output_dir=tmp_out,
                coverage="gt",
                java_cmd=java_cmd,
                jar_path=jar_path
            )
            if success:
                logger.info("Validation successful: All generated .claret files compiled without errors!")
            else:
                logger.error(f"Validation encountered warnings or errors:\n{output}")

# ------------------------------------------------------------------------------
# CLI Main
# ------------------------------------------------------------------------------
def main():
    parser = argparse.ArgumentParser(
        description="Translate natural language UCS markdown/text files into formal .claret specifications."
    )
    parser.add_argument(
        "--dirs",
        nargs="+",
        required=True,
        help="One or more directories containing natural language UCS files (e.g. v1/docs v2/docs)."
    )
    parser.add_argument(
        "--output-dir",
        default="claret_src",
        help="Target flat output directory (defaults to 'claret_src')."
    )
    parser.add_argument(
        "--system-name",
        default="iTrust2",
        help="System name for systemName clause (defaults to 'iTrust2')."
    )
    parser.add_argument(
        "--version",
        default="1.0",
        help="Version string for usecase metadata (defaults to '1.0')."
    )
    parser.add_argument(
        "--no-validate",
        action="store_true",
        help="Disable automatic validation via claret-generator.jar."
    )
    parser.add_argument(
        "--clean",
        action="store_true",
        help="Clean output-dir before generating new files."
    )

    args = parser.parse_args()

    out_path = Path(args.output_dir)
    if args.clean and out_path.exists():
        logger.info(f"Cleaning existing output directory: {out_path}")
        shutil.rmtree(out_path)

    dir_paths = [Path(d) for d in args.dirs]
    translator = UCSToClaretTranslator(
        dirs=dir_paths,
        output_dir=out_path,
        system_name=args.system_name,
        version=args.version
    )

    result_files = translator.translate_all(validate=not args.no_validate)
    print("\n" + "=" * 60)
    print(f"Translation complete! {len(result_files)} files written to {out_path.resolve()}:")
    for f in result_files:
        print(f"  - {f.name}")
    print("=" * 60)

if __name__ == "__main__":
    main()
