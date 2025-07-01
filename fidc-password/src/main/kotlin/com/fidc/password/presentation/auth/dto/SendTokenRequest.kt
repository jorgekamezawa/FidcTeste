package com.fidc.password.presentation.auth.dto

import com.fidc.password.application.auth.usecase.SendTokenInput
import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Past
import jakarta.validation.constraints.Pattern
import java.time.LocalDate

@Schema(description = "Dados para envio de token de verificação")
data class SendTokenRequest(
    @field:NotBlank(message = "CPF é obrigatório")
    @field:Pattern(
        regexp = "^\\d{3}\\.?\\d{3}\\.?\\d{3}-?\\d{2}$|^\\d{11}$",
        message = "CPF deve ter formato válido (11 dígitos com ou sem máscara)"
    )
    @JsonProperty("cpf")
    @Schema(
        description = "CPF do usuário (com ou sem máscara)",
        example = "12345678901",
        pattern = "^\\d{3}\\.?\\d{3}\\.?\\d{3}-?\\d{2}$|^\\d{11}$",
        required = true
    )
    val cpf: String,

    @field:NotNull(message = "Data de nascimento é obrigatória")
    @field:Past(message = "Data de nascimento deve ser no passado")
    @JsonProperty("birthDate")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(
        description = "Data de nascimento do usuário",
        example = "1990-05-15",
        format = "date",
        required = true
    )
    val birthDate: LocalDate
)

fun SendTokenRequest.toInput(origin: String): SendTokenInput {
    return SendTokenInput(
        cpf = this.cpf,
        birthDate = this.birthDate,
        origin = origin
    )
}