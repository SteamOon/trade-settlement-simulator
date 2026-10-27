package spring_kotlin_learning.trade_settlement_simulator.trade.api.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Positive
import spring_kotlin_learning.trade_settlement_simulator.trade.domain.TradeSide
import java.math.BigDecimal

data class ConfirmationRequest(
    @field:NotNull(message = "Side is required")
    val side: TradeSide? = null,

    @field:NotBlank(message = "ISIN is required")
    @field:Pattern(
        regexp = "^[A-Z]{2}[A-Z0-9]{9}[0-9]$",
        message = "ISIN must be a valid 12-character code"
    )
    val isin: String? = null,

    @field:NotNull(message = "Quantity is required")
    @field:Positive(message = "Quantity must be greater than zero")
    val quantity: BigDecimal? = null,

    @field:NotNull(message = "Price is required")
    @field:Positive(message = "Price must be greater than zero")
    val price: BigDecimal? = null,

    @field:NotBlank(message = "Currency is required")
    @field:Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be a valid ISO-4217 code")
    val currency: String? = null,
)
