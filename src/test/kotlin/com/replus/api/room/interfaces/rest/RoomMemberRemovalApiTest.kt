package com.replus.api.room.interfaces.rest

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RoomMemberRemovalApiTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    fun `방장은 멤버를 내보낼 수 있고 내보낸 멤버는 방 API에 접근할 수 없다`() {
        mockMvc.perform(
            delete("/api/rooms/$ROOM_ID/members/$JOON_MEMBER_ID")
                .header("Authorization", "Bearer dev-token-mina"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.memberId").value(JOON_MEMBER_ID))
            .andExpect(jsonPath("$.status").value("REMOVED"))
            .andExpect(jsonPath("$.removedAt").exists())

        val roomDetail = mockMvc.perform(
            get("/api/rooms/$ROOM_ID")
                .header("Authorization", "Bearer dev-token-mina"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.memberCount").value(2))
            .andReturn()

        val memberNodes = objectMapper.readTree(roomDetail.response.contentAsString)["members"]
        val members = (0 until memberNodes.size())
            .map { memberNodes[it]["id"].asString() }
        assertThat(members).doesNotContain(JOON_MEMBER_ID)

        mockMvc.perform(
            get("/api/rooms/$ROOM_ID")
                .header("Authorization", "Bearer dev-token-joon"),
        )
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.code").value("ROOM_MEMBER_REQUIRED"))
    }

    @Test
    fun `일반 멤버는 다른 멤버를 내보낼 수 없다`() {
        mockMvc.perform(
            delete("/api/rooms/$ROOM_ID/members/$ARA_MEMBER_ID")
                .header("Authorization", "Bearer dev-token-joon"),
        )
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.code").value("ROOM_OWNER_REQUIRED"))
    }

    @Test
    fun `방장은 자기 자신을 내보낼 수 없다`() {
        mockMvc.perform(
            delete("/api/rooms/$ROOM_ID/members/$MINA_MEMBER_ID")
                .header("Authorization", "Bearer dev-token-mina"),
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.code").value("CANNOT_REMOVE_OWNER"))
    }

    private companion object {
        const val ROOM_ID = "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa"
        const val MINA_MEMBER_ID = "bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbb1"
        const val JOON_MEMBER_ID = "bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbb2"
        const val ARA_MEMBER_ID = "bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbb3"
    }
}
