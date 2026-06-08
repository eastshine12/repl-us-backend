package com.replus.api.common.config

import org.springframework.beans.factory.config.BeanFactoryPostProcessor
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment

@Configuration(proxyBeanMethods = false)
class ProductionProfileGuardConfig {
    @Bean
    fun productionProfileGuard(environment: Environment): BeanFactoryPostProcessor =
        BeanFactoryPostProcessor {
            if (environment.activeProfiles.none { it.equals(PROD_PROFILE, ignoreCase = true) }) {
                return@BeanFactoryPostProcessor
            }

            val datasourceUrl = environment.getProperty("spring.datasource.url").orEmpty().trim()
            require(datasourceUrl.isNotBlank()) {
                "SPRING_DATASOURCE_URL is required when the prod profile is active"
            }
            require(!datasourceUrl.startsWith("jdbc:h2:", ignoreCase = true)) {
                "Prod profile must not use an H2 datasource"
            }
            require(!environment.getBooleanProperty("replus.seed-dev-data", defaultValue = false)) {
                "replus.seed-dev-data must be false when the prod profile is active"
            }
            require(!environment.getBooleanProperty("spring.h2.console.enabled", defaultValue = false)) {
                "spring.h2.console.enabled must be false when the prod profile is active"
            }
            val corsAllowedOrigins = environment.getCommaSeparatedProperty("replus.web.cors.allowed-origins")
            require(corsAllowedOrigins.isNotEmpty()) {
                "replus.web.cors.allowed-origins is required when the prod profile is active"
            }
            require(corsAllowedOrigins.none { it == "*" || it.contains("*") }) {
                "Prod profile must not allow wildcard CORS origins"
            }
        }

    private fun Environment.getBooleanProperty(name: String, defaultValue: Boolean): Boolean =
        getProperty(name, Boolean::class.java, defaultValue)

    private fun Environment.getCommaSeparatedProperty(name: String): List<String> =
        getProperty(name)
            .orEmpty()
            .split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }

    private companion object {
        const val PROD_PROFILE = "prod"
    }
}
