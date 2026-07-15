# Learning path

The shortest useful route depends on what you must build or operate.

## Application developer

Start with [getting started](../getting-started.md) to create one durable repository. Then read [the audit model](../architecture/audit-model.md) to distinguish snapshots from application rows and shadows from live entities. Finish with [DDD and CQRS](ddd-and-cqrs.md): you will see the exact persistence, audit, publication, and projection order and the gaps a production outbox must close.

## Persistence integrator

Read [repository map](../architecture/repository-map.md), then [persistence selection](../persistence/selection-guide.md). The Exposed page teaches schema and transaction ownership; the Redis page separates Lettuce and Redisson; the Kafka page makes the write-only contract explicit. Finish with [failure contracts](../operations/failure-contracts.md) to define retry and reconciliation before wiring two destinations.

## Operator or test engineer

Begin with [observability](../operations/observability.md) to identify commit failures, query cost, Kafka lag, and projection drift. [Testing](testing.md) then maps those risks to unit, database, and Testcontainers checks. The example's [`OrderProjectionFlowTest`](https://github.com/bluetape4k/bluetape4k-javers/blob/bffe19439ca891fa5301a76421bdef7ba75252a0/examples/javers-exposed-ddd/src/test/kotlin/io/bluetape4k/javers/examples/exposedddd/OrderProjectionFlowTest.kt) is the executable end-to-end reference.

For background facilities, continue to the published [Projects manual](https://bluetape4k.github.io/manual/bluetape4k-projects/) and [Exposed manual](https://bluetape4k.github.io/manual/bluetape4k-exposed/). Their generic repository, transaction, Redis, Kafka, and Testcontainers contracts are intentionally not duplicated here.
