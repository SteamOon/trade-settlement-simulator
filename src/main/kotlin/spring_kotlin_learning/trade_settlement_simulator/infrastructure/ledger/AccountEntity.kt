package spring_kotlin_learning.trade_settlement_simulator.infrastructure.ledger

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.Version
import spring_kotlin_learning.trade_settlement_simulator.ledger.domain.Account
import spring_kotlin_learning.trade_settlement_simulator.ledger.domain.AccountType
import spring_kotlin_learning.trade_settlement_simulator.trade.domain.Isin
import spring_kotlin_learning.trade_settlement_simulator.trade.domain.Money
import spring_kotlin_learning.trade_settlement_simulator.trade.domain.PartyId
import spring_kotlin_learning.trade_settlement_simulator.trade.domain.Quantity
import java.math.BigDecimal
import java.util.UUID

@Entity
@Table(name = "accounts")
class AccountEntity {
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    var id: UUID = UUID.randomUUID()

    @Column(name = "party_id", nullable = false)
    var partyId: String = ""

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 8)
    var accountType: AccountType = AccountType.CASH

    @Column(name = "currency", length = 3)
    var currency: String? = null

    @Column(name = "isin", length = 12)
    var isin: String? = null

    @Column(name = "balance", nullable = false, precision = 19, scale = 4)
    var balance: BigDecimal = BigDecimal.ZERO

    @Version
    @Column(name = "version", nullable = false)
    var version: Long = 0L

    protected constructor()

    constructor(
        id: UUID,
        partyId: String,
        accountType: AccountType,
        currency: String? = null,
        isin: String? = null,
        balance: BigDecimal = BigDecimal.ZERO,
        version: Long = 0L,
    ) {
        this.id = id
        this.partyId = partyId
        this.accountType = accountType
        this.currency = currency
        this.isin = isin
        this.balance = balance
        this.version = version
    }

    fun toDomain(): Account = when (accountType) {
        AccountType.CASH -> {
            val cashCurrency = currency ?: error("Cash account is missing a currency")
            Account.cash(
                id = id,
                partyId = PartyId(partyId),
                currency = cashCurrency,
                balance = Money.of(balance, cashCurrency),
            )
        }

        AccountType.SECURITY -> {
            val securityIsin = isin ?: error("Security account is missing an ISIN")
            Account.security(
                id = id,
                partyId = PartyId(partyId),
                isin = Isin(securityIsin),
                balance = Quantity.of(balance),
            )
        }
    }

    companion object {
        fun fromDomain(account: Account): AccountEntity = AccountEntity(
            id = account.id,
            partyId = account.partyId.value,
            accountType = account.accountType,
            currency = account.currency,
            isin = account.isin?.value,
            balance = account.balance,
            version = 0L,
        )
    }
}
