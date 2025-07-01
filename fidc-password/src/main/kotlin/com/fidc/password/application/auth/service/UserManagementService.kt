package com.fidc.password.application.auth.service

import com.fidc.password.domain.auth.dto.UserDetailResult

interface UserManagementService {
    fun getUserDetails(documentNumber: String, creditorName: String): UserDetailResult?
}