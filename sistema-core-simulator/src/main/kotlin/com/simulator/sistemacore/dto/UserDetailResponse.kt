package com.simulator.sistemacore.dto

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonProperty
import com.simulator.sistemacore.model.Creditor
import com.simulator.sistemacore.model.Relationship
import com.simulator.sistemacore.model.User
import java.time.LocalDate

data class UserDetailResponse(
    @JsonProperty("documentNumber")
    val documentNumber: String,

    @JsonProperty("name")
    val name: String,

    @JsonProperty("email")
    val email: String,

    @JsonProperty("phoneNumber")
    val phoneNumber: String,

    @JsonProperty("birthDate")
    @JsonFormat(pattern = "yyyy-MM-dd")
    val birthDate: LocalDate,

    @JsonProperty("creditor")
    val creditor: CreditorResponse,

    @JsonProperty("relationshipList")
    val relationshipList: List<RelationshipResponse>
)

data class CreditorResponse(
    @JsonProperty("name")
    val name: String,

    @JsonProperty("cge")
    val cge: String,

    @JsonProperty("documentNumber")
    val documentNumber: String
)

data class RelationshipResponse(
    @JsonProperty("id")
    val id: Long,

    @JsonProperty("type")
    val type: String,

    @JsonProperty("name")
    val name: String
)

// Extension functions
fun User.toResponse(): UserDetailResponse {
    return UserDetailResponse(
        documentNumber = this.documentNumber,
        name = this.name,
        email = this.email,
        phoneNumber = this.phoneNumber,
        birthDate = this.birthDate,
        creditor = this.creditor.toResponse(),
        relationshipList = this.relationshipList.map { it.toResponse() }
    )
}

fun Creditor.toResponse(): CreditorResponse {
    return CreditorResponse(
        name = this.name,
        cge = this.cge,
        documentNumber = this.documentNumber
    )
}

fun Relationship.toResponse(): RelationshipResponse {
    return RelationshipResponse(
        id = this.id,
        type = this.type,
        name = this.name
    )
}