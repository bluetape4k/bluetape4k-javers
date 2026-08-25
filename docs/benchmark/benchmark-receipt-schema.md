# Benchmark receipt 계약

이 문서는 `benchmark-javers-exposed-benchmark` smoke 실행이 남기는 JSON
receipt와 teardown failure receipt의 최소 계약입니다. 이 artifact는 성능
수치를 release-wide claim으로 해석하기 위한 것이 아니라, 기대한 scenario와
variant가 실제로 실행됐는지를 증명하기 위한 gate입니다.

## JMH JSON

`build/reports/benchmarks/**/*.json`의 각 파일은 JMH 결과 object 배열이어야
합니다. 각 row는 다음 필드를 포함해야 합니다.

- `benchmark`, `mode`, `params`, `generatedAt`, `sourceCommand`
- 양의 정수 `threads`, `forks`
- `primaryMetric.score`, `primaryMetric.scoreUnit`, 비어 있지 않은 `primaryMetric.rawData`

두 smoke configuration은 다음 조합을 모두 포함해야 합니다.

| benchmark class | parameter | scenario |
|---|---|---|
| `ExposedCommitMetadataIndexBenchmark` | `baseline`, `author`, `commit_date`, `both` | `insert`, `authorQuery`, `dateRangeQuery` |
| `EnversComparisonBenchmark` | `envers`, `javers_in_memory`, `javers_exposed_repository`, `javers_exposed_ddd` | `insert`, `update`, `auditQuery` |

`scripts/benchmark/benchmark_receipt.rb`가 이 조합과 row completeness를
검사하며, JSON이 없거나 비어 있거나 일부 조합이 빠지면 실패합니다.

## Teardown failure JSONL

benchmark resource 정리 실패는 JMH 결과를 덮어쓰지 않고
`build/reports/benchmarks/teardown-failures.jsonl`에 한 줄씩 기록합니다.
각 줄에는 `timestamp`, `owner`, `resource`, `message`, `exception`이
포함됩니다. validator는 이 파일이 존재하거나 malformed line이 있으면
실패하므로 schema drop/connection close 오류를 조용히 삼킬 수 없습니다.

CI와 Nightly는 receipt validator를 실행한 뒤 `build/reports/benchmarks/`
전체를 artifact로 업로드합니다. 따라서 benchmark 실행이 만든 결과와
teardown 진단을 같은 hosted receipt로 재검증할 수 있습니다.
