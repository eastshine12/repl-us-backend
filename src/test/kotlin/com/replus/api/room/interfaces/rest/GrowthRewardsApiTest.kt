package com.replus.api.room.interfaces.rest

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
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
class GrowthRewardsApiTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    fun `성장 보상은 방의 active 리플 수로 진행도를 요약한다`() {
        mockMvc.perform(
            get("/api/rooms/$ROOM_ID/growth-rewards")
                .header("Authorization", "Bearer dev-token-mina"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.roomId").value(ROOM_ID))
            .andExpect(jsonPath("$.rewards[0].type").value("ROOM_NAMEPLATE"))
            .andExpect(jsonPath("$.rewards[0].category").value("OBSERVATION"))
            .andExpect(jsonPath("$.rewards[0].status").value("LOCKED"))
            .andExpect(jsonPath("$.rewards[0].progress").value(0))
            .andExpect(jsonPath("$.rewards[0].threshold").value(1))

        val today = getToday()
        val missionId = today["mission"]["id"].asString()
        val objectKey = createUploadUrl(missionId)["objectKey"].asString()
        submitResponse(missionId, objectKey)

        mockMvc.perform(
            get("/api/rooms/$ROOM_ID/growth-rewards")
                .header("Authorization", "Bearer dev-token-mina"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.rewards[0].status").value("UNLOCKED"))
            .andExpect(jsonPath("$.rewards[0].progress").value(1))
            .andExpect(jsonPath("$.rewards[1].status").value("LOCKED"))
            .andExpect(jsonPath("$.rewards[1].progress").value(1))
    }

    @Test
    fun `active member가 아니면 성장 보상을 조회할 수 없다`() {
        val roomId = createRoomAsMina()

        mockMvc.perform(
            get("/api/rooms/$roomId/growth-rewards")
                .header("Authorization", "Bearer dev-token-joon"),
        )
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.code").value("ROOM_MEMBER_REQUIRED"))
    }

    private fun createRoomAsMina(): String =
        objectMapper
            .readTree(
                mockMvc.perform(
                    post("/api/rooms")
                        .header("Authorization", "Bearer dev-token-mina")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"name":"성장 테스트 방"}"""),
                )
                    .andExpect(status().isCreated)
                    .andReturn()
                    .response
                    .contentAsString,
            )["id"]
            .asString()

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
        ).andExpect(status().isCreated)
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
