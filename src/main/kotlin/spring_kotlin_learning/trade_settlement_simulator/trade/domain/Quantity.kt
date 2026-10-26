package spring_kotlin_learning.trade_settlement_simulator.trade.domain

import java.math.BigDecimal

@JvmInline
value class Quantity private constructor(val value: BigDecimal) {
    init {
        require(value > BigDecimal.ZERO) { "Quantity must be greater than zero, but was $value" }
    }

    companion object {
        fun of(value: BigDecimal): Quantity {
            require(value != null) { "Quantity must not be null" }
            return Quantity(value)
        }
    }

    operator fun plus(other: Quantity): Quantity = of(value.add(other.value))

    operator fun minus(other: Quantity): Quantity = of(value.subtract(other.value))

    fun compareTo(other: Quantity): Int = value.compareTo(other.value)

    override fun toString(): String = value.toPlainString()
}
