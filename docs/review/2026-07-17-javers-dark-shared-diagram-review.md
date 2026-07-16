# JaVers Dark Shared Diagram Review

## Scope

- Canonical README diagrams: 25 SVG/PNG pairs under docs/images/readme-diagrams.
- Selected manual mirrors: 21 SVG/PNG pairs under docs/manual/assets/readme-diagrams.
- Deferred manual mirrors: four Ktor/Spring Boot example pairs because no manual routes exist.
- Visual authority: bluetape4k-projects/docs/manual/assets/annotations/annotation-decision-map.png.
- Render authority: CairoSVG at scale 2.

## Per-asset ledger

Each asset entry records its reader question, source anchors, kind, XML/render result, audit counts, canonical/mirror digest, and original-size PNG inspection. Entries are added immediately after the asset passes.

### `root-readme-overview-01`

- Reader question: What is the smallest adoption path from JaVers core helpers to optional DDD, persistence, examples, and benchmarks?
- Authority: `README.md`, `README.ko.md`, and the included projects in `settings.gradle.kts`.
- Kind: architecture.
- Canonical: `docs/images/readme-diagrams/root-readme-overview-01.{svg,png}`.
- XML/render: `xmllint --noout` PASS; CairoSVG `-s 2` produced a `3200x1240` RGBA PNG.
- Audits: connectors `6`, cards `7`, intrusions `0`, crossings `0`, shared segments `0`; geometry failures `0`; endpoint PASS; mixed-corner paths `6`, quadratic bends `4`, failures `0`.
- Repair made during audit: split the two `javers-core` routes across separate right-side ports so they no longer shared an initial segment; added explicit card and connector metadata.
- Original-size eye review: all labels remain inside their cards; the long `Measure benchmarks` title clears the right edge; six 14x14 arrowheads are visible and match their connector colors; no line crosses a card or another connector; the Exposed, Redis, and Kafka icons remain readable on dark surfaces; outer and adoption frames have balanced margins.
- Manual mirror: pending Task 4.

### `bluetape4k-javers-architecture-01`

- Reader question: Which consumer entry points depend on each JaVers module, persistence adapter, and verification artifact?
- Authority: root `README.md`/`README.ko.md`, `settings.gradle.kts`, and the module source directories registered in `docs/manual/shared-diagrams.yaml`.
- Kind: architecture.
- Canonical: `docs/images/readme-diagrams/bluetape4k-javers-architecture-01.{svg,png}`.
- XML/render: `xmllint --noout` PASS; CairoSVG `-s 2` produced a `3680x2480` PNG.
- Audits: connectors `14`, cards `16`, crossings `0`, shared segments `0`; geometry failures `0`; endpoint PASS; mixed-corner paths `14`, quadratic bends `19`, failures `0`. The generic intrusion screen reports six source/target-edge contacts at its default 1.5px interior threshold; the targeted 20px interior screen reports intrusions `0`, and the original-size review confirms the contacts are the intended endpoints rather than routes through cards.
- Repairs made during audit: separated the CQRS source ports and DDD target ports, moved all top-lane starts to actual card boundaries, increased connector shafts to 4px and markers to 14x14, and retained contrasting blue/amber routes where BOM and JaVers paths run in adjacent corridors.
- Original-size eye review: every card label and detail line fits; all fourteen arrowheads are visible; dashed relations retain solid arrowheads; no route crosses another route or a card interior; icons remain legible; lane titles, cards, and the footer have clear vertical spacing.
- Manual mirror: pending Task 4.

### `javers-persistence-options-01`

- Reader question: Which persistence role is authoritative for SQL history, low-latency snapshot state, audit events, and optional cache acceleration?
- Authority: the root README persistence contract plus the `javers-exposed`, `javers-persistence-redis`, and `javers-persistence-kafka` modules registered in `settings.gradle.kts`.
- Kind: architecture.
- Canonical: `docs/images/readme-diagrams/javers-persistence-options-01.{svg,png}`.
- XML/render: `xmllint --noout` PASS; CairoSVG `-s 2` produced a `3400x1640` RGBA PNG.
- Audits: connectors `7`, cards `6`, intrusions `0`, crossings `0`, shared segments `0`; geometry failures `0`; endpoint PASS; mixed-corner paths `7`, quadratic bends `10`, failures `0`.
- Repairs made during audit: split the application source ports, separated the Exposed-to-Kafka and Exposed-to-decision source ports, and assigned three distinct decision-card target ports so the green, rose, and violet routes never overlap.
- Original-size eye review: every label fits its card; all seven 14x14 arrowheads are visible; the three decision routes remain distinguishable at a glance; no connector crosses another route or a card interior; Exposed, Redis, and Kafka icons remain readable; the rule-of-thumb footer has clear margins.
- Manual mirror: pending Task 4.

### `bom-architecture-01`

