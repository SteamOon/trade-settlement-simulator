package spring_kotlin_learning.trade_settlement_simulator.trade.domain

class PartyId(raw: String) {
    val value: String = raw.trim().uppercase()

    init {
        require(value.isNotBlank()) { "Party ID must not be blank" }
        require(value.matches(Regex("^[A-Z0-9][A-Z0-9_-]*$"))) {
            "Party ID must contain only letters, digits, underscores, or dashes, but was '$raw'"
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PartyId) return false
        return value == other.value
    }

    override fun hashCode(): Int = value.hashCode()

    override fun toString(): String = value
}
