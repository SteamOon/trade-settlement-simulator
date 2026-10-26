package spring_kotlin_learning.trade_settlement_simulator.trade.domain

import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ValueObjectTest {

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

    @Test
    fun `isin normalizes case and accepts standard format`() {
        val isin = Isin("us0378331005")
        assertEquals("US0378331005", isin.value)
    }

    @Test
    fun `isin rejects invalid format`() {
        assertFailsWith<IllegalArgumentException> { Isin("ABC") }
    }

    @Test
    fun `quantity stores a positive amount with exact decimal semantics`() {
        val quantity = Quantity.of(BigDecimal("250.00"))
        assertEquals(BigDecimal("250.00"), quantity.value)
        assertEquals(BigDecimal("500.00"), (quantity + Quantity.of(BigDecimal("250.00"))).value)
    }

    @Test
    fun `quantity rejects negative values`() {
        assertFailsWith<IllegalArgumentException> {
            Quantity.of(BigDecimal("-1.00"))
        }
    }

    @Test
    fun `party id normalizes to uppercase and trims whitespace`() {
        val partyId = PartyId("  house-01 ")
        assertEquals("HOUSE-01", partyId.value)
    }
}
