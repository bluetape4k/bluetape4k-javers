# README 대표 이미지와 아키텍처 개선

## 배경

JaVers 저장소의 README는 짧은 안내에 그쳐 Redis, Kafka, BOM과 향후
Exposed/DDD 개발 방향을 보여 주지 못했다.

## 결정

생성한 JaVers 감사 작업대 이미지를 `docs/assets/javers-workbench.png`에 저장하고,
두 언어의 README에 프로젝트 목적, 기능, 아키텍처, 모듈 표를 추가하기로 했다.

## 결과

이제 루트 README에서 감사/차이 비교, Redis 영속성, Kafka 영속성, BOM과
단계별 백로그를 한눈에 확인할 수 있다.

## 검증

- 생성된 이미지가 `docs/assets` 아래에 PNG 파일로 존재하는지 확인했다.
- 두 언어의 README가 동일한 이미지 경로를 참조하는지 검증했다.

## 향후 지침

`javers-exposed` 또는 DDD 도우미를 구현할 때는 README 아키텍처, WIP,
`AGENTS.md`, `CLAUDE.md`를 함께 갱신한다.
