# Issue #133 - Redis + Exposed Latency Strategy Plan

## Lane

Full Feature / Type A이며 strategy documentation과 validation으로 제한된다. 이 issue는
cross-module behavior, Redis, Exposed, performance strategy를 건드리지만 current
evidence는 새 production repository를 정당화하지 않는다.

## 작업

1. Spec/plan gate
   - current merged evidence로 issue #133을 refresh한다.
   - 현재 `javers-exposed`, `javers-persistence-redis`, sibling `bluetape4k-exposed` cache contract를 확인한다.
   - design spec과 이 execution plan을 작성한다.
   - PR 전에 P0=0/P1=0인 local Step 2-R 및 Step 3-R review를 실행한다.

2. Public documentation
   - `javers-exposed/README.md`를 갱신한다.
   - `javers-exposed/README.ko.md`를 갱신한다.
   - `javers-persistence-redis/README.md`를 갱신한다.
   - `javers-persistence-redis/README.ko.md`를 갱신한다.
   - English/Korean locale parity를 유지한다.

3. Tests 및 validation
   - `./gradlew :javers-persistence-redis:test :javers-exposed:test --no-configuration-cache --no-build-cache --no-parallel --console=plain`을 실행한다.
   - `git diff --check`를 실행한다.
   - 새 JaVers-specific cache tests를 추가하지 않은 이유를 기록한다. 이 issue는 새 runtime behavior 또는 production cache mapping code를 도입하지 않는다.

4. 검토 및 lesson
   - `docs/review/2026-06-07-issue-133-redis-exposed-latency-strategy-review.md`를 추가한다.
   - `docs/lessons/2026-06-07-issue-133-redis-exposed-latency-strategy.md`를 추가한다.
   - Review는 P0/P1/P2/P3 count와 명시적 P0=0/P1=0 verdict를 포함해야 한다.

5. PR
   - Lore trailer로 commit한다.
   - branch를 push한다.
   - assignee `debop`, milestone `0.3.0`으로 #133을 해결하는 PR을 만든다.
   - PR body file을 사용하고 live PR body를 검증한다.
   - final PR body section은 `## DoD Status`여야 한다.

## 중단 조건

branch가 push되고, verified body가 있는 PR이 존재하며, local targeted validation이 통과하고,
local review가 P0=0 및 P1=0을 보고하면 중단한다. explicit user approval 없이 merge하지 않는다.

## 알려진 위험

- 사용자는 새 Redisson near-cache JaVers repository를 기대할 수 있다. README는 기존 Exposed cache module이 read model 및 projection용 supported reuse path라고 명시해야 한다.
- Write-behind를 canonical audit write에 안전한 것으로 제시하면 안 된다.
- Generic cache test coverage는 `bluetape4k-exposed`에 속한다. 이 repository는 JaVers-specific mapping code가 있을 때만 JaVers-specific tests를 추가해야 한다.
