package com.replus.api.room.interfaces.rest

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class InviteCodeNormalizationApiTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    fun `소문자로 입력한 초대 코드도 같은 방 참여로 처리한다`() {
        val roomId = createRoom()
        val inviteCode = createInviteLink(roomId)
        assertThat(inviteCode).isUpperCase()

        val guestToken = createGuestToken()

        mockMvc.perform(
            post("/api/invite-links/${inviteCode.lowercase()}/join")
                .header("Authorization", "Bearer $guestToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(roomId))
            .andExpect(jsonPath("$.currentUserRole").value("MEMBER"))
            .andExpect(jsonPath("$.memberCount").value(2))
    }

    private fun createRoom(): String {
        val result = mockMvc.perform(
            post("/api/rooms")
                .header("Authorization", "Bearer dev-token-mina")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"코드 정규화방"}"""),
        )
            .andExpect(status().isCreated)
            .andReturn()

        return objectMapper.readTree(result.response.contentAsString)["id"].asString()
    }

    private fun createInviteLink(roomId: String): String {
        val result = mockMvc.perform(
            post("/api/rooms/$roomId/invite-links")
                .header("Authorization", "Bearer dev-token-mina")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"expiresInHours":24}"""),
        )
            .andExpect(status().isCreated)
            .andReturn()

        return objectMapper.readTree(result.response.contentAsString)["code"].asString()
    }

    private fun createGuestToken(): String {
        val result = mockMvc.perform(
            post("/api/auth/guest")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"displayName":"코드 손님"}"""),
        )
            .andExpect(status().isCreated)
            .andReturn()

        return objectMapper.readTree(result.response.contentAsString)["accessToken"].asString()
    }
}
