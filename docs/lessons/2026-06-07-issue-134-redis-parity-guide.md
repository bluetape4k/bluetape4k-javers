# Issue 134 Redis 동등성 지침

## 배경

#134의 Redisson near-cache 및 복합 저장소 작업에 앞서 Redis provider 간 동등성이
필요했다. 기존 모듈에는 이미 Lettuce 및 Redisson 저장소, provider별
commit/shadow 테스트, codec 계약이 있었다.

## 결정

`bluetape4k-javers`에 provider-neutral 캐시 추상화를 추가하는 대신 테스트
코드에 공용 Redis 동등성 계약을 추가한다. near-cache, read-through,
write-through, write-behind 동작은 기존 bluetape4k cache 및 Exposed cache
모듈에 계속 위임한다.

## 결과

이제 공용 계약은 Lettuce와 Redisson 모두에 대해 역시간순 스냅샷, head 재구축,
실패 전파를 확인한다. Redis 데이터베이스 전체를 비우는 대신 짧은 Base58
접미사로 끝나는 고유 저장소 prefix를 사용해 테스트 데이터를 격리한다. 모듈의
README 쌍은 각 provider를 선택해야 하는 경우를 설명한다.

## 검증

- `./gradlew :javers-persistence-redis:compileTestKotlin --no-configuration-cache --no-build-cache --no-parallel --console=plain`
- `./gradlew :javers-persistence-redis:test --no-configuration-cache --no-build-cache --no-parallel --console=plain` (`74 tests`)
- `git diff --check`

## 향후 보호 규칙

JaVers 전용 Redis 캐시 계층을 추가하기 전에 먼저
`bluetape4k-projects/cache`와 `bluetape4k-exposed` 캐시 계약을 재사용하거나
조정한다. 스냅샷 정렬, commit metadata, 쿼리 동작, 저장소 head 의미론에
대해서만 JaVers 전용 테스트를 추가한다. 공유 상태를 지우지 않고 테스트를
격리할 수 있다면 `flushdb()`보다 고유한 Redis key prefix를 우선한다.
