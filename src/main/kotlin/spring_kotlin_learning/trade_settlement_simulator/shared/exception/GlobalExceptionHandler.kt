package spring_kotlin_learning.trade_settlement_simulator.shared.exception

import jakarta.validation.ConstraintViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.time.Instant

data class ApiError(
    val status: Int,
    val error: String,
    val message: String,
    val timestamp: Instant,
    val fieldErrors: Map<String, String>? = null,
)

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleMethodArgumentNotValid(ex: MethodArgumentNotValidException): ResponseEntity<ApiError> {
        val fieldErrors = ex.bindingResult.fieldErrors.associate { (it as FieldError).field to (it.defaultMessage ?: "Invalid value") }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            ApiError(
                status = HttpStatus.BAD_REQUEST.value(),
                error = HttpStatus.BAD_REQUEST.reasonPhrase,
                message = "Validation failed",
                timestamp = Instant.now(),
                fieldErrors = fieldErrors.ifEmpty { null },
            )
        )
    }

    @ExceptionHandler(ConstraintViolationException::class)
    fun handleConstraintViolation(ex: ConstraintViolationException): ResponseEntity<ApiError> {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            ApiError(
                status = HttpStatus.BAD_REQUEST.value(),
                error = HttpStatus.BAD_REQUEST.reasonPhrase,
                message = ex.message ?: "Validation failed",
                timestamp = Instant.now(),
            )
        )
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleHttpMessageNotReadable(ex: HttpMessageNotReadableException): ResponseEntity<ApiError> {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            ApiError(
                status = HttpStatus.BAD_REQUEST.value(),
                error = HttpStatus.BAD_REQUEST.reasonPhrase,
                message = "Malformed request body",
                timestamp = Instant.now(),
            )
        )
    }

    @ExceptionHandler(ResourceNotFoundException::class)
    fun handleResourceNotFound(ex: ResourceNotFoundException): ResponseEntity<ApiError> {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
            ApiError(
                status = HttpStatus.NOT_FOUND.value(),
                error = HttpStatus.NOT_FOUND.reasonPhrase,
                message = ex.message ?: "Resource not found",
                timestamp = Instant.now(),
            )
        )
    }

    @ExceptionHandler(IdempotencyConflictException::class)
    fun handleIdempotencyConflict(ex: IdempotencyConflictException): ResponseEntity<ApiError> {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
            ApiError(
                status = HttpStatus.CONFLICT.value(),
                error = HttpStatus.CONFLICT.reasonPhrase,
                message = ex.message ?: "Idempotency conflict",
                timestamp = Instant.now(),
            )
        )
    }

    @ExceptionHandler(IllegalTradeTransitionException::class)
    fun handleIllegalTradeTransition(ex: IllegalTradeTransitionException): ResponseEntity<ApiError> {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
            ApiError(
                status = HttpStatus.CONFLICT.value(),
                error = HttpStatus.CONFLICT.reasonPhrase,
                message = ex.message ?: "Illegal trade transition",
                timestamp = Instant.now(),
            )
        )
    }
}
