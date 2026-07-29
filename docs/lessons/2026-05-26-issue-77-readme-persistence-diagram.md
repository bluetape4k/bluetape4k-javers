# 2026-05-26 — Issue 77 README 영속성 다이어그램

## 배경

Issue #77에서는 Redis, Kafka, Exposed 영속화 선택지의 관계를 보여 주는
README용 다이어그램을 요청했다.

## 결정

저장소 루트 README의 에셋 경로인 `docs/assets/`를 사용하고, 영문 다이어그램
이미지 하나를 `README.md`와 `README.ko.md`에서 공유한다. 다이어그램에는 현재
소스가 있는 Redis/Kafka 모듈을 표시한다. 아직 `javers-exposed` 모듈이 없으므로
이를 issue #3의 구현 대상으로 표시한다.

## 결과

영속화 선택지 다이어그램의 SVG/PNG 에셋과 Graphviz 레이아웃 검증 자료를
추가하고, 두 루트 README 파일에 PNG를 삽입했다.

## 검증

- SVG에서 `docs/assets/javers-persistence-options.png`를 렌더링했다.
- 렌더링한 PNG를 육안으로 검사했다.
- 최종 SVG와 Graphviz 스케치 SVG에 `xmllint --noout`을 실행했다.
- `git diff --check`를 실행했다.

## 향후 지침

issue #3이 반영되면 다이어그램의 문구를 Exposed 지원 예정에서 구현된 Exposed
JDBC 영속화로 변경한다. 새 아티팩트가 게시되면 README 모듈 표도 업데이트한다.
