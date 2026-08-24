#!/usr/bin/env python3

import json
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path


SCRIPT = Path(__file__).with_name("run_with_attempt_receipt.py")


class RunWithAttemptReceiptTest(unittest.TestCase):
    def run_helper(self, command, max_attempts=3, receipt_relative="receipt.json"):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            receipt = root / receipt_relative
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

    def test_creates_nested_receipt_directory_before_attempt_logs(self):
        command = [sys.executable, "-c", "print('ok')"]
        result, receipt, logs = self.run_helper(command, receipt_relative="nested/reports/receipt.json")

        self.assertEqual(result.returncode, 0)
        self.assertEqual(receipt["finalStatus"], "passed")
        self.assertEqual(len(receipt["attempts"]), 1)
        self.assertEqual(logs[1], "ok\n")

    def test_stops_on_test_failure_without_retry(self):
        command = [sys.executable, "-c", "print('AssertionError: product failure'); raise SystemExit(1)"]
        result, receipt, _ = self.run_helper(command)

        self.assertNotEqual(result.returncode, 0)
        self.assertEqual(receipt["finalStatus"], "failed")
        self.assertEqual(receipt["retryCount"], 0)
        self.assertEqual(receipt["firstAttempt"]["classification"], "test_failure")
        self.assertEqual(len(receipt["attempts"]), 1)

    def test_stops_on_missing_dependency_without_retry(self):
        command = [
            sys.executable,
            "-c",
            "print('Could not resolve all files for configuration :runtimeClasspath'); raise SystemExit(1)",
        ]
        result, receipt, _ = self.run_helper(command)

        self.assertNotEqual(result.returncode, 0)
        self.assertEqual(receipt["retryCount"], 0)
        self.assertEqual(receipt["firstAttempt"]["classification"], "test_failure")

    def test_retries_http_server_failure_but_not_not_found(self):
        for output, expected in (
            ("Could not GET 'https://repo.example.test/artifact' (status code 503)", "infrastructure_retry"),
            ("Could not GET 'https://repo.example.test/artifact' (status code 404)", "test_failure"),
        ):
            command = [sys.executable, "-c", f"print({output!r}); raise SystemExit(1)"]
            result, receipt, _ = self.run_helper(command, max_attempts=1)

            self.assertNotEqual(result.returncode, 0)
            self.assertEqual(receipt["firstAttempt"]["classification"], expected)

    def test_retries_explicit_network_exception_but_not_product_message(self):
        for output, expected in (
            ("java.net.ConnectException: Connection refused", "infrastructure_retry"),
            ("Caused by: java.net.SocketTimeoutException: Read timed out", "infrastructure_retry"),
            ("java.io.IOException: Resource temporarily unavailable", "infrastructure_retry"),
            ("java.io.FileNotFoundException: Too many open files", "infrastructure_retry"),
            ("java.lang.AssertionError: expected: resource temporarily unavailable", "test_failure"),
            ("org.opentest4j.AssertionFailedError: expected: docker daemon is not running", "test_failure"),
            ("Caused by: java.lang.AssertionError: expected: too many open files", "test_failure"),
            ("AssertionError: product returned connection refused", "test_failure"),
            ("AssertionError: product returned read timed out", "test_failure"),
            ("AssertionError: product returned\nconnection refused", "test_failure"),
            ("connection refused", "test_failure"),
            ("resource temporarily unavailable", "test_failure"),
            ("too many open files", "test_failure"),
            ("AssertionError: Could not GET 'https://repo.example.test/artifact' (status code 503)", "test_failure"),
            ("RuntimeError: product returned Could not GET 'https://repo.example.test/artifact' (status code 503)", "test_failure"),
            ("RuntimeError: product returned resource temporarily unavailable", "test_failure"),
            ("RuntimeError: product returned too many open files", "test_failure"),
        ):
            command = [sys.executable, "-c", f"print({output!r}); raise SystemExit(1)"]
            result, receipt, _ = self.run_helper(command, max_attempts=1)

            self.assertNotEqual(result.returncode, 0)
            self.assertEqual(receipt["firstAttempt"]["classification"], expected)


if __name__ == "__main__":
    unittest.main()
