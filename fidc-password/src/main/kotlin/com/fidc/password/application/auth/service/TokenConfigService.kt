package com.fidc.password.application.auth.service

interface TokenConfigurationService {
    fun getTokenLength(): Int
    fun getLimitAttempts(): Int
    fun getExpirationTimeMinutes(): Int
}