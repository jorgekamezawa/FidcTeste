package com.fidc.password.domain.auth.dto

class TokenSendByEmailDto(
    val cpf: String,
    val email: String,
    val tokenLength: Int,
    val limitAttempts: Int,
    val expirationTimeMinutes: Int
)