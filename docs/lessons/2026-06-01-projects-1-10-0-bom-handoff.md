# bluetape4k-projects 1.10.0 BOM 인계

## 배경

`bluetape4k-projects` 1.10.0이 릴리스되었고 Maven Central에서
`bluetape4k-bom:1.10.0`을 조회할 수 있다.

## 결정

이 저장소 자체의 릴리스 개발선은 그대로 두고, 로컬 카탈로그의 projects BOM
버전을 1.9.2에서 1.10.0으로 올린다.

## 결과

JaVers 빌드는 이제 공통 bluetape4k 모듈 버전을 안정 버전인 projects 1.10.0
BOM에서 가져온다.

## 검증

- Maven Central에서 `bluetape4k-bom:1.10.0` 요청에 HTTP 200을 반환했다.
