package com.fidc.password.infrastructure.persistence.redis.entity

import com.fidc.password.domain.auth.entity.FirstAccessState
import com.fidc.password.domain.auth.entity.FirstAccessStepEnum
import com.fasterxml.jackson.annotation.JsonFormat
import java.time.LocalDateTime
import java.util.UUID

data class FirstAccessRedisEntity(
    val id: UUID,
    val creditorName: String,
    val cpf: String,
    val step: String,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    val createdAt: LocalDateTime
)

fun FirstAccessState.toRedisEntity(): FirstAccessRedisEntity {
    return FirstAccessRedisEntity(
        id = this.id,
        creditorName = this.creditorName,
        cpf = this.cpf,
        step = this.step.name,
        createdAt = this.createdAt
    )
}

fun FirstAccessRedisEntity.toDomainEntity(): FirstAccessState {
    val stepEnum = FirstAccessStepEnum.fromValue(this.step)
        ?: throw IllegalArgumentException("Status inválido: ${this.step}")

    return FirstAccessState.reconstruct(
        id = this.id,
        creditorName = this.creditorName,
        cpf = this.cpf,
        step = stepEnum,
        createdAt = this.createdAt
    )
}