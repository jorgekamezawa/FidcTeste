package com.fidc.password.infrastructure.external.bankingticket.service

import com.fidc.password.domain.auth.dto.TokenSendByEmailDto
import com.fidc.password.domain.auth.exception.BankingTicketIntegrationException
import com.fidc.password.application.auth.service.TokenSendingService
import com.fidc.password.domain.utils.maskDocumentNumber
import com.fidc.password.domain.utils.maskEmail
import com.fidc.password.infrastructure.external.bankingticket.client.BankingTicketClient
import com.fidc.password.infrastructure.external.bankingticket.dto.BankingTicketEmailRequest
import feign.FeignException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class TokenSendingServiceImpl(
    private val bankingTicketClient: BankingTicketClient
) : TokenSendingService {

    private val logger = LoggerFactory.getLogger(this::class.java)

    override fun sendByEmail(dto: TokenSendByEmailDto) {
        logger.debug("Enviando token via Banking Ticket: document=${dto.cpf.maskDocumentNumber()}, email=${dto.email.maskEmail()}")

        try {
            val request = BankingTicketEmailRequest(
                clientDocumentNumber = dto.cpf,
                clientEmail = dto.email,
                tokenLength = dto.tokenLength,
                limitAttempts = dto.limitAttempts,
                expirationTime = dto.expirationTimeMinutes
            )

            val response = bankingTicketClient.sendTokenByEmail(request)

            if (response.success) {
                logger.debug("Token enviado com sucesso via Banking Ticket")
            } else {
                logger.error("Falha ao enviar token via Banking Ticket: ${response.message}")
                throw BankingTicketIntegrationException("Falha na comunicação com Banking Ticket: ${response.message}")
            }
        } catch (e: FeignException.BadRequest) {
            logger.error("Erro de validação no Banking Ticket (400): ${e.message}")
            throw BankingTicketIntegrationException("Dados inválidos para envio de token: ${e.message}", e)
        } catch (e: FeignException) {
            logger.error("Erro na comunicação com Banking Ticket: status=${e.status()}, message=${e.message}")
            throw BankingTicketIntegrationException("Falha na comunicação com Banking Ticket: ${e.message}", e)
        } catch (e: BankingTicketIntegrationException) {
            throw e
        } catch (e: Exception) {
            logger.error("Erro inesperado ao enviar token via Banking Ticket", e)
            throw BankingTicketIntegrationException("Erro inesperado na integração com Banking Ticket", e)
        }
    }
}