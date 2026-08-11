#!/usr/bin/env python3
"""Kover coverage artifact layout을 검증하고 정규화합니다."""

from __future__ import annotations

import sys
from pathlib import Path


REPORT_NAMES = ("report.xml", "reportJvm.xml")
ARTIFACT_MODULES = {
    "coverage-javers-core": "javers-core",
    "coverage-javers-ddd": "javers-ddd",
    "coverage-examples-javers-exposed-ddd": "javers-exposed-ddd",
    "coverage-examples-javers-ktor": "javers-ktor",
    "coverage-examples-javers-spring-boot4": "javers-spring-boot4",
    "coverage-javers-exposed": "javers-exposed",
    "coverage-persistence-redis": "javers-persistence-redis",
    "coverage-persistence-kafka": "javers-persistence-kafka",
    "coverage-javers-spring-boot4-autoconfigure": "javers-spring-boot4-autoconfigure",
}


def _candidate_directories(root: Path, artifact: str) -> tuple[Path, ...]:
    module = ARTIFACT_MODULES.get(artifact, artifact.removeprefix("coverage-"))
    candidates = [root / artifact.removeprefix("coverage-"), root / module]
    return tuple(dict.fromkeys(candidates))


def _has_report(directory: Path) -> bool:
    return any(path.is_file() and path.name in REPORT_NAMES for path in directory.rglob("*"))


def _normalize_single_artifact(root: Path, artifact: str) -> bool:
    """단일 artifact에서 평탄화된 report 파일을 기대 경로로 감쌉니다."""
    reports = [
        path
        for path in root.iterdir()
        if path.is_file() and path.name in REPORT_NAMES
    ]
    if not reports:
        return False

    artifact_dir = root / artifact
    artifact_dir.mkdir(parents=True, exist_ok=True)
    for report in reports:
        report.rename(artifact_dir / report.name)
    return True


def _normalize_artifact(root: Path, artifact: str, single_artifact: bool) -> None:
    artifact_dir = root / artifact
    if artifact_dir.is_dir():
        return

    for candidate in _candidate_directories(root, artifact):
        if not candidate.is_dir():
            continue
        artifact_dir.mkdir(parents=True)
        candidate.rename(artifact_dir / candidate.name)
        return

    if single_artifact:
        _normalize_single_artifact(root, artifact)


def validate_artifacts(root: Path, artifacts: list[str]) -> dict[str, list[str]]:
    """지원 layout을 정규화하고 누락/빈 artifact 이름을 반환합니다."""
    if not root.is_dir():
        return {"missing": list(artifacts), "empty": []}

    single_artifact = len(artifacts) == 1
    missing: list[str] = []
    empty: list[str] = []

    for artifact in artifacts:
        _normalize_artifact(root, artifact, single_artifact)
        artifact_dir = root / artifact
        if not artifact_dir.is_dir():
            missing.append(artifact)
        elif not _has_report(artifact_dir):
            empty.append(artifact)

    return {"missing": missing, "empty": empty}


def main(argv: list[str]) -> int:
    if len(argv) < 3:
        print(f"usage: {argv[0]} COVERAGE_ROOT ARTIFACT [ARTIFACT ...]", file=sys.stderr)
        return 2

    root = Path(argv[1])
    artifacts = argv[2:]
    errors = validate_artifacts(root, artifacts)
    if errors["missing"] or errors["empty"]:
        print("::error title=Coverage artifacts incomplete::Expected coverage artifacts are missing or empty.")
        if errors["missing"]:
            print("Missing artifacts:")
            print("\n".join(f"  - {artifact}" for artifact in errors["missing"]))
        if errors["empty"]:
            print("Empty artifacts:")
            print("\n".join(f"  - {artifact}" for artifact in errors["empty"]))
        print("Downloaded artifacts:")
        if root.is_dir():
            for directory in sorted(path for path in root.rglob("*") if path.is_dir()):
                print(directory)
        return 1

    print(f"Validated {len(artifacts)} expected coverage artifacts.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv))
