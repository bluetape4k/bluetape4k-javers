# bluetape4k-javers WIP

> 버전: 1.0.0-SNAPSHOT | 브랜치: `develop`
> 최종 업데이트: 2026-05-08 (이슈 반영: #3 #4 #5)

---

## 우선순위 분류

- 🔴 **High** — 릴리스 전 반드시 처리
- 🟡 **Medium** — 다음 마일스톤 대상
- 🟢 **Low** — 장기 개선 과제

---

## 현재 모듈 구조

| 모듈 | 경로 | 상태 |
|-----|------|------|
| `javers-core` | `javers-core/` | ✅ 완료 (Phase 1) |
| `javers-persistence-kafka` | `javers-persistence-kafka/` | ✅ 완료 (Phase 1) |
| `javers-persistence-redis` | `javers-persistence-redis/` | ✅ 완료 (Phase 1) |

---

## 1. javers-exposed — Exposed JDBC CdoSnapshotRepository 구현 🔴

- Issue: [#3](https://github.com/bluetape4k/bluetape4k-javers/issues/3)
- 원본: bluetape4k-projects [#115](https://github.com/bluetape4k/bluetape4k-projects/issues/115) Phase 2

### 신규 모듈

| 모듈 | 경로 |
|-----|------|
| `javers-exposed` | `javers-exposed/` |

### 구현 요구사항

- [ ] `CdoSnapshotTable` — Exposed Table DSL (global_id, commit_id, version, type, state, changed_props)
- [ ] `CommitTable` — Exposed Table DSL (commit_id, author, commit_date, properties)
- [ ] `ExposedCdoSnapshotRepository` — `JaversRepository` 구현
  - `persist(commit)` — CdoSnapshotTable + CommitTable INSERT
  - `getLatestSnapshot(globalId, javers)` — 최신 snapshot 조회
  - `getSnapshots(query)` — JQL 쿼리 지원
  - `getCommit(commitId)` — CommitTable 조회
- [ ] JSON 직렬화: `bluetape4k-exposed-jackson2` / `bluetape4k-exposed-fastjson2` 활용
- [ ] Javers gson 의존성 → Jackson 코덱 래퍼 (`JaversCodecModule`)
- [ ] DB 초기화 자동화 (`SchemaUtils.create`)
- [ ] `settings.gradle.kts` 등록

### 테스트

- [ ] H2 인메모리 통합 테스트
- [ ] PostgreSQL Testcontainers 통합 테스트
- [ ] MySQL 8 Testcontainers 통합 테스트
- [ ] JQL 쿼리 전체 경우 검증

### 문서

- [ ] KDoc 전체 public API
- [ ] README.md + README.ko.md (Mermaid classDiagram + sequenceDiagram)

#### 참고 자료
- [JaVers Repository SPI](https://javers.org/documentation/repository-configuration/)
- [JaVers JQL 예제](https://javers.org/documentation/jql-examples/)
- bluetape4k-exposed: `exposed-jackson2`, `exposed-fastjson2`, `exposed-jdbc`

---

## 2. javers-ddd — DDD 패턴 헬퍼 🟡

- Issue: [#4](https://github.com/bluetape4k/bluetape4k-javers/issues/4)
- 원본: bluetape4k-projects [#115](https://github.com/bluetape4k/bluetape4k-projects/issues/115) Phase 3
- **선행 조건**: `javers-exposed` (#3) 완료 후 진행

### 신규 모듈

| 모듈 | 경로 |
|-----|------|
| `javers-ddd` | `javers-ddd/` |

### 구현 요구사항

- [ ] `AggregateRoot<ID>` 인터페이스
- [ ] `DomainEvent` sealed class + Javers commit properties 매핑
- [ ] `AggregateRepository<T, ID>` 추상 클래스 (save/load 시 Javers commit 자동 연동)
- [ ] `DomainEventPublisher` 인터페이스 + 구현체 (Spring ApplicationEvent / Kafka / NATS)
- [ ] `@Transactional` 경계 내 원자적 커밋 + 이벤트 발행
- [ ] KDoc + README.md + README.ko.md

#### 참고 자료
- [DDD Aggregate (Martin Fowler)](https://martinfowler.com/bliki/DDD_Aggregate.html)
- [Outbox 패턴](https://microservices.io/patterns/data/transactional-outbox.html)

---

## 3. examples/javers-exposed-ddd — CQRS/Event Sourcing 데모 🟢

- Issue: [#5](https://github.com/bluetape4k/bluetape4k-javers/issues/5)
- 원본: bluetape4k-projects [#115](https://github.com/bluetape4k/bluetape4k-projects/issues/115) Phase 4
- **선행 조건**: `javers-ddd` (#4) 완료 후 진행

### 신규 모듈

| 모듈 | 경로 |
|-----|------|
| `javers-exposed-ddd` (예제) | `examples/javers-exposed-ddd/` |

### 구현 요구사항

- [ ] 주문/재고 도메인 모델 (`Order`, `Inventory` AggregateRoot)
- [ ] Command side: Exposed write + Javers commit + Kafka publish
- [ ] Query side: Kafka consumer → Redis projection
- [ ] Spring Boot 4 자동 구성 (`JaversExposedAutoConfiguration`)
- [ ] JMH 벤치마크: JPA Envers vs Javers+Exposed (INSERT / UPDATE / audit query)
- [ ] Testcontainers: PostgreSQL + Kafka + Redis
- [ ] README.md + README.ko.md (Mermaid sequenceDiagram + flowchart)

#### 참고 자료
- [CQRS 패턴 (Martin Fowler)](https://martinfowler.com/bliki/CQRS.html)
- [JaVers Event Sourcing 블로그](https://javers.org/blog/2016/01/event-sourcing-using-javers.html)

---

## 완료 기준

각 항목은 다음 조건을 모두 만족해야 완료:

- [ ] 코드 변경 완료
- [ ] 단위/통합 테스트 통과 (커버리지 70%+)
- [ ] `ide_diagnostics` 오류 0
- [ ] README.md + README.ko.md 업데이트
- [ ] KDoc 추가/수정 완료
