package spring_kotlin_learning.trade_settlement_simulator.ledger.api.dto

import spring_kotlin_learning.trade_settlement_simulator.infrastructure.ledger.AccountEntity
import spring_kotlin_learning.trade_settlement_simulator.ledger.domain.AccountType
import java.math.BigDecimal
import java.util.UUID

data class AccountResponse(
    val id: UUID,
    val partyId: String,
    val accountType: AccountType,
    val currency: String?,
    val isin: String?,
    val balance: BigDecimal,
) {
    companion object {
        fun fromEntity(entity: AccountEntity): AccountResponse = AccountResponse(
            id = entity.id,
            partyId = entity.partyId,
            accountType = entity.accountType,
            currency = entity.currency,
            isin = entity.isin,
            balance = entity.balance,
        )
    }
}
