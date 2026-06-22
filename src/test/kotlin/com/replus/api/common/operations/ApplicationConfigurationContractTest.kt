package com.replus.api.common.operations

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.file.Path
import kotlin.io.path.readText

class ApplicationConfigurationContractTest {
    private val applicationYaml = Path.of("src/main/resources/application.yml").readText()
    private val productionYaml = Path.of("src/main/resources/application-prod.yml").readText()

    @Test
    fun `application configs use the documented social login client id environment variables`() {
        val configs = listOf(applicationYaml, productionYaml)

        assertThat(configs).allSatisfy { config ->
            assertThat(config)
                .contains("REPLUS_AUTH_SOCIAL_GOOGLE_CLIENT_IDS")
                .contains("REPLUS_AUTH_SOCIAL_APPLE_CLIENT_IDS")
                .doesNotContain("REPLUS_AUTH_GOOGLE_CLIENT_IDS")
                .doesNotContain("REPLUS_AUTH_APPLE_CLIENT_IDS")
        }
    }
}
