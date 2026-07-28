# 이슈 #117 - JDK 코덱 사용 제한

## 배경

`JaversCodecs.Jdk`와 압축 기능을 포함한 JDK 기반 별칭은 Java 네이티브
직렬화를 일반적인 코덱 선택지로 노출했다. 신뢰할 수 없는 바이트를 Java로
역직렬화하는 것은 안전하지 않으므로 상위 bluetape4k 직렬화 도구는 사용 중단
상태다.

## 결정

공개 별칭은 호환성 연결 수단으로만 유지하되 각각
`@BluetapeObsoleteApi`와 `@Deprecated(level = DeprecationLevel.ERROR)`로 표시한다.
일반 코덱 테스트는 이제 JDK 역직렬화가 아닌 Kryo/Fory 바이너리 코덱을 검증한다.

## 결과

- 새 Kotlin 호출부는 명시적으로 폐기 API 사용을 허용하지 않으면 JDK 기반
  JaVers 코덱을 사용할 수 없다.
- README와 README.ko는 문자열, Kryo 또는 Fory 코덱 사용을 안내한다.
- 해당 표시가 공개 API의 일부이므로 `javers-core`는
  `bluetape4k-annotations`를 API 의존성으로 노출한다.

## 검증

- `./gradlew :javers-core:compileKotlin --no-configuration-cache --no-build-cache --no-parallel --console=plain`
- `./gradlew :javers-core:test --no-configuration-cache --no-build-cache --no-parallel --console=plain`
- `./gradlew compileKotlin --no-configuration-cache --no-build-cache --no-parallel --console=plain`
- `git diff --check`

## 향후 지침

JDK 기반 페이로드를 역직렬화하는 새 테스트나 예제를 추가하지 않는다. 호환성
테스트가 필요하다면 명시적인 폐기 API 사용 허용 뒤에 격리하고, Kryo/Fory나
JSON으로 같은 동작을 검증할 수 없는 이유를 설명한다.
