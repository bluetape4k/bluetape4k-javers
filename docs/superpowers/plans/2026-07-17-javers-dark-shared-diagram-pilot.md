# JaVers Dark Shared Diagram Pilot Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Convert all 25 JaVers README technical diagrams to the approved dark visual system, stage 21 relevant canonical pairs for the bilingual 0.3 manual through deterministic mirrors, and prevent those develop-based assets from entering the stable 0.2 manual.

**Architecture:** The editable source remains `docs/images/readme-diagrams/<asset>.svg`; CairoSVG regenerates the canonical PNG. `docs/manual/shared-diagrams.yaml` records source anchors, diagram kind, target minor, and manual placement. A small Ruby contract copies `manual: selected` pairs into `docs/manual/assets/readme-diagrams` only when `targetMinor` equals the stable manual manifest's `stableMinor`, then verifies SHA-256 equality. Existing 0.2 manual-only diagrams remain until the 0.3 release source becomes authoritative.

## Version-boundary amendment

The implementation audit found that the stable manual is pinned to release `0.2.1`, while several canonical README diagrams describe `develop` behavior for `0.3.0`, including composite repositories, EntityHook auditing, Kafka projection, Spring/Ktor examples, and the shared Exposed audit transaction. Reusing those assets in the 0.2 manual would contradict its release provenance. The inventory therefore targets minor `0.3`; mirror generation and bilingual page integration stay inactive while `docs/manual/manifest.yaml` reports `stableMinor: '0.2'`.

**Tech Stack:** SVG, CairoSVG, Ruby 3 standard library (`yaml`, `digest`, `fileutils`, `minitest`), existing JaVers manual validators, `bluetape-diagram` audit scripts, Markdown.

---

## Repository and delivery boundary

- Target repository: `/Users/debop/work/bluetape4k/bluetape4k-javers`
- Base branch: `develop`
- Implementation branch: `docs/dark-shared-diagram-assets`
- PR target: `bluetape4k/bluetape4k-javers`, base `develop`, head `docs/dark-shared-diagram-assets`
- Merge is not authorized by this plan. Stop after exact-head CI and review evidence, report merge-ready, and obtain fresh approval.
- Production Kotlin behavior is out of scope. Gradle/JVM tests are N/A unless a repository hook or required CI runs them.

## File map

**Create**

- `docs/manual/shared-diagrams.yaml` — inventory of all 25 canonical diagrams, source anchors, diagram kinds, and 21 selected manual placements.
- `scripts/manual/shared_diagram_contract.rb` — parse and validate the inventory; calculate canonical and mirror paths and digests.
- `scripts/manual/shared_diagram_contract_test.rb` — RED/GREEN tests for missing pairs, unsafe paths, selection state, digest mismatch, and orphan mirrors.
- `scripts/manual/sync_shared_diagrams.rb` — `--write` copy and `--check` parity CLI.
- `docs/review/2026-07-17-javers-dark-shared-diagram-review.md` — per-asset falsifiable validation and full-size eye-review ledger.

**Modify**

- `docs/images/readme-diagrams/*.{svg,png}` — 25 canonical pairs, SVG edited and PNG regenerated one asset at a time.
- `docs/manual/manifest.yaml` — register the 42 selected mirror files and remove registrations for superseded manual-only pairs.
- `docs/manual/en/index.md`, `docs/manual/ko/index.md`
- `docs/manual/en/architecture/repository-map.md`, `docs/manual/ko/architecture/repository-map.md`
- `docs/manual/en/persistence/selection-guide.md`, `docs/manual/ko/persistence/selection-guide.md`
- `docs/manual/en/modules/bluetape4k-javers-bom.md`, `docs/manual/ko/modules/bluetape4k-javers-bom.md`
- `docs/manual/en/modules/javers-core.md`, `docs/manual/ko/modules/javers-core.md`
- `docs/manual/en/modules/javers-ddd.md`, `docs/manual/ko/modules/javers-ddd.md`
- `docs/manual/en/modules/javers-exposed.md`, `docs/manual/ko/modules/javers-exposed.md`
- `docs/manual/en/modules/javers-persistence-kafka.md`, `docs/manual/ko/modules/javers-persistence-kafka.md`
- `docs/manual/en/modules/javers-persistence-redis.md`, `docs/manual/ko/modules/javers-persistence-redis.md`
- `docs/manual/en/examples/javers-exposed-ddd.md`, `docs/manual/ko/examples/javers-exposed-ddd.md`

**Delete after reference migration**

- `docs/manual/assets/persistence/persistence-decision-map.{svg,png}`
- `docs/manual/assets/persistence/exposed-snapshot-flow.{svg,png}`
- `docs/manual/assets/examples/ddd-cqrs-sequence.{svg,png}`

**Keep as distinct manual-only diagrams**

