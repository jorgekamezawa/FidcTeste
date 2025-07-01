package com.fidc.password.domain.utils

/**
 * Mascara um documento (CPF ou CNPJ) exibindo apenas os últimos 4 dígitos
 * Valida se o documento tem 11 dígitos (CPF) ou 14 dígitos (CNPJ)
 * Se não for um documento válido, retorna a string original
 *
 * Exemplos:
 * - "12345678901" (CPF) -> "***8901"
 * - "123.456.789-01" (CPF) -> "***8901"
 * - "12345678000123" (CNPJ) -> "***0123"
 * - "12.345.678/0001-23" (CNPJ) -> "***0123"
 * - "123456789" (inválido) -> "123456789"
 * - "" -> ""
 *
 * @return Documento mascarado no formato ***XXXX ou string original se inválido
 */
fun String.maskDocumentNumber(): String {
    if (this.isBlank()) return this

    val digitsOnly = this.replace(Regex("[^0-9]"), "")

    return when (digitsOnly.length) {
        11 -> "***${digitsOnly.takeLast(4)}"
        14 -> "***${digitsOnly.takeLast(4)}"
        else -> this
    }
}

/**
 * Mascara um email exibindo apenas os primeiros 2 caracteres do local part
 * e os últimos 1 caractere antes do @, mantendo o domínio completo
 *
 * Exemplos:
 * - "joao.silva@email.com" -> "jo***a@email.com"
 * - "ab@test.com" -> "ab@test.com" (muito pequeno, não mascara)
 * - "a@test.com" -> "a@test.com" (muito pequeno, não mascara)
 * - "teste@email.com" -> "te***e@email.com"
 * - "email-invalido" -> "email-invalido" (formato inválido, retorna original)
 *
 * @return Email mascarado no formato XX***X@domain.com
 */
fun String.maskEmail(): String {
    if (this.isBlank()) return this

    val parts = this.split("@")
    if (parts.size != 2) return this

    val localPart = parts[0]
    val domain = parts[1]

    val maskedLocal = when {
        localPart.length <= 3 -> localPart
        else -> "${localPart.take(2)}***${localPart.takeLast(1)}"
    }

    return "$maskedLocal@$domain"
}

/**
 * Mascara um telefone exibindo apenas os últimos 4 dígitos
 *
 * Exemplos:
 * - "11999887766" -> "***7766"
 * - "(11) 99988-7766" -> "***7766"
 * - "+55 11 99988-7766" -> "***7766"
 * - "123" -> "***123"
 *
 * @return Telefone mascarado no formato ***XXXX
 */
fun String.maskPhone(): String {
    if (this.isBlank()) return "***"

    val digitsOnly = this.replace(Regex("[^0-9]"), "")

    return when {
        digitsOnly.length >= 4 -> "***${digitsOnly.takeLast(4)}"
        digitsOnly.isNotEmpty() -> "***${digitsOnly}"
        else -> "***"
    }
}

/**
 * Mascara uma string genérica exibindo apenas os primeiros e últimos caracteres
 * Só mascara se sobrarem pelo menos 2 caracteres para mascarar (senão não faz sentido)
 *
 * @param visibleStart Número de caracteres visíveis no início (default: 2)
 * @param visibleEnd Número de caracteres visíveis no final (default: 2)
 * @param maskChar Caractere usado para mascarar (default: '*')
 * @param maskLength Tamanho da máscara (default: 3)
 *
 * Exemplos:
 * - "1234567890".maskGeneric() -> "12***90" (8 chars mascarados, ok)
 * - "1234567890".maskGeneric(3, 3, '*', 4) -> "123****890" (4 chars mascarados, ok)
 * - "123456".maskGeneric() -> "12***56" (2 chars mascarados, ok)
 * - "12345".maskGeneric() -> "12345" (só 1 char sobraria, não mascara)
 * - "abc".maskGeneric() -> "abc" (muito pequeno, não mascara)
 */
fun String.maskGeneric(
    visibleStart: Int = 2,
    visibleEnd: Int = 2,
    maskChar: Char = '*',
    maskLength: Int = 3
): String {
    // Precisa sobrar pelo menos 2 caracteres para mascarar
    if (this.length <= visibleStart + visibleEnd + 1) return this

    val start = this.take(visibleStart)
    val end = this.takeLast(visibleEnd)
    val mask = maskChar.toString().repeat(maskLength)

    return "$start$mask$end"
}

/**
 * Remove todos os caracteres não numéricos de uma string
 * Útil para limpeza de CPF, telefone, etc.
 *
 * Exemplos:
 * - "123.456.789-01" -> "12345678901"
 * - "(11) 99988-7766" -> "11999887766"
 * - "abc123def456" -> "123456"
 */
fun String.digitsOnly(): String {
    return this.replace(Regex("[^0-9]"), "")
}