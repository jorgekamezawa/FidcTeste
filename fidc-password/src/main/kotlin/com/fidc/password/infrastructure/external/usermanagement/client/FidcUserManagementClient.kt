package com.fidc.password.infrastructure.external.usermanagement.client

import com.fidc.password.infrastructure.external.usermanagement.dto.UserManagementUserDetailResponse
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader

@FeignClient(name = "sistema-core", url = "\${external-apis.sistema-core.base-url}")
interface FidcUserManagementClient {

    @GetMapping("/sistemacore/users/detail")
    fun getUserDetails(
        @RequestHeader("documentNumber") documentNumber: String,
        @RequestHeader("creditorName") creditorName: String
    ): UserManagementUserDetailResponse?
}