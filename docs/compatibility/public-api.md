# 공개 Kotlin API 호환성

이 저장소의 Maven Central 배포 대상 Kotlin library는 Kotlin Gradle Plugin의
ABI 검증을 사용합니다. API dump는 Kotlin default argument, companion object,
JVM 생성자와 public/protected 선언을 사람이 읽을 수 있는 baseline으로 보존합니다.

## 검증 대상과 제외 범위

검증 대상은 다음 여섯 모듈입니다.

- `javers-core`
- `javers-ddd`
- `javers-exposed`
- `javers-persistence-kafka`
- `javers-persistence-redis`
- `javers-spring-boot4-autoconfigure`

consumer BOM(`bom/`), 실행 가능한 예제(`examples/`), benchmark(`benchmark/`)는
배포 library ABI 대상이 아니므로 검증에서 제외합니다. 이 모듈들은 각자의
compile/test, publication, benchmark 검증으로 책임을 분리합니다.

## 일반 검증

```bash
./gradlew checkKotlinAbi --no-configuration-cache --no-build-cache --no-parallel
ruby scripts/compatibility/public_abi_fixture_test.rb
```

CI에서는 `checkKotlinAbi`와 fixture 계약 검사를 실행합니다. API가 바뀌면
`check`와 ABI 검증이 실패하며, 기존 baseline을 조용히 덮어쓰지 않습니다.

## 의도한 API 변경 절차

1. 변경 이유와 source/binary compatibility 영향을 PR 본문에 기록합니다.
2. `./gradlew checkKotlinAbi`의 diff를 확인하고, default argument·companion·JVM
   constructor가 기존 호출자를 계속 지원하는지 검토합니다.
3. 승인된 변경일 때만 `./gradlew updateKotlinAbi`를 실행합니다.
4. 변경된 `*/api/*.api` 파일을 구현 변경과 같은 PR에 포함하고, fixture 및 전체
   `./gradlew build` 결과를 함께 확인합니다.
5. 의도하지 않은 public API diff는 baseline을 갱신하지 않고 구현을 수정합니다.

`updateKotlinAbi`는 릴리스나 CI 단계에서 자동 실행하지 않습니다. baseline
갱신은 반드시 리뷰 가능한 diff와 함께 수행해야 합니다.
