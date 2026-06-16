package com.replus.api.common.operations

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest(
    properties = [
        "management.endpoint.health.show-details=always",
        "replus.auth.social.google.client-ids=google-web-client-id,google-ios-client-id",
        "replus.auth.social.apple.client-ids= ",
    ],
)
@AutoConfigureMockMvc
class OperationalReadinessApiTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun `readiness health endpoint is exposed`() {
        mockMvc.perform(get("/actuator/health/readiness"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("UP"))
    }

    @Test
    fun `readiness health endpoint includes database and storage dependencies`() {
        mockMvc.perform(get("/actuator/health/readiness"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.components.readinessState.status").value("UP"))
            .andExpect(jsonPath("$.components.db.status").value("UP"))
            .andExpect(jsonPath("$.components.storage.status").value("UP"))
    }

    @Test
    fun `health endpoint includes storage readiness details without secrets`() {
        mockMvc.perform(get("/actuator/health"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.components.storage.status").value("UP"))
            .andExpect(jsonPath("$.components.storage.details.mode").value("LOCAL"))
            .andExpect(jsonPath("$.components.storage.details.uploadBaseUrlConfigured").value(true))
            .andExpect(jsonPath("$.components.storage.details.playbackBaseUrlConfigured").value(true))
    }

    @Test
    fun `info endpoint exposes public application metadata`() {
        mockMvc.perform(get("/actuator/info"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.app.name").value("repl.us backend"))
            .andExpect(jsonPath("$.app.description").value("Private daily 3-second video room API"))
            .andExpect(jsonPath("$.app.version").value("0.1.0-SNAPSHOT"))
            .andExpect(jsonPath("$.build.name").value("repl-us-backend"))
            .andExpect(jsonPath("$.build.version").value("0.1.0-SNAPSHOT"))
            .andExpect(jsonPath("$.git.branch").exists())
            .andExpect(jsonPath("$.git.commit.id").exists())
    }

    @Test
    fun `info endpoint exposes social login configuration state without client ids`() {
        val result = mockMvc.perform(get("/actuator/info"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.auth.social.providers.google.clientIdsConfigured").value(true))
            .andExpect(jsonPath("$.auth.social.providers.apple.clientIdsConfigured").value(false))
            .andReturn()

        assertThat(result.response.contentAsString)
            .doesNotContain("google-web-client-id")
            .doesNotContain("google-ios-client-id")
    }
}
