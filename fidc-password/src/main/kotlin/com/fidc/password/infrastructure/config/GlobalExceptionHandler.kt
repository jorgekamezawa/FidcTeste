package com.fidc.password.infrastructure.config

import com.fidc.password.presentation.common.dto.ErrorResponse
import com.fidc.password.presentation.common.dto.ValidationErrorResponse
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.time.LocalDateTime

@RestControllerAdvice
@Order(100)
class GlobalExceptionHandler {
    private val logger = LoggerFactory.getLogger(this::class.java)

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationErrors(
        ex: MethodArgumentNotValidException,
        request: HttpServletRequest
    ): ResponseEntity<ValidationErrorResponse> {
        val status = HttpStatus.BAD_REQUEST
        logger.warn("Erro de validação: {}", ex.message)

        val fieldErrors = ex.bindingResult.fieldErrors.associate {
            it.field to (it.defaultMessage ?: "Valor inválido")
        }

        val errorResponse = ValidationErrorResponse(
            timestamp = LocalDateTime.now(),
            status = status.value(),
            error = status.reasonPhrase,
            message = "Dados de entrada inválidos",
            errors = fieldErrors
        )

        return ResponseEntity.status(status).body(errorResponse)
    }

    @ExceptionHandler(Exception::class)
    fun handleGenericException(
        ex: Exception,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        val status = HttpStatus.INTERNAL_SERVER_ERROR
        logger.error("Erro interno do servidor: {}", ex.message, ex)

        val errorResponse = ErrorResponse(
            timestamp = LocalDateTime.now(),
            status = status.value(),
            error = status.reasonPhrase,
            message = "Erro interno do servidor",
            path = request.requestURI
        )

        return ResponseEntity.status(status).body(errorResponse)
    }
}