- `docs/manual/assets/overview/repository-learning-map.{svg,png}`
- `docs/manual/assets/architecture/audit-snapshot-model.{svg,png}`

## Asset inventory and placement

| Canonical asset | Kind | Manual placement | Source authority |
|---|---|---|---|
| `root-readme-overview-01` | architecture | `index.md` | `settings.gradle.kts`, root README module table |
| `bluetape4k-javers-architecture-01` | architecture | `architecture/repository-map.md` | repository module boundaries and root README |
| `javers-persistence-options-01` | architecture/decision | `persistence/selection-guide.md` | Exposed/Redis/Kafka repository implementations |
| `bom-architecture-01` | architecture | `modules/bluetape4k-javers-bom.md` | `bom/build.gradle.kts`, version catalog |
| `javers-core-architecture-01` | architecture | `modules/javers-core.md` | `javers-core/src/main/kotlin/io/bluetape4k/javers/**` |
| `javers-core-class-diagram-01` | class | `modules/javers-core.md` | `CdoSnapshotRepository.kt`, `AbstractCdoSnapshotRepository.kt` |
| `javers-core-composite-repository-01` | architecture/flow | `modules/javers-core.md` | `repository/composite/*.kt` |
| `javers-ddd-class-diagram-01` | class | `modules/javers-ddd.md` | `AggregateRoot.kt`, `AggregateRepository.kt`, `DomainEvent*.kt` |
| `javers-ddd-save-flow-01` | sequence/flow | `modules/javers-ddd.md` | `AggregateRepository.kt`, `DomainEventPublisher.kt` |
| `javers-exposed-class-diagram-01` | class | `modules/javers-exposed.md` | `ExposedCdoSnapshotRepository.kt`, `JaversExposedTables.kt` |
| `javers-exposed-entity-hook-01` | sequence/flow | `modules/javers-exposed.md` | `hook/ExposedJaversEntityHook*.kt` |
| `javers-exposed-erd-01` | ERD | `modules/javers-exposed.md` | `schema/JaversExposedTables.kt` |
| `javers-exposed-persistence-flow-01` | sequence/flow | `modules/javers-exposed.md` | `ExposedCdoSnapshotRepository.kt` |
| `javers-kafka-repository-map-01` | architecture | `modules/javers-persistence-kafka.md` | `repository/KafkaCdoSnapshotRepository.kt`, `VanillaKafkaCdoSnapshotRepository.kt` |
| `javers-kafka-publish-flow-01` | sequence/flow | `modules/javers-persistence-kafka.md` | `KafkaSnapshotEventPublisher.kt`, `KafkaPublishTimeoutSupport.kt` |
| `javers-kafka-projection-flow-01` | sequence/flow | `modules/javers-persistence-kafka.md` | `projection/KafkaCdoSnapshotProjector.kt` |
| `javers-redis-repository-map-01` | architecture | `modules/javers-persistence-redis.md` | `LettuceCdoSnapshotRepository.kt`, `RedissonCdoSnapshotRepository.kt` |
| `javers-redis-key-layout-01` | architecture/data | `modules/javers-persistence-redis.md` | Redis repository key construction in both implementations |
| `javers-redis-save-load-flow-01` | sequence/flow | `modules/javers-persistence-redis.md` | Redis repository save/load methods |
| `examples-javers-exposed-ddd-cqrs-flow-01` | architecture/flow | `examples/javers-exposed-ddd.md` | `OrderCommandHandler.kt`, `OrderProjectionEventConsumer.kt`, `RedisOrderSummaryProjection.kt` |
| `examples-javers-exposed-ddd-sequence-01` | sequence | `examples/javers-exposed-ddd.md` | the same released example command/projection path |
| `examples-javers-ktor-request-audit-flow-01` | architecture | deferred: no manual route | `JaversKtorExampleApplication.kt`, `OrderCommandHandler.kt` |
| `examples-javers-ktor-wiring-01` | architecture | deferred: no manual route | `JaversKtorExampleApplication.kt` |
| `examples-javers-spring-boot4-request-audit-flow-01` | architecture | deferred: no manual route | `OrderController.kt`, `OrderCommandHandler.kt` |
| `examples-javers-spring-boot4-wiring-01` | architecture | deferred: no manual route | `JaversExampleConfiguration.kt`, `JaversSpringBoot4ExampleApplication.kt` |

## Approved dark palette

```ruby
PALETTE = {
  canvas: "#020617",
  surface: "#172033",
  surface_alt: "#182331",
  elevated: "#223047",
  text: "#f8fafc",
  secondary: "#d5dfeb",
  muted: "#b6c4d6",
  border: "#536377",
  blue: "#5b8def",
  teal: "#4fb8a8",
  violet: "#9b87d8",
  amber: "#d0a24c",
  olive: "#8da56d",
  red: "#d66f7e"
}.freeze
```

