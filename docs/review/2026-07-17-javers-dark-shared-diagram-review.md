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
- Original-size eye review: class names, stereotypes, and members fit their compartments; all inheritance triangles use the required 18x16 size and the dashed static dependency arrow uses the required 10x10 size; no relationship line crosses a card or another connector; the codec fan-in reads as three distinct relationships; both responsibility lanes and the footer retain clear margins.
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

### `javers-ddd-class-diagram-01`

- Reader question: Which aggregate, audit-boundary, commit-property, and publisher contracts make up the DDD helper layer?
- Authority: `javers-ddd/README.md` and all production sources under `javers-ddd/src/main/kotlin/io/bluetape4k/javers/ddd`.
- Kind: class.
- Canonical: `docs/images/readme-diagrams/javers-ddd-class-diagram-01.{svg,png}`.
- XML/render: `xmllint --noout` PASS; CairoSVG `-s 2` produced a `3720x2080` RGBA PNG. A second CairoSVG render had the same SHA-256; the full-size review used an RGB-flattened temporary copy because the app preview intermittently misdisplays nearly opaque RGBA images.
- Audits: connectors `10`, cards `12`, intrusions `0`, crossings `0`, shared segments `0`; geometry failures `0`; endpoint PASS; mixed-corner paths `10`, quadratic bends `13`, failures `0`; text hazards `0`, code without highlight `0`.
- Relationship inventory: six direct `DomainEventPublisher` implementations and four dashed/static dependencies. Inheritance markers use 18x16 hollow triangles; dependency markers use 10x10 solid heads.
- Source-accuracy repairs: replaced the false Spring-to-Noop, Kafka-to-Function, and NATS-to-Composite relationships with direct interface implementations; repeated the interface card for the integration-adapter group to keep all six relationships explicit and crossing-free; added the overridable `saveAuditBoundary(block)` contract to `AggregateRepository`.
- Original-size eye review: every type, member, group label, and footer fits; all inheritance and dependency heads remain visible; no relationship crosses a card or another line; the two interface groups clearly state that the contract is repeated only for readability; lane and outer-frame margins remain balanced.
- Manual mirror: pending Task 4.

### `javers-ddd-save-flow-01`

- Reader question: In what order does `AggregateRepository` persist application state, commit the saved aggregate to JaVers, and publish collected domain events?
- Authority: `javers-ddd/README.md`, `AggregateRepository.kt`, `DomainEventPublisher.kt`, and the Spring/Kafka publisher implementations under `javers-ddd/src/main/kotlin/io/bluetape4k/javers/ddd`.
- Kind: sequence.
- Canonical: `docs/images/readme-diagrams/javers-ddd-save-flow-01.{svg,png}`.
- XML/render: `xmllint --noout` PASS; CairoSVG `-s 2` produced a `3600x2400` RGBA PNG. The full-size review used `/tmp/javers-ddd-save-flow-flat-20260717.png` because the app preview intermittently renders nearly opaque RGBA regions as black.
- Audits: sequence-style PASS with six participant headers, six lifelines, five activation bars, eight visible numbered message pills, six fixed `userSpaceOnUse` markers, and one styled `alt/else` region; connectors `8`, cards `6`, intrusions `0`, crossings `0`, shared segments `0`; geometry failures `0`; endpoint PASS; mixed-corner paths `8`, quadratic bends `0`, failures `0`; text hazards `0`, code without highlight `0`.
- Source-accuracy repairs: replaced the old card flowchart with an actual sequence; made source persistence and the JaVers commit finish inside `saveAuditBoundary` before `publishAll`; represented Spring transaction synchronization as an explicit `afterCommit` versus immediate-delivery branch; stated that the helper is not a durable outbox.
- Repairs made during audit: normalized sequence markers to the established 14-unit 10x10 triangle family; added explicit sequence-family classes for lifelines, activations, message pills, numbering, and branch regions; enlarged the branch frame and separated the branch title, numbered message labels, arrows, and divider after the first full-size review exposed label overlap.
- Original-size eye review: all participant names and roles fit; every numbered pill has clear space above its own arrow; all eight arrowheads are visible and match the message color; the transaction boundary encloses only persistence and JaVers commit steps; the `alt/else` titles no longer collide with message labels; no connector crosses a card or another route; the outbox warning and outer-frame margins remain balanced.
- Manual mirror: pending Task 4.

