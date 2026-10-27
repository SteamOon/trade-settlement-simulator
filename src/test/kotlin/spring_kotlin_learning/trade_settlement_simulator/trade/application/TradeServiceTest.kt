package spring_kotlin_learning.trade_settlement_simulator.trade.application

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import spring_kotlin_learning.trade_settlement_simulator.PostgresTestcontainersConfiguration
import spring_kotlin_learning.trade_settlement_simulator.trade.api.dto.ConfirmationRequest
import spring_kotlin_learning.trade_settlement_simulator.trade.api.dto.CreateTradeRequest
import spring_kotlin_learning.trade_settlement_simulator.trade.domain.TradeSide
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@SpringBootTest
@Import(PostgresTestcontainersConfiguration::class)
class TradeServiceTest @Autowired constructor(
    private val tradeService: TradeService,
) {

    @Test
    fun `create and fetch a trade`() {
        val request = CreateTradeRequest(
            side = TradeSide.BUY,
            isin = "US0378331005",
            quantity = BigDecimal("100.00"),
            price = BigDecimal("50.25"),
            currency = "EUR",
            ourPartyId = "HOUSE-01",
            counterpartyId = "COUNTERPARTY-01",
            tradeDate = LocalDate.of(2026, 10, 26),
            settlementDate = LocalDate.of(2026, 10, 27),
        )

        val created = tradeService.createTrade(request, "key-create-1")

        assertNotNull(created.id)
        assertEquals("PENDING", created.status.name)

        val loaded = tradeService.getTrade(created.id)
        assertEquals(created.id, loaded.id)
        assertEquals(created.status, loaded.status)
    }

    @Test
    fun `confirmation changes the trade status`() {
        val request = CreateTradeRequest(
            side = TradeSide.BUY,
            isin = "US0378331005",
            quantity = BigDecimal("100.00"),
            price = BigDecimal("50.25"),
            currency = "EUR",
            ourPartyId = "HOUSE-01",
            counterpartyId = "COUNTERPARTY-01",
            tradeDate = LocalDate.of(2026, 10, 26),
            settlementDate = LocalDate.of(2026, 10, 27),
        )

        val created = tradeService.createTrade(request, "key-confirm-1")
        val confirmation = ConfirmationRequest(
            side = TradeSide.SELL,
            isin = "US0378331005",
            quantity = BigDecimal("100.00"),
            price = BigDecimal("50.25"),
            currency = "EUR",
        )

        val confirmed = tradeService.confirmTrade(created.id, confirmation, "key-confirm-2")

        assertEquals("MATCHED", confirmed.status.name)
    }
}
