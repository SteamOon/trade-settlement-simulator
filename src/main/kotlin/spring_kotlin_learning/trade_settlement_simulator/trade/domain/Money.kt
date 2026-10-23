package spring_kotlin_learning.trade_settlement_simulator.trade.domain

import java.math.BigDecimal

class Money private constructor(
    val amount: BigDecimal,
    val currency: String,
) {
    init {
        require(amount != null) { "Amount must not be null" }
        require(currency.matches(Regex("^[A-Z]{3}$"))) {
            "Currency must be a valid ISO-4217 code in uppercase, but was '$currency'"
        }
    }

    companion object {
        fun of(amount: BigDecimal, currency: String): Money {
            require(amount != null) { "Amount must not be null" }
            return Money(amount, currency.trim().uppercase())
        }

        fun zero(currency: String): Money = of(BigDecimal.ZERO, currency)
    }

    operator fun plus(other: Money): Money {
        requireSameCurrency(other)
        return of(amount.add(other.amount), currency)
    }

    operator fun minus(other: Money): Money {
        requireSameCurrency(other)
        return of(amount.subtract(other.amount), currency)
    }

    operator fun unaryMinus(): Money = of(amount.negate(), currency)

    fun isZero(): Boolean = amount.compareTo(BigDecimal.ZERO) == 0

    fun isPositive(): Boolean = amount.compareTo(BigDecimal.ZERO) > 0

    fun compareTo(other: Money): Int {
        requireSameCurrency(other)
        return amount.compareTo(other.amount)
    }

    private fun requireSameCurrency(other: Money) {
        require(currency == other.currency) {
            "Cannot perform arithmetic across different currencies: $currency and ${other.currency}"
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Money) return false

        return currency == other.currency && amount.compareTo(other.amount) == 0
    }

    override fun hashCode(): Int {
        var result = amount.stripTrailingZeros().hashCode()
        result = 31 * result + currency.hashCode()
        return result
    }

    override fun toString(): String = "$currency $amount"
}
