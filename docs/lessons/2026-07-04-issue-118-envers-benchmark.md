# Issue #118 Envers 벤치마크 모듈

## 배경

Envers 비교 코드가 `examples/javers-exposed-ddd` 테스트 소스에 있어 일반 예제 테스트가
벤치마크 근거를 다시 쓸 수 있었다. 저장소에는 이미 `kotlinx-benchmark` 스모크 실행을 위한
`benchmark/javers-exposed-benchmark`가 있었다.

## 결정

Envers 비교를 벤치마크 모듈로 옮기고 `mainEnversComparisonSmokeBenchmark`로 실행할 수
있게 한다. 과거에 커밋한 JSON은 스냅숏으로 유지하되, 새 실행 방법은 벤치마크 모듈을
기준으로 문서화한다.

## 결과

예제 모듈의 테스트는 더 이상 `hibernate-envers`에 의존하지 않는다. 이제 벤치마크 모듈이
변경되면 CI와 Nightly 벤치마크 작업에서 커밋 메타데이터와 Envers 스모크 작업을 모두
실행한다.

## 향후 지침

새 벤치마크 하네스를 예제 테스트 소스에 추가하지 않는다. 벤치마크 모듈 아래에 두고,
README나 워크플로 명령을 갱신하기 전에 생성된 `kotlinx-benchmark` 작업 이름을 확인한다.
