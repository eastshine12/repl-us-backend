package com.replus.api.common.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(CorsProperties::class)
class CorsConfig(
    private val corsProperties: CorsProperties,
) : WebMvcConfigurer {
    override fun addCorsMappings(registry: CorsRegistry) {
        val allowedOrigins = corsProperties.normalizedAllowedOrigins()
        if (allowedOrigins.isEmpty()) {
            return
        }

        registry.addMapping("/api/**")
            .allowedOrigins(*allowedOrigins.toTypedArray())
            .allowedMethods("GET", "POST", "PATCH", "DELETE", "OPTIONS")
            .allowedHeaders("authorization", "content-type", "x-request-id")
            .exposedHeaders("Location")
            .maxAge(3600)
    }
}

@ConfigurationProperties(prefix = "replus.web.cors")
data class CorsProperties(
    val allowedOrigins: List<String> = emptyList(),
) {
    fun normalizedAllowedOrigins(): List<String> =
        allowedOrigins
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
}
