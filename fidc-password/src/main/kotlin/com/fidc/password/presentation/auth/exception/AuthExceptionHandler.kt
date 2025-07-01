package com.fidc.password.presentation.auth.exception

import com.fidc.password.application.exception.*
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
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import java.time.LocalDateTime

@RestControllerAdvice
@Order(1) // Higher priority than global handler
class AuthExceptionHandler {
    private val logger = LoggerFactory.getLogger(this::class.java)

    @ExceptionHandler(UserNotFoundApplicationException::class)
    fun handleUserNotFound(
        ex: UserNotFoundApplicationException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        val status = HttpStatus.NOT_FOUND

        logger.warn("Usuário não encontrado: {}", ex.message)

        val errorResponse = ErrorResponse(
            timestamp = LocalDateTime.now(),
            status = status.value(),
            error = status.reasonPhrase,
            message = "Usuário não encontrado na base de dados",
            path = request.requestURI
        )

        return ResponseEntity.status(status).body(errorResponse)
    }

    @ExceptionHandler(InvalidBirthDateApplicationException::class)
    fun handleInvalidBirthDate(
        ex: InvalidBirthDateApplicationException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        val status = HttpStatus.UNPROCESSABLE_ENTITY

        logger.warn("Data de nascimento inválida: {}", ex.message)

        val errorResponse = ErrorResponse(
            timestamp = LocalDateTime.now(),
            status = status.value(),
            error = status.reasonPhrase,
            message = "Data de nascimento não confere com os dados cadastrados",
            path = request.requestURI
        )

        return ResponseEntity.status(status).body(errorResponse)
    }

    @ExceptionHandler(TokenSendApplicationException::class)
    fun handleTokenSendFailure(
        ex: TokenSendApplicationException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        val status = HttpStatus.INTERNAL_SERVER_ERROR

        logger.error("Falha ao enviar token: {}", ex.message, ex)

        val errorResponse = ErrorResponse(
            timestamp = LocalDateTime.now(),
            status = status.value(),
            error = status.reasonPhrase,
            message = "Falha no envio do token. Tente novamente em alguns instantes",
            path = request.requestURI
        )

        return ResponseEntity.status(status).body(errorResponse)
    }

    @ExceptionHandler(SistemaCoreApplicationException::class)
    fun handleSistemaCoreIntegration(
        ex: SistemaCoreApplicationException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        val status = HttpStatus.INTERNAL_SERVER_ERROR

        logger.error("Erro na integração com Sistema Core: {}", ex.message, ex)

        val errorResponse = ErrorResponse(
            timestamp = LocalDateTime.now(),
            status = status.value(),
            error = status.reasonPhrase,
            message = "Erro interno do sistema. Tente novamente mais tarde",
            path = request.requestURI
        )

        return ResponseEntity.status(status).body(errorResponse)
    }

    @ExceptionHandler(BankingTicketApplicationException::class)
    fun handleBankingTicketIntegration(
        ex: BankingTicketApplicationException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        val status = HttpStatus.INTERNAL_SERVER_ERROR

        logger.error("Erro na integração com Banking Ticket: {}", ex.message, ex)

        val errorResponse = ErrorResponse(
            timestamp = LocalDateTime.now(),
            status = status.value(),
            error = status.reasonPhrase,
            message = "Falha no envio do token. Tente novamente em alguns instantes",
            path = request.requestURI
        )

        return ResponseEntity.status(status).body(errorResponse)
    }

    @ExceptionHandler(RedisApplicationException::class)
    fun handleRedisFailure(
        ex: RedisApplicationException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        val status = HttpStatus.INTERNAL_SERVER_ERROR

        logger.error("Erro no Redis: {}", ex.message, ex)

        val errorResponse = ErrorResponse(
            timestamp = LocalDateTime.now(),
            status = status.value(),
            error = status.reasonPhrase,
            message = "Erro interno do sistema. Tente novamente mais tarde",
            path = request.requestURI
        )

        return ResponseEntity.status(status).body(errorResponse)
    }

    @ExceptionHandler(InvalidInputApplicationException::class)
    fun handleInvalidInput(
        ex: InvalidInputApplicationException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        val status = HttpStatus.BAD_REQUEST

        logger.warn("Dados de entrada inválidos: {}", ex.message)

        val errorResponse = ErrorResponse(
            timestamp = LocalDateTime.now(),
            status = status.value(),
            error = status.reasonPhrase,
            message = ex.message ?: "Dados de entrada inválidos",
            path = request.requestURI
        )

        return ResponseEntity.status(status).body(errorResponse)
    }

    @ExceptionHandler(GenericAuthApplicationException::class)
    fun handleGenericAuthError(
        ex: GenericAuthApplicationException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        val status = HttpStatus.INTERNAL_SERVER_ERROR

        logger.error("Erro genérico de autenticação: {}", ex.message, ex)

        val errorResponse = ErrorResponse(
            timestamp = LocalDateTime.now(),
            status = status.value(),
            error = status.reasonPhrase,
            message = "Erro interno do sistema. Tente novamente mais tarde",
            path = request.requestURI
        )

        return ResponseEntity.status(status).body(errorResponse)
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationErrors(
        ex: MethodArgumentNotValidException,
        request: HttpServletRequest
    ): ResponseEntity<ValidationErrorResponse> {
        val status = HttpStatus.BAD_REQUEST

        logger.warn("Erro de validação na requisição: {}", ex.message)

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

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleTypeMismatch(
        ex: MethodArgumentTypeMismatchException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        val status = HttpStatus.BAD_REQUEST

        logger.warn("Erro de tipo de parâmetro: {}", ex.message)

        val errorResponse = ErrorResponse(
            timestamp = LocalDateTime.now(),
            status = status.value(),
            error = status.reasonPhrase,
            message = "Formato de data inválido. Use o formato yyyy-MM-dd",
            path = request.requestURI
        )

        return ResponseEntity.status(status).body(errorResponse)
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(
        ex: IllegalArgumentException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        val status = HttpStatus.BAD_REQUEST

        logger.warn("Argumento inválido: {}", ex.message)

        val errorResponse = ErrorResponse(
            timestamp = LocalDateTime.now(),
            status = status.value(),
            error = status.reasonPhrase,
            message = "Dados de entrada inválidos",
            path = request.requestURI
        )

        return ResponseEntity.status(status).body(errorResponse)
    }
}