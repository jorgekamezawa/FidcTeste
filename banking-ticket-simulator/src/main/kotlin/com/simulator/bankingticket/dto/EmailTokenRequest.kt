package com.simulator.bankingticket.dto

import com.simulator.bankingticket.model.TokenRequest
import java.util.*

data class EmailTokenRequest(
    val clientDocumentNumber: String,
    val clientEmail: String,
    val tokenLength: Int,
    val limitAttempts: Int,
    val expirationTime: Int
)

data class EmailTokenResponse(
    val success: Boolean = true,
    val message: String? = null,
    val requestId: String? = null,
    val clientDocumentNumber: String? = null,
    val clientEmail: String? = null,
    val tokenLength: Int? = null,
    val limitAttempts: Int? = null,
    val expirationTime: Int? = null,
    val attemptsRemaining: Int? = null
)

data class TokenValidationRequest(
    val clientDocument: String,
    val token: String
)

data class TokenValidationResponse(
    val success: Boolean,
    val message: String,
    val valid: Boolean,
    val attemptsRemaining: Int? = null
)

fun EmailTokenRequest.toModel(): TokenRequest {
    return TokenRequest(
        id = UUID.randomUUID().toString(),
        clientDocumentNumber = this.clientDocumentNumber,
        clientEmail = this.clientEmail,
        tokenLength = this.tokenLength,
        limitAttempts = this.limitAttempts,
        expirationTime = this.expirationTime,
        tokenGenerated = generateRandomToken(this.tokenLength)
    )
}

fun TokenRequest.toEmailResponse(): EmailTokenResponse {
    return EmailTokenResponse(
        success = true,
        message = "Token enviado com sucesso",
        requestId = this.id,
        clientDocumentNumber = this.clientDocumentNumber,
        clientEmail = maskEmail(this.clientEmail),
        tokenLength = this.tokenLength,
        limitAttempts = this.limitAttempts,
        expirationTime = this.expirationTime,
        attemptsRemaining = this.limitAttempts - this.attemptsUsed
    )
}

private fun generateRandomToken(length: Int): String {
    val chars = "0123456789"
    return (1..length)
        .map { chars.random() }
        .joinToString("")
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