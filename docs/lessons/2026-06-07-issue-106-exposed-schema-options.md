# Issue 106 - Exposed 스키마 옵션

## 배경

Issue #106에서는 `javers-exposed`에 저장소 로컬 Exposed 테이블 매핑 옵션을
추가했다.

## 결정

공개 옵션 표면은 테이블 이름 중심으로 유지하되, `ExposedJaversSchema`를
구성할 때 사용자 지정 테이블 이름에서 사용자 지정 인덱스 이름을 파생한다.

## 결과

초기 H2 테스트에서는 사용자 지정 commit 테이블에 Exposed/H2 unique constraint
이름 충돌이 발생해 실패했다. 사용자 지정 테이블 이름에서 인덱스 이름을
파생함으로써 이 실패를 해결했고, 기본 singleton 테이블의 소스 호환성도
유지했다.

## 검증

- `./gradlew :javers-exposed:test --no-configuration-cache --no-build-cache --console=plain` - PASS, 테스트 31개

## 향후 보호 규칙

Exposed 테이블 이름 사용자 지정 기능을 추가할 때는 같은 변경에서 인덱스와
constraint 이름도 확인한다. 인덱스 이름을 격리하지 않은 테이블 이름 설정
기능은 하나의 데이터베이스 안에서 여전히 충돌할 수 있다.
