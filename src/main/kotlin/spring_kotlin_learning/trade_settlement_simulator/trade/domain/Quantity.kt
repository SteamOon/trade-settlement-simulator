package spring_kotlin_learning.trade_settlement_simulator.trade.domain

import java.math.BigDecimal

class Quantity private constructor(val value: BigDecimal) {
    init {
        require(value > BigDecimal.ZERO) { "Quantity must be greater than zero, but was $value" }
    }

    companion object {
        fun of(value: BigDecimal): Quantity = Quantity(value)
    }

    operator fun plus(other: Quantity): Quantity = of(value.add(other.value))

    operator fun minus(other: Quantity): Quantity = of(value.subtract(other.value))

    fun compareTo(other: Quantity): Int = value.compareTo(other.value)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Quantity) return false
        return value.compareTo(other.value) == 0
    }

    override fun hashCode(): Int = value.stripTrailingZeros().hashCode()

    override fun toString(): String = value.toPlainString()
}
