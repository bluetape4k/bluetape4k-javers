# Issue 4 javers-ddd 계획

## 작업 유형

Type A Full Design: 새 Gradle module, public API, optional integration adapter,
tests, multilingual README, CI/Nightly wiring, WIP update, PR.

## 범위

변경 대상:

- `settings.gradle.kts`
- `gradle/libs.versions.toml`
- `javers-ddd/**`
- `README.md`, `README.ko.md`
- `bom/README.md`, `bom/README.ko.md`, `bom/build.gradle.kts`
- `.github/workflows/ci.yml`
- `.github/workflows/nightly-tests.yml`
- `WIP.md`
- `docs/lessons/2026-05-26-issue-4-javers-ddd.md`

## 단계

1. `javers-ddd` module을 등록한다.
2. module dependency를 추가한다.
   - `api(project(":javers-core"))`
   - 모든 DDD helper consumer에 Exposed를 transitive runtime dependency로 강제하지 않으면서 Phase 2 integration을 검증하도록 `testImplementation(project(":javers-exposed"))`를 추가한다.
   - Spring/Kafka adapter API용 `compileOnly(libs.spring.kafka)`.
   - NATS adapter API용 `compileOnly(libs.bluetape4k.nats)`.
   - H2, MockK, bluetape4k assertions, Exposed test dependency.
3. public API를 구현한다.
   - `AggregateRoot`
   - `DomainEvent`
   - `toJaversProperties`
   - `DomainEventPublisher`
   - `NoopDomainEventPublisher`
   - `FunctionDomainEventPublisher`
   - `CompositeDomainEventPublisher`
   - `AggregateRepository`
   - Spring/Kafka/NATS publishers.
4. tests를 추가한다.
   - event property mapping,
   - publisher dispatch behavior,
   - H2와 `ExposedCdoSnapshotRepository`를 사용한 aggregate save/load/history.
5. Mermaid class diagram과 usage를 포함한 README 및 localized README를 추가한다.
6. root README와 BOM docs를 갱신한다.
7. CI/Nightly path filter, job, coverage artifact, status needs를 연결한다.
8. `WIP.md`에서 #4를 completed/current로 표시하고 #5를 next로 남긴다.
9. lesson entry를 추가한다.
10. 검증한다.
    - `./gradlew :javers-ddd:compileTestKotlin --no-configuration-cache --no-build-cache --no-parallel --console=plain`
    - `./gradlew :javers-ddd:cleanTest :javers-ddd:test --no-configuration-cache --no-build-cache --no-parallel --console=plain`
    - `./gradlew build -x test --no-configuration-cache --no-build-cache --no-parallel --console=plain`
    - `actionlint`
    - `git diff --check`
11. local/native 7-tier review를 실행하고 PR 생성 전 P0/P1=0을 요구한다.
12. Lore trailer로 commit하고 push한 뒤 #4를 닫는 `develop` 대상 PR을 연다.

## 중단 조건

`Closes #4`가 포함된 PR이 열리고, validation evidence가 PR body에 기록되며, local
branch에 unstaged change가 없으면 중단한다.

## 알려진 Tradeoff

- issue의 sealed `DomainEvent` sketch는 의도적으로 interface로 바꾼다. library consumer가 이 module 밖에서 event type을 정의해야 하기 때문이다.
- Publisher adapter는 durable outbox가 아니라 immediate delivery helper다.
