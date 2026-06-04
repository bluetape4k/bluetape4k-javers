# Issue 141 Ktor JaVers Example

## Context

Issue #141 needed a current-feature Ktor example, not a new JaVers repository capability. The example had to reuse `javers-ddd` and `javers-exposed`, stay non-Spring, and keep the project name under the `:examples-javers-*` prefix.

## Decision

Create `:examples-javers-ktor` as a small executable Ktor REST application backed by in-memory H2, Exposed JDBC, and `ExposedCdoSnapshotRepository`. Reuse bluetape4k Ktor core/testing helpers for health/readiness and integration tests.

## Outcome

The example demonstrates placing an order, marking it paid, reading command-side state, and reading bounded JaVers snapshot history. CI and Nightly now include the module, and README locale files document the Ktor/JDBC blocking boundary.

## Verification

- `./gradlew :examples-javers-ktor:compileKotlin :examples-javers-ktor:compileTestKotlin :examples-javers-ktor:test :examples-javers-ktor:koverXmlReport --no-configuration-cache --no-build-cache --no-parallel --console=plain`
- `./gradlew projects build -x test --no-configuration-cache --no-build-cache --no-parallel --console=plain`
- `actionlint .github/workflows/ci.yml .github/workflows/nightly-tests.yml`
- `git diff --check`

## Future Guidance

For future JaVers examples, prefer a narrow app-local domain over shared example-domain indirection until duplication becomes a maintenance problem. Keep example project names under `:examples-javers-*` so publishing exclusion stays simple.
