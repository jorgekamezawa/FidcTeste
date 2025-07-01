package com.simulator.sistemacore.model

import java.time.LocalDate

data class User(
    val documentNumber: String,
    val name: String,
    val email: String,
    val phoneNumber: String,
    val birthDate: LocalDate,
    val creditor: Creditor,
    val relationshipList: List<Relationship>
)

data class Creditor(
    val name: String,
    val cge: String,
    val documentNumber: String
)

data class Relationship(
    val id: Long,
    val type: String,
    val name: String
)