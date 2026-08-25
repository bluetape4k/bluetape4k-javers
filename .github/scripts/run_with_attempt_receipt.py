#!/usr/bin/env python3
"""Run a CI command with bounded infrastructure-only retries and evidence."""

from __future__ import annotations

import argparse
import json
import os
import re
import shlex
import subprocess
import sys
import time
from datetime import datetime, timezone
from pathlib import Path
from typing import Any, Sequence


INFRASTRUCTURE_PATTERNS = tuple(
    re.compile(pattern, re.IGNORECASE)
    for pattern in (
        r"(?mi)^[ \t]*(?:caused by:[ \t]*)?(?:[a-z_$][\w$]*\.)*nosuchfileexception:[^\r\n]*(?:in-progress-results|results-generic\.bin)\b[^\r\n]*$",
        r"(?mi)^[ \t]*(?:caused by:[ \t]*)?java\.net\.(?:connectexception|socketexception|sockettimeoutexception|unknownhostexception):[^\r\n]*(?:connection refused|connection reset|connect timed out|read timed out|network is unreachable|temporary failure in name resolution)[ \t]*$",
        r"(?mi)^[ \t]*(?:error:[ \t]*)?cannot connect to the docker daemon\b[^\r\n]*$",
        r"(?mi)^[ \t]*docker daemon is not running\b[^\r\n]*$",
        r"(?mi)^[ \t]*could not connect to ryuk\b[^\r\n]*$",
        r"(?mi)^[ \t]*(?:caused by:[ \t]*)?(?:org\.testcontainers\.containers\.)?containerlaunchexception:[^\r\n]*(?:timed out|could not start)\b[^\r\n]*$",
        r"(?mi)^[ \t]*(?:could not|failed to) pull image\b[^\r\n]*(?:timeout|connection|\b5\d\d\b)[^\r\n]*$",
        r"(?mi)^[ \t]*error response from daemon:[^\r\n]*(?:timeout|temporarily unavailable|connection|\b5\d\d\b)[^\r\n]*$",
        r"(?mi)^[ \t]*(?:gradle daemon\b|the message received from the daemon indicates that the daemon has)\b[^\r\n]*disappeared[^\r\n]*$",
        r"(?mi)^[ \t]*(?:>\s*)?(?:could not|failed to) (?:get|head)[ \t]+['\"]?https?://\S+.*\b(?:429|5\d\d)\b.*$",
        r"(?mi)^[ \t]*(?:caused by:[ \t]*)?java\.io\.(?:ioexception|filenotfoundexception):[^\r\n]*resource temporarily unavailable[ \t]*$",
        r"(?mi)^[ \t]*(?:caused by:[ \t]*)?java\.io\.(?:ioexception|filenotfoundexception):[^\r\n]*too many open files[ \t]*$",
        r"(?mi)^[ \t]*(?:bash|sh):[^\r\n]*(?:resource temporarily unavailable|too many open files)[ \t]*$",
    )
)

PRODUCT_FAILURE_PATTERNS = tuple(
    re.compile(pattern, re.IGNORECASE)
    for pattern in (
        r"(?mi)^[ \t]*(?:caused by:[ \t]*)?(?:[a-z_$][\w$]*\.)*(?:assertionerror|assertionfailederror)\b",
    )
)


def classify(exit_code: int, output: str) -> str:
    if exit_code == 0:
        return "passed"
    if any(pattern.search(output) for pattern in PRODUCT_FAILURE_PATTERNS):
        return "test_failure"
    if any(pattern.search(output) for pattern in INFRASTRUCTURE_PATTERNS):
        return "infrastructure_retry"
    return "test_failure"


def utc_now() -> str:
    return datetime.now(timezone.utc).isoformat().replace("+00:00", "Z")


def relative_path(path: Path) -> str:
    try:
        return str(path.relative_to(Path.cwd()))
    except ValueError:
        return str(path)


