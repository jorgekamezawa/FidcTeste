package com.fidc.password.domain.auth.repository

import com.fidc.password.domain.auth.entity.FirstAccessState

interface FirstAccessStateRepository {
    fun save(state: FirstAccessState, ttlMinutes: Int): FirstAccessState
    fun findByCreditorAndCpf(creditorName: String, cpf: String): FirstAccessState?
    fun existsByCreditorAndCpf(creditorName: String, cpf: String): Boolean
    fun deleteByCreditorAndCpf(creditorName: String, cpf: String)
    fun generateRedisKey(creditorName: String, cpf: String): String
}