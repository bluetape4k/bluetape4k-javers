# Detekt 실행 범위와 기존 위반

배포 모듈 6개를 실제 분석하고 root에서 보고서를 한 번만 합친다. 기존 위반 82개 고유 ID는 모듈 baseline에 고정했다. core의 초기 출력은 중복 ID를 포함해 47개 위반이었다. 새 위반을 추가하면 실패함을 확인했고 제거 후 전체 분석이 통과했다. HTML의 양수 Kotlin 파일 수 및 모듈별 XML과 merged XML이 없으면 CI가 실패한다. 예제와 benchmark는 이번 배포 모듈 분석 범위에 포함하지 않는다. CI에서 baseline을 자동 재생성하지 않는다.

실행: `./gradlew detekt --no-parallel --no-configuration-cache` 후
`python3 .github/scripts/validate_detekt_reports.py`.

보고서는 `build/reports/detekt/merged.xml`과 각 `javers-*/build/reports/detekt/`에 생성한다.
기존 위반은 별도 정리에서 개별 해결하고 baseline 항목을 줄인다. 새 위반을 baseline으로 숨기지 않는다.
