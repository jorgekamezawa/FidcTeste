package com.fidc.password.application.exception

sealed class AuthApplicationException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)

class UserNotFoundApplicationException(message: String) :
    AuthApplicationException("Usuário não encontrado: $message")

class InvalidBirthDateApplicationException(message: String) :
    AuthApplicationException("Data de nascimento inválida: $message")

class TokenSendApplicationException(message: String, cause: Throwable? = null) :
    AuthApplicationException("Falha ao enviar token: $message", cause)

class SistemaCoreApplicationException(message: String, cause: Throwable? = null) :
    AuthApplicationException("Erro na integração com Sistema Core: $message", cause)

class BankingTicketApplicationException(message: String, cause: Throwable? = null) :
    AuthApplicationException("Erro na integração com Banking Ticket: $message", cause)

class RedisApplicationException(message: String, cause: Throwable? = null) :
    AuthApplicationException("Erro no Redis: $message", cause)

class InvalidInputApplicationException(message: String) :
    AuthApplicationException("Dados de entrada inválidos: $message")

class GenericAuthApplicationException(message: String, cause: Throwable? = null) :
    AuthApplicationException(message, cause)