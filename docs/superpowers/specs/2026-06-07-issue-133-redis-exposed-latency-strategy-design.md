# Issue #133 - Redis + Exposed Latency Strategy 설계

## 목표

`bluetape4k-javers`에 새 cache abstraction을 추가하지 않고 JaVers snapshot용 safe Redis + Exposed latency strategy를 정의한다.

이 설계는 사용자가 다음을 언제 사용할지 결정하도록 도와야 한다.

- Durable JaVers audit source of truth로 `javers-exposed`.
- Direct Redis JaVers snapshot store로 `javers-persistence-redis`.
- Audit history에서 파생된 query-side read model 및 projection용 `bluetape4k-exposed` cache module.

## 근거

- GitHub issue #133: Redisson near-cache latency strategy for Exposed-backed
  snapshots.
- 현재 base: `develop`의 `4649eb8 test: isolate Redis parity keys`.
- Issue #134는 merge됐고 `javers-persistence-redis`의 Lettuce/Redisson repository selection을 문서화한다.
- 기존 research: `docs/research/2026-06-04-javers-multilayer-cache-pipeline.md`.
- `bluetape4k-exposed`는 reusable cache contract를 이미 제공한다.
  - `exposed-cache`: `CacheMode`, `CacheWriteMode`, local cache config,
    resilience config, and repository test fixtures.
  - `exposed-jdbc-redisson`: read-through, write-through, write-behind, and
    near-cache variants backed by Redisson maps.
  - `exposed-jdbc-lettuce`: read-through, write-through, and write-behind
    variants backed by Lettuce.

## 범위

### Strategy 계약

- `javers-exposed`는 durable history가 필요한 Exposed application의 canonical SQL-backed JaVers audit store로 남는다.
- `javers-persistence-redis`는 direct Redis-backed JaVers snapshot repository로 남는다. Exposed-backed cache layer가 아니다.
- Exposed data용 Redis near-cache, read-through, write-through, write-behind behavior는 먼저 `bluetape4k-exposed` cache module을 재사용해야 한다.
- JaVers-specific implementation은 기존 Exposed cache repository가 표현할 수 없는 JaVers snapshot semantics에 behavior가 의존할 때만 허용한다.

### Safe Cache Targets

- JaVers history에서 파생된 application read model 또는 projection.
- 명시적 TTL, invalidation, projection replay behavior가 있는 rebuildable query result.
- 이미 `bluetape4k-exposed` cache contract에 맞는 Exposed entity 또는 DTO repository.

### Unsafe Cache Targets

- Canonical `CdoSnapshot` history row.
- JaVers commit sequence 또는 repository head metadata.
- Global-id/version uniqueness 및 newest-first snapshot order.
- Durable audit state가 commit되기 전에 write-behind가 acknowledge할 수 있는 audit write.

이 target들은 invalidation, replay, failure semantics를 소유하는 issue #131 또는 이후 composite repository design에서만 다시 검토할 수 있다.

## Strategy Matrix

| Strategy | JaVers + Exposed에서 사용 | 사용하지 않을 대상 | 비고 |
|---|---|---|---|
| Cache-aside | Rebuildable read model 또는 query result | Canonical audit snapshot write | Application code가 cache fill과 invalidation을 소유한다. |
| Read-through | Redisson 또는 Lettuce가 backing하는 Exposed read-model repository | Raw `CdoSnapshot` repository replacement | 기존 `bluetape4k-exposed` fixture와 contract를 우선한다. |
| Write-through | Synchronous Redis + database latency가 허용되는 mutable read model | Commit ordering을 보존해야 하는 JaVers audit write | Audit durability는 `javers-exposed`에 남는다. |
| Write-behind | Replay 또는 drain policy가 있는 non-authoritative projection | Audit log write, commit sequence, repository head metadata | 사용 전에 failure handling이 explicit해야 한다. |
| Near-cache | Redisson 또는 Lettuce local cache support가 있는 hot read-model lookup | Composite repository가 invalidation을 소유하지 않는 canonical snapshot/head state | Rebuildable data에는 TTL과 explicit invalidation을 사용한다. |

## Public 문서 요구사항

- `javers-exposed/README.md` 및 `javers-exposed/README.ko.md`에 strategy matrix와 explicit safe/unsafe target을 추가한다.
- Redis repository user가 direct Redis audit storage와 Exposed-backed near-cache strategy를 혼동하지 않도록 `javers-persistence-redis/README.md` 및 `README.ko.md`에 cross-reference를 추가한다.
- 이 issue가 새 production repository 또는 provider-neutral JaVers cache API를 추가하지 않음을 public docs에서 명확히 한다.

## 테스트 및 검증 요구사항

- 영향을 받는 두 persistence module에 기존 targeted test를 실행한다.
  - `./gradlew :javers-persistence-redis:test :javers-exposed:test --no-configuration-cache --no-build-cache --no-parallel --console=plain`
- Generic read-through, write-through, write-behind, near-cache behavior는 기존 `bluetape4k-exposed` cache fixture coverage에 의존한다.
- 새 JaVers-specific mapping code가 도입되지 않는 한 이 repository에서 generic cache behavior test를 중복하지 않는다.
- `git diff --check`를 실행한다.

## 제외 목표

- New production repository classes.
- New Gradle dependencies.
- Spring Boot auto-configuration.
- Composite durable history plus event stream repository. 이는 issue #131에 남긴다.
- Kafka snapshot event pipeline work. 이는 issue #135 및 #136에 남긴다.

## 위험 및 완화

- Risk: Cache misuse가 audit ordering bug를 숨길 수 있다. Mitigation: canonical snapshot 및 head metadata를 unsafe cache target으로 표시한다.
- Risk: Write-behind가 durable state가 존재하기 전에 audit write를 acknowledge할 수 있다. Mitigation: 이 issue에서 JaVers audit write용 write-behind를 reject한다.
- Risk: Duplicate abstraction이 `bluetape4k-exposed`와 drift될 수 있다. Mitigation: 기존 cache contract와 fixture 재사용을 문서화한다.
- Risk: 문서만 있는 strategy가 implemented behavior로 과장될 수 있다. Mitigation: 새 production repository 또는 provider-neutral JaVers cache API가 추가되지 않는다고 명시한다.
