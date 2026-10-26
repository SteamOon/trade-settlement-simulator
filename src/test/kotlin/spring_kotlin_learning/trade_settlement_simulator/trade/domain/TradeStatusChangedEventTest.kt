package spring_kotlin_learning.trade_settlement_simulator.trade.domain

import java.time.Instant
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class TradeStatusChangedEventTest {

    @Test
    fun `status change event captures the lifecycle transition`() {
        val tradeId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000")
        val occurredAt = Instant.parse("2026-10-26T12:00:00Z")

        val event = TradeStatusChangedEvent(
            tradeId = tradeId,
            from = TradeStatus.PENDING,
            to = TradeStatus.MATCHED,
            reason = "confirmation_agrees",
            occurredAt = occurredAt,
        )

        assertEquals(tradeId, event.tradeId)
        assertEquals(TradeStatus.PENDING, event.from)
        assertEquals(TradeStatus.MATCHED, event.to)
        assertEquals("confirmation_agrees", event.reason)
        assertEquals(occurredAt, event.occurredAt)
    }
}
