# javers-persistence-redis

`javers-persistence-redis`는 Redis, Lettuce, Redisson snapshot repository를
제공합니다. repository head 검증은 fail-closed이므로 rebuild 중 손상되거나
되감긴 head가 audit history를 조용히 지우지 않습니다.

- Gradle project: `:javers-persistence-redis`
- Artifact: `io.github.bluetape4k.javers:javers-persistence-redis`
- Source directory: `javers-persistence-redis`
