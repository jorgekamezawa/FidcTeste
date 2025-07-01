package com.simulator.bankingticket.service

import com.simulator.bankingticket.model.TokenRequest
import com.simulator.bankingticket.model.TokenStatus
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap

@Service
class TokenService {

    private val logger = LoggerFactory.getLogger(this::class.java)

    // Banco de dados em memória - tokens por CPF
    private val tokensByDocument = ConcurrentHashMap<String, TokenRequest>()

    fun sendToken(tokenRequest: TokenRequest): TokenRequest {
        logger.info("Enviando token: document=***${tokenRequest.clientDocumentNumber.takeLast(4)}, email=${maskEmail(tokenRequest.clientEmail)}")

        // Verificar se já existe token ativo para este CPF
        val existingToken = tokensByDocument[tokenRequest.clientDocumentNumber]
        if (existingToken != null && existingToken.status == TokenStatus.ACTIVE) {
            logger.info("Token anterior encontrado, substituindo por novo")
        }

        // Simular envio de email (sempre sucesso)
        tokensByDocument[tokenRequest.clientDocumentNumber] = tokenRequest

        logger.info("Token gerado e armazenado: token=${tokenRequest.tokenGenerated}, requestId=${tokenRequest.id}")
        logger.debug("Email simulado enviado para: ${maskEmail(tokenRequest.clientEmail)}")

        return tokenRequest
    }

    fun validateToken(clientDocument: String, token: String): Pair<Boolean, TokenRequest?> {
        logger.info("Validando token: document=***${clientDocument.takeLast(4)}, token=$token")

        val tokenRequest = tokensByDocument[clientDocument]

        if (tokenRequest == null) {
            logger.warn("Nenhum token encontrado para o documento")
            return false to null
        }

        // Verificar se token já foi validado
        if (tokenRequest.status == TokenStatus.VALIDATED) {
            logger.warn("Token já foi utilizado anteriormente")
            return false to tokenRequest
        }

        // Verificar se token expirou (simples: 10 minutos)
        if (isTokenExpired(tokenRequest)) {
            logger.warn("Token expirado")
            val expiredToken = tokenRequest.copy(status = TokenStatus.EXPIRED)
            tokensByDocument[clientDocument] = expiredToken
            return false to expiredToken
        }

        // Verificar se excedeu tentativas
        if (tokenRequest.attemptsUsed >= tokenRequest.limitAttempts) {
            logger.warn("Limite de tentativas excedido")
            val exhaustedToken = tokenRequest.copy(status = TokenStatus.EXHAUSTED)
            tokensByDocument[clientDocument] = exhaustedToken
            return false to exhaustedToken
        }

        // Incrementar tentativas
        val updatedToken = tokenRequest.copy(
            attemptsUsed = tokenRequest.attemptsUsed + 1,
            lastAttemptAt = LocalDateTime.now()
        )

        // Verificar se token está correto
        val isValid = tokenRequest.tokenGenerated == token

        if (isValid) {
            logger.info("Token validado com sucesso")
            val validatedToken = updatedToken.copy(status = TokenStatus.VALIDATED)
            tokensByDocument[clientDocument] = validatedToken
            return true to validatedToken
        } else {
            logger.warn("Token inválido: esperado=${tokenRequest.tokenGenerated}, recebido=$token")
            tokensByDocument[clientDocument] = updatedToken
            return false to updatedToken
        }
    }

    fun getTokenInfo(clientDocument: String): TokenRequest? {
        return tokensByDocument[clientDocument]
    }

    fun getAllTokens(): List<TokenRequest> {
        return tokensByDocument.values.toList()
    }

    fun clearExpiredTokens(): Int {
        val expired = tokensByDocument.values.filter { isTokenExpired(it) }
        expired.forEach { token ->
            tokensByDocument[token.clientDocumentNumber] = token.copy(status = TokenStatus.EXPIRED)
        }
        logger.info("Tokens expirados marcados: {}", expired.size)
        return expired.size
    }

    private fun isTokenExpired(tokenRequest: TokenRequest): Boolean {
        val expirationMinutes = if (tokenRequest.expirationTime <= 0) 10 else tokenRequest.expirationTime / 60
        val expirationTime = tokenRequest.createdAt.plusMinutes(expirationMinutes.toLong())
        return LocalDateTime.now().isAfter(expirationTime)
    }

    private fun maskEmail(email: String): String {
        val parts = email.split("@")
        if (parts.size != 2) return email

        val localPart = parts[0]
        val domain = parts[1]

        val maskedLocal = if (localPart.length <= 2) {
            localPart
        } else {
            "${localPart.take(2)}***${localPart.takeLast(1)}"
        }

        return "$maskedLocal@$domain"
    }
}
