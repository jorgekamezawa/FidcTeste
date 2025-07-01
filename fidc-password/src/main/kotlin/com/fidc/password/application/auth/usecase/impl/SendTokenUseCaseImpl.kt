package com.fidc.password.application.auth.usecase.impl

import com.fidc.password.application.auth.service.TokenConfigurationService
import com.fidc.password.application.auth.usecase.SendTokenInput
import com.fidc.password.application.auth.usecase.SendTokenOutput
import com.fidc.password.application.auth.usecase.SendTokenUseCase
import com.fidc.password.application.exception.*
import com.fidc.password.domain.auth.dto.TokenSendByEmailDto
import com.fidc.password.domain.auth.dto.UserDetailResult
import com.fidc.password.domain.auth.entity.FirstAccessState
import com.fidc.password.domain.auth.exception.AuthDomainException
import com.fidc.password.domain.auth.exception.InvalidBirthDateException
import com.fidc.password.domain.auth.exception.TokenSendFailedException
import com.fidc.password.domain.auth.exception.UserNotFoundInUserManagementException
import com.fidc.password.domain.auth.repository.FirstAccessStateRepository
import com.fidc.password.application.auth.service.TokenSendingService
import com.fidc.password.application.auth.service.UserManagementService
import com.fidc.password.domain.utils.digitsOnly
import com.fidc.password.domain.utils.maskDocumentNumber
import com.fidc.password.domain.utils.maskEmail
import com.fidc.password.infrastructure.config.RedisProperties
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
@Transactional
class SendTokenUseCaseImpl(
    private val userManagementService: UserManagementService,
    private val tokenSendingService: TokenSendingService,
    private val firstAccessStateRepository: FirstAccessStateRepository,
    private val redisProperties: RedisProperties,
    private val tokenConfigurationService: TokenConfigurationService
) : SendTokenUseCase {

    private val logger = LoggerFactory.getLogger(this::class.java)

    override fun execute(input: SendTokenInput): SendTokenOutput {
        logger.info("Iniciando envio de token: cpf=${input.cpf.maskDocumentNumber()}, origin=${input.origin}")

        try {
            validateInput(input)

            val creditorName = input.origin.trim().lowercase()
            val cleancpf = input.cpf.digitsOnly()

            val userCoreInfo = validateInUserManagement(cleancpf, creditorName)
            validateBirthDate(input.birthDate, userCoreInfo.birthDate)

            sendTokenByEmail(cleancpf, userCoreInfo.email)
            saveFirstAccessState(creditorName, cleancpf)

            val expirationMinutes = redisProperties.firstAccess.ttlMinutes.toLong()

            logger.info("Token enviado com sucesso: cpf=${cleancpf.maskDocumentNumber()}, email=${userCoreInfo.email.maskEmail()}")

            return SendTokenOutput(
                message = "Token enviado com sucesso",
                expirationTimeMinutes = expirationMinutes,
                clientEmail = userCoreInfo.email.maskEmail()
            )

        } catch (e: AuthDomainException) {
            logger.warn("Erro de domínio ao enviar token: {}", e.message)
            throw mapDomainToApplicationException(e)
        } catch (e: AuthApplicationException) {
            logger.warn("Erro de aplicação ao enviar token: {}", e.message)
            throw e
        } catch (e: Exception) {
            logger.error("Erro inesperado ao enviar token", e)
            throw GenericAuthApplicationException("Erro interno do sistema", e)
        }
    }

    private fun validateInput(input: SendTokenInput) {
        require(input.cpf.isNotBlank()) { "CPF é obrigatório" }
        require(input.origin.isNotBlank()) { "Origin é obrigatório" }
    }

    private fun validateInUserManagement(
        cpf: String,
        creditorName: String
    ): UserDetailResult {
        logger.debug("Validando usuário no User Management: cpf=${cpf.maskDocumentNumber()}, creditor=$creditorName")

        val userInfo = userManagementService.getUserDetails(cpf, creditorName)
            ?: throw UserNotFoundInUserManagementException(cpf, creditorName)

        logger.debug("Usuário encontrado no User Management: name=${userInfo.name}")
        return userInfo
    }

    private fun validateBirthDate(providedDate: LocalDate, expectedDate: LocalDate) {
        if (providedDate != expectedDate) {
            logger.error("Data de nascimento não confere: provided=$providedDate, expected=$expectedDate")
            throw InvalidBirthDateException()
        }
    }

    private fun sendTokenByEmail(cpf: String, email: String) {
        logger.debug("Enviando token para: cpf=${cpf.maskDocumentNumber()}, email=${email.maskEmail()}")

        try {
            tokenSendingService.sendByEmail(
                TokenSendByEmailDto(
                    cpf = cpf,
                    email = email,
                    tokenLength = tokenConfigurationService.getTokenLength(),
                    limitAttempts = tokenConfigurationService.getLimitAttempts(),
                    expirationTimeMinutes = tokenConfigurationService.getExpirationTimeMinutes()
                )
            )
        } catch (ex: Exception) {
            throw TokenSendFailedException(email, ex.message)
        }
        logger.debug("Token enviado com sucesso")
    }

    private fun saveFirstAccessState(creditorName: String, cpf: String) {
        logger.debug("Salvando estado no Redis: cpf=${cpf.maskDocumentNumber()}, creditor=$creditorName")

        val existingState = firstAccessStateRepository.findByCreditorAndCpf(creditorName, cpf)
        if (existingState != null) {
            logger.debug("Estado anterior encontrado, será sobrescrito")
        }

        val state = FirstAccessState.create(creditorName, cpf)
        firstAccessStateRepository.save(state, redisProperties.firstAccess.ttlMinutes)

        logger.debug("Estado salvo no Redis com TTL de {} minutos", redisProperties.firstAccess.ttlMinutes)
    }

    private fun mapDomainToApplicationException(domainException: AuthDomainException): AuthApplicationException {
        return when (domainException) {
            is UserNotFoundInUserManagementException -> UserNotFoundApplicationException(
                domainException.message ?: "Usuário não encontrado"
            )

            is InvalidBirthDateException -> InvalidBirthDateApplicationException(
                domainException.message ?: "Data de nascimento inválida"
            )

            is TokenSendFailedException -> TokenSendApplicationException(
                domainException.message ?: "Falha ao enviar token"
            )

            else -> GenericAuthApplicationException("Erro interno do sistema", domainException)
        }
    }
}