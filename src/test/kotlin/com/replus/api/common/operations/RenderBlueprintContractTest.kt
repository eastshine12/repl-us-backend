package com.replus.api.common.operations

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean
import org.springframework.core.io.FileSystemResource
import java.nio.file.Path
import java.util.Properties

class RenderBlueprintContractTest {
    private val properties = loadRenderBlueprint()

    @Test
    fun `blueprint provisions a private postgres database for the api`() {
        assertThat(properties["databases[0].name"]).isEqualTo("repl-us-postgres")
        assertThat(properties["databases[0].databaseName"]).isEqualTo("replus")
        assertThat(properties["databases[0].user"]).isEqualTo("replus")
        assertThat(properties["databases[0].plan"]).isEqualTo("free")
        assertThat(properties["databases[0].ipAllowList"]).isEqualTo("")
    }

    @Test
    fun `api service receives render postgres connection string through database url`() {
        val databaseUrlIndex = envVarIndexFor("DATABASE_URL")

        assertThat(properties["services[0].envVars[$databaseUrlIndex].fromDatabase.name"])
            .isEqualTo("repl-us-postgres")
        assertThat(properties["services[0].envVars[$databaseUrlIndex].fromDatabase.property"])
            .isEqualTo("connectionString")
    }

    @Test
    fun `api service keeps production health and profile settings`() {
        assertThat(properties["services[0].type"]).isEqualTo("web")
        assertThat(properties["services[0].runtime"]).isEqualTo("docker")
        assertThat(properties["services[0].healthCheckPath"]).isEqualTo("/actuator/health/readiness")

        val prodProfileIndex = envVarIndexFor("SPRING_PROFILES_ACTIVE")
        assertThat(properties["services[0].envVars[$prodProfileIndex].value"]).isEqualTo("prod")
    }

    private fun envVarIndexFor(key: String): Int {
        for (index in 0..50) {
            if (properties["services[0].envVars[$index].key"] == key) {
                return index
            }
        }
        error("render.yaml is missing env var $key")
    }

    private fun loadRenderBlueprint(): Properties =
        YamlPropertiesFactoryBean()
            .apply {
                setResources(FileSystemResource(Path.of("render.yaml")))
            }
            .getObject()
            ?: error("render.yaml could not be loaded")
}