### `javers-exposed-class-diagram-01`

- Reader question: How does the SQL-backed repository inherit shared JaVers behavior and construct the configurable Exposed schema and table mappings?
- Authority: `javers-exposed/README.md`, `ExposedCdoSnapshotRepository.kt`, and `JaversExposedTables.kt`.
- Kind: class.
- Canonical: `docs/images/readme-diagrams/javers-exposed-class-diagram-01.{svg,png}`.
- XML/render: `xmllint --noout` PASS; CairoSVG `-s 2` produced a `3760x2080` RGBA PNG. The full-size review used `/tmp/javers-exposed-class-flat-20260717.png` to avoid the app RGBA preview defect.
- Audits: connectors `7`, cards `8`, intrusions `0`, crossings `0`, shared segments `0`; geometry failures `0`; endpoint PASS; mixed-corner paths `7`, quadratic bends `8`, failures `0`; text hazards `0`, code without highlight `0`.
- Relationship inventory: one repository inheritance and six constructor/ownership/use dependencies. The inheritance marker uses an 18x16 hollow triangle; all dependency markers use 10x10 solid heads.
- Source-accuracy repairs: replaced generic `Repository Options` and `Table Names` labels with `ExposedCdoSnapshotRepositoryOptions` and `ExposedJaversTableNames`; expanded the commit and snapshot mapping fields to include `commit_date_instant` and `changed_properties`; retained the repository codec default and SQL-query fallback contract.
- Repairs made during audit: separated the two schema-to-table source ports and corridors, restored visible compartment dividers after palette conversion, and used a smaller exact-name title style instead of abbreviating public types.
- Original-size eye review: all exact type names, members, and table fields fit their compartments; the inheritance head and six dependency heads are visible and semantically distinct; dashed dependencies keep solid heads; no relationship crosses another line or card; the two schema-owned table routes leave separate ports; lane and outer-frame margins remain balanced.
- Manual mirror: pending Task 4.

### `javers-exposed-entity-hook-01`

- Reader question: How does an Exposed DAO lifecycle event become a JaVers object commit or shallow-delete commit, and which writes remain outside the hook?
- Authority: `javers-exposed/README.md`, `ExposedJaversEntityHookMapping.kt`, and `ExposedJaversEntityHookSubscription.kt`.
- Kind: sequence.
- Canonical: `docs/images/readme-diagrams/javers-exposed-entity-hook-01.{svg,png}`.
- XML/render: `xmllint --noout` PASS; CairoSVG `-s 2` produced a `3680x2560` RGBA PNG. The full-size review used `/tmp/javers-exposed-entity-hook-flat2-20260717.jpg` because the app intermittently blackens large PNG previews even after RGB flattening.
- Audits: sequence-style PASS with six participant headers, six lifelines, five activation bars, twelve visible numbered message pills, six fixed `userSpaceOnUse` markers, and one styled `alt/else` region; connectors `12`, cards `6`, intrusions `0`, crossings `0`, shared segments `0`; geometry failures `0`; endpoint PASS; mixed-corner paths `12`, quadratic bends `0`, failures `0`; text hazards `0`, code without highlight `0`.
- Source-accuracy repairs: replaced the old card flowchart with an actual sequence; showed the final-registered-event check through `TransactionManager`, mapping selection, `toAuditObject` for Created/Updated, `toGlobalId` for Removed, and the two distinct JaVers commit APIs; retained the recursion/closed guard as the subscription responsibility and the application-scope close requirement in the footer.
- Scope note: the footer explicitly excludes raw Exposed DSL writes, external database writes, and CDC streams, matching the module contract.
- Repairs made during audit: extended three activation bars after the endpoint audit found bottom-corner contacts; moved the Created/Updated messages down and right after the first full-size review exposed overlap between the branch title and message 7.
- Original-size eye review: all participant labels fit; twelve numbered pills remain readable and clear their arrows; all message heads are visible and color-matched; Created/Updated and Removed branches are spatially distinct; no route crosses a card or another connector; the branch title, divider, cleanup note, and outer margins have clear separation.
- Manual mirror: pending Task 4.

