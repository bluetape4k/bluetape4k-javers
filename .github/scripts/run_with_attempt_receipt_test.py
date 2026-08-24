#!/usr/bin/env python3

import json
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path


SCRIPT = Path(__file__).with_name("run_with_attempt_receipt.py")


class RunWithAttemptReceiptTest(unittest.TestCase):
    def run_helper(self, command, max_attempts=3):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            receipt = root / "receipt.json"
            result = subprocess.run(
                [
                    sys.executable,
                    str(SCRIPT),
                    "--name",
                    "fixture",
                    "--max-attempts",
                    str(max_attempts),
                    "--sleep-seconds",
                    "0",
                    "--receipt",
                    str(receipt),
                    "--",
                    *command,
                ],
                check=False,
                capture_output=True,
                text=True,
            )
            payload = json.loads(receipt.read_text(encoding="utf-8"))
            logs = {
                attempt["attempt"]: Path(attempt["log"]).read_text(encoding="utf-8")
                for attempt in payload["attempts"]
            }
            return result, payload, logs

    def test_retries_infrastructure_failure_and_records_first_attempt(self):
        with tempfile.TemporaryDirectory() as directory:
            marker = Path(directory) / "marker"
            command = [
                sys.executable,
                "-c",
                "from pathlib import Path; import sys; p=Path(sys.argv[1]); "
                "print('NoSuchFileException: in-progress-results-generic.bin'); "
                "(p.write_text('done') if not p.exists() else sys.exit(0)); sys.exit(1)",
                str(marker),
            ]
            result, receipt, logs = self.run_helper(command)

        self.assertEqual(result.returncode, 0)
        self.assertEqual(receipt["finalStatus"], "passed_after_retry")
        self.assertEqual(receipt["retryCount"], 1)
        self.assertEqual(receipt["firstAttempt"]["classification"], "infrastructure_retry")
        self.assertEqual(receipt["attempts"][1]["classification"], "passed")
        self.assertIn("NoSuchFileException", logs[1])

    def test_stops_on_test_failure_without_retry(self):
        command = [sys.executable, "-c", "print('AssertionError: product failure'); raise SystemExit(1)"]
        result, receipt, _ = self.run_helper(command)

        self.assertNotEqual(result.returncode, 0)
        self.assertEqual(receipt["finalStatus"], "failed")
        self.assertEqual(receipt["retryCount"], 0)
        self.assertEqual(receipt["firstAttempt"]["classification"], "test_failure")
        self.assertEqual(len(receipt["attempts"]), 1)


if __name__ == "__main__":
    unittest.main()
