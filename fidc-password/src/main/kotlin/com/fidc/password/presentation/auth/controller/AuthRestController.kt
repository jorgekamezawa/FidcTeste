package com.fidc.password.presentation.auth.controller

import com.fidc.password.application.auth.usecase.SendTokenUseCase
import com.fidc.password.domain.utils.maskDocumentNumber
import com.fidc.password.presentation.auth.dto.SendTokenRequest
import com.fidc.password.presentation.auth.dto.SendTokenResponse
import com.fidc.password.presentation.auth.dto.toInput
import com.fidc.password.presentation.auth.dto.toResponse
import com.fidc.password.presentation.auth.swagger.AuthRestApiDoc
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/auth")
class AuthRestController(
    private val sendTokenUseCase: SendTokenUseCase
) : AuthRestApiDoc {

    private val logger = LoggerFactory.getLogger(this::class.java)

    @PostMapping("/send-token")
    @ResponseStatus(HttpStatus.OK)
    override fun sendToken(
        @Valid @RequestBody request: SendTokenRequest,
        @RequestHeader("origin") origin: String
    ): SendTokenResponse {
        logger.info("Recebida requisição de envio de token: cpf=${request.cpf.maskDocumentNumber()}, origin=$origin")

        val output = sendTokenUseCase.execute(request.toInput(origin))
        val response = output.toResponse()

        logger.info("Token enviado com sucesso: expiresIn=${output.expirationTimeMinutes}min")
        return response
    }
}