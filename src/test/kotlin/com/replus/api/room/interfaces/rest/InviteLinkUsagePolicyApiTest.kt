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
class InviteLinkUsagePolicyApiTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    fun `일회용 초대 링크는 첫 신규 참여 후 다시 사용할 수 없다`() {
        val roomId = createRoom()
        val inviteCode = createInviteLink(roomId, maxUses = 1)

        val firstGuestToken = createGuestToken("첫 손님")
        mockMvc.perform(
            post("/api/invite-links/$inviteCode/join")
                .header("Authorization", "Bearer $firstGuestToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(roomId))
            .andExpect(jsonPath("$.memberCount").value(2))
            .andExpect(jsonPath("$.currentUserRole").value("MEMBER"))

        val secondGuestToken = createGuestToken("두 번째 손님")
        mockMvc.perform(
            post("/api/invite-links/$inviteCode/join")
                .header("Authorization", "Bearer $secondGuestToken"),
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.code").value("INVITE_LINK_USAGE_LIMIT_REACHED"))
    }

    private fun createRoom(): String {
        val result = mockMvc.perform(
            post("/api/rooms")
                .header("Authorization", "Bearer dev-token-mina")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"일회용 초대방"}"""),
        )
            .andExpect(status().isCreated)
            .andReturn()

        return objectMapper.readTree(result.response.contentAsString)["id"].asString()
    }

    private fun createInviteLink(roomId: String, maxUses: Int): String {
        val result = mockMvc.perform(
            post("/api/rooms/$roomId/invite-links")
                .header("Authorization", "Bearer dev-token-mina")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"expiresInHours":24,"maxUses":$maxUses}"""),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.maxUses").value(maxUses))
            .andExpect(jsonPath("$.uses").value(0))
            .andReturn()

        val invite = objectMapper.readTree(result.response.contentAsString)
        val code = invite["code"].asString()
        assertThat(code).doesNotContain(roomId.take(8))
        return code
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
