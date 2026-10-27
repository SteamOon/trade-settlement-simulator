package spring_kotlin_learning.trade_settlement_simulator.infrastructure.ledger

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import spring_kotlin_learning.trade_settlement_simulator.ledger.domain.Posting
import java.math.BigDecimal
import java.util.UUID

@Embeddable
class PostingEmbeddable {
    @Column(name = "account_id", nullable = false)
    var accountId: UUID = UUID(0, 0)

    @Column(name = "debit", nullable = false, precision = 19, scale = 4)
    var debit: BigDecimal = BigDecimal.ZERO

    @Column(name = "credit", nullable = false, precision = 19, scale = 4)
    var credit: BigDecimal = BigDecimal.ZERO

    protected constructor()

    constructor(accountId: UUID, debit: BigDecimal = BigDecimal.ZERO, credit: BigDecimal = BigDecimal.ZERO) {
        this.accountId = accountId
        this.debit = debit
        this.credit = credit
    }

    fun toDomain(): Posting = Posting(
        accountId = accountId,
        debit = debit,
        credit = credit,
    )

    companion object {
        fun fromDomain(posting: Posting): PostingEmbeddable = PostingEmbeddable(
            accountId = posting.accountId,
            debit = posting.debit,
            credit = posting.credit,
        )
    }
}
