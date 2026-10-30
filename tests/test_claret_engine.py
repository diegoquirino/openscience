#!/usr/bin/env python3
"""
test_claret_engine.py
=====================
Unit tests for the claret-version-control-system core engine and skills suite.
"""

import sys
import csv
import tempfile
import unittest
from pathlib import Path

# Add scripts and project root to sys.path for robust resolution
PROJECT_ROOT = Path(__file__).resolve().parent.parent
SCRIPTS_DIR = PROJECT_ROOT / "scripts"
for p in [str(SCRIPTS_DIR), str(PROJECT_ROOT)]:
    if p not in sys.path:
        sys.path.insert(0, p)

try:
    from scripts.claret_engine import (
        to_pascal_case_with_acronyms,
        format_version,
        format_branch_title,
        normalize_content,
        extract_system_name,
        extract_granular_diffs,
        generate_diff_csv
    )
except ImportError:
    from claret_engine import (  # type: ignore
        to_pascal_case_with_acronyms,
        format_version,
        format_branch_title,
        normalize_content,
        extract_system_name,
        extract_granular_diffs,
        generate_diff_csv
    )

class TestClaretEngine(unittest.TestCase):

    def test_pascal_case_with_acronyms(self):
        cases = {
            "CRUD_Cliente.claret": "CRUDCliente.claret",
            "CRUD_Diagnosticos.claret": "CRUDDiagnosticos.claret",
            "ExtracaoLLT.claret": "ExtracaoLLT.claret",
            "login-minitest-alternative-format-dsl.claret": "LoginMinitestAlternativeFormatDSL.claret",
            "CombinandoExtracoesDiferentesPerfis.dsl.claret": "CombinandoExtracoesDiferentesPerfis.dsl.claret",
            "visualizar_e_editar_conta.claret": "VisualizarEEditarConta.claret"
        }
        for original, expected in cases.items():
            result = to_pascal_case_with_acronyms(original)
            self.assertEqual(result, expected, f"Failed for {original}")

    def test_format_version(self):
        self.assertEqual(format_version(1), "1.0")
        self.assertEqual(format_version(2), "2.0")
        self.assertEqual(format_version("1"), "1.0")
        self.assertEqual(format_version("1.1"), "1.1")
        self.assertEqual(format_version("v2.5"), "2.5")

    def test_format_branch_title(self):
        self.assertEqual(format_branch_title("saff-study"), "SAFF Study")
        self.assertEqual(format_branch_title("abc_da_net"), "Abc Da Net")
        self.assertEqual(format_branch_title("claret-version-control-system"), "CLARET Version Control System")

    def test_normalize_content(self):
        raw = "  System   \"SAFF\",  {\n\n\n    usecase \"Customer Management\" \n\n}  "
        normalized = normalize_content(raw)
        self.assertEqual(
            normalized,
            'system "saff", {\nusecase "customer management"\n}'
        )

    def test_extract_system_name(self):
        content = 'system "SAFF", {\n    usecase "Login" {\n    }\n}'
        self.assertEqual(extract_system_name(content), "SAFF")

    def test_extract_granular_diffs_dsl(self):
        src = (
            'system "saff", {\n'
            '  actor admin, "Administrador"\n'
            '  actor op, "Operador"\n'
            '  preCondition "Estar logado"\n'
            '  basicFlow {\n'
            '    step 1, admin, "Clica no botao"\n'
            '    step 2, system, "Exibe pagina"\n'
            '  }\n'
            '}'
        )
        tgt = (
            'system "saff", {\n'
            '  actor admin, "Administrador do sistema"\n'
            '  preCondition "Estar autenticado no extrator"\n'
            '  basicFlow {\n'
            '    step 1, admin, "Pressiona o botao"\n'
            '    step 2, system, "Exibe pagina"\n'
            '  }\n'
            '}'
        )
        records = extract_granular_diffs(
            raw_src=src,
            raw_tgt=tgt,
            origin_version="v1.0",
            target_version="v1.1",
            file_name="Sample.claret",
            system_name="SAFF",
            is_dsl=True
        )
        # Should decompose into 3 separate changes: actors, precondition, step 1
        self.assertEqual(len(records), 3)
        self.assertEqual(records[0]["origin_version"], "v1.0")
        self.assertEqual(records[0]["target_version"], "v1.1")
        self.assertIn("actor admin", records[0]["origin_content"])
        self.assertIn("actor op", records[0]["origin_content"])
        self.assertEqual(records[0]["target_content"], 'actor admin, "administrador do sistema"')

        self.assertIn("precondition", records[1]["origin_content"])
        self.assertIn("precondition", records[1]["target_content"])

        self.assertIn("step 1", records[2]["origin_content"])
        self.assertIn("step 1", records[2]["target_content"])

    def test_extract_granular_diffs_identical(self):
        content = 'system "SAFF", {\n  step 1, admin, "Login"\n}'
        records = extract_granular_diffs(
            raw_src=content,
            raw_tgt=content,
            origin_version="v1.0",
            target_version="v1.1",
            file_name="Sample.claret",
            system_name="SAFF"
        )
        self.assertEqual(records, [])

    def test_generate_diff_csv(self):
        recs = [{
            "file": "Test.claret",
            "system": "SAFF",
            "origin_version": "v1.0",
            "origin_content": 'actor a, "Admin"',
            "target_version": "v1.1",
            "target_content": 'actor a, "Super"'
        }]
        with tempfile.NamedTemporaryFile(suffix=".csv", delete=False) as tf:
            tf_path = Path(tf.name)
        try:
            generate_diff_csv(recs, tf_path)
            with open(tf_path, encoding="utf-8") as f:
                reader = csv.DictReader(f)
                headers = reader.fieldnames
                self.assertEqual(headers, ["#", "file", "system", "origin_version", "origin_content", "target_version", "target_content"])
                rows = list(reader)
                self.assertEqual(len(rows), 1)
                self.assertEqual(rows[0]["origin_version"], "v1.0")
                self.assertEqual(rows[0]["origin_content"], 'actor a, "Admin"')
        finally:
            if tf_path.exists():
                tf_path.unlink()

