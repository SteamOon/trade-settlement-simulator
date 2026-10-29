package spring_kotlin_learning.trade_settlement_simulator.ledger.application

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import spring_kotlin_learning.trade_settlement_simulator.infrastructure.ledger.AccountEntity
import spring_kotlin_learning.trade_settlement_simulator.infrastructure.ledger.AccountRepository
import spring_kotlin_learning.trade_settlement_simulator.infrastructure.ledger.JournalEntryEntity
import spring_kotlin_learning.trade_settlement_simulator.infrastructure.ledger.JournalEntryRepository
import spring_kotlin_learning.trade_settlement_simulator.infrastructure.trade.TradeEntity
import spring_kotlin_learning.trade_settlement_simulator.infrastructure.trade.TradeRepository
import spring_kotlin_learning.trade_settlement_simulator.ledger.domain.AccountType
import spring_kotlin_learning.trade_settlement_simulator.ledger.domain.JournalEntry
import spring_kotlin_learning.trade_settlement_simulator.ledger.domain.Posting
import spring_kotlin_learning.trade_settlement_simulator.shared.exception.IllegalTradeTransitionException
import spring_kotlin_learning.trade_settlement_simulator.shared.exception.ResourceNotFoundException
import spring_kotlin_learning.trade_settlement_simulator.trade.domain.Money
import spring_kotlin_learning.trade_settlement_simulator.trade.domain.Trade
import spring_kotlin_learning.trade_settlement_simulator.trade.domain.TradeSide
import spring_kotlin_learning.trade_settlement_simulator.trade.domain.TradeStatus
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Service
class SettlementService(
    private val tradeRepository: TradeRepository,
    private val accountRepository: AccountRepository,
    private val journalEntryRepository: JournalEntryRepository,
) {

    @Transactional
    fun attemptSettlement(tradeId: UUID): TradeStatus {
        val tradeEntity = tradeRepository.findById(tradeId)
            .orElseThrow { ResourceNotFoundException("Trade $tradeId not found") }
        val trade = tradeEntity.toDomain()

        if (trade.status == TradeStatus.SETTLED) {
            return trade.status
        }

        if (trade.status != TradeStatus.MATCHED) {
            throw IllegalTradeTransitionException("Cannot settle a trade in ${trade.status} state")
        }

        val buyerPartyId = if (trade.side == TradeSide.BUY) trade.ourPartyId.value else trade.counterpartyId.value
        val sellerPartyId = if (trade.side == TradeSide.BUY) trade.counterpartyId.value else trade.ourPartyId.value

        val requiredCash = Money.of(
            amount = trade.quantity.value.multiply(trade.price.amount),
            currency = trade.price.currency,
        )

        val buyerCash = accountRepository.findByPartyIdAndAccountTypeAndCurrency(
            partyId = buyerPartyId,
            accountType = AccountType.CASH,
            currency = trade.price.currency,
        ).orElseThrow { IllegalStateException("No cash account found for buyer $buyerPartyId") }

        val sellerCash = accountRepository.findByPartyIdAndAccountTypeAndCurrency(
            partyId = sellerPartyId,
            accountType = AccountType.CASH,
            currency = trade.price.currency,
        ).orElseThrow { IllegalStateException("No cash account found for seller $sellerPartyId") }

        val buyerSecurity = accountRepository.findByPartyIdAndAccountTypeAndIsin(
            partyId = buyerPartyId,
            accountType = AccountType.SECURITY,
            isin = trade.isin.value,
        ).orElseThrow { IllegalStateException("No security account found for buyer $buyerPartyId") }

        val sellerSecurity = accountRepository.findByPartyIdAndAccountTypeAndIsin(
            partyId = sellerPartyId,
            accountType = AccountType.SECURITY,
            isin = trade.isin.value,
        ).orElseThrow { IllegalStateException("No security account found for seller $sellerPartyId") }

        if (buyerCash.toDomain().moneyBalance() < requiredCash) {
            val failed = trade.fail("insufficient_cash_balance")
            tradeRepository.save(TradeEntity.fromDomain(trade))
            return failed
        }

        if (sellerSecurity.toDomain().quantityBalance() < trade.quantity) {
            val failed = trade.fail("insufficient_security_balance")
            tradeRepository.save(TradeEntity.fromDomain(trade))
            return failed
        }

        val settledTrade = trade
        settledTrade.settle()

        val buyerCashEntity = buyerCash
        val sellerCashEntity = sellerCash
        val buyerSecurityEntity = buyerSecurity
        val sellerSecurityEntity = sellerSecurity

        buyerCashEntity.balance = buyerCash.toDomain().moneyBalance().amount.subtract(requiredCash.amount)
        sellerCashEntity.balance = sellerCash.toDomain().moneyBalance().amount.add(requiredCash.amount)
        buyerSecurityEntity.balance = buyerSecurity.toDomain().quantityBalance().value.add(trade.quantity.value)
        sellerSecurityEntity.balance = sellerSecurity.toDomain().quantityBalance().value.subtract(trade.quantity.value)

        val journalEntry = JournalEntry.create(
            tradeId = trade.id,
            bookedAt = Instant.now(),
            postings = listOf(
                Posting.debit(buyerCash.toDomain(), requiredCash),
                Posting.credit(sellerCash.toDomain(), requiredCash),
                Posting.debit(sellerSecurity.toDomain(), trade.quantity),
                Posting.credit(buyerSecurity.toDomain(), trade.quantity),
            ),
            description = "Settlement for trade ${trade.id}",
        )

        tradeRepository.save(TradeEntity.fromDomain(settledTrade))
        accountRepository.saveAll(listOf(buyerCashEntity, sellerCashEntity, buyerSecurityEntity, sellerSecurityEntity))
        journalEntryRepository.save(JournalEntryEntity.fromDomain(journalEntry))

        return settledTrade.status
    }
}
