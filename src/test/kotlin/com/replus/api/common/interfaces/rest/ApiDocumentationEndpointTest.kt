package com.replus.api.common.interfaces.rest

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
class ApiDocumentationEndpointTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun `openapi yaml is publicly exposed`() {
        val result = mockMvc.perform(get("/api-docs/openapi.yaml"))
            .andExpect(status().isOk)
            .andExpect(content().contentTypeCompatibleWith(MediaType.parseMediaType("application/yaml")))
            .andReturn()

        assertThat(result.response.contentAsString)
            .startsWith("openapi: 3.0.3")
            .contains("title: Three Second Room MVP API")
            .contains("/api/auth/social:")
            .contains("/api/rooms/{roomId}/today:")
    }
}