### `javers-exposed-erd-01`

- Reader question: Which physical columns and keys store JaVers commit metadata and snapshot versions, and how are the two tables logically related?
- Authority: `javers-exposed/README.md` and `JaversExposedTables.kt`.
- Kind: ERD.
- Canonical: `docs/images/readme-diagrams/javers-exposed-erd-01.{svg,png}`.
- XML/render: `xmllint --noout` PASS; CairoSVG `-s 2` produced a `3120x1720` RGBA PNG. The full-size review used `/tmp/javers-exposed-erd-flat-20260717.jpg`.
- Audits: connectors `3`, cards/table groups `3`, intrusions `0`, crossings `0`, shared segments `0`; geometry failures `0`; endpoint PASS; mixed-corner paths `3`, quadratic bends `6`, failures `0`; text hazards `0`, code without highlight `0`.
- ERD fallback inventory: two physical tables, one schema owner card, two schema-ownership connectors, one logical join connector, two visible cardinality labels (`1`, `0..*`), and one relationship label (`logical join, no FK`); label/table/connector overlap count `0` by full-size inspection.
- Source-accuracy result: all production columns, types, nullability, primary keys, the commit sequence index, snapshot query indexes, and the non-enforced `commit_id` logical join match `JaversExposedTables.kt`.
- Repairs made during audit: converted all table, header, badge, divider, and footer surfaces to the shared dark palette; raised connector shafts to 4px; normalized schema and join heads to 10x10; attached explicit card/connector metadata for non-zero audit evidence.
- Original-size eye review: every column and constraint fits its table; PK/IX/JOIN badges remain distinct; the `1` and `0..*` labels identify the correct endpoints without touching the dashed join; the relationship label clears the connector; both ownership routes are separate and visible; no line crosses a table or another route; bottom whitespace is occupied by the query-pushdown summary rather than unused canvas.
- Manual mirror: pending Task 4.

### `javers-exposed-persistence-flow-01`

- Reader question: What does the SQL repository write, and when does a JaVers read use exact SQL pushdown instead of the shared in-memory filter semantics?
- Authority: `javers-exposed/README.md` and `ExposedCdoSnapshotRepository.kt`.
- Kind: sequence.
- Canonical: `docs/images/readme-diagrams/javers-exposed-persistence-flow-01.{svg,png}`.
- XML/render: `xmllint --noout` PASS; CairoSVG `-s 2` produced a `3800x3360` RGBA PNG. Because the app blackened the large single preview, the final full-resolution review used two original-scale crops: `/tmp/javers-exposed-persistence-flow-top-final.jpg` and `/tmp/javers-exposed-persistence-flow-bottom-final.jpg`.
- Audits: sequence-style PASS with four participant headers, four lifelines, six activation bars, seventeen visible numbered message pills, six fixed `userSpaceOnUse` markers, and one styled `alt/else` region; connectors `17`, cards `4`, intrusions `0`, crossings `0`, shared segments `0`; geometry failures `0`; endpoint PASS; mixed-corner paths `17`, quadratic bends `0`, failures `0`; text hazards `0`, code without highlight `0`.
- Source-accuracy repairs: replaced the two-row card flowchart with a write/read sequence; showed codec serialization before the SQL transaction, commit metadata insertion only when absent, snapshot-row persistence, exact SQL filter/page pushdown, decoded selected rows, and the conservative fallback for aggregate, changed-property, commit-property, range, and other unsupported JaVers semantics.
- Repairs made during audit: gave exact and fallback branches distinct cyan versus rose/violet message families; moved message 1 away from the write-frame title and message 13 away from the `else` condition after the first full-size review found both overlaps.
- Original-size eye review: all participant names, phase titles, branch conditions, and seventeen pills fit; every arrowhead is visible and matches its message; the write and read phases are distinct; the exact and fallback paths do not collide; branch labels remain fully readable; no connector crosses a card or another route; footer and outer margins remain balanced.
- Manual mirror: pending Task 4.

