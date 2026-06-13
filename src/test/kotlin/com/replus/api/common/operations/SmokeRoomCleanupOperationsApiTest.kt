package com.replus.api.common.operations

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.assertj.core.api.Assertions.assertThat
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper
import java.util.UUID

@SpringBootTest(
    properties = [
        "replus.operations.smoke-cleanup.enabled=true",
        "replus.operations.smoke-cleanup.token=cleanup-secret",
    ],
)
@AutoConfigureMockMvc
@Transactional
class SmokeRoomCleanupOperationsApiTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Test
    fun `operations token can delete smoke room data`() {
        val ownerToken = createGuestToken("Smoke Owner")
        val roomId = createRoom(ownerToken, "Smoke Room Cleanup")
        createInviteLink(ownerToken, roomId)
        createTodayMission(ownerToken, roomId)

        mockMvc.perform(
            delete("/internal/operations/smoke-rooms/$roomId")
                .header("X-Replus-Operations-Token", "cleanup-secret"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.roomId").value(roomId))
            .andExpect(jsonPath("$.deleted").value(true))

        assertTableCount("rooms", "id", roomId).isEqualTo(0)
        assertTableCount("room_members", "room_id", roomId).isEqualTo(0)
        assertTableCount("invite_links", "room_id", roomId).isEqualTo(0)
        assertTableCount("missions", "room_id", roomId).isEqualTo(0)
    }

    @Test
    fun `cleanup rejects non smoke room names`() {
        val ownerToken = createGuestToken("Real Owner")
        val roomId = createRoom(ownerToken, "Real Room")

        mockMvc.perform(
            delete("/internal/operations/smoke-rooms/$roomId")
                .header("X-Replus-Operations-Token", "cleanup-secret"),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))

        mockMvc.perform(
            get("/api/rooms/$roomId")
                .header("Authorization", "Bearer $ownerToken"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(roomId))
    }

    @Test
    fun `cleanup requires operations token`() {
        val ownerToken = createGuestToken("Smoke Owner")
        val roomId = createRoom(ownerToken, "Smoke Room Token")

        mockMvc.perform(
            delete("/internal/operations/smoke-rooms/$roomId")
                .header("X-Replus-Operations-Token", "wrong-token"),
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"))
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

    private fun createRoom(accessToken: String, name: String): String {
        val result = mockMvc.perform(
            post("/api/rooms")
                .header("Authorization", "Bearer $accessToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"$name"}"""),
        )
            .andExpect(status().isCreated)
            .andReturn()

        return objectMapper.readTree(result.response.contentAsString)["id"].asString()
    }

    private fun createInviteLink(accessToken: String, roomId: String) {
        mockMvc.perform(
            post("/api/rooms/$roomId/invite-links")
                .header("Authorization", "Bearer $accessToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"expiresInHours":1,"maxUses":1,"rotate":true}"""),
        )
            .andExpect(status().isCreated)
    }

    private fun createTodayMission(accessToken: String, roomId: String) {
        mockMvc.perform(
            get("/api/rooms/$roomId/today")
                .header("Authorization", "Bearer $accessToken"),
        )
            .andExpect(status().isOk)
    }

    private fun assertTableCount(tableName: String, columnName: String, roomId: String) =
        assertThat(
            jdbcTemplate.queryForObject(
                "select count(*) from $tableName where $columnName = ?",
                Long::class.java,
                UUID.fromString(roomId),
            ),
        )
}
