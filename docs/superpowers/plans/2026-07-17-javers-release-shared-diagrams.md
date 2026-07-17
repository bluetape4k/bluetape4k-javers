# JaVers release-aligned shared diagrams

## Goal

Reuse README diagrams in the versioned manual without allowing the current
Snapshot model to leak into an older stable manual.

## Version rule

The manual manifest is the authority for both prose and shared diagrams. The
sync contract reads `releaseRef` and `releaseCommit` from
`docs/manual/manifest.yaml`, verifies that the ref resolves to the pinned
commit, and copies selected assets from that Git object rather than from the
working tree.

For manual 0.2, release `0.2.1` contains three reusable README diagram pairs:

- `root-readme-overview-01`
- `bluetape4k-javers-architecture-01`
- `bom-architecture-01`

The other 22 canonical README pairs describe the `0.3-SNAPSHOT` line or were
added after `0.2.1`. They remain `deferred` until a stable release contains
them and the manual manifest advances to that release.

## Current delivery

1. Leave the Snapshot README diagrams unchanged.
2. Copy the three selected SVG/PNG pairs from tag `0.2.1` into
   `docs/manual/assets/readme-diagrams`.
3. Embed those release assets in the English and Korean manual pages.
4. Verify release-ref/commit provenance, release-to-mirror digests, bilingual
   references, manual contracts, and links.

## Future dark-theme transition

Dark styling starts in the Snapshot README assets. After the next stable
release captures those assets, update the manual manifest and selection list,
then regenerate manual mirrors from the new release ref. Do not recolor an
older manual independently.