### `javers-kafka-repository-map-01`

- Reader question: How do the Spring and vanilla write-only repositories publish snapshot payloads, and how does the separate projector build a durable read model?
- Authority: `javers-persistence-kafka/README.md` and all production sources under `javers-persistence-kafka/src/main/kotlin/io/bluetape4k/javers/kafka`.
- Kind: architecture.
- Canonical: `docs/images/readme-diagrams/javers-kafka-repository-map-01.{svg,png}`.
- XML/render: `xmllint --noout` PASS; CairoSVG `-s 2` produced a `4280x1960` RGBA PNG. The original-size review used `/tmp/javers-kafka-repository-map-final.jpg`.
- Audits: connectors `7`, cards `9`, intrusions `0`, crossings `0`, shared segments `0`; geometry failures `0`; endpoint PASS; mixed-corner paths `7`, quadratic bends `8`, failures `0`; text hazards `0`, code without highlight `0`.
- Source-accuracy repairs: removed the false write-only claim from `AbstractCdoSnapshotRepository`; made `JaversCodecs.String` the encoded record-value step; kept `CdoSnapshotEvent` metadata explicitly in process while only its payload goes on the current Kafka wire; placed the empty/false/zero read contract on both concrete publisher repositories; retained deterministic projector ordering, skip-existing behavior, and post-batch offset commits.
- Repairs made during audit: removed duplicate marker definitions; normalized primary arrowheads to 14x14 and the static dependency head to 10x10; replaced both diagonal publisher routes with separate orthogonal topic ports; replaced the floating frame-to-frame line with a real event-to-projector dependency; separated projector outputs and used a direct horizontal read-repository route to avoid a false shared junction.
- Original-size eye review: every card title and responsibility line fits; all seven arrowheads are visible; publisher-to-topic routes remain distinct; the event-to-projector dependency reaches the projector from above without crossing a card; read-repository and replay-option routes leave separate projector ports; no connector crosses another route or card; lane, footer, and outer-frame margins remain balanced.
- Manual mirror: pending Task 4.

### `javers-kafka-publish-flow-01`

- Reader question: When does a Kafka-backed snapshot write count as accepted, and how do timeout, send failure, and interruption propagate back to JaVers?
- Authority: `KafkaCdoSnapshotRepository.kt`, `VanillaKafkaCdoSnapshotRepository.kt`, `KafkaSnapshotEventPublisher.kt`, `VanillaKafkaSnapshotEventPublisher.kt`, `KafkaSnapshotKeyDiagnostics.kt`, and `KafkaPublishTimeoutSupport.kt`.
- Kind: sequence.
- Canonical: `docs/images/readme-diagrams/javers-kafka-publish-flow-01.{svg,png}`.
- XML/render: `xmllint --noout` PASS; CairoSVG `-s 2` produced a `3600x2800` RGBA PNG. The final full-resolution review used `/tmp/javers-kafka-publish-flow-top-final.jpg` and `/tmp/javers-kafka-publish-flow-bottom-final.jpg` because the app blackened the large single preview.
- Audits: sequence-style PASS with five participant headers, five lifelines, four activation bars, eleven visible numbered message pills, five fixed `userSpaceOnUse` markers, and one styled `alt/else` region; connectors `11`, cards `5`, intrusions `0`, crossings `0`, shared segments `0`; geometry failures `0`; endpoint PASS; mixed-corner paths `11`, quadratic bends `0`, failures `0`; text hazards `0`, code without highlight `0`.
- Source-accuracy repairs: replaced the old two-lane card flowchart with an actual sequence; showed snapshot encoding and event construction before the synchronous publisher call; made Kafka acknowledgement the success gate; showed timeout/send/interruption as a propagated `RuntimeException` that prevents JaVers from advancing that snapshot write; retained Spring explicit/default topic selection, vanilla optional post-ACK flush, interrupt restoration, and fingerprint-only key diagnostics.
- Risk contract: the footer calls out prefix publication during a multi-snapshot commit and the resulting retry-tolerance requirement for consumers and projectors.
- Repairs made during audit: raised all message shafts to 4px and arrowheads to the required 14-unit 10x10 family; added participant roles, activation bars, numbered pills, and a subdued branch frame; widened the acknowledgement branch title after the first original-size preview exposed text beyond its capsule.
- Original-size eye review: all participant titles and roles fit; eleven numbered pills clear their arrows; every arrowhead is visible and color-matched; the acknowledgement and failure branches are spatially distinct; branch titles fit their capsules; no message crosses another connector or participant card; the two-line operational warning fits the footer with balanced margins.
- Manual mirror: pending Task 4.

