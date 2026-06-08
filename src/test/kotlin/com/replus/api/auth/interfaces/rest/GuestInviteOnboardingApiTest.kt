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
class GuestInviteOnboardingApiTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    fun `게스트는 초대 코드로 방에 참여하고 내 방 목록에서 방을 볼 수 있다`() {
        val session = mockMvc.perform(
            post("/api/auth/guest")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"displayName":"초대 손님"}"""),
        )
            .andExpect(status().isCreated)
            .andReturn()

        val accessToken = objectMapper
            .readTree(session.response.contentAsString)["accessToken"]
            .asString()

        val joined = mockMvc.perform(
            post("/api/invite-links/R3S9KQ/join")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(ROOM_ID))
            .andExpect(jsonPath("$.memberCount").value(4))
            .andExpect(jsonPath("$.maxMembers").value(6))
            .andExpect(jsonPath("$.currentUserRole").value("MEMBER"))
            .andExpect(jsonPath("$.members[3].user.displayName").value("초대 손님"))
            .andReturn()

        assertThat(joined.response.getHeader(HttpHeaders.LOCATION)).isEqualTo("/api/rooms/$ROOM_ID")

        mockMvc.perform(
            get("/api/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.rooms[0].id").value(ROOM_ID))
            .andExpect(jsonPath("$.rooms[0].name").value("책상 위 소동방"))
            .andExpect(jsonPath("$.rooms[0].memberCount").value(4))
            .andExpect(jsonPath("$.rooms[0].currentUserRole").value("MEMBER"))
    }

    private companion object {
        const val ROOM_ID = "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa"
    }
}
