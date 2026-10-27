package spring_kotlin_learning.trade_settlement_simulator.infrastructure.idempotency

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional
import java.util.UUID

@Repository
interface IdempotencyKeyRepository : JpaRepository<IdempotencyKeyEntity, UUID> {
    fun findByTradeIdAndOperationAndKey(tradeId: UUID, operation: String, key: String): Optional<IdempotencyKeyEntity>
}
