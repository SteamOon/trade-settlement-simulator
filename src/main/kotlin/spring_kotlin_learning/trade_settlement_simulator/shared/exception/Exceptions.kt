package spring_kotlin_learning.trade_settlement_simulator.shared.exception

class IllegalTradeTransitionException(
    message: String,
    cause: Throwable? = null
) : IllegalStateException(message, cause)

class ResourceNotFoundException(
    message: String
) : RuntimeException(message)

class IdempotencyConflictException(
    message: String
) : RuntimeException(message)
