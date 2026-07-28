# Issue #133 - Redis + Exposed 지연 시간 전략

## 배경

Issue #133은 #134의 Redis 동등성 지침이 반영된 뒤 Exposed 기반 JaVers
스냅샷을 위한 Redisson near-cache 지연 시간 전략을 요구했다.

## 결정

JaVers 고유의 동작 공백이 입증되지 않는 한 새로운 JaVers 캐시 추상화를
추가하지 않는다. `javers-exposed`는 영속 SQL audit 저장소로,
`javers-persistence-redis`는 직접 Redis audit 저장소로 유지하고, read model과
projection에는 `bluetape4k-exposed` 캐시 모듈을 재사용한다.

## 결과

이제 README 언어 쌍에는 안전한 캐시 대상, 안전하지 않은 canonical audit 대상,
cache-aside, read-through, write-through, write-behind, near-cache 전략
매트릭스가 문서화되어 있다.

## 검증

- 대상 모듈 테스트 통과:
  `./gradlew :javers-persistence-redis:test :javers-exposed:test --no-configuration-cache --no-build-cache --no-parallel --console=plain`
- `:javers-persistence-redis:test`: 테스트 74개 실행.
- `:javers-exposed:test`: 테스트 53개 실행.
- `git diff --check`: 통과.

## 향후 지침

향후 이슈에서 복합 영속 이력과 event/projection 동작을 구현한다면 invalidation,
replay, drain 실패 처리, 저장소 head/commit sequence 의미론을 명시적으로
책임져야 한다. canonical JaVers audit 쓰기를 기본적으로 write-behind 캐시
동작을 통해 처리하지 않는다.
