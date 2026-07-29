# 중앙 의존성 거버넌스 동기화

## 배경

하위 저장소의 Dependabot PR이 공유 의존성 버전을 저장소별로 하나씩 변경하면서
bluetape4k 조직 전체에 버전 불일치가 발생했다.

## 결정

공유 의존성 버전은 먼저 `bluetape4k-dependencies`에서 변경한 뒤
`sync-shared-versions.py`로 이 저장소에 반영한다. 또한 이 저장소의
Dependabot은 중앙에서 관리하는 의존성 이름을 무시하도록 설정하여 이후 PR이
중앙 진실 공급원을 거치도록 한다.

## 결과

로컬 버전 카탈로그와 `.github/dependabot.yml`이 중앙 의존성 거버넌스 정책을
따르게 되었다.

## 검증

- 이 저장소에서 `sync-shared-versions.py --write --check --summary` 실행
- 이 저장소에서 `sync-dependabot-ignores.py --write --check --summary` 실행
- `git diff --check`

## 향후 준수 사항

중앙에서 관리하는 의존성을 변경하는 저장소별 Dependabot PR을 병합하지 않는다.
먼저 `bluetape4k-dependencies`를 변경한 뒤 이 저장소를 동기화한다.
