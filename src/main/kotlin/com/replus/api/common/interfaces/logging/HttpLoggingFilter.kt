package com.replus.api.common.interfaces.logging

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.UUID

@Component
class HttpLoggingFilter : OncePerRequestFilter() {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val requestId = request.getHeader(REQUEST_ID_HEADER)?.takeIf { it.isNotBlank() }
            ?: UUID.randomUUID().toString()
        val startedAt = System.currentTimeMillis()
        MDC.put("requestId", requestId)
        response.setHeader(REQUEST_ID_HEADER, requestId)

        try {
            log.info(
                "http_request_started requestId={} method={} path={} query={}",
                requestId,
                request.method,
                request.requestURI,
                request.queryString ?: "",
            )
            filterChain.doFilter(request, response)
        } finally {
            log.info(
                "http_request_finished requestId={} method={} path={} status={} durationMs={}",
                requestId,
                request.method,
                request.requestURI,
                response.status,
                System.currentTimeMillis() - startedAt,
            )
            MDC.remove("requestId")
        }
    }

    companion object {
        private const val REQUEST_ID_HEADER = "X-Request-Id"
    }
}
