package com.fidc.password.infrastructure.external.usermanagement.dto

import com.fasterxml.jackson.annotation.JsonFormat
import com.fidc.password.domain.auth.dto.CreditorResult
import com.fidc.password.domain.auth.dto.RelationshipResult
import com.fidc.password.domain.auth.dto.UserDetailResult
import java.time.LocalDate

data class UserManagementUserDetailResponse(
    val documentNumber: String,
    val name: String,
    val email: String,
    val phoneNumber: String?,
    @JsonFormat(pattern = "yyyy-MM-dd")
    val birthDate: LocalDate,
    val creditor: UserManagementCreditorResponse,
    val relationshipList: List<UserManagementRelationshipResponse>
)

data class UserManagementCreditorResponse(
    val name: String,
    val cge: String,
    val documentNumber: String
)

data class UserManagementRelationshipResponse(
    val id: Long,
    val type: String,
    val name: String
)

fun UserManagementUserDetailResponse.toDomainDto(): UserDetailResult {
    return UserDetailResult(
        documentNumber = this.documentNumber,
        name = this.name,
        email = this.email,
        phoneNumber = this.phoneNumber,
        birthDate = this.birthDate,
        creditor = this.creditor.toDomainDto(),
        relationshipList = this.relationshipList.map { it.toDomainDto() }
    )
}

fun UserManagementCreditorResponse.toDomainDto(): CreditorResult {
    return CreditorResult(
        name = this.name,
        cge = this.cge,
        documentNumber = this.documentNumber
    )
}

fun UserManagementRelationshipResponse.toDomainDto(): RelationshipResult {
    return RelationshipResult(
        id = this.id,
        type = this.type,
        name = this.name
    )
}
