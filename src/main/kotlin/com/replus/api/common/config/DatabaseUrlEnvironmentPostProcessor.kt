package com.replus.api.common.config

import org.springframework.boot.EnvironmentPostProcessor
import org.springframework.boot.SpringApplication
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor
import org.springframework.core.Ordered
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.env.MapPropertySource
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

class DatabaseUrlEnvironmentPostProcessor : EnvironmentPostProcessor, Ordered {
    override fun postProcessEnvironment(environment: ConfigurableEnvironment, application: SpringApplication) {
        val explicitDatasourceUrl = environment.getProperty(SPRING_DATASOURCE_URL).orEmpty().trim()
        if (explicitDatasourceUrl.isNotBlank()) {
            return
        }

        val databaseUrl = environment.getProperty(DATABASE_URL).orEmpty().trim()
        if (databaseUrl.isBlank()) {
            return
        }

        environment.propertySources.addFirst(
            MapPropertySource(
                PROPERTY_SOURCE_NAME,
                RenderPostgresDatabaseUrl.parse(databaseUrl)
                    .toSpringDatasourceProperties(environment),
            ),
        )
    }

    override fun getOrder(): Int = ConfigDataEnvironmentPostProcessor.ORDER + 1

    private class RenderPostgresDatabaseUrl(
        private val host: String,
        private val port: Int,
        private val databaseName: String,
        private val query: String?,
        private val username: String?,
        private val password: String?,
    ) {
        fun toSpringDatasourceProperties(environment: ConfigurableEnvironment): Map<String, String> {
            val properties = linkedMapOf(
                SPRING_DATASOURCE_URL to buildJdbcUrl(),
            )
            if (username != null && environment.getProperty(SPRING_DATASOURCE_USERNAME).isNullOrBlank()) {
                properties[SPRING_DATASOURCE_USERNAME] = username
            }
            if (password != null && environment.getProperty(SPRING_DATASOURCE_PASSWORD).isNullOrBlank()) {
                properties[SPRING_DATASOURCE_PASSWORD] = password
            }
            return properties
        }

        private fun buildJdbcUrl(): String =
            buildString {
                append("jdbc:postgresql://")
                append(host)
                if (port > 0) {
                    append(":")
                    append(port)
                }
                append("/")
                append(databaseName)
                if (!query.isNullOrBlank()) {
                    append("?")
                    append(query)
                }
            }

        companion object {
            fun parse(rawDatabaseUrl: String): RenderPostgresDatabaseUrl {
                val uri = URI(rawDatabaseUrl)
                check(uri.scheme.equals("postgresql", ignoreCase = true)) {
                    "DATABASE_URL must use postgresql://"
                }
                val databaseName = uri.rawPath
                    .orEmpty()
                    .removePrefix("/")
                check(databaseName.isNotBlank()) {
                    "DATABASE_URL must include a database name"
                }
                val userInfoParts = uri.rawUserInfo
                    ?.split(":", limit = 2)
                    ?: emptyList()
                return RenderPostgresDatabaseUrl(
                    host = checkNotNull(uri.host) { "DATABASE_URL must include a host" },
                    port = uri.port,
                    databaseName = databaseName,
                    query = uri.rawQuery,
                    username = userInfoParts.getOrNull(0)?.urlDecode(),
                    password = userInfoParts.getOrNull(1)?.urlDecode(),
                )
            }

            private fun String.urlDecode(): String =
                URLDecoder.decode(this, StandardCharsets.UTF_8)
        }
    }

    private companion object {
        const val DATABASE_URL = "DATABASE_URL"
        const val PROPERTY_SOURCE_NAME = "renderDatabaseUrl"
        const val SPRING_DATASOURCE_PASSWORD = "spring.datasource.password"
        const val SPRING_DATASOURCE_URL = "spring.datasource.url"
        const val SPRING_DATASOURCE_USERNAME = "spring.datasource.username"
    }
}
