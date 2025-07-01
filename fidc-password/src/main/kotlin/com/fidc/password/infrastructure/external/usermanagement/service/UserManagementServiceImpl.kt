package com.fidc.password.infrastructure.external.usermanagement.service

import com.fidc.password.domain.auth.dto.UserDetailResult
import com.fidc.password.domain.auth.exception.UserManagementIntegrationException
import com.fidc.password.application.auth.service.UserManagementService
import com.fidc.password.domain.utils.maskDocumentNumber
import com.fidc.password.domain.utils.maskEmail
import com.fidc.password.infrastructure.external.usermanagement.client.FidcUserManagementClient
import com.fidc.password.infrastructure.external.usermanagement.dto.toDomainDto
import feign.FeignException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class UserManagementServiceImpl(
    private val fidcUserManagementClient: FidcUserManagementClient
) : UserManagementService {

    private val logger = LoggerFactory.getLogger(this::class.java)

    override fun getUserDetails(documentNumber: String, creditorName: String): UserDetailResult? {
        logger.debug("Consultando detalhes do usuário: documentNumber=${documentNumber.maskDocumentNumber()}, creditorName=$creditorName")

        return try {
            val response = fidcUserManagementClient.getUserDetails(documentNumber, creditorName)

            if (response == null) {
                logger.debug("Usuário não encontrado no User Management")
                return null
            }

            val userCoreInfo = response.toDomainDto()
            logger.debug("Usuário encontrado: name=${userCoreInfo.name}, email=${userCoreInfo.email.maskEmail()}")

            userCoreInfo

        } catch (e: FeignException.NotFound) {
            logger.debug("Usuário não encontrado no User Management (404)")
            null
        } catch (e: FeignException) {
            logger.error("Erro na comunicação com User Management: status=${e.status()}, message=${e.message}")
            throw UserManagementIntegrationException("Falha na comunicação com User Management: ${e.message}", e)
        } catch (e: Exception) {
            logger.error("Erro inesperado ao consultar User Management", e)
            throw UserManagementIntegrationException("Erro inesperado na integração com User Management", e)
        }
    }
}