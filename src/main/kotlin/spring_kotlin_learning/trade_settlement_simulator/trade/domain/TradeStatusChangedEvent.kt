package spring_kotlin_learning.trade_settlement_simulator.trade.domain

import java.time.Instant
import java.util.UUID

data class TradeStatusChangedEvent(
    val tradeId: UUID,
    val from: TradeStatus?,
    val to: TradeStatus,
    val reason: String? = null,
    val occurredAt: Instant = Instant.now(),
)
