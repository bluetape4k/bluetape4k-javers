# Issue #135 스냅샷 이벤트 파이프라인에서 얻은 교훈

## 배경

Issue #135에서는 전송 기술에 독립적인 JaVers 스냅샷 이벤트 계약을 추가하고 기존
Kafka 저장소가 이벤트 발행자를 통해 발행하도록 변경했다.

## 결정

`javers-core`는 전송 기술에 독립적이고 관련 의존성이 없는 상태로 유지한다. Kafka
전용 발행자와 규칙에 맞는 `bluetape4k-kafka` 프로듀서 헬퍼 사용 코드는
`javers-persistence-kafka`에 둔다.

생성자 검증이 필요한 `data class` 값은 비공개 주 생성자와 companion
`operator fun invoke(...)`를 함께 사용한다. 생성되는 `copy()`가 생성자의
가시성을 따르도록 `@ConsistentCopyVisibility`를 추가한다.

## 결과

Spring Kafka 저장소와 vanilla Kafka 저장소는 쓰기 전용 동작과 발행 실패 전파를
유지하면서 `CdoSnapshotEvent<String>` 메타데이터와 페이로드 생성 로직을 공유한다.

엄격한 후속 리뷰를 거쳐 `bluetape4k-kafka` 의존성을 `api`에서
`implementation`으로 옮기고, 추적 로그에서 전체 스냅샷 페이로드를 제거했으며,
저장소가 생성한 프로듀서와 Spring 인터럽트 처리를 검증하는 테스트를 추가했다.

## 검증

- `./gradlew :javers-core:test :javers-persistence-kafka:test --no-configuration-cache --no-build-cache --no-parallel --console=plain`
- `./gradlew :javers-persistence-kafka:dependencies --configuration api --no-configuration-cache --no-build-cache --console=plain`
- `./gradlew :javers-persistence-kafka:dependencies --configuration runtimeClasspath --no-configuration-cache --no-build-cache --console=plain`
- `git diff --check`

## 향후 유의 사항

NATS, SQS 또는 다른 전송 기술을 추가할 때는 `CdoSnapshotEventPublisher<T>`를
감싸는 전송 어댑터만 구현한다. 전송 기술 의존성을 `javers-core`에 추가해서는 안
된다.
