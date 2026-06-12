package com.replus.api.common.operations

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.file.Path
import kotlin.io.path.readText

class BuildMetadataContractTest {
    private val buildGradle = Path.of("build.gradle.kts").readText()

    @Test
    fun `git metadata generator reads Render default git environment variables`() {
        assertThat(buildGradle)
            .contains("RENDER_GIT_COMMIT")
            .contains("RENDER_GIT_BRANCH")
    }

    @Test
    fun `docker build arg defaults do not mask Render git environment variables`() {
        assertThat(buildGradle).contains("takeUnless { it == \"unknown\" }")
        assertThat(Path.of("Dockerfile").readText())
            .contains("ARG REPLUS_BUILD_GIT_COMMIT=")
            .contains("ARG REPLUS_BUILD_GIT_BRANCH=")
            .doesNotContain("ARG REPLUS_BUILD_GIT_COMMIT=unknown")
            .doesNotContain("ARG REPLUS_BUILD_GIT_BRANCH=unknown")
    }
}
