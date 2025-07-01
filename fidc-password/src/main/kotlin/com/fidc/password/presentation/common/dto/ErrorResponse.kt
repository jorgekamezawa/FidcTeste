package com.fidc.password.presentation.common.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "Resposta padrão de erro da API")
data class ErrorResponse(
    @Schema(description = "Timestamp do erro", example = "2025-06-30T10:30:00")
    val timestamp: LocalDateTime,
    @Schema(description = "Código HTTP", example = "400")
    val status: Int,
    @Schema(description = "Tipo do erro", example = "Bad Request")
    val error: String,
    @Schema(description = "Mensagem do erro", example = "Dados inválidos")
    val message: String,
    @Schema(description = "Path da requisição", example = "/auth/send-token")
    val path: String
)