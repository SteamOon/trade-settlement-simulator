package spring_kotlin_learning.trade_settlement_simulator.ledger.application

import org.springframework.beans.factory.annotation.Autowired
import spring_kotlin_learning.trade_settlement_simulator.BaseIntegrationTest
import spring_kotlin_learning.trade_settlement_simulator.infrastructure.ledger.JournalEntryRepository
import spring_kotlin_learning.trade_settlement_simulator.infrastructure.trade.TradeEntity
import spring_kotlin_learning.trade_settlement_simulator.infrastructure.trade.TradeRepository
import spring_kotlin_learning.trade_settlement_simulator.trade.api.dto.ConfirmationRequest
import spring_kotlin_learning.trade_settlement_simulator.trade.api.dto.CreateTradeRequest
import spring_kotlin_learning.trade_settlement_simulator.trade.application.TradeService
import spring_kotlin_learning.trade_settlement_simulator.trade.domain.TradeSide
import spring_kotlin_learning.trade_settlement_simulator.trade.domain.TradeStatus
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SettlementServiceTest @Autowired constructor(
    private val tradeService: TradeService,
    private val settlementService: SettlementService,
    private val tradeRepository: TradeRepository,
    private val journalEntryRepository: JournalEntryRepository,
) : BaseIntegrationTest() {

    @Test
    fun `matched trade settles with a balanced four-posting journal`() {
        val created = tradeService.createTrade(
            CreateTradeRequest(
                side = TradeSide.BUY,
                isin = "US0378331005",
                quantity = BigDecimal("100.00"),
                price = BigDecimal("50.25"),
                currency = "EUR",
                ourPartyId = "HOUSE-01",
                counterpartyId = "COUNTERPARTY-01",
                tradeDate = LocalDate.of(2026, 10, 26),
                settlementDate = LocalDate.of(2026, 10, 27),
            ),
            "settle-1",
        )

        val matched = tradeService.confirmTrade(
            created.id,
            ConfirmationRequest(
                side = TradeSide.SELL,
                isin = "US0378331005",
                quantity = BigDecimal("100.00"),
                price = BigDecimal("50.25"),
                currency = "EUR",
            ),
            "settle-confirm-1",
        )

        assertEquals(TradeStatus.MATCHED.name, matched.status.name)

        val status = settlementService.attemptSettlement(created.id)

        assertEquals(TradeStatus.SETTLED, status)

        val persistedTrade = tradeRepository.findById(created.id).orElseThrow().toDomain()
        assertEquals(TradeStatus.SETTLED, persistedTrade.status)
        assertEquals(3, persistedTrade.history.size)
        assertTrue(persistedTrade.history.any { it.to == TradeStatus.MATCHED })
        assertTrue(persistedTrade.history.any { it.to == TradeStatus.SETTLED })

        val journalEntries = journalEntryRepository.findByTradeId(created.id)
        assertEquals(1, journalEntries.size)
        assertEquals(4, journalEntries.first().postings.size)
        assertTrue(journalEntries.first().toDomain().isBalanced())
    }

    @Test
    fun `settlement fails when buyer has insufficient cash balance and creates no journal entries`() {
        val created = tradeService.createTrade(
            CreateTradeRequest(
                side = TradeSide.SELL,
                isin = "US0378331005",
                quantity = BigDecimal("1000.00"),
                price = BigDecimal("50.00"),
                currency = "EUR",
                ourPartyId = "HOUSE-01",
                counterpartyId = "COUNTERPARTY-01",
                tradeDate = LocalDate.of(2026, 10, 26),
                settlementDate = LocalDate.of(2026, 10, 27),
            ),
            "settle-fail-cash-1",
        )

        tradeService.confirmTrade(
            created.id,
            ConfirmationRequest(
                side = TradeSide.BUY,
                isin = "US0378331005",
                quantity = BigDecimal("1000.00"),
                price = BigDecimal("50.00"),
                currency = "EUR",
            ),
            "settle-fail-cash-confirm-1",
        )

        val status = settlementService.attemptSettlement(created.id)

        assertEquals(TradeStatus.FAILED, status)

        val persisted = tradeRepository.findById(created.id).orElseThrow().toDomain()
        assertEquals(TradeStatus.FAILED, persisted.status)
        assertEquals("insufficient_cash_balance", persisted.history.last().reason)

        val journalEntries = journalEntryRepository.findByTradeId(created.id)
        assertTrue(journalEntries.isEmpty(), "No journal entries should be booked for failed settlement")

        // Recovery: retry trade back to MATCHED
        val retried = tradeService.retryTrade(created.id, "settle-retry-1")
        assertEquals(TradeStatus.MATCHED, retried.status)

        val persistedRetried = tradeRepository.findById(created.id).orElseThrow().toDomain()
        assertEquals(TradeStatus.MATCHED, persistedRetried.status)
        assertEquals("retry", persistedRetried.history.last().reason)
    }

    @Test
    fun `settlement fails when seller has insufficient security balance and can be cancelled`() {
        val created = tradeService.createTrade(
            CreateTradeRequest(
                side = TradeSide.BUY,
                isin = "US0378331005",
                quantity = BigDecimal("600.00"),
                price = BigDecimal("10.00"),
                currency = "EUR",
                ourPartyId = "HOUSE-01",
                counterpartyId = "COUNTERPARTY-01",
                tradeDate = LocalDate.of(2026, 10, 26),
                settlementDate = LocalDate.of(2026, 10, 27),
            ),
            "settle-fail-sec-1",
        )

        tradeService.confirmTrade(
            created.id,
            ConfirmationRequest(
                side = TradeSide.SELL,
                isin = "US0378331005",
                quantity = BigDecimal("600.00"),
                price = BigDecimal("10.00"),
                currency = "EUR",
            ),
            "settle-fail-sec-confirm-1",
        )

        val status = settlementService.attemptSettlement(created.id)

        assertEquals(TradeStatus.FAILED, status)

        val persisted = tradeRepository.findById(created.id).orElseThrow().toDomain()
        assertEquals(TradeStatus.FAILED, persisted.status)
        assertEquals("insufficient_security_balance", persisted.history.last().reason)

        val journalEntries = journalEntryRepository.findByTradeId(created.id)
        assertTrue(journalEntries.isEmpty(), "No journal entries should be booked for failed settlement")

        val cancelled = tradeService.cancelTrade(created.id, "settle-cancel-sec-1")
        assertEquals(TradeStatus.CANCELLED, cancelled.status)
    }
}
