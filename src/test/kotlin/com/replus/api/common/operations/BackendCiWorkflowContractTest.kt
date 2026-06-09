package com.replus.api.common.operations

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean
import org.springframework.core.io.FileSystemResource
import java.nio.file.Path
import java.util.Properties

class BackendCiWorkflowContractTest {
    private val workflow = loadBackendCiWorkflow()

    @Test
    fun `docker build job smoke tests the built image`() {
        val smokeStepIndex = stepIndexFor("Smoke Docker image")
        val smokeStepRun = workflow["jobs.docker-build.steps[$smokeStepIndex].run"].toString()

        assertThat(smokeStepRun).contains("docker run")
        assertThat(smokeStepRun).contains("SPRING_PROFILES_ACTIVE=local")
        assertThat(smokeStepRun).contains("scripts/smoke-api.sh")
    }

    @Test
    fun `docker build job still builds image before smoke`() {
        val buildStepIndex = stepIndexFor("Build Docker image")
        val smokeStepIndex = stepIndexFor("Smoke Docker image")

        assertThat(buildStepIndex).isLessThan(smokeStepIndex)
        assertThat(workflow["jobs.docker-build.steps[$buildStepIndex].run"].toString())
            .contains("docker build")
    }

    private fun stepIndexFor(name: String): Int {
        for (index in 0..20) {
            if (workflow["jobs.docker-build.steps[$index].name"] == name) {
                return index
            }
        }
        error("backend-ci.yml docker-build job is missing step: $name")
    }

    private fun loadBackendCiWorkflow(): Properties =
        YamlPropertiesFactoryBean()
            .apply {
                setResources(FileSystemResource(Path.of(".github/workflows/backend-ci.yml")))
            }
            .getObject()
            ?: error("backend-ci.yml could not be loaded")
}
