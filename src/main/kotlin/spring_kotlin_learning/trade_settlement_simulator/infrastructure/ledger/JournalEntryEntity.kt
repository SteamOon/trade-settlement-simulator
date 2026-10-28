package spring_kotlin_learning.trade_settlement_simulator.infrastructure.ledger

import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.Table
import spring_kotlin_learning.trade_settlement_simulator.ledger.domain.JournalEntry
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "journal_entries")
class JournalEntryEntity {
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    var id: UUID = UUID.randomUUID()

    @Column(name = "trade_id", nullable = false)
    var tradeId: UUID = UUID.randomUUID()

    @Column(name = "booked_at", nullable = false)
    var bookedAt: Instant = Instant.now()

    @Column(name = "description")
    var description: String? = null

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "postings",
        joinColumns = [JoinColumn(name = "journal_entry_id")],
    )
    var postings: MutableList<PostingEmbeddable> = mutableListOf()

    protected constructor()

    constructor(
        id: UUID,
        tradeId: UUID,
        bookedAt: Instant,
        postings: List<PostingEmbeddable>,
        description: String? = null,
    ) {
        this.id = id
        this.tradeId = tradeId
        this.bookedAt = bookedAt
        this.postings = postings.toMutableList()
        this.description = description
    }

    fun toDomain(): JournalEntry = JournalEntry.create(
        tradeId = tradeId,
        bookedAt = bookedAt,
        postings = postings.map { it.toDomain() },
        description = description,
        id = id,
    )

    companion object {
        fun fromDomain(journalEntry: JournalEntry): JournalEntryEntity = JournalEntryEntity(
            id = journalEntry.id,
            tradeId = journalEntry.tradeId,
            bookedAt = journalEntry.bookedAt,
            postings = journalEntry.postings.map { PostingEmbeddable.fromDomain(it) },
            description = journalEntry.description,
        )
    }
}
