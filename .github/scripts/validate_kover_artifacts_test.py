#!/usr/bin/env python3
"""Kover artifact layout 정규화 회귀 테스트입니다."""

from __future__ import annotations

import importlib.util
import tempfile
import unittest
from pathlib import Path


SCRIPT = Path(__file__).with_name("validate_kover_artifacts.py")
SPEC = importlib.util.spec_from_file_location("validate_kover_artifacts", SCRIPT)
if SPEC is None or SPEC.loader is None:
    raise RuntimeError(f"Unable to load {SCRIPT}")
MODULE = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(MODULE)


class ValidateKoverArtifactsTest(unittest.TestCase):
    def test_normalizes_flattened_single_report(self) -> None:
        with tempfile.TemporaryDirectory() as temp_dir:
            root = Path(temp_dir)
            (root / "report.xml").write_text("<report />", encoding="utf-8")

            errors = MODULE.validate_artifacts(root, ["coverage-persistence-redis"])

            self.assertEqual({"missing": [], "empty": []}, errors)
            self.assertTrue(root.joinpath("coverage-persistence-redis", "report.xml").is_file())

    def test_normalizes_single_module_directory(self) -> None:
        with tempfile.TemporaryDirectory() as temp_dir:
            root = Path(temp_dir)
            report = root / "javers-persistence-redis" / "build" / "reports" / "kover" / "report.xml"
            report.parent.mkdir(parents=True)
            report.write_text("<report />", encoding="utf-8")

            errors = MODULE.validate_artifacts(root, ["coverage-persistence-redis"])

            self.assertEqual({"missing": [], "empty": []}, errors)
            self.assertTrue(root.joinpath("coverage-persistence-redis", "javers-persistence-redis", "build").is_dir())

    def test_accepts_named_directories_for_multiple_artifacts(self) -> None:
        with tempfile.TemporaryDirectory() as temp_dir:
            root = Path(temp_dir)
            for artifact in ("coverage-javers-core", "coverage-persistence-redis"):
                report = root / artifact / "report.xml"
                report.parent.mkdir(parents=True)
                report.write_text("<report />", encoding="utf-8")

            errors = MODULE.validate_artifacts(
                root,
                ["coverage-javers-core", "coverage-persistence-redis"],
            )

            self.assertEqual({"missing": [], "empty": []}, errors)

    def test_reports_missing_or_empty_artifacts(self) -> None:
        with tempfile.TemporaryDirectory() as temp_dir:
            root = Path(temp_dir)
            (root / "coverage-javers-core").mkdir()

            errors = MODULE.validate_artifacts(
                root,
                ["coverage-javers-core", "coverage-persistence-redis"],
            )

            self.assertEqual(
                {
                    "missing": ["coverage-persistence-redis"],
                    "empty": ["coverage-javers-core"],
                },
                errors,
            )


if __name__ == "__main__":
    unittest.main()