White canvases and pastel fills such as `#ffffff`, `#f8fafc` as a background, `#dbeafe`, `#dcfce7`, `#fef3c7`, `#f5f3ff`, `#ede9fe`, and `#fed7aa` are forbidden as card/canvas fills. `#f8fafc` remains valid only as foreground text.

### Task 1: Create the isolated implementation worktree and checklist

**Files:**
- Create in target worktree: `docs/superpowers/plans/2026-07-17-javers-dark-shared-diagram-pilot.md` as a copy of this approved plan
- Create in target worktree: `docs/review/2026-07-17-javers-dark-shared-diagram-review.md`

- [ ] **Step 1: Create the branch worktree**

Run from `/Users/debop/work/bluetape4k/bluetape4k-javers`:

```bash
git fetch origin develop
git worktree add .worktrees/docs-dark-shared-diagram-assets -b docs/dark-shared-diagram-assets origin/develop
```

Expected: a clean worktree on `docs/dark-shared-diagram-assets` whose HEAD equals `origin/develop`.

- [ ] **Step 2: Instantiate CL, CG, and Type E rows**

Add a `## Execution checklist` section to the copied plan containing CL-01..CL-08, CG-01..CG-18, and E-01..E-08. Classify production behavior, Kotlin diagnostics, Gradle tests, Testcontainers, benchmark, chezmoi parity, and release/publish as N/A with the evidence “only SVG, PNG, Markdown, YAML, and Ruby manual tooling are touched.” Keep PR rows required and merge rows pending.

- [ ] **Step 3: Verify preflight**

Run:

```bash
repo-status
gno query "bluetape4k-javers README diagram manual" -c bluetape4k-docs --fast --no-rerank
gno query "bluetape4k-javers diagram manual PR" -c bluetape4k-github --fast --no-rerank
```

Expected: clean worktree; documentation precedent found or explicit no-result evidence recorded.

- [ ] **Step 4: Commit the approved plan copy**

```bash
git add docs/superpowers/plans/2026-07-17-javers-dark-shared-diagram-pilot.md docs/review/2026-07-17-javers-dark-shared-diagram-review.md
git commit -m "Preserve the approved JaVers diagram migration contract

Constraint: Keep README SVG files as the only editable visual source
Confidence: high
Scope-risk: narrow
Directive: Validate and inspect one rendered PNG before editing the next asset
Tested: git diff --check
Not-tested: diagram conversion has not started"
```

### Task 2: Add the shared-diagram inventory and contract with TDD

**Files:**
- Create: `docs/manual/shared-diagrams.yaml`
- Create: `scripts/manual/shared_diagram_contract.rb`
- Create: `scripts/manual/shared_diagram_contract_test.rb`
- Create: `scripts/manual/sync_shared_diagrams.rb`

- [ ] **Step 1: Write failing contract tests**

Define these exact Minitest cases in `scripts/manual/shared_diagram_contract_test.rb`:

```ruby
def test_inventory_contains_all_25_canonical_pairs
  assert_equal 25, contract.entries.size
  assert_equal 21, contract.entries.count(&:selected?)
  assert_equal 4, contract.entries.count(&:deferred?)
end

def test_rejects_unsafe_relative_paths
  error = assert_raises(SharedDiagrams::ContractError) { contract_for("canonical" => "../escape") }
  assert_match(/unsafe canonical path/, error.message)
end

def test_check_reports_missing_canonical_pair
  FileUtils.rm(temp_root.join("docs/images/readme-diagrams/sample.png"))
  assert_includes contract.errors, "sample: missing canonical PNG"
end

def test_check_reports_digest_mismatch
  File.binwrite(temp_root.join("docs/manual/assets/readme-diagrams/sample.png"), "different")
  assert_includes contract.errors, "sample: canonical and mirror PNG digests differ"
end

def test_check_reports_orphan_mirror
  File.write(temp_root.join("docs/manual/assets/readme-diagrams/orphan.svg"), "<svg/>")
  assert_includes contract.errors, "orphan mirror asset: orphan.svg"
end
```

- [ ] **Step 2: Run the tests and confirm RED**

```bash
ruby scripts/manual/shared_diagram_contract_test.rb
```

Expected: FAIL because `shared_diagram_contract.rb` does not exist.

- [ ] **Step 3: Implement the contract model**

Implement `SharedDiagrams::Entry` with `id`, `kind`, `canonical`, `manual`, `manual_pages`, and `source_paths`; `selected?` returns `manual == "selected"`, and `deferred?` returns `manual == "deferred"`. Implement `SharedDiagrams::Contract#errors`, `#sync!`, and SHA-256 comparison. Every path must be relative, contain no `..`, and resolve inside the repository root.

