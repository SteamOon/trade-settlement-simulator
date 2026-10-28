package spring_kotlin_learning.trade_settlement_simulator.infrastructure.ledger

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import spring_kotlin_learning.trade_settlement_simulator.ledger.domain.AccountType
import java.util.Optional
import java.util.UUID

@Repository
interface AccountRepository : JpaRepository<AccountEntity, UUID> {
    fun findByPartyIdAndAccountTypeAndCurrency(
        partyId: String,
        accountType: AccountType,
        currency: String,
    ): Optional<AccountEntity>

    fun findByPartyIdAndAccountTypeAndIsin(
        partyId: String,
        accountType: AccountType,
        isin: String,
    ): Optional<AccountEntity>
}
