# 검토 - Issue 210 Lettuce Repository Lifecycle

일자: 2026-06-26
범위: `LettuceCdoSnapshotRepository`, Redis persistence tests, Spring Boot auto-configuration tests.

## 결과

남은 P0/P1 finding은 없다.

## 검토 기록 요약

- Ownership boundary가 명시적이다. caller는 `RedisClient` ownership을 유지하고,
  repository는 해당 client에서 자신이 연 connection만 소유한다.
- Repository는 더 이상 shared `LettuceClients` cached connection을 닫지 않아
  cross-consumer shutdown side effect를 피한다.
- `close()`는 idempotent하며 initialized lazy connection만 닫는다.
- Spring Boot cleanup은 bean의 `AutoCloseable` contract에 의존하며
  `ApplicationContextRunner` shutdown test로 커버된다.

## 검증 증거

- CodeGraph `detect_changes_tool` on `HEAD` reported changed files:
  `LettuceCdoSnapshotRepository.kt`, `LettuceJaversCommitTest.kt`,
  `JaversAutoConfigurationTest.kt`; test gaps: 0.
- `git diff --check` - PASS.
- `./gradlew :javers-persistence-redis:test --tests '*LettuceJaversCommitTest*' :javers-spring-boot4-autoconfigure:test --tests '*JaversAutoConfigurationTest*' --no-configuration-cache --no-build-cache --no-parallel --console=plain` - PASS.
- `./gradlew :javers-persistence-redis:test :javers-spring-boot4-autoconfigure:test --no-configuration-cache --no-build-cache --no-parallel --console=plain` - PASS.

## 잔여 위험

Gradle은 repository의 기존 generic Gradle 10 deprecation summary를 emit했다.
targeted tail output에서 issue-specific warning은 관찰되지 않았다.