Use these fixed roots:

```ruby
CANONICAL_ROOT = "docs/images/readme-diagrams"
MIRROR_ROOT = "docs/manual/assets/readme-diagrams"
VALID_KINDS = %w[architecture class erd flow sequence].freeze
VALID_MANUAL_STATES = %w[selected deferred].freeze
```

`sync!` copies both `.svg` and `.png` only for selected entries and removes mirror files not declared by selected entries.

- [ ] **Step 4: Implement the CLI**

`scripts/manual/sync_shared_diagrams.rb --write` invokes `sync!` and then fails if `errors` is non-empty. `--check` performs no writes and exits non-zero with one line per error. Any other argument prints `usage: sync_shared_diagrams.rb --write|--check` and exits 2.

- [ ] **Step 5: Add all inventory rows**

Populate `docs/manual/shared-diagrams.yaml` from the 25-row table above. Each row contains exact canonical basename, one normalized kind, `manual: selected|deferred`, exact English/Korean page paths for selected rows, and the listed source paths.

- [ ] **Step 6: Run GREEN tests**

```bash
ruby scripts/manual/shared_diagram_contract_test.rb
ruby scripts/manual/sync_shared_diagrams.rb --check
```

Expected: Minitest PASS; parity check initially FAIL only because selected mirrors have not been generated. Record that expected integration failure without checking the parity gate.

- [ ] **Step 7: Commit contract tooling**

Commit the four files with a Lore message whose `Tested:` trailer records the passing Minitest command and whose `Not-tested:` trailer records pending mirror generation.

### Task 3: Convert canonical diagrams one at a time

**Files:**
- Modify: `docs/images/readme-diagrams/*.{svg,png}`
- Modify after every asset: `docs/review/2026-07-17-javers-dark-shared-diagram-review.md`

- [ ] **Step 1: Process assets in this fixed order**

```text
root-readme-overview-01
bluetape4k-javers-architecture-01
javers-persistence-options-01
bom-architecture-01
javers-core-architecture-01
javers-core-class-diagram-01
javers-core-composite-repository-01
javers-ddd-class-diagram-01
javers-ddd-save-flow-01
javers-exposed-class-diagram-01
javers-exposed-entity-hook-01
javers-exposed-erd-01
javers-exposed-persistence-flow-01
javers-kafka-repository-map-01
javers-kafka-publish-flow-01
javers-kafka-projection-flow-01
javers-redis-repository-map-01
javers-redis-key-layout-01
javers-redis-save-load-flow-01
examples-javers-exposed-ddd-cqrs-flow-01
examples-javers-exposed-ddd-sequence-01
examples-javers-ktor-wiring-01
examples-javers-ktor-request-audit-flow-01
examples-javers-spring-boot4-wiring-01
examples-javers-spring-boot4-request-audit-flow-01
```

- [ ] **Step 2: For each asset, re-read its authority before editing**

Read the README section embedding the asset and every source path registered for it. Record the reader question, represented source types/components, and relationships in the review ledger before changing SVG.

- [ ] **Step 3: Apply the dark visual contract to that one SVG**

Keep the existing information model and layout unless a checklist defect requires moving or widening cards, rerouting connectors, or expanding the viewBox. Use `#020617` for canvas, the approved dark surfaces for cards, `#f8fafc`/`#d5dfeb` for text, `#536377` for neutral borders, and muted semantic accents. Preserve orthogonal rounded routes and semantic connector colors. For dashed UML relations, put both `stroke-dasharray="none"` and `style="stroke-dasharray:none"` on marker child paths.

- [ ] **Step 4: Render and mechanically validate that one asset**

For `ASSET=<basename>` run:

```bash
xmllint --noout "docs/images/readme-diagrams/${ASSET}.svg"
cairosvg "docs/images/readme-diagrams/${ASSET}.svg" -o "docs/images/readme-diagrams/${ASSET}.png" -s 2
python3 ~/.codex/skills/bluetape-diagram/scripts/diagram-connector-audit.py "docs/images/readme-diagrams/${ASSET}.svg"
python3 ~/.codex/skills/bluetape-diagram/scripts/diagram-geometry-audit.py --fail-diagonal "docs/images/readme-diagrams/${ASSET}.svg"
python3 ~/.codex/skills/bluetape-diagram/scripts/diagram-endpoint-audit.py "docs/images/readme-diagrams/${ASSET}.svg"
python3 ~/.codex/skills/bluetape-diagram/scripts/diagram-mixed-corner-audit.py "docs/images/readme-diagrams/${ASSET}.svg"
```

Add the matching kind-specific audit from `bluetape-diagram`; for sequence assets run `diagram-sequence-style-audit.py`. Expected: every command exits 0; PNG dimensions equal exactly twice the SVG canvas dimensions.

