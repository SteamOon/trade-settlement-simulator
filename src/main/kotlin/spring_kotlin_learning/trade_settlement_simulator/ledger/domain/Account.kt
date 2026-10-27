package spring_kotlin_learning.trade_settlement_simulator.ledger.domain

import spring_kotlin_learning.trade_settlement_simulator.trade.domain.Isin
import spring_kotlin_learning.trade_settlement_simulator.trade.domain.Money
import spring_kotlin_learning.trade_settlement_simulator.trade.domain.PartyId
import spring_kotlin_learning.trade_settlement_simulator.trade.domain.Quantity
import java.math.BigDecimal
import java.util.UUID

class Account private constructor(
    val id: UUID,
    val partyId: PartyId,
    val accountType: AccountType,
    val currency: String? = null,
    val isin: Isin? = null,
    val balance: BigDecimal,
) {
    init {
        when (accountType) {
            AccountType.CASH -> {
                require(currency != null && currency.matches(Regex("^[A-Z]{3}$"))) {
                    "Cash accounts must specify a valid 3-letter ISO currency code"
                }
                require(isin == null) { "Cash accounts cannot have an ISIN" }
            }
            AccountType.SECURITY -> {
                require(currency == null) { "Security accounts cannot store a currency code" }
                require(isin != null) { "Security accounts must specify an ISIN" }
            }
        }
    }

    fun moneyBalance(): Money {
        require(accountType == AccountType.CASH && currency != null) {
            "This account is not a cash account"
        }
        return Money.of(balance, currency)
    }

    fun quantityBalance(): Quantity {
        require(accountType == AccountType.SECURITY && isin != null) {
            "This account is not a security account"
        }
        return Quantity.of(balance)
    }

    companion object {
        fun cash(
            id: UUID,
            partyId: PartyId,
            currency: String,
            balance: Money,
        ): Account = Account(
            id = id,
            partyId = partyId,
            accountType = AccountType.CASH,
            currency = currency.trim().uppercase(),
            isin = null,
            balance = balance.amount,
        )

        fun security(
            id: UUID,
            partyId: PartyId,
            isin: Isin,
            balance: Quantity,
        ): Account = Account(
            id = id,
            partyId = partyId,
            accountType = AccountType.SECURITY,
            currency = null,
            isin = isin,
            balance = balance.value,
        )
    }
}
