# Issue 27 Jackson3 Javers Dependencies

## Context

Javers modules consumed `bluetape4k-jackson2` without a parallel Jackson3 path.

## Decision

Add a `bluetape4k-jackson3` catalog alias and switch the Javers core, Kafka,
and Redis persistence modules to that alias.

## Outcome

The affected modules now resolve Jackson support through `bluetape4k-jackson3`.

## Verification

- `./gradlew :javers-core:testClasses :javers-persistence-kafka:testClasses :javers-persistence-redis:testClasses`

## Future Notes

Keep Jackson2/Jackson3 dual-line modules explicit; migrate only single-line
consumers unless both compatibility paths are intentionally maintained.
