package com.fidc.password.infrastructure.config

import jakarta.servlet.Filter
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.MDC
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import java.util.*

@Component
@Order(1)
class CorrelationIdFilter : Filter {

    companion object {
        private const val CORRELATION_ID_KEY = "correlationId"
        private const val CORRELATION_ID_HEADER = "X-Correlation-ID"
    }

    override fun doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
        val httpRequest = request as HttpServletRequest

        val correlationId = httpRequest.getHeader(CORRELATION_ID_HEADER) ?: generateCorrelationId()

        try {
            MDC.put(CORRELATION_ID_KEY, correlationId)
            chain.doFilter(request, response)
        } finally {
            MDC.clear()
        }
    }

    private fun generateCorrelationId(): String {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16)
    }
}