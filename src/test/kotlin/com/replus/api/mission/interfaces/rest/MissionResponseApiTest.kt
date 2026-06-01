package com.replus.api.mission.interfaces.rest

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
        "delete from mission_release_states",
        "delete from mission_responses",
        "delete from video_assets",
        "delete from missions",
    ],
)
class MissionResponseApiTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    fun `업로드 URL은 현재 멤버의 object key를 발급한다`() {
        // given
        val today = getToday()
        val roomId = today["room"]["id"].asString()
        val missionId = today["mission"]["id"].asString()
        val memberId = today["viewer"]["memberId"].asString()
        val requestBody = """
            {
              "contentType": "video/webm",
              "fileSizeBytes": 842120,
              "durationSeconds": 3,
              "hasAudio": true,
              "width": 720,
              "height": 1280
            }
        """.trimIndent()

        // when
        val result = mockMvc.perform(
            post("/api/rooms/$roomId/missions/$missionId/responses/upload-url")
                .header("Authorization", "Bearer dev-token-mina")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody),
        )

        // then
        result.andExpect(status().isCreated)
            .andExpect(jsonPath("$.method").value("PUT"))
            .andExpect(jsonPath("$.objectKey").value("rooms/$roomId/missions/$missionId/members/$memberId.webm"))
            .andExpect(jsonPath("$.requiredHeaders['Content-Type']").value("video/webm"))
            .andExpect(jsonPath("$.maxFileSizeBytes").value(15000000))
    }

    @Test
    fun `이미 active 리플이 있으면 중복 제출은 거절된다`() {
        // given
        val today = getToday()
        val roomId = today["room"]["id"].asString()
        val missionId = today["mission"]["id"].asString()
        val uploadUrl = createUploadUrl(roomId, missionId)
        val objectKey = uploadUrl["objectKey"].asString()
        val requestBody = missionResponseBody(objectKey)

        mockMvc.perform(
            post("/api/rooms/$roomId/missions/$missionId/responses")
                .header("Authorization", "Bearer dev-token-mina")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody),
        ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.roomId").value(roomId))
            .andExpect(jsonPath("$.missionId").value(missionId))
            .andExpect(jsonPath("$.status").value("ACTIVE"))
            .andExpect(jsonPath("$.visibility").value("VISIBLE"))
            .andExpect(jsonPath("$.video.objectKey").value(objectKey))

        // when
        val result = mockMvc.perform(
            post("/api/rooms/$roomId/missions/$missionId/responses")
                .header("Authorization", "Bearer dev-token-mina")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody),
        )

        // then
        result.andExpect(status().isConflict)
            .andExpect(jsonPath("$.code").value("RESPONSE_ALREADY_EXISTS"))
    }

    private fun getToday() =
        objectMapper.readTree(
            mockMvc.perform(
                get("/api/rooms/aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa/today")
                    .header("Authorization", "Bearer dev-token-mina"),
            )
                .andExpect(status().isOk)
                .andReturn()
                .response
                .contentAsString,
        )

    private fun createUploadUrl(roomId: String, missionId: String) =
        objectMapper.readTree(
            mockMvc.perform(
                post("/api/rooms/$roomId/missions/$missionId/responses/upload-url")
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
}
