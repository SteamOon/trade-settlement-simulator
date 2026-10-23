package spring_kotlin_learning.trade_settlement_simulator.trade.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import java.math.BigDecimal

class MoneyTest {

    @Test
    fun `money supports exact arithmetic within the same currency`() {
        val price = Money.of(BigDecimal("100.50"), "EUR")
        val fee = Money.of(BigDecimal("20.25"), "EUR")

        assertEquals(Money.of(BigDecimal("120.75"), "EUR"), price + fee)
        assertEquals(Money.of(BigDecimal("80.25"), "EUR"), price - fee)
    }

    @Test
    fun `money rejects mixed currencies`() {
        val eur = Money.of(BigDecimal("10.00"), "EUR")
        val usd = Money.of(BigDecimal("10.00"), "USD")

        assertFailsWith<IllegalArgumentException> {
            eur + usd
        }
    }

    @Test
    fun `money normalizes currency to uppercase`() {
        val money = Money.of(BigDecimal("42.00"), "eur")

        assertEquals("EUR", money.currency)
    }
}
