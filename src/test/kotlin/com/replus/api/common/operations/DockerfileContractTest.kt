package com.replus.api.common.operations

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.file.Path
import kotlin.io.path.readText

class DockerfileContractTest {
    private val dockerfile = Path.of("Dockerfile").readText()

    @Test
    fun `docker build stage accepts git metadata build arguments`() {
        assertThat(dockerfile)
            .contains("ARG REPLUS_BUILD_GIT_COMMIT=unknown")
            .contains("ARG REPLUS_BUILD_GIT_BRANCH=unknown")
            .contains("ARG REPLUS_BUILD_GIT_COMMIT_TIME=")
            .contains("ENV REPLUS_BUILD_GIT_COMMIT=")
            .contains("ENV REPLUS_BUILD_GIT_BRANCH=")
            .contains("ENV REPLUS_BUILD_GIT_COMMIT_TIME=")
    }
}
