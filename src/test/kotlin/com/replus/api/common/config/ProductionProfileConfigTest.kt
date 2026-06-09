package com.replus.api.common.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer
import org.springframework.boot.test.context.runner.ApplicationContextRunner

class ProductionProfileConfigTest {
    private val contextRunner = ApplicationContextRunner()
        .withInitializer(ConfigDataApplicationContextInitializer())
        .withUserConfiguration(ProductionProfileGuardConfig::class.java)

    @Test
    fun `prod profile disables development conveniences by default`() {
        contextRunner
            .withPropertyValues(*validProdProperties())
            .run { context ->
                assertThat(context).hasNotFailed()
                assertThat(context.environment.getProperty("replus.seed-dev-data", Boolean::class.java))
                    .isFalse()
                assertThat(context.environment.getProperty("spring.h2.console.enabled", Boolean::class.java))
                    .isFalse()
                assertThat(
                    context.environment.getProperty(
                        "replus.mission.lifecycle.scheduler.enabled",
                        Boolean::class.java,
                    ),
                ).isFalse()
            }
    }

    @Test
    fun `prod profile fails fast when datasource url is blank`() {
        contextRunner
            .withPropertyValues(
                *validProdProperties(
                    "spring.datasource.url= ",
                ),
            )
            .run { context ->
                assertThat(context).hasFailed()
                assertThat(context.startupFailure)
                    .hasMessageContaining("SPRING_DATASOURCE_URL is required when the prod profile is active")
            }
    }

    @Test
    fun `prod profile fails fast when datasource url points to h2`() {
        contextRunner
            .withPropertyValues(
                *validProdProperties(
                    "spring.datasource.url=jdbc:h2:mem:replus",
                ),
            )
            .run { context ->
                assertThat(context).hasFailed()
                assertThat(context.startupFailure)
                    .hasMessageContaining("Prod profile must not use an H2 datasource")
            }
    }

    @Test
    fun `prod profile fails fast when dev seed data is explicitly enabled`() {
        contextRunner
            .withPropertyValues(
                *validProdProperties(
                    "replus.seed-dev-data=true",
                ),
            )
            .run { context ->
                assertThat(context).hasFailed()
                assertThat(context.startupFailure)
                    .hasMessageContaining("replus.seed-dev-data must be false when the prod profile is active")
            }
    }

    @Test
    fun `prod profile fails fast when h2 console is explicitly enabled`() {
        contextRunner
            .withPropertyValues(
                *validProdProperties(
                    "spring.h2.console.enabled=true",
                ),
            )
            .run { context ->
                assertThat(context).hasFailed()
                assertThat(context.startupFailure)
                    .hasMessageContaining("spring.h2.console.enabled must be false when the prod profile is active")
            }
    }

    @Test
    fun `prod profile fails fast when cors allowed origins are blank`() {
        contextRunner
            .withPropertyValues(
                *validProdProperties(
                    "replus.web.cors.allowed-origins= ",
                ),
            )
            .run { context ->
                assertThat(context).hasFailed()
                assertThat(context.startupFailure)
                    .hasMessageContaining(
                        "replus.web.cors.allowed-origins is required when the prod profile is active",
                    )
            }
    }

    @Test
    fun `prod profile fails fast when cors allowed origins include wildcard`() {
        contextRunner
            .withPropertyValues(
                *validProdProperties(
                    "replus.web.cors.allowed-origins=https://app.example.test,*",
                ),
            )
            .run { context ->
                assertThat(context).hasFailed()
                assertThat(context.startupFailure)
                    .hasMessageContaining("Prod profile must not allow wildcard CORS origins")
            }
    }

    @Test
    fun `prod profile fails fast when cors allowed origins include localhost`() {
        contextRunner
            .withPropertyValues(
                *validProdProperties(
                    "replus.web.cors.allowed-origins=https://app.example.test,http://localhost:3000",
                ),
            )
            .run { context ->
                assertThat(context).hasFailed()
                assertThat(context.startupFailure)
                    .hasMessageContaining("Prod profile must not use localhost CORS origins")
            }
    }

    @Test
    fun `prod profile fails fast when cors allowed origins include non https origin`() {
        contextRunner
            .withPropertyValues(
                *validProdProperties(
                    "replus.web.cors.allowed-origins=https://app.example.test,http://preview.example.test",
                ),
            )
            .run { context ->
                assertThat(context).hasFailed()
                assertThat(context.startupFailure)
                    .hasMessageContaining("Prod profile requires HTTPS CORS origins")
            }
    }

