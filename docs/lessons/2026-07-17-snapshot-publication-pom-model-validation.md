# Snapshot Publication POM Model Validation

## Context

The generated `javers-exposed` POM imported `exposed-bom` without a version.
Maven consequently rejected both that dependency-management entry and the
versionless `exposed-dao` dependency.

## Decision

Use `bt4k.exposed.bom` for Exposed platform imports and validate every
publication POM structurally plus through Maven effective-model construction
before CI, snapshot publishing, and release publishing.

## Outcome

All seven generated publication POMs now resolve their managed dependencies
with valid Maven models.

## Verification

- `ruby scripts/publication/publication_pom_audit_test.rb`
- `./gradlew generatePomFileForBluetapeJaversPublication -PsnapshotVersion=-SNAPSHOT --no-daemon --no-configuration-cache --no-build-cache`
- `ruby scripts/publication/validate_poms.rb`

## Future Guidance

A Gradle build succeeding does not prove that its generated Maven POM is
consumable. Treat generated POM validation as a publish prerequisite.
