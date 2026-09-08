# 이벤트 수가 metadata 보존 계약을 바꾸지 않게 한다

단일 이벤트 attributes는 저장되지만 다중 이벤트는 개수·타입으로 축약되었다.
0·1·N cardinality를 각각 검증해야 공개 문서와 구현의 차이를 찾을 수 있다.

단일 키와 기존 요약 키를 유지하고 다중 이벤트에만 순서별 namespace를 추가했다.
사용자 속성은 각 이벤트의 event namespace 아래에 둔다. 동일 속성명, reserved key,
서로 다른 aggregate ID와 발생 시각, 실제 JaVers commit 저장을 함께 검증한다.

metadata 크기는 조용히 절단하지 않는다. backend 제한과 큰 이벤트 저장은 호출자 책임으로
명시한다. 향후 컬렉션 API는 단일 원소와 다중 원소의 정보 보존을 같은 테스트 표에 포함한다.
