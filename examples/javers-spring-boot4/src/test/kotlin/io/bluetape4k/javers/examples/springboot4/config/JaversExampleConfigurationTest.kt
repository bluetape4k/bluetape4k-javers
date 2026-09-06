package io.bluetape4k.javers.examples.springboot4.config

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.assertFailsWith
import org.junit.jupiter.api.Test
import org.springframework.mock.env.MockEnvironment

class JaversExampleConfigurationTest {

    private val configuration = JaversExampleConfiguration()

    @Test
    fun `repository options preserve custom table names and migration ownership`() {
        val environment = MockEnvironment()
            .withProperty("javers.example.repository.commit-table-name", "audit_commit")
            .withProperty("javers.example.repository.snapshot-table-name", "audit_snapshot")
            .withProperty("javers.example.repository.create-schema-on-ensure", "false")

        val options = configuration.exampleSnapshotRepositoryOptions(environment)

        options.tableNames.commitTableName shouldBeEqualTo "audit_commit"
        options.tableNames.snapshotTableName shouldBeEqualTo "audit_snapshot"
        options.createSchemaOnEnsure shouldBeEqualTo false
    }

    @Test
    fun `repository schema ownership rejects invalid boolean configuration`() {
        val environment = MockEnvironment()
            .withProperty("javers.example.repository.create-schema-on-ensure", "sometimes")

        assertFailsWith<IllegalStateException> {
            configuration.exampleSnapshotRepositoryOptions(environment)
        }
    }
}
