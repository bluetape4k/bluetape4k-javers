# Issue #131 복합 CDO 스냅샷 저장소

## 배경

Issue #131에서는 하나의 읽기/쿼리 주 저장소와 순서가 보장된 보조 저장소 팬아웃을
결합하는 JaVers `CdoSnapshotRepository` 직접 구현을 추가했다. 핵심 설계 수정은
`persist(commit)`이 `saveSnapshot()`으로 스냅샷만 팬아웃하지 않고
`primary.persist(commit)`을 직접 호출해야 한다는 점이었다. 주 저장소가 자체
헤드와 순서 의미 체계를 소유하기 때문이다.

## 결정

- 복합 저장소는 `javers-core` 아래에 기존 기능을 확장하는 형태로 둔다.
- 모든 읽기는 주 저장소에 위임한다.
- 주 저장소에 먼저 쓰고, 그다음 보조 저장소에 순서대로 쓴다.
- 위임 대상 전체의 원자성을 보장하지 않는다. 주 저장소 쓰기가 성공한 뒤 보조
  저장소에서 실패하면 주 저장소를 롤백하지 않고 실패를 그대로 드러낸다.
- `CdoSnapshotRepository`가 안전한 롤백 계약을 제공하지 않으므로 롤백이나 분산
  트랜잭션 동작은 도입하지 않는다.

## 결과

구현에는 명시적 실패 정책, 위임 대상의 실패 메타데이터, 집계 예외, 주 저장소 우선
쓰기 동작, README 언어별 문서 갱신, 영문 레이블을 사용한 README 다이어그램이
포함되었다. 리뷰 과정에서는 `CompositeCdoSnapshotException`이 페이로드를 만들기
전에 빈 실패 목록을 거부하고 방어적 복사본을 저장하도록 계약을 강화했다.

## 검증

- `./gradlew :javers-core:test --no-configuration-cache --no-build-cache --console=plain`
  - `BUILD SUCCESSFUL`
  - `SUCCESS: Executed 197 tests in 13.8s`
- `./gradlew :javers-persistence-kafka:test --no-configuration-cache --no-build-cache --no-parallel --console=plain`
  - `BUILD SUCCESS`
  - `SUCCESS: Executed 39 tests in 12.8s`
- 정적 금지 패턴 검사에서 일치 항목이 없었다.
- README 다이어그램 PNG를 렌더링하고 육안으로 확인했다.

## 향후 유의 사항

향후 복합 저장소를 작업할 때는 상속과 직접 위임 중 하나를 선택하기 전에 JaVers
저장소의 헤드/순서 계약을 검토한다. 주 저장소 쓰기가 성공한 뒤 보조 저장소
쓰기가 실패할 수 있다면 롤백 동작을 암시하지 말고 비원자적 일관성 의미 체계를
KDoc, README, 테스트에 명시한다.
