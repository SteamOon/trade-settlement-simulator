package spring_kotlin_learning.trade_settlement_simulator.trade.domain

import java.math.BigDecimal
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TradeAggregateTest {

    @Test
    fun `new trade is pending and records the initial transition`() {
        val trade = Trade.create(
            side = TradeSide.BUY,
            isin = Isin("US0378331005"),
            quantity = Quantity.of(BigDecimal("100.00")),
            price = Money.of(BigDecimal("50.25"), "EUR"),
            ourPartyId = PartyId("HOUSE-01"),
            counterpartyId = PartyId("COUNTERPARTY-01"),
            tradeDate = LocalDate.of(2026, 10, 26),
        )

        assertEquals(TradeStatus.PENDING, trade.status)
        assertEquals(TradeStatus.PENDING, trade.history.last().to)
    }

    @Test
    fun `matching confirmation moves from pending to matched`() {
        val trade = Trade.create(
            side = TradeSide.BUY,
            isin = Isin("US0378331005"),
            quantity = Quantity.of(BigDecimal("100.00")),
            price = Money.of(BigDecimal("50.25"), "EUR"),
            ourPartyId = PartyId("HOUSE-01"),
            counterpartyId = PartyId("COUNTERPARTY-01"),
            tradeDate = LocalDate.of(2026, 10, 26),
        )

        trade.confirm(
            TradeConfirmation(
                side = TradeSide.SELL,
                isin = Isin("US0378331005"),
                quantity = Quantity.of(BigDecimal("100.00")),
                price = Money.of(BigDecimal("50.25"), "EUR"),
            )
        )

        assertEquals(TradeStatus.MATCHED, trade.status)
        assertEquals("confirmation_agrees", trade.history.last().reason)
    }

    @Test
    fun `settling before matching is rejected`() {
        val trade = Trade.create(
            side = TradeSide.BUY,
            isin = Isin("US0378331005"),
            quantity = Quantity.of(BigDecimal("100.00")),
            price = Money.of(BigDecimal("50.25"), "EUR"),
            ourPartyId = PartyId("HOUSE-01"),
            counterpartyId = PartyId("COUNTERPARTY-01"),
            tradeDate = LocalDate.of(2026, 10, 26),
        )

        assertFailsWith<IllegalTradeTransitionException> {
            trade.settle()
        }
    }
}
