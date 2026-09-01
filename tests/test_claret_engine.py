#!/usr/bin/env python3
"""
test_claret_engine.py
=====================
Unit tests for the claret-version-control-system core engine and skills suite.
"""

import sys
import unittest
from pathlib import Path

# Add scripts to sys.path
PROJECT_ROOT = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(PROJECT_ROOT / "scripts"))

from claret_engine import (
    to_pascal_case_with_acronyms,
    format_version,
    format_branch_title,
    normalize_content,
    extract_system_name
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

if __name__ == "__main__":
    unittest.main()
