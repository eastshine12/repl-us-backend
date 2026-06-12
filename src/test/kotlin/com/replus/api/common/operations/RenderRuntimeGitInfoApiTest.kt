package com.replus.api.common.operations

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
        "RENDER_GIT_BRANCH=main",
        "RENDER_GIT_COMMIT=4ed51b535228b293f878826da2173c39014751fd",
    ],
)
@AutoConfigureMockMvc
class RenderRuntimeGitInfoApiTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun `info endpoint prefers Render runtime git metadata`() {
        mockMvc.perform(get("/actuator/info"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.git.branch").value("main"))
            .andExpect(jsonPath("$.git.commit.id").value("4ed51b535228b293f878826da2173c39014751fd"))
    }
}
