package io.bluetape4k.javers.persistence.kafka.projection

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldHaveSize
import io.bluetape4k.codec.Base58
import io.bluetape4k.javers.persistence.kafka.repository.KafkaProvider
import io.bluetape4k.javers.persistence.kafka.repository.VanillaKafkaCdoSnapshotRepository
import io.bluetape4k.javers.persistence.kafka.repository.VanillaKafkaCdoSnapshotRepositoryOptions
import io.bluetape4k.javers.persistence.redis.repository.LettuceCdoSnapshotRepository
import io.bluetape4k.testcontainers.storage.RedisServer
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.javers.core.JaversBuilder
import org.javers.core.model.SnapshotEntity
import org.junit.jupiter.api.Test
import java.time.Duration

class KafkaCdoSnapshotProjectorIntegrationTest {

    @Test
    fun `projector rebuilds Redis projection from vanilla Kafka snapshot stream`() {
        val topic = "javers.projection.${Base58.randomString(8)}"
        val readRepository = LettuceCdoSnapshotRepository(
            name = "projection-${Base58.randomString(8)}",
            client = RedisServer.Launcher.LettuceLib.getRedisClient(),
        )
        val readJavers = JaversBuilder.javers()
            .registerJaversRepository(readRepository)
            .build()
        val writeRepository = VanillaKafkaCdoSnapshotRepository(
            producerConfigs = producerConfigs(),
            options = VanillaKafkaCdoSnapshotRepositoryOptions(
                topic = topic,
                flushAfterSend = true,
            ),
        )
        val writeJavers = JaversBuilder.javers()
            .registerJaversRepository(writeRepository)
            .build()

        try {
            val entity = SnapshotEntity(1).apply { intProperty = 7 }
            val firstSnapshot = writeJavers.commit("projection", entity).snapshots.first()
            entity.intProperty = 11
            val secondCommit = writeJavers.commit("projection", entity)

            val projector = KafkaCdoSnapshotProjector(
                consumerConfigs = consumerConfigs(topic),
                jsonConverter = readJavers.jsonConverter,
                projectionRepository = readRepository,
                options = KafkaCdoSnapshotProjectionOptions(
                    topic = topic,
                    pollTimeout = Duration.ofMillis(250),
                ),
            )
            val firstReplay = projector.use {
                awaitProjection(it, expectedProjected = 2)
            }

            val duplicateProjector = KafkaCdoSnapshotProjector(
                consumerConfigs = consumerConfigs(topic),
                jsonConverter = readJavers.jsonConverter,
                projectionRepository = readRepository,
                options = KafkaCdoSnapshotProjectionOptions(
                    topic = topic,
                    pollTimeout = Duration.ofMillis(250),
                ),
            )
            val duplicateReplay = duplicateProjector.use {
                awaitProjection(it, expectedSkipped = 2)
            }

            firstReplay.projectedSnapshots shouldBeEqualTo 2
            duplicateReplay.skippedSnapshots shouldBeEqualTo 2
            readRepository.getHeadId() shouldBeEqualTo secondCommit.id
            readRepository.loadSnapshots(firstSnapshot.globalId.value()) shouldHaveSize 2
            readRepository.loadSnapshots(firstSnapshot.globalId.value())
                .map { it.commitMetadata.id } shouldBeEqualTo listOf(secondCommit.id, firstSnapshot.commitMetadata.id)
        } finally {
            writeRepository.close()
        }
    }

    private fun awaitProjection(
        projector: KafkaCdoSnapshotProjector,
        expectedProjected: Int = 0,
        expectedSkipped: Int = 0,
    ): KafkaCdoSnapshotProjectionResult {
        var total = KafkaCdoSnapshotProjectionResult()
        repeat(30) {
            total += projector.projectOnce()
            if (total.projectedSnapshots >= expectedProjected && total.skippedSnapshots >= expectedSkipped) {
                return total
            }
        }
        error("Timed out waiting for projection. total=$total")
    }

    private fun producerConfigs(): Map<String, Any> =
        KafkaProvider.producerProperties + mapOf(
            ProducerConfig.CLIENT_ID_CONFIG to "projection-producer-${Base58.randomString(8)}",
        )

    private fun consumerConfigs(topic: String): Map<String, Any> =
        mapOf(
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to KafkaProvider.kafka.bootstrapServers,
            ConsumerConfig.CLIENT_ID_CONFIG to "projection-consumer-${Base58.randomString(8)}",
            ConsumerConfig.GROUP_ID_CONFIG to "projection-${topic}-${Base58.randomString(8)}",
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest",
            ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG to false,
        )
}
