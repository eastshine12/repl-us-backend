package com.replus.api.room.interfaces.rest

import com.replus.api.mission.domain.repository.MissionReleaseStateRepository
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
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper
import java.time.Instant
import java.util.UUID

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
class RoomWallApiTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var missionReleaseStateRepository: MissionReleaseStateRepository

    @Test
    fun `벽 조회는 내 리플은 준비 상태로 친구 리플은 공개 전 잠금 상태로 내려준다`() {
        val today = getToday()
        val roomId = today["room"]["id"].asString()
        val missionId = today["mission"]["id"].asString()

        val myResponse = submitResponseForToken(roomId, missionId, token = "dev-token-mina")
        submitResponseForToken(roomId, missionId, token = "dev-token-joon")

        mockMvc.perform(
            get("/api/rooms/$roomId/wall")
                .header("Authorization", "Bearer dev-token-mina"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.roomId").value(roomId))
            .andExpect(jsonPath("$.viewer.hasSubmittedToday").value(true))
            .andExpect(jsonPath("$.viewer.todayResponseId").value(myResponse["id"].asString()))
            .andExpect(jsonPath("$.viewport.width").value(1600))
            .andExpect(jsonPath("$.frames[0].slotIndex").value(0))
            .andExpect(jsonPath("$.frames[0].status").value("READY"))
            .andExpect(jsonPath("$.frames[0].response.isMine").value(true))
            .andExpect(jsonPath("$.frames[0].response.video.objectKey").value(myResponse["video"]["objectKey"].asString()))
            .andExpect(jsonPath("$.frames[1].slotIndex").value(1))
            .andExpect(jsonPath("$.frames[1].status").value("LOCKED"))
            .andExpect(jsonPath("$.frames[1].response").doesNotExist())
    }

    @Test
    fun `공개 시간이 지나면 벽 조회는 친구 리플도 준비 상태로 내려준다`() {
        val today = getToday()
        val roomId = today["room"]["id"].asString()
        val missionId = today["mission"]["id"].asString()

        submitResponseForToken(roomId, missionId, token = "dev-token-mina")
        val friendResponse = submitResponseForToken(roomId, missionId, token = "dev-token-joon")
        submitResponseForToken(roomId, missionId, token = "dev-token-ara")

        val releaseState = missionReleaseStateRepository.findByMissionId(UUID.fromString(missionId))!!
        missionReleaseStateRepository.save(
            releaseState.copy(releaseScheduledAt = Instant.parse("2026-05-24T09:15:00Z")),
        )

        mockMvc.perform(
            get("/api/rooms/$roomId/wall")
                .header("Authorization", "Bearer dev-token-mina"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.frames[1].status").value("READY"))
            .andExpect(jsonPath("$.frames[1].response.id").value(friendResponse["id"].asString()))
            .andExpect(jsonPath("$.frames[1].response.isMine").value(false))
            .andExpect(jsonPath("$.frames[1].response.video.objectKey").value(friendResponse["video"]["objectKey"].asString()))
    }

    @Test
    fun `벽 조회는 보이는 리플의 리액션 요약을 내려준다`() {
        val today = getToday()
        val roomId = today["room"]["id"].asString()
        val missionId = today["mission"]["id"].asString()
        val myResponse = submitResponseForToken(roomId, missionId, token = "dev-token-mina")
        val responseId = myResponse["id"].asString()

        mockMvc.perform(
            post("/api/rooms/$roomId/responses/$responseId/reactions")
                .header("Authorization", "Bearer dev-token-mina")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"type":"HEART"}"""),
        ).andExpect(status().isCreated)

        mockMvc.perform(
            get("/api/rooms/$roomId/wall")
                .header("Authorization", "Bearer dev-token-mina"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.frames[0].response.reactionSummary[0].type").value("HEART"))
            .andExpect(jsonPath("$.frames[0].response.reactionSummary[0].count").value(1))
            .andExpect(jsonPath("$.frames[0].response.reactionSummary[0].reactedByViewer").value(true))
    }

    @Test
    fun `active member가 아니면 벽을 조회할 수 없다`() {
        val roomId = createRoomAsMina()

        mockMvc.perform(
            get("/api/rooms/$roomId/wall")
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
                        .content("""{"name":"벽 테스트 방"}"""),
                )
                    .andExpect(status().isCreated)
                    .andReturn()
                    .response
                    .contentAsString,
            )["id"]
            .asString()

    private fun getToday(token: String = "dev-token-mina"): JsonNode =
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

    private fun submitResponseForToken(roomId: String, missionId: String, token: String): JsonNode {
        val objectKey = createUploadUrl(roomId, missionId, token)["objectKey"].asString()
        return objectMapper.readTree(
            mockMvc.perform(
                post("/api/rooms/$roomId/missions/$missionId/responses")
                    .header("Authorization", "Bearer $token")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(missionResponseBody(objectKey)),
            )
                .andExpect(status().isCreated)
                .andReturn()
                .response
                .contentAsString,
        )
    }

    private fun createUploadUrl(roomId: String, missionId: String, token: String): JsonNode =
        objectMapper.readTree(
            mockMvc.perform(
                post("/api/rooms/$roomId/missions/$missionId/responses/upload-url")
                    .header("Authorization", "Bearer $token")
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

    private companion object {
        const val ROOM_ID = "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa"
    }
}
