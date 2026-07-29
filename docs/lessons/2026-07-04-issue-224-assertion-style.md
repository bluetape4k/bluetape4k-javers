# Issue #224 단언문 스타일 정리

## 배경

Issue #224는 컬렉션 크기를 스칼라 값 동등 비교로 검사하는 테스트를 저장소
전체에서 정리하는 작업을 추적한다.

## 결정

`collection.size shouldBeEqualTo n`을 `collection shouldHaveSize n`으로
교체하고, Boolean 동등 비교는 Boolean 전용 bluetape4k 단언 매처로 교체한다.

## 결과

이제 JaVers 테스트 코드는 최신 테스트에서 사용하는 것과 동일한 bluetape4k
단언 스타일을 따른다. 컬렉션에는 직접 컬렉션 매처를 사용하고, 동등 비교가
적절한 곳에는 중위 표기 동등 비교를 사용하며, Boolean 조건식에는 Boolean
전용 매처를 사용한다.

## 검증 근거

- 금지된 단언문 스타일을 검색한 결과 활성 Kotlin 코드에서 일치 항목이 없었다.
- `git diff --check`
- `./gradlew compileTestKotlin --no-configuration-cache`
- `./gradlew test --no-configuration-cache`
- CodeGraph 영향 흐름 검사 결과 프로덕션 흐름은 0개였다.

## 향후 지침

테스트를 수정할 때는 스칼라 값으로 투영하기보다 매처가 의도를 직접 표현하도록
한다. `collection shouldHaveSize n`, `value.shouldBeTrue()`,
`value.shouldBeFalse()`, 중위 표기 `actual shouldBeEqualTo expected`를
사용한다.
