# Issue 140 Spring Boot 4 Example Lesson

## Context

Issue #140 added a Spring Boot 4 example for the current JaVers + Exposed +
DDD helper feature set.

## Decision

- Use `:examples-javers-*` Gradle project names for example modules so future
  publishing logic can exclude examples by project-name prefix.
- Keep example file paths under `examples/` and preserve the existing
  directory-based exclusion as a fallback.
- Use explicit Spring Boot 4 wiring instead of adding auto-configuration.
- Use Boot 4 MVC test support through
  `org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc`.
- Use Jackson 3 `tools.jackson.module:jackson-module-kotlin`, not Jackson 2
  `com.fasterxml.jackson.module:jackson-module-kotlin`.

## Outcome

- Added `examples/javers-spring-boot4`.
- Renamed the existing example Gradle project to `:examples-javers-exposed-ddd`.
- Added CI/Nightly jobs and coverage artifacts for the new Spring Boot 4 example.
- Added 7-Tier spec, plan, and code-review artifacts with final `P0 = 0`,
  `P1 = 0`.

## Verification

- `./gradlew :examples-javers-spring-boot4:compileKotlin :examples-javers-spring-boot4:compileTestKotlin :examples-javers-spring-boot4:test --no-configuration-cache --no-build-cache --no-parallel --console=plain`
- `./gradlew projects --no-configuration-cache --no-build-cache --console=plain`
- `./gradlew :examples-javers-exposed-ddd:test :examples-javers-spring-boot4:test --no-configuration-cache --no-build-cache --no-parallel --console=plain`
- `./gradlew :examples-javers-spring-boot4:koverXmlReport --no-configuration-cache --no-build-cache --no-parallel --console=plain`
- `actionlint .github/workflows/ci.yml .github/workflows/nightly-tests.yml`
- `rg -n -F "\\'" .github/workflows`
- `git diff --check`

## Future Agents

- For new example modules in this repo, use project names matching
  `:examples-javers-*`.
- When using Spring Boot 4 MVC tests, prefer the Boot 4 `webmvc.test`
  auto-config package and add `spring-boot-starter-webmvc-test`.
- Keep benchmark metric files stable unless the task explicitly re-baselines
  benchmark numbers.
