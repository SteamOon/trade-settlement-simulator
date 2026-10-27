package spring_kotlin_learning.trade_settlement_simulator.ledger.domain

import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

data class JournalEntry private constructor(
    val id: UUID,
    val tradeId: UUID,
    val bookedAt: Instant,
    val postings: List<Posting>,
    val description: String? = null,
) {
    init {
        require(postings.isNotEmpty()) { "A journal entry must contain at least one posting" }
        require(isBalanced()) { "Journal entry debit total must equal credit total" }
    }

    fun totalDebit(): BigDecimal = postings.fold(BigDecimal.ZERO) { total, posting -> total.add(posting.debit) }

    fun totalCredit(): BigDecimal = postings.fold(BigDecimal.ZERO) { total, posting -> total.add(posting.credit) }

    fun isBalanced(): Boolean = totalDebit().compareTo(totalCredit()) == 0

    companion object {
        fun create(
            tradeId: UUID,
            bookedAt: Instant,
            postings: List<Posting>,
            description: String? = null,
            id: UUID = UUID.randomUUID(),
        ): JournalEntry = JournalEntry(
            id = id,
            tradeId = tradeId,
            bookedAt = bookedAt,
            postings = postings.toList(),
            description = description,
        )
    }
}
