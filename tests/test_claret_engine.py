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

if __name__ == "__main__":
    unittest.main()
