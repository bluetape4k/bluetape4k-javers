# Lesson — issue #310 BOM publishable module 목록과 EN/KO 문서 계약

## 배경

milestone `0.4.0`의 issue #310은 BOM이 실제로 관리하는 publishable module 목록과
EN/KO module manual이 어긋난 문제를 다룬다. `bom/build.gradle.kts`는 BOM 자신,
`examples/**`, `benchmark/**`를 제외한 subproject를 constraint에 넣는다.

생성된 publication POM을 확인한 결과 현재 constraint는 다음 여섯 개다.

- `javers-core`
- `javers-ddd`
- `javers-exposed`
- `javers-persistence-kafka`
- `javers-persistence-redis`
- `javers-spring-boot4-autoconfigure`

기존 EN/KO BOM manual은 다섯 개만 나열했고, `examples/javers-exposed-ddd`를
constraint로 만든다고 설명했다. 현재 구현은 example과 benchmark를 모두 제외하므로
이 설명은 release contract와 맞지 않았다.

## 결정

- generated BOM POM을 publishable module 목록의 기준으로 삼는다.
- EN/KO manual에 동일한 `BOM_PUBLISHED_MODULES` marker를 두고, 두 locale이 같은
  module set을 선언하도록 한다.
- `scripts/publication/bom_contract.rb`가 generated POM의 constraint, marker의
  EN/KO parity, `examples-*`·`benchmark-*` exclusion을 fail-closed로 검사한다.
- CI의 publication-POM 검증 단계에서 contract checker를 실행한다. module을 추가하거나
  제거하면 문서 marker를 함께 바꾸지 않는 한 CI가 실패한다.

## 검증

다음 검증은 격리 worktree에서 순서대로 실행했다.

```bash
ruby scripts/publication/bom_contract_test.rb
ruby scripts/publication/publication_pom_audit_test.rb
ruby scripts/publication/publication_pom_integration_test.rb
./gradlew generatePomFileForBluetapeJaversPublication \
  -PsnapshotVersion=-SNAPSHOT --no-daemon --no-configuration-cache --no-build-cache
ruby scripts/publication/bom_contract.rb \
  bom/build/publications/BluetapeJavers/pom-default.xml \
  docs/manual/en/modules/bluetape4k-javers-bom.md \
  docs/manual/ko/modules/bluetape4k-javers-bom.md
```

결과는 Ruby `4 + 5 + 2` tests 모두 통과, Gradle publication POM 생성 성공,
`bom-contract: failures=0`이다. Maven publication-POM 검증과 `git diff --check`도
pre-PR 단계에서 다시 실행한다.

## Testcontainers 판단

이 변경은 BOM metadata와 문서·검증 스크립트만 다루며 Redis, Kafka, DB, HTTP runtime
경계를 변경하지 않는다. 따라서 `bluetape4k-testcontainers` 통합 테스트는 계약을 증명할
대상이 없어 N/A다. 대신 실제 Gradle publication model이 만든 POM을 읽고, 두 locale의
consumer-facing module set을 비교한다. persistence adapter나 runtime wiring을 함께
변경하는 후속 작업에서는 해당 launcher를 순차 실행해야 한다.

## 다음 guard

새 publishable subproject를 등록하거나 기존 subproject를 example/benchmark로 이동할 때
generated BOM POM, EN/KO marker, `bom_contract.rb` 결과를 같은 HEAD에서 확인한다.
`:bluetape4k-javers-bom:dependencies` 출력만으로는 `java-platform` constraint가
충분히 드러나지 않을 수 있으므로 publication POM을 계약 증거로 사용한다.
