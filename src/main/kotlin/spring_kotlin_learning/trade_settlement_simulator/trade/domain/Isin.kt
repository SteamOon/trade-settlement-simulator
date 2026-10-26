package spring_kotlin_learning.trade_settlement_simulator.trade.domain

class Isin(raw: String) {
    val value: String = raw.trim().uppercase()

    init {
        require(value.isNotBlank()) { "ISIN must not be blank" }
        require(value.matches(Regex("^[A-Z]{2}[A-Z0-9]{9}[0-9]$"))) {
            "ISIN must be a 12-character code with 2 letters, 9 alphanumeric characters, and a trailing digit, but was '$raw'"
        }
    }

    override fun toString(): String = value
}