### `javers-kafka-projection-flow-01`

- Reader question: In what deterministic order does the projector replay Kafka records, avoid duplicates, persist snapshots, and commit consumer offsets?
- Authority: `KafkaCdoSnapshotProjector.kt` and `KafkaCdoSnapshotProjectionOptions.kt`.
- Kind: sequence.
- Canonical: `docs/images/readme-diagrams/javers-kafka-projection-flow-01.{svg,png}`.
- XML/render: `xmllint --noout` PASS; CairoSVG `-s 2` produced a `3600x3000` RGBA PNG. The final full-resolution review used `/tmp/javers-kafka-projection-flow-top.jpg`, `/tmp/javers-kafka-projection-flow-bottom-final.jpg`, and `/tmp/javers-kafka-projection-flow-middle-final.jpg`.
- Audits: sequence-style PASS with five participant headers, five lifelines, four activation bars, eleven visible numbered message pills, five fixed `userSpaceOnUse` markers, and nested loop/alt regions; connectors `11`, cards `5`, intrusions `0`, crossings `0`, shared segments `0`; geometry failures `0`; endpoint PASS; mixed-corner paths `11`, quadratic bends `0`, failures `0`; text hazards `0`, code without highlight `0`.
- Source-accuracy repairs: replaced the old card flowchart with an actual sequence; showed polling followed by partition/offset sorting, `JaversCodecs.String` plus `JsonConverter` decoding, conditional duplicate lookup by GlobalId and commit/version, `projectSnapshot` only for new snapshots, and `commitSync` only for a non-empty fully successful batch when enabled.
- Failure and lifecycle contracts: the footer states that decode/project failures leave offsets unchanged for retry, explains the consecutive-empty-poll stop condition of `replayUntilIdle`, and keeps consumer ownership as the close-behavior boundary.
- Repairs made during audit: used 4px message shafts and required 14-unit 10x10 heads; added explicit participant roles, activations, numbered pills, deterministic loop framing, duplicate-skip branching, and the post-batch commit gate; widened all three branch-title capsules after the first original-size review exposed text overflow; clarified that `loadSnapshots` runs only when skip-existing checks are enabled.
- Original-size eye review: participant titles and roles fit; all eleven pills clear their arrows; every arrowhead is visible and color-matched; the loop, duplicate branch, and commit gate remain visually nested without collisions; branch titles fit their capsules; no message crosses another connector or a participant card; the two-line retry/lifecycle note fits the footer with balanced margins.
- Manual mirror: pending Task 4.

### `javers-redis-repository-map-01`

