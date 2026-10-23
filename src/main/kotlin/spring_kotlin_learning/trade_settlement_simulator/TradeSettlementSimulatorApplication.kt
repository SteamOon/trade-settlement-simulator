package spring_kotlin_learning.trade_settlement_simulator

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class TradeSettlementSimulatorApplication

fun main(args: Array<String>) {
	runApplication<TradeSettlementSimulatorApplication>(*args)
}