try:
    from scripts import claret_engine as ce
except ImportError:
    import claret_engine as ce  # type: ignore

# The two equivalent syntaxes of claret-generator (samples/login-minitest*.claret).
SPACE_SYNTAX = '''systemName "Email"
usecase "Login User" {
  version "1.0" type "Creation" user "Dalton" date "01/01/2018"
  actor emailUser "Email User"
  preCondition "There is an active network connection."
  basic {
    step 1 emailUser "launches the login screen"
    step 2 system "presents a form containing a username text field, a masked password field, and a submit button"
    step 3 emailUser "fills out the fields and click on the submit button" af[1]
    step 4 system "displays a successful message" ef[1,2]
  }
  alternative 1 "Username is predicted" {
    step 1 emailUser "selects a suggested user name, types password and click on the submit button" bs 4
  }
  exception 1 "User does not exist in database" {
    step 1 system "alerts that user does not exist"
  }
  exception 2 "Incorrect username/password combination" {
    step 1 system "alerts that username and/or password are incorrect"
  }
  postCondition "User successfully logged"
}
'''

COMMA_SYNTAX = '''system "Email", {
    usecase "Login User Alternative Format", {
        version "1.0", type:"Creation", user:"Dalton", date:"01/01/2018"
        actor emailUser, "Email User"
        preCondition "There is an active network connection."
        basicFlow {
            step 1, emailUser, "launches the login screen"
            step 2, system, "presents a form containing a username text field, a masked password field, and a submit button"
            step 3, emailUser, "fills out the fields and click on the submit button", af:[1]
            step 4, system, "displays a successful message", ef:[1,2]
        }
        alternative 1, "Username is predicted", {
            step 1, emailUser, "selects a suggested user name, types password and click on the submit button", bfs:4
        }
        exception 1, "User does not exist in database", {
            step 1, system, "alerts that user does not exist"
        }
        exception 2, "Incorrect username/password combination", {
            step 1, system, "alerts that username and/or password are incorrect"
        }
        postCondition "User successfully logged"
    }
}
'''


class _StubGitHub:
    """Serves the .claret of both versions and no test suite."""

    def __init__(self, src: str, tgt: str):
        self.files = {"v1.0": src, "v1.1": tgt}

    def fetch_file_content_at_ref(self, ref, file_path):
        return self.files.get(ref) if file_path.startswith("src/") else ""


def _cia(orig: str, tgt: str, src_file: str = "", tgt_file: str = "") -> dict:
    row = {"file": "LoginUser.claret", "origin_version": "v1.0", "target_version": "v1.1",
           "origin_content": ce.normalize_content(orig), "target_content": ce.normalize_content(tgt)}
    return ce.analyze_diff_impact(row, _StubGitHub(src_file, tgt_file or src_file))


