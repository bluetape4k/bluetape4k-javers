# Issue 110 오래된 스냅샷 식별자 보호 장치

## 배경

`AbstractCdoSnapshotRepository.getSnapshots(snapshotIdentifiers)`는 요청받은
스냅샷 버전을 찾을 때 목록 인덱스 계산을 사용했다. 이 방식은 스냅샷 목록의
버전이 빠짐없이 연속적이라고 가정한다. 영속화된 버전이 오래되었거나 일부
누락되면 잘못된 스냅샷을 가리키거나 범위 초과 예외가 발생할 수 있었다.

## 결정

대상 전역 ID의 이력을 불러온 뒤 `CdoSnapshot.version`이 정확히 일치하는
항목으로 스냅샷 식별자를 찾는다. 누락되었거나 오래된 식별자는 예외를 던지지
않고 반환할 부분 집합에서 제외한다.

## 결과

유효한 식별자는 예상한 영속 스냅샷을 계속 반환하고, 오래된 양수 식별자는
무시한다. 공통 저장소 계약은 이제 테스트 전용 저장소로 버전 누락 상황을
재현한다.

## 검증

- `./gradlew :javers-core:test --tests "org.javers.repository.jql.InMemoryJaversShadowTest" --no-configuration-cache --no-build-cache --no-parallel --console=plain`
- `./gradlew :javers-core:test :javers-exposed:test --no-configuration-cache --no-build-cache --no-parallel --console=plain`
- `./gradlew :javers-persistence-redis:test :javers-persistence-kafka:test --no-configuration-cache --no-build-cache --no-parallel --console=plain`
- `./gradlew :javers-core:test --no-configuration-cache --no-build-cache --no-parallel --console=plain`
- `git diff --check`

## 향후 지침

스토리지 계약이 빈틈없는 버전 순서를 명시적으로 보장하지 않는다면 JaVers
스냅샷 버전을 목록 인덱스에 매핑하지 않는다. 호출자가 `SnapshotIdentifier`
값을 제공할 때는 정확한 버전 일치를 우선한다.
