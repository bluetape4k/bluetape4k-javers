# javers-persistence-redis

`javers-persistence-redis` provides Redis, Lettuce, and Redisson snapshot
repositories. Repository head validation is fail-closed, so a corrupt or
rewound head cannot silently erase audit history during rebuild.

- Gradle project: `:javers-persistence-redis`
- Artifact: `io.github.bluetape4k.javers:javers-persistence-redis`
- Source directory: `javers-persistence-redis`
