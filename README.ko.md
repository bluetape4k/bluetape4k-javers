# bluetape4k-javers

[![CI](https://github.com/bluetape4k/bluetape4k-javers/actions/workflows/ci.yml/badge.svg)](https://github.com/bluetape4k/bluetape4k-javers/actions/workflows/ci.yml)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.3-7F52FF?logo=kotlin)](https://kotlinlang.org)
[![JVM](https://img.shields.io/badge/JVM-21-ED8B00?logo=openjdk)](https://openjdk.org)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

[English](./README.md) | 한국어

![bluetape4k JaVers 감사 작업대 일러스트](./docs/assets/javers-workbench.png)

[JaVers](https://javers.org) 객체 감사(audit)와 diff를 위한 Kotlin/JVM 통합 라이브러리입니다.
CDO snapshot과 event-sourced change stream을 위해 Redis, Kafka persistence 선택지를 제공합니다.

## 프로젝트 목적

`bluetape4k-javers`는 JaVers의 기본 in-memory, MongoDB, JDBC 저장소 선택지를 확장합니다.
cache-backed read, Redis persistence, Kafka event stream, 향후 Exposed 기반 repository layer가
필요한 Kotlin 서비스의 audit/diff 인프라를 목표로 합니다.

## 제공 기능

- **JaVers core helper** — extension, codec, cache-backed repository 구성 요소
- **Redis persistence** — Lettuce/Redisson 기반 snapshot 저장 경로
- **Kafka persistence** — event stream 기반 CDO snapshot persistence
- **BOM 지원** — 소비자 dependency version 정렬을 위한 `bluetape4k-javers-bom`
- **구현 backlog** — Exposed persistence, DDD helper, CQRS/Event Sourcing 예제 phase chain

## 아키텍처

![javers Architecture diagram](docs/images/readme-diagrams/bluetape4k-javers-architecture-01.png)

## 모듈

| 모듈 | Artifact | 역할 |
|---|---|---|
| `javers-core` | `io.github.bluetape4k.javers:javers-core` | JaVers extension, codec, cache-backed repository |
| `javers-persistence-redis` | `io.github.bluetape4k.javers:javers-persistence-redis` | Redis/Lettuce/Redisson CDO snapshot persistence |
| `javers-persistence-kafka` | `io.github.bluetape4k.javers:javers-persistence-kafka` | Kafka-backed CDO snapshot persistence (쓰기 전용 이벤트 스트림; 읽기는 항상 빈 결과 반환) |
| `bluetape4k-javers-bom` | `io.github.bluetape4k.javers:bluetape4k-javers-bom` | JaVers artifact 정렬용 consumer BOM |

## 요구사항

- JDK 21+
- Kotlin 2.3+
- JaVers 7.11.0

## 빌드

```bash
./gradlew build -x test
./gradlew build
./gradlew :javers-core:test
./gradlew :javers-persistence-redis:test
./gradlew :javers-persistence-kafka:test
```

## 참고 자료

- [JaVers](https://javers.org)
- [JaVers Feature Overview](https://javers.org/features)
- [JaVers VS Envers Comparison](https://javers.org/blog/2017/12/javers-vs-envers-comparision.html)
- [Using JaVers for Data Model Auditing in Spring Data](https://www.baeldung.com/spring-data-javers-audit)
- [Spring Data에서 데이터 모델 감사를 위해 JaVers 사용](https://recordsoflife.tistory.com/486)
