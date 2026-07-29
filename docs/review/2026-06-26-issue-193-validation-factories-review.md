# Issue 193 검토 기록

## 범위

Issue에 나열된 validation factory alignment target을 검토했다.

- `javers-exposed/.../JaversExposedTables.kt`
- `javers-core/.../CdoSnapshotEvent.kt`
- `examples/javers-exposed-ddd/.../domain/Order.kt`
- `examples/javers-spring-boot4/.../domain/Order.kt`
- `examples/javers-ktor/.../domain/Order.kt`

## 결과

최종 diff에는 P0/P1 finding이 없다.

## 증거

- 영향받은 table-name 및 order aggregate data class는 이제 private constructor와
  companion factory를 사용한다.
- example order factory는 direct regression test에서 empty items, non-positive
  quantities, non-positive unit prices를 reject한다.
- `CdoSnapshotEventMetadata` numeric validation은 이제 `snapshotVersion`과
  `repositorySequence`에 bluetape4k validation helper를 사용한다.
- Targeted rerun이 통과했다.
  `./gradlew :javers-core:test :javers-exposed:test :examples-javers-exposed-ddd:test :examples-javers-ktor:test :examples-javers-spring-boot4:test --rerun-tasks --no-configuration-cache --no-build-cache --no-parallel --console=plain`

## 잔여 위험

Kotlin visibility contract는 compile-time에 enforce된다. Regression test는 public
factory behavior를 커버하지만, 외부 `copy()` 호출 시도는 compile failure가
되므로 표현하지 않는다.
