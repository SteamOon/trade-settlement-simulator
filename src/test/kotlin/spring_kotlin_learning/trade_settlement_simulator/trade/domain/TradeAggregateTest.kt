package spring_kotlin_learning.trade_settlement_simulator.trade.domain

import java.math.BigDecimal
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TradeAggregateTest {

    private fun createTrade(): Trade = Trade.create(
        side = TradeSide.BUY,
        isin = Isin("US0378331005"),
        quantity = Quantity.of(BigDecimal("100.00")),
        price = Money.of(BigDecimal("50.25"), "EUR"),
        ourPartyId = PartyId("HOUSE-01"),
        counterpartyId = PartyId("COUNTERPARTY-01"),
        tradeDate = LocalDate.of(2026, 10, 26),
    )

    private fun matchingConfirmation(): TradeConfirmation = TradeConfirmation(
        side = TradeSide.SELL,
        isin = Isin("US0378331005"),
        quantity = Quantity.of(BigDecimal("100.00")),
        price = Money.of(BigDecimal("50.25"), "EUR"),
    )

    @Test
    fun `new trade is pending and records the initial transition`() {
        val trade = createTrade()

        assertEquals(TradeStatus.PENDING, trade.status)
        assertEquals(TradeStatus.PENDING, trade.history.last().to)
    }

    @Test
    fun `matching confirmation moves from pending to matched`() {
        val trade = createTrade()

        trade.confirm(matchingConfirmation())

        assertEquals(TradeStatus.MATCHED, trade.status)
        assertEquals("confirmation_agrees", trade.history.last().reason)
    }

    @Test
    fun `mismatched confirmation moves from pending to unmatched with field-level reason`() {
        val trade = createTrade()

        trade.confirm(
            TradeConfirmation(
                side = TradeSide.BUY,
                isin = Isin("US0378331005"),
                quantity = Quantity.of(BigDecimal("100.00")),
                price = Money.of(BigDecimal("50.25"), "EUR"),
            )
        )

        assertEquals(TradeStatus.UNMATCHED, trade.status)
        assertEquals("side_mismatch", trade.history.last().reason)
    }

    @Test
    fun `unmatched trade can recover with a later matching confirmation`() {
        val trade = createTrade()

        trade.confirm(
            TradeConfirmation(
                side = TradeSide.BUY,
                isin = Isin("US0378331005"),
                quantity = Quantity.of(BigDecimal("100.00")),
                price = Money.of(BigDecimal("50.25"), "EUR"),
            )
        )
        trade.confirm(matchingConfirmation())

        assertEquals(TradeStatus.MATCHED, trade.status)
        assertEquals("confirmation_agrees", trade.history.last().reason)
    }

    @Test
    fun `legal transitions follow the expected lifecycle`() {
        val pendingTrade = createTrade()
        pendingTrade.confirm(matchingConfirmation())
        assertEquals(TradeStatus.MATCHED, pendingTrade.status)

        pendingTrade.settle()
        assertEquals(TradeStatus.SETTLED, pendingTrade.status)

        val failedTrade = createTrade()
        failedTrade.confirm(matchingConfirmation())
        failedTrade.fail("insufficient_cash")
        assertEquals(TradeStatus.FAILED, failedTrade.status)

        failedTrade.retry()
        assertEquals(TradeStatus.MATCHED, failedTrade.status)

        val unmatchedTrade = createTrade()
        unmatchedTrade.confirm(
            TradeConfirmation(
                side = TradeSide.BUY,
                isin = Isin("US0378331005"),
                quantity = Quantity.of(BigDecimal("100.00")),
                price = Money.of(BigDecimal("50.25"), "EUR"),
            )
        )
        unmatchedTrade.cancel()
        assertEquals(TradeStatus.CANCELLED, unmatchedTrade.status)
    }

    @Test
    fun `illegal transitions are rejected with a domain exception`() {
        val pendingTrade = createTrade()
        assertFailsWith<IllegalTradeTransitionException> { pendingTrade.settle() }
        assertFailsWith<IllegalTradeTransitionException> { pendingTrade.cancel() }

        val matchedTrade = createTrade()
        matchedTrade.confirm(matchingConfirmation())
        assertFailsWith<IllegalTradeTransitionException> { matchedTrade.cancel() }
        assertFailsWith<IllegalTradeTransitionException> {
            matchedTrade.confirm(
                TradeConfirmation(
                    side = TradeSide.BUY,
                    isin = Isin("US0378331005"),
                    quantity = Quantity.of(BigDecimal("100.00")),
                    price = Money.of(BigDecimal("50.25"), "EUR"),
                )
            )
        }

        val failedTrade = createTrade()
        failedTrade.confirm(matchingConfirmation())
        failedTrade.fail("insufficient_cash")
        assertFailsWith<IllegalTradeTransitionException> { failedTrade.settle() }
        assertFailsWith<IllegalTradeTransitionException> { failedTrade.confirm(matchingConfirmation()) }
    }

    @Test
    fun `settling before matching is rejected`() {
        val trade = createTrade()

        assertFailsWith<IllegalTradeTransitionException> {
            trade.settle()
        }
    }
}
