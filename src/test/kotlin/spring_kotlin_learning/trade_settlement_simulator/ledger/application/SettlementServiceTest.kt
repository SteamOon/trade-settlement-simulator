package spring_kotlin_learning.trade_settlement_simulator.ledger.application

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import spring_kotlin_learning.trade_settlement_simulator.PostgresTestcontainersConfiguration
import spring_kotlin_learning.trade_settlement_simulator.infrastructure.ledger.JournalEntryRepository
import spring_kotlin_learning.trade_settlement_simulator.infrastructure.trade.TradeEntity
import spring_kotlin_learning.trade_settlement_simulator.infrastructure.trade.TradeRepository
import spring_kotlin_learning.trade_settlement_simulator.trade.api.dto.CreateTradeRequest
import spring_kotlin_learning.trade_settlement_simulator.trade.application.TradeService
import spring_kotlin_learning.trade_settlement_simulator.trade.domain.TradeSide
import spring_kotlin_learning.trade_settlement_simulator.trade.domain.TradeStatus
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@SpringBootTest
@Import(PostgresTestcontainersConfiguration::class)
class SettlementServiceTest @Autowired constructor(
    private val tradeService: TradeService,
    private val settlementService: SettlementService,
    private val tradeRepository: TradeRepository,
    private val journalEntryRepository: JournalEntryRepository,
) {

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
            spring_kotlin_learning.trade_settlement_simulator.trade.api.dto.ConfirmationRequest(
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

        val journalEntries = journalEntryRepository.findByTradeId(created.id)
        assertEquals(1, journalEntries.size)
        assertEquals(4, journalEntries.first().postings.size)
        assertTrue(journalEntries.first().toDomain().isBalanced())
    }
}
