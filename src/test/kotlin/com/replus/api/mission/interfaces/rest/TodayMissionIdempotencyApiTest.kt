package com.replus.api.mission.interfaces.rest

import com.replus.api.mission.domain.repository.MissionRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper
import java.time.LocalDate
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
class TodayMissionIdempotencyApiTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var missionRepository: MissionRepository

    @Test
    fun `오늘 미션 조회는 처음 한 번만 생성하고 이후 같은 미션을 반환한다`() {
        val first = getToday()
        val second = getToday()

        val firstMission = first["mission"]
        val secondMission = second["mission"]
        val missionId = firstMission["id"].asString()
        val missionDate = firstMission["missionDate"].asString()

        assertThat(secondMission["id"].asString()).isEqualTo(missionId)
        assertThat(secondMission["createdAt"].asString()).isEqualTo(firstMission["createdAt"].asString())
        assertThat(secondMission["prompt"].asString()).isEqualTo(firstMission["prompt"].asString())
        assertThat(second["serverDate"].asString()).isEqualTo(first["serverDate"].asString())

        val storedMission = missionRepository.findByRoomIdAndMissionDate(
            roomId = UUID.fromString(ROOM_ID),
            missionDate = LocalDate.parse(missionDate),
        )
        assertThat(storedMission).isNotNull
        assertThat(storedMission!!.id.toString()).isEqualTo(missionId)
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
