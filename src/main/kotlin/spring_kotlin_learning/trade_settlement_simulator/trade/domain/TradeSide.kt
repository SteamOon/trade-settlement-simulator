package spring_kotlin_learning.trade_settlement_simulator.trade.domain

enum class TradeSide {
    BUY,
    SELL;

    fun opposite(): TradeSide = if (this == BUY) SELL else BUY
}
