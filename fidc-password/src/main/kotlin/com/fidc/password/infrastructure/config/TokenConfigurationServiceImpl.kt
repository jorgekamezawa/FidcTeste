package com.fidc.password.infrastructure.config

import com.fidc.password.application.auth.service.TokenConfigurationService
import org.springframework.stereotype.Service

@Service
class TokenConfigurationServiceImpl(
    private val tokenProperties: TokenProperties
) : TokenConfigurationService {
    override fun getTokenLength(): Int = tokenProperties.length
    override fun getLimitAttempts(): Int = tokenProperties.limitAttempts
    override fun getExpirationTimeMinutes(): Int = tokenProperties.expirationTimeMinutes
}