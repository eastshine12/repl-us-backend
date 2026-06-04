package com.replus.api.room.interfaces.rest

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
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
class RoomCreateApiTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    fun `방을 생성하면 요청자가 owner active member가 되고 내 방 목록에 포함된다`() {
        val created = mockMvc.perform(
            post("/api/rooms")
                .header("Authorization", "Bearer dev-token-mina")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"  새벽 컷 방  "}"""),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.name").value("새벽 컷 방"))
            .andExpect(jsonPath("$.memberCount").value(1))
            .andExpect(jsonPath("$.maxMembers").value(6))
            .andExpect(jsonPath("$.currentUserRole").value("OWNER"))
            .andExpect(jsonPath("$.members[0].role").value("OWNER"))
            .andExpect(jsonPath("$.members[0].status").value("ACTIVE"))
            .andExpect(jsonPath("$.members[0].user.displayName").value("민아"))
            .andReturn()

        val createdBody = objectMapper.readTree(created.response.contentAsString)
        val roomId = createdBody["id"].asString()
        val ownerMemberId = createdBody["ownerMemberId"].asString()

        assertThat(created.response.getHeader("Location")).isEqualTo("/api/rooms/$roomId")
        assertThat(createdBody["currentUserMemberId"].asString()).isEqualTo(ownerMemberId)
        assertThat(createdBody["members"][0]["id"].asString()).isEqualTo(ownerMemberId)

        mockMvc.perform(
            get("/api/me")
                .header("Authorization", "Bearer dev-token-mina"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.rooms[0].id").value(roomId))
            .andExpect(jsonPath("$.rooms[0].name").value("새벽 컷 방"))
            .andExpect(jsonPath("$.rooms[0].memberCount").value(1))
            .andExpect(jsonPath("$.rooms[0].currentUserRole").value("OWNER"))
    }

    @Test
    fun `빈 이름으로 방을 생성할 수 없다`() {
        mockMvc.perform(
            post("/api/rooms")
                .header("Authorization", "Bearer dev-token-mina")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"   "}"""),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
            .andExpect(jsonPath("$.fieldErrors[0].field").value("name"))
    }
}
