package com.replus.api.room.interfaces.rest

import com.replus.api.room.domain.model.InviteLink
import com.replus.api.room.domain.repository.InviteLinkRepository
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
import java.time.Instant
import java.util.UUID

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class InviteLinkExpirationPolicyApiTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var inviteLinkRepository: InviteLinkRepository

    @Test
    fun `만료된 초대 링크로 참여할 수 없다`() {
        inviteLinkRepository.save(
            InviteLink(
                code = EXPIRED_CODE,
                roomId = UUID.fromString(ROOM_ID),
                createdByMemberId = UUID.fromString(MINA_MEMBER_ID),
                expiresAt = Instant.parse("2020-01-01T00:00:00Z"),
                maxUses = null,
                uses = 0,
                createdAt = Instant.parse("2019-12-31T23:00:00Z"),
            ),
        )

        val guestToken = createGuestToken("만료 손님")

        mockMvc.perform(
            post("/api/invite-links/$EXPIRED_CODE/join")
                .header("Authorization", "Bearer $guestToken"),
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.code").value("INVITE_LINK_EXPIRED"))
            .andExpect(jsonPath("$.detail").value("The invite link has expired."))
    }

    @Test
    fun `만료된 기존 링크는 새 초대 링크 발급 때 재사용하지 않는다`() {
        val room = createRoom()
        val roomId = room["id"].asString()
        val ownerMemberId = room["ownerMemberId"].asString()
        inviteLinkRepository.save(
            InviteLink(
                code = EXPIRED_CODE,
                roomId = UUID.fromString(roomId),
                createdByMemberId = UUID.fromString(ownerMemberId),
                expiresAt = Instant.parse("2020-01-01T00:00:00Z"),
                maxUses = null,
                uses = 0,
                createdAt = Instant.parse("2019-12-31T23:00:00Z"),
            ),
        )

        val invite = mockMvc.perform(
            post("/api/rooms/$roomId/invite-links")
                .header("Authorization", "Bearer dev-token-mina")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"expiresInHours":24}"""),
        )
            .andExpect(status().isCreated)
            .andReturn()

        val inviteBody = objectMapper.readTree(invite.response.contentAsString)
        assertThat(inviteBody["code"].asString()).isNotEqualTo(EXPIRED_CODE)
        assertThat(inviteBody["uses"].asInt()).isEqualTo(0)
    }

    private fun createRoom() =
        mockMvc.perform(
            post("/api/rooms")
                .header("Authorization", "Bearer dev-token-mina")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"만료 초대방"}"""),
        )
            .andExpect(status().isCreated)
            .andReturn()
            .let { objectMapper.readTree(it.response.contentAsString) }

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

    private companion object {
        const val ROOM_ID = "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa"
        const val MINA_MEMBER_ID = "bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbb1"
        const val EXPIRED_CODE = "EXP2RD"
    }
}
