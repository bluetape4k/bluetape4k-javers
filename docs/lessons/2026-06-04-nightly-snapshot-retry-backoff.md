## 배경

Nightly 및 CI 매트릭스 작업은 Central 스냅샷에서 상위 `1.11.0-SNAPSHOT` 아티팩트를
해결하는 동안 간헐적으로 실패했다. 로컬 Central 메타데이터 확인에서는 HTTP 200이
반환되었지만 GitHub 호스팅 러너는 간헐적으로 HTTP 403을 받았다.

## 결정

CI와 Nightly Gradle 단계에 동일한 재시도 정책을 사용한다. 최대 다섯 번 시도하며
각 시도 사이에 30초 동안 대기한다.

## 결과

이제 워크플로는 일시적인 Central 스냅샷 메타데이터 장애가 복구될 시간을 더 확보한
후에 모듈 테스트를 실패로 표시한다.

## 검증

- `git diff --check`
- `actionlint .github/workflows/*.yml`

## 향후 지침

하위 bluetape4k 저장소가 아직 출시되지 않은 상위 스냅샷을 사용할 때는 먼저 상위
프로젝트를 안정화한다. 그 후 상위 CI 및 Nightly 게이트가 통과하면 하위 Nightly를
다시 실행한다.
