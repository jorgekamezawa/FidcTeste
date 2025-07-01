package com.fidc.password.presentation.auth.swagger

import com.fidc.password.presentation.auth.dto.SendTokenRequest
import com.fidc.password.presentation.auth.dto.SendTokenResponse
import com.fidc.password.presentation.common.dto.ErrorResponse
import com.fidc.password.presentation.common.dto.ValidationErrorResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader

@Tag(
    name = "Autenticação",
    description = "Operações para primeiro acesso e redefinição de senha"
)
interface AuthRestApiDoc {

    @Operation(
        summary = "Enviar token de verificação",
        description = "Envia um token de verificação por email para início do processo de primeiro acesso ou redefinição de senha. " +
                "O usuário deve existir na base de leads do credor e a data de nascimento deve conferir com os dados cadastrados."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Token enviado com sucesso",
                content = [Content(
                    schema = Schema(implementation = SendTokenResponse::class),
                    mediaType = "application/json"
                )]
            ),
            ApiResponse(
                responseCode = "400",
                description = "Dados de entrada inválidos - erros de validação",
                content = [Content(
                    schema = Schema(implementation = ValidationErrorResponse::class),
                    mediaType = "application/json"
                )]
            ),
            ApiResponse(
                responseCode = "404",
                description = "Usuário não encontrado na base de leads",
                content = [Content(
                    schema = Schema(implementation = ErrorResponse::class),
                    mediaType = "application/json"
                )]
            )
        ]
    )
    fun sendToken(
        @Valid @RequestBody request: SendTokenRequest,
        @RequestHeader("origin") origin: String
    ): SendTokenResponse
}
