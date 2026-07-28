# 2026-05-26 이슈 #88 — javers-exposed-ddd 명령 측 예제

## 배경

CQRS 예제를 검토하기 쉬운 단위로 유지하기 위해 상위 이슈 #5를 #88, #89, #90으로
분리했다. #88은 명령 측의 Exposed + JaVers + DDD 도우미 흐름만 담당한다.

## 결정

파일은 `examples/javers-exposed-ddd`에 두고 Gradle 프로젝트 경로는
`:javers-exposed-ddd`로 매핑한다. 첫 번째 작업 범위에서는 Kafka, Redis, 벤치마크를
제외한다.

## 결과

첫 번째 예제 작업에서 이제 Exposed를 통해 `Order` 애그리거트를 영속화하고,
`ExposedCdoSnapshotRepository`를 통해 스냅샷을 커밋하며,
`AggregateRepository`를 통해 `DomainEvent` 인스턴스를 발행한다.

## 검증

- `./gradlew :javers-exposed-ddd:test --no-configuration-cache --no-build-cache --no-parallel --console=plain`

## 향후 지침

#89에서는 Testcontainers를 사용하는 Kafka-to-Redis 프로젝션 테스트를 순차적으로
추가해야 한다. #90에서는 README의 설명을 갱신하기 전에 원시 벤치마크 산출물을
기록해야 한다.
