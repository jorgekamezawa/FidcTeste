package com.fidc.password.domain.auth.entity

import com.fidc.password.domain.constants.BusinessConstants
import com.fidc.password.domain.utils.digitsOnly
import com.fidc.password.domain.utils.maskDocumentNumber
import java.time.LocalDateTime
import java.util.*

class FirstAccessState private constructor(
    private val _id: UUID,
    private val _creditorName: String,
    private val _cpf: String,
    private var _step: FirstAccessStepEnum,
    private val _createdAt: LocalDateTime
) {
    val id: UUID get() = _id
    val creditorName: String get() = _creditorName
    val cpf: String get() = _cpf
    val step: FirstAccessStepEnum get() = _step
    val createdAt: LocalDateTime get() = _createdAt

    companion object {
        fun create(creditorName: String, cpf: String): FirstAccessState {
            validateCreditorName(creditorName)
            validateCpf(cpf)

            return FirstAccessState(
                _id = UUID.randomUUID(),
                _creditorName = creditorName.trim().lowercase(),
                _cpf = cpf.digitsOnly(),
                _step = FirstAccessStepEnum.TOKEN_SENT,
                _createdAt = LocalDateTime.now()
            )
        }

        fun reconstruct(
            id: UUID,
            creditorName: String,
            cpf: String,
            step: FirstAccessStepEnum,
            createdAt: LocalDateTime
        ): FirstAccessState {
            return FirstAccessState(
                _id = id,
                _creditorName = creditorName,
                _cpf = cpf,
                _step = step,
                _createdAt = createdAt
            )
        }

        private fun validateCreditorName(creditorName: String) {
            require(creditorName.isNotBlank()) { "Nome do credor não pode estar vazio" }
            require(creditorName.trim().length in BusinessConstants.MIN_NAME_LENGTH..BusinessConstants.MAX_NAME_LENGTH) { "Nome do credor deve ter entre 2 e 50 caracteres" }
        }

        private fun validateCpf(cpf: String) {
            require(cpf.isNotBlank()) { "CPF não pode estar vazio" }
            val cleanCpf = cpf.digitsOnly()
            require(cleanCpf.length == BusinessConstants.CPF_LENGTH) { "CPF deve conter exatamente 11 dígitos" }
            require(cleanCpf.all { it.isDigit() }) { "CPF deve conter apenas dígitos" }
        }
    }

    fun markTokenSent(): FirstAccessState {
        require(_step == FirstAccessStepEnum.TOKEN_SENT) { "Estado deve ser TOKEN_SENT para marcar como enviado" }
        return this
    }

    fun canReceiveNewToken(): Boolean {
        return _step == FirstAccessStepEnum.TOKEN_SENT
    }

    fun isExpired(ttlMinutes: Int): Boolean {
        val expirationTime = _createdAt.plusMinutes(ttlMinutes.toLong())
        return LocalDateTime.now().isAfter(expirationTime)
    }

    fun getRemainingTimeMinutes(ttlMinutes: Int): Long {
        val expirationTime = _createdAt.plusMinutes(ttlMinutes.toLong())
        val now = LocalDateTime.now()

        return if (now.isBefore(expirationTime)) {
            java.time.Duration.between(now, expirationTime).toMinutes()
        } else {
            0L
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FirstAccessState) return false
        return _id == other._id
    }

    override fun hashCode(): Int {
        return _id.hashCode()
    }

    override fun toString(): String {
        return "FirstAccessState(id=$_id, creditorName=$_creditorName, cpf=${_cpf.maskDocumentNumber()}, status=${_step.description})"
    }
}