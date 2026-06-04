# 2026-06-04 Issue 158 Nightly Config Cache And Catalog

## Context

Nightly and CI compile-only workflows use snapshot and BOM-managed dependencies, so stale Gradle/configuration state can surface versionless dependency coordinates or amplify transient snapshot metadata failures.

## Decision

Keep Nightly and CI refresh/compile Gradle commands on `--no-configuration-cache` and keep local bluetape4k aliases versioned through their repo-defined version key.

## Outcome

Nightly and CI compile-only commands no longer rely on configuration cache during dependency refresh, and repo-local catalog aliases avoid `group:artifact:.` coordinates.

## Verification

- `actionlint .github/workflows/ci.yml .github/workflows/nightly-tests.yml`: pass.
- `git diff --check`: pass.
- Version-ref audit: no missing `version.ref` keys.
- `./gradlew build -x test --parallel --no-configuration-cache --no-daemon`: pass.

## Future Rule

For Nightly or CI jobs that refresh/compile snapshot dependencies, disable both Gradle action cache and configuration cache unless a repo-specific proof says otherwise.
