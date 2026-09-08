# Detekt 실행 범위와 기존 위반

배포 모듈 6개를 실제 분석하고 root에서 보고서를 한 번만 합친다. 기존 위반 82개 고유 ID는 모듈 baseline에 고정했다. core의 초기 출력은 중복 ID를 포함해 47개 위반이었다. 새 위반을 추가하면 실패함을 확인했고 제거 후 전체 분석이 통과했다. HTML의 양수 Kotlin 파일 수 및 모듈별 XML과 merged XML이 없으면 CI가 실패한다. 예제와 benchmark는 이번 배포 모듈 분석 범위에 포함하지 않는다. CI에서 baseline을 자동 재생성하지 않는다.

재발 방지: build 성공이나 빈 분석 보고서만으로 변경된 source의 검증을 주장하지 않는다. 변경 입력부터 실제 실행 대상과 결과 artifact까지 연결해 확인한다.

구현 리뷰에서 새 Detekt job이 최종 CI Status의 needs에 빠졌음을 발견했다. 새 검증 job은 실행 존재뿐 아니라 최종 gate의 dependency를 검사한다. 실패 분석 artifact에는 always 조건을 적용한다.
