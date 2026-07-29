# Issue #88 — javers-exposed-ddd Command-Side Example 계획

일자: 2026-05-26
Issue: https://github.com/bluetape4k/bluetape4k-javers/issues/88
Spec: `docs/superpowers/specs/2026-05-26-issue-88-exposed-ddd-command-design.md`

## 작업 유형

Type A — Full Design. 새 example module, CI/Nightly coverage, user-facing README
update를 추가한다.

## 작업

1. Module registration
   - `settings.gradle.kts`에 `javers-exposed-ddd`를 추가한다.
   - `javers-exposed-ddd/build.gradle.kts`를 추가한다.
   - tests에는 H2를 사용하고 Kafka/Redis dependency는 #89로 미룬다.

2. Command-side domain model
   - order ID/customer ID/item/status value type을 추가한다.
   - JaVers `@Id`가 있는 `Order` aggregate를 추가한다.
   - command 및 domain event type을 추가한다.

3. Exposed persistence
   - `OrdersTable`을 추가한다.
   - Exposed JDBC backed repository implementation을 추가한다.
   - 이 첫 slice에서는 line item에 JSON을 사용한다.

4. Command handler
   - `OrderCommandHandler`를 추가한다.
   - `AggregateRepository`를 통해 aggregate state를 저장한다.
   - command마다 domain event 하나를 publish한다.

5. Tests
   - H2-backed test fixture가 order 및 JaVers table을 만든다.
   - persisted aggregate, JaVers snapshot history, commit properties, published events를 검증한다.

6. 문서 및 workflow coverage
   - module README.md와 README.ko.md를 추가한다.
   - root README.md와 README.ko.md를 갱신한다.
   - WIP.md를 갱신하고 lesson file을 추가한다.
   - example module용 CI 및 Nightly path filters/jobs를 갱신한다.

7. 검증
   - `./gradlew :javers-exposed-ddd:test --no-configuration-cache --no-build-cache --no-parallel --console=plain`
   - `./gradlew build -x test --no-configuration-cache --no-build-cache --no-parallel --console=plain`
   - `actionlint`
   - `git diff --check`
   - GitHub PR checks after PR creation.

## PR 경계

이 PR은 #88만 닫고 #5를 parent로 reference한다. #5, #89, #90은 닫으면 안 된다.

## 열린 제약

- Local/native 7-tier review가 required review gate다. historical external CLI tool outage는 past evidence gap을 설명할 때만 적는다.
- #88에서는 의도적으로 Testcontainers를 사용하지 않는다. #89가 Kafka/Redis integration tests를 추가하며, 해당 tests는 serially 실행해야 한다.
