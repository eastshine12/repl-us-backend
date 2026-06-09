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
            val webBaseUrl = environment.getProperty("replus.web-base-url").orEmpty().trim()
            require(webBaseUrl.isNotBlank()) {
                "replus.web-base-url is required when the prod profile is active"
            }
            require(!webBaseUrl.isLocalhostReference()) {
                "Prod profile must not use localhost as replus.web-base-url"
            }
            require(webBaseUrl.startsWith("https://", ignoreCase = true)) {
                "Prod profile requires an HTTPS replus.web-base-url"
            }
            val corsAllowedOrigins = environment.getCommaSeparatedProperty("replus.web.cors.allowed-origins")
            require(corsAllowedOrigins.isNotEmpty()) {
                "replus.web.cors.allowed-origins is required when the prod profile is active"
            }
            require(corsAllowedOrigins.none { it == "*" || it.contains("*") }) {
                "Prod profile must not allow wildcard CORS origins"
            }
            require(corsAllowedOrigins.none { it.isLocalhostReference() }) {
                "Prod profile must not use localhost CORS origins"
            }
            require(corsAllowedOrigins.all { it.startsWith("https://", ignoreCase = true) }) {
                "Prod profile requires HTTPS CORS origins"
            }
            val storageMode = environment.getProperty("replus.storage.mode").orEmpty().trim()
            if (storageMode.equals("object-storage", ignoreCase = true)) {
                val publicBaseUrl = environment
                    .getProperty("replus.storage.object-storage.public-base-url")
                    .orEmpty()
                    .trim()
                require(publicBaseUrl.isNotBlank()) {
                    "replus.storage.object-storage.public-base-url is required when object storage is enabled in prod"
                }
                require(!publicBaseUrl.isLocalhostReference()) {
                    "Prod profile must not use localhost as replus.storage.object-storage.public-base-url"
                }
                require(publicBaseUrl.startsWith("https://", ignoreCase = true)) {
                    "Prod profile requires an HTTPS replus.storage.object-storage.public-base-url"
                }
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

    private fun String.isLocalhostReference(): Boolean =
        contains("localhost", ignoreCase = true) || contains("127.0.0.1")

    private companion object {
        const val PROD_PROFILE = "prod"
    }
}
