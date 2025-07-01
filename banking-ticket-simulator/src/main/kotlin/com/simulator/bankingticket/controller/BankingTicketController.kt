package com.simulator.bankingticket.controller

import com.simulator.bankingticket.model.TokenRequest
import com.simulator.bankingticket.service.TokenService
import com.simulator.bankingticket.dto.*
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/ticket")
@Tag(name = "Banking Ticket", description = "Simulador do Banking Ticket para envio e validação de tokens")
class BankingTicketController(
    private val tokenService: TokenService
) {

    private val logger = LoggerFactory.getLogger(this::class.java)

    @PostMapping("/v4/email")
    @Operation(
        summary = "Enviar token por email",
        description = "Envia um token de verificação por email para o cliente"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Token enviado com sucesso",
                content = [Content(schema = Schema(implementation = EmailTokenResponse::class))]
            ),
            ApiResponse(
                responseCode = "400",
                description = "Dados inválidos"
            )
        ]
    )
    fun sendTokenByEmail(@RequestBody request: EmailTokenRequest): ResponseEntity<EmailTokenResponse> {
        logger.info("Recebida requisição de envio de token: document=***${request.clientDocumentNumber.takeLast(4)}")

        try {
            // Validações básicas
            require(request.clientDocumentNumber.isNotBlank()) { "Document é obrigatório" }
            require(request.clientEmail.isNotBlank()) { "Email é obrigatório" }
            require(request.tokenLength in 4..8) { "Token length deve estar entre 4 e 8" }
            require(request.limitAttempts in 1..10) { "Limit attempts deve estar entre 1 e 10" }

            val tokenRequest = request.toModel()
            val savedToken = tokenService.sendToken(tokenRequest)
            val response = savedToken.toEmailResponse()

            logger.info("Token enviado com sucesso: requestId=${savedToken.id}")
            return ResponseEntity.ok(response)

        } catch (e: IllegalArgumentException) {
            logger.warn("Dados inválidos: {}", e.message)
            val errorResponse = EmailTokenResponse(
                success = false,
                message = e.message
            )
            return ResponseEntity.badRequest().body(errorResponse)
        } catch (e: Exception) {
            logger.error("Erro interno ao enviar token", e)
            val errorResponse = EmailTokenResponse(
                success = false,
                message = "Erro interno do servidor"
            )
            return ResponseEntity.internalServerError().body(errorResponse)
        }
    }

    @PostMapping("/v4/validate")
    @Operation(
        summary = "Validar token",
        description = "Valida um token enviado anteriormente"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Validação processada",
                content = [Content(schema = Schema(implementation = TokenValidationResponse::class))]
            )
        ]
    )
    fun validateToken(@RequestBody request: TokenValidationRequest): ResponseEntity<TokenValidationResponse> {
        logger.info("Recebida requisição de validação: document=***${request.clientDocument.takeLast(4)}")

        val (isValid, tokenRequest) = tokenService.validateToken(request.clientDocument, request.token)

        val response = if (isValid) {
            TokenValidationResponse(
                success = true,
                message = "Token válido",
                valid = true,
                attemptsRemaining = tokenRequest?.let { it.limitAttempts - it.attemptsUsed }
            )
        } else {
            val message = when {
                tokenRequest == null -> "Token não encontrado"
                tokenRequest.status.name == "EXPIRED" -> "Token expirado"
                tokenRequest.status.name == "EXHAUSTED" -> "Limite de tentativas excedido"
                tokenRequest.status.name == "VALIDATED" -> "Token já foi utilizado"
                else -> "Token inválido"
            }

            TokenValidationResponse(
                success = false,
                message = message,
                valid = false,
                attemptsRemaining = tokenRequest?.let { it.limitAttempts - it.attemptsUsed }
            )
        }

        logger.info("Validação concluída: valid={}, message={}", response.valid, response.message)
        return ResponseEntity.ok(response)
    }

    @GetMapping("/tokens")
    @Operation(
        summary = "Listar todos os tokens",
        description = "Lista todos os tokens enviados (para debugging)"
    )
    fun getAllTokens(): List<TokenRequest> {
        logger.info("Listando todos os tokens")
        return tokenService.getAllTokens()
    }

    @GetMapping("/tokens/{document}")
    @Operation(
        summary = "Buscar token por documento",
        description = "Busca informações do token por documento (para debugging)"
    )
    fun getTokenByDocument(@PathVariable document: String): ResponseEntity<TokenRequest> {
        val token = tokenService.getTokenInfo(document)
        return if (token != null) {
            ResponseEntity.ok(token)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @PostMapping("/admin/clear-expired")
    @Operation(
        summary = "Limpar tokens expirados",
        description = "Marca tokens expirados como expirados (para manutenção)"
    )
    fun clearExpiredTokens(): Map<String, Any> {
        val cleared = tokenService.clearExpiredTokens()
        return mapOf(
            "cleared" to cleared,
            "message" to "Tokens expirados processados"
        )
    }

    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Verifica se o serviço está funcionando")
    fun health(): Map<String, String> {
        return mapOf(
            "status" to "UP",
            "service" to "Banking Ticket Simulator",
            "activeTokens" to tokenService.getAllTokens().size.toString()
        )
    }
}