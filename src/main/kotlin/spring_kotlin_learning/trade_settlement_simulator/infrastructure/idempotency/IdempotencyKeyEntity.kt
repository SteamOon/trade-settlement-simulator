package spring_kotlin_learning.trade_settlement_simulator.infrastructure.idempotency

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "idempotency_keys",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["trade_id", "operation_name", "key_value"]),
    ],
)
class IdempotencyKeyEntity {
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    var id: UUID = UUID.randomUUID()

    @Column(name = "trade_id", nullable = false)
    var tradeId: UUID = UUID.randomUUID()

    @Column(name = "operation_name", nullable = false)
    var operation: String = ""

    @Column(name = "key_value", nullable = false)
    var key: String = ""

    @Column(name = "request_hash", nullable = false)
    var requestHash: String = ""

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now()

    protected constructor()

    constructor(
        tradeId: UUID,
        operation: String,
        key: String,
        requestHash: String,
        createdAt: Instant = Instant.now(),
        id: UUID = UUID.randomUUID(),
    ) {
        this.id = id
        this.tradeId = tradeId
        this.operation = operation
        this.key = key
        this.requestHash = requestHash
        this.createdAt = createdAt
    }
}
