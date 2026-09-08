#!/usr/bin/env python3
"""배포 모듈마다 실제 Kotlin 분석 보고서가 생성됐는지 검증한다."""
import re
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

MODULES = (
    "javers-core", "javers-ddd", "javers-exposed", "javers-persistence-kafka",
    "javers-persistence-redis", "javers-spring-boot4-autoconfigure",
)


def validate(root):
    ET.parse(root / "build/reports/detekt/merged.xml")
    for module in MODULES:
        reports = root / module / "build/reports/detekt"
        ET.parse(reports / "detekt.xml")
        html = (reports / "detekt.html").read_text()
        metric = re.search(r"([0-9,]+) number of kt files", html)
        if metric is None or int(metric.group(1).replace(",", "")) <= 0:
            raise ValueError(f"{module}: Kotlin 분석 파일 수가 없거나 0입니다")
        print(f"{module}: {metric.group(1)} Kotlin files, XML/HTML 확인")


if __name__ == "__main__":
    try:
        validate(Path(__file__).resolve().parents[2])
    except (OSError, ValueError, ET.ParseError) as error:
        print(f"Detekt 보고서 검증 실패: {error}", file=sys.stderr)
        sys.exit(1)
