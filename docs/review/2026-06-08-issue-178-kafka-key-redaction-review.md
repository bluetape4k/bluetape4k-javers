# Issue #178 - Kafka Key Redaction Review

## Scope

- `javers-persistence-kafka/src/main/kotlin/io/bluetape4k/javers/persistence/kafka/repository/KafkaSnapshotKeyDiagnostics.kt`
- `javers-persistence-kafka/src/main/kotlin/io/bluetape4k/javers/persistence/kafka/repository/KafkaCdoSnapshotRepository.kt`
- `javers-persistence-kafka/src/main/kotlin/io/bluetape4k/javers/persistence/kafka/repository/KafkaSnapshotEventPublisher.kt`
- `javers-persistence-kafka/src/main/kotlin/io/bluetape4k/javers/persistence/kafka/repository/VanillaKafkaCdoSnapshotRepository.kt`
- `javers-persistence-kafka/src/main/kotlin/io/bluetape4k/javers/persistence/kafka/repository/VanillaKafkaSnapshotEventPublisher.kt`
- `javers-persistence-kafka/src/test/kotlin/io/bluetape4k/javers/persistence/kafka/repository/*Kafka*CdoSnapshotRepositoryTest.kt`
- `javers-persistence-kafka/src/test/kotlin/io/bluetape4k/javers/persistence/kafka/repository/KafkaSnapshotKeyDiagnosticsTest.kt`
- `javers-persistence-kafka/README.md`
- `javers-persistence-kafka/README.ko.md`

## 7-Tier Lite Findings

| Tier | Lens | Evidence | Verdict |
|---|---|---|---|
| 1 Security/privacy | Raw Kafka key diagnostics | Logs and exception messages now use `keyFingerprint` plus `keyLength`; raw key, prefix, suffix, and masked variants are not emitted. | P0=0, P1=0, P2=0, P3=0 |
| 2 Correctness/transport | Record key preservation | The Kafka `sendDefault(key, payload)` and vanilla `ProducerRecord(topic, key, payload)` calls still use the original key value. | P0=0, P1=0, P2=0, P3=0 |
| 3 Operations/SRE | Troubleshooting utility | Stable SHA-256 prefix supports pseudonymous correlation without revealing raw natural ids; README explicitly says this is not anonymization. Length helps malformed-key diagnosis. | P0=0, P1=0, P2=0, P3=0 |
| 4 Kotlin/API quality | Helper boundary | Formatter is `internal`, small, dependency-light, and reuses bluetape4k core hex encoding. No public API was added. | P0=0, P1=0, P2=0, P3=0 |
| 5 Tests/silent failure | Regression coverage | Tests cover deterministic formatter output, Spring log redaction, Spring failure/interruption messages, vanilla log redaction, vanilla failure/timeout/interruption messages, and record key preservation through existing tests. | P0=0, P1=0, P2=0, P3=0 |
| 6 Docs/user contract | README parity | English and Korean README files now document the same allowed and forbidden diagnostic key surfaces. | P0=0, P1=0, P2=0, P3=0 |
| 7 Workflow/evidence | Issue and validation | Issue #178 body was updated before implementation; local targeted tests and diff whitespace check passed. | P0=0, P1=0, P2=0, P3=0 |

## Validation Evidence

- `./gradlew :javers-persistence-kafka:test --no-configuration-cache --no-build-cache --no-parallel --console=plain`
  - PASS, 32 tests.
- `git diff --check`
  - PASS.
- `rg 'key=\\$key|key=\\[\\$key\\]' javers-persistence-kafka/src/main`
  - No raw key interpolation remains.

## Gate Verdict

P0=0, P1=0. The change is ready for PR creation after staging confirms all new helper, test, lesson, and review files are included.
