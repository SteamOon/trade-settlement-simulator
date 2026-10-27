package spring_kotlin_learning.trade_settlement_simulator.trade.api

import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import spring_kotlin_learning.trade_settlement_simulator.trade.api.dto.ConfirmationRequest
import spring_kotlin_learning.trade_settlement_simulator.trade.api.dto.CreateTradeRequest
import spring_kotlin_learning.trade_settlement_simulator.trade.api.dto.TradeResponse
import spring_kotlin_learning.trade_settlement_simulator.trade.application.TradeService
import java.net.URI
import java.util.UUID

@RestController
@Validated
@RequestMapping("/api")
class TradeController(
    private val tradeService: TradeService,
) {

    @PostMapping("/trades")
    fun createTrade(
        @Valid @RequestBody request: CreateTradeRequest,
        @RequestHeader(value = "Idempotency-Key", required = false) idempotencyKey: String?,
    ): ResponseEntity<TradeResponse> {
        val response = tradeService.createTrade(request, idempotencyKey)
        return ResponseEntity
            .created(URI.create("/api/trades/${response.id}"))
            .body(response)
    }

    @GetMapping("/trades/{tradeId}")
    fun getTrade(@PathVariable tradeId: UUID): ResponseEntity<TradeResponse> {
        return ResponseEntity.ok(tradeService.getTrade(tradeId))
    }

    @PostMapping("/trades/{tradeId}/confirmation")
    fun confirmTrade(
        @PathVariable tradeId: UUID,
        @Valid @RequestBody request: ConfirmationRequest,
        @RequestHeader(value = "Idempotency-Key", required = false) idempotencyKey: String?,
    ): ResponseEntity<TradeResponse> {
        return ResponseEntity.status(HttpStatus.OK).body(
            tradeService.confirmTrade(tradeId, request, idempotencyKey)
        )
    }

    @PostMapping("/trades/{tradeId}/retry")
    fun retryTrade(
        @PathVariable tradeId: UUID,
        @RequestHeader(value = "Idempotency-Key", required = false) idempotencyKey: String?,
    ): ResponseEntity<TradeResponse> {
        return ResponseEntity.status(HttpStatus.OK).body(
            tradeService.retryTrade(tradeId, idempotencyKey)
        )
    }

    @PostMapping("/trades/{tradeId}/cancel")
    fun cancelTrade(
        @PathVariable tradeId: UUID,
        @RequestHeader(value = "Idempotency-Key", required = false) idempotencyKey: String?,
    ): ResponseEntity<TradeResponse> {
        return ResponseEntity.status(HttpStatus.OK).body(
            tradeService.cancelTrade(tradeId, idempotencyKey)
        )
    }
}
