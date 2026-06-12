package com.replus.api.common.operations

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.file.Path
import kotlin.io.path.readText

class DockerfileContractTest {
    private val dockerfile = Path.of("Dockerfile").readText()
    private val dockerignore = Path.of(".dockerignore").readText()

    @Test
    fun `docker build stage accepts git metadata build arguments`() {
        assertThat(dockerfile)
            .contains("ARG REPLUS_BUILD_GIT_COMMIT=")
            .contains("ARG REPLUS_BUILD_GIT_BRANCH=")
            .contains("ARG REPLUS_BUILD_GIT_COMMIT_TIME=")
            .contains("ENV REPLUS_BUILD_GIT_COMMIT=")
            .contains("ENV REPLUS_BUILD_GIT_BRANCH=")
            .contains("ENV REPLUS_BUILD_GIT_COMMIT_TIME=")
    }

    @Test
    fun `docker build does not require git directory in the build context`() {
        assertThat(dockerignore.lines()).contains(".git")
        assertThat(dockerfile).doesNotContain("COPY .git ./.git")
    }
}
