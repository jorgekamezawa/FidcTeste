package com.fidc.password.domain.auth.dto

import java.time.LocalDate

data class UserDetailResult(
    val documentNumber: String,
    val name: String,
    val email: String,
    val phoneNumber: String?,
    val birthDate: LocalDate,
    val creditor: CreditorResult,
    val relationshipList: List<RelationshipResult>
)

data class CreditorResult(
    val name: String,
    val cge: String,
    val documentNumber: String
)

data class RelationshipResult(
    val id: Long,
    val type: String,
    val name: String
)