def write_receipt(path: Path, receipt: dict[str, Any]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    temporary = path.with_suffix(path.suffix + ".tmp")
    temporary.write_text(json.dumps(receipt, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    temporary.replace(path)


def append_summary(receipt: dict[str, Any]) -> None:
    summary = os.environ.get("GITHUB_STEP_SUMMARY")
    if not summary:
        return

    first = receipt["attempts"][0]
    lines = [
        f"### Attempt receipt: `{receipt['name']}`",
        f"- Final status: `{receipt['finalStatus']}`",
        f"- First attempt: `{first['classification']}` (exit `{first['exitCode']}`)",
        f"- First-attempt log: `{first['log']}`",
        f"- Retry count: `{receipt['retryCount']}` (infrastructure failures only)",
        "- A path-filtered or skipped job is not runtime coverage proof.",
    ]
    if receipt["finalStatus"] == "passed_after_retry":
        lines.append("- Green result includes an infrastructure retry; inspect the first-attempt evidence.")
    if receipt["finalStatus"] == "failed":
        lines.append("- Final failure is preserved; test failures are never retried by this helper.")
    with Path(summary).open("a", encoding="utf-8") as handle:
        handle.write("\n".join(lines) + "\n")


def run(name: str, max_attempts: int, sleep_seconds: int, receipt_path: Path, command: Sequence[str]) -> int:
    attempts: list[dict[str, Any]] = []
    command_text = shlex.join(command)
    final_status = "failed"

    for attempt_number in range(1, max_attempts + 1):
        started_at = utc_now()
        completed = subprocess.run(
            list(command),
            stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT,
            text=True,
            check=False,
        )
        output = completed.stdout or ""
        sys.stdout.write(output)
        sys.stdout.flush()

        log_path = receipt_path.parent / f"{name}-attempt-{attempt_number}.log"
        log_path.parent.mkdir(parents=True, exist_ok=True)
        log_path.write_text(output, encoding="utf-8")
        classification = classify(completed.returncode, output)
        attempts.append(
            {
                "attempt": attempt_number,
                "startedAt": started_at,
                "finishedAt": utc_now(),
                "exitCode": completed.returncode,
                "classification": classification,
                "log": relative_path(log_path),
            }
        )

        if completed.returncode == 0:
            final_status = "passed" if attempt_number == 1 else "passed_after_retry"
            break
        if classification != "infrastructure_retry" or attempt_number == max_attempts:
            break
        time.sleep(sleep_seconds)

    receipt = {
        "name": name,
        "command": command_text,
        "maxAttempts": max_attempts,
        "retryPolicy": "infrastructure failures only; test failures stop immediately",
        "firstAttempt": attempts[0],
        "attempts": attempts,
        "retryCount": len(attempts) - 1,
        "finalStatus": final_status,
        "finalExitCode": attempts[-1]["exitCode"],
    }
    write_receipt(receipt_path, receipt)
    append_summary(receipt)

    if final_status == "passed_after_retry":
        print(f"{name}: passed after {len(attempts) - 1} infrastructure retry")
    elif final_status == "failed":
        print(f"{name}: failed on {attempts[-1]['classification']} (attempt {len(attempts)})", file=sys.stderr)
    return int(receipt["finalExitCode"])


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--name", required=True)
    parser.add_argument("--max-attempts", type=int, required=True)
    parser.add_argument("--sleep-seconds", type=int, default=30)
    parser.add_argument("--receipt", type=Path, required=True)
    parser.add_argument("command", nargs=argparse.REMAINDER)
    arguments = parser.parse_args()
    if arguments.command and arguments.command[0] == "--":
        arguments.command = arguments.command[1:]
    if arguments.max_attempts < 1:
        parser.error("--max-attempts must be positive")
    if arguments.sleep_seconds < 0:
        parser.error("--sleep-seconds must not be negative")
    if not arguments.command:
        parser.error("a command is required after --")
    return arguments


if __name__ == "__main__":
    arguments = parse_args()
    raise SystemExit(
        run(
            arguments.name,
            arguments.max_attempts,
            arguments.sleep_seconds,
            arguments.receipt,
            arguments.command,
        )
    )
