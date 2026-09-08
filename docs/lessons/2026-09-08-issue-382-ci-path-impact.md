# CI 변경 영향 범위

공통 Gradle 입력은 9개 테스트 job 모두를 선택한다. 모듈 소스는 production뿐 아니라 test dependency를 따라 전파한다. 문서와 benchmark는 전용 정책을 유지한다. 실제 workflow filter를 읽는 8개 회귀 검증과 actionlint가 통과했다.

재발 방지: build 성공이나 빈 분석 보고서만으로 변경된 source의 검증을 주장하지 않는다. 변경 입력부터 실제 실행 대상과 결과 artifact까지 연결해 확인한다.

#385의 shared test source는 세 예제만 선택하는 별도 회귀 단언으로 고정했다.
