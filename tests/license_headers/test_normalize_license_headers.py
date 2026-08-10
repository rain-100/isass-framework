# SPDX-License-Identifier: LGPL-3.0-only

from __future__ import annotations

import importlib.util
import sys
import tempfile
import unittest
from pathlib import Path


SCRIPT = Path(__file__).resolve().parents[2] / "scripts" / "normalize_license_headers.py"
SPEC = importlib.util.spec_from_file_location("normalize_license_headers", SCRIPT)
assert SPEC and SPEC.loader
normalizer = importlib.util.module_from_spec(SPEC)
sys.modules[SPEC.name] = normalizer
SPEC.loader.exec_module(normalizer)


LGPL_HEADER = """/*
 * GNU LESSER GENERAL PUBLIC LICENSE
 * Version 3, 29 June 2007
 * Copyright (C) 2007 Free Software Foundation, Inc. <http://fsf.org/>
 * This version of the GNU Lesser General Public License incorporates the terms and conditions of version 3 of the GNU General Public License.
 * 0. Additional Definitions.
 * 4. Combined Works.
 * 6. Revised Versions of the GNU Lesser General Public License.
 * A proxy's public statement of acceptance is permanent authorization.
 */"""


class NormalizeLicenseHeadersTest(unittest.TestCase):
    def setUp(self) -> None:
        self.temporary = tempfile.TemporaryDirectory()
        self.root = Path(self.temporary.name)
        self.lgpl = normalizer.ProjectConfig(
            "fixture",
            self.root,
            "LGPL-3.0-only",
            ("src",),
        )
        self.apache = normalizer.ProjectConfig(
            "fixture",
            self.root,
            "Apache-2.0",
            ("src",),
        )

    def tearDown(self) -> None:
        self.temporary.cleanup()

    def classify(self, name: str, content: bytes | str, config=None):
        path = self.root / name
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes(content.encode("utf-8") if isinstance(content, str) else content)
        return normalizer.classify_file(path, config or self.lgpl)

    def test_adds_spdx_without_inventing_copyright(self) -> None:
        result = self.classify("NoLicense.java", "package example;\n\nclass NoLicense {}\n")
        self.assertEqual(("NO_LICENSE", "ADD"), (result.classification, result.action))
        normalized = result.normalized.decode()
        self.assertTrue(normalized.startswith("// SPDX-License-Identifier: LGPL-3.0-only\n\n"))
        self.assertNotIn("Copyright", normalized)
        self.assertTrue(normalized.endswith("package example;\n\nclass NoLicense {}\n"))

    def test_replaces_only_recognized_full_lgpl_boilerplate(self) -> None:
        source = LGPL_HEADER + "\n\npackage example;\n\nclass Verbose {}\n"
        result = self.classify("Verbose.java", source)
        self.assertEqual(("TARGET_VERBOSE_LICENSE", "REPLACE"), (result.classification, result.action))
        normalized = result.normalized.decode()
        self.assertEqual(1, normalized.count("SPDX-License-Identifier"))
        self.assertNotIn("Free Software Foundation", normalized)
        self.assertTrue(normalized.endswith("package example;\n\nclass Verbose {}\n"))

    def test_replaces_commented_out_lgpl_without_uncommenting_source(self) -> None:
        commented_header = "\n".join(f"// {line}" for line in LGPL_HEADER.splitlines())
        source = commented_header + "\n//\n// package example;\n// class Disabled {}\n"
        result = self.classify("Disabled.java", source)
        self.assertEqual("REPLACE", result.action)
        normalized = result.normalized.decode()
        self.assertIn("// package example;", normalized)
        self.assertIn("// class Disabled {}", normalized)
        self.assertNotIn("GNU LESSER", normalized)

    def test_line_comment_license_stops_before_operational_comments(self) -> None:
        source = """# GNU LESSER GENERAL PUBLIC LICENSE
# Version 3, 29 June 2007
# Copyright (C) 2007 Free Software Foundation, Inc.
# This version of the GNU Lesser General Public License incorporates the terms and conditions of version 3 of the GNU General Public License.
# 0. Additional Definitions.
# 4. Combined Works.
# 6. Revised Versions of the GNU Lesser General Public License.
# A proxy can decide whether future versions shall apply.
# That proxy's public statement of acceptance is permanent authorization for the
# Library.
#
# Operational setting: keep this explanation.
value=true
"""
        result = self.classify("settings.properties", source)
        self.assertEqual("REPLACE", result.action)
        normalized = result.normalized.decode()
        self.assertIn("# Operational setting: keep this explanation.", normalized)
        self.assertNotIn("GNU LESSER", normalized)
        self.assertNotIn("\n#\n# Operational", normalized)

    def test_normalization_is_idempotent(self) -> None:
        first = self.classify("Idempotent.java", LGPL_HEADER + "\n\npackage example;\n")
        self.assertEqual("REPLACE", first.action)
        second = self.classify("Idempotent.java", first.normalized)
        self.assertEqual(("TARGET_SHORT_LICENSE", "OK"), (second.classification, second.action))

    def test_existing_target_spdx_is_unchanged(self) -> None:
        source = "// SPDX-License-Identifier: LGPL-3.0-only\n\npackage example;\n"
        result = self.classify("Spdx.java", source)
        self.assertEqual("OK", result.action)

    def test_relocates_standalone_target_spdx_after_package(self) -> None:
        source = "package example;\n\n// SPDX-License-Identifier: LGPL-3.0-only\n\nclass Misplaced {}\n"
        result = self.classify("Misplaced.java", source)
        self.assertEqual(("TARGET_LICENSE_MISPLACED", "RELOCATE"), (result.classification, result.action))
        normalized = result.normalized.decode()
        self.assertTrue(normalized.startswith("// SPDX-License-Identifier: LGPL-3.0-only\n\npackage"))
        self.assertEqual(1, normalized.count("SPDX-License-Identifier"))

    def test_relocates_verbose_header_but_preserves_normal_comment(self) -> None:
        source = "/** Internal utility. */\n\n" + LGPL_HEADER + "\n\npackage example;\n"
        result = self.classify("VerboseMisplaced.java", source)
        self.assertEqual("RELOCATE", result.action)
        normalized = result.normalized.decode()
        self.assertIn("/** Internal utility. */", normalized)
        self.assertNotIn("GNU LESSER", normalized)

    def test_preserves_third_party_apache_copyright(self) -> None:
        source = """/*
 * Copyright 2018 Google LLC
 * Licensed under the Apache License, Version 2.0 (the \"License\");
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing,
 * limitations under the License.
 */
package example;
"""
        result = self.classify("ThirdParty.java", source, self.apache)
        self.assertEqual(("THIRD_PARTY_LICENSE", "SKIP"), (result.classification, result.action))
        self.assertIsNone(result.normalized)

    def test_preserves_parenthesized_c_third_party_notice(self) -> None:
        source = "/* (c) 2014-2024 Guillermo Rauch. Released under the MIT License. */\n"
        result = self.classify("socket.js", source)
        self.assertEqual(("THIRD_PARTY_LICENSE", "SKIP"), (result.classification, result.action))

    def test_multiple_copyright_holders_are_skipped(self) -> None:
        source = """/*
 * Copyright 2018 Original Author
 * Copyright 2026 Current Company
 * Licensed under an unknown license.
 */
package example;
"""
        result = self.classify("Multiple.java", source)
        self.assertEqual(("MULTIPLE_COPYRIGHT", "SKIP"), (result.classification, result.action))

    def test_other_spdx_expression_is_warned_and_preserved(self) -> None:
        source = "// SPDX-License-Identifier: MIT OR Apache-2.0\n\npackage example;\n"
        result = self.classify("Other.java", source)
        self.assertEqual(("OTHER_LICENSE", "WARN"), (result.classification, result.action))
        self.assertIn("MIT OR Apache-2.0", result.reason)

    def test_duplicate_spdx_is_ambiguous(self) -> None:
        source = """// SPDX-License-Identifier: LGPL-3.0-only

// SPDX-License-Identifier: LGPL-3.0-only

package example;
"""
        result = self.classify("Duplicate.java", source)
        self.assertEqual(("AMBIGUOUS", "WARN"), (result.classification, result.action))

    def test_generated_source_is_skipped(self) -> None:
        source = "// Code generated by protoc. DO NOT EDIT.\n\npackage example;\n"
        result = self.classify("Generated.java", source)
        self.assertEqual(("GENERATED", "SKIP"), (result.classification, result.action))

    def test_normal_comment_is_preserved(self) -> None:
        source = "/** Internal utility. */\npackage example;\n"
        result = self.classify("NormalComment.java", source)
        normalized = result.normalized.decode()
        self.assertEqual("ADD", result.action)
        self.assertIn("/** Internal utility. */\npackage example;", normalized)

    def test_target_spdx_with_attribution_is_left_intact(self) -> None:
        source = """/*
 * Copyright 2018 Original Author
 * SPDX-License-Identifier: LGPL-3.0-only
 */
package example;
"""
        result = self.classify("Attributed.java", source)
        self.assertEqual(("TARGET_SHORT_LICENSE", "OK"), (result.classification, result.action))

    def test_unknown_legal_header_is_warned_without_rewrite(self) -> None:
        source = "/* Licensed under the Example License. */\npackage example;\n"
        result = self.classify("Unknown.java", source)
        self.assertEqual(("OTHER_LICENSE", "WARN"), (result.classification, result.action))
        self.assertIsNone(result.normalized)

    def test_empty_and_whitespace_only_files_are_skipped(self) -> None:
        for name, content in (("Empty.java", b""), ("Whitespace.java", b" \r\n\t")):
            with self.subTest(name=name):
                result = self.classify(name, content)
                self.assertEqual(("EMPTY", "SKIP"), (result.classification, result.action))

    def test_utf8_bom_is_preserved(self) -> None:
        source = normalizer.UTF8_BOM + b"package example;\n"
        result = self.classify("Bom.java", source)
        self.assertTrue(result.normalized.startswith(normalizer.UTF8_BOM))
        self.assertEqual(1, result.normalized.count(normalizer.UTF8_BOM))

    def test_crlf_is_preserved_without_introducing_bare_lf(self) -> None:
        source = b"package example;\r\n\r\nclass CrLf {}\r\n"
        result = self.classify("CrLf.java", source)
        payload = result.normalized
        self.assertNotIn(b"\n", payload.replace(b"\r\n", b""))
        self.assertTrue(payload.endswith(source))

    def test_shebang_remains_first_line(self) -> None:
        source = "#!/usr/bin/env python3\n\nprint('ok')\n"
        result = self.classify("script.py", source)
        normalized = result.normalized.decode()
        self.assertTrue(
            normalized.startswith(
                "#!/usr/bin/env python3\n\n# SPDX-License-Identifier: LGPL-3.0-only\n\n"
            )
        )

    def test_python_encoding_declaration_precedes_spdx(self) -> None:
        source = "#!/usr/bin/env python3\n# -*- coding: utf-8 -*-\n\nprint('ok')\n"
        result = self.classify("encoding.py", source)
        normalized = result.normalized.decode()
        self.assertLess(normalized.index("coding: utf-8"), normalized.index("SPDX-License-Identifier"))

    def test_xml_declaration_precedes_spdx(self) -> None:
        source = '<?xml version="1.0" encoding="UTF-8"?>\n<root/>\n'
        result = self.classify("document.xml", source)
        normalized = result.normalized.decode()
        self.assertTrue(normalized.startswith('<?xml version="1.0" encoding="UTF-8"?>\n'))
        self.assertLess(normalized.index("?>"), normalized.index("SPDX-License-Identifier"))

    def test_vue_uses_xml_comment_syntax(self) -> None:
        source = "<template><div /></template>\n"
        result = self.classify("Component.vue", source)
        self.assertTrue(
            result.normalized.decode().startswith(
                "<!-- SPDX-License-Identifier: LGPL-3.0-only -->\n\n"
            )
        )

    def test_explicit_generator_segments_emit_target_language_comments(self) -> None:
        java_result = self.classify("copyright.ftl", LGPL_HEADER + "\n")
        xml_header = LGPL_HEADER.replace("/*", "<!--", 1).replace("*/", "-->")
        xml_result = self.classify("copyright.xml.ftl", xml_header + "\n")
        self.assertTrue(java_result.normalized.decode().startswith("// SPDX-License-Identifier"))
        self.assertTrue(xml_result.normalized.decode().startswith("<!-- SPDX-License-Identifier"))

    def test_non_utf8_input_is_reported_without_rewrite(self) -> None:
        result = self.classify("Invalid.java", b"\xff\xfe\x00x")
        self.assertIn(result.classification, {"UNSUPPORTED"})
        self.assertIn(result.action, {"SKIP", "ERROR"})
        self.assertIsNone(result.normalized)

    def test_minified_bundle_is_skipped(self) -> None:
        result = self.classify("bundle.js", "const value = 1;\n")
        self.assertEqual(("MINIFIED", "SKIP"), (result.classification, result.action))

    def test_discovery_excludes_dependencies_generated_and_special_files(self) -> None:
        for relative in (
            "src/Owned.java",
            "src/generated/Generated.java",
            "src/vendor/Vendor.java",
            "src/node_modules/Dependency.js",
            "src/pom.xml",
        ):
            path = self.root / relative
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text("class Value {}\n")
        discovered = normalizer.discover_files(self.lgpl, (), (), ())
        self.assertEqual([self.root / "src/Owned.java"], discovered)


if __name__ == "__main__":
    unittest.main()
