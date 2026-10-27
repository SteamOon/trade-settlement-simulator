package spring_kotlin_learning.trade_settlement_simulator.trade.domain

import spring_kotlin_learning.trade_settlement_simulator.shared.exception.IllegalTradeTransitionException
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

class Trade private constructor(
    val id: UUID,
    val side: TradeSide,
    val isin: Isin,
    val quantity: Quantity,
    val price: Money,
    val ourPartyId: PartyId,
    val counterpartyId: PartyId,
    val tradeDate: LocalDate,
    val settlementDate: LocalDate,
    status: TradeStatus = TradeStatus.PENDING,
    val version: Long = 0L,
) {
    private val _history = mutableListOf<TradeStatusChangedEvent>()

    var status: TradeStatus = status
        private set

    val history: List<TradeStatusChangedEvent>
        get() = _history.toList()

    init {
        recordTransition(null, status, "POST_trades")
    }

    fun confirm(incoming: TradeConfirmation): TradeStatus {
        return when (status) {
            TradeStatus.PENDING -> {
                val nextStatus = if (matchesConfirmation(incoming)) TradeStatus.MATCHED else TradeStatus.UNMATCHED
                val reason = if (nextStatus == TradeStatus.MATCHED) "confirmation_agrees" else mismatchReason(incoming)
                transitionTo(nextStatus, reason)
                status
            }

            TradeStatus.UNMATCHED -> {
                if (matchesConfirmation(incoming)) {
                    transitionTo(TradeStatus.MATCHED, "confirmation_agrees")
                } else {
                    transitionTo(TradeStatus.UNMATCHED, mismatchReason(incoming))
                }
                status
            }

            TradeStatus.MATCHED -> {
                if (matchesConfirmation(incoming)) {
                    return status
                }
                throw IllegalTradeTransitionException("Trade is already matched; confirmation does not match the captured trade details")
            }

            else -> throw IllegalTradeTransitionException("Cannot confirm a trade in $status state")
        }
    }

    fun settle(): TradeStatus {
        return when (status) {
            TradeStatus.MATCHED -> {
                transitionTo(TradeStatus.SETTLED, "settle_success")
                status
            }
            TradeStatus.SETTLED -> status
            else -> throw IllegalTradeTransitionException("Cannot settle a trade in $status state")
        }
    }

    fun fail(reason: String): TradeStatus {
        return when (status) {
            TradeStatus.MATCHED -> {
                transitionTo(TradeStatus.FAILED, reason)
                status
            }
            TradeStatus.FAILED -> status
            else -> throw IllegalTradeTransitionException("Cannot fail a trade in $status state")
        }
    }

    fun retry(): TradeStatus {
        return when (status) {
            TradeStatus.FAILED -> {
                transitionTo(TradeStatus.MATCHED, "retry")
                status
            }
            TradeStatus.MATCHED -> status
            else -> throw IllegalTradeTransitionException("Cannot retry a trade in $status state")
        }
    }

    fun cancel(): TradeStatus {
        return when (status) {
            TradeStatus.FAILED, TradeStatus.UNMATCHED -> {
                transitionTo(TradeStatus.CANCELLED, "cancel")
                status
            }
            TradeStatus.CANCELLED -> status
            else -> throw IllegalTradeTransitionException("Cannot cancel a trade in $status state")
        }
    }

    fun matchesConfirmation(incoming: TradeConfirmation): Boolean {
        return incoming.side == side.opposite()
            && incoming.isin == isin
            && incoming.quantity == quantity
            && incoming.price == price
    }

    private fun mismatchReason(incoming: TradeConfirmation): String {
        val reasons = mutableListOf<String>()

        if (incoming.side != side.opposite()) reasons += "side_mismatch"
        if (incoming.isin != isin) reasons += "isin_mismatch"
        if (incoming.quantity != quantity) reasons += "quantity_mismatch"
        if (incoming.price != price) reasons += "price_mismatch"

        return if (reasons.isEmpty()) "confirmation_disagrees" else reasons.joinToString(",")
    }

    private fun transitionTo(targetStatus: TradeStatus, reason: String?) {
        if (status == targetStatus) {
            return
        }

        val previousStatus = status
        status = targetStatus
        _history.add(
            TradeStatusChangedEvent(
                tradeId = id,
                from = previousStatus,
                to = targetStatus,
                reason = reason,
                occurredAt = Instant.now(),
            )
        )
    }

    private fun recordTransition(fromStatus: TradeStatus?, toStatus: TradeStatus, reason: String?) {
        _history.add(
            TradeStatusChangedEvent(
                tradeId = id,
                from = fromStatus,
                to = toStatus,
                reason = reason,
                occurredAt = Instant.now(),
            )
        )
    }

    companion object {
        fun create(
            side: TradeSide,
            isin: Isin,
            quantity: Quantity,
            price: Money,
            ourPartyId: PartyId,
            counterpartyId: PartyId,
            tradeDate: LocalDate,
            settlementDate: LocalDate = tradeDate.plusDays(1),
            id: UUID = UUID.randomUUID(),
            version: Long = 0L,
        ): Trade {
            return Trade(
                id = id,
                side = side,
                isin = isin,
                quantity = quantity,
                price = price,
                ourPartyId = ourPartyId,
                counterpartyId = counterpartyId,
                tradeDate = tradeDate,
                settlementDate = settlementDate,
                status = TradeStatus.PENDING,
                version = version,
            )
        }

        fun fromPersistence(
            id: UUID,
            side: TradeSide,
            isin: Isin,
            quantity: Quantity,
            price: Money,
            ourPartyId: PartyId,
            counterpartyId: PartyId,
            tradeDate: LocalDate,
            settlementDate: LocalDate,
            status: TradeStatus,
            version: Long = 0L,
            history: List<TradeStatusChangedEvent> = emptyList(),
        ): Trade {
            val trade = Trade(
                id = id,
                side = side,
                isin = isin,
                quantity = quantity,
                price = price,
                ourPartyId = ourPartyId,
                counterpartyId = counterpartyId,
                tradeDate = tradeDate,
                settlementDate = settlementDate,
                status = status,
                version = version,
            )
            trade._history.clear()
            trade._history.addAll(history)
            return trade
        }
    }
}

data class TradeConfirmation(
    val side: TradeSide,
    val isin: Isin,
    val quantity: Quantity,
    val price: Money,
)
