package spring_kotlin_learning.trade_settlement_simulator.ledger.application

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import spring_kotlin_learning.trade_settlement_simulator.infrastructure.trade.TradeRepository
import spring_kotlin_learning.trade_settlement_simulator.trade.domain.TradeStatus
import java.time.Clock
import java.time.LocalDate

@Component
class TradeSettlementJob(
    private val tradeRepository: TradeRepository,
    private val settlementService: SettlementService,
    private val clock: Clock,
) {
    private val logger = LoggerFactory.getLogger(TradeSettlementJob::class.java)

    @Scheduled(fixedDelayString = "\${app.settlement.fixed-delay-ms:60000}")
    fun processDueSettlements() {
        val today = LocalDate.now(clock)
        val dueTrades = tradeRepository.findByStatusAndSettlementDateLessThanEqual(TradeStatus.MATCHED, today)

        dueTrades.forEach { trade ->
            try {
                settlementService.attemptSettlement(trade.id)
            } catch (ex: Exception) {
                logger.warn("Settlement failed for trade {} on {}: {}", trade.id, today, ex.message, ex)
            }
        }
    }
}
