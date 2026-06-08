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
class RoomDetailTodaySummaryApiTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    fun `방 상세는 오늘 미션과 내 미제출 상태를 요약한다`() {
        val today = getToday()
        val mission = today["mission"]

        mockMvc.perform(
            get("/api/rooms/$ROOM_ID")
                .header("Authorization", "Bearer dev-token-mina"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(ROOM_ID))
            .andExpect(jsonPath("$.today.missionId").value(mission["id"].asString()))
            .andExpect(jsonPath("$.today.missionDate").value(mission["missionDate"].asString()))
            .andExpect(jsonPath("$.today.prompt").value(mission["prompt"].asString()))
            .andExpect(jsonPath("$.today.category").value(mission["category"].asString()))
            .andExpect(jsonPath("$.today.myResponseStatus").value("NOT_SUBMITTED"))
            .andExpect(jsonPath("$.today.myResponseId").doesNotExist())
    }

    @Test
    fun `방 상세는 오늘 리플 제출 후 내 제출 상태와 리플 id를 요약한다`() {
        val today = getToday()
        val missionId = today["mission"]["id"].asString()
        val objectKey = createUploadUrl(missionId)["objectKey"].asString()
        val responseId = submitResponse(missionId, objectKey)

        mockMvc.perform(
            get("/api/rooms/$ROOM_ID")
                .header("Authorization", "Bearer dev-token-mina"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.today.missionId").value(missionId))
            .andExpect(jsonPath("$.today.myResponseStatus").value("SUBMITTED"))
            .andExpect(jsonPath("$.today.myResponseId").value(responseId))
    }

    private fun getToday() =
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

    private fun createUploadUrl(missionId: String) =
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

    private fun submitResponse(missionId: String, objectKey: String): String =
        objectMapper
            .readTree(
                mockMvc.perform(
                    post("/api/rooms/$ROOM_ID/missions/$missionId/responses")
                        .header("Authorization", "Bearer dev-token-mina")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(missionResponseBody(objectKey)),
                )
                    .andExpect(status().isCreated)
                    .andReturn()
                    .response
                    .contentAsString,
            )["id"]
            .asString()

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