- [ ] **Step 5: Inspect the authoritative PNG at original size**

Open `docs/images/readme-diagrams/${ASSET}.png` individually. Check all text stays inside cards, arrowheads are at least the guide size and remain solid after rasterization, connectors attach to intended ports, no line crosses a card or unrelated connector, labels clear lines, dark contrast is readable, and whitespace is balanced. A contact sheet may screen the batch but cannot satisfy this step.

- [ ] **Step 6: Record evidence before the next asset**

Append one ledger section containing exact canonical paths, README and source anchors, kind, commands, PNG dimensions, audit counts, and full-size inspection notes. Do not begin the next asset until the current ledger entry has zero unresolved defects.

- [ ] **Step 7: Commit after each reader-owned group**

Create separate commits for root+BOM, Core, DDD, Exposed, Kafka, Redis, Exposed-DDD example, Ktor example, and Spring Boot example. Each commit records the exact rendered assets and audit commands in `Tested:`.

### Task 4: Generate selected manual mirrors and prove parity

**Files:**
- Modify: `docs/manual/shared-diagrams.yaml`
- Modify: `scripts/manual/shared_diagram_contract.rb`
- Modify: `scripts/manual/shared_diagram_contract_test.rb`
- Modify: `scripts/manual/sync_shared_diagrams.rb`

- [x] **Step 1: Pin the mirror target to minor 0.3**

Set `schemaVersion: 2` and `targetMinor: '0.3'`. The contract reads `stableMinor` from `docs/manual/manifest.yaml` and activates selected entries only when both minors match.

- [x] **Step 2: Prove the stable 0.2 gate**

```bash
ruby scripts/manual/sync_shared_diagrams.rb --write
```

Expected while stable is 0.2: `selected=21 active=0 target=0.3 stable=0.2`, zero mirror files, and no contract errors.

- [ ] **Step 3: Generate and register mirrors after the 0.3 stable transition**

After a real 0.3 release updates the manual manifest and release provenance, generate 21 SVG/PNG pairs and add the 42 paths under `overview.assets`. Do not register the four deferred Ktor/Spring Boot pairs.

- [ ] **Step 4: Prove active parity and absence of orphans after release**

```bash
ruby scripts/manual/sync_shared_diagrams.rb --check
find docs/manual/assets/readme-diagrams -maxdepth 1 -name '*.svg' | wc -l
find docs/manual/assets/readme-diagrams -maxdepth 1 -name '*.png' | wc -l
```

Expected: parity PASS, `21` SVG, `21` PNG, no orphan diagnostics.

- [x] **Step 5: Commit the version-aware mirror contract**

Commit the schema and contract changes. The commit message must state that generated mirrors remain inactive until stable minor 0.3 and must never be edited directly.

### Task 5: Integrate shared diagrams into bilingual manuals

This task is release-gated. Do not modify the 0.2 pages or delete their release-specific diagrams. Execute these steps only after the stable manual manifest and release provenance move to 0.3.

**Files:**
- Modify the 20 English/Korean pages listed in the file map.
- Delete the three duplicate manual-only SVG/PNG pairs listed in the file map after all references move.

- [ ] **Step 1: Add localized architecture sections**

Insert `## Architecture and design` and `## 아키텍처와 설계` after the page's dependency/purpose section and before quick-start recipes. For each embedded asset, write three short paragraphs: the question it answers, the concrete public/runtime/persistence components shown, and the design consequence. Korean prose must be edited as natural technical Korean, not a sentence-by-sentence translation.

Use this link form from module/example pages:

```markdown
[![JaVers core repository architecture](../../assets/readme-diagrams/javers-core-architecture-01.png)](../../assets/readme-diagrams/javers-core-architecture-01.svg)
```

Use `../assets/...` from `index.md` and `../../assets/...` from two-level overview/module/example pages.

- [ ] **Step 2: Replace three duplicate manual-only diagrams**

Replace `persistence-decision-map` with `javers-persistence-options-01`, `exposed-snapshot-flow` with `javers-exposed-persistence-flow-01`, and `ddd-cqrs-sequence` with `examples-javers-exposed-ddd-sequence-01`. Delete the old pairs only after `rg` returns no references.

- [ ] **Step 3: Preserve two unique manual diagrams**

Keep `repository-learning-map` on Home because it explains learning order, and keep `audit-snapshot-model` because it explains the commit/snapshot conceptual model rather than a module class map.

- [ ] **Step 4: Verify bilingual image parity and links**

```bash
rg -n 'readme-diagrams/' docs/manual/en docs/manual/ko
rg -n 'persistence-decision-map|exposed-snapshot-flow|ddd-cqrs-sequence' docs/manual docs/manual/manifest.yaml
ruby scripts/manual/validate_manuals.rb
```

