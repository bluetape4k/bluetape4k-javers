# Projects 1.9.2 BOM 인계

## 배경

`bluetape4k-projects` 1.9.2가 릴리스되었고 Maven Central에서
`bluetape4k-bom:1.9.2`을 조회할 수 있다.

## 결정

이 릴리스 준비 브랜치에서는 대응하는 projects 스냅샷 대신 안정 버전인
`bluetape4k-bom` 1.9.2 계열을 사용한다.

## 결과

버전 카탈로그는 이제 이 저장소 자체의 릴리스 계열은 변경하지 않은 채
`io.github.bluetape4k:bluetape4k-bom`을 안정 버전 1.9.2에서 해석한다.

## 검증

- `bluetape4k-bom:1.9.2`에 대한 Maven Central 응답이 HTTP 200임을 확인
- `./gradlew help --refresh-dependencies --no-daemon --no-configuration-cache --no-build-cache`
