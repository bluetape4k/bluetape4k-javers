# JaVers 캐시 파이프라인 조사에서 얻은 교훈

## 배경

JaVers 캐시 및 파이프라인 계획 이슈에는 `bluetape4k-wiki`가 아니라 이 저장소에
지속적으로 보존되는 조사 문서가 필요했다.

## 결정

출처에 근거한 문서를 `docs/research/` 아래에 저장하고 GitHub 이슈에서 연결한다.
향후 구현에서는 JaVers 전용 캐시 추상화를 추가하기 전에
`bluetape4k-projects/cache/*`와
`bluetape4k-exposed/exposed-jdbc-{lettuce,redisson}`을 재사용해야 한다.

## 결과

이 조사는 이제 이슈 #131과 #133-#136에 적용되는 기존 Redis/Lettuce/Redisson,
Exposed 캐시, 파이프라인 및 가상 스레드 제약을 기록한다.

## 검증

- `git diff --check`
- GitHub 이슈 #131, #133, #134에 재사용 및 가상 스레드 제약을 반영함.

## 향후 지침

JaVers 캐시 작업을 계획할 때는 먼저 `exposed-jdbc-lettuce`와
`exposed-jdbc-redisson`을 살펴본다. JDBC 경로는 가상 스레드에 적합하게 유지하고
`bluetape4k-javers` 내부에 새로운 공급자 중립 캐시 계약을 만들지 않는다.
