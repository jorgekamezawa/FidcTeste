package com.fidc.password.infrastructure.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@ConfigurationProperties(prefix = "fidc.password.redis")
@Component
data class RedisProperties(
    val firstAccess: FirstAccessConfig = FirstAccessConfig()
) {
    data class FirstAccessConfig(
        val ttlMinutes: Int = 10,
        val keyPrefix: String = "first_access"
    )
}