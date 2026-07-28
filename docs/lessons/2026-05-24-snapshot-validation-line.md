# 스냅숏 검증 버전

## 배경

이전 릴리스 이후 스냅숏 검증을 진행하려면, 일치하는 상위 bluetape4k
스냅숏을 사용하면서 저장소를 다음 개발 버전으로 전환해야 했다.

## 결정

`baseVersion=0.1.3`으로 설정하고 `snapshotVersion=`은 비워 둔 채
`bluetape4k-bom:1.9.2-SNAPSHOT`을 사용한다.

## 결과

`gradle.properties`에 스냅숏 접미사를 커밋하지 않아도 저장소에서
`publish-snapshot.yml`로 `0.1.3-SNAPSHOT`을 게시할 수 있다.

## 검증

스냅숏 검증 트레인에서 검증할 예정이다.
