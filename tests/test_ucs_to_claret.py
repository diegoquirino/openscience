#!/usr/bin/env python3
"""
test_ucs_to_claret.py
=====================
Unit tests for the ucs-to-claret translation skill and CLARET generator.
"""

import sys
import tempfile
import unittest
from pathlib import Path

# Setup paths
PROJECT_ROOT = Path(__file__).resolve().parent.parent
SKILL_SCRIPTS = PROJECT_ROOT / ".claude" / "skills" / "ucs-to-claret" / "scripts"
SHARED_SCRIPTS = PROJECT_ROOT / "scripts"

for p in [str(SKILL_SCRIPTS), str(SHARED_SCRIPTS), str(PROJECT_ROOT)]:
    if p not in sys.path:
        sys.path.insert(0, p)

from ucs_to_claret import (
    UCSParser,
    ClaretModelGenerator,
    UCSToClaretTranslator,
    identify_actor,
    clean_step_text
)

SAMPLE_UCS_MD = """# UC1 User Functionality

## 1.1 Preconditions
The iTrust2 Admin has authenticated themselves in the iTrust2 Medical Records system [UC3](uc3).

## 1.2 Main Flow
The admin selects the option to create a user [S1] or delete a user [S2].  A success message is displayed and the action is logged.

## 1.3 Sub-flows

  * [S1]: An Admin enters a user name, a password, confirm password, the role, and if the user is enabled (or active) in the system. The Admin presses the button to add the user [E1][E2].
  * [S2]: An Admin selects a user from the list of possible users, confirms the delete, and presses the button to delete the user.

## 1.4 Alternative Flows

  * [E1] The system prompts the Admin to correct the format of a required data field because the input of that data field does not match that specified in Section 1.6.
  * [E2] The password and repeated password must match or an error is displayed.
"""

class TestUCSToClaret(unittest.TestCase):

    def test_clean_step_text(self):
        text = 'An Admin enters [S1] and presses button [E1][E2] with "quotes".'
        cleaned = clean_step_text(text)
        self.assertNotIn('[S1]', cleaned)
        self.assertNotIn('[E1]', cleaned)
        self.assertNotIn('[E2]', cleaned)
        self.assertNotIn('"', cleaned)
        self.assertIn("An Admin enters", cleaned)

    def test_identify_actor(self):
        self.assertEqual(identify_actor("An Admin enters a user name")[0], "admin")
        self.assertEqual(identify_actor("The Patient views their records")[0], "patient")
        self.assertEqual(identify_actor("HCP Shelly Vang records office visit")[0], "hcp")
        self.assertEqual(identify_actor("A user logs into the system")[0], "user")

    def test_parse_and_generate_claret(self):
        with tempfile.NamedTemporaryFile("w", suffix=".md", delete=False, encoding="utf-8") as f:
            f.write(SAMPLE_UCS_MD)
            tmp_path = Path(f.name)

        try:
            ucs = UCSParser.parse_file(tmp_path, system_name="iTrust2")
            self.assertIsNotNone(ucs)
            self.assertEqual(ucs.uc_id, "UC1")
            self.assertIn("User Functionality", ucs.uc_name)
            self.assertEqual(len(ucs.sub_flows), 2)
            self.assertEqual(len(ucs.alt_flows), 2)

            generator = ClaretModelGenerator(ucs, version="1.0")
            claret_code = generator.build_specification()

            # Verify CLARET grammar elements
            self.assertIn('systemName "iTrust2"', claret_code)
            self.assertIn('usecase "UC1 User Functionality" {', claret_code)
            self.assertIn('actor admin "Admin"', claret_code)
            self.assertIn('basic {', claret_code)
            self.assertIn('step 1 admin', claret_code)
            self.assertIn('step 2 system', claret_code)
            self.assertIn('af[1]', claret_code)
            self.assertIn('ef[1, 2]', claret_code)
            self.assertIn('alternative 1', claret_code)
            self.assertIn('bs 2', claret_code)
            self.assertIn('exception 1', claret_code)
            self.assertIn('exception 2', claret_code)
            self.assertIn('postCondition', claret_code)

            # Golden Rule check: alternation
            basic_lines = [l.strip() for l in claret_code.splitlines() if l.strip().startswith('step ')]
            self.assertTrue(len(basic_lines) >= 2)
        finally:
            tmp_path.unlink()

    def test_batch_translation_flat_output(self):
        with tempfile.TemporaryDirectory() as src_dir, tempfile.TemporaryDirectory() as out_dir:
            f1 = Path(src_dir) / "UC1.md"
            f1.write_text(SAMPLE_UCS_MD, encoding="utf-8")

            translator = UCSToClaretTranslator(
                dirs=[Path(src_dir)],
                output_dir=Path(out_dir),
                system_name="iTrust2",
                version="1.0"
            )
            results = translator.translate_all(validate=False)
            self.assertEqual(len(results), 1)
            self.assertEqual(results[0].name, "UC1UserFunctionality.claret")
            self.assertTrue((Path(out_dir) / "UC1UserFunctionality.claret").exists())

if __name__ == "__main__":
    unittest.main()

