package spring_kotlin_learning.trade_settlement_simulator.infrastructure.trade

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import spring_kotlin_learning.trade_settlement_simulator.trade.domain.Isin
import spring_kotlin_learning.trade_settlement_simulator.trade.domain.Money
import spring_kotlin_learning.trade_settlement_simulator.trade.domain.PartyId
import spring_kotlin_learning.trade_settlement_simulator.trade.domain.Quantity
import java.math.BigDecimal

@Embeddable
class IsinEmbeddable {
    @Column(name = "isin", nullable = false, length = 12)
    var value: String = ""

    protected constructor()

    constructor(value: String) {
        this.value = value
    }

    fun toDomain(): Isin = Isin(value)

    companion object {
        fun fromDomain(isin: Isin): IsinEmbeddable = IsinEmbeddable(isin.value)
    }
}

@Embeddable
class PartyIdEmbeddable {
    @Column(name = "party_id", nullable = false)
    var value: String = ""

    protected constructor()

    constructor(value: String) {
        this.value = value
    }

    fun toDomain(): PartyId = PartyId(value)

    companion object {
        fun fromDomain(partyId: PartyId): PartyIdEmbeddable = PartyIdEmbeddable(partyId.value)
    }
}

@Embeddable
class QuantityEmbeddable {
    @Column(name = "quantity", nullable = false, precision = 19, scale = 4)
    var value: BigDecimal = BigDecimal.ZERO

    protected constructor()

    constructor(value: BigDecimal) {
        this.value = value
    }

    fun toDomain(): Quantity = Quantity.of(value)

    companion object {
        fun fromDomain(quantity: Quantity): QuantityEmbeddable = QuantityEmbeddable(quantity.value)
    }
}

@Embeddable
class MoneyEmbeddable {
    @Column(name = "price_amount", nullable = false, precision = 19, scale = 4)
    var amount: BigDecimal = BigDecimal.ZERO

    @Column(name = "price_currency", nullable = false, length = 3)
    var currency: String = ""

    protected constructor()

    constructor(amount: BigDecimal, currency: String) {
        this.amount = amount
        this.currency = currency
    }

    fun toDomain(): Money = Money.of(amount, currency)

    companion object {
        fun fromDomain(money: Money): MoneyEmbeddable = MoneyEmbeddable(money.amount, money.currency)
    }
}
