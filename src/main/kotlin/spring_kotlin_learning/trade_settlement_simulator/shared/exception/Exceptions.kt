package spring_kotlin_learning.trade_settlement_simulator.trade.domain

class IllegalTradeTransitionException(
    message: String,
    cause: Throwable? = null
) : IllegalStateException(message, cause)
