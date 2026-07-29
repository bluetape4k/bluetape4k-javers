# README 다이어그램 인포그래픽

## 배경

README 파일은 아키텍처, 클래스, 시퀀스, ERD 등의 다이어그램을 Mermaid 코드
블록으로 표현했다. 워크스페이스 전체의 시각 자료 지침이 검토를 거친 파스텔톤
인포그래픽 PNG를 사용하고 재사용할 SVG 원본을 보관하는 방식으로 변경되었다.

## 결정

README의 Mermaid 블록을 생성된 PNG 이미지 링크로 교체하고, 대응하는 SVG
원본을 PNG 파일 옆에 저장한다. 다이어그램 문구는 영어만 사용하고, 큰 레이블에는
Architects Daughter를, 세부 문구에는 Comic Mono를 적용한다. 아키텍처,
클래스, 시퀀스, ERD 다이어그램에는 각각의 유형에 맞는 레이아웃을 사용한다.

## 결과

`bluetape4k.github.io/docs/readme-diagram-samples`의 2026-05-19 공통 스타일
가이드에 따라 README 다이어그램을 렌더링했다. 루트 README의 에셋은 저장소별
배치 규칙이 있으면 해당 규칙을 따른다.

## 검증

저장소 간 변환 작업에서 `rsvg-convert`로 PNG/SVG 에셋을 생성하고 README
링크를 확인했다.

## 향후 지침

README 다이어그램은 PNG로 삽입하고 편집용 SVG 원본을 함께 보관한다. 시각적
일관성이 중요할 때는 원시 Mermaid나 단순한 Mermaid 테마 색상 변경 방식으로
되돌리지 않는다.