- Reader question: What does the JaVers BOM manage, what runtime behavior does it deliberately omit, and which published artifacts are constrained?
- Authority: `bom/build.gradle.kts`, `bom/README.md`, and the current publishable project set in `settings.gradle.kts`.
- Kind: architecture.
- Canonical: `docs/images/readme-diagrams/bom-architecture-01.{svg,png}`.
- XML/render: `xmllint --noout` PASS; CairoSVG `-s 2` produced a `3360x1960` RGBA PNG.
- Audits: connectors `1`, cards `9`, intrusions `0`, crossings `0`, shared segments `0`; geometry failures `0`; endpoint PASS; mixed-corner paths `1`, quadratic bends `0`, failures `0`.
- Source-accuracy repair: added the published `bluetape4k-javers-spring-boot4-autoconfigure` constraint that was present in the BOM source and README but missing from the old diagram.
- Original-size eye review: all six managed artifact names and descriptions fit; the single 14x14 arrowhead is visible and lands on the managed-artifact boundary; no connector crosses a card; Exposed, Redis, and Kafka icons remain readable; the expanded artifact frame and footer keep balanced vertical margins.
- Manual mirror: pending Task 4.

### `javers-core-architecture-01`

- Reader question: How do application calls reach the core Kotlin APIs, repository primitives, local implementations, and downstream persistence adapters?
- Authority: `javers-core/README.md` and the public production sources under `javers-core/src/main/kotlin/io/bluetape4k/javers`.
- Kind: architecture.
- Canonical: `docs/images/readme-diagrams/javers-core-architecture-01.{svg,png}`.
- XML/render: `xmllint --noout` PASS; CairoSVG `-s 2` produced a `3360x1800` RGBA PNG.
- Audits: connectors `8`, cards `10`, intrusions `0`, crossings `0`, shared segments `0`; geometry failures `0`; endpoint PASS; mixed-corner paths `8`, quadratic bends `10`, failures `0`.
- Original-size eye review: all responsibility labels fit; every 14x14 arrowhead is visible; the three repository-base routes leave distinct ports and stay separated; no connector crosses a card or another route; dashed adapter flow remains legible; lane headings and footer retain clear margins.
- Manual mirror: pending Task 4.

### `javers-core-class-diagram-01`

- Reader question: Which repository, codec, dispatcher, and commit-id contracts are implemented by the concrete core helpers?
- Authority: `CdoSnapshotRepository.kt`, `AbstractCdoSnapshotRepository.kt`, the codec package, local repository packages, dispatcher package, and `SnowflakeCommitIdGenerator.kt`.
- Kind: class.
- Canonical: `docs/images/readme-diagrams/javers-core-class-diagram-01.{svg,png}`.
- XML/render: `xmllint --noout` PASS; CairoSVG `-s 2` produced a `3520x2080` RGBA PNG.
- Audits: connectors `9`, cards `12`, intrusions `0`, crossings `0`, shared segments `0`; geometry failures `0`; endpoint PASS; mixed-corner paths `9`, quadratic bends `10`, failures `0`.
- Source-accuracy repair: added `projectSnapshot(snapshot)` to the public repository contract card.
- Repairs made during audit: assigned separate inheritance target ports to String, Binary, and Map codecs so their three implementation routes no longer cross or share a terminal segment; raised member-text contrast after the original-size preview exposed low-contrast class details.
- Original-size eye review: class names, stereotypes, and members fit their compartments; all inheritance triangles use the required 18x16 size and the dashed dependency arrow uses the 14x14 primary-flow size; no relationship line crosses a card or another connector; the codec fan-in reads as three distinct relationships; both responsibility lanes and the footer retain clear margins.
- Manual mirror: pending Task 4.

### `javers-core-composite-repository-01`

- Reader question: How does a composite repository keep one read authority while persisting to ordered secondary projection and stream repositories?
- Authority: `javers-core/README.md`, `CompositeCdoSnapshotRepository.kt`, and `CompositeCdoSnapshotRepositoryOptions.kt`.
- Kind: architecture.
- Canonical: `docs/images/readme-diagrams/javers-core-composite-repository-01.{svg,png}`.
- XML/render: `xmllint --noout` PASS; CairoSVG `-s 2` produced a `3400x2000` RGBA PNG.
- Audits: connectors `5`, cards `6`, intrusions `0`, crossings `0`, shared segments `0`; geometry failures `0`; endpoint PASS; mixed-corner paths `5`, quadratic bends `4`, failures `0`; text hazards `0`, code without highlight `0`.
- Source-accuracy repair: clarified that `FAIL_FAST` and `BEST_EFFORT` govern secondary writes; retained the primary-first, non-transactional contract and primary-only read path.
- Repairs made during audit: removed the shared fan-out trunk, assigned separate bottom ports on the primary card, routed Redis and Kafka through independent rounded corridors, and raised all primary-flow markers from 10x10 to 14x14.
- Original-size eye review: all labels fit their cards; five arrowheads are visible and match their connectors; the amber and rose fan-out paths remain separate from their source ports to their targets; no connector crosses a card or another route; Redis, Kafka, and database icons remain readable; frame, title, and footer margins are balanced.
- Manual mirror: pending Task 4.
