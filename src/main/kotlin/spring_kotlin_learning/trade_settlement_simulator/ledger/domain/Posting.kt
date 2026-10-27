package spring_kotlin_learning.trade_settlement_simulator.ledger.domain

import spring_kotlin_learning.trade_settlement_simulator.trade.domain.Money
import spring_kotlin_learning.trade_settlement_simulator.trade.domain.Quantity
import java.math.BigDecimal
import java.util.UUID

data class Posting(
    val accountId: UUID,
    val debit: BigDecimal = BigDecimal.ZERO,
    val credit: BigDecimal = BigDecimal.ZERO,
) {
    init {
        val hasDebit = debit > BigDecimal.ZERO
        val hasCredit = credit > BigDecimal.ZERO

        require(!(hasDebit && hasCredit) && (hasDebit || hasCredit || (debit == BigDecimal.ZERO && credit == BigDecimal.ZERO))) {
            "Each posting must be either a debit or a credit, never both"
        }
    }

    companion object {
        fun debit(account: Account, amount: Money): Posting = Posting(
            accountId = account.id,
            debit = amount.amount,
            credit = BigDecimal.ZERO,
        )

        fun credit(account: Account, amount: Money): Posting = Posting(
            accountId = account.id,
            debit = BigDecimal.ZERO,
            credit = amount.amount,
        )

        fun debit(account: Account, quantity: Quantity): Posting = Posting(
            accountId = account.id,
            debit = quantity.value,
            credit = BigDecimal.ZERO,
        )

        fun credit(account: Account, quantity: Quantity): Posting = Posting(
            accountId = account.id,
            debit = BigDecimal.ZERO,
            credit = quantity.value,
        )
    }
}
