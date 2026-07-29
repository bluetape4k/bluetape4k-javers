# README 다이어그램 이미지 검증

## 배경

bluetape4k-javers의 README 다이어그램을 공용 파스텔 인포그래픽 렌더러로 새로 만들었다. 현재 Mermaid 블록과 git 이력에서 복원한 기존 README 다이어그램 이미지 링크가 작업 대상이었다.

## 결정

README에 표시하는 아티팩트는 PNG를 사용하고, 재사용할 수 있도록 SVG 원본을 PNG 파일 옆에 보관한다. 다이어그램 레이블은 영어로만 작성한다. `Diagram`, `Architecture`, `Sequence Diagram` 같은 일반적인 제목은 모듈을 구체적으로 나타내는 영어 제목으로 교체한다. 영어가 아닌 텍스트를 제거하면서 시퀀스 레이블을 잃은 경우에는 의미 없는 일반 레이블 대신 참여 컴포넌트 이름을 사용한다.

## 결과

- 렌더링된 아티팩트 4개
- PNG 파일 2개
- SVG 원본 파일 2개
- 누락된 README 이미지 링크 없음
- README 파일에 로컬 SVG 이미지 삽입 없음
- 남아 있는 Mermaid 코드 블록 없음
- 형태 검사 대상 없음

## 검증

- `node /Users/debop/work/bluetape4k/.omx/scripts/refine-readme-diagrams.mjs .`
- README 이미지 링크 및 Mermaid 잔여물 검사기
- PNG/SVG 형태 검사기
- 시각적 콘택트 시트 검토: `/tmp/bluetape4k-javers-diagram-review-samples.png`
- `git diff --check`

## 향후 지침

원본 Mermaid 소스를 사용할 수 있다면 이전에 교체된 블록의 git 이력까지 확인해 다시 생성한다. 이미지 크기는 콘텐츠에 맞추고, 가짜 채움 노드를 넣지 않으며, SVG 원본을 보존하고, 게시 전에 샘플 시트를 검토한다.
