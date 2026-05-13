# Changelog

All notable changes to `bluetape4k-javers` are documented here.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).
This project follows [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- Root README hero image plus refreshed purpose, feature, and Mermaid architecture documentation.
- GitHub Actions workflows for CI, nightly, snapshot, release, and code-quality checks ([PR #2](https://github.com/bluetape4k/bluetape4k-javers/pull/2)).
- `bluetape4k-javers-bom` BOM module for JaVers library consumers ([PR #10](https://github.com/bluetape4k/bluetape4k-javers/pull/10)).
- English and Korean README files for the JaVers BOM module ([PR #11](https://github.com/bluetape4k/bluetape4k-javers/pull/11)).
- JaVers implementation backlog captured in repository docs ([PR #12](https://github.com/bluetape4k/bluetape4k-javers/pull/12)).

### Changed

- Updated WIP snapshot from current assigned GitHub issues and refreshed agent guidance.
- Dependency governance, compatibility guard, Nightly lane, and Kover policy maintenance landed through PR #14 through PR #24.
- CI uses path filtering and retry configuration ([PR #8](https://github.com/bluetape4k/bluetape4k-javers/pull/8)).
- Test code migrated from Kluent to `bluetape4k-assertions` via `bluetape4k-junit5` ([PR #9](https://github.com/bluetape4k/bluetape4k-javers/pull/9)).
