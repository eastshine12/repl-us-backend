package com.replus.api.mission.interfaces.rest

import com.replus.api.mission.domain.repository.MissionReleaseStateRepository
import org.assertj.core.api.Assertions.assertThat
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
class ResponseInteractionApiTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var missionReleaseStateRepository: MissionReleaseStateRepository

    @Test
    fun `공개된 친구 리플에 리액션을 추가한다`() {
        // given
        val fixture = releasedFixture()
        val friendResponseId = fixture.friendResponseId

        // when
        val result = mockMvc.perform(
            post("/api/rooms/${fixture.roomId}/responses/$friendResponseId/reactions")
                .header("Authorization", "Bearer dev-token-mina")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"type":"HEART"}"""),
        )

        // then
        result.andExpect(status().isCreated)
            .andExpect(jsonPath("$.responseId").value(friendResponseId))
            .andExpect(jsonPath("$.memberId").value(fixture.viewerMemberId))
            .andExpect(jsonPath("$.type").value("HEART"))
            .andExpect(jsonPath("$.createdAt").exists())
    }

    @Test
    fun `공개된 친구 리플에 댓글을 추가한다`() {
        // given
        val fixture = releasedFixture()
        val friendResponseId = fixture.friendResponseId

        // when
        val result = mockMvc.perform(
            post("/api/rooms/${fixture.roomId}/responses/$friendResponseId/comments")
                .header("Authorization", "Bearer dev-token-mina")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"body":"편의점 어디 거야?"}"""),
        )

        // then
        result.andExpect(status().isCreated)
            .andExpect(jsonPath("$.responseId").value(friendResponseId))
            .andExpect(jsonPath("$.memberId").value(fixture.viewerMemberId))
            .andExpect(jsonPath("$.author.displayName").value("민아"))
            .andExpect(jsonPath("$.body").value("편의점 어디 거야?"))
            .andExpect(jsonPath("$.createdAt").exists())
    }

    @Test
    fun `아직 잠긴 친구 리플에는 리액션을 추가할 수 없다`() {
        // given
        val today = getToday()
        val roomId = today["room"]["id"].asString()
        val missionId = today["mission"]["id"].asString()
        val friendResponse = submitResponseForToken(roomId, missionId, token = "dev-token-joon")
        val friendResponseId = friendResponse["id"].asString()

        // when
        val result = mockMvc.perform(
            post("/api/rooms/$roomId/responses/$friendResponseId/reactions")
                .header("Authorization", "Bearer dev-token-mina")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"type":"WOW"}"""),
        )

        // then
        result.andExpect(status().isConflict)
            .andExpect(jsonPath("$.code").value("RESPONSE_NOT_VISIBLE"))
    }

    @Test
    fun `리액션 생성 후 오늘 화면은 리액션 요약을 보여준다`() {
        // given
        val fixture = releasedFixture()
        val friendResponseId = fixture.friendResponseId

        mockMvc.perform(
            post("/api/rooms/${fixture.roomId}/responses/$friendResponseId/reactions")
                .header("Authorization", "Bearer dev-token-mina")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"type":"HEART"}"""),
        ).andExpect(status().isCreated)

        // when
        val today = getTodayByRoomId(fixture.roomId)

        // then
        val friendResponse = findResponse(today, friendResponseId)
        val reactionSummary = friendResponse["reactionSummary"]
        assertThat(reactionSummary.size()).isEqualTo(1)
        assertThat(reactionSummary[0]["type"].asString()).isEqualTo("HEART")
        assertThat(reactionSummary[0]["count"].intValue()).isEqualTo(1)
        assertThat(reactionSummary[0]["reactedByViewer"].booleanValue()).isTrue()
    }

    @Test
    fun `공개된 리플의 댓글 목록을 작성 순서로 조회한다`() {
        // given
        val fixture = releasedFixture()
        val friendResponseId = fixture.friendResponseId

        createComment(fixture.roomId, friendResponseId, token = "dev-token-mina", body = "첫 댓글")
        createComment(fixture.roomId, friendResponseId, token = "dev-token-ara", body = "두 번째 댓글")

        // when
        val result = mockMvc.perform(
            get("/api/rooms/${fixture.roomId}/responses/$friendResponseId/comments")
                .header("Authorization", "Bearer dev-token-mina"),
        )

        // then
        result.andExpect(status().isOk)
            .andExpect(jsonPath("$.comments[0].body").value("첫 댓글"))
            .andExpect(jsonPath("$.comments[0].author.displayName").value("민아"))
            .andExpect(jsonPath("$.comments[1].body").value("두 번째 댓글"))
            .andExpect(jsonPath("$.comments[1].author.displayName").value("아라"))
    }

    @Test
    fun `아직 잠긴 친구 리플의 댓글 목록은 조회할 수 없다`() {
        // given
        val today = getToday()
        val roomId = today["room"]["id"].asString()
        val missionId = today["mission"]["id"].asString()
        val friendResponse = submitResponseForToken(roomId, missionId, token = "dev-token-joon")
        val friendResponseId = friendResponse["id"].asString()

        // when
        val result = mockMvc.perform(
            get("/api/rooms/$roomId/responses/$friendResponseId/comments")
                .header("Authorization", "Bearer dev-token-mina"),
        )

        // then
        result.andExpect(status().isConflict)
            .andExpect(jsonPath("$.code").value("RESPONSE_NOT_VISIBLE"))
    }

    private fun releasedFixture(): ReleasedFixture {
        val today = getToday()
        val roomId = today["room"]["id"].asString()
        val missionId = today["mission"]["id"].asString()
        val viewerMemberId = today["viewer"]["memberId"].asString()

        submitResponseForToken(roomId, missionId, token = "dev-token-mina")
        val friendResponse = submitResponseForToken(roomId, missionId, token = "dev-token-joon")
        submitResponseForToken(roomId, missionId, token = "dev-token-ara")

        val releaseState = missionReleaseStateRepository.findByMissionId(UUID.fromString(missionId))!!
        missionReleaseStateRepository.save(
            releaseState.copy(releaseScheduledAt = Instant.parse("2026-05-24T09:15:00Z")),
        )

        mockMvc.perform(
            get("/api/rooms/$roomId/today")
                .header("Authorization", "Bearer dev-token-mina"),
        ).andExpect(status().isOk)

        assertThat(missionReleaseStateRepository.findByMissionId(UUID.fromString(missionId))!!.releasedAt)
            .isNotNull

        return ReleasedFixture(
            roomId = roomId,
            viewerMemberId = viewerMemberId,
            friendResponseId = friendResponse["id"].asString(),
        )
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

    private fun getTodayByRoomId(roomId: String, token: String = "dev-token-mina") =
        objectMapper.readTree(
            mockMvc.perform(
                get("/api/rooms/$roomId/today")
                    .header("Authorization", "Bearer $token"),
            )
                .andExpect(status().isOk)
                .andReturn()
                .response
                .contentAsString,
        )

    private fun findResponse(today: tools.jackson.databind.JsonNode, responseId: String): tools.jackson.databind.JsonNode {
        val responses = today["responses"]
        return (0 until responses.size())
            .map { responses[it] }
            .first { it["id"].asString() == responseId }
    }

    private fun createComment(roomId: String, responseId: String, token: String, body: String) {
        mockMvc.perform(
            post("/api/rooms/$roomId/responses/$responseId/comments")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"body":"$body"}"""),
        ).andExpect(status().isCreated)
    }

    private fun submitResponseForToken(roomId: String, missionId: String, token: String) =
        objectMapper.readTree(
            mockMvc.perform(
                post("/api/rooms/$roomId/missions/$missionId/responses")
                    .header("Authorization", "Bearer $token")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(missionResponseBody(createUploadUrl(roomId, missionId, token)["objectKey"].asString())),
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

    private data class ReleasedFixture(
        val roomId: String,
        val viewerMemberId: String,
        val friendResponseId: String,
    )
}
