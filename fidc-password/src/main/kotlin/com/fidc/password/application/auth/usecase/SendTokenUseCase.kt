package com.fidc.password.application.auth.usecase

import java.time.LocalDate

interface SendTokenUseCase {
    fun execute(input: SendTokenInput): SendTokenOutput
}

data class SendTokenInput(
    val cpf: String,
    val birthDate: LocalDate,
    val origin: String
)

data class SendTokenOutput(
    val message: String,
    val expirationTimeMinutes: Long,
    val clientEmail: String
)