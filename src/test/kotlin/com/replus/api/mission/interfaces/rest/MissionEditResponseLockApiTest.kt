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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
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
class MissionEditResponseLockApiTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    fun `첫 active 리플 이후 오늘 미션은 수정 불가 상태가 되고 수정 요청도 거절된다`() {
        val today = getToday()
        val missionId = today["mission"]["id"].asString()
        val objectKey = createUploadUrl(missionId)["objectKey"].asString()

        mockMvc.perform(
            get("/api/rooms/$ROOM_ID/today")
                .header("Authorization", "Bearer dev-token-mina"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.mission.id").value(missionId))
            .andExpect(jsonPath("$.mission.canEdit").value(true))

        submitResponse(missionId, objectKey)

        mockMvc.perform(
            get("/api/rooms/$ROOM_ID/today")
                .header("Authorization", "Bearer dev-token-mina"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.mission.id").value(missionId))
            .andExpect(jsonPath("$.mission.canEdit").value(false))
            .andExpect(jsonPath("$.participation.submittedCount").value(1))

        mockMvc.perform(
            patch("/api/rooms/$ROOM_ID/missions/$missionId")
                .header("Authorization", "Bearer dev-token-mina")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"prompt":"이미 늦은 질문 수정","category":"MOOD"}"""),
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.code").value("MISSION_ALREADY_HAS_RESPONSE"))
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

    private fun createUploadUrl(missionId: String): JsonNode =
        objectMapper.readTree(
            mockMvc.perform(
                post("/api/rooms/$ROOM_ID/missions/$missionId/responses/upload-url")
                    .header("Authorization", "Bearer dev-token-mina")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                              "contentType": "video/webm",
                              "fileSizeBytes": 842120,
                              "durationSeconds": 3,
                              "hasAudio": true,
                              "width": 720,
                              "height": 1280
                            }
                        """.trimIndent(),
                    ),
            )
                .andExpect(status().isCreated)
                .andReturn()
                .response
                .contentAsString,
        )

    private fun submitResponse(missionId: String, objectKey: String) {
        mockMvc.perform(
            post("/api/rooms/$ROOM_ID/missions/$missionId/responses")
                .header("Authorization", "Bearer dev-token-mina")
                .contentType(MediaType.APPLICATION_JSON)
                .content(missionResponseBody(objectKey)),
        )
            .andExpect(status().isCreated)
    }

    private fun missionResponseBody(objectKey: String): String =
        """
            {
              "objectKey": "$objectKey",
              "contentType": "video/webm",
              "fileSizeBytes": 842120,
              "durationSeconds": 3,
              "hasAudio": true,
              "width": 720,
              "height": 1280,
              "clientCapturedAt": "2026-05-24T09:15:00Z"
            }
        """.trimIndent()

    private companion object {
        const val ROOM_ID = "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa"
    }
}
