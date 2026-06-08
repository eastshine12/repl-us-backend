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
class InviteLinkRotationPolicyApiTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    fun `방장이 초대 링크를 재발급하면 이전 코드는 만료되고 새 코드만 참여에 사용할 수 있다`() {
        val roomId = createRoom()
        val previousCode = createInviteLink(roomId, rotate = false)

        val rotatedCode = createInviteLink(roomId, rotate = true)
        assertThat(rotatedCode).isNotEqualTo(previousCode)

        val previousGuestToken = createGuestToken("이전 링크 손님")
        mockMvc.perform(
            post("/api/invite-links/$previousCode/join")
                .header("Authorization", "Bearer $previousGuestToken"),
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.code").value("INVITE_LINK_EXPIRED"))

        val newGuestToken = createGuestToken("새 링크 손님")
        mockMvc.perform(
            post("/api/invite-links/$rotatedCode/join")
                .header("Authorization", "Bearer $newGuestToken"),
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
                .content("""{"name":"재발급 초대방"}"""),
        )
            .andExpect(status().isCreated)
            .andReturn()

        return objectMapper.readTree(result.response.contentAsString)["id"].asString()
    }

    private fun createInviteLink(roomId: String, rotate: Boolean): String {
        val result = mockMvc.perform(
            post("/api/rooms/$roomId/invite-links")
                .header("Authorization", "Bearer dev-token-mina")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"expiresInHours":24,"rotate":$rotate}"""),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.uses").value(0))
            .andReturn()

        return objectMapper.readTree(result.response.contentAsString)["code"].asString()
    }

    private fun createGuestToken(displayName: String): String {
        val result = mockMvc.perform(
            post("/api/auth/guest")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"displayName":"$displayName"}"""),
        )
            .andExpect(status().isCreated)
            .andReturn()

        return objectMapper.readTree(result.response.contentAsString)["accessToken"].asString()
    }
}