- Reader question: How do the Lettuce and Redisson adapters implement the shared binary snapshot contract, and where do their write guarantees differ?
- Authority: `javers-persistence-redis/README.md`, `LettuceCdoSnapshotRepository.kt`, and `RedissonCdoSnapshotRepository.kt`.
- Kind: architecture.
- Canonical: `docs/images/readme-diagrams/javers-redis-repository-map-01.{svg,png}`.
- XML/render: `xmllint --noout` PASS; CairoSVG `-s 2` produced a `4280x1960` RGBA PNG. The original-size review used the canonical PNG directly.
- Audits: connectors `11`, cards `11`, intrusions `0`, crossings `0`, shared segments `0`; geometry failures `0`; endpoint PASS; mixed-corner paths `11`, quadratic bends `14`, failures `0`; text hazards `0`, code without highlight `0`.
- Source-accuracy repairs: replaced the generic adapter summaries with the dedicated Lettuce read/transaction connections, lock-serialized `MULTI/EXEC` commit and projection boundary, Redisson `RListMultimap` plus `RMap` layout, and the best-effort multi-structure write risk; retained newest-first reads and restored-head semantics as shared behavior.
- Repairs made during audit: removed duplicate markers; normalized primary heads to 14x14 and inheritance heads to 10x10; connected the abstract base to both concrete repositories; replaced diagonal fan-out with separate orthogonal ports; routed read and write storage paths to different keyspace edges so no line crosses or shares a terminal segment.
- Original-size eye review: every title and responsibility line fits its card; all eleven arrowheads are visible; Lettuce and Redisson inheritance paths reach their concrete repositories; cyan read and amber write paths remain distinct; Redis icons and keyspace labels are readable; no connector crosses another route or card; both footer lines and outer margins remain balanced.
- Manual mirror: pending Task 4.

### `javers-redis-key-layout-01`

- Reader question: Which Redis key families or distributed objects hold snapshots and commit sequences for each adapter?
- Authority: `LettuceCdoSnapshotRepository.kt` and `RedissonCdoSnapshotRepository.kt`.
- Kind: storage layout.
- Canonical: `docs/images/readme-diagrams/javers-redis-key-layout-01.{svg,png}`.
- XML/render: `xmllint --noout` PASS; CairoSVG `-s 2` produced a `3360x1840` RGBA PNG. The original-size review used the canonical PNG directly.
- Audits: connectors `2`, cards/table groups `3`, intrusions `0`, crossings `0`, shared segments `0`; geometry failures `0`; endpoint PASS; mixed-corner paths `2`, quadratic bends `4`, failures `0`; text hazards `0`, code without highlight `0`.
- Source-accuracy repairs: documented all three Lettuce key families with their exact repository-scoped suffixes and values; separated `saveSnapshot` from `persistCommit` transaction semantics; documented both Redisson object names, newest-first reversal, the default JaVers snapshot codec, and the Redisson byte-array storage codec.
- Repairs made during audit: restored readable row text and table dividers after palette conversion; normalized the two summary heads to 10x10 and shafts to 4px; assigned separate summary-card target ports so the dashed routes do not share a segment or arrowhead.
- Original-size eye review: all key names, object names, value descriptions, and operation notes fit their table areas; Redis icons remain crisp; row and divider contrast is sufficient on both cards; both arrowheads are visible and separately connected; no route crosses a card or another connector; the shared invariant and outer margins are balanced.
- Manual mirror: pending Task 4.

### `javers-redis-save-load-flow-01`

