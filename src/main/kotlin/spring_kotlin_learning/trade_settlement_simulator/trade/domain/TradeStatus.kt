package spring_kotlin_learning.trade_settlement_simulator.trade.domain

enum class TradeStatus(val description: String) {
    PENDING("Awaiting matching"),
	MATCHED("Awaiting settlement")
    SETTLED("Successfully settled"),
    FAILED("Settlement failed"),
	CANCELLED("Inactive")
}
