# Issue 193 검증 팩토리 일관성 확보

## 배경

Issue #193에서는 공개 `copy()` 경로를 노출하면서도 생성자 인자를 `init` 블록에서
검증하던 나머지 데이터 클래스를 확인했다.

## 결정

영향받는 값/애그리거트 타입에는 비공개 생성자, 컴패니언 팩토리와 함께
`@ConsistentCopyVisibility`를 사용한다.

- `ExposedJaversTableNames`
- 예제 `Order` 애그리거트 3개

검증은 컴패니언 팩토리에 유지한다. 잘못된 항목 목록이 일반 생성 경로에서 검증을 우회하지
못하도록 예제 애그리거트의 팩토리를 직접 호출하는 회귀 테스트도 추가한다.

## 검증

- `rg "init \\{|require\\(items" javers-core javers-exposed examples/javers-exposed-ddd examples/javers-spring-boot4 examples/javers-ktor --glob '*.kt'`
- `./gradlew :javers-core:test :javers-exposed:test :examples-javers-exposed-ddd:test :examples-javers-ktor:test :examples-javers-spring-boot4:test --rerun-tasks --no-configuration-cache --no-build-cache --no-parallel --console=plain`

Gradle 실행은 다음 결과로 성공했다.

- `:javers-core:test`: 테스트 191개
- `:javers-exposed:test`: 테스트 53개
- `:examples-javers-exposed-ddd:test`: 테스트 6개
- `:examples-javers-spring-boot4:test`: 테스트 6개
- `:examples-javers-ktor:test`: 테스트 7개

## 후속 작업

일반적인 Gradle 10 지원 중단 요약은 이 이슈의 소스 변경 범위에 포함되지 않는다. 더 넓은
Gradle 릴리스 준비 작업에서 처리해야 한다.
