package com.fidc.password.domain.auth.exception

sealed class AuthDomainException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)

class UserNotFoundInUserManagementException(documentNumber: String, creditorName: String) :
    AuthDomainException("Usuário não encontrado na base de leads: CPF=$documentNumber, Credor=$creditorName")

class InvalidBirthDateException : AuthDomainException("Data de nascimento não confere!")

class TokenSendFailedException(clientEmail: String, reason: String? = null) :
    AuthDomainException("Falha ao enviar token para o email $clientEmail${reason?.let { ": $it" } ?: ""}")

class InvalidCreditorNameException(creditorName: String) :
    AuthDomainException("Nome do credor inválido: $creditorName")

class InvalidCpfFormatException(cpf: String) :
    AuthDomainException("Formato de CPF inválido: $cpf")

class FirstAccessStateExpiredException(creditorName: String, cpf: String) :
    AuthDomainException("Estado de primeiro acesso expirado para CPF=$cpf, Credor=$creditorName")

class UserManagementIntegrationException(message: String, cause: Throwable? = null) :
    AuthDomainException("Erro na integração com Sistema Core: $message", cause)

class BankingTicketIntegrationException(message: String, cause: Throwable? = null) :
    AuthDomainException("Erro na integração com Banking Ticket: $message", cause)