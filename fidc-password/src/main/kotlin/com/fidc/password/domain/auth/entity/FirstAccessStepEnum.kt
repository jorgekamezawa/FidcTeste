package com.fidc.password.domain.auth.entity

enum class FirstAccessStepEnum(val description: String) {
    TOKEN_SENT("Token Enviado"),
    TOKEN_VALIDATED("Token Validado");

    companion object {
        fun fromValue(value: String): FirstAccessStepEnum? =
            try {
                valueOf(value.uppercase())
            } catch (e: IllegalArgumentException) {
                null
            }
    }

    fun isTokenSent() = this == TOKEN_SENT
    fun isTokenCalidated() = this == TOKEN_VALIDATED
}