# 2026-05-25 — JaVers Redis head 재빌드 복원

## 배경

Milestone 0.1.3의 issues #62와 #76에서 영속 JaVers 저장소가 스냅샷과 커밋
시퀀스 메타데이터를 유지하더라도 재빌드된 저장소의 `getHeadId()`가 `null`을
반환할 수 있다는 문제가 드러났다.

## 결정

공유 스냅샷 저장소 추상화에 지연 실행되는 `loadHeadId()` 훅을 추가하고
Redis 기반 저장소에서만 재정의한다. Lettuce는 Redis 시퀀스 해시에서 최신
헤드를 복원한다. Redisson은 문자열 맵 키와 long 맵 값을 처리하는 복합 코덱을
사용한 뒤 최신 시퀀스 항목을 복원한다.

Kafka는 쓰기 전용으로 유지한다. 읽기 측 감사 상태를 복원할 수 있는 것처럼
처리하지 않고, 재빌드를 지원하지 않는 계약을 테스트로 명시한다.

## 결과

이제 Redis 저장소를 재빌드해도 최신 헤드 커밋 ID가 유지된다. 새 테스트는
Lettuce와 Redisson의 복원 동작, Kafka의 쓰기 전용 부정 계약을 검증한다.

## 검증

- `./gradlew :javers-core:compileKotlin :javers-persistence-redis:compileKotlin :javers-persistence-redis:compileTestKotlin :javers-persistence-kafka:compileTestKotlin --no-build-cache --no-parallel --console=plain`
- `./gradlew :javers-persistence-redis:cleanTest :javers-persistence-redis:test :javers-persistence-kafka:cleanTest :javers-persistence-kafka:test --no-build-cache --no-parallel --console=plain`
- 과거 외부 CLI 리뷰 산출물에는 `P0=0`, `P1=0`, `APPROVE`가 기록되어 있다.
  현재 작업에서는 로컬/네이티브 7-tier 리뷰를 필수 게이트로 사용해야 한다.

## 향후 준수 사항

저장소가 시퀀스 메타데이터를 영속화한다면 기존 데이터를 대상으로 새 저장소
인스턴스를 만드는 재빌드 테스트를 추가하고 `getHeadId()`를 검증해야 한다.
이 검증을 통과하기 전에는 재시작 안전성을 보장한다고 판단하지 않는다.
