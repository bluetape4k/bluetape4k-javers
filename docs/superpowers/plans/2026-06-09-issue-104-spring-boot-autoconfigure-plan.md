# Issue 104 Spring Boot 4 Auto-Configuration Plan

## 범위

기존 JaVers Exposed, Redis, Kafka repository backend용 Spring Boot 4 auto-configuration
module을 구현한다.

## 단계

1. `javers-spring-boot4-autoconfigure`를 만들고 Gradle에 등록한다.
2. configuration properties와 backend-specific auto-configuration phase를 추가한다.
3. backend selection, backoff, optional-class absence에 대한 `ApplicationContextRunner` tests를 추가한다.
4. module README files를 추가하고 root README locale set, CHANGELOG, repo `AGENTS.md`를 갱신한다.
5. CI 및 Nightly test/coverage workflows에 module을 등록한다.
6. Gradle compile/test/Kover, `actionlint`, `git diff --check`, 7-Tier review로 검증한다.
7. `develop` 대상 PR을 만든다. explicit user approval 이후에만 merge한다.

## 검증 명령

```bash
./gradlew projects --no-configuration-cache --no-build-cache --console=plain
./gradlew :javers-spring-boot4-autoconfigure:compileKotlin :javers-spring-boot4-autoconfigure:compileTestKotlin :javers-spring-boot4-autoconfigure:test :javers-spring-boot4-autoconfigure:koverXmlReport --no-configuration-cache --no-build-cache --no-parallel --console=plain
./gradlew :javers-core:compileKotlin :javers-exposed:compileKotlin :javers-persistence-redis:compileKotlin :javers-persistence-kafka:compileKotlin --no-configuration-cache --no-build-cache --no-parallel --console=plain
./gradlew build -x test --no-configuration-cache --no-build-cache --no-parallel --console=plain
actionlint .github/workflows/ci.yml .github/workflows/nightly-tests.yml
git diff --check
```

## 계획 검토

| Tier | 결과 | 비고 |
|---|---|---|
| Security | PASS | credential creation 또는 secret handling은 계획하지 않았다. |
| Ops/SRE | PASS | conditional startup과 caller-owned client가 예상치 못한 side effect를 줄인다. |
| Structural | PASS | 별도 module로 기존 repository module 확장을 피한다. |
| Kotlin | PASS | Spring Boot 4 slice auto-configuration convention을 사용한다. |
| Tests | PASS | tests는 지원되는 모든 backend branch와 backoff behavior를 target한다. |
| Performance | PASS | background polling, retry, thread ownership은 추가하지 않는다. |
| Docs/Release | PASS | module registration과 README locale update가 explicit plan item이다. |

Final gate: `P0 = 0`, `P1 = 0`.
