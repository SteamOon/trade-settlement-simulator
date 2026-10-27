package spring_kotlin_learning.trade_settlement_simulator.infrastructure.trade

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import spring_kotlin_learning.trade_settlement_simulator.trade.domain.TradeStatus
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "trade_status_history")
class TradeStatusHistoryEntity {
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    var id: UUID = UUID.randomUUID()

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trade_id", nullable = false)
    lateinit var trade: TradeEntity

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 16)
    var fromStatus: TradeStatus? = null

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 16)
    lateinit var toStatus: TradeStatus

    @Column(name = "reason")
    var reason: String? = null

    @Column(name = "occurred_at", nullable = false)
    var occurredAt: Instant = Instant.now()

    protected constructor()

    constructor(
        trade: TradeEntity,
        fromStatus: TradeStatus?,
        toStatus: TradeStatus,
        reason: String? = null,
        occurredAt: Instant = Instant.now(),
    ) {
        this.trade = trade
        this.fromStatus = fromStatus
        this.toStatus = toStatus
        this.reason = reason
        this.occurredAt = occurredAt
    }
}
