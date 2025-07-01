package com.fidc.password.presentation.common.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "Resposta de erro com detalhes de validação")
data class ValidationErrorResponse(
    @Schema(description = "Timestamp do erro", example = "2025-06-30T10:30:00")
    val timestamp: LocalDateTime,
    @Schema(description = "Código HTTP", example = "400")
    val status: Int,
    @Schema(description = "Tipo do erro", example = "Bad Request")
    val error: String,
    @Schema(description = "Mensagem geral", example = "Dados de entrada inválidos")
    val message: String,
    @Schema(description = "Erros específicos por campo")
    val errors: Map<String, String>
)