package spring_kotlin_learning.trade_settlement_simulator.ledger.domain

import spring_kotlin_learning.trade_settlement_simulator.trade.domain.Isin
import spring_kotlin_learning.trade_settlement_simulator.trade.domain.Money
import spring_kotlin_learning.trade_settlement_simulator.trade.domain.PartyId
import spring_kotlin_learning.trade_settlement_simulator.trade.domain.Quantity
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class JournalEntryTest {
    @Test
    fun `balanced journal has equal total debit and credit`() {
        val houseCash = Account.cash(
            id = UUID.randomUUID(),
            partyId = PartyId("HOUSE-01"),
            currency = "EUR",
            balance = Money.of(BigDecimal("1000.00"), "EUR"),
        )

        val counterpartyCash = Account.cash(
            id = UUID.randomUUID(),
            partyId = PartyId("COUNTERPARTY-01"),
            currency = "EUR",
            balance = Money.of(BigDecimal("500.00"), "EUR"),
        )

        val houseSecurity = Account.security(
            id = UUID.randomUUID(),
            partyId = PartyId("HOUSE-01"),
            isin = Isin("US0378331005"),
            balance = Quantity.of(BigDecimal("100.00")),
        )

        val counterpartySecurity = Account.security(
            id = UUID.randomUUID(),
            partyId = PartyId("COUNTERPARTY-01"),
            isin = Isin("US0378331005"),
            balance = Quantity.of(BigDecimal("50.00")),
        )

        val journal = JournalEntry.create(
            tradeId = UUID.randomUUID(),
            bookedAt = Instant.now(),
            postings = listOf(
                Posting.debit(houseCash, Money.of(BigDecimal("100.00"), "EUR")),
                Posting.credit(counterpartyCash, Money.of(BigDecimal("100.00"), "EUR")),
                Posting.debit(counterpartySecurity, Quantity.of(BigDecimal("10.00"))),
                Posting.credit(houseSecurity, Quantity.of(BigDecimal("10.00"))),
            ),
        )

        assertEquals(BigDecimal("110.00"), journal.totalDebit())
        assertEquals(BigDecimal("110.00"), journal.totalCredit())
        assertEquals(true, journal.isBalanced())
    }

    @Test
    fun `posting cannot be both debit and credit`() {
        val account = Account.cash(
            id = UUID.randomUUID(),
            partyId = PartyId("HOUSE-01"),
            currency = "EUR",
            balance = Money.of(BigDecimal("1000.00"), "EUR"),
        )

        assertFailsWith<IllegalArgumentException> {
            Posting(accountId = account.id, debit = BigDecimal("10.00"), credit = BigDecimal("10.00"))
        }
    }

    @Test
    fun `accounts support zero balance for both cash and security`() {
        val zeroCash = Account.cash(
            id = UUID.randomUUID(),
            partyId = PartyId("HOUSE-01"),
            currency = "EUR",
            balance = Money.zero("EUR"),
        )
        val zeroSecurity = Account.security(
            id = UUID.randomUUID(),
            partyId = PartyId("COUNTERPARTY-01"),
            isin = Isin("US0378331005"),
            balance = Quantity.zero(),
        )

        assertEquals(true, zeroCash.moneyBalance().isZero())
        assertEquals(true, zeroSecurity.quantityBalance().isZero())
    }
}
