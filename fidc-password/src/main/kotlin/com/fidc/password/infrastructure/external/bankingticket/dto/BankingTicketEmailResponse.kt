package com.fidc.password.infrastructure.external.bankingticket.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class BankingTicketEmailResponse(
    val success: Boolean = true,
    val message: String? = null
)