    @Test
    fun `prod profile fails fast when web base url is blank`() {
        contextRunner
            .withPropertyValues(
                *validProdProperties(
                    "replus.web-base-url= ",
                ),
            )
            .run { context ->
                assertThat(context).hasFailed()
                assertThat(context.startupFailure)
                    .hasMessageContaining("replus.web-base-url is required when the prod profile is active")
            }
    }

    @Test
    fun `prod profile fails fast when web base url is not configured`() {
        contextRunner
            .withPropertyValues(
                *validProdProperties(without = setOf("replus.web-base-url")),
            )
            .run { context ->
                assertThat(context).hasFailed()
                assertThat(context.startupFailure)
                    .hasMessageContaining("replus.web-base-url is required when the prod profile is active")
            }
    }

    @Test
    fun `prod profile fails fast when web base url points to localhost`() {
        contextRunner
            .withPropertyValues(
                *validProdProperties(
                    "replus.web-base-url=http://localhost:3000",
                ),
            )
            .run { context ->
                assertThat(context).hasFailed()
                assertThat(context.startupFailure)
                    .hasMessageContaining("Prod profile must not use localhost as replus.web-base-url")
            }
    }

    @Test
    fun `prod profile fails fast when web base url is not https`() {
        contextRunner
            .withPropertyValues(
                *validProdProperties(
                    "replus.web-base-url=http://app.example.test",
                ),
            )
            .run { context ->
                assertThat(context).hasFailed()
                assertThat(context.startupFailure)
                    .hasMessageContaining("Prod profile requires an HTTPS replus.web-base-url")
            }
    }

    @Test
    fun `prod profile fails fast when object storage public base url is blank`() {
        contextRunner
            .withPropertyValues(
                *validProdProperties(
                    "replus.storage.object-storage.public-base-url= ",
                ),
            )
            .run { context ->
                assertThat(context).hasFailed()
                assertThat(context.startupFailure)
                    .hasMessageContaining(
                        "replus.storage.object-storage.public-base-url is required when object storage is enabled in prod",
                    )
            }
    }

    @Test
    fun `prod profile fails fast when object storage public base url is not configured`() {
        contextRunner
            .withPropertyValues(
                *validProdProperties(without = setOf("replus.storage.object-storage.public-base-url")),
            )
            .run { context ->
                assertThat(context).hasFailed()
                assertThat(context.startupFailure)
                    .hasMessageContaining(
                        "replus.storage.object-storage.public-base-url is required when object storage is enabled in prod",
                    )
            }
    }

    @Test
    fun `prod profile fails fast when object storage public base url points to localhost`() {
        contextRunner
            .withPropertyValues(
                *validProdProperties(
                    "replus.storage.object-storage.public-base-url=http://localhost:8080/mock-playback",
                ),
            )
            .run { context ->
                assertThat(context).hasFailed()
                assertThat(context.startupFailure)
                    .hasMessageContaining(
                        "Prod profile must not use localhost as replus.storage.object-storage.public-base-url",
                    )
            }
    }

    @Test
    fun `prod profile fails fast when object storage public base url is not https`() {
        contextRunner
            .withPropertyValues(
                *validProdProperties(
                    "replus.storage.object-storage.public-base-url=http://cdn.example.test/videos/",
                ),
            )
            .run { context ->
                assertThat(context).hasFailed()
                assertThat(context.startupFailure)
                    .hasMessageContaining(
                        "Prod profile requires an HTTPS replus.storage.object-storage.public-base-url",
                    )
            }
    }

    private fun validProdProperties(
        vararg overrides: String,
        without: Set<String> = emptySet(),
    ): Array<String> {
        val overriddenNames = overrides.mapTo(mutableSetOf()) { it.propertyName() }
        return (
            listOf(
                "spring.profiles.active=prod",
                "spring.datasource.url=jdbc:postgresql://db.example.test:5432/replus",
                "replus.web-base-url=https://app.example.test",
                "replus.web.cors.allowed-origins=https://app.example.test",
                "replus.storage.mode=object-storage",
                "replus.storage.object-storage.public-base-url=https://cdn.example.test/videos/",
            ).filterNot { it.propertyName() in overriddenNames || it.propertyName() in without } +
                overrides
            ).toTypedArray()
    }

    private fun String.propertyName(): String =
        substringBefore("=")
}
