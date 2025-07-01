package com.fidc.password.infrastructure.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "app.auth.token")
data class TokenProperties(
    val length: Int = 6,
    val limitAttempts: Int = 5,
    val expirationTimeMinutes: Int = 600
) {
    fun validate() {
        require(length > 0) { "Token length deve ser positivo" }
        require(limitAttempts > 0) { "Limit attempts deve ser positivo" }
        require(expirationTimeMinutes > 0) { "Expiration time deve ser positivo" }

        if (length < 6 && limitAttempts > 3) {
            throw IllegalArgumentException(
                "Tokens com menos de 6 dígitos não devem permitir mais que 3 tentativas por segurança"
            )
        }
    }
}