Expected: each selected image appears in its intended English and Korean page; old duplicate names return zero results; manual validation passes.

- [ ] **Step 5: Commit manual integration**

Commit English and Korean pages, manifest updates, and duplicate removals together. Record natural-Korean review and link validation in the Lore trailers.

### Task 6: Run full documentation and visual DoD

**Files:**
- Modify: `docs/review/2026-07-17-javers-dark-shared-diagram-review.md`

- [ ] **Step 1: Run Ruby/manual contract tests**

```bash
ruby scripts/manual/shared_diagram_contract_test.rb
ruby scripts/manual/export_manifest_test.rb
ruby scripts/manual/manual_contract_test.rb
ruby scripts/manual/release_contract_test.rb
ruby scripts/manual/release_inventory_test.rb
ruby scripts/manual/sync_shared_diagrams.rb --check
ruby scripts/manual/validate_manuals.rb
ruby scripts/manual/validate_release_manuals.rb
```

Expected: all tests and validators pass.

- [ ] **Step 2: Run full canonical SVG and PNG checks**

```bash
find docs/images/readme-diagrams -name '*.svg' -print0 | xargs -0 -n1 xmllint --noout
find docs/images/readme-diagrams -name '*.svg' -print0 | xargs -0 python3 ~/.codex/skills/bluetape-diagram/scripts/diagram-connector-audit.py
find docs/images/readme-diagrams -name '*.svg' -print0 | xargs -0 python3 ~/.codex/skills/bluetape-diagram/scripts/diagram-geometry-audit.py --fail-diagonal
find docs/images/readme-diagrams -name '*.svg' -print0 | xargs -0 python3 ~/.codex/skills/bluetape-diagram/scripts/diagram-endpoint-audit.py
find docs/images/readme-diagrams -name '*.svg' -print0 | xargs -0 python3 ~/.codex/skills/bluetape-diagram/scripts/diagram-mixed-corner-audit.py
```

Expected: 25 SVG files, zero audit failures. Run the class, ERD, and sequence-specific audits on the inventory subsets and record exact counts.

- [ ] **Step 3: Run README and manual link checks**

Resolve every local PNG link in `README.md`, `README.ko.md`, module/example READMEs, and `docs/manual/{en,ko}`. Before the 0.3 release, expected: missing links `0`, README SVG embeds `0`, selected manual mirror pairs `0`, deferred manual pairs `0`, and the version gate reports `active=0`.

- [ ] **Step 4: Perform final individual and set-level visual review**

Reopen all 25 canonical PNGs individually at original size, then build a contact sheet only to detect style drift across the set. Record unresolved visual defects `0`. The final ledger must state card overflow, connector intrusion/crossing, endpoint, marker/arrowhead, contrast, clipping, and whitespace results for every asset.

- [ ] **Step 5: Run final diff gates**

```bash
git diff --check
git status --short
git diff --stat origin/develop...HEAD
```

Expected: no whitespace errors, only approved docs/image/Ruby files changed, production Kotlin files changed `0`.

- [ ] **Step 6: Evaluate lesson gate and converge**

Reuse `docs/lessons/2026-06-18-readme-diagram-refresh.md` and the approved cross-repo design when no new failure/recovery/design/operations guidance emerged. Otherwise add one concise lesson. Run a final review with P0=0/P1=0 and commit the converged head.

### Task 7: Create and verify the JaVers PR

**Files:**
- No new repository files unless PR review requires repair.

- [ ] **Step 1: Verify delivery authority and exact head**

Confirm target `bluetape4k/bluetape4k-javers`, base `develop`, head `docs/dark-shared-diagram-assets`, CG-01..CG-10 PASS, and exact local head SHA.

- [ ] **Step 2: Push without force and verify remote SHA**

```bash
git push -u origin docs/dark-shared-diagram-assets
git rev-parse HEAD
git ls-remote --heads origin docs/dark-shared-diagram-assets
```

Expected: local and remote head SHAs match.

- [ ] **Step 3: Create and verify the PR**

Create the PR, assign `debop`, copy issue metadata only if an issue is explicitly linked, and ensure the final Markdown heading is `## DoD Status`. Verify with:

```bash
gh pr view --json number,url,headRefName,headRefOid,baseRefName,assignees,labels,milestone,body
```

- [ ] **Step 4: Wait for exact-head CI and current review**

```bash
gh pr checks --watch
gh pr view --json headRefOid,reviews,reviewDecision,statusCheckRollup
```

Expected: required checks pass on the pushed head and no unresolved blocking review remains.

- [ ] **Step 5: Report merge-ready and stop**

Report exact PR URL, head SHA, diagram totals, visual ledger result, Ruby/manual validation, lesson decision, and `Required checks: X/Y; N/A: N; Blocked: 0`. Leave CG-16 and E-08 PENDING until the user gives fresh merge approval.


