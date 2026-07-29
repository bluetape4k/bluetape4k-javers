# Issue #105 - Kafka 감사 프로젝션에서 얻은 교훈

## 배경

Kafka 스냅샷 저장소는 의도적으로 쓰기 전용이지만, #105에는 읽기를 지원하는 감사
경로가 필요했다.

## 결정

Kafka 저장소는 발행자 역할로 유지하고, Kafka 레코드를 기존
`CdoSnapshotRepository`에 재생하는 별도의 프로젝터를 도입한다.

## 결과

프로젝션 경로는 새로운 저장소 추상화를 추가하지 않고 bluetape4k-kafka 소비자,
기존 JaVers 코덱, Redis/Caffeine 저장소 대상, Testcontainers 실행기를 재사용한다.

## 검증

- `./gradlew :javers-persistence-kafka:test --no-configuration-cache --no-build-cache --no-parallel --console=plain`
- 결과: `SUCCESS: Executed 39 tests in 10.9s`

## 향후 지침

#131에서는 영속 읽기 저장소와 Kafka 발행을 명시적으로 조합한다. Kafka 저장소에
암묵적으로 읽기 기능을 추가해서는 안 된다.
