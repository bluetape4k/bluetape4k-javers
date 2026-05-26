package io.bluetape4k.javers.examples.exposedddd.persistence

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.javatime.timestamp

/**
 * Command-side order table for the JaVers + Exposed DDD example.
 */
object OrdersTable: Table("example_order") {
    val id = varchar("id", 64)
    val customerId = varchar("customer_id", 64)
    val status = varchar("status", 32)
    val itemsJson = text("items_json")
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")

    override val primaryKey = PrimaryKey(id, name = "pk_example_order")
}
