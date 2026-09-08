# 외부 aggregate adapter의 완료 경계

명세 검토에서 snapshot의 참조 동일성만으로 임의 객체의 불변성이나 clear 실패 원자성을 보장할 수 없음을
확인했다. generic 객체를 deep-copy하거나 복구 저장소를 새로 만드는 대신 기존 Exposed 계약과 같은
깊은 불변 이벤트·단일 소유자·원자적 clear 전제를 명시한다. callback 상태 예약과 실제 transaction 성공
확인 후 발행 순서를 별도 테스트로 고정한다.

계획 검토는 rollback이 audit 이전에도 발생함과 callback 재진입/취소 객체 보존을 드러냈다. 다음 adapter
작업은 정상 경로뿐 아니라 각 단계 이전·중간·이후 실패표를 먼저 작성한다. 성능 검토는 자동 재시도 없는
O(N) 계약과 호출자 timeout/횟수 제한을 명시하도록 보완했다. 보안 로그는 단계와 예외 클래스만 허용한다.

RED에서는 API가 없어서 컴파일 실패했고 구현 후 원본 Exposed aggregate를 사용하는 신규 12개와 기존
24개 테스트가 통과했다. 이는 DB와 외부 메시지의 분산 원자성을 증명하지 않는다. 각 저장소의 transaction
참여와 at-least-once 소비자 멱등성은 계속 호출자 책임으로 남긴다.

통합 Detekt가 callback 경계의 Throwable catch를 검출했다. 취소와 Error까지 registration을 종료하고 동일 객체를 재전파해야 하므로 해당 helper 한 곳만 이유를 적어 suppress한다. 일반 catch 확대나 baseline 추가로 숨기지 않는다.
