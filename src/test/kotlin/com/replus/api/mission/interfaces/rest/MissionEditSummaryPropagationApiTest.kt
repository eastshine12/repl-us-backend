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
class MissionEditSummaryPropagationApiTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    fun `방장이 오늘 질문을 수정하면 today와 방 목록과 방 상세 요약에 반영된다`() {
        val today = getToday()
        val missionId = today["mission"]["id"].asString()

        mockMvc.perform(
            patch("/api/rooms/$ROOM_ID/missions/$missionId")
                .header("Authorization", "Bearer dev-token-mina")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"prompt":"오늘 제일 웃긴 물건은?","category":"MOOD"}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(missionId))
            .andExpect(jsonPath("$.prompt").value("오늘 제일 웃긴 물건은?"))
            .andExpect(jsonPath("$.category").value("MOOD"))
            .andExpect(jsonPath("$.editCount").value(1))

        mockMvc.perform(
            get("/api/rooms/$ROOM_ID/today")
                .header("Authorization", "Bearer dev-token-mina"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.mission.id").value(missionId))
            .andExpect(jsonPath("$.mission.prompt").value("오늘 제일 웃긴 물건은?"))
            .andExpect(jsonPath("$.mission.category").value("MOOD"))

        mockMvc.perform(
            get("/api/me")
                .header("Authorization", "Bearer dev-token-mina"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.rooms[0].today.missionId").value(missionId))
            .andExpect(jsonPath("$.rooms[0].today.prompt").value("오늘 제일 웃긴 물건은?"))
            .andExpect(jsonPath("$.rooms[0].today.category").value("MOOD"))

        mockMvc.perform(
            get("/api/rooms/$ROOM_ID")
                .header("Authorization", "Bearer dev-token-mina"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.today.missionId").value(missionId))
            .andExpect(jsonPath("$.today.prompt").value("오늘 제일 웃긴 물건은?"))
            .andExpect(jsonPath("$.today.category").value("MOOD"))
    }

    private fun getToday(): JsonNode =
        objectMapper.readTree(
            mockMvc.perform(
                get("/api/rooms/$ROOM_ID/today")
                    .header("Authorization", "Bearer dev-token-mina"),
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