## Execution checklist

Status is recorded with fresh evidence. An unchecked row blocks dependent work.

### Checklist contract

- [x] **CL-01 — Create before mutation**
  - **Action:** Instantiate router, common, and Type E checklist rows before implementation edits.
  - **Evidence:** This section was created with the approved plan before diagram/tooling mutation.
  - **Failure:** Stop and reconstruct the checklist before continuing.
- [x] **CL-02 — Classify every item**
  - **Action:** Classify applicable, conditional, and N/A rows.
  - **Evidence:** Docs/diagram/Ruby rows are required; JVM, Testcontainers, benchmark, chezmoi, release, and publish rows are N/A; PR rows are required; merge rows are PENDING.
  - **Failure:** Treat unclassified rows as required and unchecked.
- [ ] **CL-03 — Respect dependency order**
  - **Action:** Execute rows in documented order.
  - **Evidence:** Task and evidence timestamps remain monotonic.
  - **Failure:** Stop and rerun affected downstream proof.
- [ ] **CL-04 — Record evidence immediately**
  - **Action:** Attach evidence when each row passes.
  - **Evidence:** Commands, counts, paths, and results recorded beside each row and per-asset ledger.
  - **Failure:** Leave the row unchecked.
- [ ] **CL-05 — Fail closed**
  - **Action:** Block downstream work on PENDING or failed prerequisites.
  - **Evidence:** No dependent row advances past an unchecked prerequisite.
  - **Failure:** Repair and rerun invalid downstream proof.
- [ ] **CL-06 — Repair skipped or reordered work**
  - **Action:** Repair any missed row and refresh its dependents.
  - **Evidence:** Repair result and repeated downstream evidence.
  - **Failure:** Final status remains BLOCKED.
- [ ] **CL-07 — Refresh irreversible holds**
  - **Action:** Refresh authority immediately before push, PR, and merge.
  - **Evidence:** Current repo/base/head and exact action read back at each hold.
  - **Failure:** Do not execute the side effect.
- [ ] **CL-08 — Count before completion**
  - **Action:** Reconcile required, N/A, pending, and blocked rows.
  - **Evidence:** Final Required checks X/Y, N/A N, Blocked N report.
  - **Failure:** Completion claim is forbidden.

### Common gates

- [x] **CG-01 — Re-read authority**
  - **Action:** Read AGENTS, skills, approved spec/plan, status, and diff.
  - **Evidence:** Workspace and repo AGENTS, writing/executing plans, bluetape workflow/maintenance/diagram skills read; approved plan commit 0dc86e5; clean target worktree at 4713040.
  - **Failure:** Stop before editing.
- [x] **CG-02 — Query historical/current evidence**
  - **Action:** Query GNO docs and GitHub evidence.
  - **Evidence:** Docs query returned the Leader inventory precedent and manual examples; GitHub query returned no indexed result, so live repo/PR evidence remains authoritative.
  - **Failure:** Stop decisions that depend on missing history.
- [x] **CG-03 — Protect user work and boundaries**
  - **Action:** Isolate the worktree and exclude unrelated changes.
  - **Evidence:** Worktree .worktrees/docs-dark-shared-diagram-assets, base origin/develop, clean start, production Kotlin excluded.
  - **Failure:** Preserve or block; never discard user work.
- [x] **CG-04 — Apply policy and audience boundaries**
  - **Action:** Apply English labels, bilingual manuals, and permission boundaries.
  - **Evidence:** Shared English assets; English/Korean prose parity required; no global policy, Claude, or vendor surfaces touched.
  - **Failure:** Repair language or authorization drift.
- [ ] **CG-05 — Reuse ecosystem patterns**
  - **Action:** Reuse current JaVers manual renderer/validators and bluetape diagram audits.
  - **Evidence:** Exact reused files and commands recorded after contract implementation.
  - **Failure:** Stop new abstraction work.
- [ ] **CG-06 — Prove public and documentation contracts**
  - **Action:** Update manual registration, localized pages, and README asset contract.
  - **Evidence:** Manifest, link, parity, and language checks pass.
  - **Failure:** Block delivery.
- [ ] **CG-07 — Lock behavior and run targeted proof**
  - **Action:** Use RED/GREEN for Ruby behavior and targeted diagram/manual validation.
  - **Evidence:** Failing then passing contract tests plus visual audits.
  - **Failure:** Return to implementation.
- [x] **CG-08 — Serialize heavyweight checks**
  - **Action:** Classify heavyweight runtime checks.
  - **Evidence:** N/A: no Kotlin, Testcontainers, native/JNI, emulator, benchmark, or shared runtime behavior changes.
  - **Failure:** Reclassify if scope changes.
