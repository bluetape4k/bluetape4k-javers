# README 다이어그램 개선

## 배경

JaVers README 세트의 루트, BOM, core, DDD, Exposed, Redis, Kafka, 예제 모듈을
모듈 단위로 다시 작성하고, 모든 README 다이어그램을
`docs/images/readme-diagrams` 아래의 SVG/PNG 쌍으로 통합했다.

초기 산출물은 PPT처럼 보이는 굵은 선, 작은 카드 라벨, 선 라벨 충돌,
카드에 너무 가까운 꺾임, 레이어 내부 여백 불균형, 실제 아티팩트 단위와 맞지 않는 BOM 설명,
예제 모듈을 라이브러리 제공 기능처럼 보이게 하는 표현 때문에 반복 수정이 필요했다.

## 결정 및 확인 사항

- Graphviz 산출물과 스크립트 대신 직접 편집하는 SVG 원본을 관리한다.
- 카드 라벨은 `Architects Daughter`, 보조 텍스트는 `Comic Mono`를 사용한다.
- Kafka, Redis, DB는 일반 사각형보다 아이콘과 라벨 조합 또는 DB 원기둥 형태로 표시한다.
- README 독자가 실제로 설치하거나 호출하는 아티팩트 단위로 설명한다.
- 선이 복잡하면 삭제하지 않고 카드 위치, 포트, 통로를 먼저 재배치한다.
- 레이어 여백은 외곽 프레임이 아니라 라벨 영역을 제외한 내부 카드 배치를 기준으로 본다.
- 시퀀스 다이어그램은 불투명 레이어를 쓰지 않고 흐름과 분기를 색상과 영역으로 구분한다.
- ERD는 설명 카드보다 테이블명, 컬럼명, 키/인덱스/관계 정보를 우선한다.

## 결과

- 모든 README 다이어그램이 `docs/images/readme-diagrams/readme-*` 형태의 별도 하위 폴더가
  아니라, 저장소 공통 `docs/images/readme-diagrams` 폴더에서 일관된 파일명으로 관리된다.
- 루트/BOM/core/DDD/Exposed/Redis/Kafka/예제 README를 영어/한국어 쌍으로 갱신했다.
- Ktor와 Spring Boot 예제 흐름은 요청, 명령 상태, 감사 경로를 분리해 선 교차를
  제거했다.
- 사용자 검토를 거치며 모듈별 커밋으로 나누어 검토 가능한 이력을 남겼다.

## 검증

- `xmllint --noout docs/images/readme-diagrams/*.svg`
- README 이미지 링크 확인: 모든 README 이미지 링크가 정상적으로 해석됨
- Graphviz 잔여물 확인: `.dot`, `.plain`, `*sketch.svg`, `*graphviz*` 파일 또는 README 참조가 없음
- 사용자 정의 선 교차 확인: 모든 `docs/images/readme-diagrams/*.svg`에서 `crossings=0`이 보고됨
- PNG 콘택트 시트: `/tmp/javers-readme-diagrams-final-contact-sheet.png`를 육안으로 확인함
- `git diff --check`

문서와 이미지만 변경했으므로 Gradle 테스트는 실행하지 않았다.

## 향후 지침

- 한 번에 한 모듈씩 작업하고, 승인을 요청하기 전에 PNG를 렌더링해 확인한 다음 커밋한다.
- 카드 라벨 글꼴 크기를 키운 뒤 텍스트 영역이 커지면 카드 사이 간격을 줄이지 말고 카드나
  다이어그램 너비를 늘린다.
- 흐름 다이어그램을 승인하기 전에 선분 교차 검사를 실행하고, 간선이 밀집된 영역을 직접
  확인한다.
- 모듈 소스가 해당 아티팩트를 실제로 제공하지 않는다면 예제를 BOM이나 라이브러리가
  제공하는 동작으로 설명하지 않는다.
- bluetape4k 스킬을 사용했다면 최종 PR 본문을 DoD 형식으로 유지한다.
