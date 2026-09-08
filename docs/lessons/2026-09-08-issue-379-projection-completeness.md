# Snapshot 존재와 projection 완료를 구분한다

기존 Kafka 중복 재생 테스트는 row 수만 확인했다. row만 저장한 fixture에서도 offset이
확정되어 head가 없는 상태를 영구히 건너뛰었다. 새 회귀 테스트는 offset 미확정을 요구하며
기존 코드에서 실패했다.

중복 제거 전에 저장소가 소유한 metadata를 검증한다. sequence를 모르면 과거 순서를
추정하지 않고 재생을 거부한다. 복구는 새 저장소에 전체 스트림을 순서대로 재생한다.
완전한 중복에서는 저장된 sequence로 head를 갱신하며 row를 다시 쓰지 않는다.

향후 테스트는 row, sequence/head, offset을 각각 확인한다. 같은 프로세스에서의 정상
중복뿐 아니라 부분 저장 후 새 저장소 인스턴스의 재시작 경로와 과거 snapshot 재생도 포함한다.
검증 대상은 Kafka projector 회귀 테스트, Caffeine, Redis 저장소 테스트다.
