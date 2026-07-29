# Nightly 스냅샷 새로 고침

## 배경

Nightly는 Gradle 캐시를 복원하고 변경 가능한 bluetape4k Central 스냅샷 아티팩트를
사용한다. 오래된 스냅샷 메타데이터 또는 동시에 발생하는 Central 스냅샷 메타데이터
요청으로 인해 테스트가 실행되기 전에 모듈 작업이 실패할 수 있다.

## 결정

Nightly Gradle 호출에 `--refresh-dependencies`를 전달하고 예약 cron의 분을 서로 다르게
설정한다. 이를 통해 모든 하위 저장소를 동시에 시작하지 않으면서 스냅샷 메타데이터를
다시 확인한다.

## 결과

Nightly는 빌드 상태에 대한 캐시 재사용을 유지하면서 변경 가능한 메타데이터를 새로
고치고, 예약 실행 시 저장소 간 Central 스냅샷 경합을 줄인다.

## 검증

- `actionlint .github/workflows/nightly-tests.yml`
- `git diff --check`

## 향후 규칙

하위 저장소가 bluetape4k 스냅샷을 사용할 때는 예약된 Nightly 워크플로의 시차를
유지하고, 저장소가 안정 버전 의존성으로 돌아갈 때까지 Gradle 호출에
`--refresh-dependencies`를 유지한다.
