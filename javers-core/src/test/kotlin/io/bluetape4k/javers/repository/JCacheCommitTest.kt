package io.bluetape4k.javers.repository

import io.bluetape4k.cache.jcache.JCaching
import io.bluetape4k.codec.Base58
import io.bluetape4k.javers.repository.jcache.JCacheCdoSnapshotRepository
import org.javers.core.Javers
import org.javers.core.JaversBuilder
import org.javers.core.repository.AbstractJaversCommitTest

class JCacheCommitTest: AbstractJaversCommitTest() {

    override fun newJavers(): Javers {
        val cacheManager = JCaching.Caffeine.cacheManager
        val repo = JCacheCdoSnapshotRepository("jcache-commit-${Base58.randomString(12)}", cacheManager)
        return JaversBuilder.javers()
            .registerJaversRepository(repo)
            .build()
    }
}
