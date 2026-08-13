# Lesson — issue #323 Redis 매뉴얼의 릴리스 출처와 현재 계약 경계

**관련 이슈**: [#323](https://github.com/bluetape4k/bluetape4k-javers/issues/323)
**관련 구현**: [#306](https://github.com/bluetape4k/bluetape4k-javers/issues/306), [#314](https://github.com/bluetape4k/bluetape4k-javers/pull/314)
**영향 문서**: `docs/manual/en/**`, `docs/manual/ko/**`
**작성일**: 2026-08-13

## 배경

`docs/manual/manifest.yaml`은 매뉴얼을 `0.3.0` release commit
`978d0490fc438570e7520643aed50e20614772d1`에 고정한다. 이 release의
`LettuceCdoSnapshotRepository.close()`는 초기화된 connection만 닫고 `closed`를
나중에 설정하므로, 아직 초기화하지 않은 lazy connection은 `close()` 뒤 operation에서
다시 열릴 수 있다.

현재 `develop`의 #306 수정은 `closed`를 먼저 고정하고 이후 read/write operation을
`IllegalStateException`으로 거부하는 terminal lifecycle 계약을 추가했다. 미커밋 EN/KO
module manual 변경을 그대로 반영하면 release source가 설명하는 동작과 현재 develop
동작을 한 문서에서 혼합하게 된다. 또한 Lettuce quick start가 호출자가 소유한
`RedisClient`만 종료해 repository connection의 자원 소유권 예제가 불완전했다.

## 결정

- release manual에는 고정된 `0.3.0` source의 비-terminal 동작을 명시하고, terminal
  계약이 `0.3.0` 이후 수정됐다는 경계를 함께 기록한다.
- Lettuce quick start는 `repository.close()`를 `redisClient.shutdown()`보다 먼저
  호출해 자원 소유권 순서를 실행 가능한 예제로 만든다.
- EN/KO 문서는 같은 버전 경계·예제 구조·기술 토큰을 유지한다.
- 현재 `0.4.0` develop 계약은 module README와 현재 source에서 검증하고,
  release manual의 pinned source link를 `develop`로 바꾸지 않는다.

## 검증 경계

문서 변경 자체는 프로덕션 동작을 바꾸지 않지만, 설명하는 현재 Redis adapter
계약은 실제 Redis 경계를 갖는다. 따라서 `bluetape4k-testcontainers`의 Redis launcher를
사용한 `:javers-persistence-redis:test`를 생략하지 않는다. 구현 #306에서 이미
`RedisServer.Launcher.LettuceLib` 기반 회귀를 통과했고, 이번 PR에서도 같은 모듈 테스트를
격리 worktree에서 순차 재검증한다.

## 재사용 규칙

release manual의 source link가 immutable commit을 가리키면, 현재 `develop` 계약을
그대로 복사하지 않는다. release source와 current source의 차이를 먼저 확인하고, 다음
중 하나를 선택한다.

1. release 문서를 유지하면서 버전 경계를 명시한다.
2. 새 release manual train을 별도 범위로 갱신하고 manifest, links, locale parity를
   함께 검증한다.

어느 경로든 `git diff --check`, release/manual contract, EN/KO 로케일 정합성을 같은
HEAD에서 확인한다.
