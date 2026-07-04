package io.bluetape4k.javers.persistence.kafka.repository

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.assertions.shouldHaveSize
import io.bluetape4k.javers.codecs.JaversCodecs
import io.bluetape4k.javers.repository.AbstractCdoSnapshotRepository
import org.javers.core.JaversBuilder
import org.javers.core.model.SnapshotEntity
import org.junit.jupiter.api.Test
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.SendResult
import java.util.concurrent.CompletableFuture

class KafkaCdoSnapshotRepositoryCodecContractTest {

    @Test
    fun `repository publishes snapshots with plain string json codec`() {
        val kafkaTemplate = CapturingKafkaTemplate()
        val repository = KafkaCdoSnapshotRepository(kafkaTemplate)
        val javers = JaversBuilder.javers()
            .registerJaversRepository(repository)
            .build()

        repository.codec() shouldBeEqualTo JaversCodecs.String

        javers.commit("codec-contract", SnapshotEntity(1).apply { intProperty = 1 })

        kafkaTemplate.records shouldHaveSize 1
        val (key, payload) = kafkaTemplate.records.single()

        key shouldBeEqualTo "org.javers.core.model.SnapshotEntity/1"
        payload.shouldNotBeNull()
        JaversCodecs.String.decode(requireNotNull(payload)).shouldNotBeNull()
    }

    private fun AbstractCdoSnapshotRepository<*>.codec(): Any {
        val field = AbstractCdoSnapshotRepository::class.java.getDeclaredField("codec")
        field.isAccessible = true
        return field.get(this)
    }

    private class CapturingKafkaTemplate: KafkaTemplate<String, String>(KafkaProvider.producerFactory) {
        val records = mutableListOf<Pair<String, String?>>()

        init {
            setDefaultTopic(KafkaProvider.TEST_TOPIC)
        }

        override fun sendDefault(key: String, data: String?): CompletableFuture<SendResult<String, String>> {
            records += key to data
            @Suppress("UNCHECKED_CAST")
            return CompletableFuture.completedFuture(null) as CompletableFuture<SendResult<String, String>>
        }
    }
}
