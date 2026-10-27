package spring_kotlin_learning.trade_settlement_simulator.trade.api.dto

import jakarta.validation.Validation
import spring_kotlin_learning.trade_settlement_simulator.trade.domain.TradeSide
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertTrue

class TradeDtoValidationTest {
    private val validator = Validation.buildDefaultValidatorFactory().validator

    @Test
    fun `create trade request rejects invalid fields`() {
        val request = CreateTradeRequest(
            side = TradeSide.BUY,
            isin = "",
            quantity = BigDecimal.ZERO,
            price = BigDecimal.ZERO,
            currency = "usd",
            ourPartyId = "",
            counterpartyId = "CPTY-1",
            tradeDate = LocalDate.now(),
            settlementDate = null,
        )

        val violations = validator.validate(request)

        assertTrue(violations.any { it.propertyPath.toString() == "isin" })
        assertTrue(violations.any { it.propertyPath.toString() == "quantity" })
        assertTrue(violations.any { it.propertyPath.toString() == "price" })
        assertTrue(violations.any { it.propertyPath.toString() == "currency" })
        assertTrue(violations.any { it.propertyPath.toString() == "ourPartyId" })
    }

    @Test
    fun `confirmation request requires trading details`() {
        val request = ConfirmationRequest(
            side = null,
            isin = "",
            quantity = BigDecimal.ZERO,
            price = BigDecimal.ZERO,
        )

        val violations = validator.validate(request)

        assertTrue(violations.any { it.propertyPath.toString() == "side" })
        assertTrue(violations.any { it.propertyPath.toString() == "isin" })
        assertTrue(violations.any { it.propertyPath.toString() == "quantity" })
        assertTrue(violations.any { it.propertyPath.toString() == "price" })
    }
}
