package com.replus.api.room.interfaces.rest

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

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RoomLeaveApiTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun `일반 멤버는 스스로 방을 나갈 수 있고 내 방 목록에서 방이 사라진다`() {
        mockMvc.perform(
            delete("/api/rooms/$ROOM_ID/members/me")
                .header("Authorization", "Bearer dev-token-joon"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.memberId").value(JOON_MEMBER_ID))
            .andExpect(jsonPath("$.status").value("REMOVED"))
            .andExpect(jsonPath("$.removedAt").exists())

        mockMvc.perform(
            get("/api/me")
                .header("Authorization", "Bearer dev-token-joon"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.rooms").isEmpty)

        mockMvc.perform(
            get("/api/rooms/$ROOM_ID")
                .header("Authorization", "Bearer dev-token-joon"),
        )
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.code").value("ROOM_MEMBER_REQUIRED"))
    }

    @Test
    fun `방장은 자기 자신을 방에서 내보낼 수 없다`() {
        mockMvc.perform(
            delete("/api/rooms/$ROOM_ID/members/me")
                .header("Authorization", "Bearer dev-token-mina"),
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.code").value("CANNOT_REMOVE_OWNER"))
    }

    private companion object {
        const val ROOM_ID = "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa"
        const val JOON_MEMBER_ID = "bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbb2"
    }
}
