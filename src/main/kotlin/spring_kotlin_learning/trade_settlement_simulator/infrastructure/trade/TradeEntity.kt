package spring_kotlin_learning.trade_settlement_simulator.infrastructure.trade

import jakarta.persistence.AttributeOverride
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import jakarta.persistence.Version
import spring_kotlin_learning.trade_settlement_simulator.trade.domain.TradeSide
import spring_kotlin_learning.trade_settlement_simulator.trade.domain.TradeStatus
import java.time.LocalDate
import java.util.UUID

@Entity
@Table(name = "trades")
class TradeEntity {
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    var id: UUID = UUID.randomUUID()

    @Enumerated(EnumType.STRING)
    @Column(name = "side", nullable = false, length = 4)
    lateinit var side: TradeSide

    @Embedded
    @AttributeOverride(name = "value", column = Column(name = "isin", nullable = false, length = 12))
    lateinit var isin: IsinEmbeddable

    @Embedded
    @AttributeOverride(name = "value", column = Column(name = "quantity", nullable = false, precision = 19, scale = 4))
    lateinit var quantity: QuantityEmbeddable

    @Embedded
    @AttributeOverride(name = "amount", column = Column(name = "price_amount", nullable = false, precision = 19, scale = 4))
    @AttributeOverride(name = "currency", column = Column(name = "price_currency", nullable = false, length = 3))
    lateinit var price: MoneyEmbeddable

    @Embedded
    @AttributeOverride(name = "value", column = Column(name = "our_party_id", nullable = false))
    lateinit var ourPartyId: PartyIdEmbeddable

    @Embedded
    @AttributeOverride(name = "value", column = Column(name = "counterparty_id", nullable = false))
    lateinit var counterpartyId: PartyIdEmbeddable

    @Column(name = "trade_date", nullable = false)
    lateinit var tradeDate: LocalDate

    @Column(name = "settlement_date", nullable = false)
    lateinit var settlementDate: LocalDate

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    lateinit var status: TradeStatus

    @OneToMany(mappedBy = "trade", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    var history: MutableList<TradeStatusHistoryEntity> = mutableListOf()

    @Version
    @Column(name = "version", nullable = false)
    var version: Long = 0L

    protected constructor()

    constructor(
        id: UUID,
        side: TradeSide,
        isin: IsinEmbeddable,
        quantity: QuantityEmbeddable,
        price: MoneyEmbeddable,
        ourPartyId: PartyIdEmbeddable,
        counterpartyId: PartyIdEmbeddable,
        tradeDate: LocalDate,
        settlementDate: LocalDate,
        status: TradeStatus,
        version: Long = 0L,
    ) {
        this.id = id
        this.side = side
        this.isin = isin
        this.quantity = quantity
        this.price = price
        this.ourPartyId = ourPartyId
        this.counterpartyId = counterpartyId
        this.tradeDate = tradeDate
        this.settlementDate = settlementDate
        this.status = status
        this.version = version
    }
}
