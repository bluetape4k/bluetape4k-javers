# Lettuce 저장소의 terminal close 계약과 Testcontainers 검증

관련 이슈: #306
영향받는 모듈: `javers-persistence-redis`, `javers-spring-boot4-autoconfigure`

## 배경

`LettuceCdoSnapshotRepository`는 read/write connection을 지연 초기화한다. 기존
`close()`는 이미 초기화된 connection만 닫았고, close 이후 operation을 거부하지
않았다. 따라서 첫 사용 전에 닫은 저장소가 다음 read/write에서 connection을 다시
열 수 있었고, 초기화된 저장소도 종료 뒤 접근 계약이 명확하지 않았다.

## 근본 원인

종료 상태가 command delegate보다 늦게 확인되었고, write operation은 command에
접근하기 전에 snapshot을 encode했다. public read path에는 공통 lifecycle guard가
없었으며, `closed`의 가시성도 명시되지 않았다.

## 결정

- `close()`는 terminal lifecycle로 정의한다. close 시작 시 `closed = true`를 먼저
  기록하고 read/write connection을 각각 독립적으로 닫는다.
- 모든 read command getter와 write entry point가 `ensureOpen()`을 통과하게 해
  close 이후 지연 초기화나 encode를 허용하지 않는다.
- `closed`는 `@Volatile`로 공개하고 `close()`는 기존처럼 멱등성을 유지한다.
- 저장소가 소유한 connection만 닫으며 호출자가 소유한 `RedisClient`는 shutdown하지
  않는다.

## 검증

회귀 테스트는 실제 Redis를 띄우는 `bluetape4k-testcontainers`의
`RedisServer.Launcher.LettuceLib` fixture를 사용해 검증했다. 테스트 순서는 Redis
컨테이너 경계를 보존하기 위해 순차 실행했다.

- RED: close 전 첫 read/write가 예외 없이 connection을 다시 열었다.
- RED: write guard를 entry point보다 encode 뒤에 두면 `IllegalStateException` 대신
  malformed snapshot의 `IllegalArgumentException`이 먼저 발생했다.
- GREEN: close-before-first-use read/write가 모두 `IllegalStateException`을 받았다.
- `./gradlew :javers-persistence-redis:test --no-build-cache --no-daemon --console=plain`
  - 83개 통과
- `./gradlew :javers-spring-boot4-autoconfigure:test --no-build-cache --no-daemon --console=plain`
  - 18개 통과

## 향후 지침

호출자 소유 client 위에 지연 초기화된 connection을 만드는 repository의 lifecycle을
변경할 때는 mock 단위 테스트만으로 끝내지 않는다. 최소한
close-before-first-use read/write, 초기화 후 read/write, 멱등 close, caller-owned
client 비종료를 `bluetape4k-testcontainers` 기반 통합 fixture에서 순차 검증하고,
terminal close 계약을 KDoc·README·매뉴얼에 함께 기록한다.
