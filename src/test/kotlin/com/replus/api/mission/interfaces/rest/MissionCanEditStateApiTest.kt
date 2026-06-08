package com.replus.api.mission.interfaces.rest

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper

@SpringBootTest
@AutoConfigureMockMvc
@Sql(
    statements = [
        "delete from response_comments",
        "delete from response_reactions",
        "delete from mission_release_states",
        "delete from mission_responses",
        "delete from video_assets",
        "delete from missions",
    ],
)
class MissionCanEditStateApiTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    fun `오늘 미션은 방장만 첫 리플 전 한 번 수정 가능 상태로 보인다`() {
        val ownerToday = getToday(token = "dev-token-mina")
        val missionId = ownerToday["mission"]["id"].asString()

        mockMvc.perform(
            get("/api/rooms/$ROOM_ID/today")
                .header("Authorization", "Bearer dev-token-mina"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.mission.id").value(missionId))
            .andExpect(jsonPath("$.mission.canEdit").value(true))

        mockMvc.perform(
            get("/api/rooms/$ROOM_ID/today")
                .header("Authorization", "Bearer dev-token-joon"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.mission.id").value(missionId))
            .andExpect(jsonPath("$.mission.canEdit").value(false))

        mockMvc.perform(
            patch("/api/rooms/$ROOM_ID/missions/$missionId")
                .header("Authorization", "Bearer dev-token-mina")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"prompt":"오늘 가장 의외였던 장면은?","category":"OBSERVATION"}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(missionId))
            .andExpect(jsonPath("$.canEdit").value(false))
            .andExpect(jsonPath("$.editCount").value(1))

        mockMvc.perform(
            get("/api/rooms/$ROOM_ID/today")
                .header("Authorization", "Bearer dev-token-mina"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.mission.id").value(missionId))
            .andExpect(jsonPath("$.mission.canEdit").value(false))
    }

    private fun getToday(token: String): JsonNode =
        objectMapper.readTree(
            mockMvc.perform(
                get("/api/rooms/$ROOM_ID/today")
                    .header("Authorization", "Bearer $token"),
            )
                .andExpect(status().isOk)
                .andReturn()
                .response
                .contentAsString,
        )

    private companion object {
        const val ROOM_ID = "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa"
    }
}
