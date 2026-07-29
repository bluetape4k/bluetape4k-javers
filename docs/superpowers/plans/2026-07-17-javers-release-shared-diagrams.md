# JaVers release-aligned shared diagrams

## 목표

current Snapshot model이 더 오래된 stable manual로 새어 들어가지 않도록 막으면서,
versioned manual에서 README diagram을 재사용한다.

## Version rule

manual manifest는 prose와 shared diagram 모두의 authority다. sync contract는
`docs/manual/manifest.yaml`에서 `releaseRef`와 `releaseCommit`을 읽고, ref가 pinned
commit으로 resolve되는지 검증한 뒤 working tree가 아니라 해당 Git object에서 선택된
asset을 복사한다.

manual 0.2에서 release `0.2.1`은 세 개의 reusable README diagram pair를 포함한다.

- `root-readme-overview-01`
- `bluetape4k-javers-architecture-01`
- `bom-architecture-01`

다른 22개 canonical README pair는 `0.3-SNAPSHOT` line을 설명하거나 `0.2.1` 이후에
추가됐다. stable release가 이를 포함하고 manual manifest가 해당 release로 advance될
때까지 `deferred`로 남긴다.

## 현재 delivery

1. Snapshot README diagram은 변경하지 않는다.
2. tag `0.2.1`에서 선택된 세 SVG/PNG pair를 `docs/manual/assets/readme-diagrams`로 복사한다.
3. 해당 release asset을 English 및 Korean manual page에 embed한다.
4. release-ref/commit provenance, release-to-mirror digest, bilingual reference, manual contract, link를 검증한다.

## 향후 dark-theme transition

Dark styling은 Snapshot README asset에서 시작된다. 다음 stable release가 해당 asset을
capture한 뒤 manual manifest와 selection list를 갱신하고 새 release ref에서 manual
mirror를 다시 생성한다. older manual을 독립적으로 recolor하지 않는다.
