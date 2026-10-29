package spring_kotlin_learning.trade_settlement_simulator.ledger.application

import org.springframework.beans.factory.annotation.Autowired
import spring_kotlin_learning.trade_settlement_simulator.BaseIntegrationTest
import spring_kotlin_learning.trade_settlement_simulator.infrastructure.ledger.JournalEntryRepository
import spring_kotlin_learning.trade_settlement_simulator.infrastructure.trade.TradeRepository
import spring_kotlin_learning.trade_settlement_simulator.trade.api.dto.ConfirmationRequest
import spring_kotlin_learning.trade_settlement_simulator.trade.api.dto.CreateTradeRequest
import spring_kotlin_learning.trade_settlement_simulator.trade.application.TradeService
import spring_kotlin_learning.trade_settlement_simulator.trade.domain.TradeSide
import spring_kotlin_learning.trade_settlement_simulator.trade.domain.TradeStatus
import java.math.BigDecimal
import java.time.Clock
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TradeSettlementJobTest @Autowired constructor(
    private val tradeSettlementJob: TradeSettlementJob,
    private val tradeService: TradeService,
    private val tradeRepository: TradeRepository,
    private val journalEntryRepository: JournalEntryRepository,
    private val clock: Clock,
) : BaseIntegrationTest() {

    @Test
    fun `scheduler settles matched trades due for settlement and ignores future trades`() {
        val today = LocalDate.now(clock)

        // 1. Due trade (settlementDate is today)
        val dueTrade = tradeService.createTrade(
            CreateTradeRequest(
                side = TradeSide.BUY,
                isin = "US0378331005",
                quantity = BigDecimal("50.00"),
                price = BigDecimal("100.00"),
                currency = "EUR",
                ourPartyId = "HOUSE-01",
                counterpartyId = "COUNTERPARTY-01",
                tradeDate = today.minusDays(1),
                settlementDate = today,
            ),
            "job-due-1",
        )
        tradeService.confirmTrade(
            dueTrade.id,
            ConfirmationRequest(
                side = TradeSide.SELL,
                isin = "US0378331005",
                quantity = BigDecimal("50.00"),
                price = BigDecimal("100.00"),
                currency = "EUR",
            ),
            "job-due-confirm-1",
        )

        // 2. Future trade (settlementDate is tomorrow)
        val futureTrade = tradeService.createTrade(
            CreateTradeRequest(
                side = TradeSide.BUY,
                isin = "US0378331005",
                quantity = BigDecimal("25.00"),
                price = BigDecimal("100.00"),
                currency = "EUR",
                ourPartyId = "HOUSE-01",
                counterpartyId = "COUNTERPARTY-01",
                tradeDate = today,
                settlementDate = today.plusDays(1),
            ),
            "job-future-1",
        )
        tradeService.confirmTrade(
            futureTrade.id,
            ConfirmationRequest(
                side = TradeSide.SELL,
                isin = "US0378331005",
                quantity = BigDecimal("25.00"),
                price = BigDecimal("100.00"),
                currency = "EUR",
            ),
            "job-future-confirm-1",
        )

        // 3. Unmatched trade (due today, but still PENDING)
        val pendingTrade = tradeService.createTrade(
            CreateTradeRequest(
                side = TradeSide.BUY,
                isin = "US0378331005",
                quantity = BigDecimal("10.00"),
                price = BigDecimal("100.00"),
                currency = "EUR",
                ourPartyId = "HOUSE-01",
                counterpartyId = "COUNTERPARTY-01",
                tradeDate = today.minusDays(1),
                settlementDate = today,
            ),
            "job-pending-1",
        )

        // Execute scheduled batch
        tradeSettlementJob.processDueSettlements()

        // Assert due trade is SETTLED with balanced journal
        val settledTrade = tradeRepository.findById(dueTrade.id).orElseThrow().toDomain()
        assertEquals(TradeStatus.SETTLED, settledTrade.status)
        assertEquals(1, journalEntryRepository.findByTradeId(dueTrade.id).size)

        // Assert future trade remains MATCHED
        val persistedFuture = tradeRepository.findById(futureTrade.id).orElseThrow().toDomain()
        assertEquals(TradeStatus.MATCHED, persistedFuture.status)
        assertTrue(journalEntryRepository.findByTradeId(futureTrade.id).isEmpty())

        // Assert pending trade remains PENDING
        val persistedPending = tradeRepository.findById(pendingTrade.id).orElseThrow().toDomain()
        assertEquals(TradeStatus.PENDING, persistedPending.status)
        assertTrue(journalEntryRepository.findByTradeId(pendingTrade.id).isEmpty())
    }

    @Test
    fun `scheduler isolates failures so one failing trade does not abort remaining batch`() {
        val today = LocalDate.now(clock)

        // Trade 1: will fail settlement due to insufficient cash (COUNTERPARTY-01 needs 100k, only has 25k)
        val failingTrade = tradeService.createTrade(
            CreateTradeRequest(
                side = TradeSide.SELL,
                isin = "US0378331005",
                quantity = BigDecimal("1000.00"),
                price = BigDecimal("100.00"),
                currency = "EUR",
                ourPartyId = "HOUSE-01",
                counterpartyId = "COUNTERPARTY-01",
                tradeDate = today.minusDays(1),
                settlementDate = today,
            ),
            "job-failing-1",
        )
        tradeService.confirmTrade(
            failingTrade.id,
            ConfirmationRequest(
                side = TradeSide.BUY,
                isin = "US0378331005",
                quantity = BigDecimal("1000.00"),
                price = BigDecimal("100.00"),
                currency = "EUR",
            ),
            "job-failing-confirm-1",
        )

        // Trade 2: valid trade that should settle
        val succeedingTrade = tradeService.createTrade(
            CreateTradeRequest(
                side = TradeSide.BUY,
                isin = "US0378331005",
                quantity = BigDecimal("10.00"),
                price = BigDecimal("50.00"),
                currency = "EUR",
                ourPartyId = "HOUSE-01",
                counterpartyId = "COUNTERPARTY-01",
                tradeDate = today.minusDays(1),
                settlementDate = today,
            ),
            "job-succeeding-1",
        )
        tradeService.confirmTrade(
            succeedingTrade.id,
            ConfirmationRequest(
                side = TradeSide.SELL,
                isin = "US0378331005",
                quantity = BigDecimal("10.00"),
                price = BigDecimal("50.00"),
                currency = "EUR",
            ),
            "job-succeeding-confirm-1",
        )

        // Execute scheduled batch
        tradeSettlementJob.processDueSettlements()

        // Assert failing trade moved to FAILED
        val persistedFailing = tradeRepository.findById(failingTrade.id).orElseThrow().toDomain()
        assertEquals(TradeStatus.FAILED, persistedFailing.status)

        // Assert succeeding trade still settled despite previous failure in the same batch
        val persistedSucceeding = tradeRepository.findById(succeedingTrade.id).orElseThrow().toDomain()
        assertEquals(TradeStatus.SETTLED, persistedSucceeding.status)
        assertEquals(1, journalEntryRepository.findByTradeId(succeedingTrade.id).size)
    }
}
