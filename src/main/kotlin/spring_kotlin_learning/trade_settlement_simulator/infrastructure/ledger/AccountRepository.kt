package spring_kotlin_learning.trade_settlement_simulator.infrastructure.ledger

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface AccountRepository : JpaRepository<AccountEntity, UUID>
