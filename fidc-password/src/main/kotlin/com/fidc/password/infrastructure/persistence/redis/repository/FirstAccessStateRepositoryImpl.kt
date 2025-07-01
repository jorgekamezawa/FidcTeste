package com.fidc.password.infrastructure.persistence.redis.repository

import com.fasterxml.jackson.databind.ObjectMapper
import com.fidc.password.domain.auth.entity.FirstAccessState
import com.fidc.password.domain.auth.repository.FirstAccessStateRepository
import com.fidc.password.infrastructure.config.RedisProperties
import com.fidc.password.infrastructure.persistence.redis.entity.FirstAccessRedisEntity
import com.fidc.password.infrastructure.persistence.redis.entity.toDomainEntity
import com.fidc.password.infrastructure.persistence.redis.entity.toRedisEntity
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Repository
import java.util.concurrent.TimeUnit

@Repository
class FirstAccessStateRepositoryImpl(
    private val redisTemplate: RedisTemplate<String, Any>,
    private val redisProperties: RedisProperties,
    private val objectMapper: ObjectMapper
) : FirstAccessStateRepository {

    private val logger = LoggerFactory.getLogger(this::class.java)

    override fun save(state: FirstAccessState, ttlMinutes: Int): FirstAccessState {
        val key = generateRedisKey(state.creditorName, state.cpf)
        val redisEntity = state.toRedisEntity()

        logger.debug("Salvando estado no Redis: key=$key, ttl=${ttlMinutes}min")

        try {
            redisTemplate.opsForValue().set(key, redisEntity, ttlMinutes.toLong(), TimeUnit.MINUTES)
            logger.debug("Estado salvo com sucesso no Redis")
            return state
        } catch (e: Exception) {
            logger.error("Erro ao salvar estado no Redis: key=$key", e)
            throw RuntimeException("Erro ao salvar estado no Redis", e)
        }
    }

    override fun findByCreditorAndCpf(creditorName: String, cpf: String): FirstAccessState? {
        val key = generateRedisKey(creditorName, cpf)

        logger.debug("Buscando estado no Redis: key=$key")

        return try {
            val value = redisTemplate.opsForValue().get(key)

            if (value != null) {
                val redisEntity = when (value) {
                    is FirstAccessRedisEntity -> value
                    is Map<*, *> -> {
                        try {
                            val json = objectMapper.writeValueAsString(value)
                            objectMapper.readValue(json, FirstAccessRedisEntity::class.java)
                        } catch (e: Exception) {
                            logger.error("Erro ao deserializar estado do Redis: key=$key", e)
                            return null
                        }
                    }
                    else -> {
                        logger.warn("Tipo de objeto inesperado no Redis: key=$key, type=${value::class.java}")
                        return null
                    }
                }

                logger.debug("Estado encontrado no Redis")
                redisEntity.toDomainEntity()
            } else {
                logger.debug("Estado não encontrado no Redis")
                null
            }
        } catch (e: Exception) {
            logger.error("Erro ao buscar estado no Redis: key=$key", e)
            throw RuntimeException("Erro ao buscar estado no Redis", e)
        }
    }

    override fun existsByCreditorAndCpf(creditorName: String, cpf: String): Boolean {
        val key = generateRedisKey(creditorName, cpf)

        return try {
            val exists = redisTemplate.hasKey(key)
            logger.debug("Verificação de existência no Redis: key=$key, exists=$exists")
            exists
        } catch (e: Exception) {
            logger.error("Erro ao verificar existência no Redis: key=$key", e)
            throw RuntimeException("Erro ao verificar existência no Redis", e)
        }
    }

    override fun deleteByCreditorAndCpf(creditorName: String, cpf: String) {
        val key = generateRedisKey(creditorName, cpf)

        logger.debug("Removendo estado do Redis: key=$key")

        try {
            val deleted = redisTemplate.delete(key)
            if (deleted) {
                logger.debug("Estado removido com sucesso do Redis")
            } else {
                logger.debug("Estado não existia no Redis para remoção")
            }
        } catch (e: Exception) {
            logger.error("Erro ao remover estado do Redis: key=$key", e)
            throw RuntimeException("Erro ao remover estado do Redis", e)
        }
    }

    override fun generateRedisKey(creditorName: String, cpf: String): String {
        val keyPrefix = redisProperties.firstAccess.keyPrefix
        return "${keyPrefix}:${creditorName.lowercase()}:$cpf"
    }
}