- Reader question: How do Redis snapshot commits, projected snapshots, newest-first reads, and head restoration differ between the Lettuce and Redisson adapters?
- Authority: `LettuceCdoSnapshotRepository.kt` and `RedissonCdoSnapshotRepository.kt`.
- Kind: sequence.
- Canonical: `docs/images/readme-diagrams/javers-redis-save-load-flow-01.{svg,png}`.
- XML/render: `xmllint --noout` PASS; CairoSVG `-s 2` produced a `3680x3600` RGBA PNG. The app blackened the final RGBA preview, so the original-size review used `/tmp/javers-redis-save-load-flow-final.jpg`.
- Audits: sequence-style PASS with five participant headers, five lifelines, four activation bars, seventeen visible numbered message pills, five fixed `userSpaceOnUse` markers, and two styled branch regions; connectors `17`, cards `5`, intrusions `0`, crossings `0`, shared segments `0`; geometry failures `0`; endpoint PASS; mixed-corner paths `17`, quadratic bends `0`, failures `0`; text hazards `0`, code without highlight `0`.
- Source-accuracy repairs: replaced the misleading single save/read card chain with an actual sequence; showed encoding before the write boundary, Lettuce `MULTI/EXEC` across snapshot/index and sequence metadata, Redisson snapshot writes before its sequence update, the resulting partial-prefix risk, adapter-specific loading, decoding, newest-first normalization, and maximum-sequence head restoration.
- Repairs made during audit: added sequence-family participant, activation, pill, numbering, branch, and marker metadata; extended the repository activation after the endpoint audit found the final return on its bottom edge; retained separate colors for snapshot bytes, sequence metadata, codec work, and the Redisson risk path.
- Original-size eye review: participant titles and roles fit; all seventeen numbered pills clear their arrows; every arrowhead is visible and color-matched; Lettuce and Redisson write branches are distinct; the read/head region is separated from persistence; no message crosses another connector or participant card; both footer warnings fit with balanced margins.
- Manual mirror: pending Task 4.

### `examples-javers-exposed-ddd-cqrs-flow-01`

- Reader question: How does the example move an order command through the audited Exposed write model, Kafka, and a Redis-only query model?
- Authority: `examples/javers-exposed-ddd/README.md`, `OrderCommandHandler.kt`, `OrderRepository.kt`, `OrderProjectionEventConsumer.kt`, `RedisOrderSummaryProjection.kt`, and `OrderQueryService.kt`.
- Kind: architecture.
- Canonical: `docs/images/readme-diagrams/examples-javers-exposed-ddd-cqrs-flow-01.{svg,png}`.
- XML/render: `xmllint --noout` PASS; CairoSVG `-s 2` produced a `3840x2160` RGBA PNG. The app blackened the final RGBA preview, so the original-size review used `/tmp/examples-javers-exposed-ddd-cqrs-flow-final.jpg`.
- Audits: connectors `9`, cards `10`, intrusions `0`, crossings `0`, shared segments `0`; geometry failures `0`; endpoint PASS; mixed-corner paths `9`, quadratic bends `10`, failures `0`; text hazards `0`, code without highlight `0`.
- Source-accuracy repairs: made the Exposed source-row and JaVers writes an explicit single database transaction, placed domain-event publication after that commit, retained per-order Kafka keys, showed the consumer applying events to the Redis summary, and kept `OrderQueryService` on the Redis-only read path.
- Boundary contract: the footer preserves the example's deliberate exclusions—no HTTP layer and no production outbox—and identifies the H2, Kafka, and Redis test-container boundary.
- Repairs made during audit: merged existing SVG classes instead of emitting duplicate `class` attributes; converted the canvas, lanes, cards, shadows, shafts, and primary arrowheads to the shared dark architecture family; added ten explicit card audit anchors; split the repository transaction explanation into three lines after the first full-size review exposed text against the card edge.
- Original-size eye review: all ten card titles and responsibility lines fit; every arrowhead remains visible and color-matched; command, write/audit, event-stream, and projection/read responsibilities are distinct; the two persistence routes leave separate repository ports; the Kafka-to-consumer route crosses no card; no connector overlaps another route; lane titles, footer, and outer margins remain balanced.
- Manual mirror: pending Task 4.

### `examples-javers-exposed-ddd-sequence-01`

