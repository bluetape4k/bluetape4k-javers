# Issue #178 - Kafka Key Redaction Review

## 범위

- `javers-persistence-kafka/src/main/kotlin/io/bluetape4k/javers/persistence/kafka/repository/KafkaSnapshotKeyDiagnostics.kt`
- `javers-persistence-kafka/src/main/kotlin/io/bluetape4k/javers/persistence/kafka/repository/KafkaCdoSnapshotRepository.kt`
- `javers-persistence-kafka/src/main/kotlin/io/bluetape4k/javers/persistence/kafka/repository/KafkaSnapshotEventPublisher.kt`
- `javers-persistence-kafka/src/main/kotlin/io/bluetape4k/javers/persistence/kafka/repository/VanillaKafkaCdoSnapshotRepository.kt`
- `javers-persistence-kafka/src/main/kotlin/io/bluetape4k/javers/persistence/kafka/repository/VanillaKafkaSnapshotEventPublisher.kt`
- `javers-persistence-kafka/src/test/kotlin/io/bluetape4k/javers/persistence/kafka/repository/*Kafka*CdoSnapshotRepositoryTest.kt`
- `javers-persistence-kafka/src/test/kotlin/io/bluetape4k/javers/persistence/kafka/repository/KafkaSnapshotKeyDiagnosticsTest.kt`
- `javers-persistence-kafka/README.md`
- `javers-persistence-kafka/README.ko.md`

## 7-Tier Lite 검토 결과

| Tier | 관점 | 증거 | 판정 |
|---|---|---|---|
| 1 Security/privacy | Raw Kafka key diagnostics | log와 exception message는 이제 `keyFingerprint`와 `keyLength`를 사용한다. raw key, prefix, suffix, masked variant는 emit하지 않는다. | P0=0, P1=0, P2=0, P3=0 |
| 2 Correctness/transport | Record key preservation | Kafka `sendDefault(key, payload)`와 vanilla `ProducerRecord(topic, key, payload)` call은 여전히 original key value를 사용한다. | P0=0, P1=0, P2=0, P3=0 |
| 3 Operations/SRE | Troubleshooting utility | Stable SHA-256 prefix는 raw natural id를 드러내지 않고 pseudonymous correlation을 지원한다. README는 이것이 anonymization이 아니라고 명시한다. length는 malformed-key diagnosis를 돕는다. | P0=0, P1=0, P2=0, P3=0 |
| 4 Kotlin/API quality | Helper boundary | Formatter는 `internal`, small, dependency-light이며 bluetape4k core hex encoding을 재사용한다. Public API는 추가하지 않았다. | P0=0, P1=0, P2=0, P3=0 |
| 5 Tests/silent failure | Regression coverage | 테스트는 deterministic formatter output, Spring log redaction, Spring failure/interruption message, vanilla log redaction, vanilla failure/timeout/interruption message, 기존 테스트를 통한 record key preservation을 커버한다. | P0=0, P1=0, P2=0, P3=0 |
| 6 Docs/user contract | README parity | English/Korean README file은 같은 allowed/forbidden diagnostic key surface를 문서화한다. | P0=0, P1=0, P2=0, P3=0 |
| 7 Workflow/evidence | Issue 및 validation | Issue #178 body는 구현 전에 갱신했다. local targeted tests와 diff whitespace check가 통과했다. | P0=0, P1=0, P2=0, P3=0 |

## 검증 증거

- `./gradlew :javers-persistence-kafka:test --no-configuration-cache --no-build-cache --no-parallel --console=plain`
  - PASS, 32 tests.
- `git diff --check`
  - PASS.
- `rg 'key=\\$key|key=\\[\\$key\\]' javers-persistence-kafka/src/main`
  - raw key interpolation은 남아 있지 않다.

## Gate 판정

P0=0, P1=0. staging에서 모든 new helper, test, lesson, review file 포함을 확인한 뒤 PR creation이 가능하다.
