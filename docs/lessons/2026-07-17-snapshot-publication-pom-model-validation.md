# 스냅샷 배포 POM 모델 검증

## 배경

생성된 `javers-exposed` POM은 버전 없이 `exposed-bom`을 가져왔다. 그 결과
Maven은 해당 dependency-management 항목과 버전이 없는 `exposed-dao`
의존성을 모두 거부했다.

## 결정

Exposed 플랫폼을 가져올 때 `bt4k.exposed.bom`을 사용한다. CI, 스냅샷 배포,
릴리스 배포 전에 모든 배포 POM의 구조를 검증하고 Maven 유효 모델
(effective model) 생성으로도 검증한다.

## 결과

이제 생성된 배포 POM 일곱 개 모두 유효한 Maven 모델을 사용하여 관리 대상
의존성을 해석한다.

## 검증

- `ruby scripts/publication/publication_pom_audit_test.rb`
- `./gradlew generatePomFileForBluetapeJaversPublication -PsnapshotVersion=-SNAPSHOT --no-daemon --no-configuration-cache --no-build-cache`
- `ruby scripts/publication/validate_poms.rb`

## 향후 지침

Gradle 빌드 성공만으로 생성된 Maven POM을 소비할 수 있음이 증명되지는 않는다.
생성된 POM 검증을 배포의 필수 조건으로 취급한다.
