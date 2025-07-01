package com.simulator.bankingticket.model

import java.time.LocalDateTime

data class TokenRequest(
    val id: String,
    val clientDocumentNumber: String,
    val clientEmail: String,
    val tokenLength: Int,
    val limitAttempts: Int,
    val expirationTime: Int,
    val tokenGenerated: String,
    val attemptsUsed: Int = 0,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val lastAttemptAt: LocalDateTime? = null,
    val status: TokenStatus = TokenStatus.ACTIVE
)

enum class TokenStatus {
    ACTIVE,
    EXPIRED,
    EXHAUSTED,
    VALIDATED
}