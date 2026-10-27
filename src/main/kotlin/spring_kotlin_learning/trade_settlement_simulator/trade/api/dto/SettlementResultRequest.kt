package spring_kotlin_learning.trade_settlement_simulator.trade.api.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern

data class SettlementResultRequest(
    @field:NotBlank(message = "Settlement result is required")
    @field:Pattern(regexp = "^(SETTLED|FAILED)$", message = "Result must be either SETTLED or FAILED")
    val result: String? = null,

    val reason: String? = null,
)
