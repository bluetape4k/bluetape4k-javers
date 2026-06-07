# Issue 106 Local Review - Exposed Schema Options

## Scope

- Issue: #106, `feat: make Exposed schema mapping configurable`
- Branch: `feat/issue-106-exposed-schema-options`
- Module: `javers-exposed`
- Changed surface:
  - `ExposedCdoSnapshotRepositoryOptions`
  - `ExposedJaversTableNames`
  - `ExposedJaversSchema`
  - configurable `CommitTableMapping` / `CdoSnapshotTableMapping`

## Review Result

- P0: 0
- P1: 0
- Gate: PASS

## 7-Tier Checks

| Tier | Result | Evidence |
|---|---:|---|
| API compatibility | PASS | Existing constructor call shape remains source-compatible; `database` and `codec` defaults are unchanged and `options` is appended. |
| Persistence semantics | PASS | Repository still writes commit metadata and snapshot rows through one Exposed transaction path, now using instance-local table mappings. |
| Schema ownership | PASS | `createSchemaOnEnsure=false` is covered by H2 test: `ensureSchema()` is no-op, external `SchemaUtils.create(*schema.tables)` enables writes. |
| Dialect coverage | PASS | H2, PostgreSQL, and MySQL v8 matrix covers custom table names. |
| Index safety | PASS | Custom schemas derive index names from custom table names to avoid same-database index/constraint collisions. |
| Documentation | PASS | `javers-exposed/README.md` and `README.ko.md` document custom table names and external migration ownership. |
| Tooling fallback | PASS | CodeGraph was checked but stale (`last_updated=2026-05-17`); direct source/test inspection and Gradle verification were used as fallback. IntelliJ diagnostics MCP was unavailable in this session. |

## Validation Evidence

- `./gradlew :javers-exposed:compileKotlin :javers-exposed:compileTestKotlin --no-configuration-cache --no-build-cache --console=plain` - PASS
- `./gradlew :javers-exposed:test --tests 'io.bluetape4k.javers.persistence.exposed.repository.ExposedCdoSnapshotRepositoryH2Test' --no-configuration-cache --no-build-cache --console=plain` - PASS, 8 tests
- `./gradlew :javers-exposed:test --tests 'io.bluetape4k.javers.persistence.exposed.repository.ExposedCdoSnapshotRepositoryDatabaseSmokeTest' --no-configuration-cache --no-build-cache --console=plain` - PASS, 21 tests
- `./gradlew :javers-exposed:test --no-configuration-cache --no-build-cache --console=plain` - PASS, 31 tests

## Residual Risk

- Custom schema/catalog placement is still delegated to Exposed or external migration tooling; this change controls table names and schema creation ownership, not database-specific catalog DDL.
