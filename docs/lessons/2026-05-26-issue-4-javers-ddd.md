# Issue 4 javers-ddd 헬퍼 모듈

## 배경

Issue #4는 `javers-exposed`에 이어 애그리거트 루트, 도메인 이벤트, JaVers
커밋/이력 접근, 이벤트 게시자 어댑터를 위한 DDD 헬퍼 계층을 추가한다.

## 결정

라이브러리 사용자가 이 모듈 외부에서 이벤트 타입을 정의해야 하므로 issue에서
제안한 sealed class 초안 대신 확장 가능한 `DomainEvent` 인터페이스를 사용한다.
애그리거트 영속화는 추상화한 상태로 두고 사용자가 Exposed/Spring Data/직접
작성한 영속화 훅을 제공하게 한다. JaVers는 기본 애그리거트 저장소가 아니라
커밋/이력/shadow를 불러오는 데 사용한다.

## 결과

애그리거트 계약, 이벤트 속성 매핑, `AggregateRepository`,
no-op/function/composite 게시자, 선택형 Spring/Kafka/NATS 어댑터를 제공하는
`javers-ddd`를 추가했다. 테스트는 이벤트 매핑, 게시자 디스패치, H2와
`ExposedCdoSnapshotRepository`를 사용한 애그리거트 저장/조회/이력을
검증한다.

## 검증

- `./gradlew :javers-ddd:compileTestKotlin --no-configuration-cache --no-build-cache --no-parallel --console=plain`
- `./gradlew :javers-ddd:cleanTest :javers-ddd:test --no-configuration-cache --no-build-cache --no-parallel --console=plain`
- `actionlint`
- `git diff --check`

## 후속 작업

JaVers 애그리거트 타입에는 애그리거트 ID 속성의 `@Id`와
`registerEntity(...)` 같은 명시적인 엔티티 ID 매핑이 필요하다. 예제와
Phase 4 CQRS/Event Sourcing 데모에서 이 요구사항이 분명하게 드러나도록 한다.
