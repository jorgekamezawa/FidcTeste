package com.fidc.password.infrastructure.config

import feign.RequestInterceptor
import feign.RequestTemplate
import org.slf4j.MDC
import org.springframework.stereotype.Component

@Component
class FeignCorrelationIdInterceptor : RequestInterceptor {

    companion object {
        private const val CORRELATION_ID_HEADER = "X-Correlation-ID"
    }

    override fun apply(requestTemplate: RequestTemplate) {
        val correlationId = MDC.get("correlationId")
        if (correlationId != null) {
            requestTemplate.header(CORRELATION_ID_HEADER, correlationId)
        }
    }
}