- Reader question: In what order does a command persist the order and JaVers audit, publish Kafka events, update the projection, and serve a Redis-only query?
- Authority: `examples/javers-exposed-ddd/README.md`, `OrderCommandHandler.kt`, `OrderRepository.kt`, `OrderProjectionEventConsumer.kt`, `RedisOrderSummaryProjection.kt`, and `OrderQueryService.kt`.
- Kind: sequence.
- Canonical: `docs/images/readme-diagrams/examples-javers-exposed-ddd-sequence-01.{svg,png}`.
- XML/render: `xmllint --noout` PASS; CairoSVG `-s 2` produced a `3600x2480` RGBA PNG. The full-size review used `/tmp/examples-javers-exposed-ddd-sequence-final.jpg`; after the app blackened a later whole-image preview, the final lower-right coordinate review used `/tmp/examples-javers-exposed-ddd-sequence-bottom-right.jpg`.
- Audits: sequence-style PASS with eight participant headers, eight lifelines, seven activation bars, eleven visible numbered message pills, six fixed `userSpaceOnUse` markers, and one transaction frame; connectors `11`, cards `8`, intrusions `0`, crossings `0`, shared segments `0`; geometry failures `0`; endpoint PASS; mixed-corner paths `11`, quadratic bends `0`, failures `0`; text hazards `0`, code without highlight `0`.
- Source-accuracy repairs: enclosed the order-row and JaVers snapshot operations in one Exposed transaction, kept Kafka publication and acknowledgement after that boundary, separated the Kafka broker from the projection consumer, showed ordered polling before consumer decoding/application, and retained the Redis-only query path.
- Repairs made during audit: converted all sequence surfaces, labels, shafts, heads, activations, and lifelines to the shared dark family; added explicit card/connector metadata; extended handler and repository activations through the publish acknowledgement; moved the transaction title away from the repository activation; widened messages 9 and 10 after the first full-size review exposed text overflow, then narrowed message 10 to restore the outer-frame margin.
- Original-size eye review: all eight participant titles and roles fit; eleven numbered pills clear their message shafts; every arrowhead is visible and color-matched; the database transaction, Kafka publication, projection consumption, and query phases remain distinct; the transaction title clears all activations; the two projection pills fit inside the outer frame; no message crosses another connector or participant card; the ordering note and outer margins remain balanced.
- Manual mirror: pending Task 4.

### `examples-javers-ktor-request-audit-flow-01`

- Reader question: Which Ktor requests write audited state, read the current order row, or read bounded JaVers history?
- Authority: `examples/javers-ktor/README.md`, `JaversKtorExampleApplication.kt`, `OrderCommandHandler.kt`, and `OrderRepository.kt`.
- Kind: architecture.
- Canonical: `docs/images/readme-diagrams/examples-javers-ktor-request-audit-flow-01.{svg,png}`.
- XML/render: `xmllint --noout` PASS; CairoSVG `-s 2` produced a `3920x2240` RGBA PNG. The first final whole-image review used the canonical PNG; after the app blackened the later render, the changed transaction card was rechecked at original scale with `/tmp/examples-javers-ktor-request-audit-flow-card-final.jpg`.
- Audits: connectors `9`, cards `10`, intrusions `0`, crossings `0`, shared segments `0`; geometry failures `0`; endpoint PASS; mixed-corner paths `9`, quadratic bends `13`, failures `0`; text hazards `0`, code without highlight `0`.
- Source-accuracy repairs: distinguished command writes, current-row reads, and bounded history reads; made the command-side save explicitly combine the order row and JaVers snapshot in one Exposed transaction; retained author and domain-event metadata on the snapshot commit; kept current reads on the Exposed table and history reads on JaVers snapshots.
- Repairs made during audit: converted the canvas, lanes, ten cards, shadows, shafts, and primary heads to the shared dark architecture family; normalized residual light Ktor and JaVers cards; attached explicit card/connector metadata; replaced the ambiguous `save aggregate` wording with the actual audited transaction boundary.
- Original-size eye review: all request paths, card titles, endpoint strings, and responsibility lines fit; every arrowhead is visible and color-matched; the write, current-read, and history-read routes remain distinct; both repository read routes use separate ports; no connector crosses another route or card; the history-limit footer and outer margins remain balanced.
- Manual mirror: deferred by the current shared-diagram inventory.