- [ ] **CG-09 — Evaluate the lesson gate**
  - **Action:** Review task/diff against existing diagram lessons.
  - **Evidence:** Lesson path or concrete N/A rationale covering failure, recovery, design, and operations.
  - **Failure:** Repair before pre-PR review.
- [ ] **CG-10 — Converge final pre-PR proof**
  - **Action:** Complete leaf gates, final review, checks, and commit.
  - **Evidence:** P0=0/P1=0, fresh checks, exact head SHA.
  - **Failure:** PR remains blocked.
- [ ] **CG-11 — Verify PR delivery authority**
  - **Action:** Verify approved target/base/head and prerequisites.
  - **Evidence:** bluetape4k/bluetape4k-javers, develop, docs/dark-shared-diagram-assets and CG-01..10 PASS.
  - **Failure:** Stop before PR creation.
- [ ] **CG-12 — Publish exact head**
  - **Action:** Push the authorized branch without force and read remote SHA.
  - **Evidence:** Matching local/remote SHA.
  - **Failure:** Stop before PR creation.
- [ ] **CG-13 — Create and verify PR**
  - **Action:** Create PR, assign debop, end body with DoD Status, and read it live.
  - **Evidence:** PR URL, metadata, head SHA, and body.
  - **Failure:** Repair live PR.
- [ ] **CG-14 — Pass CI and live review**
  - **Action:** Wait for exact-head CI and current review state.
  - **Evidence:** Successful checks and no blocking review.
  - **Failure:** PENDING or return to repair.
- [ ] **CG-15 — Report merge-ready**
  - **Action:** Reconcile checks and report exact PR/head.
  - **Evidence:** User-visible merge-ready report with counts.
  - **Failure:** Repair missing evidence.
- [ ] **CG-16 — Obtain fresh merge approval**
  - **Action:** Wait for approval after CG-15.
  - **Evidence:** PENDING until user approves exact PR/head.
  - **Failure:** Never auto-merge.
- [ ] **CG-17 — Execute and verify merge**
  - **Action:** Merge only after CG-16.
  - **Evidence:** Merged state and SHA.
  - **Failure:** Stop and diagnose.
- [ ] **CG-18 — Synchronize and clean up**
  - **Action:** Sync local checkout and safely clean merged worktree/branch.
  - **Evidence:** Local/upstream SHAs and cleanup result.
  - **Failure:** Preserve ambiguous state.
- [x] **CG-X01 — Other irreversible actions**
  - **Action:** Classify tag, release, publish, dispatch, and deletion.
  - **Evidence:** N/A: none are in the approved scope.
  - **Failure:** Reclassify and obtain authority if scope changes.

### Type E maintenance gates

- [x] **E-01 — Route support skills**
  - **Action:** Load diagram and documentation support skills.
  - **Evidence:** bluetape-diagram and its common/architecture/class/ERD/sequence references loaded; bilingual manual policy loaded.
  - **Failure:** Stop before editing.
- [x] **E-02 — Discover current guidance**
  - **Action:** Read sources, READMEs, manual structure, lessons, GNO, and reproduction chain.
  - **Evidence:** 25 canonical pairs, 5 manual-only pairs, manifest, render/validation scripts, and 2026-06-18 lesson inspected.
  - **Failure:** Remain read-only.
- [x] **E-03 — Preserve behavior and ownership**
  - **Action:** Keep production behavior unchanged and canonical SVG ownership explicit.
  - **Evidence:** Scope limited to SVG/PNG/Markdown/YAML/Ruby tooling; canonical README SVG remains editable source.
  - **Failure:** Revert or reclassify.
- [x] **E-04 — Apply and prove parity**
  - **Action:** Classify chezmoi parity.
  - **Evidence:** N/A: repository docs assets are not chezmoi-managed; canonical/manual digest parity is handled by Task 4.
  - **Failure:** Reclassify if a managed source appears.
- [ ] **E-05 — Run maintenance verification**
  - **Action:** Run diff, references, Ruby/manual, diagram, link, and visual checks.
  - **Evidence:** Fresh final command results.
  - **Failure:** Repair before commit/push.
- [ ] **E-06 — Complete durable pre-PR proof**
  - **Action:** Verify duplicates, locales, registration, mirrors, and pruning.
  - **Evidence:** Final diff and all triggered maintenance checks PASS.
  - **Failure:** Block PR.
- [ ] **E-07 — Deliver and report through PR gates**
  - **Action:** Complete CG-11..CG-15.
  - **Evidence:** Exact-head PR and merge-ready report.
  - **Failure:** CI/review wait is PENDING.
- [ ] **E-08 — Close only after fresh merge approval**
  - **Action:** Complete CG-16..CG-18 after fresh approval.
  - **Evidence:** PENDING until merge-ready approval.
  - **Failure:** Do not merge early.
