package com.replus.api.mission.interfaces.rest

import com.replus.api.mission.domain.repository.MissionReleaseStateRepository
import com.replus.api.mission.domain.repository.VideoAssetRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.databind.ObjectMapper
import java.time.Duration
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
class MissionResponseApiTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var missionReleaseStateRepository: MissionReleaseStateRepository

    @Autowired
    private lateinit var videoAssetRepository: VideoAssetRepository

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

        val videoAsset = videoAssetRepository.findByObjectKey("rooms/$roomId/missions/$missionId/members/$memberId.webm")
        assertThat(videoAsset).isNotNull
        assertThat(videoAsset!!.status.name).isEqualTo("PENDING_UPLOAD")
    }

    @Test
    fun `리플 제출 후 오늘 화면은 내 영상 메타데이터를 보여준다`() {
        // given
        val today = getToday()
        val roomId = today["room"]["id"].asString()
        val missionId = today["mission"]["id"].asString()
        val uploadUrl = createUploadUrl(roomId, missionId)
        val objectKey = uploadUrl["objectKey"].asString()

        // when
        submitResponse(roomId, missionId, objectKey)

        // then
        val videoAsset = videoAssetRepository.findByObjectKey(objectKey)
        assertThat(videoAsset).isNotNull
        assertThat(videoAsset!!.status.name).isEqualTo("READY")

        mockMvc.perform(
            get("/api/rooms/$roomId/today")
                .header("Authorization", "Bearer dev-token-mina"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.viewer.hasSubmittedToday").value(true))
            .andExpect(jsonPath("$.viewer.todayResponseId").exists())
            .andExpect(jsonPath("$.participation.submittedCount").value(1))
            .andExpect(jsonPath("$.responses[0].isMine").value(true))
            .andExpect(jsonPath("$.responses[0].visibility").value("VISIBLE"))
            .andExpect(jsonPath("$.responses[0].video.objectKey").value(objectKey))
            .andExpect(jsonPath("$.responses[0].video.durationSeconds").value(3))
            .andExpect(jsonPath("$.responses[0].video.hasAudio").value(true))
    }

    @Test
    fun `리플 삭제 후 오늘 화면과 방 요약은 내 미제출 상태로 돌아간다`() {
        // given
        val today = getToday()
        val roomId = today["room"]["id"].asString()
        val missionId = today["mission"]["id"].asString()
        val objectKey = createUploadUrl(roomId, missionId)["objectKey"].asString()
        val responseId = submitResponse(roomId, missionId, objectKey)

        // when
        val result = mockMvc.perform(
            delete("/api/rooms/$roomId/responses/$responseId")
                .header("Authorization", "Bearer dev-token-mina"),
        )

        // then
        result.andExpect(status().isOk)
            .andExpect(jsonPath("$.responseId").value(responseId))
            .andExpect(jsonPath("$.status").value("DELETED"))
            .andExpect(jsonPath("$.frameStatus").value("DELETED"))
            .andExpect(jsonPath("$.deletedAt").exists())

        mockMvc.perform(
            get("/api/rooms/$roomId/today")
                .header("Authorization", "Bearer dev-token-mina"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.viewer.hasSubmittedToday").value(false))
            .andExpect(jsonPath("$.viewer.todayResponseId").doesNotExist())
            .andExpect(jsonPath("$.participation.submittedCount").value(0))
            .andExpect(jsonPath("$.responses").isEmpty)

        mockMvc.perform(
            get("/api/me")
                .header("Authorization", "Bearer dev-token-mina"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.rooms[0].today.myResponseStatus").value("NOT_SUBMITTED"))
            .andExpect(jsonPath("$.rooms[0].today.myResponseId").doesNotExist())

        mockMvc.perform(
            get("/api/rooms/$roomId")
                .header("Authorization", "Bearer dev-token-mina"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.today.myResponseStatus").value("NOT_SUBMITTED"))
            .andExpect(jsonPath("$.today.myResponseId").doesNotExist())
    }

    @Test
    fun `전원 제출 후에는 리플 삭제가 거절된다`() {
        // given
        val today = getToday()
        val roomId = today["room"]["id"].asString()
        val missionId = today["mission"]["id"].asString()

        val responseId = submitResponseForToken(roomId, missionId, token = "dev-token-mina")
        submitResponseForToken(roomId, missionId, token = "dev-token-joon")
        submitResponseForToken(roomId, missionId, token = "dev-token-ara")

        // when
        val result = mockMvc.perform(
            delete("/api/rooms/$roomId/responses/$responseId")
                .header("Authorization", "Bearer dev-token-mina"),
        )

        // then
        result.andExpect(status().isConflict)
            .andExpect(jsonPath("$.code").value("RESPONSE_RELEASE_LOCKED"))
    }

    @Test
    fun `업로드 URL 발급 없이 리플 제출하면 거절된다`() {
        // given
        val today = getToday()
        val roomId = today["room"]["id"].asString()
        val missionId = today["mission"]["id"].asString()
        val memberId = today["viewer"]["memberId"].asString()
        val objectKey = "rooms/$roomId/missions/$missionId/members/$memberId.webm"

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

    @Test
    fun `전원 제출되면 60초 공개 예약 상태를 저장한다`() {
        // given
        val today = getToday()
        val roomId = today["room"]["id"].asString()
        val missionId = today["mission"]["id"].asString()

        submitResponseForToken(roomId, missionId, token = "dev-token-mina")
        submitResponseForToken(roomId, missionId, token = "dev-token-joon")

        assertThat(missionReleaseStateRepository.findByMissionId(UUID.fromString(missionId)))
            .isNull()

        // when
        submitResponseForToken(roomId, missionId, token = "dev-token-ara")

        // then
        val releaseState = missionReleaseStateRepository.findByMissionId(UUID.fromString(missionId))
        assertThat(releaseState).isNotNull
        assertThat(releaseState!!.allSubmittedAt).isNotNull
        assertThat(releaseState.releaseScheduledAt).isNotNull
        assertThat(Duration.between(releaseState.allSubmittedAt, releaseState.releaseScheduledAt))
            .isEqualTo(Duration.ofSeconds(60))
        assertThat(releaseState.releasedAt).isNull()

        mockMvc.perform(
            get("/api/rooms/$roomId/today")
                .header("Authorization", "Bearer dev-token-mina"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.participation.submittedCount").value(3))
            .andExpect(jsonPath("$.participation.allSubmitted").value(true))
            .andExpect(jsonPath("$.participation.canViewFriendResponses").value(false))
    }

    @Test
    fun `공개 예약 시간이 지나면 오늘 화면에서 친구 영상도 보인다`() {
        // given
        val today = getToday()
        val roomId = today["room"]["id"].asString()
        val missionId = today["mission"]["id"].asString()

        submitResponseForToken(roomId, missionId, token = "dev-token-mina")
        submitResponseForToken(roomId, missionId, token = "dev-token-joon")
        submitResponseForToken(roomId, missionId, token = "dev-token-ara")

        val releaseState = missionReleaseStateRepository.findByMissionId(UUID.fromString(missionId))!!
        missionReleaseStateRepository.save(
            releaseState.copy(
                releaseScheduledAt = Instant.parse("2026-05-24T09:15:00Z"),
            ),
        )

        // when
        val result = mockMvc.perform(
            get("/api/rooms/$roomId/today")
                .header("Authorization", "Bearer dev-token-mina"),
        )

        // then
        val responseJson = objectMapper.readTree(
            result.andExpect(status().isOk)
                .andExpect(jsonPath("$.participation.canViewFriendResponses").value(true))
                .andReturn()
                .response
                .contentAsString,
        )
        val responses = responseJson["responses"]
        val friendResponses = (0 until responses.size())
            .map { responses[it] }
            .filter { !it["isMine"].booleanValue() }

        assertThat(friendResponses).hasSize(2)
        assertThat(friendResponses).allSatisfy {
            assertThat(it["visibility"].asString()).isEqualTo("VISIBLE")
            assertThat(it["video"]["objectKey"].asString()).isNotBlank()
        }

        val openedState = missionReleaseStateRepository.findByMissionId(UUID.fromString(missionId))
        assertThat(openedState!!.releasedAt).isNotNull
    }

    private fun getToday(token: String = "dev-token-mina") =
        objectMapper.readTree(
            mockMvc.perform(
                get("/api/rooms/aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa/today")
                    .header("Authorization", "Bearer $token"),
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

    private fun createUploadUrl(roomId: String, missionId: String, token: String) =
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

    private fun submitResponse(
        roomId: String,
        missionId: String,
        objectKey: String,
        token: String = "dev-token-mina",
    ): String =
        objectMapper
            .readTree(
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
            )["id"]
            .asString()

    private fun submitResponseForToken(roomId: String, missionId: String, token: String): String {
        val objectKey = createUploadUrl(roomId, missionId, token)["objectKey"].asString()
        return submitResponse(roomId, missionId, objectKey, token)
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
}
