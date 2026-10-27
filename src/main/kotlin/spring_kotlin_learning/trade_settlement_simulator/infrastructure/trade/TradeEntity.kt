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
import spring_kotlin_learning.trade_settlement_simulator.trade.domain.Isin
import spring_kotlin_learning.trade_settlement_simulator.trade.domain.Money
import spring_kotlin_learning.trade_settlement_simulator.trade.domain.PartyId
import spring_kotlin_learning.trade_settlement_simulator.trade.domain.Quantity
import spring_kotlin_learning.trade_settlement_simulator.trade.domain.Trade
import spring_kotlin_learning.trade_settlement_simulator.trade.domain.TradeSide
import spring_kotlin_learning.trade_settlement_simulator.trade.domain.TradeStatus
import spring_kotlin_learning.trade_settlement_simulator.trade.domain.TradeStatusChangedEvent
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
    @AttributeOverride(name = "value", column = Column(name = "our_party_id", nullable = false, length = 64))
    lateinit var ourPartyId: PartyIdEmbeddable

    @Embedded
    @AttributeOverride(name = "value", column = Column(name = "counterparty_id", nullable = false, length = 64))
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
        history: MutableList<TradeStatusHistoryEntity> = mutableListOf(),
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
        this.history = history
        this.history.forEach { it.trade = this }
    }

    fun toDomain(): Trade {
        val historyEvents = history.map {
            TradeStatusChangedEvent(
                tradeId = id,
                from = it.fromStatus,
                to = it.toStatus,
                reason = it.reason,
                occurredAt = it.occurredAt,
            )
        }

        return Trade.fromPersistence(
            id = id,
            side = side,
            isin = Isin(isin.value),
            quantity = Quantity.of(quantity.value),
            price = Money.of(price.amount, price.currency),
            ourPartyId = PartyId(ourPartyId.value),
            counterpartyId = PartyId(counterpartyId.value),
            tradeDate = tradeDate,
            settlementDate = settlementDate,
            status = status,
            version = version,
            history = historyEvents,
        )
    }

    companion object {
        fun fromDomain(trade: Trade): TradeEntity {
            val historyEntities = trade.history.map { event ->
                TradeStatusHistoryEntity(
                    trade = TradeEntity(),
                    fromStatus = event.from,
                    toStatus = event.to,
                    reason = event.reason,
                    occurredAt = event.occurredAt,
                )
            }.toMutableList()

            val entity = TradeEntity(
                id = trade.id,
                side = trade.side,
                isin = IsinEmbeddable.fromDomain(Isin(trade.isin.value)),
                quantity = QuantityEmbeddable.fromDomain(Quantity.of(trade.quantity.value)),
                price = MoneyEmbeddable.fromDomain(Money.of(trade.price.amount, trade.price.currency)),
                ourPartyId = PartyIdEmbeddable.fromDomain(PartyId(trade.ourPartyId.value)),
                counterpartyId = PartyIdEmbeddable.fromDomain(PartyId(trade.counterpartyId.value)),
                tradeDate = trade.tradeDate,
                settlementDate = trade.settlementDate,
                status = trade.status,
                version = trade.version,
                history = historyEntities,
            )
            entity.history.forEach { it.trade = entity }
            return entity
        }
    }
}
