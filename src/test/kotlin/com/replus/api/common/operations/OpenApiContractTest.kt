package com.replus.api.common.operations

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.file.Path
import kotlin.io.path.readText

class OpenApiContractTest {
    private val openApi = Path.of("docs/api/openapi.yaml").readText()

    @Test
    fun `openapi contract documents deployed openapi endpoint`() {
        assertThat(openApi)
            .contains("  /api-docs/openapi.yaml:")
            .contains("operationId: getOpenApiYaml")
            .contains("application/yaml")
    }
}
