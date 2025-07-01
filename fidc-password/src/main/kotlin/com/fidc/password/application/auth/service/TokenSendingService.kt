package com.fidc.password.application.auth.service

import com.fidc.password.domain.auth.dto.TokenSendByEmailDto

interface TokenSendingService {
    fun sendByEmail(dto: TokenSendByEmailDto)
}