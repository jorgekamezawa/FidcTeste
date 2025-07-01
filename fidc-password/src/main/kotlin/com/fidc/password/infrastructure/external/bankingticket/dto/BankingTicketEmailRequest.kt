package com.fidc.password.infrastructure.external.bankingticket.dto

data class BankingTicketEmailRequest(
    val clientDocumentNumber: String,
    val clientEmail: String,
    val tokenLength: Int,
    val limitAttempts: Int,
    val expirationTime: Int
)