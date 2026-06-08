package com.replus.api.mission.interfaces.rest

import com.replus.api.mission.application.port.VideoStoragePort
import com.replus.api.mission.application.port.VideoUploadTarget
import com.replus.api.mission.application.port.VideoUploadVerification
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.databind.ObjectMapper
import java.time.Instant

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
class MissionResponseUploadVerificationApiTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    fun `storage upload verification 실패 시 리플 제출은 거절된다`() {
        // given
        val today = getToday()
        val roomId = today["room"]["id"].asString()
        val missionId = today["mission"]["id"].asString()
        val objectKey = createUploadUrl(roomId, missionId)["objectKey"].asString()

        // when
        val result = mockMvc.perform(
            post("/api/rooms/$roomId/missions/$missionId/responses")
                .header("Authorization", "Bearer dev-token-mina")
                .contentType(MediaType.APPLICATION_JSON)
                .content(missionResponseBody(objectKey)),
        )

        // then
        result.andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
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

    @TestConfiguration
    class FailingUploadVerificationStorageConfig {
        @Bean
        @Primary
        fun failingVideoStoragePort(): VideoStoragePort =
            object : VideoStoragePort {
                override fun createUploadTarget(
                    objectKey: String,
                    contentType: String,
                    expiresAt: Instant,
                    maxFileSizeBytes: Long,
                ): VideoUploadTarget =
                    VideoUploadTarget(
                        uploadUrl = "https://uploads.example.dev/$objectKey",
                        method = "PUT",
                        objectKey = objectKey,
                        requiredHeaders = mapOf("Content-Type" to contentType),
                        expiresAt = expiresAt,
                        maxFileSizeBytes = maxFileSizeBytes,
                    )

                override fun verifyUploadedObject(
                    objectKey: String,
                    expectedContentType: String,
                    expectedFileSizeBytes: Long,
                ): VideoUploadVerification =
                    VideoUploadVerification(
                        exists = false,
                        contentType = null,
                        fileSizeBytes = null,
                    )

                override fun playbackUrl(objectKey: String): String =
                    "https://cdn.example.dev/$objectKey"

                override fun thumbnailUrl(objectKey: String): String =
                    "https://cdn.example.dev/$objectKey"
            }
    }
}
