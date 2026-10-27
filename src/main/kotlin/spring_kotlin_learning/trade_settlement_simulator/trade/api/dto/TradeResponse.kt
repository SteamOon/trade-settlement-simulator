package spring_kotlin_learning.trade_settlement_simulator.trade.api.dto

import spring_kotlin_learning.trade_settlement_simulator.trade.domain.TradeStatus
import spring_kotlin_learning.trade_settlement_simulator.trade.domain.TradeStatusChangedEvent
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class TradeResponse(
    val id: UUID,
    val side: String,
    val isin: String,
    val quantity: BigDecimal,
    val price: BigDecimal,
    val currency: String,
    val ourPartyId: String,
    val counterpartyId: String,
    val tradeDate: LocalDate,
    val settlementDate: LocalDate,
    val status: TradeStatus,
    val version: Long,
    val history: List<TradeHistoryResponse>,
)

data class TradeHistoryResponse(
    val tradeId: UUID,
    val from: TradeStatus?,
    val to: TradeStatus,
    val reason: String?,
    val occurredAt: Instant,
) {
    companion object {
        fun from(event: TradeStatusChangedEvent): TradeHistoryResponse = TradeHistoryResponse(
            tradeId = event.tradeId,
            from = event.from,
            to = event.to,
            reason = event.reason,
            occurredAt = event.occurredAt,
        )
    }
}
