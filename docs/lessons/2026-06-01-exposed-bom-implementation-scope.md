# Exposed BOM 구현 범위

## 배경

`bluetape4k-dependencies 1.2.0` 트레인은 `bluetape4k-exposed-bom`을
`1.10.0` 개발선으로 올린다. `javers-exposed`는 bluetape4k Exposed 통합
아티팩트를 사용하지만, BOM 플랫폼 자체가 공용 API에 노출되어서는 안 된다.

## 결정

`javers-exposed`에서 `implementation(platform(...))`으로
`bluetape4k-exposed-bom`을 가져온다.

## 결과

모듈은 트레인 카탈로그와 버전을 맞추면서도 API 범위에 BOM 플랫폼을 노출하지
않는다.

## 검증

- Maven Central에서 `bluetape4k-exposed-bom:1.10.0` 요청에 HTTP 200을
  반환했다.
- `./gradlew :javers-exposed:build --no-daemon --console=plain`이 통과했다.

## 향후 지침

구체적인 공용 Exposed 아티팩트의 타입이 공용 계약에 포함될 때만 `api`에
둔다. bluetape4k Exposed BOM 플랫폼은 내부 의존성으로 유지한다.
