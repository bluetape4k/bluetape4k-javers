# 이슈 113 javers-core KDoc 영문화 정리

## 배경

이슈 #113에서는 `javers-core/src/main/kotlin` 아래 프로덕션 소스에 남아 있던
한국어 KDoc과 주석을 발견했다.

## 결정

동작, 시그니처, README 파일 또는 API 구조는 변경하지 않고 외부에 공개되는
프로덕션 KDoc과 주석을 영어로 번역한다.

## 결과

`javers-core/src/main/kotlin`의 base, codec, commit, diff, dispatcher,
repository 및 JQL 확장 API에 남아 있던 한국어 텍스트를 제거했다.

## 검증 증거

- `javers-core/src/main/kotlin` 전체의 한국어 텍스트 검사
- `./gradlew :javers-core:compileKotlin --no-configuration-cache --no-build-cache --console=plain`
- `git diff --check`

## 향후 지침

공개 프로덕션 KDoc은 영어로 유지해야 한다. 내부 문서와 교훈 문서는 한국어로
작성할 수 있지만, 문서 정리 이슈를 종료하기 전에 소스 KDoc을 검사해야 한다.
