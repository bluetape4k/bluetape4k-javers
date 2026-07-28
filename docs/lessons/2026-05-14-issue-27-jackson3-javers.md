# 이슈 27: JaVers의 Jackson3 의존성

## 배경

JaVers 모듈은 `bluetape4k-jackson2`를 사용했으며, 이에 대응하는 Jackson3
경로는 없었다.

## 결정

`bluetape4k-jackson3` 카탈로그 별칭을 추가하고 JaVers 코어, Kafka 영속성,
Redis 영속성 모듈이 이 별칭을 사용하도록 전환하기로 했다.

## 결과

대상 모듈은 이제 `bluetape4k-jackson3`를 통해 Jackson 지원 의존성을 해석한다.

## 검증

- `./gradlew :javers-core:testClasses :javers-persistence-kafka:testClasses :javers-persistence-redis:testClasses`

## 향후 지침

Jackson2/Jackson3를 함께 지원하는 모듈은 두 계열을 명시적으로 유지한다.
두 호환 경로를 의도적으로 유지하는 경우가 아니라면 단일 계열만 사용하는
모듈만 마이그레이션한다.
