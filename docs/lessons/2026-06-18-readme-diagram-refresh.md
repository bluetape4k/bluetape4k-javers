# README Diagram Refresh

## Context

JaVers README 세트의 root, BOM, core, DDD, Exposed, Redis, Kafka, example 모듈을
모듈 단위로 다시 작성하고, 모든 README 다이어그램을
`docs/images/readme-diagrams` 아래의 SVG/PNG 쌍으로 통합했다.

초기 산출물은 PPT처럼 보이는 굵은 선, 작은 카드 라벨, line label 충돌,
card 근접 bend, 레이어 내부 여백 불균형, 실제 artifact 단위와 맞지 않는 BOM 설명,
예제 모듈을 라이브러리 제공 기능처럼 보이게 하는 표현 때문에 반복 수정이 필요했다.

## Decision Or Finding

- Graphviz 산출물과 스크립트 대신 source-backed SVG를 직접 관리한다.
- 카드 라벨은 `Architects Daughter`, 보조 텍스트는 `Comic Mono`를 사용한다.
- Kafka, Redis, DB는 일반 사각형보다 icon + label 또는 DB cylinder 형태로 표시한다.
- README 독자가 실제로 설치하거나 호출하는 artifact 단위로 설명한다.
- 선이 복잡하면 삭제하지 않고 card 위치, port, corridor를 먼저 재배치한다.
- layer 여백은 외곽 frame이 아니라 label 영역을 제외한 내부 card 배치를 기준으로 본다.
- sequence diagram은 불투명 layer를 쓰지 않고 흐름과 분기를 색상/영역으로 구분한다.
- ERD는 설명 카드보다 테이블명, 컬럼명, 키/인덱스/관계 정보를 우선한다.

## Outcome

- 모든 README 다이어그램이 `docs/images/readme-diagrams/readme-*` 형태의 별도 하위 폴더가
  아니라, repo 공통 `docs/images/readme-diagrams` 폴더에서 일관된 파일명으로 관리된다.
- root/BOM/core/DDD/Exposed/Redis/Kafka/example README가 English/Korean 쌍으로 갱신됐다.
- Ktor와 Spring Boot example flow는 request, command state, audit path를 분리해 선 교차를
  제거했다.
- 사용자 검토를 거치며 모듈별 커밋으로 쪼개어 review 가능한 history를 남겼다.

## Verification

- `xmllint --noout docs/images/readme-diagrams/*.svg`
- README image link resolver: all README image links resolve
- Graphviz residue check: no `.dot`, `.plain`, `*sketch.svg`, `*graphviz*` files or README references
- Custom line-crossing check: every `docs/images/readme-diagrams/*.svg` reported `crossings=0`
- PNG contact sheet: `/tmp/javers-readme-diagrams-final-contact-sheet.png` visually inspected
- `git diff --check`

Gradle tests were not run because this was a documentation and image-only refresh.

## Future Guidance

- Work one module at a time, render and inspect the PNG before asking for approval, then commit.
- If text grows after increasing card label font size, widen the card or diagram instead of reducing
  spacing between cards.
- Before accepting a flow diagram, run a segment crossing check and then inspect the dense edge cluster
  manually.
- Do not describe examples as BOM-provided or library-provided behavior unless the module source
  actually provides that artifact.
- Keep final PR bodies in DoD form when a bluetape4k skill was active.