class TestClaretDualSyntax(unittest.TestCase):

    def test_system_name_is_never_a_step_text(self):
        self.assertEqual(ce.extract_system_name(SPACE_SYNTAX), "Email")
        self.assertEqual(ce.extract_system_name(COMMA_SYNTAX), "Email")
        self.assertEqual(ce.extract_system_name(ce.normalize_content(SPACE_SYNTAX)), "email")

    def test_clause_types(self):
        cases = {
            'systemName "Email"': "systemname", 'system "Email", {': "system",
            "basic {": "basic", "basicFlow {": "basicflow", 'requirements "REQ-01"': "requirements",
            'step 3 emailUser "x" af[1]': "step", 'step 3, emailUser, "x", af:[1]': "step",
            'alternative 1 "A" {': "alternative", 'alternative 1, "A", {': "alternative",
        }
        for line, expected in cases.items():
            self.assertEqual(ce.get_clause_type(line), expected, line)

    def test_enclosing_flow_in_both_syntaxes(self):
        for content in (SPACE_SYNTAX, COMMA_SYNTAX):
            self.assertEqual(ce.find_enclosing_flow_in_claret(content, "launches the login screen")[:2], ("basic", 0))
            self.assertEqual(ce.find_enclosing_flow_in_claret(content, "selects a suggested user name")[:2],
                             ("alternative", 1))
            self.assertEqual(ce.find_enclosing_flow_in_claret(content, "alerts that user does not exist")[:2],
                             ("exception", 1))

    def test_branch_annotations_are_syntax_independent(self):
        space = ce.branch_annotations('step 4 system "displays" ef[1,2]\nstep 1 u "selects" bs 4')
        comma = ce.branch_annotations('step 4, system, "displays", ef:[2, 1]\nstep 1, u, "selects", bfs:4')
        self.assertEqual(space, comma)
        self.assertEqual(space, {"af": [], "ef": [(1, 2)], "bs": [(4,)]})
        # annotations inside quoted text are ignored
        self.assertEqual(ce.branch_annotations('step 1 u "runs jobs 3 times and bs 2"')["bs"], [])

    def test_step_actor(self):
        self.assertEqual(ce.step_actor('step 3 emailUser "x" af[1]'), "emailuser")
        self.assertEqual(ce.step_actor('step 3, emailUser, "x", af:[1]'), "emailuser")

    def test_cia_reassign_on_return_step_change(self):
        for o, t in (('step 1 u "selects" bs 4', 'step 1 u "selects" bs 3'),
                     ('step 1, u, "selects", bfs:4', 'step 1, u, "selects", bfs:3')):
            r = _cia(o, t, SPACE_SYNTAX)
            self.assertEqual(r["model_element"], "BRANCHING_CONDITION")
            self.assertEqual(r["tcm_operation"], "Reassign")

    def test_cia_branch_change_on_deviation_set(self):
        r = _cia('step 4 system "displays" ef[1,2]', 'step 4 system "displays" ef[1,2,3]', SPACE_SYNTAX)
        self.assertEqual(r["model_element"], "BRANCHING_CONDITION")

    def test_cia_syntax_only_change_is_keep(self):
        r = _cia('step 3 emailUser "fills out" af[1]', 'step 3, emailUser, "fills out", af:[1]', SPACE_SYNTAX)
        self.assertNotEqual(r["model_element"], "BRANCHING_CONDITION")
        self.assertEqual((r["actual_change_impact"], r["tcm_operation"]), ("Low", "Keep"))

    def test_cia_actor_change_in_both_syntaxes(self):
        for o, t in (('step 1 emailUser "launches"', 'step 1 admin "launches"'),
                     ('step 1, emailUser, "launches"', 'step 1, admin, "launches"')):
            self.assertEqual(_cia(o, t, SPACE_SYNTAX)["model_element"], "STEP_ACTOR")

    def test_cia_metadata_in_both_syntaxes(self):
        for o, t in (('version "1.0" type "Creation"', 'version "1.1" type "Modification"'),
                     ('version "1.0", type:"Creation"', 'version "1.1", type:"Modification"')):
            self.assertEqual(_cia(o, t, SPACE_SYNTAX)["model_element"], "MODEL_METADATA")

    def test_cia_classifies_both_samples_alike(self):
        def edit(content):
            return content.replace("launches the login screen", "opens the login screen")
        a = _cia('step 1 emailuser "launches the login screen"', 'step 1 emailuser "opens the login screen"',
                 SPACE_SYNTAX, edit(SPACE_SYNTAX))
        b = _cia('step 1, emailuser, "launches the login screen"', 'step 1, emailuser, "opens the login screen"',
                 COMMA_SYNTAX, edit(COMMA_SYNTAX))
        for key in ("model_element", "actual_change_impact", "tcm_operation", "primitive_operation"):
            self.assertEqual(a[key], b[key], key)
        self.assertEqual(a["model_element"], "BASIC_FLOW_STEP")


if __name__ == "__main__":
    unittest.main()
