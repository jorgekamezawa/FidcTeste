package com.fidc.password.presentation.auth.dto

import com.fidc.password.application.auth.usecase.SendTokenOutput
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Resposta do envio de token de verificação")
data class SendTokenResponse(
    @Schema(
        description = "Mensagem descritiva do resultado",
        example = "Token enviado com sucesso",
        required = true
    )
    val message: String,

    @Schema(
        description = "Tempo em minutos até a expiração do token",
        example = "10",
        minimum = "1",
        required = true
    )
    val expirationTimeMinutes: Long,

    @Schema(
        description = "Email mascarado onde o token foi enviado",
        example = "jo***o@email.com",
        required = true
    )
    val clientEmail: String
)

fun SendTokenOutput.toResponse(): SendTokenResponse {
    return SendTokenResponse(
        message = this.message,
        expirationTimeMinutes = this.expirationTimeMinutes,
        clientEmail = this.clientEmail
    )
}