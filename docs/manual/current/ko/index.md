# bluetape4k-javers 1.0.0 현재 매뉴얼

이 문서는 `1.0.0` 릴리스 라인의 현재 한국어·영어 매뉴얼 표면입니다.
`settings.gradle.kts`에 등록된 모듈을 기준으로 작성하므로 예제와 benchmark도
Maven 배포 artifact와 혼동하지 않고 함께 확인할 수 있습니다.

릴리스에 고정한 `0.3.0` 매뉴얼은 기존 `docs/manual/en`과
`docs/manual/ko` 표면에 immutable 상태로 남아 있습니다. 이 current 표면은
release workflow가 사용하는 현재 source tree의 0.3 이후 모듈과 계약을
설명합니다.

## 모듈 지도

- 기반: [javers-core](modules/javers-core.md), [javers-ddd](modules/javers-ddd.md), [소비자 BOM](modules/bluetape4k-javers-bom.md)
- 영속성: [javers-exposed](modules/javers-exposed.md), [javers-persistence-redis](modules/javers-persistence-redis.md), [javers-persistence-kafka](modules/javers-persistence-kafka.md)
- 프레임워크 연동: [Spring Boot 4 자동 구성](modules/javers-spring-boot4-autoconfigure.md)
- 실행 예제: [Exposed DDD](modules/examples-javers-exposed-ddd.md), [Ktor](modules/examples-javers-ktor.md), [Spring Boot 4](modules/examples-javers-spring-boot4.md)
- 증거: [Exposed benchmark](modules/benchmark-javers-exposed-benchmark.md)

배포 좌표는 library와 BOM 모듈에만 기록합니다. 예제와 benchmark는 source
tree 소비자이며 Maven artifact로 배포하지 않습니다.
