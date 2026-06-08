package com.replus.api.auth.interfaces.rest

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class GuestSessionApiTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    fun `게스트 세션을 만들면 bearer token으로 현재 사용자를 조회할 수 있다`() {
        val session = mockMvc.perform(
            post("/api/auth/guest")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"displayName":"  새 손님  "}"""),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.tokenType").value("Bearer"))
            .andExpect(jsonPath("$.user.displayName").value("새 손님"))
            .andExpect(jsonPath("$.user.isGuest").value(true))
            .andReturn()

        assertThat(session.response.getHeader(HttpHeaders.CACHE_CONTROL)).isEqualTo("no-store")

        val sessionBody = objectMapper.readTree(session.response.contentAsString)
        val accessToken = sessionBody["accessToken"].asString()
        val userId = sessionBody["user"]["id"].asString()

        assertThat(accessToken).startsWith("guest_")

        mockMvc.perform(
            get("/api/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.user.id").value(userId))
            .andExpect(jsonPath("$.user.displayName").value("새 손님"))
            .andExpect(jsonPath("$.user.isGuest").value(true))
            .andExpect(jsonPath("$.rooms").isArray)
            .andExpect(jsonPath("$.rooms").isEmpty)
    }

    @Test
    fun `displayName 없이 게스트 세션을 만들면 기본 이름을 사용한다`() {
        mockMvc.perform(
            post("/api/auth/guest"),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.user.displayName").value("Guest"))
            .andExpect(jsonPath("$.user.isGuest").value(true))
    }

    @Test
    fun `빈 displayName은 거절된다`() {
        mockMvc.perform(
            post("/api/auth/guest")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"displayName":""}"""),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
            .andExpect(jsonPath("$.fieldErrors[0].field").value("displayName"))
    }
}
