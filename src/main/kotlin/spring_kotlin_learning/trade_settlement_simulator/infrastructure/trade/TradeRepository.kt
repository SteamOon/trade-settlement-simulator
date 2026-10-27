package spring_kotlin_learning.trade_settlement_simulator.infrastructure.trade

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface TradeRepository : JpaRepository<TradeEntity, UUID>
