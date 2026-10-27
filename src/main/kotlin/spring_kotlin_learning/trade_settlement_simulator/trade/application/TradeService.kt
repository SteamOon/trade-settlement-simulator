package spring_kotlin_learning.trade_settlement_simulator.trade.application

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import spring_kotlin_learning.trade_settlement_simulator.infrastructure.idempotency.IdempotencyKeyEntity
import spring_kotlin_learning.trade_settlement_simulator.infrastructure.idempotency.IdempotencyKeyRepository
import spring_kotlin_learning.trade_settlement_simulator.infrastructure.trade.TradeEntity
import spring_kotlin_learning.trade_settlement_simulator.infrastructure.trade.TradeRepository
import spring_kotlin_learning.trade_settlement_simulator.infrastructure.trade.TradeStatusHistoryEntity
import spring_kotlin_learning.trade_settlement_simulator.trade.api.dto.ConfirmationRequest
import spring_kotlin_learning.trade_settlement_simulator.trade.api.dto.CreateTradeRequest
import spring_kotlin_learning.trade_settlement_simulator.trade.api.dto.TradeHistoryResponse
import spring_kotlin_learning.trade_settlement_simulator.trade.api.dto.TradeResponse
import spring_kotlin_learning.trade_settlement_simulator.trade.domain.Isin
import spring_kotlin_learning.trade_settlement_simulator.trade.domain.Money
import spring_kotlin_learning.trade_settlement_simulator.trade.domain.PartyId
import spring_kotlin_learning.trade_settlement_simulator.trade.domain.Quantity
import spring_kotlin_learning.trade_settlement_simulator.trade.domain.Trade
import spring_kotlin_learning.trade_settlement_simulator.trade.domain.TradeConfirmation
import spring_kotlin_learning.trade_settlement_simulator.trade.domain.TradeSide
import java.math.BigDecimal
import java.security.MessageDigest
import java.time.Instant
import java.util.UUID

@Service
class TradeService(
    private val tradeRepository: TradeRepository,
    private val idempotencyKeyRepository: IdempotencyKeyRepository,
) {

    @Transactional(readOnly = true)
    fun getTrade(tradeId: UUID): TradeResponse {
        val entity = tradeRepository.findById(tradeId)
            .orElseThrow { NoSuchElementException("Trade $tradeId not found") }
        return toResponse(entity.toDomain())
    }

    @Transactional
    fun createTrade(request: CreateTradeRequest, idempotencyKey: String? = null): TradeResponse {
        val trade = Trade.create(
            side = request.side!!,
            isin = Isin(request.isin!!),
            quantity = Quantity.of(request.quantity!!),
            price = Money.of(request.price!!, request.currency!!),
            ourPartyId = PartyId(request.ourPartyId!!),
            counterpartyId = PartyId(request.counterpartyId!!),
            tradeDate = request.tradeDate!!,
            settlementDate = request.effectiveSettlementDate(),
        )

        val saved = tradeRepository.save(TradeEntity.fromDomain(trade))
        recordIdempotency(saved.id, "create", idempotencyKey, request)
        return toResponse(saved.toDomain())
    }

    @Transactional
    fun confirmTrade(tradeId: UUID, request: ConfirmationRequest, idempotencyKey: String? = null): TradeResponse {
        val trade = loadTrade(tradeId)
        val confirmation = TradeConfirmation(
            side = request.side!!,
            isin = Isin(request.isin!!),
            quantity = Quantity.of(request.quantity!!),
            price = Money.of(request.price!!, request.currency!!),
        )

        trade.confirm(confirmation)
        val response = saveAndRespond(trade)
        recordIdempotency(tradeId, "confirm", idempotencyKey, request)
        return response
    }

    @Transactional
    fun retryTrade(tradeId: UUID, idempotencyKey: String? = null): TradeResponse {
        val trade = loadTrade(tradeId)
        trade.retry()
        val response = saveAndRespond(trade)
        recordIdempotency(tradeId, "retry", idempotencyKey, mapOf("tradeId" to tradeId.toString()))
        return response
    }

    @Transactional
    fun cancelTrade(tradeId: UUID, idempotencyKey: String? = null): TradeResponse {
        val trade = loadTrade(tradeId)
        trade.cancel()
        val response = saveAndRespond(trade)
        recordIdempotency(tradeId, "cancel", idempotencyKey, mapOf("tradeId" to tradeId.toString()))
        return response
    }

    @Transactional(readOnly = true)
    fun findIdempotencyKey(tradeId: UUID, operation: String, key: String): IdempotencyKeyEntity? {
        return idempotencyKeyRepository.findByTradeIdAndOperationAndKey(tradeId, operation, key).orElse(null)
    }

    private fun loadTrade(tradeId: UUID): Trade {
        val entity = tradeRepository.findById(tradeId)
            .orElseThrow { NoSuchElementException("Trade $tradeId not found") }
        return entity.toDomain()
    }

    private fun saveAndRespond(trade: Trade): TradeResponse {
        val entity = TradeEntity.fromDomain(trade)
        val saved = tradeRepository.save(entity)
        return toResponse(saved.toDomain())
    }

    private fun recordIdempotency(
        tradeId: UUID,
        operation: String,
        idempotencyKey: String?,
        payload: Any,
    ) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return
        }

        val existing = idempotencyKeyRepository.findByTradeIdAndOperationAndKey(tradeId, operation, idempotencyKey)
        if (existing.isPresent) {
            val existingPayload = existing.get().requestHash
            val incomingHash = payloadHash(payload)
            if (existingPayload != incomingHash) {
                throw IllegalStateException("Idempotency key conflict for $operation on trade $tradeId")
            }
            return
        }

        idempotencyKeyRepository.save(
            IdempotencyKeyEntity(
                tradeId = tradeId,
                operation = operation,
                key = idempotencyKey,
                requestHash = payloadHash(payload),
                createdAt = Instant.now(),
            )
        )
    }

    private fun payloadHash(payload: Any): String {
        val canonical = when (payload) {
            is String -> payload
            is Map<*, *> -> payload.entries.joinToString(separator = "&") { "${it.key}=${it.value}" }
            else -> payload.toString()
        }

        val digest = MessageDigest.getInstance("SHA-256").digest(canonical.toByteArray())
        return digest.joinToString(separator = "") { "%02x".format(it) }
    }

    private fun toResponse(trade: Trade): TradeResponse = TradeResponse(
        id = trade.id,
        side = trade.side.name,
        isin = trade.isin.value,
        quantity = trade.quantity.value,
        price = trade.price.amount,
        currency = trade.price.currency,
        ourPartyId = trade.ourPartyId.value,
        counterpartyId = trade.counterpartyId.value,
        tradeDate = trade.tradeDate,
        settlementDate = trade.settlementDate,
        status = trade.status,
        version = trade.version,
        history = trade.history.map(TradeHistoryResponse::from),
    )
}
