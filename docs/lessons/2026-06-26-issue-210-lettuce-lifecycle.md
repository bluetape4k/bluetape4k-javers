# 교훈 - Issue 210 Lettuce 저장소 생명주기 (2026-06-26)

관련 이슈: #210
영향받는 모듈: `javers-persistence-redis`, `javers-spring-boot4-autoconfigure`

## 배경

`LettuceCdoSnapshotRepository`는 호출자가 제공한 `RedisClient`를 받지만, 저장소 종료
계약을 명시하지 않은 채 지연 초기화되는 읽기/쓰기 Lettuce 명령 핸들을 생성했다. 따라서
Spring 자동 구성으로 저장소를 빈으로 생성하면 컨텍스트 종료 시 저장소가 만든 연결 자원의
정리 책임이 불분명했다.

## 결정

`RedisClient`의 소유권은 호출자에게 유지하고, 저장소는 해당 클라이언트에서 직접 연
읽기/쓰기 연결만 소유한다. 저장소는 `AutoCloseable`을 구현하고, 초기화된 읽기/쓰기 연결을
멱등하게 닫으며, `RedisClient.shutdown()`은 호출하지 않는다.

## 결과

영속성 테스트는 이제 커밋 과정에서 열린 읽기/쓰기 연결을
`LettuceCdoSnapshotRepository.close()`가 닫는다는 것을 검증한다. Spring Boot 자동 구성
테스트는 컨텍스트 종료 시 저장소 정리를 호출하면서 애플리케이션 소유 `RedisClient`는
종료하지 않는다는 것을 검증한다.

## 검증

- `./gradlew :javers-persistence-redis:test --tests '*LettuceJaversCommitTest*' :javers-spring-boot4-autoconfigure:test --tests '*JaversAutoConfigurationTest*' --no-configuration-cache --no-build-cache --no-parallel --console=plain` - 통과, 테스트 7개 + 18개.
- `./gradlew :javers-persistence-redis:test :javers-spring-boot4-autoconfigure:test --no-configuration-cache --no-build-cache --no-parallel --console=plain` - 통과, 테스트 76개 + 18개.

## 향후 방지 지침

저장소가 호출자 소유 클라이언트를 받으면서 자체 연결이나 프로듀서 핸들을 연다면 소유권
구분을 문서화해야 한다. 자원 정리와 호출자 소유권 보존을 모두 검증하는 종료 테스트도
추가한다.
